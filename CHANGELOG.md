# Changelog

## [0.0.8] - 2026-11-07

### Added
- Game setting for amount of cards required to win a game
- Game setting for maximum duration for each turn
- Game setting for maximum steal time for each steal action
- Game setting for maximum buffer time after confirming input
- Game setting for amount of chips each player can keep
- Game setting for amount of chips each player starts with
- Game settings for actions players can perform with chips
- Different game modes, feauturing different difficulties
- Session Manager for session IDs
- Song Set 'Hits Made in Germany', feauturing songs from Germany
- Song Set 'Wacken', feauturing rock and metal hits from one of Germany's biggest festivals.

### Changed
- Game Settings are now centralized in one class
- Some dialogs now take the existing stage instead of creating a new window for accessibility
- Reworked layout for game setup
- 'Move Time' is now 'Turn Time'
- Set Images now with less padding
- Nordics Rock Set updated Image and Description

### Fixed
- If song isn't available in user's country, the game now asks the user if they want to skip the song without losing a chip.
- No update dialog with current version anymore because of suffix

### Deprecated

### Removed