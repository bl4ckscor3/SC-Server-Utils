package bl4ckscor3.mod.scserverutils;

import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import bl4ckscor3.mod.scserverutils.configuration.AutosaveInterval;
import bl4ckscor3.mod.scserverutils.configuration.CommandConfig;
import bl4ckscor3.mod.scserverutils.configuration.CustomServerLinks;
import bl4ckscor3.mod.scserverutils.configuration.PhantomSpawns;
import bl4ckscor3.mod.scserverutils.mute.MuteEventsHandler;
import bl4ckscor3.mod.scserverutils.mute.PlayerDataManager;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.ServerOpList;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.util.parsing.packrat.commands.CommandArgumentParser;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Mod(SCServerUtils.MODID)
@EventBusSubscriber
public class SCServerUtils {
	public static final String MODID = "scserverutils";
	public static final Logger LOGGER = LogUtils.getLogger();
	public static PlayerDataManager playerDataManager;
	public static List<String> mutedPlayersUUID = new ArrayList<>();

	public SCServerUtils(IEventBus modEventBus, ModContainer modContainer) {
		modContainer.registerConfig(Type.STARTUP, Configuration.SPEC, "scserverutils-common.toml");

		NeoForge.EVENT_BUS.register(MuteEventsHandler.class);

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

	public static List<ServerPlayer> getOnlineOperators() {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

		List<ServerPlayer> onlinePlayers = server.getPlayerList().getPlayers();

		ServerOpList opList = server.getPlayerList().getOps();

		return onlinePlayers.stream()
				.filter(player -> {
					ServerOpListEntry entry = opList.get(player.getGameProfile());
					return entry != null && entry.getLevel() >= 1; // niveau OP ≥ 1
				})
				.collect(Collectors.toList());
	}

	public static void logCancelledMessage(ServerPlayer player, String message) {
		final String logMessage = "[CANCELLED] <" + player.getDisplayName().getString() + "> " + message;
		LOGGER.info(logMessage);

		for (ServerPlayer p: getOnlineOperators()) {
			p.sendSystemMessage(Component.literal(logMessage));
		}
	}

	public static Component parseComponent(Level level, String message) {
		CommandArgumentParser<Component> parser = ComponentArgument.TAG_PARSER.withCodec(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), ComponentArgument.TAG_PARSER, ComponentSerialization.CODEC, ComponentArgument.ERROR_INVALID_COMPONENT);

		try {
			return parser.parseForCommands(new StringReader(message));
		}
		catch (CommandSyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}
