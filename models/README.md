# SMOLCASE Model Manifest

## Trained Policies

| Policy | Status | Steps | Best Distance | TFLite Export | Notes |
|--------|--------|-------|--------------|---------------|-------|
| `smolcase_gait` | ✅ Trained | 500K | +7.32m vs CPG -1.13m | ❌ Pending | Walk Forward baseline |

## Checkpoints

Saved every 50K steps during training. See `checkpoints/` directory.

## Export Pipeline

1. Train with PPO in MuJoCo
2. Evaluate: `python sim/src/smolcase_train.py --mode eval --episodes 5`
3. Export to TFLite: `python sim/src/smolcase_train.py --mode export --model models/smolcase_gait.zip`
4. Move `.tflite` to `models/exports/`
5. Update this manifest

## Next Policies to Train

See `docs/02-behaviour-training/SMOLCASE-Behaviour-Training-Plan.md` for full curriculum.

Priority queue:
1. Walk Backward
2. Turn Left / Turn Right
3. Recover from Fall (back)
4. Recover from Fall (front)
5. Bow / Greet
