package horror.blueice129.feature;

import com.google.gson.*;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ItemEntity;
// import net.minecraft.item.ArmorItem; // Unused import removed
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import horror.blueice129.HorrorMod129;
import horror.blueice129.utils.ChunkLoader;
import horror.blueice129.utils.StructurePlacer;

/**
 * handles having items spawn as if a player had died
 */

public class PlayerDeathItems {
    // Will be used when implementing item spawning mechanics
    private static final Random RANDOM = Random.create();
    private static final Gson GSON = new Gson();

    /**
     * gets json data from file and returns a structured object
     * 
     * @param server The Minecraft server instance
     * @return JsonObject containing the parsed dead player items data, or null if loading failed
     */
    public static JsonObject getJsonData(MinecraftServer server) {
        // Using correct namespaced path format: namespace:path
        Identifier resourceId = new Identifier("horror-mod-129", "json/deadPlayerItems.json");
        ResourceManager resourceManager = server.getResourceManager();
        
        try {
            Resource resource = resourceManager.getResource(resourceId).orElseThrow();
            try (Reader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                return GSON.fromJson(reader, JsonObject.class);
            }
        } catch (IOException e) {
            HorrorMod129.LOGGER.error("Failed to load dead player items JSON data: {}", e.getMessage());
        } catch (JsonSyntaxException e) {
            HorrorMod129.LOGGER.error("Invalid JSON in dead player items data: {}", e.getMessage());
        } catch (Exception e) {
            HorrorMod129.LOGGER.error("Error loading dead player items data: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Triggers a fake player death event, spawning items from the configuration at a location near the player
     * 
     * @param server The Minecraft server instance
     * @param player The player to spawn items around
     * @return boolean indicating if the event was successfully triggered
     */
    public static boolean triggerEvent(MinecraftServer server, net.minecraft.server.network.ServerPlayerEntity player) {
        // Get a random surface location near the player to spawn items
        BlockPos pos = findRandomSurfaceLocation(server.getOverworld(), player);
        if (pos == null) {
            HorrorMod129.LOGGER.error("Could not find suitable location for player death items");
            return false;
        }
        
        // Get agro meter value for item tier selection
        horror.blueice129.data.HorrorModPersistentState state = horror.blueice129.data.HorrorModPersistentState.getServerState(server);
        int agroMeter = state.getIntValue("agroMeter", 0);
        
        // Generate the items based on the config and agro meter
        List<ItemStack> items = generatePlayerItems(server, agroMeter);
        if (items.isEmpty()) {
            HorrorMod129.LOGGER.error("Failed to generate player death items");
            return false;
        }
        
        // Scatter the items around the location
        boolean success = scatterPlayerItems(server.getOverworld(), pos, items);
        
        HorrorMod129.LOGGER.info("Player death items event completed: " + success);
        return success;
    }
    
    /**
     * Legacy method for backward compatibility
     */
    public static boolean triggerEvent(MinecraftServer server) {
        // Get a random player
        if (server.getPlayerManager().getPlayerList().isEmpty()) {
            HorrorMod129.LOGGER.error("No players online to trigger player death items");
            return false;
        }
        
        // Pick a random player to spawn items around
        net.minecraft.server.network.ServerPlayerEntity player = server.getPlayerManager().getPlayerList()
                .get(RANDOM.nextInt(server.getPlayerManager().getPlayerList().size()));
        
        return triggerEvent(server, player);
    }
    
    /**
     * Finds a random location on the surface near a player to spawn items
     * 
     * @param world The server world to search in
     * @param player The player to search around
     * @return A suitable BlockPos, or null if none found
     */
    private static BlockPos findRandomSurfaceLocation(ServerWorld world, net.minecraft.server.network.ServerPlayerEntity player) {
        // Find a random location that's on solid ground, not in water, near the player
        BlockPos playerPos = player.getBlockPos();
        
        // Use the StructurePlacer utility to find a suitable location
        // Between 30 and 100 blocks from player, not in water
        return StructurePlacer.findSurfaceLocation(world, playerPos, player, 30, 100, true);
    }
    
    /**
     * Generates a list of items based on the configuration and agro meter
     * 
     * @param server The Minecraft server instance
     * @param agroMeter The current agro meter value (0-10)
     * @return List of ItemStacks to spawn
     */
    private static List<ItemStack> generatePlayerItems(MinecraftServer server, int agroMeter) {
        List<ItemStack> items = new ArrayList<>();
        JsonObject json = getJsonData(server);
        if (json == null) {
            return items;
        }
        
        // Choose a class tier for the player based on agro meter (low, mid, high)
        String classTier = chooseClassTierBasedOnAgro(agroMeter);
        
        // Add armor items based on the chosen class tier
        addArmorItems(items, json, classTier);
        
        // Add essential tools (sword, pickaxe, etc.)
        addEssentialItems(items, json, classTier);
        
        // Add consumables (food)
        addConsumableItems(items, json);
        
        // Add random miscellaneous items
        addRandomItems(items, json);
        
        return items;
    }
    
    /*
     * Legacy method for backward compatibility - kept for reference
     *
    private static List<ItemStack> generatePlayerItems(MinecraftServer server) {
        return generatePlayerItems(server, 0); // Default to 0 agro
    }
    */
    
    /**
     * Choose a class tier based on agro meter value
     * 
     * @param agroMeter The current agro meter value (0-10)
     * @return String representing the class tier
     */
    private static String chooseClassTierBasedOnAgro(int agroMeter) {
        // Clamp agro meter between 0 and 10
        agroMeter = Math.max(0, Math.min(10, agroMeter));
        
        // Calculate probabilities based on agro meter
        int lowClassProb, midClassProb;
        
        if (agroMeter <= 3) {
            // Low agro: mostly low class, some mid class, rare high class
            lowClassProb = 70 - agroMeter * 10;  // 70-40%
            midClassProb = 25 + agroMeter * 5;   // 25-40%
            // highClassProb = 5 + agroMeter * 5;   // 5-20% - calculated implicitly as 100-(lowClassProb+midClassProb)
        } else if (agroMeter <= 7) {
            // Mid agro: mostly mid class, some low class, increasing high class
            lowClassProb = 40 - (agroMeter - 3) * 10;  // 40-0%
            midClassProb = 40;                         // 40% constant
            // highClassProb = 20 + (agroMeter - 3) * 10; // 20-60% - calculated implicitly as 100-(lowClassProb+midClassProb)
        } else {
            // High agro: decreasing mid class, increasing high class
            lowClassProb = 0;                          // 0% constant
            midClassProb = 40 - (agroMeter - 7) * 10;  // 40-10%
            // highClassProb = 60 + (agroMeter - 7) * 10; // 60-90% - calculated implicitly as 100-(lowClassProb+midClassProb)
        }
        
        // Roll based on calculated probabilities
        int roll = RANDOM.nextInt(100);
        
        if (roll < lowClassProb) {
            return "lowClass";
        } else if (roll < lowClassProb + midClassProb) {
            return "midClass";
        } else {
            return "highClass";
        }
    }
    
    /**
     * Add armor items to the list based on the chosen class tier
     * Each piece of armor can be a different material based on class tier
     * 
     * @param items List to add items to
     * @param json JSON configuration
     * @param classTier The chosen class tier
     */
    private static void addArmorItems(List<ItemStack> items, JsonObject json, String classTier) {
        try {
            JsonObject armour = json.getAsJsonObject("armour");
            int minDurabilityPercent = armour.get("minDurabilityPercent").getAsInt();
            int minMendingDurabilityPercent = armour.get("minMendingDurabilityPercent").getAsInt();
            
            // Get material distribution for the chosen class tier
            JsonObject materialDistribution = armour.getAsJsonObject(classTier);
            List<String> materials = new ArrayList<>();
            List<Integer> weights = new ArrayList<>();
            
            for (Map.Entry<String, JsonElement> entry : materialDistribution.entrySet()) {
                materials.add(entry.getKey());
                weights.add(entry.getValue().getAsInt());
            }
            
            // Add each armor piece (helmet, chestplate, leggings, boots) with potentially different materials
            String helmetMaterial = chooseRandomWeighted(materials, weights);
            String chestplateMaterial = chooseRandomWeighted(materials, weights);
            String leggingsMaterial = chooseRandomWeighted(materials, weights);
            String bootsMaterial = chooseRandomWeighted(materials, weights);
            
            // Add each piece with its selected material
            addArmorPiece(items, armour, "helmet", helmetMaterial, minDurabilityPercent, minMendingDurabilityPercent);
            addArmorPiece(items, armour, "chestplate", chestplateMaterial, minDurabilityPercent, minMendingDurabilityPercent);
            addArmorPiece(items, armour, "leggings", leggingsMaterial, minDurabilityPercent, minMendingDurabilityPercent);
            addArmorPiece(items, armour, "boots", bootsMaterial, minDurabilityPercent, minMendingDurabilityPercent);
        } catch (Exception e) {
            HorrorMod129.LOGGER.error("Error adding armor items: {}", e.getMessage());
        }
    }
    
    /**
     * Add a single armor piece to the items list
     * 
     * @param items List to add the armor to
     * @param armourConfig Armor config JSON object
     * @param pieceType Type of armor piece (helmet, chestplate, etc.)
     * @param material Material of the armor
     * @param minDurabilityPercent Minimum durability percentage
     * @param minMendingDurabilityPercent Minimum durability percentage for items with Mending
     */
    private static void addArmorPiece(List<ItemStack> items, JsonObject armourConfig, String pieceType, String material, 
                                    int minDurabilityPercent, int minMendingDurabilityPercent) {
        try {
            // Get the item based on material and piece type
            String itemKey = material + "_" + pieceType;
            Item item = Registries.ITEM.get(new Identifier(itemKey));
            
            // Skip if item doesn't exist
            if (item == Items.AIR) {
                return;
            }
            
            // Create the item stack
            ItemStack stack = new ItemStack(item);
            
            // Apply enchantments based on config
            JsonObject pieceConfig = armourConfig.getAsJsonObject(pieceType);
            Map<Enchantment, Integer> enchantments = generateEnchantments(pieceConfig.getAsJsonObject("enchants"));
            
            // Apply custom name if applicable
            applyCustomName(stack, pieceConfig);
            
            // Apply durability damage
            applyDurabilityDamage(stack, enchantments, minDurabilityPercent, minMendingDurabilityPercent);
            
            // Apply enchantments to the item
            EnchantmentHelper.set(enchantments, stack);
            
            // Add the item to the list
            items.add(stack);
        } catch (Exception e) {
            HorrorMod129.LOGGER.error("Error adding armor piece {}: {}", pieceType, e.getMessage());
        }
    }
    
    /**
     * Add essential tools to the items list
     * Each tool can be a different material based on class tier
     * 
     * @param items List to add items to
     * @param json JSON configuration
     * @param classTier The chosen class tier
     */
    private static void addEssentialItems(List<ItemStack> items, JsonObject json, String classTier) {
        try {
            JsonObject essentials = json.getAsJsonObject("Essentials");
            int minDurabilityPercent = essentials.get("minDurabilityPercent").getAsInt();
            int minMendingDurabilityPercent = essentials.get("minMendingDurabilityPercent").getAsInt();
            
            // Get material distribution for the chosen class tier
            JsonObject materialDistribution = essentials.getAsJsonObject(classTier);
            List<String> materials = new ArrayList<>();
            List<Integer> weights = new ArrayList<>();
            
            for (Map.Entry<String, JsonElement> entry : materialDistribution.entrySet()) {
                materials.add(entry.getKey());
                weights.add(entry.getValue().getAsInt());
            }
            
            // Choose a random material for each tool independently
            String swordMaterial = chooseRandomWeighted(materials, weights);
            String pickaxeMaterial = chooseRandomWeighted(materials, weights);
            String axeMaterial = chooseRandomWeighted(materials, weights);
            String shovelMaterial = chooseRandomWeighted(materials, weights);
            
            // There's a 30% chance that all tools will be the same material
            if (RANDOM.nextInt(100) < 30 && !materials.isEmpty()) {
                String commonMaterial = chooseRandomWeighted(materials, weights);
                swordMaterial = commonMaterial;
                pickaxeMaterial = commonMaterial;
                axeMaterial = commonMaterial;
                shovelMaterial = commonMaterial;
            } 
            
            // Add each essential tool with its selected material
            addToolItem(items, essentials, "sword", swordMaterial, minDurabilityPercent, minMendingDurabilityPercent);
            addToolItem(items, essentials, "pickaxe", pickaxeMaterial, minDurabilityPercent, minMendingDurabilityPercent);
            addToolItem(items, essentials, "axe", axeMaterial, minDurabilityPercent, minMendingDurabilityPercent);
            addToolItem(items, essentials, "shovel", shovelMaterial, minDurabilityPercent, minMendingDurabilityPercent);
        } catch (Exception e) {
            HorrorMod129.LOGGER.error("Error adding essential items: {}", e.getMessage());
        }
    }
    
    /**
     * Add a single tool to the items list
     * 
     * @param items List to add the tool to
     * @param essentialsConfig Essential tools config JSON object
     * @param toolType Type of tool (sword, pickaxe, etc.)
     * @param material Material of the tool
     * @param minDurabilityPercent Minimum durability percentage
     * @param minMendingDurabilityPercent Minimum durability percentage for items with Mending
     */
    private static void addToolItem(List<ItemStack> items, JsonObject essentialsConfig, String toolType, String material,
                                 int minDurabilityPercent, int minMendingDurabilityPercent) {
        try {
            // Get the item based on material and tool type
            String itemKey = material + "_" + toolType;
            Item item = Registries.ITEM.get(new Identifier(itemKey));
            
            // Skip if item doesn't exist
            if (item == Items.AIR) {
                return;
            }
            
            // Create the item stack
            ItemStack stack = new ItemStack(item);
            
            // Apply enchantments based on config
            JsonObject toolConfig = essentialsConfig.getAsJsonObject(toolType);
            Map<Enchantment, Integer> enchantments = generateEnchantments(toolConfig.getAsJsonObject("enchants"));
            
            // Apply custom name if applicable
            applyCustomName(stack, toolConfig);
            
            // Apply durability damage
            applyDurabilityDamage(stack, enchantments, minDurabilityPercent, minMendingDurabilityPercent);
            
            // Apply enchantments to the item
            EnchantmentHelper.set(enchantments, stack);
            
            // Add the item to the list
            items.add(stack);
        } catch (Exception e) {
            HorrorMod129.LOGGER.error("Error adding tool item {}: {}", toolType, e.getMessage());
        }
    }
    
    /**
     * Generate enchantments for an item based on the config
     * 
     * @param enchantsConfig JSON object containing enchantment configuration
     * @return Map of enchantments to their levels
     */
    private static Map<Enchantment, Integer> generateEnchantments(JsonObject enchantsConfig) {
        Map<Enchantment, Integer> enchantments = new HashMap<>();
        
        for (Map.Entry<String, JsonElement> entry : enchantsConfig.entrySet()) {
            String enchantId = entry.getKey();
            JsonObject enchantConfig = entry.getValue().getAsJsonObject();
            
            // Check if this enchantment should be applied based on chance
            double chance = enchantConfig.get("chance").getAsDouble();
            if (RANDOM.nextDouble() > chance) {
                continue;
            }
            
            // Get the enchantment
            Enchantment enchantment = Registries.ENCHANTMENT.get(new Identifier(enchantId));
            if (enchantment == null) {
                continue;
            }
            
            // Get a random level between min and max
            int minLevel = enchantConfig.get("minLevel").getAsInt();
            int maxLevel = enchantConfig.get("maxLevel").getAsInt();
            int level = minLevel + RANDOM.nextInt(maxLevel - minLevel + 1);
            
            // Add the enchantment
            enchantments.put(enchantment, level);
        }
        
        return enchantments;
    }
    
    /**
     * Apply a custom name to an item if applicable
     * 
     * @param stack The ItemStack to name
     * @param config JSON object containing name configuration
     */
    private static void applyCustomName(ItemStack stack, JsonObject config) {
        try {
            double nameChance = config.get("nameChance").getAsDouble();
            
            // Check if a custom name should be applied
            if (RANDOM.nextDouble() <= nameChance) {
                JsonArray namesArray = config.getAsJsonArray("names");
                int nameIndex = RANDOM.nextInt(namesArray.size());
                String name = namesArray.get(nameIndex).getAsString();
                
                stack.setCustomName(Text.literal(name));
            }
        } catch (Exception e) {
            // If anything goes wrong, just don't apply a name
        }
    }
    
    /**
     * Apply durability damage to an item
     * 
     * @param stack The ItemStack to damage
     * @param enchantments Map of enchantments on the item
     * @param minDurabilityPercent Minimum durability percentage
     * @param minMendingDurabilityPercent Minimum durability percentage for items with Mending
     */
    private static void applyDurabilityDamage(ItemStack stack, Map<Enchantment, Integer> enchantments, 
                                           int minDurabilityPercent, int minMendingDurabilityPercent) {
        if (!stack.isDamageable()) {
            return;
        }
        
        // Check if the item has Mending
        boolean hasMending = enchantments.keySet().stream()
            .anyMatch(enchantment -> enchantment.getTranslationKey().equals("enchantment.minecraft.mending"));
        
        // Calculate damage amount
        int maxDamage = stack.getMaxDamage();
        int minDurability = hasMending ? minMendingDurabilityPercent : minDurabilityPercent;
        int maxDamageToApply = maxDamage - (maxDamage * minDurability / 100);
        
        // Apply a random amount of damage up to the maximum calculated
        int damageToApply = RANDOM.nextInt(maxDamageToApply);
        stack.setDamage(damageToApply);
    }
    
    /**
     * Add consumable items (food) to the items list
     * 
     * @param items List to add items to
     * @param json JSON configuration
     */
    private static void addConsumableItems(List<ItemStack> items, JsonObject json) {
        try {
            JsonObject consumables = json.getAsJsonObject("consumables");
            JsonObject food = consumables.getAsJsonObject("food");
            
            // Get food types
            JsonArray typesArray = food.getAsJsonArray("types");
            List<String> foodTypes = new ArrayList<>();
            for (JsonElement element : typesArray) {
                foodTypes.add(element.getAsString());
            }
            
            // Determine amount of food to add
            int minAmount = food.get("minAmount").getAsInt();
            int maxAmount = food.get("maxAmount").getAsInt();
            int totalAmount = minAmount + RANDOM.nextInt(maxAmount - minAmount + 1);
            
            // Add random food items
            while (totalAmount > 0) {
                // Choose a random food type
                String foodType = foodTypes.get(RANDOM.nextInt(foodTypes.size()));
                Item foodItem = Registries.ITEM.get(new Identifier(foodType));
                
                // Skip if item doesn't exist
                if (foodItem == Items.AIR) {
                    continue;
                }
                
                // Create a stack with up to 64 items (or remaining amount)
                int stackSize = Math.min(64, totalAmount);
                ItemStack stack = new ItemStack(foodItem, stackSize);
                items.add(stack);
                
                totalAmount -= stackSize;
            }
        } catch (Exception e) {
            HorrorMod129.LOGGER.error("Error adding consumable items: {}", e.getMessage());
        }
    }
    
    /**
     * Add random miscellaneous items to the items list
     * 
     * @param items List to add items to
     * @param json JSON configuration
     */
    private static void addRandomItems(List<ItemStack> items, JsonObject json) {
        try {
            JsonObject randomItems = json.getAsJsonObject("randomItems");
            
            // Determine how many random items to add
            int minNumberItems = randomItems.get("minNumberItems").getAsInt();
            int maxNumberItems = randomItems.get("maxNumberItems").getAsInt();
            int numberItems = minNumberItems + RANDOM.nextInt(maxNumberItems - minNumberItems + 1);
            
            // Get item types by stack size
            JsonArray stack64Array = randomItems.getAsJsonArray("64Stacking");
            JsonArray stack16Array = randomItems.getAsJsonArray("16Stacking");
            JsonArray stack1Array = randomItems.getAsJsonArray("1Stacking");
            
            // Add random items
            for (int i = 0; i < numberItems; i++) {
                // Choose which stacking category to use
                int stackCategory = RANDOM.nextInt(10);
                JsonArray itemArray;
                int maxStackSize;
                
                if (stackCategory < 6) { // 60% chance for 64-stacking items
                    itemArray = stack64Array;
                    maxStackSize = 64;
                } else if (stackCategory < 9) { // 30% chance for 16-stacking items
                    itemArray = stack16Array;
                    maxStackSize = 16;
                } else { // 10% chance for 1-stacking items
                    itemArray = stack1Array;
                    maxStackSize = 1;
                }
                
                // Choose a random item from the category
                String itemId = itemArray.get(RANDOM.nextInt(itemArray.size())).getAsString();
                Item item = Registries.ITEM.get(new Identifier(itemId));
                
                // Skip if item doesn't exist
                if (item == Items.AIR) {
                    continue;
                }
                
                // Create a stack with a random amount
                int stackSize = 1 + RANDOM.nextInt(maxStackSize);
                ItemStack stack = new ItemStack(item, stackSize);
                items.add(stack);
            }
        } catch (Exception e) {
            HorrorMod129.LOGGER.error("Error adding random items: {}", e.getMessage());
        }
    }
    
    /**
     * Scatter the items around a location as if a player died
     * 
     * @param world The world to spawn items in
     * @param pos The central position to scatter items around
     * @param items The items to scatter
     * @return boolean indicating success
     */
    private static boolean scatterPlayerItems(ServerWorld world, BlockPos pos, List<ItemStack> items) {
        if (items.isEmpty()) {
            return false;
        }
        
        // Make sure the chunks are loaded
        if (!ChunkLoader.loadChunksInRadius(world, pos, 2)) {
            return false;
        }
        
        HorrorMod129.LOGGER.info("Scattering {} player death items at {}", items.size(), pos);
        
        // Convert BlockPos to Vec3d for more precise positioning
        Vec3d spawnPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        
        // Scatter each item
        for (ItemStack stack : items) {
            // Calculate random offset from center
            double xOffset = RANDOM.nextDouble() * 2.0 - 1.0; // -1.0 to 1.0
            double yOffset = RANDOM.nextDouble() * 0.5; // 0 to 0.5
            double zOffset = RANDOM.nextDouble() * 2.0 - 1.0; // -1.0 to 1.0
            
            // Calculate velocity for the item
            double xVel = RANDOM.nextDouble() * 0.2 - 0.1; // -0.1 to 0.1
            double yVel = RANDOM.nextDouble() * 0.2 + 0.2; // 0.2 to 0.4
            double zVel = RANDOM.nextDouble() * 0.2 - 0.1; // -0.1 to 0.1
            
            // Create and spawn the item entity
            ItemEntity itemEntity = new ItemEntity(
                world, 
                spawnPos.x + xOffset, 
                spawnPos.y + yOffset, 
                spawnPos.z + zOffset, 
                stack.copy()
            );
            
            // Set the item's velocity
            itemEntity.setVelocity(xVel, yVel, zVel);
            
            // Set standard pickup delay
            itemEntity.setPickupDelay(40); // 2 seconds
            
            // Spawn the item in the world
            world.spawnEntity(itemEntity);
        }
        
        return true;
    }
    
    /**
     * Choose a random item from a weighted list
     * 
     * @param options List of options
     * @param weights List of corresponding weights
     * @return The chosen option
     */
    private static String chooseRandomWeighted(List<String> options, List<Integer> weights) {
        int totalWeight = 0;
        for (int weight : weights) {
            totalWeight += weight;
        }
        
        int randomValue = RANDOM.nextInt(totalWeight);
        int runningTotal = 0;
        
        for (int i = 0; i < options.size(); i++) {
            runningTotal += weights.get(i);
            if (randomValue < runningTotal) {
                return options.get(i);
            }
        }
        
        // Fallback in case of error
        return options.get(0);
    }
}
