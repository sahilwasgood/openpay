# OpenPay brand

One line: **pay even with zero signal.** Everything below exists to serve that sentence.

## Voice

- Plain, short, declarative. Bilkul simple.
- Honest about limits before the user finds them. "No confirmation, no record."
- Relatable over technical: khata, udhaar, the mall basement, the hill road.
- Never: fintech-speak, growth words, exclamation marks, emojis in product copy.

## Palette

| Token | Hex | Use |
|---|---|---|
| ink | `#0D0F0E` | dark surfaces, primary text on light |
| paper | `#FAFAF7` | light surfaces |
| green | `#0E7C46` | primary actions, the brand color |
| green bright | `#2FBF71` | accents on dark, rail dots |
| green soft | `#E4F2E8` | tints, badges |
| amber | `#B45309` | warnings, pending states |
| line | `#E3E1D8` | hairlines on light |
| dark line | `#262B28` | hairlines on dark |

Rules: tint neutrals toward green instead of dead gray. One accent per view. The dark green glow (radial, 16% opacity) is the only background effect allowed.

## Type

- Display: **Space Grotesk** (600, 700). Headlines, numbers, buttons.
- Body: **Inter** (400, 500). Everything that needs reading, not admiring.
- Mono: system monospace for serials, refs, code chips, eyebrows.
- Headlines 60px+, letter spacing -0.02em or tighter. Body 14 to 17px on product, 20px+ in video.

## Iconography

Stroke based, 1.7 weight, round caps, single color. Filled shapes only for the brand mark itself.

## Motion

- 0.3 to 0.7 second entrances, `power2`/`power3` ease, elements enter with transform + opacity, staggered 0.2 to 0.45s.
- Ambient motion is one shared breath or drift across 2 to 5 decorative layers, slow (10s+).
- Nothing bounces except brand moments (`back.out` 1.6 to 1.7 on the mark and QR reveals).
- State changes in product explain themselves: the rail ladder dims what is not picked, the picked card lifts.

## Applications in this repo

- Site and web app: `site/styles.css` is the token source of truth for the web.
- Android: `ui/theme/Theme.kt` mirrors the same tokens, Space Grotesk and Inter ship as variable fonts in `res/font/`.
- Videos: `brand/videos/` holds the HyperFrames projects. Each `index.html` is a self-contained composition; regenerate any of them with `npx hyperframes render --quality looks --output out.mp4`.

## Copy rules

No em or en dashes, use the middle dot ·. No "no ads" style claims, state what the code proves instead. Numbers in rupees with the Rs prefix in plain text, ₹ in UI. NPCI services are always named as NPCI's.
