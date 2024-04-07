package bl4ckscor3.mod.scserverutils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import bl4ckscor3.mod.scserverutils.commands.EnderchestCommand;
import bl4ckscor3.mod.scserverutils.commands.InvseeCommand;
import bl4ckscor3.mod.scserverutils.commands.RulesCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.Mod.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod("scserverutils")
@EventBusSubscriber
public class SCServerUtils {
	public static final SimpleCommandExceptionType NOT_PLAYER_EXCEPTION = new SimpleCommandExceptionType(Component.literal("This command is only accessible for players."));

	public SCServerUtils() {
		//clients don't need the mod installed
		ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> IExtensionPoint.DisplayTest.IGNORESERVERONLY, (remote, isServer) -> true));
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

		EnderchestCommand.register(dispatcher);
		InvseeCommand.register(dispatcher);
		RulesCommand.register(dispatcher);
	}
}
