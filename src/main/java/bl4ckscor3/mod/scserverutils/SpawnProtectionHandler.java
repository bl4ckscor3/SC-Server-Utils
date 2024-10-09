package bl4ckscor3.mod.scserverutils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import bl4ckscor3.mod.scserverutils.configuration.NetherSpawnProtection;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class SpawnProtectionHandler {
	public static final String IN_SPAWN_PROTECTION_TAG = "in_spawn_protection";
	private static List<Supplier<MobEffectInstance>> effects = new ArrayList<>();

	public static void addListeners(IEventBus modEventBus) {
		Configuration config = Configuration.instance;
		boolean pvpPreventionEnabled = config.spawnProtectionPvpPrevention.enabled().get();
		boolean effectsEnabled = config.spawnProtectionEffects.enabled().get();

		if (pvpPreventionEnabled || effectsEnabled)
			NeoForge.EVENT_BUS.addListener(SpawnProtectionHandler::onPlayerTickPost);

		if (pvpPreventionEnabled)
			NeoForge.EVENT_BUS.addListener(SpawnProtectionHandler::onLivingIncomingDamage);

		if (effectsEnabled) {
			effects = config.spawnProtectionEffects.resolve();
			modEventBus.addListener(SpawnProtectionHandler::reloadEffects);
		}

		if (config.noSpawnProtectionSpawns.enabled().get())
			NeoForge.EVENT_BUS.addListener(SpawnProtectionHandler::onFinalizeSpawn);
	}

	private static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
		if (event.getEntity() instanceof Player target && target.level() instanceof ServerLevel level) {
			if (level.dimension().equals(Level.NETHER) && !Configuration.instance.spawnProtectionPvpPrevention.inNether().get())
				return;

			if (event.getSource().getEntity() instanceof Player attacker)
				event.setCanceled(isInSpawnProtection(level, target.blockPosition()) || isInSpawnProtection(level, attacker.blockPosition()));
		}
	}

	private static void onPlayerTickPost(PlayerTickEvent.Post event) {
		Player player = event.getEntity();

		if (player.level() instanceof ServerLevel level) {
			if (level.dimension().equals(Level.NETHER) && !Configuration.instance.spawnProtectionPvpPrevention.inNether().get())
				return;

			boolean wasInSpawnProtectedArea = player.getTags().contains(IN_SPAWN_PROTECTION_TAG);

			if (wasInSpawnProtectedArea != isInSpawnProtection(level, player.blockPosition())) {
				if (wasInSpawnProtectedArea) {
					player.displayClientMessage(Component.translatable("scserverutils.pvp_on").withStyle(ChatFormatting.RED), true);
					player.removeTag(IN_SPAWN_PROTECTION_TAG);
					effects.forEach(effect -> player.removeEffect(effect.get().getEffect()));
				}
				else {
					player.displayClientMessage(Component.translatable("scserverutils.pvp_off").withStyle(ChatFormatting.GREEN), true);
					player.addTag(IN_SPAWN_PROTECTION_TAG);
					effects.forEach(effect -> player.addEffect(effect.get()));
				}
			}
		}
	}

	private static void onFinalizeSpawn(FinalizeSpawnEvent event) {
		if (event.getLevel() instanceof ServerLevel level && isInSpawnProtection(level, BlockPos.containing(event.getX(), event.getY(), event.getZ()))) {
			event.setSpawnCancelled(true);
			event.setCanceled(true);
		}
	}

	public static boolean isInSpawnProtection(ServerLevel level, BlockPos pos) {
		int radius, xOrigin, zOrigin;

		if (level.dimension() == Level.NETHER) {
			NetherSpawnProtection netherSpawnProtection = Configuration.instance.netherSpawnProtection;

			if (!netherSpawnProtection.enabled().get())
				return false;

			radius = netherSpawnProtection.radius().get();
			xOrigin = netherSpawnProtection.xOrigin().get();
			zOrigin = netherSpawnProtection.zOrigin().get();
		}
		else {
			BlockPos spawnPos = level.getSharedSpawnPos();

			radius = level.getServer().getSpawnProtectionRadius();
			xOrigin = spawnPos.getX();
			zOrigin = spawnPos.getZ();
		}

		int adjustedX = Mth.abs(pos.getX() - xOrigin);
		int adjustedZ = Mth.abs(pos.getZ() - zOrigin);

		return Math.max(adjustedX, adjustedZ) <= radius;
	}

	private static void reloadEffects(ModConfigEvent.Reloading event) {
		if (event.getConfig().getSpec() == Configuration.SPEC)
			effects = Configuration.instance.spawnProtectionEffects.resolve();
	}
}
