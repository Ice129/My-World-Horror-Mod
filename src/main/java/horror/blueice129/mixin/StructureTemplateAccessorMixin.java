package horror.blueice129.mixin;

import net.minecraft.structure.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(StructureTemplate.class)
public interface StructureTemplateAccessorMixin {

	@Accessor("blockInfoLists")
	List<StructureTemplate.PalettedBlockInfoList> horror$getBlockInfoLists();

	@Accessor("entities")
	List<StructureTemplate.StructureEntityInfo> horror$getEntities();
}