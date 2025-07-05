package bl4ckscor3.mod.scserverutils.mute;

import bl4ckscor3.mod.scserverutils.SCServerUtils;
import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import bl4ckscor3.mod.scserverutils.configuration.MuteMessages;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.nio.file.Path;
import java.util.Arrays;

import static bl4ckscor3.mod.scserverutils.SCServerUtils.*;

public class MuteEventsHandler {

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player.getTags().contains("suspended")) {
            MuteMessages muteMessages = Configuration.instance.muteMessages;
            Component messageComponent = parseComponent(player.level(), muteMessages.cancelledMessageSuspend().get());
            player.sendSystemMessage(messageComponent);
            logCancelledMessage(player, event.getRawText());
            event.setCanceled(true);
            return;
        }

        if (SCServerUtils.mutedPlayersUUID.contains(player.getStringUUID())) {
            MuteMessages muteMessages = Configuration.instance.muteMessages;
            Component messageComponent = parseComponent(player.level(), muteMessages.cancelledMessageMute().get());
            player.sendSystemMessage(messageComponent);
            logCancelledMessage(player, event.getRawText());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onCommandEvent(CommandEvent event) {

        String fullCmd = event.getParseResults().getReader().getString();
        String[] args = fullCmd.split(" ");

        if (args.length > 0) {
            String cmd = args[0].replace("/", "");

            if (cmd.equalsIgnoreCase("msg") || cmd.equalsIgnoreCase("tell") || cmd.equalsIgnoreCase("w")) {
                try {
                    ServerPlayer sender = event.getParseResults().getContext().getSource().getPlayerOrException();
                    if (SCServerUtils.mutedPlayersUUID.contains(sender.getStringUUID())) {
                        for (ServerPlayer op: getOnlineOperators()) {
                            if (op.getDisplayName().toString().equalsIgnoreCase(args[1])) {
                                op.sendSystemMessage(Component.literal("Note: The following message was sent by a muted player. Since you have operator permission, the message wasn't cancelled."));
                                return;
                            }
                        }
                        logCancelledMessage(sender, String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                        event.setCanceled(true);
                        MuteMessages muteMessages = Configuration.instance.muteMessages;
                        Component messageComponent = parseComponent(sender.level(), muteMessages.cancelledMessageMute().get());
                        sender.sendSystemMessage(messageComponent);

                    } else if (sender.getTags().contains("suspended")) {
                        for (ServerPlayer op: getOnlineOperators()) {
                            if (op.getDisplayName().toString().equalsIgnoreCase(args[1])) {
                                op.sendSystemMessage(Component.literal("Note: The following message was sent by a suspended player. Since you have operator permission, the message wasn't cancelled."));
                                return;
                            }
                        }
                        logCancelledMessage(sender, String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                        event.setCanceled(true);
                        MuteMessages muteMessages = Configuration.instance.muteMessages;
                        Component messageComponent = parseComponent(sender.level(), muteMessages.cancelledMessageSuspend().get());
                        sender.sendSystemMessage(messageComponent);
                    }
                } catch (CommandSyntaxException ignored) {}

            } else if (cmd.equalsIgnoreCase("teammsg") || cmd.equalsIgnoreCase(("say"))) {
                try {
                    ServerPlayer sender = event.getParseResults().getContext().getSource().getPlayerOrException();
                    if (mutedPlayersUUID.contains(sender.getStringUUID())) {
                        logCancelledMessage(sender, String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                        event.setCanceled(true);
                        MuteMessages muteMessages = Configuration.instance.muteMessages;
                        Component messageComponent = parseComponent(sender.level(), muteMessages.cancelledMessageMute().get());
                        sender.sendSystemMessage(messageComponent);
                    } else if (sender.getTags().contains("suspended")) {
                        logCancelledMessage(sender, String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                        event.setCanceled(true);
                        MuteMessages muteMessages = Configuration.instance.muteMessages;
                        Component messageComponent = parseComponent(sender.level(), muteMessages.cancelledMessageSuspend().get());
                        sender.sendSystemMessage(messageComponent);
                    }
                } catch (CommandSyntaxException ignored) {}
            }
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        Path serverPath = event.getServer().getServerDirectory();
        playerDataManager = new PlayerDataManager(serverPath);
        for (PlayerMuteData playerData: playerDataManager.getEntries()) {
            mutedPlayersUUID.add(playerData.uuid);
        }

    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        if (playerDataManager != null) {
            playerDataManager.save();
        }
    }

    @SubscribeEvent
    public static void onWorldSave(LevelEvent.Save event) {
        if (event.getLevel() instanceof ServerLevel && ((ServerLevel) event.getLevel()).dimension() == Level.OVERWORLD) {
            if (playerDataManager != null) {
                playerDataManager.save();
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer p = (ServerPlayer) event.getEntity();

            if ((event.getPlacedBlock().getBlock() instanceof SignBlock) || (event.getPlacedBlock().getBlock() instanceof WallSignBlock))
                if (p.getTags().contains("suspended") || mutedPlayersUUID.contains(p.getStringUUID())) {
                    event.setCanceled(true);
                    event.getLevel().playSound(p, event.getPos(), SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 1.0F, 1.0F);
                    logCancelledMessage(p, "[BLOCK] The placement of a sign by this muted/suspended player was canceled.");
                }
        }
    }
}
