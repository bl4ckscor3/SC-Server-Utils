package bl4ckscor3.mod.scserverutils.commands;

import bl4ckscor3.mod.scserverutils.SCServerUtils;
import bl4ckscor3.mod.scserverutils.mute.PlayerMuteData;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.awt.*;

public class MuteCommand {

    private MuteCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, int permissionLevel) {

        dispatcher.register(Commands.literal("mute")
                .requires(commandSource -> commandSource.hasPermission(permissionLevel))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("reason", StringArgumentType.greedyString()).executes(ctx -> {
                            // code run by the command
                            ServerPlayer playerToMute = EntityArgument.getPlayer(ctx, "player");
                            if (SCServerUtils.mutedPlayersUUID.contains(playerToMute.getStringUUID())) {
                                // send failed message
                                return 1;
                            }
                            String reason = StringArgumentType.getString(ctx, "reason");
                            SCServerUtils.playerDataManager.addEntry(new PlayerMuteData(playerToMute.getName().toString(), playerToMute.getStringUUID(), reason));

                            // send message to player
                            // send success message
                            return 1;
                        }
                ))));

        dispatcher.register(Commands.literal("unmute")
                .requires(source -> source.hasPermission(permissionLevel))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> {

                            // code run by the command
                            ServerPlayer playerToUnmute = EntityArgument.getPlayer(ctx, "player");

                            if (!SCServerUtils.mutedPlayersUUID.contains(playerToUnmute.getStringUUID())) {
                                // send failure message
                                return 1;
                            }
                            SCServerUtils.playerDataManager.removeEntry(SCServerUtils.playerDataManager.getPlayerDataByUUID(playerToUnmute.getStringUUID()));
                            // send message
                            // send success message
                            return 1;
                        })
                ));
    }
}