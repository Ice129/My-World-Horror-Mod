# Blueice129 Entity Goal System Architecture

## Overview

This document explains the goal system architecture for the Blueice129 entity, a state-based AI system that allows for complex, modular behaviors.

---

## How Minecraft Goals Work

### Basic Concept
Goals (also called AI tasks) are Minecraft's way of controlling entity behavior. Each goal represents a specific behavior or action an entity can perform.

### Key Properties of Goals:

1. **Priority System**
   - Goals are assigned integer priorities (0-100+)
   - Lower numbers = higher priority
   - Priority 0 is the highest priority
   - Example: Swimming (priority 0) should always override wandering (priority 5)

2. **Control Flags**
   Goals can set control flags to prevent conflicts:
   - `MOVE`: Controls entity movement/pathfinding
   - `LOOK`: Controls where the entity looks
   - `JUMP`: Controls jumping behavior
   
   Goals with overlapping control flags will not run simultaneously. The higher priority goal takes precedence.

3. **Goal Lifecycle**
   ```
   canStart() → start() → tick() (repeated) → shouldContinue() → stop()
   ```
   - `canStart()`: Checked every tick, returns true when goal should begin
   - `start()`: Called once when goal activates
   - `tick()`: Called every game tick while goal is active
   - `shouldContinue()`: Checked every tick, returns false to stop goal
   - `stop()`: Called once when goal deactivates

4. **Multiple Active Goals**
   - YES, multiple goals CAN run simultaneously!
   - Goals only conflict if they use the same control flags
   - Example: A "LookAtPlayer" goal (LOOK) and "Wander" goal (MOVE) can both run at once
   - Example: Two goals both using MOVE flag will conflict - only the higher priority one runs

---

## Our Goal System Architecture

### Design Philosophy

We use a **State-Based Goal Profile System** where each entity state has a predefined set of goals that activate together. This provides:
- Clear separation of behaviors
- Easy state transitions
- Modular, reusable goals
- Scalable architecture for complex behaviors

### Core Components

#### 1. `BaseBlueice129Goal` (Abstract Base Class)
**Location:** `horror.blueice129.entity.goals.BaseBlueice129Goal`

All custom goals extend this base class, which provides:
- Access to the Blueice129Entity instance
- Simplified lifecycle methods
- State checking utilities

**Key Methods:**
```java
protected abstract boolean shouldStart();      // When should goal begin?
protected boolean shouldKeepRunning();         // When should goal continue?
protected void onStart();                      // Initialize when starting
protected void onStop();                       // Cleanup when stopping
public abstract void tick();                   // What to do each tick
protected boolean isInState(EntityState);      // Check current state
```

#### 2. `GoalProfile` (Goal Container)
**Location:** `horror.blueice129.entity.goals.GoalProfile`

A profile is a collection of goals with their priorities for a specific state.

**Usage:**
```java
GoalProfile profile = GoalProfile.create()
    .addGoal(0, new SwimGoal(entity))           // Priority 0 (highest)
    .addGoal(1, new FleeGoal(entity))           // Priority 1
    .addGoal(2, new LookAroundGoal(entity));    // Priority 2 (lowest)
```

#### 3. `GoalProfileRegistry` (State Manager)
**Location:** `horror.blueice129.entity.goals.GoalProfileRegistry`

This is the brain of the system. It:
- Maps each EntityState to a GoalProfile
- Handles goal switching when state changes
- Manages goal activation/deactivation

**How It Works:**
1. When entity changes state, `setState()` is called
2. `updateGoals()` is triggered
3. Registry clears all current goals
4. Registry adds all goals from the new state's profile
5. Goals automatically start running based on their conditions

---

## Goal Categories

Goals are organized into packages by behavior type:

### 1. Basic Goals (`horror.blueice129.entity.goals.basic`)
Fundamental behaviors used across multiple states:
- **SwimGoal**: Keep entity afloat in water/lava
- **LookAtPlayerGoal**: Look at nearby players
- **LookAroundGoal**: Random head movements
- **WanderGoal**: Random pathfinding movement

### 2. Animation Goals (`horror.blueice129.entity.goals.animation`)
Visual/cosmetic behaviors:
- **ErraticHeadMovementGoal**: Rapid head shaking (panic)
- **RandomCrouchGoal**: Random crouch toggles
- **HotbarCycleGoal**: Simulate hotbar scrolling
- **AlwaysCrouchGoal**: Stay crouched
- **PeekGoal**: Brief looking out from hiding
- **StandStillGoal**: Freeze all movement

### 3. Movement Goals (`horror.blueice129.entity.goals.movement`)
Complex pathfinding and positioning:
- **HideBehindStructuresGoal**: Find and hide behind obstacles
- **StayUndergroundGoal**: Maintain underground position
- **FollowPlayerUndergroundGoal**: Follow while staying underground

### 4. Interaction Goals (`horror.blueice129.entity.goals.interaction`)
World interaction behaviors:
- **MimicPlayerMiningGoal**: Copy player mining actions
- **OpenChestsGoal**: Find and open chests
- **OpenDoorsGoal**: Interact with doors
- **PressButtonsGoal**: Press buttons
- **FlipLeversGoal**: Toggle levers

### 5. Building Goals (`horror.blueice129.entity.goals.building`)
Block manipulation:
- **PlaceBlocksGoal**: Place blocks from inventory
- **BreakBlocksGoal**: Mine blocks with tools
- **ClearAreaGoal**: Systematically clear regions

---

## State Profiles

Each EntityState has a pre-configured goal profile:

### PASSIVE State
```
Priority 0: SwimGoal (always stay afloat)
Priority 1: LookAtPlayerGoal (occasionally look at player)
Priority 2: WanderGoal (wander randomly)
Priority 3: LookAroundGoal (look around)
```
**Behavior:** Peaceful, idle wandering

### PANICED State
```
Priority 0: SwimGoal
Priority 1: PanicedGoal (flee from player)
Priority 2: ErraticHeadMovementGoal (shake head)
Priority 3: RandomCrouchGoal (crouch randomly)
Priority 4: HotbarCycleGoal (cycle hotbar)
```
**Behavior:** Frantic fleeing with erratic movements

### SURFACE_HIDING State
```
Priority 0: SwimGoal
Priority 1: HideBehindStructuresGoal (pathfind behind obstacles)
Priority 2: AlwaysCrouchGoal (stay crouched)
Priority 3: PeekGoal (occasionally peek out)
```
**Behavior:** Stealthy hiding behind structures

### UNDERGROUND_BURROWING State
```
Priority 0: SwimGoal
Priority 1: StayUndergroundGoal (maintain underground position)
Priority 2: FollowPlayerUndergroundGoal (follow player underground)
Priority 3: MimicPlayerMiningGoal (copy player mining)
Priority 4: AlwaysCrouchGoal (stay crouched)
```
**Behavior:** Underground stalking and mimicry

### IN_MENUS State
```
Priority 0: SwimGoal
Priority 1: StandStillGoal (freeze completely)
```
**Behavior:** Complete stillness (simulating menu interaction)

### INVESTIGATING State
```
Priority 0: SwimGoal
Priority 1: OpenChestsGoal (find and open chests)
Priority 2: OpenDoorsGoal (interact with doors)
Priority 3: PressButtonsGoal (press buttons)
Priority 4: FlipLeversGoal (toggle levers)
Priority 5: WanderGoal (explore)
Priority 6: LookAroundGoal (look around)
```
**Behavior:** Curious exploration of environment

### UPGRADING_HOUSE State
```
Priority 0: SwimGoal
Priority 1: PlaceBlocksGoal (place blocks)
Priority 2: BreakBlocksGoal (mine blocks)
Priority 3: ClearAreaGoal (clear areas)
Priority 4: WanderGoal (move around)
```
**Behavior:** Building and terraforming

---

## Creating New Goals

### Step 1: Extend BaseBlueice129Goal

```java
package horror.blueice129.entity.goals.category;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import net.minecraft.entity.ai.goal.Goal;
import java.util.EnumSet;

public class MyCustomGoal extends BaseBlueice129Goal {
    
    // Constructor
    public MyCustomGoal(Blueice129Entity entity) {
        super(entity, 10); // Check every 10 ticks
        // Set control flags to prevent conflicts
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }
    
    // When should this goal start?
    @Override
    protected boolean shouldStart() {
        // Example: Start if in specific state and random chance
        return isInState(EntityState.INVESTIGATING) && 
               entity.getRandom().nextFloat() < 0.1F;
    }
    
    // When should this goal continue running?
    @Override
    protected boolean shouldKeepRunning() {
        // Continue while condition is true
        return isInState(EntityState.INVESTIGATING);
    }
    
    // Initialize when goal starts
    @Override
    protected void onStart() {
        // Setup code here
    }
    
    // What happens each tick while active?
    @Override
    public void tick() {
        // Action code here
        // Examples:
        // - entity.getNavigation().startMovingTo(x, y, z, speed)
        // - entity.getLookControl().lookAt(x, y, z)
        // - entity.getJumpControl().setActive()
    }
    
    // Cleanup when goal stops
    @Override
    protected void onStop() {
        // Cleanup code here
    }
}
```

### Step 2: Add to Goal Profile

Edit `GoalProfileRegistry.java`:

```java
private GoalProfile createMyStateProfile() {
    return GoalProfile.create()
        .addGoal(0, new SwimGoal(entity))
        .addGoal(1, new MyCustomGoal(entity))  // Add your goal
        .addGoal(2, new WanderGoal(entity, 1.0));
}
```

### Step 3: Register Profile

```java
private void initializeProfiles() {
    // ... existing profiles ...
    registerProfile(EntityState.MY_STATE, createMyStateProfile());
}
```

---

## Complex Goal Patterns

### Pattern 1: Pathfinding to Target

```java
@Override
public void tick() {
    BlockPos target = findTarget();
    if (target != null) {
        entity.getNavigation().startMovingTo(
            target.getX(), 
            target.getY(), 
            target.getZ(), 
            1.0 // speed
        );
    }
}
```

### Pattern 2: Timed Actions

```java
private int actionTimer = 0;

@Override
protected void onStart() {
    actionTimer = 60; // 3 seconds
}

@Override
public void tick() {
    actionTimer--;
    if (actionTimer == 0) {
        performAction();
    }
}

@Override
protected boolean shouldKeepRunning() {
    return actionTimer > 0;
}
```

### Pattern 3: State Machine within Goal

```java
private enum GoalState { SEARCHING, APPROACHING, ACTING }
private GoalState goalState = GoalState.SEARCHING;

@Override
public void tick() {
    switch (goalState) {
        case SEARCHING:
            if (findTarget()) {
                goalState = GoalState.APPROACHING;
            }
            break;
        case APPROACHING:
            if (reachedTarget()) {
                goalState = GoalState.ACTING;
            }
            break;
        case ACTING:
            performAction();
            break;
    }
}
```

### Pattern 4: Block Placement

```java
@Override
public void tick() {
    BlockPos targetPos = findPlacementPosition();
    BlockState blockToPlace = getBlockFromInventory();
    
    if (targetPos != null && blockToPlace != null) {
        entity.getWorld().setBlockState(targetPos, blockToPlace);
        // Play sound, animate, etc.
    }
}
```

### Pattern 5: Block Breaking

```java
private BlockPos breakingPos = null;
private int breakProgress = 0;

@Override
public void tick() {
    if (breakingPos == null) {
        breakingPos = findBlockToBreak();
        breakProgress = 0;
    } else {
        breakProgress++;
        
        if (breakProgress >= getBreakTime(breakingPos)) {
            entity.getWorld().breakBlock(breakingPos, true);
            breakingPos = null;
        }
    }
}
```

---

## Performance Considerations

### 1. Cache Expensive Calculations
```java
private PlayerEntity cachedPlayer = null;
private int cacheTimer = 0;

@Override
protected boolean shouldStart() {
    if (cacheTimer-- <= 0) {
        cachedPlayer = entity.getWorld().getClosestPlayer(entity, 64.0);
        cacheTimer = 20; // Refresh every second
    }
    return cachedPlayer != null;
}
```

### 3. Early Exit Conditions
```java
@Override
public void tick() {
    // Check cheapest conditions first
    if (!isInState(EntityState.INVESTIGATING)) return;
    if (entity.getRandom().nextFloat() > 0.1F) return;
    
    // Expensive operations only if needed
    BlockPos target = expensivePathfinding();
    // ...
}
```

---

## Debugging Goals

### Add Debug Logging

```java
@Override
protected void onStart() {
    System.out.println("[Blueice129] Starting goal: " + this.getClass().getSimpleName());
}

@Override
public void tick() {
    if (entity.age % 20 == 0) { // Log every second
        System.out.println("[Blueice129] Goal active: " + this.getClass().getSimpleName());
    }
}
```

### Check Active Goals

In the entity's tick method:
```java
@Override
public void tick() {
    super.tick();
    
    if (this.age % 100 == 0) { // Every 5 seconds
        System.out.println("Active goals:");
        this.goalSelector.getRunningGoals().forEach(goal -> 
            System.out.println("  - " + goal.getGoal().getClass().getSimpleName())
        );
    }
}
```

---

## Future Expansion

### Adding New States

1. Add state to enum in `Blueice129Entity.java`
2. Create goal profile method in `GoalProfileRegistry.java`
3. Register profile in `initializeProfiles()`
4. Add state transition logic in entity's `tick()` method

### Adding New Goal Categories

1. Create new package: `horror.blueice129.entity.goals.newcategory`
2. Create goals extending `BaseBlueice129Goal`
3. Add goals to relevant state profiles

### Helper Classes

For complex operations, create helper classes:
- `PathfindingHelper`: Advanced pathfinding utilities
- `BlockSearcher`: Find specific blocks in area
- `InventoryManager`: Manage entity inventory
- `AnimationController`: Complex animation sequences

Example:
```java
public class PathfindingHelper {
    public static BlockPos findNearestBlock(Entity entity, Block targetBlock, int radius) {
        // Complex search logic
    }
    
    public static boolean pathfindToBlock(MobEntity entity, BlockPos target, double speed) {
        // Pathfinding logic
    }
}
```

---

## Summary

**Key Takeaways:**
1. Goals are modular, reusable behaviors with priorities
2. Multiple goals can run simultaneously if they don't conflict
3. State-based profiles make complex behaviors manageable
4. BaseBlueice129Goal simplifies goal creation
5. Keep shouldStart() checks lightweight and cache expensive calculations
6. System is expandable - add new goals/states easily

**To Add a New Behavior:**
1. Create a goal class extending `BaseBlueice129Goal`
2. Implement `shouldStart()`, `tick()`, and optional lifecycle methods
3. Add goal to appropriate state profile(s)
4. Test and iterate!

This architecture provides a solid foundation for creating complex, believable entity behaviors while maintaining clean, maintainable code.
