package bl4ckscor3.mod.scserverutils.commands;

import bl4ckscor3.mod.scserverutils.SCServerUtils;
import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import bl4ckscor3.mod.scserverutils.configuration.MuteMessages;
import bl4ckscor3.mod.scserverutils.mute.PlayerMuteData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

public class MuteCommand {

    private MuteCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, int permissionLevel) {

        dispatcher.register(Commands.literal("mute")
                .requires(commandSource -> commandSource.hasPermission(permissionLevel))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("reason", StringArgumentType.greedyString()).executes(ctx -> {

                            MuteMessages muteMessages = Configuration.instance.muteMessages;

                            ServerPlayer playerToMute = EntityArgument.getPlayer(ctx, "player");
                            if (SCServerUtils.mutedPlayersUUID.contains(playerToMute.getStringUUID())) {
                                Component messageToOperator = SCServerUtils.parseComponent(ctx.getSource().getPlayerOrException().level(), muteMessages.muteFailedAlreadyMuted().get());
                                ctx.getSource().sendFailure(messageToOperator);
                                return 1;
                            }
                            String reason = StringArgumentType.getString(ctx, "reason");
                            SCServerUtils.playerDataManager.addEntry(new PlayerMuteData(playerToMute.getName().toString(), playerToMute.getStringUUID(), reason));

                            Component messageComponent = SCServerUtils.parseComponent(playerToMute.level(), muteMessages.muteStarts().get());
                            playerToMute.sendSystemMessage(messageComponent);

                            Component messageToOperator = SCServerUtils.parseComponent(ctx.getSource().getPlayerOrException().level(), muteMessages.muteSuccess().get());
                            ctx.getSource().sendSuccess(() -> messageToOperator, true);
                            return 1;
                        }
                ))));

        dispatcher.register(Commands.literal("unmute")
                .requires(source -> source.hasPermission(permissionLevel))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> {

                            MuteMessages muteMessages = Configuration.instance.muteMessages;

                            ServerPlayer playerToUnmute = EntityArgument.getPlayer(ctx, "player");

                            if (!SCServerUtils.mutedPlayersUUID.contains(playerToUnmute.getStringUUID())) {
                                Component messageToOperator = SCServerUtils.parseComponent(ctx.getSource().getPlayerOrException().level(), muteMessages.unmuteFailedNotMuted().get());
                                ctx.getSource().sendFailure(messageToOperator);
                                return 1;
                            }
                            SCServerUtils.playerDataManager.removeEntry(SCServerUtils.playerDataManager.getPlayerDataByUUID(playerToUnmute.getStringUUID()));


                            Component messageComponent = SCServerUtils.parseComponent(playerToUnmute.level(), muteMessages.muteEnds().get());
                            playerToUnmute.sendSystemMessage(messageComponent);

                            Component messageToOperator = SCServerUtils.parseComponent(ctx.getSource().getPlayerOrException().level(), muteMessages.unmuteSuccess().get());
                            ctx.getSource().sendSuccess(() -> messageToOperator, true);
                            return 1;
                        })
                ));
    }
}