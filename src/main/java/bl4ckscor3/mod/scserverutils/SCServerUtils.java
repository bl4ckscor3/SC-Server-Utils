package bl4ckscor3.mod.scserverutils;

import org.slf4j.Logger;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;

import bl4ckscor3.mod.scserverutils.commands.EnderchestCommand;
import bl4ckscor3.mod.scserverutils.commands.InvseeCommand;
import bl4ckscor3.mod.scserverutils.commands.RulesCommand;
import bl4ckscor3.mod.scserverutils.configuration.AutosaveInterval;
import bl4ckscor3.mod.scserverutils.configuration.Commands;
import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import bl4ckscor3.mod.scserverutils.mixin.MinecraftServerAccessor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.Mod.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

@Mod("scserverutils")
@EventBusSubscriber
public class SCServerUtils {
	public static final Logger LOGGER = LogUtils.getLogger();
	public static final SimpleCommandExceptionType NOT_PLAYER_EXCEPTION = new SimpleCommandExceptionType(Component.literal("This command is only accessible for players."));

	public SCServerUtils() {
		//clients don't need the mod installed
		ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> IExtensionPoint.DisplayTest.IGNORESERVERONLY, (remote, isServer) -> true));
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
		Commands commandsConfig = Configuration.instance.commands;
		CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

		if (commandsConfig.enderchestEnabled().get())
			EnderchestCommand.register(dispatcher);

		if (commandsConfig.invseeEnabled().get())
			InvseeCommand.register(dispatcher);

		if (commandsConfig.rulesEnabled().get())
			RulesCommand.register(dispatcher);
	}
}
