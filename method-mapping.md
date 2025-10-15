# Method Mapping from HorrorModPersistentState to StateSaverAndLoader

## Basic Methods

| HorrorModPersistentState Method | StateSaverAndLoader Equivalent | Notes |
|--------------------------------|-------------------------------|-------|
| `getServerState(server)` | `getServerState(server)` | Direct equivalent |

## Integer Values

| HorrorModPersistentState Method | StateSaverAndLoader Equivalent | Notes |
|--------------------------------|-------------------------------|-------|
| `getIntValue(id, defaultValue)` | Use `intValues` map directly | StateSaverAndLoader doesn't have a direct method |
| `setIntValue(id, value)` | Use `intValues` map directly | StateSaverAndLoader doesn't have a direct method |
| `removeIntValue(id)` | Use `intValues` map directly | StateSaverAndLoader doesn't have a direct method |
| `hasIntValue(id)` | Use `intValues` map directly | StateSaverAndLoader doesn't have a direct method |
| `getIntValueIds()` | Use `intValues.keySet()` directly | StateSaverAndLoader doesn't have a direct method |

## Block Positions

| HorrorModPersistentState Method | StateSaverAndLoader Equivalent | Notes |
|--------------------------------|-------------------------------|-------|
| `getPosition(id)` | Use `blockPositions` map directly | StateSaverAndLoader doesn't have a direct method |
| `setPosition(id, pos)` | Use `blockPositions` map directly | StateSaverAndLoader doesn't have a direct method |
| `removePosition(id)` | Use `blockPositions` map directly | StateSaverAndLoader doesn't have a direct method |
| `hasPosition(id)` | Use `blockPositions.containsKey(id)` | StateSaverAndLoader doesn't have a direct method |
| `getPositionIds()` | Use `blockPositions.keySet()` | StateSaverAndLoader doesn't have a direct method |

## Timers

| HorrorModPersistentState Method | StateSaverAndLoader Equivalent | Notes |
|--------------------------------|-------------------------------|-------|
| `getTimer(timerId)` | Use `intValues` map | Will need to use a prefix like "timer_" + timerId |
| `setTimer(timerId, value)` | Use `intValues` map | Will need to use a prefix like "timer_" + timerId |
| `incrementTimer(timerId, amount)` | Manual implementation | Get, increment, set using intValues |
| `decrementTimer(timerId, amount)` | Manual implementation | Get, decrement, set using intValues |
| `hasTimer(timerId)` | Check with prefix | `intValues.containsKey("timer_" + timerId)` |
| `removeTimer(timerId)` | Remove with prefix | `intValues.remove("timer_" + timerId)` |
| `getTimerIds()` | Filter intValues keys | Filter keys that start with "timer_" |

## Position Lists

| HorrorModPersistentState Method | StateSaverAndLoader Equivalent | Notes |
|--------------------------------|-------------------------------|-------|
| `getPositionList(id)` | Use StripMineBlocks | Need to convert position list to StripMineBlocks |
| `setPositionList(id, positions)` | Use StripMineBlocks | Need to convert position list to StripMineBlocks |
| `addPositionToList(id, pos)` | No direct equivalent | Need significant refactoring |
| `removePositionFromList(id, pos)` | No direct equivalent | Need significant refactoring |
| `removePositionList(id)` | Remove StripMineBlocks | Use `removeStripMineBlocks(id)` |
| `getPositionListIds()` | Get StripMineBlocks IDs | Use `getStripMineBlocksIds()` |

## 2D Integer Arrays

| HorrorModPersistentState Method | StateSaverAndLoader Equivalent | Notes |
|--------------------------------|-------------------------------|-------|
| `getInt2DArray(id)` | No direct equivalent | Need significant refactoring |
| `setInt2DArray(id, array)` | No direct equivalent | Need significant refactoring |
| `hasInt2DArray(id)` | No direct equivalent | Need significant refactoring |
| `removeInt2DArray(id)` | No direct equivalent | Need significant refactoring |
| `getInt2DArrayIds()` | No direct equivalent | Need significant refactoring |

## StripMineBlocks 

| HorrorModPersistentState Method | StateSaverAndLoader Equivalent | Notes |
|--------------------------------|-------------------------------|-------|
| No direct equivalent | `getStripMineBlocks(id)` | New functionality |
| No direct equivalent | `setStripMineBlocks(id, blocks)` | New functionality |
| No direct equivalent | `removeStripMineBlocks(id)` | New functionality |
| No direct equivalent | `hasStripMineBlocks(id)` | New functionality |
| No direct equivalent | `getStripMineBlocksIds()` | New functionality |