# 🏗️ Blame Mod - Netsphere Dimension

> A Minecraft dimension inspired by Tsutomu Nihei's *BLAME!* manga, featuring an endless procedurally-generated megastructure with canyon architecture, multi-level floors, and interconnected navigation systems.

---

## 🎯 Overview

**Blame Mod** adds the **Netsphere** dimension to Minecraft - a vast artificial canyon structure that extends infinitely. Navigate through multiple floor levels using ladders and ramps while exploring the architectural details carved into the canyon walls.

### Key Features
- **Procedurally generated canyon** with smoothly varying centerline
- **Three distinct floor types** with different materials and characteristics
- **Vertical navigation** via ladders breaking through floors
- **Inter-floor ramps** built into canyon walls
- **Architectural facades** with carved arch windows
- **Deterministic generation** - same seed always produces the same structure

---

## 🌍 The Netsphere Dimension

### Canyon Structure
The canyon is **160 blocks wide** (±80 from center) and extends infinitely in the Z direction. The canyon centerline smoothly shifts horizontally, creating a winding path through the megastructure.

### Floor Types

The dimension features **three procedurally-selected floor types** that repeat every 14 blocks vertically:

#### **Type 0: Clean Slab Floors**
- Material: **Smooth Stone Slabs**
- Thickness: **1 block**
- Characteristics: Thin, elegant platforms
- Facade density: **High** (40% arch coverage)

#### **Type 1: Industrial Floors**
- Material: **Polished Andesite** with slab tops
- Thickness: **2-3 blocks** (varies by position)
- Characteristics: Sturdy, heavy construction
- Facade density: **Moderate** (50% threshold)

#### **Type 2: Broken Floors**
- Material: **Light Gray Concrete**
- Thickness: **1 block**
- Characteristics: Damaged, with 15% edge erosion
- Facade density: **Rare** (75% threshold)

### Navigation Systems

#### **Ladders**
- Spawn probability: **0.2%** per column
- Placement: Attached to canyon walls (1 block from wall)
- Height: Extend from floor to floor, breaking through floor thickness
- Direction: Face away from wall for easy climbing

#### **Ramps**
- Frequency: **100%** per floor per chunk
- Width: **7 blocks** (Z direction)
- Depth: **3 blocks** into wall
- Material: **Stone Stairs**
- Rise: 1 block per 2 horizontal blocks

### Architectural Details

#### **Facade Arches**
- Grid spacing: **8 blocks horizontal × 12 blocks vertical**
- Arch size: **3 blocks wide × 6 blocks tall**
- Shape: Rectangular base with semicircular top
- Depth: **2 blocks** into canyon wall
- Placement: Noise-based with floor-type variation

---

## 🛠️ Technical Details

### Generation Parameters
- **Canyon half-width**: 80 blocks
- **Floor spacing**: 14 blocks vertically
- **Floor ledge**: Extends 10 blocks into canyon
- **Anchor step**: 128 blocks (controls canyon curvature smoothness)
- **Amplitude**: ±80 blocks (horizontal drift range)

### Noise Systems
- **Floor placement**: Value noise with 35% threshold
- **Edge erosion**: 15% probability on outermost ledge blocks
- **Facade carving**: Varies by floor type (40%-75%)
- **Floor type selection**: Deterministic hash per floor index

### Commands
- `/canyon_info` - Displays your position relative to the canyon center

---

## 💻 Development

### Technical Stack
- **Minecraft Version**: 1.21.x
- **Mod Loader**: Forge
- **Java Version**: 21
- **Mod ID**: `blamemod`

### Project Structure
```
src/main/java/net/liferquest/blamemod/
├── worldgen/
│   ├── NetsphereChunkPostProcessor.java  # Main terrain generation
│   ├── ModDimensions.java                # Dimension registration
│   └── ModLevelStems.java                # Level stem configuration
├── command/
│   └── CanyonInfoCommand.java            # Debug command
└── BlameMod.java                         # Main mod class
```

### Key Algorithms
- **Smoothstep interpolation** for canyon centerline variation
- **MurmurHash3-style mixing** for deterministic randomization
- **Grid-aligned positioning** for facade arch placement
- **Distance field calculations** for arch shapes

---

## 🎮 Getting Started

1. **Build the mod** using Gradle:
   ```bash
   gradlew build
   ```

2. **Run the client**:
   ```bash
   gradlew runClient
   ```

3. **Access the Netsphere**: Use the portal system (implementation pending) or use creative mode to change dimensions

4. **Navigate**: Look for ladders on walls and ramps in the structure to move between floors

5. **Explore**: The canyon extends infinitely - travel along the Z axis to see the canyon wind through the megastructure

---

## 📝 License

See [LICENSE.txt](LICENSE.txt) for details.

---

## 🙏 Credits

Inspired by Tsutomu Nihei's *BLAME!* manga and the concept of an endless megastructure.