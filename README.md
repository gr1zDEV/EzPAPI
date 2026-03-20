# EZPAPI

EZPAPI is a Paper/Folia backend utility plugin that lets admins and console store configurable per-player values, then expose them through built-in PlaceholderAPI placeholders.

## Features
- Config-defined placeholder schema with `boolean`, `integer`, `double`, and `string` types.
- YAML-backed UUID storage for player overrides.
- Admin/console-only `/ezpapi` command set for `set`, `get`, `toggle`, `reset`, and `reload`.
- Built-in PlaceholderAPI expansion using `%ezpapi_<key>%` and `%ezpapi_<key>_formatted%`.
- Optional Floodgate-aware placeholders: `%ezpapi_is_bedrock%` and `%ezpapi_platform%`.
- Paper + Folia safe implementation with no unsafe scheduler usage.

## Dependencies
- **Paper / Folia 1.21.x**
- **PlaceholderAPI** is optional but recommended. If it is missing, the plugin still loads but does not register placeholders.
- **Floodgate** is optional. If present, EZPAPI can detect Bedrock players for the optional platform placeholders.

## Build
```bash
gradle build
```

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
- Player overrides are stored separately in `players.yml`.
- Resetting a value removes the override so the default applies again.
- Reloading refreshes the placeholder schema and messages.

## Storage
EZPAPI stores only explicit per-player overrides in `plugins/EZPAPI/players.yml`.
Defaults stay in `config.yml`, so unset values always resolve from the schema instead of duplicating data into every player record.

## Bedrock / Geyser / Floodgate compatibility
- Player data is stored by UUID only.
- Commands resolve online and offline players by name when the server can resolve them.
- Floodgate is optional and never required for normal functionality.
- Bedrock players joining through Geyser/Floodgate use the same backend variable system as Java players.

## Admin-only design
EZPAPI is intentionally a backend/admin utility plugin, not a normal player command plugin.
By default, only operators or holders of `ezpapi.admin` can use the command suite.
