package horror.blueice129.scheduler;

import horror.blueice129.data.HorrorModPersistentState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;


public class LocationBoundingBoxScheduler {

    // each tick check each players location against a list of bounding boxes
    // if a player is inside a bounding box, trigger the corresponding event at a specified location
    // list of bounding boxes, functions to call, and locations to be passed to the functions should be in persistent state
    // once a bounding box is triggered, its corosponding data should be removed from the persistent state


    // all these lists should be the same length, and the index of the bounding box should correspond to the index of the function to call and the location to trigger at
    // extra care should be taken to ensure that the data is removed from the lists correctly to avoid desyncing the indexes
    private final static String boundingBoxListKey = "boundingBoxList"; // will be a list of groups of 2 block corners that define the bounding box
    private final static String boundingBoxEventListKey = "boundingBoxEventList"; // contains the name of the function to call, will be used in a switch
    private final static String boundingBoxTriggerLocationListKey = "boundingBoxTriggerLocationList";

    private static HorrorModPersistentState state;


    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(LocationBoundingBoxScheduler::onServerTick);
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.isClient())
                return;
            if (world.getRegistryKey() == net.minecraft.world.World.OVERWORLD) {
                state = HorrorModPersistentState.getServerState(server);
            }
        });
    }

    public static void onServerTick(net.minecraft.server.MinecraftServer server) {
        // Skip processing if server is empty
        if (server.getPlayerManager().getPlayerList().isEmpty()) {
            return;
        }

        // Get the lists from the persistent state
        var boundingBoxList = state.getPositionList(boundingBoxListKey);
        var boundingBoxEventList = state.getStringList(boundingBoxEventListKey);
        var boundingBoxTriggerLocationList = state.getPositionList(boundingBoxTriggerLocationListKey);

        
    }
}
