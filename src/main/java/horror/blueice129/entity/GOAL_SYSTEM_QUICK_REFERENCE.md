# Goal System Quick Reference

## Quick Start Guide

### Creating a New Goal (5 Steps)

1. **Create the goal class:**
```java
package horror.blueice129.entity.goals.category;

import horror.blueice129.entity.Blueice129Entity;
import horror.blueice129.entity.goals.BaseBlueice129Goal;
import net.minecraft.entity.ai.goal.Goal;
import java.util.EnumSet;

public class MyGoal extends BaseBlueice129Goal {
    
    public MyGoal(Blueice129Entity entity) {
        super(entity, 20); // Check every 20 ticks
        this.setControls(EnumSet.of(Goal.Control.MOVE)); // Set control flags
    }
    
    @Override
    protected boolean shouldStart() {
        // When should this goal activate?
        return isInState(EntityState.MY_STATE);
    }
    
    @Override
    public void tick() {
        // What should happen each tick?
    }
}
```

2. **Add import to GoalProfileRegistry.java:**
```java
import horror.blueice129.entity.goals.category.MyGoal;
```

3. **Add to state profile in GoalProfileRegistry.java:**
```java
private GoalProfile createMyStateProfile() {
    return GoalProfile.create()
        .addGoal(0, new SwimGoal(entity))
        .addGoal(1, new MyGoal(entity));  // Add your goal here
}
```

4. **Register the profile (if new state):**
```java
private void initializeProfiles() {
    // ...
    registerProfile(EntityState.MY_STATE, createMyStateProfile());
}
```

5. **Done!** The goal will automatically activate when the entity enters that state.

---

## Control Flags Reference

```java
// Choose one or more:
this.setControls(EnumSet.of(
    Goal.Control.MOVE,   // Controls pathfinding/movement
    Goal.Control.LOOK,   // Controls head/look direction
    Goal.Control.JUMP    // Controls jumping
));

// Multiple flags:
this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));

// No flags (can run with anything):
this.setControls(EnumSet.noneOf(Goal.Control.class));
```

---

## Common Goal Patterns

### Pathfinding to a Location
```java
@Override
public void tick() {
    BlockPos target = findTarget();
    if (target != null) {
        entity.getNavigation().startMovingTo(
            target.getX(), 
            target.getY(), 
            target.getZ(), 
            1.0  // speed multiplier
        );
    }
}
```

### Looking at Player
```java
private PlayerEntity targetPlayer;

@Override
protected void onStart() {
    targetPlayer = entity.getWorld().getClosestPlayer(entity, 64.0);
}

@Override
public void tick() {
    if (targetPlayer != null) {
        entity.getLookControl().lookAt(
            targetPlayer.getX(),
            targetPlayer.getEyeY(),
            targetPlayer.getZ()
        );
    }
}
```

### Fleeing from Player
```java
@Override
public void tick() {
    PlayerEntity player = entity.getWorld().getClosestPlayer(entity, 16.0);
    if (player != null) {
        double dx = entity.getX() - player.getX();
        double dz = entity.getZ() - player.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        
        if (distance > 0) {
            double fleeX = entity.getX() + (dx / distance) * 10.0;
            double fleeZ = entity.getZ() + (dz / distance) * 10.0;
            entity.getNavigation().startMovingTo(fleeX, entity.getY(), fleeZ, 1.5);
        }
    }
}
```

### Timed Action
```java
private int timer = 0;

@Override
protected void onStart() {
    timer = 100; // 5 seconds
}

@Override
protected boolean shouldKeepRunning() {
    return timer > 0;
}

@Override
public void tick() {
    timer--;
    if (timer == 50) {
        performHalfwayAction();
    }
    if (timer == 0) {
        performFinalAction();
    }
}
```

### Random Chance Activation
```java
@Override
protected boolean shouldStart() {
    return isInState(EntityState.INVESTIGATING) && 
           entity.getRandom().nextFloat() < 0.05F; // 5% chance per check
}
```

### Check if Navigation Reached Target
```java
@Override
public void tick() {
    if (entity.getNavigation().isIdle()) {
        // Reached destination or no path
        onReachedTarget();
    }
}
```

---

## Lifecycle Hook Reference

```java
// Called when goal should activate (checked every checkInterval ticks)
@Override
protected boolean shouldStart() {
    return true; // Return true to activate
}

// Called once when goal activates
@Override
protected void onStart() {
    // Initialize variables, find targets, etc.
}

// Called every tick while goal is active
@Override
public void tick() {
    // Main goal logic here
}

// Called every tick while active to check if should continue
@Override
protected boolean shouldKeepRunning() {
    return true; // Return false to stop goal
}

// Called once when goal deactivates
@Override
protected void onStop() {
    // Cleanup, stop navigation, etc.
}
```

---

## Entity Control Reference

### Movement/Navigation
```java
// Start moving to coordinates
entity.getNavigation().startMovingTo(x, y, z, speed);

// Stop moving
entity.getNavigation().stop();

// Check if moving
boolean isMoving = !entity.getNavigation().isIdle();

// Check if can reach position
boolean canReach = entity.getNavigation().findPathTo(blockPos, 10) != null;
```

### Looking
```java
// Look at coordinates
entity.getLookControl().lookAt(x, y, z);

// Look at entity
entity.getLookControl().lookAt(targetEntity);

// Look at entity's eyes
entity.getLookControl().lookAt(targetEntity.getX(), targetEntity.getEyeY(), targetEntity.getZ());
```

### Jumping
```java
// Make entity jump
entity.getJumpControl().setActive();
```

### Random Values
```java
// Random float 0.0 to 1.0
float f = entity.getRandom().nextFloat();

// Random int 0 to n-1
int i = entity.getRandom().nextInt(10); // 0-9

// Random boolean
boolean b = entity.getRandom().nextBoolean();
```

### State Checking
```java
// Check entity's state
if (isInState(EntityState.PANICED)) { }

// Get current state
EntityState state = getCurrentState();
```

### World Queries
```java
// Find closest player
PlayerEntity player = entity.getWorld().getClosestPlayer(entity, radius);

// Get block at position
BlockState block = entity.getWorld().getBlockState(blockPos);

// Check if sky is visible
boolean canSeeSky = entity.getWorld().isSkyVisible(blockPos);

// Check distance to entity
double distSquared = entity.squaredDistanceTo(otherEntity);
```

---

## Priority Guidelines

```
0     - Critical (swimming, avoid damage)
1-5   - Primary behavior (main goal for current state)
6-10  - Secondary behavior (supporting actions)
11-15 - Tertiary behavior (idle animations, looking)
20+   - Low priority (rare actions)
```

### Example Priority Scheme
```
0  - SwimGoal (always stay afloat)
1  - FleeGoal (run from danger)
2  - AttackGoal (fight back)
5  - FollowGoal (follow player)
8  - InteractGoal (use environment)
10 - WanderGoal (random movement)
15 - LookAtPlayerGoal (look at player)
20 - LookAroundGoal (random looking)
```

---

## Check Interval Guidelines

```
1  tick  - Needs instant response (combat, danger)
5  ticks - High frequency (0.25 seconds)
10 ticks - Medium frequency (0.5 seconds) 
20 ticks - Normal frequency (1 second) - RECOMMENDED DEFAULT
40 ticks - Low frequency (2 seconds)
100 ticks - Rare checks (5 seconds)
```

---

## Common Mistakes to Avoid

1. **❌ Don't forget control flags**
   ```java
   // BAD: No control flags set
   public MyGoal(Blueice129Entity entity) {
       super(entity, 20);
       // Missing: this.setControls(...)
   }
   
   // GOOD: Control flags specified
   public MyGoal(Blueice129Entity entity) {
       super(entity, 20);
       this.setControls(EnumSet.of(Goal.Control.MOVE));
   }
   ```

2. **❌ Don't use tick() for initialization**
   ```java
   // BAD: Initializing in tick()
   @Override
   public void tick() {
       if (target == null) {
           target = findTarget(); // Called every tick!
       }
   }
   
   // GOOD: Initialize in onStart()
   @Override
   protected void onStart() {
       target = findTarget(); // Called once
   }
   ```

3. **❌ Don't forget to stop navigation**
   ```java
   // BAD: Navigation continues after goal stops
   @Override
   protected void onStop() {
       // Missing: entity.getNavigation().stop();
   }
   
   // GOOD: Clean up navigation
   @Override
   protected void onStop() {
       entity.getNavigation().stop();
   }
   ```

4. **❌ Don't check expensive conditions every tick**
   ```java
   // BAD: Expensive check every tick
   public MyGoal(Blueice129Entity entity) {
       super(entity, 1); // Checks every tick
   }
   
   // GOOD: Use appropriate interval
   public MyGoal(Blueice129Entity entity) {
       super(entity, 20); // Checks every second
   }
   ```

---

## Testing Your Goal

1. **Add debug logging:**
```java
@Override
protected void onStart() {
    System.out.println("[MyGoal] Started!");
}

@Override
public void tick() {
    if (entity.age % 20 == 0) { // Every second
        System.out.println("[MyGoal] Still running...");
    }
}

@Override
protected void onStop() {
    System.out.println("[MyGoal] Stopped!");
}
```

2. **Test state transitions:**
   - Use `/debug` command to change entity state
   - Verify goals activate/deactivate correctly
   - Check for conflicting control flags

3. **Monitor performance:**
   - Check tick time (should be <1ms per goal)
   - Reduce check intervals if needed
   - Cache expensive calculations

---

## File Structure Reference

```
horror.blueice129.entity.goals/
├── BaseBlueice129Goal.java          # Base class (extend this)
├── GoalProfile.java                 # Goal container
├── GoalProfileRegistry.java         # State manager (edit this)
├── PanicedGoal.java                 # Example goal
├── basic/
│   ├── SwimGoal.java
│   ├── LookAtPlayerGoal.java
│   ├── WanderGoal.java
│   └── LookAroundGoal.java
├── animation/
│   ├── ErraticHeadMovementGoal.java
│   ├── RandomCrouchGoal.java
│   ├── HotbarCycleGoal.java
│   ├── AlwaysCrouchGoal.java
│   ├── PeekGoal.java
│   └── StandStillGoal.java
├── movement/
│   ├── HideBehindStructuresGoal.java
│   ├── StayUndergroundGoal.java
│   └── FollowPlayerUndergroundGoal.java
├── interaction/
│   ├── MimicPlayerMiningGoal.java
│   ├── OpenChestsGoal.java
│   ├── OpenDoorsGoal.java
│   ├── PressButtonsGoal.java
│   └── FlipLeversGoal.java
└── building/
    ├── PlaceBlocksGoal.java
    ├── BreakBlocksGoal.java
    └── ClearAreaGoal.java
```

---

## Need More Help?

See `GOAL_SYSTEM_ARCHITECTURE.md` for:
- Detailed explanations of how goals work
- Complex goal patterns
- Performance optimization tips
- Debugging strategies
- Architecture decisions and rationale
