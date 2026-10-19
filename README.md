# SimpleNick

Minimal nickname plugin for **Paper 26.3** and **Java 25**.

Commands (OP only):
- `/nick <real-player-name> <nickname>` — rename an online player, including spaces (up to 32 characters)
- `/unnick <real-player-name>` — restore the original name
- `/nicklist` — show saved nicknames

Tab completion suggests online real player names. Nicknames are saved in `plugins/SimpleNick/config.yml`, visible in the player list and normal chat (also to the renamed player). Real Minecraft account names, skins, nametags above heads, and third-party chat plugins are not changed. Nicknames are not a Mojang account rename.

## Installation
Download the JAR from the latest successful [Actions build](../../actions/workflows/build.yml), extract the artifact ZIP, place the JAR into the server's `plugins` directory and restart Paper.

## Build
Requires Java 25 and Gradle 9.x. Run `gradle build`. The JAR is under `build/libs/`.

## License
GNU GPL v3 or later. See LICENSE.
