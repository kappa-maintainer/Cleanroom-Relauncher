# Changelog

## [Unreleased]

### Changed (BREAKING)
- The config GUI is no longer controlled by two booleans. `Show Config` is now a mode with the values
  `WHEN_OUTDATED` (default), `ALWAYS` and `NEVER`; `Always Show Config` was removed. Existing values are
  dropped on first load, so the new default applies.
- `WHEN_OUTDATED` shows the config GUI only when the instance does not run Cleanroom yet or does not match
  the version pinned by the local MMC pack. This replaces the `Show Config = false` write-back, so the
  configuration file is no longer modified at runtime and stays identical across launches.
- The latest Cleanroom version is resolved through the official download API
  (`https://download.cleanroommc.com/api/download`, the endpoint used by the official Cleanroom Updater)
  instead of scraping `maven-metadata.xml`.

### Changed
- Launch behaviour is decided from the loader and launcher layout instead of the JVM version: instances
  already running Cleanroom are never relaunched again, and non-Prism launchers on Java 9+ are handled
  like every other non-MultiMC launcher.

## [1.0.0] - 2023-09-15

### Added
- This is a default template changelog that follows the [KeepAChangelog Convention](https://keepachangelog.com/en/1.1.0/)
