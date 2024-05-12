package bl4ckscor3.mod.scserverutils;

import org.slf4j.Logger;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;

import bl4ckscor3.mod.scserverutils.configuration.AutosaveInterval;
import bl4ckscor3.mod.scserverutils.configuration.CommandConfig;
import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import bl4ckscor3.mod.scserverutils.configuration.PhantomSpawns;
import bl4ckscor3.mod.scserverutils.mixin.MinecraftServerAccessor;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.Mod.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

@Mod(SCServerUtils.MODID)
@EventBusSubscriber
public class SCServerUtils {
	public static final String MODID = "scserverutils";
	public static final Logger LOGGER = LogUtils.getLogger();

	public SCServerUtils() {
		//clients don't need the mod installed
		ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> IExtensionPoint.DisplayTest.IGNORESERVERONLY, (remote, isServer) -> true));

		if (Configuration.instance.deathLog.enabled().get())
			NeoForge.EVENT_BUS.addListener(DeathLogger::onLivingDeath);
	}

	@SubscribeEvent
	public static void onServerAboutToStart(ServerAboutToStartEvent event) {
		AutosaveInterval autosaveInterval = Configuration.instance.autosaveInterval;

		if (autosaveInterval.enabled().get()) {
			int interval = autosaveInterval.interval().get();

			((MinecraftServerAccessor) event.getServer()).setTicksUntilAutosave(20 * interval);
			LOGGER.info("Autosave interval set to {} seconds", interval);
		}
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

		for (CommandConfig commandConfig : Configuration.instance.commands) {
			if (commandConfig.enabled().get())
				commandConfig.registrar().get().accept(dispatcher, commandConfig.permissionLevel().get());
		}
	}

	@SubscribeEvent
	public static void onPlayerSpawnPhantoms(PlayerSpawnPhantomsEvent event) {
		PhantomSpawns phantomSpawns = Configuration.instance.phantomSpawns;

		if (phantomSpawns.enabled().get())
			event.setPhantomsToSpawn(phantomSpawns.min().get() + event.getEntity().level().random.nextInt(phantomSpawns.max().get() + 1));
	}
}
