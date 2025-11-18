# Blueice129 Entity State Debug Commands

## Overview
Use these commands to test and debug the Blueice129 entity's state-based behavior system.

## Command Syntax

### Set Entity State
```
/blueice129 state <state_name>
```

Changes the state of the nearest Blueice129 entity (within 64 blocks).

### Get Entity State
```
/blueice129 state get
```

Displays the current state of the nearest Blueice129 entity.

## Available States

| State Command | Description |
|--------------|-------------|
| `passive` | Default state - peaceful wandering, occasional player glances |
| `paniced` | Fleeing from player with erratic head movements and random crouching |
| `surface_hiding` | Hiding behind structures, crouched, occasional peeking |
| `underground_burrowing` | Following player underground while staying crouched |
| `in_menus` | Completely still, simulating menu interaction |
| `investigating` | Opening chests, doors, pressing buttons, flipping levers |
| `upgrading_house` | Placing and breaking blocks, clearing areas |

## Usage Examples

### Set entity to panic state:
```
/blueice129 state paniced
```
**Result:** Entity will flee from player with erratic movements

### Set entity to investigating state:
```
/blueice129 state investigating
```
**Result:** Entity will interact with environment (chests, doors, etc.)

### Check current state:
```
/blueice129 state get
```
**Result:** Displays: "Blueice129 entity state: 'passive' (12.5 blocks away)"

## Testing State Behaviors

### Test PASSIVE State
1. Spawn a Blueice129 entity
2. Run: `/blueice129 state passive`
3. **Observe:** Entity wanders randomly, occasionally looks at you

### Test PANICED State
1. Run: `/blueice129 state paniced`
2. **Observe:** Entity flees, head shakes, crouch toggles, random jumps

### Test SURFACE_HIDING State
1. Be near trees or structures
2. Run: `/blueice129 state surface_hiding`
3. **Observe:** Entity hides behind structures while crouched

### Test UNDERGROUND_BURROWING State
1. Go underground/in a cave
2. Run: `/blueice129 state underground_burrowing`
3. **Observe:** Entity follows you underground, mimics mining

### Test IN_MENUS State
1. Run: `/blueice129 state in_menus`
2. **Observe:** Entity freezes completely still

### Test INVESTIGATING State
1. Be near chests, doors, buttons, levers
2. Run: `/blueice129 state investigating`
3. **Observe:** Entity wanders and interacts with objects

### Test UPGRADING_HOUSE State
1. Be near a structure
2. Run: `/blueice129 state upgrading_house`
3. **Observe:** Entity places/breaks blocks

## Requirements

- **Permission Level:** Operator (level 2)
- **Range:** Entity must be within 64 blocks
- **Environment:** Works in both dedicated and integrated servers

## Error Messages

### "No Blueice129 entity found within 64 blocks"
**Solution:** Spawn a Blueice129 entity or move closer to an existing one

### "Invalid state name: [name]"
**Solution:** Check spelling - state names must be exact (all caps with underscores)

### "This command must be run by a player"
**Solution:** Run the command in-game, not from server console

## Goal System Integration

When you change the entity's state:
1. **Old goals are cleared** - All goals from previous state are removed
2. **New goals are applied** - Goal profile for new state is activated
3. **Behaviors change immediately** - Entity starts acting according to new state

Each state has its own goal profile with specific priorities:
- **Priority 0** - Critical (swimming)
- **Priority 1-5** - Primary behavior (state-specific main goals)
- **Priority 6-15** - Secondary behavior (supporting actions)
- **Priority 20+** - Low priority (ambient actions)

## Debugging Tips

### Watch Goal Activation
Set a state and observe which goals activate:
```
/blueice129 state paniced
```
Then watch for behaviors: fleeing, head shaking, crouching

### Compare States
Switch between states to see behavior differences:
```
/blueice129 state passive
(wait 10 seconds)
/blueice129 state investigating
```

### Test State Transitions
Once state transition logic is implemented in `tick()`, states will change automatically. Use `get` to monitor transitions:
```
/blueice129 state get
```

## Quick State Reference

### Movement States
- `passive` - Random wandering
- `paniced` - Fleeing
- `surface_hiding` - Hiding behind obstacles
- `underground_burrowing` - Underground following

### Action States
- `in_menus` - Frozen
- `investigating` - Interacting
- `upgrading_house` - Building

## Related Files

- **Goal Definitions:** `src/main/java/horror/blueice129/entity/goals/`
- **Profile Registry:** `GoalProfileRegistry.java`
- **Entity Logic:** `Blueice129Entity.java`
- **Command Handler:** `DebugCommands.java`

## Future Enhancements

### Coming Soon:
- Automatic state transitions based on game events
- State history tracking
- State-specific parameters (duration, intensity, etc.)
- Multiple entity state management
- State presets/macros

## See Also

- `GOAL_SYSTEM_ARCHITECTURE.md` - Complete goal system documentation
- `GOAL_SYSTEM_QUICK_REFERENCE.md` - Quick goal creation guide
