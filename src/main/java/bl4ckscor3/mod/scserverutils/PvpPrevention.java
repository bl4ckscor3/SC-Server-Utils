package bl4ckscor3.mod.scserverutils;

import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

public class PvpPrevention {
	public static void onAttackEntity(AttackEntityEvent event) {
		if (event.getTarget() instanceof Player target && target.level() instanceof ServerLevel level) {
			if (level.dimension().equals(Level.NETHER) && !Configuration.instance.spawnProtectionPvpPrevention.inNether().get())
				return;

			Player attacker = event.getEntity();
			MinecraftServer server = level.getServer();

			if (server.isUnderSpawnProtection(level, target.blockPosition(), target) || server.isUnderSpawnProtection(level, attacker.blockPosition(), attacker))
				event.setCanceled(true);
		}
	}
}
