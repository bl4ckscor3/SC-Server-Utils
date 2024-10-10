package bl4ckscor3.mod.scserverutils.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

public record SpawnProtectionEffects(BooleanValue enabled, ConfigValue<List<? extends String>> effects) {
	private static final Logger LOGGER = LogUtils.getLogger();

	public List<Supplier<MobEffectInstance>> resolve() {
		List<Supplier<MobEffectInstance>> resolvedEffects = new ArrayList<>();

		for (String entry : effects.get()) {
			String[] split = entry.split("\\|");

			if (split.length == 3) {
				int duration = Integer.parseInt(split[1]);
				int amplifier = Integer.parseInt(split[2]);

				if (validateValue(duration, entry, -1) && validateValue(amplifier, entry, 1)) {
					ResourceLocation effectLocation = ResourceLocation.parse(split[0]);

					if (!BuiltInRegistries.MOB_EFFECT.containsKey(effectLocation)) {
						LOGGER.warn("Effect \"{}\" does not exist, skipping", effectLocation);
						continue;
					}

					//the amplifier is actually 0-indexed, but 1-indexed in the config for ease of use
					resolvedEffects.add(() -> new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.getHolder(effectLocation).get(), duration, amplifier - 1, false, false));
				}
			}
			else
				LOGGER.warn("Not enough information provided for effect \"{}\", skipping", entry);
		}

		return resolvedEffects;
	}

	private static boolean validateValue(int value, String entry, int min) {
		if (value < min) {
			LOGGER.warn("Value \"{}\" cannot be less than {} for entry \"{}\", skipping", value, min, entry);
			return false;
		}

		return true;
	}
}