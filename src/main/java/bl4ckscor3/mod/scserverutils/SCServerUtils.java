package bl4ckscor3.mod.scserverutils;

import bl4ckscor3.mod.scserverutils.configuration.*;
import bl4ckscor3.mod.scserverutils.mute.PlayerDataManager;
import bl4ckscor3.mod.scserverutils.mute.PlayerMuteData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;

import bl4ckscor3.mod.scserverutils.mixin.MinecraftServerAccessor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.protocol.common.ClientboundServerLinksPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Mod(SCServerUtils.MODID)
@EventBusSubscriber
public class SCServerUtils {
	public static final String MODID = "scserverutils";
	public static final Logger LOGGER = LogUtils.getLogger();
	public static PlayerDataManager playerDataManager;
	public static List<String> mutedPlayersUUID = new ArrayList<>();

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

		if (phantomSpawns.enabled().get()) {
			Player player = event.getEntity();

			if (phantomSpawns.disableInSpawnProtection().get() && SpawnProtectionHandler.isInSpawnProtection(player.level(), player.blockPosition()))
				event.setPhantomsToSpawn(0);
			else
				event.setPhantomsToSpawn(player.level().random.nextIntBetweenInclusive(phantomSpawns.min().get(), phantomSpawns.max().get()));
		}
	}

	@SubscribeEvent
	public static void onServerChat(ServerChatEvent event) {
		ServerPlayer player = event.getPlayer();
		if (player.getTags().contains("suspended")) {
			player.sendSystemMessage(Component.literal("§cYou are currently suspended, you can't send messages in chat."));
			event.setCanceled(true);
			return;
		}

		if (mutedPlayersUUID.contains(player.getStringUUID())) {
			//send message (how does this system works with config ?)
			event.setCanceled(true);
			return;
		}
	}

	@SubscribeEvent
	public void onServerStarted(ServerStartedEvent event) {
		ServerLevel overworld = event.getServer().overworld();
		Path worldPath = overworld.getServer().getWorldPath(LevelResource.ROOT); // Récupère le chemin du monde
		playerDataManager = new PlayerDataManager(worldPath);
		for (PlayerMuteData playerData: playerDataManager.getEntries()) {
			mutedPlayersUUID.add(playerData.uuid);
		}

	}

	@SubscribeEvent
	public void onServerStopping(ServerStoppingEvent event) {
		if (playerDataManager != null) {
			playerDataManager.save();
		}
	}

	@SubscribeEvent
	public void onWorldSave(LevelEvent.Save event) {
		if (event.getLevel() instanceof ServerLevel && ((ServerLevel) event.getLevel()).dimension() == Level.OVERWORLD) {
			if (playerDataManager != null) {
				playerDataManager.save();
			}
		}
	}
}
