package horror.blueice129.scheduler;

import horror.blueice129.HorrorMod129;
import horror.blueice129.feature.UndergroundTunnelEvent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class UndergroundTunnelScheduler {
    private UndergroundTunnelScheduler() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(UndergroundTunnelEvent::tick);
        HorrorMod129.LOGGER.info("Registered UndergroundTunnelScheduler");
    }
}