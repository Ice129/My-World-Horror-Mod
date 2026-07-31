package horror.blueice129.feature;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import horror.blueice129.mixin.MobEntityAccessor;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class AnimalFleeEvent {
    public static final int RADIUS = 15;
    public static final int MIN_ANIMALS = 5;
    public static final int FLEE_DURATION_TICKS = 20 * 10;

    private static final Map<UUID, ActiveFleeSession> ACTIVE_FLEES = new LinkedHashMap<>();

    public static int getNearbyAnimalCount(ServerPlayerEntity player) {
        return getNearbyAnimals(player).size();
    }

    public static boolean triggerEvent(ServerPlayerEntity player) {
        List<AnimalEntity> animals = getNearbyAnimals(player);

        if (animals.size() < MIN_ANIMALS) {
            return false;
        }

        for (AnimalEntity animal : animals) {
            startFlee(animal, player);
        }

        return true;
    }

    public static void tick() {
        if (ACTIVE_FLEES.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, ActiveFleeSession>> iterator = ACTIVE_FLEES.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveFleeSession> entry = iterator.next();
            ActiveFleeSession session = entry.getValue();

            if (session.animal.isRemoved() || !session.animal.isAlive()) {
                session.stop();
                iterator.remove();
                continue;
            }

            if (session.ticksRemaining-- <= 0) {
                session.stop();
                iterator.remove();
            }
        }
    }

    private static void startFlee(AnimalEntity animal, ServerPlayerEntity targetPlayer) {
        ActiveFleeSession existing = ACTIVE_FLEES.remove(animal.getUuid());
        if (existing != null) {
            existing.stop();
        }

        AnimalFleeGoal goal = new AnimalFleeGoal(animal, targetPlayer);
        GoalSelector goalSelector = ((MobEntityAccessor) animal).getGoalSelector();
        goalSelector.add(0, goal);
        ACTIVE_FLEES.put(animal.getUuid(), new ActiveFleeSession(animal, goal));
    }

    private static List<AnimalEntity> getNearbyAnimals(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        int radiusSquared = RADIUS * RADIUS;

        return world.getEntitiesByClass(
                AnimalEntity.class,
                player.getBoundingBox().expand(RADIUS),
                animal -> animal.squaredDistanceTo(player) <= radiusSquared);
    }

    private static final class ActiveFleeSession {
        private final AnimalEntity animal;
        private final AnimalFleeGoal goal;
        private int ticksRemaining = FLEE_DURATION_TICKS;

        private ActiveFleeSession(AnimalEntity animal, AnimalFleeGoal goal) {
            this.animal = animal;
            this.goal = goal;
        }

        private void stop() {
            GoalSelector goalSelector = ((MobEntityAccessor) animal).getGoalSelector();
            goalSelector.clear(activeGoal -> activeGoal == goal);
        }
    }
}