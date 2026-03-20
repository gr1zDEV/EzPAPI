# EZPAPI

EZPAPI is a Paper/Folia backend utility plugin that lets admins and console store configurable per-player values, then expose them through built-in PlaceholderAPI placeholders.

## Features
- Config-defined placeholder schema with `boolean`, `integer`, `double`, and `string` types.
- SQLite-backed UUID storage for player overrides in `plugins/EZPAPI/ezpapi.db`.
- Admin/console-only `/ezpapi` command set for `set`, `get`, `toggle`, `reset`, and `reload`.
- Built-in PlaceholderAPI expansion using `%ezpapi_<key>%` and `%ezpapi_<key>_formatted%`.
- Internal PlaceholderAPI registration that persists across `/papi reload` and serves both player and offline-player API calls.
- Optional Floodgate-aware placeholders: `%ezpapi_is_bedrock%` and `%ezpapi_platform%`.
- Paper + Folia safe implementation with no unsafe scheduler usage.
- One-time migration from legacy `players.yml` storage into SQLite.

## Dependencies
- **Paper / Folia 1.21.x**
- **PlaceholderAPI** is optional but recommended. If it is missing, the plugin still loads but does not register placeholders.
- **Floodgate** is optional. If present, EZPAPI can detect Bedrock players for the optional platform placeholders.
- **SQLite JDBC** is declared as a runtime library in `plugin.yml`, so Paper downloads it for the server at runtime.

## Build
```bash
gradle build
```

## Release automation
Publishing a GitHub Release now triggers `.github/workflows/release-build.yml`, which checks out the release tag, builds the plugin with Java 21 + Gradle, uploads the generated JARs as workflow artifacts, and attaches them to the GitHub release automatically.

## Commands
These commands are meant for admins, console, automation, and backend workflows.

- `/ezpapi set <player> <key> <value>`
- `/ezpapi get <player> <key>`
- `/ezpapi toggle <player> <key>`
- `/ezpapi reset <player> <key>`
- `/ezpapi reload`

Examples:
- `/ezpapi set Gr1zZtv nightvision_toggle true`
- `/ezpapi get Gr1zZtv nightvision_toggle`
- `/ezpapi toggle Gr1zZtv chat_toggle`
- `/ezpapi reset Gr1zZtv multiplier`

## Placeholder usage
Commands use plain configured keys such as `nightvision_toggle`.
Other plugins read them with PlaceholderAPI placeholders:

- `%ezpapi_nightvision_toggle%` -> `true` or `false`
- `%ezpapi_nightvision_toggle_formatted%` -> configured boolean format such as `&aON`
- `%ezpapi_rank_name%` -> `Member` or the player's overridden value
- `%ezpapi_platform%` -> `bedrock` or `java`

If a player has no stored override yet, EZPAPI returns the default from `config.yml`.

### How the PlaceholderAPI expansion is registered
EZPAPI ships its own internal `PlaceholderExpansion` implementation. During plugin enable, EZPAPI loads its config and SQLite storage first, then registers the `ezpapi` expansion before command setup so PlaceholderAPI-aware plugins can immediately resolve `%ezpapi_<key>%` placeholders at runtime.

The expansion also overrides `persist()` and returns `true`, so PlaceholderAPI keeps the internal expansion registered during `/papi reload` instead of unloading it. That means plugins such as zMenu, EZChat, and DeluxeMenus can keep calling PlaceholderAPI normally without needing a separate eCloud expansion jar.

### Using EZPAPI placeholders from other plugins
Any plugin that already supports PlaceholderAPI can use EZPAPI values directly, including plugins that call `PlaceholderAPI.setPlaceholders(player, text)` internally when opening menus, formatting chat, or rendering scoreboards.

Examples:
- `%ezpapi_nightvision_toggle%`
- `%ezpapi_nightvision_toggle_formatted%`
- `%ezpapi_rank_name%`
- `%ezpapi_platform%`

Example `zMenu` item snippet:

```yml
items:
  toggle-nightvision:
    material: LIME_DYE
    name: "&aNight Vision"
    lore:
      - "&7Current state: %ezpapi_nightvision_toggle_formatted%"
      - "&8Raw value: %ezpapi_nightvision_toggle%"
```

When zMenu opens that menu for a player, PlaceholderAPI runtime parsing resolves those placeholders through EZPAPI's internal expansion. If the player has no SQLite override yet, EZPAPI returns the configured default value instead.

## Configuring new placeholders
Add entries under `placeholders:` in `config.yml`.

```yml
placeholders:
  nightvision_toggle:
    type: boolean
    default: false
    true-format: "&aON"
    false-format: "&cOFF"

  coins_boost:
    type: integer
    default: 0

  rank_name:
    type: string
    default: "Member"
```

Rules:
- Only keys defined in `config.yml` are valid.
- SQLite stores only player-specific overrides as text rows.
- `config.yml` remains the source of truth for valid keys, types, formats, and defaults.
- Resetting a value deletes the SQLite override so the default applies again.
- Reloading refreshes the placeholder schema and messages without replacing the database file.

## Storage
EZPAPI stores only explicit per-player overrides in `plugins/EZPAPI/ezpapi.db`.
Defaults stay in `config.yml`, so unset values always resolve from the schema instead of duplicating data into every player record.

### SQLite schema
EZPAPI creates the `player_variables` table on startup:

- `player_uuid TEXT NOT NULL`
- `variable_key TEXT NOT NULL`
- `value TEXT NOT NULL`
- `updated_at INTEGER NOT NULL`
- `PRIMARY KEY (player_uuid, variable_key)`

Values are stored as text in SQLite and parsed back into the configured placeholder type when read.

### YAML migration
If EZPAPI finds a legacy `players.yml` file on startup, it migrates each valid UUID/key override into SQLite one time.
After a successful migration, the old file is renamed to `players.yml.migrated` and will not be migrated again.
Unknown keys or invalid stored values are skipped with clear console warnings.

## Bedrock / Geyser / Floodgate compatibility
- Player data is stored by UUID only.
- Commands resolve online and offline players by name when the server can resolve them.
- Floodgate is optional and never required for normal functionality.
- Bedrock players joining through Geyser/Floodgate use the same backend variable system as Java players.

## Admin-only design
EZPAPI is intentionally a backend/admin utility plugin, not a normal player command plugin.
By default, only operators or holders of `ezpapi.admin` can use the command suite.
