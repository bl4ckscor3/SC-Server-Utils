package bl4ckscor3.mod.scserverutils;

import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingAttackEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class PvpPrevention {
	public static final String IN_SPAWN_PROTECTION_TAG = "in_spawn_protection";

	public static void addListeners() {
		NeoForge.EVENT_BUS.addListener(PvpPrevention::onLivingAttack);
		NeoForge.EVENT_BUS.addListener(PvpPrevention::onPlayerTickPost);
	}

	public static void onLivingAttack(LivingAttackEvent event) {
		if (event.getEntity() instanceof Player target && target.level() instanceof ServerLevel level) {
			if (level.dimension().equals(Level.NETHER) && !Configuration.instance.spawnProtectionPvpPrevention.inNether().get())
				return;

			if (event.getSource().getEntity() instanceof Player attacker) {
				MinecraftServer server = level.getServer();

				if (server.isUnderSpawnProtection(level, target.blockPosition(), target) || server.isUnderSpawnProtection(level, attacker.blockPosition(), attacker))
					event.setCanceled(true);
			}
		}
	}

	public static void onPlayerTickPost(PlayerTickEvent.Post event) {
		Player player = event.getEntity();

		if (player.level() instanceof ServerLevel level) {
			if (level.dimension().equals(Level.NETHER) && !Configuration.instance.spawnProtectionPvpPrevention.inNether().get())
				return;

			boolean wasInSpawnProtectedArea = player.getTags().contains(IN_SPAWN_PROTECTION_TAG);

			if (wasInSpawnProtectedArea != player.getServer().isUnderSpawnProtection(level, player.blockPosition(), player)) {
				if (wasInSpawnProtectedArea) {
					player.displayClientMessage(Component.translatable("scserverutils.pvp_on").withStyle(ChatFormatting.RED), true);
					player.removeTag(IN_SPAWN_PROTECTION_TAG);
				}
				else {
					player.displayClientMessage(Component.translatable("scserverutils.pvp_off").withStyle(ChatFormatting.GREEN), true);
					player.addTag(IN_SPAWN_PROTECTION_TAG);
				}
			}
		}
	}
}
