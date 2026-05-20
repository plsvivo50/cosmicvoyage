# Space-Rendering Architecture

> Version: 1.0 | Date: 2026-05-20 | Source: devlog_2026-05-09 ~ devlog_2026-05-13a

---

## 1. Overview

Space rendering uses a **dual-sky system** that switches between two visual modes based on distance to the target celestial body, plus a **HUD navigation overlay** for distant guidance.

---

## 2. Dual-Sky System

### 2.1 StarsBackground (Primary Sky)

- Pure black background with stars
- Active at all distances
- Uses `SpaceSkyRenderer` with custom star field

### 2.2 World Curvature Effect (Near-Earth/Moon)

- 3D sphere rendering of Earth/Moon visible when close
- Earth: radius = `SpaceConstants.EARTH_RENDER_RADIUS` (200.0f)
- Moon: radius = `SpaceConstants.MOON_RENDER_RADIUS` (50.0f)
- Atmosphere halo: `SpaceConstants.EARTH_ATMOSPHERE_RADIUS` (240.0f)

---

## 3. LOD Switching System

### 3.1 Distance Thresholds

| Threshold | Value | Effect |
|:---|:---|:---|
| `HUD_HIDE_DISTANCE` | 600.0f (3x EARTH_RADIUS) | Hide HUD arrows, show 3D sphere |
| `TRIGGER_LAND_EARTH` | 800.0f | Trigger Earth return transition |
| `TRIGGER_LAND_MOON` | 200.0f | Trigger Moon landing transition |

### 3.2 Visual State Machine

```
Distance > 600: HUD arrows visible (navigation aid)
Distance < 600: HUD hidden, 3D sphere takes over
Distance < trigger: Transition sequence begins
```

---

## 4. Renderer Responsibilities

### 4.1 EarthRenderer
- Renders Earth sphere at `(0, 0, 0)` in space dimension
- Uses `EARTH_RENDER_RADIUS` for sphere size
- Atmosphere effect at `EARTH_ATMOSPHERE_RADIUS`
- Sun angle derived from player `yRot` (normalized via `wrapDegrees`)

### 4.2 MoonRenderer
- Renders Moon sphere at `(MOON_DISTANCE, 0, 0)` (12000, 0, 0)
- Uses `MOON_RENDER_RADIUS` for sphere size
- No atmosphere (Moon has none)

### 4.3 NavigationHUDRenderer
- Renders directional arrows pointing to Earth/Moon
- Active when distance > `HUD_HIDE_DISTANCE`
- Hidden when 3D sphere provides sufficient visual guidance

### 4.4 SpaceSkyRenderer
- Renders star field background
- Always active regardless of distance
- Pure black base with procedural star distribution

---

## 5. Key Constants Reference

All rendering dimensions derive from `SpaceConstants.EARTH_RADIUS`:

```java
EARTH_RENDER_RADIUS       = EARTH_RADIUS;              // 200.0f
EARTH_ATMOSPHERE_RADIUS   = EARTH_RADIUS * 1.2f;       // 240.0f
MOON_RADIUS               = EARTH_RADIUS * 0.25f;      // 50.0f
MOON_RENDER_RADIUS        = MOON_RADIUS;               // 50.0f
HUD_HIDE_DISTANCE         = EARTH_RADIUS * 3.0f;       // 600.0f
MOON_DISTANCE             = 12000.0f;                   // Fixed position
```

---

## 6. Known Issues & Decisions

| Issue | Decision | Date |
|:---|:---|:---|
| Sun angle jump at 360deg | `wrapDegrees` normalization (P1-6) | 2026-05-20 |
| Gimbal lock in 6DoF | Cache `lastRight` vector, fallback on degeneration | 2026-05-12 |
| FOV flicker from sprinting | Three-layer defense: PlayerTick + RenderTick + FOV event | 2026-05-12 |
| Collision handling | Velocity scaled to 0.3 on wall impact | 2026-05-11 |
