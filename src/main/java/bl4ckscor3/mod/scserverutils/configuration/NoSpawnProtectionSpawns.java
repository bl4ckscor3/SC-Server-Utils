package bl4ckscor3.mod.scserverutils.configuration;

import java.util.List;

import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

public record NoSpawnProtectionSpawns(BooleanValue enabled, ConfigValue<List<? extends String>> allowedSpawnTypes) {
	public List<MobSpawnType> resolve() {
		//@formatter:off
		return allowedSpawnTypes.get()
				.stream()
				.map(name -> Enum.valueOf(MobSpawnType.class, name))
				.toList();
	}
}