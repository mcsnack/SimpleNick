# SimpleNick v2.2.0

Lightweight nickname and colored group-prefix plugin for **Paper 26.3** and **Java 25**.

## Commands (OP only)
- `/nick <player> <nickname>` — set nickname (up to 32 characters; spaces allowed)
- `/unnick <player>` — reset nickname, preserving group
- `/nicklist` — list nicknames
- `/group <group> <color>` — any player sets their own group\n- `/group <player> <group> <color>` — OP sets another player's group
- `/ungroup` — any player removes their own group\n- `/ungroup <player>` — OP removes another player's group

Example: `/nick Luca HansWurst` then `/group Luca Technik red` displays **[Technik] HansWurst** in chat and TAB, with only the bracketed prefix in red.

Group names accept 1–8 ASCII letters, numbers or underscores. All 16 standard Minecraft colors are supported: `black`, `dark_blue`, `dark_green`, `dark_aqua`, `dark_red`, `dark_purple`, `gold`, `gray`, `dark_gray`, `blue`, `green`, `aqua`, `red`, `light_purple`, `yellow`, `white`. Color and player-name TAB completion is provided.

All changes persist across restarts in `plugins/SimpleNick/config.yml`. Nickname commands and changing another player's group require operator status; self-group commands are available to everyone. Players must be online to be modified. Display names in the default chat renderer and player TAB list are affected; real usernames, skins and overhead nametags are unchanged. Third-party chat plugins may override formatting.

## Installation
Download the JAR from a successful [Actions build](../../actions/workflows/build.yml), put it into your Paper server's `plugins` directory, and restart. Existing v1 nicknames remain saved.

## License
GNU GPL v3 or later. Copyright © 2026 mal0o_Live. See LICENSE.
