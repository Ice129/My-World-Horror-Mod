package horror.blueice129.mixin;

import net.minecraft.item.ArmorItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ArmorItem.class)
public interface ArmorItemAccessor {
    @Accessor("type")
    ArmorItem.Type getArmorType();
}
