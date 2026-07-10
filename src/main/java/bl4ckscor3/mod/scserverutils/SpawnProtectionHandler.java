package bl4ckscor3.mod.scserverutils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import bl4ckscor3.mod.scserverutils.configuration.spawnprotection.Dimension;
import bl4ckscor3.mod.scserverutils.configuration.spawnprotection.MobSpawning;
import bl4ckscor3.mod.scserverutils.configuration.spawnprotection.RiftStabilizer;
import bl4ckscor3.mod.scserverutils.configuration.spawnprotection.SpawnProtection;
import net.geforcemods.securitycraft.blockentities.RiftStabilizerBlockEntity.TeleportationType;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.parsing.packrat.commands.CommandArgumentParser;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class SpawnProtectionHandler {
	public static final String IN_SPAWN_PROTECTION_TAG = "in_spawn_protection";
	private static List<Supplier<MobEffectInstance>> effects = new ArrayList<>();
	private static MobSpawning.Info spawnInfo = new MobSpawning.Info(List.of(), "", List.of(), List.of());

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

		NeoForge.EVENT_BUS.addListener(SpawnProtectionHandler::onEntityMobGriefing);
		NeoForge.EVENT_BUS.addListener(SpawnProtectionHandler::onExplosionDetonate);
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
			boolean wasInSpawnProtectedArea = player.entityTags().contains(IN_SPAWN_PROTECTION_TAG);
			SpawnProtection spawnProtection = Configuration.instance.spawnProtection;

			if (wasInSpawnProtectedArea != isInSpawnProtection(level, player.blockPosition())) {
				boolean isNether = level.dimension().equals(Level.NETHER);

				if (wasInSpawnProtectedArea) {
					Component message = parseComponent(level, spawnProtection.messages().leave().get());

					player.sendOverlayMessage(message);
					player.removeTag(IN_SPAWN_PROTECTION_TAG);

					if (!isNether || spawnProtection.effects().inNether().get())
						effects.forEach(effect -> player.removeEffect(effect.get().getEffect()));
				}
				else {
					Component message = parseComponent(level, spawnProtection.messages().enter().get());

					player.sendOverlayMessage(message);
					player.addTag(IN_SPAWN_PROTECTION_TAG);

					if (!isNether || spawnProtection.effects().inNether().get())
						effects.forEach(effect -> player.addEffect(effect.get()));
				}
			}
		}
	}

	private static void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
		if (event.getEntity() instanceof Player player && player.entityTags().contains(IN_SPAWN_PROTECTION_TAG)) {
			boolean isNether = event.getDimension().equals(Level.NETHER);
			SpawnProtection spawnProtection = Configuration.instance.spawnProtection;

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
		Mob entity = event.getEntity();
		EntityType<?> entityType = entity.getType();
		boolean hasBypassTag = entity.entityTags().contains(spawnInfo.bypassTag());
		boolean allowedSpawnType = spawnInfo.allowedSpawnTypes().contains(event.getSpawnType());
		boolean allowedEntityType = spawnInfo.allowedEntityTypes().contains(entityType);
		boolean verbose = spawnInfo.verboseLoggingFor().contains(entityType);
		BlockPos spawnAt = BlockPos.containing(event.getX(), event.getY(), event.getZ());

		if (verbose) {
			SCServerUtils.LOGGER.info("Spawn type: {}", event.getSpawnType());
			SCServerUtils.LOGGER.info("Is spawn type allowed: {}", allowedSpawnType);
			SCServerUtils.LOGGER.info("Entity to spawn: {}", entity);
			SCServerUtils.LOGGER.info("Entity spawns at {}", spawnAt);
			SCServerUtils.LOGGER.info("Is entity type allowed: {}", allowedEntityType);
			SCServerUtils.LOGGER.info("Has bypass tag: {}", hasBypassTag);
		}

		if (!hasBypassTag && isInSpawnProtection(level, spawnAt, verbose)) {
			if (!allowedSpawnType && !allowedEntityType) {
				if (verbose)
					SCServerUtils.LOGGER.info("Cancelling spawn");

				event.setSpawnCancelled(true);
				event.setCanceled(true);
				return;
			}
		}

		if (verbose)
			SCServerUtils.LOGGER.info("Not cancelling spawn");
	}

	private static void onEntityTeleport(EntityTeleportEvent event) {
		Entity entity = event.getEntity();
		Level level = entity.level();
		RiftStabilizer spawnProtectionRiftStabilizer = Configuration.instance.spawnProtection.riftStabilizer();

		if (disallowTeleport(event, level, event.getPrev(), spawnProtectionRiftStabilizer.disallowedTeleportationTypesFromSpawn())
			|| disallowTeleport(event, level, event.getTarget(), spawnProtectionRiftStabilizer.disallowedTeleportationTypesToSpawn())) {
			if (entity instanceof Player player) {
				if (player.permissions().hasPermission(SCServerUtils.getPermissionFromLevel(spawnProtectionRiftStabilizer.bypassPermissionLevel().get())))
					return;

				Component message = parseComponent(level, spawnProtectionRiftStabilizer.message().get());

				player.sendOverlayMessage(message);
			}

			event.setCanceled(true);
		}
	}

	private static void onEntityMobGriefing(EntityMobGriefingEvent event) {
		Entity entity = event.getEntity();

		if (isInSpawnProtection(entity.level(), entity.blockPosition()) && event.canGrief())
			event.setCanGrief(Configuration.instance.spawnProtection.mobGriefing().get());
	}

	private static void onExplosionDetonate(ExplosionEvent.Detonate event) {
		if (Configuration.instance.spawnProtection.mobGriefing().get())
			return;

		Level level = event.getLevel();
		List<BlockPos> toRemove = new ArrayList<>();

		for (BlockPos pos : event.getAffectedBlocks()) {
			if (isInSpawnProtection(level, pos))
				toRemove.add(pos);
		}

		event.getAffectedBlocks().removeAll(toRemove);
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

		Dimension dimensionSpawnProtection = null;

		if (level.dimension() == Level.NETHER)
			dimensionSpawnProtection = Configuration.instance.spawnProtection.nether();
		else if (level.dimension() == Level.END)
			dimensionSpawnProtection = Configuration.instance.spawnProtection.end();

		if (dimensionSpawnProtection != null) {
			if (!dimensionSpawnProtection.enabled().get()) {
				if (verbose)
					SCServerUtils.LOGGER.info("Spawn protection disabled, returning false");

				return false;
			}

			radius = dimensionSpawnProtection.radius().get();
			xOrigin = dimensionSpawnProtection.xOrigin().get();
			zOrigin = dimensionSpawnProtection.zOrigin().get();
		}
		else if (level.dimension() == Level.OVERWORLD) {
			BlockPos spawnPos = level.getRespawnData().pos();

			if (level.getServer() instanceof DedicatedServer server)
				radius = server.spawnProtectionRadius();
			else
				radius = 0;

			xOrigin = spawnPos.getX();
			zOrigin = spawnPos.getZ();
		}
		else {
			if (verbose)
				SCServerUtils.LOGGER.info("No known dimension with spawn protection, returning false");

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

	private static Component parseComponent(Level level, String message) {
		CommandArgumentParser<Component> parser = ComponentArgument.TAG_PARSER.withCodec(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), ComponentArgument.TAG_PARSER, ComponentSerialization.CODEC, ComponentArgument.ERROR_INVALID_COMPONENT);

		try {
			return parser.parseForCommands(new StringReader(message));
		}
		catch (CommandSyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}
