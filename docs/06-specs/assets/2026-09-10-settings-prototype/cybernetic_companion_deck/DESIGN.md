---
name: Cybernetic Companion Deck
colors:
  surface: '#0f131c'
  surface-dim: '#0f131c'
  surface-bright: '#353942'
  surface-container-lowest: '#0a0e16'
  surface-container-low: '#181c24'
  surface-container: '#1c2028'
  surface-container-high: '#262a33'
  surface-container-highest: '#31353e'
  on-surface: '#dfe2ee'
  on-surface-variant: '#b9cacb'
  inverse-surface: '#dfe2ee'
  inverse-on-surface: '#2c3039'
  outline: '#849495'
  outline-variant: '#3b494b'
  surface-tint: '#00dbe9'
  primary: '#dbfcff'
  on-primary: '#00363a'
  primary-container: '#00f0ff'
  on-primary-container: '#006970'
  inverse-primary: '#006970'
  secondary: '#ffb2ba'
  on-secondary: '#67001f'
  secondary-container: '#dd034e'
  on-secondary-container: '#fff1f1'
  tertiary: '#eef7ff'
  on-tertiary: '#00354a'
  tertiary-container: '#aee0ff'
  on-tertiary-container: '#00668b'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#7df4ff'
  primary-fixed-dim: '#00dbe9'
  on-primary-fixed: '#002022'
  on-primary-fixed-variant: '#004f54'
  secondary-fixed: '#ffd9dc'
  secondary-fixed-dim: '#ffb2ba'
  on-secondary-fixed: '#400010'
  on-secondary-fixed-variant: '#910030'
  tertiary-fixed: '#c4e7ff'
  tertiary-fixed-dim: '#7bd0ff'
  on-tertiary-fixed: '#001e2c'
  on-tertiary-fixed-variant: '#004c69'
  background: '#0f131c'
  on-background: '#dfe2ee'
  surface-variant: '#31353e'
  oled-pure: '#05070B'
  surface-deck: '#0E1420'
  surface-panel: '#141C2B'
  surface-border: '#1E2A3E'
  surface-border-active: '#00F0FF'
  led-matrix-off: '#0D1520'
  led-matrix-dim: '#082B3E'
  neon-coral: '#F43F5E'
  status-warning: '#F59E0B'
  status-online: '#10B981'
  text-dim: '#64748B'
  text-mid: '#94A3B8'
  text-bright: '#F1F5F9'
typography:
  headline-xl:
    fontFamily: Space Grotesk
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 38px
    letterSpacing: -0.02em
  headline-xl-mobile:
    fontFamily: Space Grotesk
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-lg:
    fontFamily: Space Grotesk
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Space Grotesk
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-code-lg:
    fontFamily: JetBrains Mono
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 18px
    letterSpacing: 0.05em
  label-code-md:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.04em
  label-code-sm:
    fontFamily: JetBrains Mono
    fontSize: 10px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.08em
  telemetry-num:
    fontFamily: JetBrains Mono
    fontSize: 20px
    fontWeight: '700'
    lineHeight: 24px
    letterSpacing: -0.02em
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  space-xxs: 0.125rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-base: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
  space-2xl: 3rem
  panel-gutter: 0.75rem
  screen-margin: 1rem
---

## Brand & Style
This design system defines the interface for an intimate, tactile pocket AI companion. The aesthetic converges retro-futuristic laboratory hardware, cybernetic telemetry decks, and tactile synth instruments. The target audience values technical sovereignty, pocket hardware craft, and low-latency interaction with expressive synthetic life.

The visual grammar balances pitch-black OLED power conservation with luminescent dot-matrix LED optics and crisp, machined instrument panel controls. It merges functional terminal brutalism with ambient cybernetic warmth: utilitarian diagnostic readouts meet emotive LED gaze expressions.

## Colors
The palette is engineered specifically for high-efficiency OLED displays and high-contrast ambient legibility. The background utilizes absolute near-black values (`#05070B` and `#0B0F17`) to maximize physical depth and allow glowing phosphor and diode components to visually project outward from the chassis.

- **Primary (`#00F0FF`)**: High-voltage cyan evoking cold cathode tubes and primary LED matrices. Used for key state toggles, active track indicators, and primary command vectors.
- **Secondary (`#FF2E63`)**: Bio-luminescent heart coral. Serves as emotional expression states, warning indicators, and high-priority alerts.
- **Tertiary (`#38BDF8`)**: Softened atmospheric cerulean used for secondary telemetry, dial tracks, and ambient matrix backlighting.
- **Neutral (`#0B0F17`)**: Dense cyber-chassis slate. Serves as the substrate for tactile modules, switch cavities, and input frames.

## Typography
Typography reflects a dedicated dual-layer architecture: operational system telemetry uses monospaced mechanics (`JetBrains Mono`), human prose uses neutral clarity (`Inter`), and module headers use structured geometric tension (`Space Grotesk`).

Telemetry labels, parameter names, sensor readouts, and masked cryptographic tokens must always render in uppercase `JetBrains Mono` with expanded tracking. Headings are concise, clinical, and framed as machine subsystem declarations (e.g., `THINKING_ENGINE`, `NEURAL_DIALS`).

## Elevation & Depth
Depth is created without generic drop shadows. Instead, the design system implements tactile chassis routing, luminous LED bloom, and low-contrast technical wire-borders:

- **Panel Recesses**: Inactive containers and input bays use inner chassis darks (`#05070B` or `#0E1420`) framed by razor-sharp 1px structural outlines (`#1E2A3E`).
- **Diode Glow**: Active elements (toggled switches, active sliders, matrix pins) emit direct localized luminescence using double-layered glows: a tight primary halo (`box-shadow: 0 0 6px rgba(0, 240, 255, 0.65)`) combined with a diffuse field (`box-shadow: 0 0 16px rgba(0, 240, 255, 0.25)`).
- **Physical Bevels**: Modals, drawer sheets, and critical alert panes introduce a 1px top border highlight (`rgba(255, 255, 255, 0.12)`) simulating physical light raking across a bevelled enclosure edge.

## Shapes
Shapes evoke injection-molded tech frames and CNC-milled synth shells. Radii are kept crisp and micro-softened (Level 1 / 4px default) to retain sharp cybernetic discipline while preventing harsh corners on mobile glass.

- **Standard Bays & Buttons**: 4px radius (`0.25rem`) with 1px tactile borders.
- **Sliders & Indicators**: Hardware track channels utilize 2px micro-radii; indicator LEDs and circular micro-switches use fully rounded pins (50% circle).
- **Dot-Matrix Pixels**: Emotive face and indicator elements use discrete round LED units with subtle physical housing rims.

## Components

### Micro-Switches & Toggles
- **Structure**: Horizontal enclosure (`44px` height target, `18px` inner slot) featuring a mechanical sliding pill switch.
- **States**: 
  - Off: Dark slate housing (`#0E1420`), 1px outline (`#1E2A3E`), dim grey toggle notch (`#64748B`).
  - On: Glowing cyan slider thumb (`#00F0FF`) with matched cyan drop glow and active micro-dot readout.

### Parameter Dials & Sliders (Hardware Potentiometers)
- **Track**: Recessed 4px line with an LED segmented tick-mark background.
- **Fill**: Solid high-saturation glow (`#00F0FF` or `#FF2E63`) displaying percentage or discrete interval levels.
- **Thumb**: Industrial rectangular tactile grip with a center notch, displaying live numeric telemetry above in `JetBrains Mono` (`telemetry-num`).

### Input Fields (Terminal Registers)
- **Container**: OLED dark field (`#05070B`) with 1px border (`#1E2A3E`).
- **Focus**: Border flashes immediately to active cyan (`#00F0FF`) with an interior faint cyan corner bracket indicator.
- **Masking**: Secret keys and sensitive bearer tokens display discrete masked dot glyphs with a quick-reveal button marked `REVEAL_KEY`.

### Subsystem Panels (Cards)
- **Header**: Rigid terminal ribbon containing subsystem name in uppercase label code (`THINKING ENGINE [SYS-01]`) accompanied by an active status pin (green dot for live agent, amber for offline/rules).
- **Body**: Inset container background `#0E1420` surrounded by `1px solid #1E2A3E`.

### Segmented Buttons (Radio Switch Arrays)
- Used for mutually exclusive engine options (Agent / Nano / Gemma / Rules).
- Formed as a conjoined hardware rail where each item occupies equal width. Selected segment gains an elevated dark-slate cap, a solid 1px cyan outline, and sharp cyan monospaced text.

### Telemetry Readout Badges
- Compact chips showing polling rates and real-time millisecond tickers (`200ms`, `LIVE: X=-0.12`).
- Styled with faint semi-transparent LED cyan backings (`rgba(0, 240, 255, 0.08)`) and high-contrast monospaced cyan typography.