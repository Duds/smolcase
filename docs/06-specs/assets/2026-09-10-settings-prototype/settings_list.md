# SMOLCASE Settings List

Settings are opened by long-pressing the face.

## Thinking Engine

Choose one backend:

- **Agent**: cloud, OpenAI-compatible endpoint. Default backend.
- **Gemini Nano**: on-device and private.
- **Gemma 4 E2B**: on-device, sideloaded, unstable on Pixel 8 Tensor G3.
- **Rules only**: offline canned responses, no AI.

### Agent endpoint

- **Provider label**: human-readable provider name, such as `OpenRouter`.
- **Base URL**: default `https://openrouter.ai/api/v1`.
- **API key**: masked bearer token.
- **Model**: default `mistralai/mixtral-8x22b-instruct`.
- **Max tokens**: `64` to `1024`, default `200`.
- **Temperature**: `0.00` to `2.00`, default `0.80`.

The endpoint supports OpenAI-compatible providers, including OpenRouter, Together, Groq, Perplexity, and local vLLM servers.

## Personality

- **Humor**: `0%` to `100%`, default `75%`.
- **Honesty**: `0%` to `100%`, default `90%`.

Both dials affect the creature's responses and are stored locally.

## Voice

- **Voice**: cycle through available English Android text-to-speech voices.
- Default: automatic male voice selection.
- Voice samples use: `Systems nominal.`

## Cloud TTS

Disabled by default. When enabled, cloud TTS is tried first and local Android TTS is used as the fallback.

- **Enable cloud TTS**: on/off.
- **Provider label**: default `ElevenLabs`.
- **Base URL**: default `https://api.elevenlabs.io/v1`.
- **API key**: masked.
- **Voice ID**: default `21m00Tcm4TlvDq8ikWAM`.
- **Model ID**: default `eleven_monolingual_v1`.
- **Stability**: `0%` to `100%`, default `50%`.
- **Similarity**: `0%` to `100%`, default `75%`.

## Cloud Vision

Disabled by default. Captures and analyses a single camera frame only when explicitly requested. It is not continuous cloud streaming.

- **Enable cloud vision**: on/off.
- **Base URL**: default `https://openrouter.ai/api/v1`.
- **API key**: masked.
- **Model**: default `gpt-4o`.

## Cloud Reply Generation

Disabled by default. Routes reply generation through an independent cloud endpoint when enabled.

- **Enable cloud reply generation**: on/off.
- **Base URL**: default `https://openrouter.ai/api/v1`.
- **API key**: masked.
- **Model**: default `mistralai/mixtral-8x22b-instruct`.

## Sensors

Each sensor has an on/off toggle, a polling interval, and a live readout.

| Sensor | What SMOLCASE learns | Default | Default interval |
|---|---|---:|---:|
| Magnetometer | Which direction the creature faces | On | 500 ms |
| Accelerometer | Whether the creature is moved or picked up | On | 200 ms |
| Gyroscope | Rotation and orientation changes | On | 200 ms |
| Light | Whether the desk is lit or the room is dark | Off | 2,000 ms |
| Barometer | Weather changes and altitude | Off | 2,000 ms |
| Proximity | Whether something is close to the face | On | 500 ms |
| Ambient temperature | Room temperature | Off | 5,000 ms |

Available interval choices are `100`, `200`, `500`, `1,000`, `2,000`, `5,000`, and `10,000` milliseconds.

## Save behaviour

- Use **Save** to persist the settings form.
- Sensor changes persist immediately.
- API keys are stored in private Android `SharedPreferences`.
- Leaving with unsaved changes shows a discard warning.
- Cloud features fall back locally when the network or provider is unavailable.

## Storage namespaces

- `smolcase_llm`: thinking engine and cloud settings.
- `smolcase_dials`: humor, honesty, and selected voice.
- `smolcase_sensors`: sensor toggles and polling intervals.

## Notes

- Existing `kimi_*` endpoint keys migrate to `agent_*` keys automatically.
- The old `KIMI` backend name migrates to `AGENT`.
- Gemma is migrated to Agent when read because Gemma 4 E2B is unstable on Tensor G3.
