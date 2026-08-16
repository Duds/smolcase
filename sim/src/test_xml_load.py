import mujoco

# Test loading the GrowBot MuJoCo model
xml_path = "growbot_current_body.xml"
model = mujoco.MjModel.from_xml_path(xml_path)
data = mujoco.MjData(model)

print(f"✅ Model loaded successfully!")
print(f"   Bodies: {model.nbody}")
print(f"   Joints: {model.njnt}")
print(f"   Actuators: {model.nu}")
print(f"   DOFs: {model.nv}")
print(f"   Sensors: {model.nsensordata}")
print()

# Print body names
print("Bodies:")
for i in range(model.nbody):
    name = mujoco.mj_id2name(model, mujoco.mjtObj.mjOBJ_BODY, i)
    print(f"   [{i}] {name}")

print()
print("Joints:")
for i in range(model.njnt):
    name = mujoco.mj_id2name(model, mujoco.mjtObj.mjOBJ_JOINT, i)
    jnt_range = model.jnt_range[i]
    print(f"   [{i}] {name} | range: [{jnt_range[0]:.1f}, {jnt_range[1]:.1f}] deg")

print()
print("Actuators:")
for i in range(model.nu):
    name = mujoco.mj_id2name(model, mujoco.mjtObj.mjOBJ_ACTUATOR, i)
    ctrl_range = model.actuator_ctrlrange[i]
    print(f"   [{i}] {name} | ctrl_range: [{ctrl_range[0]:.2f}, {ctrl_range[1]:.2f}]")

print()
print("Sensors:")
for i in range(model.nsensor):
    name = mujoco.mj_id2name(model, mujoco.mjtObj.mjOBJ_SENSOR, i)
    print(f"   [{i}] {name}")

# Run one step to verify dynamics work
mujoco.mj_step(model, data)
print(f"\n✅ Simulation step OK. qpos: {data.qpos[:6]}")
