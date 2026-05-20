# Dimension Transition Flow

> Version: 1.0 | Date: 2026-05-20 | Source: P0 refactoring + devlog_2026-05-18

---

## 1. Overview

Five transition points form a complete round-trip loop:

```
Earth --(enter)--> Space --(moon)--> Moon --(escape)--> Space --(earth)--> Earth
       |                                                              |
       +<-------------------- (return) <-------------------------------+
```

---

## 2. Transition Matrix

| # | From | To | Trigger | Packet | Anchor Action |
|:---:|:---:|:---:|:---|:---|:---|
| 1 | Earth | Space | `/cvspace enter` or y>=300 | `LaunchToSpacePacket` | Save Earth anchor (setEarthAnchor) |
| 2 | Space | Moon | Distance < 200 to Moon | `MoonTransitionPacket` | None |
| 3 | Moon | Space | y >= 200 on Moon | `ReturnToSpacePacket` | Save Moon anchor (setMoonAnchor) |
| 4 | Space | Earth | Distance < 800 to Earth | `LandOnEarthPacket` | Read Earth anchor (getEarthAnchor) |
| 5 | Any | Earth anchor | `/cvspace return` | (direct teleport) | Read Earth anchor |

---

## 3. Coordinate Constants

All entry/exit coordinates are derived from `SpaceConstants`:

| Point | Coordinate | Derivation |
|:---|:---|:---|
| Space entry (from Earth) | `(0, 400, -1200)` | `(0, EARTH_RADIUS+200, -(EARTH_RADIUS+TRIGGER_LAND_EARTH+200))` |
| Moon entry (from Space) | `(12000, 150, 0)` | `(MOON_DISTANCE, MOON_RADIUS+100, 0)` |
| Space re-entry (from Moon) | `(0, 400, -1200)` | Same as entry |
| Earth return (from Space) | Earth anchor position | From SpaceData.getEarthAnchor() |

**Hard constraint**: All coordinates must satisfy `distanceTo(Earth) > TRIGGER_LAND_EARTH` to prevent teleport loops.

---

## 4. Anchor System (SpaceData)

### 4.1 Dual Anchor Storage

```java
// Earth anchor — set when leaving Earth, read when returning
SpaceData.setEarthAnchor(x, y, z)   // LaunchToSpacePacket
SpaceData.getEarthAnchor()            // LandOnEarthPacket, CvSpaceCommand return

// Moon anchor — set when leaving Moon, read when returning (future)  
SpaceData.setMoonAnchor(x, y, z)    // ReturnToSpacePacket
SpaceData.getMoonAnchor()             // (future Moon return feature)
```

### 4.2 NBT Version Migration

- v1 (old): Single `anchorX/Y/Z + hasAnchor` — auto-migrated to Earth anchor
- v2 (current): Separate `earthAnchor` + `moonAnchor` with `DATA_VERSION = 2`

---

## 5. State Tracking (SpaceState)

```java
// Memory-only tracking (not persisted)
SpaceState.enterSpace(uuid)   // LaunchToSpacePacket handle
SpaceState.exitSpace(uuid)    // LandOnEarthPacket handle
SpaceState.isInSpace(uuid)    // Used by commands/HUD

// Auto-cleanup on disconnect
@SubscribeEvent
onPlayerLoggedOut -> IN_SPACE.remove(uuid)  // Prevents memory leak
```

---

## 6. Code Architecture

### 6.1 Packet Organization (P0-5)

```
network/
  CosmicVoyagePacketHandler.java    # SimpleChannel registration only (~60 lines)
  packet/
    ShipSyncPacket.java              # Position/velocity sync
    MoonTransitionPacket.java        # Space -> Moon
    LaunchToSpacePacket.java         # Earth -> Space
    LandOnEarthPacket.java           # Space -> Earth
    ReturnToSpacePacket.java         # Moon -> Space
```

### 6.2 Trigger Architecture

```
TransitionHandler (client-side)
  onClientTick
    if SPACE dimension:
      handleSpaceDimension()
        - Check Moon distance -> MoonTransitionPacket
        - Check Earth distance -> LandOnEarthPacket
    if MOON dimension:
      handleMoonDimension()
        - Check y >= 200 -> ReturnToSpacePacket
```

**Key design**: All distance checks use `mc.player.distanceToSqr()` — no ship requirement (P1 fix).

---

## 7. Testing Checklist

| Test | Command / Action | Expected Result |
|:---|:---|:---|
| Earth -> Space | `/cvspace enter` | Position (0, 400, -1200) |
| Auto-launch | Fly to y>=300 in Overworld | Same as above |
| Space -> Moon | Fly toward Moon | Position (12000, 150, 0) |
| Moon -> Space | Fly to y>=200 on Moon | Position (0, 400, -1200) |
| Space -> Earth | Fly toward Earth | Earth anchor position |
| Earth anchor | `/cvspace return` | Earth anchor position |
| No loop | Check after any transition | No immediate re-transition |
