package bl4ckscor3.mod.scserverutils.mixin.sdlink;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.hypherionmc.sdlink.api.messaging.discord.DiscordMessage;
import com.hypherionmc.sdlink.server.ServerEvents;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;

import bl4ckscor3.mod.scserverutils.configuration.Configuration;

@Mixin(ServerEvents.class)
public class ServerEventsMixin {
	@WrapWithCondition(method = "commandEvent", at = @At(value = "INVOKE", target = "Lcom/hypherionmc/sdlink/api/messaging/discord/DiscordMessage;sendMessage(Z)V"))
	private boolean scserverutils$inhibitServerCommands(DiscordMessage message, boolean immediately, @Local(name = "username") String username) {
		return Configuration.instance.sdLink.logServerCommands().get() || !username.equals("Server");
	}
}
