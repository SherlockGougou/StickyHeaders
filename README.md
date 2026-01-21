# StickyHeader (traditional View)

This repo contains a reusable Android View-based sticky header container.

## Module

- `:stickyheader`: the library module.
- `:app`: a tiny demo app.

## Library: `StickyContainerLayout`

A vertical container (`ViewGroup`) that:

- Lays out children vertically.
- Supports marking multiple children as **sticky** via XML `app:layout_sticky="true"`.
- When scrolling vertically, sticky children will **pin to the top** (supports multiple pinned in `stack` mode).
- Works with a nested scrolling child (e.g. `RecyclerView`) via `NestedScrollingParent3`.

### XML usage

```xml
<com.gouqinglin.stickyheader.lib.StickyContainerLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:nestedScrollableChildId="@id/recycler"
    app:stickyMode="stack">

    <TextView
        ...
        app:layout_sticky="true" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recycler"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</com.gouqinglin.stickyheader.lib.StickyContainerLayout>
```

### Attributes

Container:
- `app:stickyMode`: `stack` | `single`
- `app:stickyTopInset`: top inset applied to pinned area (useful with status bar / toolbar)
- `app:nestedScrollableChildId`: which child should be treated as the nested scroll target

Child LayoutParams:
- `app:layout_sticky`: whether this child is sticky
- `app:layout_stickyGroup`: reserved for future grouping support
- `app:layout_stickyZIndex`: reserved for future z-index support

### Public APIs

- `setStickyMode(StickyMode)`
- `setStickyTopInset(px: Int)`
- `setNestedScrollableChild(view: View?)`
- `invalidateSticky()`
- `setOnStickyChangedListener(OnStickyChangedListener?)`

## Notes / roadmap

This is a solid baseline that already supports:
- Multiple sticky headers
- A RecyclerView as a child with nested scrolling support

Next improvements (already designed for):
- Better touch + fling handling (handoff between parent and child)
- Grouped stickies
- Custom `StickyBehavior` for transitions/animations
- WindowInsets integration inside the container
