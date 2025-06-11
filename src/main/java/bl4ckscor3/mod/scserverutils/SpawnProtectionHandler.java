package bl4ckscor3.mod.scserverutils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import bl4ckscor3.mod.scserverutils.configuration.spawnprotection.MobSpawning;
import bl4ckscor3.mod.scserverutils.configuration.spawnprotection.Nether;
import bl4ckscor3.mod.scserverutils.configuration.spawnprotection.RiftStabilizer;
import bl4ckscor3.mod.scserverutils.configuration.spawnprotection.SpawnProtection;
import net.geforcemods.securitycraft.blockentities.RiftStabilizerBlockEntity.TeleportationType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class SpawnProtectionHandler {
	public static final String IN_SPAWN_PROTECTION_TAG = "in_spawn_protection";
	private static List<Supplier<MobEffectInstance>> effects = new ArrayList<>();
	private static MobSpawning.Info spawnInfo = new MobSpawning.Info(List.of(), List.of());

	public static void addListeners(IEventBus modEventBus) {
		SpawnProtection spawnProtection = Configuration.instance.spawnProtection;
		boolean pvpPreventionEnabled = spawnProtection.pvpPrevention().enabled().get();
		boolean effectsEnabled = spawnProtection.effects().enabled().get();

		if (pvpPreventionEnabled || effectsEnabled) {
			NeoForge.EVENT_BUS.addListener(SpawnProtectionHandler::onPlayerTickPost);
			NeoForge.EVENT_BUS.addListener(SpawnProtectionHandler::onEntityTravelToDimension);
		}

		if (pvpPreventionEnabled)
			NeoForge.EVENT_BUS.addListener(SpawnProtectionHandler::onLivingIncomingDamage);

		if (effectsEnabled) {
			effects = spawnProtection.effects().resolve();
			modEventBus.addListener(SpawnProtectionHandler::reloadResolvedConfigValues);
		}

		if (spawnProtection.mobSpawning().enabled().get()) {
			spawnInfo = spawnProtection.mobSpawning().resolve();
			NeoForge.EVENT_BUS.addListener(SpawnProtectionHandler::onFinalizeSpawn);
		}

		if (spawnProtection.riftStabilizer().enabled().get())
			NeoForge.EVENT_BUS.addListener(SpawnProtectionHandler::onEntityTeleport);
	}

	private static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
		if (event.getEntity() instanceof Player target && target.level() instanceof ServerLevel level) {
			if (level.dimension().equals(Level.NETHER) && !Configuration.instance.spawnProtection.pvpPrevention().inNether().get())
				return;

			if (event.getSource().getEntity() instanceof Player attacker)
				event.setCanceled(isInSpawnProtection(level, target.blockPosition()) || isInSpawnProtection(level, attacker.blockPosition()));
		}
	}

	private static void onPlayerTickPost(PlayerTickEvent.Post event) {
		Player player = event.getEntity();

		if (player.level() instanceof ServerLevel level) {
			boolean wasInSpawnProtectedArea = player.getTags().contains(IN_SPAWN_PROTECTION_TAG);
			SpawnProtection spawnProtection = Configuration.instance.spawnProtection;

			if (wasInSpawnProtectedArea != isInSpawnProtection(level, player.blockPosition())) {
				boolean isNether = level.dimension().equals(Level.NETHER);

				if (wasInSpawnProtectedArea) {
					player.removeTag(IN_SPAWN_PROTECTION_TAG);

					if (!isNether || spawnProtection.pvpPrevention().inNether().get())
						player.displayClientMessage(Component.translatable("scserverutils.pvp_on").withStyle(ChatFormatting.RED), true);

					if (!isNether || spawnProtection.effects().inNether().get())
						effects.forEach(effect -> player.removeEffect(effect.get().getEffect()));
				}
				else {
					player.addTag(IN_SPAWN_PROTECTION_TAG);

					if (!isNether || spawnProtection.pvpPrevention().inNether().get())
						player.displayClientMessage(Component.translatable("scserverutils.pvp_off").withStyle(ChatFormatting.GREEN), true);

					if (!isNether || spawnProtection.effects().inNether().get())
						effects.forEach(effect -> player.addEffect(effect.get()));
				}
			}
		}
	}

	private static void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
		if (event.getEntity() instanceof Player player && player.getTags().contains(IN_SPAWN_PROTECTION_TAG)) {
			boolean isNether = event.getDimension().equals(Level.NETHER);
			SpawnProtection spawnProtection = Configuration.instance.spawnProtection;

			if (!spawnProtection.pvpPrevention().inNether().get()) {
				if (isNether)
					player.displayClientMessage(Component.translatable("scserverutils.pvp_on").withStyle(ChatFormatting.RED), true);
				else
					player.displayClientMessage(Component.translatable("scserverutils.pvp_off").withStyle(ChatFormatting.GREEN), true);
			}

			if (!spawnProtection.effects().inNether().get()) {
				if (isNether)
					effects.forEach(effect -> player.removeEffect(effect.get().getEffect()));
				else
					effects.forEach(effect -> player.addEffect(effect.get()));
			}
		}
	}

	private static void onFinalizeSpawn(FinalizeSpawnEvent event) {
		ServerLevel level = event.getLevel().getLevel();
		boolean verbose = spawnInfo.verboseLoggingFor().contains(event.getEntity().getType());

		if (verbose) {
			SCServerUtils.LOGGER.info("Spawn type: {}", event.getSpawnType());
			SCServerUtils.LOGGER.info("Is spawn type disallowed: {}", !spawnInfo.allowedSpawnTypes().contains(event.getSpawnType()));
			SCServerUtils.LOGGER.info("Entity spawns at {}", BlockPos.containing(event.getX(), event.getY(), event.getZ()));
		}

		if (isInSpawnProtection(level, BlockPos.containing(event.getX(), event.getY(), event.getZ()), verbose) && !spawnInfo.allowedSpawnTypes().contains(event.getSpawnType())) {
			if (verbose)
				SCServerUtils.LOGGER.info("Cancelling spawn");

			event.setSpawnCancelled(true);
			event.setCanceled(true);
		}
		else if (verbose)
			SCServerUtils.LOGGER.info("Not cancelling spawn");
	}

	private static void onEntityTeleport(EntityTeleportEvent event) {
		Entity entity = event.getEntity();
		Level level = entity.level();
		RiftStabilizer spawnProtectionRiftStabilizer = Configuration.instance.spawnProtection.riftStabilizer();

		if (disallowTeleport(event, level, event.getPrev(), spawnProtectionRiftStabilizer.disallowedTeleportationTypesFromSpawn())
			|| disallowTeleport(event, level, event.getTarget(), spawnProtectionRiftStabilizer.disallowedTeleportationTypesToSpawn())) {
			if (entity instanceof Player player) {
				if (player.hasPermissions(spawnProtectionRiftStabilizer.bypassPermissionLevel().get()))
					return;

				player.displayClientMessage(Component.translatableWithFallback(spawnProtectionRiftStabilizer.langKey().get(), spawnProtectionRiftStabilizer.fallback().get()).withStyle(ChatFormatting.RED), true);
			}

			event.setCanceled(true);
		}
	}

	private static boolean disallowTeleport(EntityTeleportEvent event, Level level, Vec3 posToCheck, ConfigValue<List<? extends String>> disallowedTeleportationTypes) {
		if (isInSpawnProtection(level, BlockPos.containing(posToCheck))) {
			TeleportationType type = TeleportationType.getTypeFromEvent(event);

			return type != null && disallowedTeleportationTypes.get().stream().anyMatch(type.name()::equals);
		}

		return false;
	}

	public static boolean isInSpawnProtection(Level level, BlockPos pos) {
		return isInSpawnProtection(level, pos, false);
	}

	public static boolean isInSpawnProtection(Level level, BlockPos pos, boolean verbose) {
		int radius, xOrigin, zOrigin;

		if (verbose)
			SCServerUtils.LOGGER.info("Spawn protection check in: {}", level.dimension());

		if (level.dimension() == Level.NETHER) {
			Nether netherSpawnProtection = Configuration.instance.spawnProtection.nether();

			if (!netherSpawnProtection.enabled().get()) {
				if (verbose)
					SCServerUtils.LOGGER.info("Spawn protection disabled, returning false");

				return false;
			}

			radius = netherSpawnProtection.radius().get();
			xOrigin = netherSpawnProtection.xOrigin().get();
			zOrigin = netherSpawnProtection.zOrigin().get();
		}
		else if (level.dimension() == Level.OVERWORLD) {
			BlockPos spawnPos = level.getSharedSpawnPos();

			radius = level.getServer().getSpawnProtectionRadius();
			xOrigin = spawnPos.getX();
			zOrigin = spawnPos.getZ();
		}
		else {
			if (verbose)
				SCServerUtils.LOGGER.info("Neither nether nor overworld, returning false");

			return false;
		}

		int adjustedX = Mth.abs(pos.getX() - xOrigin);
		int adjustedZ = Mth.abs(pos.getZ() - zOrigin);
		int max = Math.max(adjustedX, adjustedZ);

		if (verbose) {
			SCServerUtils.LOGGER.info("radius: {}", radius);
			SCServerUtils.LOGGER.info("xOrigin: {}", xOrigin);
			SCServerUtils.LOGGER.info("zOrigin: {}", zOrigin);
			SCServerUtils.LOGGER.info("adjustedX: {}", adjustedX);
			SCServerUtils.LOGGER.info("adjustedZ: {}", adjustedZ);
			SCServerUtils.LOGGER.info("max: {}", max);
			SCServerUtils.LOGGER.info("Returning max <= radius: {}", max <= radius);
		}

		return max <= radius;
	}

	private static void reloadResolvedConfigValues(ModConfigEvent.Reloading event) {
		if (event.getConfig().getSpec() == Configuration.SPEC) {
			SpawnProtection spawnProtection = Configuration.instance.spawnProtection;
			effects = spawnProtection.effects().resolve();
			spawnInfo = spawnProtection.mobSpawning().resolve();
		}
	}
}
