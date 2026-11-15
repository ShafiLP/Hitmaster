# Changelog

## [0.0.8] - 2026-11-07

### Added
- Different game modes, feauturing different difficulties
- Dummy settings, only card amount and chip amount works for now, others will be added later
- Song Set 'Hits Made in Germany', feauturing songs from Germany
- Song Set 'Wacken', feauturing rock and metal hits from one of Germany's biggest festivals.
- Session Manager for session IDs

### Changed
- Game Settings are now centralized in one class
- Some dialogs now take the existing stage instead of creating a new window for accessibility
- Reworked layout for game setup
- 'Move Time' is now 'Turn Time'
- Set Images now with less padding
- Nordics Rock Set updated Image and Description
- Parts of logic that were inside GameView class moved to GameLogic

### Fixed
- If song isn't available in user's country, the game now asks the user if they want to skip the song without losing a chip
- No update dialog with current version anymore because of suffix
- Dragging on StealView doesn't draw card on GameView anymore
