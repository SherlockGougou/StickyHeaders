# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-01-21

### Added
- Initial release
- `StickyLinearLayout` - A vertical LinearLayout that supports multi-level sticky headers
- `ViewOffsetHelper` - Helper class for managing view offsets via translationY
- `AppBarLayoutBehavior` - Optional behavior to fix common AppBarLayout scrolling issues
- Support for unlimited number of sticky headers with `app:layout_pin="true"` attribute
- Proper stacking order for multiple sticky headers
- Seamless integration with `AppBarLayout` and `CoordinatorLayout`
- `OnStickyStateChangedListener` - Callback interface for monitoring sticky state changes
  - `onViewPinned()` - Called when a view becomes pinned
  - `onViewUnpinned()` - Called when a view becomes unpinned
  - `onPinnedViewOffsetChanged()` - Called when pinned view offset changes
  - `onPinnedViewsChanged()` - Called when the list of pinned views changes
  - `onPinnedHeightChanged()` - Called when total pinned height changes
- Helper methods: `getPinnedViews()`, `getPinnedHeight()`, `isViewPinned()`
- **Two sticky modes via `app:stickyMode` attribute:**
  - `multi` (default): Multiple headers stack on top of each other
  - `single`: Only one header visible, new header pushes out the previous one
- Programmatic mode switching via `stickyMode` property

### Features
- Multi-level sticky header support
- Automatic stacking of pinned views
- Custom drawing order to ensure correct z-order
- Works with `RecyclerView`, `NestedScrollView`, and other scrolling containers
- Minimal API surface - just one attribute: `layout_pin`
- Rich callback interface for state monitoring

## [Unreleased]

### Planned
- Support for horizontal orientation
- Animation options for pin/unpin transitions
- Snap behavior customization
