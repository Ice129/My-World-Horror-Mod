package horror.blueice129.mixin;

import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemEntity.class)
public abstract interface ItemEntityAccessor {
    @Accessor("pickupDelay")
    public abstract int getPickupDelay();
}
