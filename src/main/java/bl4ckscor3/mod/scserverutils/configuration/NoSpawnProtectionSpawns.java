package bl4ckscor3.mod.scserverutils.configuration;

import java.util.List;
import java.util.Objects;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

public record NoSpawnProtectionSpawns(BooleanValue enabled, ConfigValue<List<? extends String>> allowedSpawnTypes, ConfigValue<List<? extends String>> verboseLoggingFor) {
	public Info resolve() {
		//@formatter:off
		return new Info(
				allowedSpawnTypes.get()
					.stream()
					.map(EntitySpawnReason::valueOf)
					.toList(),
				verboseLoggingFor.get()
					.stream()
					.map(ResourceLocation::parse)
					.map(BuiltInRegistries.ENTITY_TYPE::get)
					.filter(Objects::nonNull)
					.map(EntityType.class::cast)
					.toList());
	}

	@SuppressWarnings("rawtypes")
	public record Info(List<EntitySpawnReason> allowedSpawnTypes, List<EntityType> verboseLoggingFor) {}
}