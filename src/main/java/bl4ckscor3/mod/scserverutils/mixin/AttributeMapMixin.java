package bl4ckscor3.mod.scserverutils.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

/**
 * Gets rid of "Unknown attribute" spam, because there is a lot of that when loading chunks in NeoForge that have last been
 * saved with Forge.
 */
@Mixin(AttributeMap.class)
public abstract class AttributeMapMixin {
	@Shadow
	public abstract AttributeInstance getInstance(Attribute attribute);

	@Overwrite
	public void load(ListTag nbt) {
		for (int i = 0; i < nbt.size(); ++i) {
			CompoundTag tag = nbt.getCompound(i);
			String attributeName = tag.getString("Name");

			Util.ifElse(BuiltInRegistries.ATTRIBUTE.getOptional(ResourceLocation.tryParse(attributeName)), attribute -> {
				AttributeInstance instance = getInstance(attribute);

				if (instance != null)
					instance.load(tag);
			}, () -> {});
		}
	}
}
