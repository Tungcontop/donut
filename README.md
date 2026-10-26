# Donut Pay Tracker (Fabric, Minecraft 1.21.4, client-side)

Lists incoming `<name> paid you $<amount>` chat messages in a HUD. Open chat (T / Enter)
and click WIN / LOSE / SKIP to mark the current payment with a check, an X, or a dash.
"Undo last" reverts the most recent mark. Scroll the mouse wheel over the panel for older entries.

It only reads chat and draws on your own screen. It never sends commands or payments.

## Build
1. Install JDK 21.
2. In this folder run `gradle wrapper --gradle-version 8.11.1` once (needs Gradle installed),
   or copy `src/` and `fabric.mod.json` into the official Fabric example mod template.
3. `./gradlew build` -> jar is in `build/libs/donut-pay-tracker-1.0.0.jar`
4. Put the jar in your `mods` folder next to Fabric API for 1.21.4.

## Try it without the server
`/donutpay test 25.1M` adds a fake payment. `/donutpay clear` empties the list.

## Tweaks
- Panel position: `originX` / `originY` in `HudRenderer.java`
- What a win pays (the "x = ..." line): `WIN_MULTIPLIER` in `PaymentTracker.java` (default 2.0)
- Rows shown at once: `VISIBLE_ROWS` in `PaymentTracker.java`
- Chat format it matches: the `PAID_YOU` regex in `PaymentTracker.java`
