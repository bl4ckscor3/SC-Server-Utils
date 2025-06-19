package bl4ckscor3.mod.scserverutils;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ServerChatEvent;

public class ChatMuteHandler {

    public static void register(IEventBus modEventBus) {

    }

    public static void onPlayerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();

        if (player.getTags().contains("muted")) {

            player.sendSystemMessage(Component.literal("§cYou are currently muted, you can't send messages in chat."));
            event.setCanceled(true);
        }
    }
}
