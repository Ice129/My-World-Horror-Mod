package horror.blueice129.feature;

import horror.blueice129.HorrorMod129;
import horror.blueice129.utils.LineOfSightUtils;
import horror.blueice129.utils.ChunkLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.block.entity.ChestBlockEntity;

import net.minecraft.util.math.random.Random;

public class HomeVisitorEvent {
    private static final Random random = Random.create();

    public static void triggerEvent(MinecraftServer server, ServerPlayerEntity player, BlockPos bedPos) {
        int itemsStolen = chestStealer(server, bedPos);
        int doorsOpened = doorOpener(server, player, bedPos);
        int signsPlaced = signPlacer(server, player, bedPos);
        int flowersPlanted = flowerPlanter(server, player, bedPos);
        int trapsSet = trapSetter(server, player, bedPos);
        boolean playerLeft = playerLeavesWorld(server);

        HorrorMod129.LOGGER.info("HomeVisitorEvent: Items stolen: " + itemsStolen + ", Doors opened: " + doorsOpened +
                ", Signs placed: " + signsPlaced + ", Flowers planted: " + flowersPlanted + ", Traps set: " + trapsSet +
                ", Player left: " + playerLeft);
    }

    private static int chestStealer(MinecraftServer server, BlockPos bedPos) {
        // list of wanted items, e.g., diamonds, gold, emeralds, enchanted items
        var wantedItems = java.util.Arrays.asList(
                Items.COAL, Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.EMERALD,
                Items.COOKED_BEEF, Items.COOKED_CHICKEN, Items.COOKED_PORKCHOP, Items.DIAMOND_AXE, Items.DIAMOND_SWORD,
                Items.DIAMOND_HORSE_ARMOR, Items.TNT, Items.ENDER_PEARL, Items.IRON_BLOCK, Items.GOLD_BLOCK,
                Items.DIAMOND_BLOCK,
                Items.ENCHANTED_GOLDEN_APPLE, Items.NETHERITE_INGOT, Items.NETHERITE_SCRAP, Items.MUSIC_DISC_11,
                Items.MUSIC_DISC_OTHERSIDE,
                Items.ENCHANTED_BOOK, Items.WHEAT, Items.BREAD, Items.CAKE, Items.GOLDEN_CARROT, Items.GOLDEN_APPLE,
                Items.HAY_BLOCK,
                Items.IRON_HORSE_ARMOR, Items.IRON_SWORD, Items.IRON_AXE, Items.IRON_PICKAXE, Items.IRON_SHOVEL,
                Items.IRON_HELMET,
                Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);

        int searchRadius = 20; // Search radius around the bed
        int itemsStolen = 0;
        int itemTypeStolen = 0;
        int maxItemsToSteal = 7; // Limit the number of items stolen per event
        var world = server.getOverworld(); // Use overworld directly as specified

        // Iterate through blocks in the defined radius, if chest, iterate through
        // contents. Note: this can be expensive; bail out early when we've stolen
        // enough item types (global limit).
        for (BlockPos pos : BlockPos.iterate(bedPos.add(-searchRadius, -searchRadius, -searchRadius),
                bedPos.add(searchRadius, searchRadius, searchRadius))) {
            // Make sure the chunk is loaded before accessing blocks/tiles
            if (!ChunkLoader.loadChunksInRadius(world, pos, 1)) {
                continue;
            }

            var blockState = world.getBlockState(pos);
            if (blockState.getBlock() instanceof net.minecraft.block.ChestBlock) {
                var chestEntity = world.getBlockEntity(pos);
                if (chestEntity instanceof net.minecraft.block.entity.ChestBlockEntity chest) {
                    for (int i = 0; i < chest.size(); i++) {
                        var itemStack = chest.getStack(i);

                        // skip empty slots immediately
                        if (itemStack.isEmpty()) continue;

                        var item = itemStack.getItem();
                        if (!wantedItems.contains(item)) continue;

                        if (random.nextDouble() > 0.3) {
                            continue;
                        }

                        int numberOfItemsToSteal = Math.min(itemStack.getCount(),
                                random.nextBetween(1, itemStack.getCount() + 1));

                        if (numberOfItemsToSteal <= 0) continue;

                        // Capture name before decrementing so logs don't show AIR when we
                        // remove the whole stack
                        String stolenName = item.getName().getString();

                        itemStack.decrement(numberOfItemsToSteal);

                        // mark chest dirty so changes persist / sync
                        chest.markDirty();

                        itemsStolen += numberOfItemsToSteal;
                        itemTypeStolen++;
                        HorrorMod129.LOGGER.info("Stole " + numberOfItemsToSteal + " of "
                                + stolenName + " from chest at " + pos);

                        // Stop completely when we've stolen enough different item types
                        if (itemTypeStolen >= maxItemsToSteal) {
                            return itemsStolen;
                        }
                    }
                }
            }
        }
        return itemsStolen;
    }

    private static int doorOpener(MinecraftServer server, ServerPlayerEntity player, BlockPos bedPos) {
        // get all doors within 15 blocks of the bed position in overworld
        int doorsOpened = 0;
        var world = server.getOverworld(); // Use overworld directly as specified
        int searchRadius = 15;

        for (BlockPos pos : BlockPos.iterate(bedPos.add(-searchRadius, -searchRadius, -searchRadius),
                bedPos.add(searchRadius, searchRadius, searchRadius))) {
            // Make sure the chunk is loaded before accessing blocks
            if (!ChunkLoader.loadChunksInRadius(world, pos, 1)) {
                continue; // Skip if chunk couldn't be loaded
            }

            var blockState = world.getBlockState(pos);
            if (blockState.getBlock() instanceof DoorBlock) {
                if (random.nextDouble() > 0.5)
                    continue; // 50% chance to skip this door
                if (LineOfSightUtils.isBlockRenderedOnScreen(player, pos, 50))
                    continue; // only open if out of line of sight

                doorsOpened++;
                // toggle the door state
                boolean currentlyOpen = blockState.get(DoorBlock.OPEN);
                world.setBlockState(pos, blockState.with(DoorBlock.OPEN, !currentlyOpen));
            }
        }
        return doorsOpened;
    }

    private static int signPlacer(MinecraftServer server, ServerPlayerEntity player, BlockPos bedPos) {
        // list of possible sign texts
        var signMessages = java.util.Arrays.asList(
                "you're not me, are you?",
                "i dont recognise your username",
                "no-one was meant to be here but me.",
                "Blueice129 was here.");
        // place sign at end of bed, or closeest block possible
        var world = server.getOverworld();

        boolean placed = false;
        int trys = 0;
        BlockPos signPos = bedPos.add(1, 0, 0);
        while (!placed) {
            // Make sure the chunk is loaded before accessing blocks
            if (!ChunkLoader.loadChunksInRadius(world, signPos, 1)) {
                // Try another position if the chunk couldn't be loaded
                signPos = signPos.add(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
                trys++;
                if (trys > 10)
                    return 0; // Give up after 10 tries
                continue;
            }

            if (world.getBlockState(signPos).isAir() && !world.getBlockState(signPos.down()).isAir()) {
                world.setBlockState(signPos, Blocks.OAK_SIGN.getDefaultState());
                var signEntity = world.getBlockEntity(signPos);
                if (signEntity instanceof net.minecraft.block.entity.SignBlockEntity sign) {
                    String message = signMessages.get(random.nextInt(signMessages.size()));
                    String[] words = message.split(" ");
                    String[] lines = { "", "", "", "" };
                    int lineIndex = 0;

                    for (String word : words) {
                        // If adding this word would exceed line length
                        if (lines[lineIndex].length() + word.length() + (lines[lineIndex].isEmpty() ? 0 : 1) > 15) {
                            lineIndex++;
                            if (lineIndex >= 4)
                                break; // No more lines available
                            lines[lineIndex] = word;
                        } else {
                            // Add space if not first word on line
                            if (!lines[lineIndex].isEmpty()) {
                                lines[lineIndex] += " ";
                            }
                            lines[lineIndex] += word;
                        }
                    }

                    sign.changeText(signText -> {
                        return signText.withMessage(0, Text.literal(lines[0]))
                                .withMessage(1, Text.literal(lines[1]))
                                .withMessage(2, Text.literal(lines[2]))
                                .withMessage(3, Text.literal(lines[3]));
                    }, true);
                    sign.markDirty();
                }
                placed = true;
            } else {
                signPos = signPos.add(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
                trys++;
                if (trys > 10) {
                    break; // give up after 10 tries
                }
            }
        }

        return placed ? 1 : 0;
    }

    private static int flowerPlanter(MinecraftServer server, ServerPlayerEntity player, BlockPos bedPos) {
        if (random.nextDouble() < 0.5) // 50% chance to plant flowers
            return 0;
        int searchRadius = 15;
        var world = server.getOverworld();
        int flowersPlanted = 0;
        // flower = lilly of the valley
        var flowerBlock = net.minecraft.block.Blocks.LILY_OF_THE_VALLEY;

        for (BlockPos pos : BlockPos.iterate(bedPos.add(-searchRadius, -searchRadius, -searchRadius),
                bedPos.add(searchRadius, searchRadius, searchRadius))) {
            // Make sure the chunk is loaded before accessing blocks
            if (!ChunkLoader.loadChunksInRadius(world, pos, 1)) {
                continue; // Skip if chunk couldn't be loaded
            }

            if (world.getBlockState(pos).isAir()
                    && world.getBlockState(pos.down()).getBlock().equals(net.minecraft.block.Blocks.GRASS_BLOCK)) {

                if (random.nextDouble() > 0.05)
                    continue; // 5% chance to plant a flower
                if (LineOfSightUtils.isBlockRenderedOnScreen(player, pos, 50))
                    continue; // only plant if out of line of sight

                world.setBlockState(pos, flowerBlock.getDefaultState());
                flowersPlanted++;
            }

        }
        return flowersPlanted;
    }

    private static int trapSetter(MinecraftServer server, ServerPlayerEntity player, BlockPos bedPos) {
        if (random.nextDouble() < 0.2) { // 20% chance to set a trap
            // place 1 trapped chest somewhere random 5-15 blocks from the bed
            int searchRadius = 15;
            var world = server.getOverworld();
            boolean placed = false;
            int trys = 0;
            while (!placed) {
                // Make sure we're at least 5 blocks away from the bed, but not more than 15
                int xOffset = random.nextInt(searchRadius - 5) + 5;
                int zOffset = random.nextInt(searchRadius - 5) + 5;
                if (random.nextBoolean())
                    xOffset = -xOffset;
                if (random.nextBoolean())
                    zOffset = -zOffset;

                var pos = bedPos.add(xOffset, 0, zOffset);

                // Find a suitable Y level for the chest
                pos = findSuitableY(world, pos);

                var chestBlock = world.getBlockState(pos);
                var blockBelow = world.getBlockState(pos.down());
                boolean isAreaClear = true;

                // Instead of checking all surrounding blocks, just check if there's enough
                // solid ground below
                boolean hasEnoughSolidGround = true;
                BlockPos tntPos = pos.down(3);

                // Check if the TNT position and 1 block below it are solid enough
                if (world.getBlockState(tntPos).isAir() || world.getBlockState(tntPos.down()).isAir()) {
                    HorrorMod129.LOGGER.info("Cannot place trap, TNT area not solid at " + tntPos);
                    hasEnoughSolidGround = false;
                }

                if (!hasEnoughSolidGround) {
                    isAreaClear = false;
                }

                // Check if the block where we want to place the chest is replaceable
                if (!(chestBlock.isAir() || chestBlock.getBlock().equals(Blocks.GRASS)
                        || chestBlock.getBlock().equals(Blocks.TALL_GRASS)
                        || chestBlock.getBlock() instanceof net.minecraft.block.FlowerBlock)) {
                    HorrorMod129.LOGGER.info("Cannot place trap, second check failed at " + pos + " block is "
                            + chestBlock.getBlock().toString());
                    isAreaClear = false;
                }

                if (blockBelow.isAir()) {
                    HorrorMod129.LOGGER.info("Cannot place trap, third check failed at " + pos.down());
                    isAreaClear = false;
                }

                if (LineOfSightUtils.isBlockRenderedOnScreen(player, pos, 50)) {
                    HorrorMod129.LOGGER.info("Cannot place trap, player has line of sight at " + pos);
                    isAreaClear = false; // only place if out of line of sight
                }

                if (isAreaClear) {
                    world.setBlockState(pos, Blocks.TRAPPED_CHEST.getDefaultState());
                    // put a writtten paper in the chest that says "this is singleplayer"
                    var chestEntity = world.getBlockEntity(pos);
                    if (chestEntity instanceof ChestBlockEntity) {
                        var itemStack = new ItemStack(Items.PAPER);
                        itemStack.setCustomName(Text.literal("how did you get access to me?"));
                        ((ChestBlockEntity) chestEntity).setStack(13, itemStack);
                    }
                    var tntBlock = Blocks.TNT.getDefaultState();
                    var tntMiddle = pos.down(3);
                    world.setBlockState(tntMiddle, tntBlock);
                    world.setBlockState(tntMiddle.down(), tntBlock);
                    world.setBlockState(tntMiddle.up(), tntBlock);
                    world.setBlockState(tntMiddle.north(), tntBlock);
                    world.setBlockState(tntMiddle.south(), tntBlock);
                    world.setBlockState(tntMiddle.east(), tntBlock);
                    world.setBlockState(tntMiddle.west(), tntBlock);
                    HorrorMod129.LOGGER.info("Placed trapped chest with TNT trap at " + pos);
                    placed = true;
                }
                trys++;
                horror.blueice129.HorrorMod129.LOGGER
                        .info("Trap placement try " + trys + " at " + pos + ", isAreaClear: " + isAreaClear);
                if (trys > 40) {
                    HorrorMod129.LOGGER.info("Giving up on trap placement after 40 tries");
                    break; // give up after 40 tries (increased for more attempts)
                }
            }
            return placed ? 1 : 0;
        }

        return 0;
    }

    private static BlockPos findSuitableY(net.minecraft.world.World world, BlockPos pos) {
        // Start from a bit above current position and find a suitable place
        BlockPos startPos = new BlockPos(pos.getX(), pos.getY() + 20, pos.getZ());

        // Search downward for a valid position
        for (int y = startPos.getY(); y > world.getBottomY() + 10; y--) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());

            if (world.getBlockState(checkPos).isAir() &&
                    !world.getBlockState(checkPos.down()).isAir() &&
                    world.getBlockState(checkPos.down()).isFullCube(world, checkPos.down())) {

                // Make sure there's space for TNT below
                if (!world.getBlockState(checkPos.down(2)).isAir() &&
                        !world.getBlockState(checkPos.down(3)).isAir() &&
                        !world.getBlockState(checkPos.down(4)).isAir()) {
                    return checkPos;
                }
            }
        }

        // If no suitable position found, return original pos
        return pos;
    }

    private static boolean playerLeavesWorld(MinecraftServer server) {
        if (random.nextDouble() < 0.3) { // 30% chance the player leaves
            // send a chat message saying another player just left the world
            // TODO: make this work with global logged in variable yet to be made
            server.getPlayerManager().broadcast(
                    Text.literal("Blueice129 left the game").styled(style -> style.withColor(0xFFFF55)), false);
            return true;
        }
        return false; // Return true if the player has left, false otherwise
    }

}
