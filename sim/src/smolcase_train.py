#!/usr/bin/env python3
"""
SMOLCASE MuJoCo Training Pipeline
==================================
Train a gait policy for a 2-servo biped robot using PPO.

Usage:
    python3 smolcase_train.py --mode train --timesteps 500000
    python3 smolcase_train.py --mode eval --model models/smolcase_gait.zip
    python3 smolcase_train.py --mode cpg --episodes 10
    python3 smolcase_train.py --mode export --model models/smolcase_gait.zip

Phases:
    1. CPG baseline: hand-tuned oscillator gait (no learning)
    2. PPO training: reinforcement learning in MuJoCo simulation
    3. Export: convert trained policy to TFLite for Pixel 8 deployment
"""

from __future__ import annotations

import argparse
import os
import time
from pathlib import Path
from typing import Any

import gymnasium as gym
import mujoco
import numpy as np
from gymnasium import spaces
from stable_baselines3 import PPO
from stable_baselines3.common.callbacks import BaseCallback, CheckpointCallback
from stable_baselines3.common.monitor import Monitor
from stable_baselines3.common.vec_env import DummyVecEnv

# ──────────────────────────────────────────────────────────────────────────────
# Configuration
# ──────────────────────────────────────────────────────────────────────────────

XML_PATH = "growbot_current_body.xml"
MODELS_DIR = Path("models")
LOGS_DIR = Path("logs")
CPG_FREQ = 1.5  # Hz — step frequency
CPG_AMP = 0.8  # rad — leg swing amplitude
CPG_CENTER = 0.0  # rad — neutral joint position
CTRL_LIMIT = 1.57  # ~90 deg in rad — servo target limit
FRAME_SKIP = 5  # physics steps per RL step (500Hz / 5 = 100Hz control)
MAX_EPISODE_STEPS = 5000  # 50 seconds at 100Hz

# Reward weights
W_FORWARD = 2.0
W_UPRIGHT = 0.5
W_ENERGY = 0.05
W_FOOT_CONTACT = 0.1
W_ALIVE = 1.0
W_ACTION = 0.01

# ──────────────────────────────────────────────────────────────────────────────
# CPG Gait Controller (Baseline — no learning)
# ──────────────────────────────────────────────────────────────────────────────

class CPGGait:
    """
    Central Pattern Generator: simple sinusoidal oscillator for alternating gait.
    Produces target joint positions for left and right legs.
    """

    def __init__(self, freq: float = CPG_FREQ, amp: float = CPG_AMP,
                 center: float = CPG_CENTER):
        self.omega = 2.0 * np.pi * freq
        self.amp = amp
        self.center = center
        self.phase_offset = np.pi  # 180 deg out of phase for alternating walk

    def step(self, t: float, speed: float = 1.0) -> np.ndarray:
        """
        Returns [left_target, right_target] in radians.
        speed: 0.0=stand, 1.0=normal walk, -1.0=backwards
        """
        left = self.center + speed * self.amp * np.sin(self.omega * t)
        right = self.center + speed * self.amp * np.sin(self.omega * t + self.phase_offset)
        return np.array([left, right], dtype=np.float32)

    def turn(self, t: float, amount: float) -> np.ndarray:
        """
        amount: -1.0=sharp left, 0.0=straight, 1.0=sharp right
        """
        bias = amount * 0.3  # rad bias for turning
        left = self.center + self.amp * np.sin(self.omega * t) + bias
        right = self.center + self.amp * np.sin(self.omega * t + self.phase_offset) - bias
        return np.array([left, right], dtype=np.float32)


# ──────────────────────────────────────────────────────────────────────────────
# GrowBot MuJoCo Environment
# ──────────────────────────────────────────────────────────────────────────────

class GrowBotEnv(gym.Env):
    """
    Gymnasium environment for a 2-servo biped walking robot.

    Action space: Box([-1, -1], [1, 1]) — normalized servo targets
    Observation space: 13-dim vector:
        [0:2]   body pitch, roll (from IMU)
        [2:4]   gyro x, y (angular velocity)
        [4:6]   joint positions (left, right)
        [6:8]   joint velocities (left, right)
        [8]     body height above floor
        [9:11]  last action
        [11:13] foot contact booleans
    """

    metadata = {"render_modes": ["human", "rgb_array"], "render_fps": 50}

    def __init__(self, xml_path: str = XML_PATH, frame_skip: int = FRAME_SKIP,
                 max_episode_steps: int = MAX_EPISODE_STEPS,
                 render_mode: str | None = None):
        super().__init__()

        self.render_mode = render_mode
        self.frame_skip = frame_skip
        self.max_episode_steps = max_episode_steps

        # Load MuJoCo model
        self.model = mujoco.MjModel.from_xml_path(xml_path)
        self.data = mujoco.MjData(self.model)

        # Find relevant body/joint IDs
        self.body_id = mujoco.mj_name2id(self.model, mujoco.mjtObj.mjOBJ_BODY, "base_body")
        self.joint_left = mujoco.mj_name2id(self.model, mujoco.mjtObj.mjOBJ_JOINT, "joint_left")
        self.joint_right = mujoco.mj_name2id(self.model, mujoco.mjtObj.mjOBJ_JOINT, "joint_right")
        self.leg_left = mujoco.mj_name2id(self.model, mujoco.mjtObj.mjOBJ_BODY, "leg_left")
        self.leg_right = mujoco.mj_name2id(self.model, mujoco.mjtObj.mjOBJ_BODY, "leg_right")

        # Actuator indices
        self.act_left = 0
        self.act_right = 1

        # Joint QPOS indices (after freejoint which has 7 dims: pos[x,y,z] + quat[w,x,y,z])
        # Free joint occupies qpos[0:7], then joint_left at qpos[7], joint_right at qpos[8]
        self.qpos_left = 7
        self.qpos_right = 8
        self.qvel_left = 6  # free joint has 6 DOFs
        self.qvel_right = 7

        # Action / observation spaces
        self.action_space = spaces.Box(low=-1.0, high=1.0, shape=(2,), dtype=np.float32)

        obs_dim = 13
        obs_high = np.ones(obs_dim, dtype=np.float32) * np.inf
        obs_low = -obs_high
        self.observation_space = spaces.Box(low=obs_low, high=obs_high, dtype=np.float32)

        # Rendering
        self.renderer = None
        if render_mode == "rgb_array":
            self.renderer = mujoco.Renderer(self.model, height=240, width=320)

        # Episode state
        self.step_count = 0
        self.last_action = np.zeros(2, dtype=np.float32)
        self._initial_x = 0.0

    def _get_obs(self) -> np.ndarray:
        """Build observation vector from MuJoCo state."""
        qpos = self.data.qpos
        qvel = self.data.qvel

        # Body orientation (pitch, roll from quaternion)
        # Free joint quaternion is at qpos[3:7] (w, x, y, z)
        quat = qpos[3:7]
        w, x, y, z = quat
        pitch = np.arctan2(2.0 * (w * y - z * x), 1.0 - 2.0 * (y * y + z * z))
        roll = np.arctan2(2.0 * (w * x + y * z), 1.0 - 2.0 * (x * x + y * y))

        # Gyro (angular velocity) — free joint qvel[0:3] is angular vel
        gyro = qvel[0:3]

        # Joint positions and velocities
        jpos_left = qpos[self.qpos_left]
        jpos_right = qpos[self.qpos_right]
        jvel_left = qvel[self.qvel_left]
        jvel_right = qvel[self.qvel_right]

        # Body height
        height = qpos[2]  # z-position of base_body

        # Foot contact (approximate: check if leg tip is near floor)
        # Simplified: check body height — if < threshold, we're down
        foot_contact_left = 1.0 if height < 0.07 else 0.0
        foot_contact_right = foot_contact_left  # Simplified

        obs = np.array([
            pitch, roll,
            gyro[0], gyro[1],
            jpos_left, jpos_right,
            jvel_left, jvel_right,
            height,
            self.last_action[0], self.last_action[1],
            foot_contact_left, foot_contact_right,
        ], dtype=np.float32)

        return obs

    def _compute_reward(self, action: np.ndarray) -> tuple[float, bool, bool]:
        """
        Compute reward and done flag.
        Returns: (reward, terminated, truncated)
        """
        qpos = self.data.qpos
        qvel = self.data.qvel

        # Forward velocity (positive x direction)
        forward_vel = qvel[0]  # x-velocity of free joint

        # Upright bonus (penalize tilting too far)
        quat = qpos[3:7]
        w, x, y, z = quat
        pitch = np.arctan2(2.0 * (w * y - z * x), 1.0 - 2.0 * (y * y + z * z))
        roll = np.arctan2(2.0 * (w * x + y * z), 1.0 - 2.0 * (x * x + y * y))
        upright_penalty = abs(pitch) + abs(roll)

        # Height check — fallen?
        height = qpos[2]
        fallen = height < 0.04 or upright_penalty > 1.2  # ~68 deg

        # Energy penalty (joint velocities squared)
        jvel_left = qvel[self.qvel_left]
        jvel_right = qvel[self.qvel_right]
        energy = jvel_left ** 2 + jvel_right ** 2

        # Action magnitude penalty (discourage jitter)
        action_penalty = np.sum(action ** 2)

        # Foot contact bonus (encourage grounded steps)
        contact_bonus = 0.0
        if height > 0.04 and not fallen:
            contact_bonus = 0.1

        reward = (
            W_FORWARD * forward_vel
            - W_UPRIGHT * upright_penalty
            - W_ENERGY * energy
            - W_ACTION * action_penalty
            + contact_bonus
            + W_ALIVE  # alive bonus per step
        )

        # Termination conditions
        terminated = bool(fallen)
        truncated = self.step_count >= self.max_episode_steps

        return float(reward), terminated, truncated

    def reset(self, *, seed: int | None = None, options: dict | None = None):
        super().reset(seed=seed)

        # Reset MuJoCo state
        mujoco.mj_resetData(self.model, self.data)

        # Randomize initial pose slightly for robustness
        if self.np_random is not None:
            noise = self.np_random.normal(0, 0.05, size=self.model.nq)
            self.data.qpos[:] = self.model.qpos0 + noise
            # Keep z-height reasonable
            self.data.qpos[2] = max(0.06, self.data.qpos[2])
        else:
            self.data.qpos[:] = self.model.qpos0

        mujoco.mj_forward(self.model, self.data)

        self.step_count = 0
        self.last_action = np.zeros(2, dtype=np.float32)
        self._initial_x = float(self.data.qpos[0])

        obs = self._get_obs()
        info = {}

        if self.render_mode == "human":
            self.render()

        return obs, info

    def step(self, action: np.ndarray):
        # Normalize action [-1, 1] to servo target range
        # Map to +/- CTRL_LIMIT radians
        targets = np.clip(action, -1.0, 1.0) * CTRL_LIMIT
        self.data.ctrl[self.act_left] = targets[0]
        self.data.ctrl[self.act_right] = targets[1]
        self.last_action = np.copy(action)

        # Step physics multiple times per control step
        for _ in range(self.frame_skip):
            mujoco.mj_step(self.model, self.data)

        self.step_count += 1

        obs = self._get_obs()
        reward, terminated, truncated = self._compute_reward(action)

        info = {
            "forward_vel": float(self.data.qvel[0]),
            "height": float(self.data.qpos[2]),
            "x_pos": float(self.data.qpos[0] - self._initial_x),
        }

        if self.render_mode == "human":
            self.render()

        return obs, reward, terminated, truncated, info

    def render(self):
        if self.render_mode == "rgb_array" and self.renderer is not None:
            self.renderer.update_scene(self.data)
            return self.renderer.render()
        return None

    def close(self):
        if self.renderer is not None:
            self.renderer.close()


# ──────────────────────────────────────────────────────────────────────────────
# Training Callback: Log Progress
# ──────────────────────────────────────────────────────────────────────────────

class TrainingMonitorCallback(BaseCallback):
    """Custom callback to print training progress."""

    def __init__(self, check_freq: int = 10000, verbose: int = 1):
        super().__init__(verbose)
        self.check_freq = check_freq
        self.episode_rewards = []
        self.episode_lengths = []

    def _on_step(self) -> bool:
        if self.n_calls % self.check_freq == 0:
            # Get stats from the logger
            mean_reward = np.mean(self.episode_rewards[-20:]) if self.episode_rewards else 0
            print(f"  Step {self.n_calls:,} | mean reward (last 20 eps): {mean_reward:+.3f}")
        return True

    def _on_rollout_end(self) -> None:
        # Collect episode info from the environment
        infos = self.locals.get("infos", [])
        for info in infos:
            if "episode" in info:
                self.episode_rewards.append(info["episode"]["r"])
                self.episode_lengths.append(info["episode"]["l"])


# ──────────────────────────────────────────────────────────────────────────────
# CPG Evaluation
# ──────────────────────────────────────────────────────────────────────────────

def evaluate_cpg(env: GrowBotEnv, episodes: int = 10, render: bool = False):
    """Evaluate the CPG baseline gait."""
    cpg = CPGGait(freq=CPG_FREQ, amp=CPG_AMP, center=CPG_CENTER)
    dt = env.model.opt.timestep * env.frame_skip  # control timestep

    rewards_all = []
    distances_all = []

    print(f"\nEvaluating CPG baseline ({episodes} episodes)...")

    for ep in range(episodes):
        obs, _ = env.reset()
        episode_reward = 0.0
        episode_steps = 0

        for step in range(MAX_EPISODE_STEPS):
            t = step * dt
            targets = cpg.step(t, speed=1.0)
            # Convert rad targets to normalized action [-1, 1]
            action = np.clip(targets / CTRL_LIMIT, -1.0, 1.0)

            obs, reward, terminated, truncated, info = env.step(action)
            episode_reward += reward
            episode_steps += 1

            if render and env.render_mode == "rgb_array":
                frame = env.render()
                # Could save frames here

            if terminated or truncated:
                break

        rewards_all.append(episode_reward)
        distances_all.append(info.get("x_pos", 0.0))
        print(f"   Episode {ep+1}: reward={episode_reward:+.2f} | steps={episode_steps} | distance={info.get('x_pos', 0):.3f}m")

    print(f"\nCPG Results:")
    print(f"   Mean reward:  {np.mean(rewards_all):+.2f} +/- {np.std(rewards_all):.2f}")
    print(f"   Mean distance: {np.mean(distances_all):+.3f}m +/- {np.std(distances_all):.3f}m")

    return rewards_all, distances_all


# ──────────────────────────────────────────────────────────────────────────────
# PPO Training
# ──────────────────────────────────────────────────────────────────────────────

def train_ppo(total_timesteps: int = 500_000, save_dir: Path = MODELS_DIR):
    """Train a PPO policy for gait."""
    save_dir.mkdir(exist_ok=True)
    LOGS_DIR.mkdir(exist_ok=True)

    print(f"\nStarting PPO training for {total_timesteps:,} timesteps...")
    print(f"   Save dir: {save_dir.absolute()}")
    print(f"   Logs dir: {LOGS_DIR.absolute()}")

    # Create vectorized environment (SB3 expects VecEnv)
    def make_env():
        env = GrowBotEnv(render_mode=None)
        env = Monitor(env)
        return env

    vec_env = DummyVecEnv([make_env])

    # PPO hyperparameters tuned for locomotion
    model = PPO(
        "MlpPolicy",
        vec_env,
        learning_rate=3e-4,
        n_steps=2048,
        batch_size=64,
        n_epochs=10,
        gamma=0.99,
        gae_lambda=0.95,
        clip_range=0.2,
        ent_coef=0.01,  # Encourage exploration
        vf_coef=0.5,
        max_grad_norm=0.5,
        verbose=1,
        tensorboard_log=str(LOGS_DIR),
        device="auto",
    )

    # Save checkpoints every 50k steps
    checkpoint_cb = CheckpointCallback(
        save_freq=50_000,
        save_path=str(save_dir / "checkpoints"),
        name_prefix="smolcase_gait",
    )

    # Custom monitor
    monitor_cb = TrainingMonitorCallback(check_freq=10_000)

    # Train
    start = time.time()
    model.learn(
        total_timesteps=total_timesteps,
        callback=[checkpoint_cb, monitor_cb],
        progress_bar=True,
    )
    elapsed = time.time() - start

    # Save final model
    final_path = save_dir / "smolcase_gait.zip"
    model.save(final_path)
    print(f"\nTraining complete in {elapsed/60:.1f} minutes")
    print(f"   Final model saved: {final_path}")

    vec_env.close()
    return model


# ──────────────────────────────────────────────────────────────────────────────
# Policy Evaluation
# ──────────────────────────────────────────────────────────────────────────────

def evaluate_policy(model_path: str, episodes: int = 10):
    """Evaluate a trained PPO policy."""
    env = GrowBotEnv(render_mode=None)
    env = Monitor(env)

    model = PPO.load(model_path, env=env)

    print(f"\nEvaluating trained policy: {model_path}")

    rewards_all = []
    distances_all = []

    for ep in range(episodes):
        obs, _ = env.reset()
        episode_reward = 0.0
        episode_steps = 0
        done = False

        while not done:
            action, _ = model.predict(obs, deterministic=True)
            obs, reward, terminated, truncated, info = env.step(action)
            episode_reward += reward
            episode_steps += 1
            done = terminated or truncated

        rewards_all.append(episode_reward)
        distances_all.append(info.get("x_pos", 0.0))
        print(f"   Episode {ep+1}: reward={episode_reward:+.2f} | steps={episode_steps} | distance={info.get('x_pos', 0):.3f}m")

    print(f"\nPolicy Results:")
    print(f"   Mean reward:   {np.mean(rewards_all):+.2f} +/- {np.std(rewards_all):.2f}")
    print(f"   Mean distance: {np.mean(distances_all):+.3f}m +/- {np.std(distances_all):.3f}m")

    env.close()
    return rewards_all, distances_all


# ──────────────────────────────────────────────────────────────────────────────
# Export to TFLite
# ──────────────────────────────────────────────────────────────────────────────

def export_to_tflite(model_path: str, output_path: str = "models/smolcase_gait.tflite"):
    """
    Export trained PPO policy to TensorFlow Lite.
    This creates a standalone .tflite file that can run on Pixel 8.
    """
    import torch
    import torch.onnx
    import onnx
    import tf2onnx
    import tensorflow as tf

    print(f"\nExporting policy to TFLite...")

    # Load the policy network
    model = PPO.load(model_path)
    policy = model.policy

    # Get the observation dimension
    obs_dim = policy.observation_space.shape[0]

    # Create a wrapper that replicates the forward pass through the policy head
    class PolicyWrapper(torch.nn.Module):
        def __init__(self, policy_module):
            super().__init__()
            self.mlp_extractor = policy_module.mlp_extractor
            self.action_net = policy_module.action_net

        def forward(self, obs):
            # obs: [batch, obs_dim]
            features = self.mlp_extractor(obs)
            action_logits = self.action_net(features)
            # For deterministic inference, we just need the mean
            # In SB3, action_net outputs the mean of the Gaussian
            return action_logits

    wrapper = PolicyWrapper(policy)
    wrapper.eval()

    # Dummy input for tracing
    dummy_input = torch.randn(1, obs_dim)

    # Export to ONNX
    onnx_path = output_path.replace(".tflite", ".onnx")
    torch.onnx.export(
        wrapper,
        dummy_input,
        onnx_path,
        input_names=["observation"],
        output_names=["action"],
        dynamic_axes={"observation": {0: "batch"}, "action": {0: "batch"}},
        opset_version=11,
    )
    print(f"   ONNX: {onnx_path}")

    # Convert ONNX to TensorFlow SavedModel
    import onnx
    from onnx_tf.backend import prepare

    onnx_model = onnx.load(onnx_path)
    tf_rep = prepare(onnx_model)
    tf_path = output_path.replace(".tflite", "_tf")
    tf_rep.export_graph(tf_path)
    print(f"   TF SavedModel: {tf_path}")

    # Convert to TFLite
    converter = tf.lite.TFLiteConverter.from_saved_model(tf_path)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_types = [tf.float32]
    tflite_model = converter.convert()

    with open(output_path, "wb") as f:
        f.write(tflite_model)
    print(f"   TFLite: {output_path}")

    # Also save a simple inference test
    print(f"\nTesting TFLite inference...")
    interpreter = tf.lite.Interpreter(model_path=output_path)
    interpreter.allocate_tensors()

    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    print(f"   Input:  {input_details[0]['shape']}  dtype={input_details[0]['dtype']}")
    print(f"   Output: {output_details[0]['shape']} dtype={output_details[0]['dtype']}")

    # Test inference
    test_obs = np.zeros((1, obs_dim), dtype=np.float32)
    interpreter.set_tensor(input_details[0]['index'], test_obs)
    interpreter.invoke()
    action = interpreter.get_tensor(output_details[0]['index'])
    print(f"   Test action output: {action}")
    print(f"\nExport complete! Deploy '{output_path}' to your Pixel 8.")


# ──────────────────────────────────────────────────────────────────────────────
# Main CLI
# ──────────────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="SMOLCASE Gait Training")
    parser.add_argument("--mode", choices=["train", "eval", "cpg", "export", "test"],
                        default="train", help="What to run")
    parser.add_argument("--model", type=str, default="models/smolcase_gait.zip",
                        help="Path to trained model (for eval/export)")
    parser.add_argument("--timesteps", type=int, default=500_000,
                        help="Training timesteps")
    parser.add_argument("--episodes", type=int, default=10,
                        help="Evaluation episodes")
    parser.add_argument("--render", action="store_true",
                        help="Enable rendering (slow)")
    args = parser.parse_args()

    if args.mode == "test":
        # Quick smoke test of the environment
        print("Environment smoke test...")
        env = GrowBotEnv(render_mode=None)
        obs, _ = env.reset(seed=42)
        print(f"   Observation shape: {obs.shape}")
        print(f"   Observation sample: {obs[:5]}")
        action = env.action_space.sample()
        obs, reward, terminated, truncated, info = env.step(action)
        print(f"   Step OK: reward={reward:.3f}, done={terminated or truncated}")
        env.close()
        print("   Environment test passed!")
        return

    if args.mode == "cpg":
        env = GrowBotEnv(render_mode="rgb_array" if args.render else None)
        evaluate_cpg(env, episodes=args.episodes, render=args.render)
        env.close()
        return

    if args.mode == "train":
        train_ppo(total_timesteps=args.timesteps)
        return

    if args.mode == "eval":
        if not Path(args.model).exists():
            print(f"Model not found: {args.model}")
            return
        evaluate_policy(args.model, episodes=args.episodes)
        return

    if args.mode == "export":
        if not Path(args.model).exists():
            print(f"Model not found: {args.model}")
            return
        export_to_tflite(args.model)
        return


if __name__ == "__main__":
    main()
