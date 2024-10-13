package bl4ckscor3.mod.scserverutils;

import org.slf4j.Logger;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;

import bl4ckscor3.mod.scserverutils.configuration.AutosaveInterval;
import bl4ckscor3.mod.scserverutils.configuration.CommandConfig;
import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import bl4ckscor3.mod.scserverutils.configuration.CustomServerLinks;
import bl4ckscor3.mod.scserverutils.configuration.PhantomSpawns;
import bl4ckscor3.mod.scserverutils.mixin.MinecraftServerAccessor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.protocol.common.ClientboundServerLinksPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

@Mod(SCServerUtils.MODID)
@EventBusSubscriber
public class SCServerUtils {
	public static final String MODID = "scserverutils";
	public static final Logger LOGGER = LogUtils.getLogger();

	public SCServerUtils(IEventBus modEventBus, ModContainer modContainer) {
		modContainer.registerConfig(Type.STARTUP, Configuration.SPEC, "scserverutils-common.toml");

		if (Configuration.instance.deathLog.enabled().get())
			NeoForge.EVENT_BUS.addListener(DeathLogger::onLivingDeath);

		SpawnProtectionHandler.addListeners(modEventBus);
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
	public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			CustomServerLinks customServerLinks = Configuration.instance.customServerLinks;

			if (customServerLinks.enabled().get())
				player.connection.send(new ClientboundServerLinksPacket(customServerLinks.resolve()));
		}
	}

	@SubscribeEvent
	public static void onPlayerSpawnPhantoms(PlayerSpawnPhantomsEvent event) {
		PhantomSpawns phantomSpawns = Configuration.instance.phantomSpawns;

		if (phantomSpawns.enabled().get())
			event.setPhantomsToSpawn(phantomSpawns.min().get() + event.getEntity().level().random.nextInt(phantomSpawns.max().get() + 1));
	}
}
