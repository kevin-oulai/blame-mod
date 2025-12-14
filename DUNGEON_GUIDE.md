# Blame Mod - BLAME! Megastructure World Generator

## Overview
This mod creates an infinite, nightmarish megastructure inspired by Tsutomu Nihei's BLAME! manga. Experience a procedurally generated world of incomprehensible scale, built by ancient machines with no regard for human comfort or logic.

## Core Features

### Stratas System (Vertical Layering)
The world is divided into vertical layers of varying heights, each with distinct characteristics:

- **Micro-stratas** (10-20 blocks): Cramped maintenance tunnels and vent shafts
- **Standard stratas** (50-100 blocks): "Normal" habitation zones with malfunctioning human spaces
- **Industrial stratas** (100-200 blocks): Massive machinery and pipe networks
- **Mega-stratas** (200-500 blocks): Cathedral-like spaces with minimal structures
- **Void stratas** (1000+ blocks): Pure emptiness with occasional single structures

### Uncanny Architecture
Reality-breaking structures that create unease:

- **Doors to nowhere**: Doorways opening directly into the void
- **Staircases to oblivion**: Stairs that end at walls or empty space
- **Windows without rooms**: Glass panes facing solid walls
- **Upside-down spaces**: Rooms with furniture on ceilings
- **Impossible geometry**: Spaces that shouldn't fit but do
- **Repetitive errors**: Same malformed room generated hundreds of times
- **Scale glitches**: Doors ranging from 1 to 20 blocks tall

### Robot-Built Human Spaces (Malfunctioning)
Spaces intended for humans, but wrong:

- **Beds in walls**: Furniture embedded impossibly
- **Sealed amenities**: Furnaces and crafting tables behind glass
- **Flooded habitations**: Water-filled rooms with intact furniture
- **Inverted furniture**: Chairs and tables on ceilings
- **Inaccessible doors**: Entrances 10 blocks up or in floors
- **Exposed facilities**: Bathrooms and private spaces in public corridors
- **Submerged archives**: Libraries and storage underwater

### Space Alternation
Extreme shifts between tiny and massive spaces:

- **Compression zones**: 2-block tunnels suddenly expanding to cathedrals
- **Expansion chambers**: Tiny doors leading to 100-block tall rooms
- **Nested paradoxes**: Giant rooms containing tiny sealed chambers
- **Claustrophobic spirals**: Narrow staircases extending 200+ blocks
- **The Abyss**: Chunks that are pure void with single catwalks

### Super Rare Megastructures (0.1-1% spawn)

**The Elevator Shaft**: 2000+ block vertical shaft with broken platforms every 100 blocks

**The Sphere**: Perfect hollow sphere (50-100 blocks diameter) with single entrance, empty interior

**The Factory Floor**: 500x500 area of moving pistons with no apparent purpose

**The Archive**: Endless corridors of chest rooms, mostly empty or containing junk

**The Greenhouse**: Massive glass dome (200 blocks) with dead vegetation

**The Grid**: Infinite 10x10x10 meter identical cubes, one random cube has exit

**The Server Room**: Walls of redstone components generating heat and noise

**The Recursion Chamber**: Room containing smaller versions of itself, nested infinitely

### Strata-Specific Biomes

**Living Stratas** (50-100 blocks):
- Apartments with wrongly-placed furniture
- Occasional skeleton remains
- Dim lighting from embedded sources
- Eerie silence

**Industrial Stratas** (100-200 blocks):
- Massive 5x5 block pipes everywhere
- Furnace arrays and heavy machinery
- Constant mechanical sounds
- Dangerous moving parts

**Server Stratas** (30-50 blocks):
- Walls of repeaters and comparators
- Excessive redstone wiring
- Heat sources behind barriers
- Electronic humming

**Agricultural Stratas** (80-150 blocks):
- Dead farmland stretching endlessly
- Broken irrigation systems
- Abandoned tools and storage
- Oppressive emptiness

**Void Stratas** (1000+ blocks):
- Occasional single pillar or platform
- Risk of terminal velocity falls
- Profound loneliness
- Navigation challenges

**Maintenance Stratas** (10-30 blocks):
- Cramped crawlspaces
- Extensive ladder networks
- Empty toolboxes
- Suffocating atmosphere

### Procedural Anomalies

- **The Glitch**: Overlapping duplicate chunks
- **The Void Room**: Room where floor is missing
- **The Mirror**: Structures duplicated and flipped
- **The Recursion**: Rooms containing smaller versions of themselves
- **The Breach**: Sudden transitions between incompatible stratas

### Navigation Features

- **Dead-end marathons**: 500+ block corridors ending in walls
- **False exits**: Signs indicating exits that lead nowhere
- **Loop corridors**: Paths returning to start
- **One-way passages**: Trapdoors and drops forcing forward movement
- **The Maze**: Pure labyrinth chunks

### Lighting Philosophy

- **Over-lit voids**: Bright illumination of emptiness
- **Dark habitations**: Living spaces without light
- **Blinking lights**: Random timed redstone lamps
- **Colored zones**: Red, blue, and green lighting districts
- **The Blackout**: Randomly occurring dark stratas

### Vertical Connectivity

- **The Shaft Network**: Vertical connections between stratas
- **Emergency ladders**: Extending 500+ blocks
- **Broken elevators**: Water elevators with missing sections
- **Gravity wells**: Soul sand columns for controlled descent
- **The Drop**: Holes through multiple stratas

### Current Implementation Status

✅ Massive vertical scale (Y=-64 to Y=320)
✅ Void-based generation
✅ Mega pillars and walls
✅ Sparse bridge network
✅ Ladder access systems
✅ Industrial details (pipes, chains, lanterns)
✅ No mob spawning
🔄 Stratas system (in progress)
🔄 Uncanny architecture (in progress)
🔄 Robot-malfunction spaces (in progress)
🔄 Super rare megastructures (planned)
🔄 Strata-specific biomes (planned)

## How to Access the Dungeon Dimension

### Method 1: Using Commands (Creative/Op)
1. Open the game in creative mode or as an operator
2. Use the command:
   ```
   /execute in blamemod:dungeon run tp @s ~ ~ ~
   ```

### Method 2: Create a Dungeon World
When creating a new world, you can select the dungeon dimension as the world type through datapacks.

## Technical Details

### Structure Pattern
```
[ ][ ][ ]  [ ][ ][ ]  [ ][ ][ ]
[ ][R][ ]  [ ][R][ ]  [ ][R][ ]  (R = Room, [ ] = Corridor)
[ ][ ][ ]  [ ][ ][ ]  [ ][ ][ ]
```

### Dimensions
- Floor Height: Y=50
- Room Ceiling: 8 blocks high
- Corridor Height: 5 blocks high
- Room Size: 16x16 blocks
- Corridor Width: 4 blocks

### Biome Settings
- No precipitation
- Dark atmosphere (sky color: #000000)
- Cave ambient sounds
- High monster spawn rates

## File Structure

### Java Files
- `DungeonChunkGenerator.java`: Main chunk generation logic
- `ModChunkGenerators.java`: Chunk generator registration
- `ModBiomes.java`: Biome definition
- `ModWorldGenProvider.java`: Data generation
- `DataGenerators.java`: Data generation event handler

### JSON Files
- `dimension_type/dungeon.json`: Dimension type configuration
- `dimension/dungeon.json`: Dimension configuration
- `worldgen/biome/dungeon.json`: Biome data

## Future Enhancements
- Custom mob spawners in rooms
- Treasure rooms with better loot
- Boss rooms every N chunks
- Different room themes (prison, library, armory)
- Traps and puzzles
- Custom structures (pillars, fountains, etc.)

## Development Notes
The chunk generator extends `ChunkGenerator` and implements:
- `buildSurface()`: Main generation method
- `generateRoom()`: Creates dungeon rooms with walls, floor, ceiling
- `generateCorridor()`: Creates connecting passages
- `addDungeonDecorations()`: Adds random decorative elements

All generation is deterministic based on chunk position and seed for consistent world generation.
