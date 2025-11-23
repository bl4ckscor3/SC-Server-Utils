package bl4ckscor3.mod.scserverutils.configuration.spawnprotection;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

@SuppressWarnings("rawtypes")
public record MobSpawning(BooleanValue enabled, ConfigValue<List<? extends String>> allowedSpawnTypes, ConfigValue<List<? extends String>> allowedEntityTypes, ConfigValue<List<? extends String>> verboseLoggingFor) {
	public Info resolve() {
		//@formatter:off
		return new Info(
			allowedSpawnTypes.get()
				.stream()
				.map(EntitySpawnReason::valueOf)
				.toList(),
			getEntityTypeList(allowedEntityTypes),
			getEntityTypeList(verboseLoggingFor)
		);
	}

	private static List<EntityType> getEntityTypeList(ConfigValue<List<? extends String>> configValue) {
		return configValue.get()
			.stream()
			.map(ResourceLocation::parse)
			.map(BuiltInRegistries.ENTITY_TYPE::get)
			.filter(Optional::isPresent)
			.map(EntityType.class::cast)
			.toList();
	}

	public record Info(List<EntitySpawnReason> allowedSpawnTypes, List<EntityType> allowedEntityTypes, List<EntityType> verboseLoggingFor) {}
}