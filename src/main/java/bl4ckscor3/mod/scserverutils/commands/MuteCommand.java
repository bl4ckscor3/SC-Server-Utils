package bl4ckscor3.mod.scserverutils.commands;

import bl4ckscor3.mod.scserverutils.SCServerUtils;
import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import bl4ckscor3.mod.scserverutils.configuration.MuteMessages;
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

import java.util.Collection;

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

                            Component messageComponent = SCServerUtils.parseComponent(playerToMute.level(), String.format(muteMessages.muteStarts().get(), reason));
                            playerToMute.sendSystemMessage(messageComponent);

                            Component messageToOperator = SCServerUtils.parseComponent(ctx.getSource().getLevel(), String.format(muteMessages.muteSuccess().get(), playerToMute.getName().getString()));
                            ctx.getSource().sendSuccess(() -> messageToOperator, true);
                            return 1;
                        }
                ))));

        dispatcher.register(Commands.literal("unmute")
                .requires(source -> source.hasPermission(permissionLevel))
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .executes(ctx -> {

                            MuteMessages muteMessages = Configuration.instance.muteMessages;

                            Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
                            if (!profiles.isEmpty()) {

                                GameProfile targetProfile = profiles.iterator().next();
                                ServerPlayer playerToUnmute = ctx.getSource().getServer().getPlayerList().getPlayer(targetProfile.getId());


                                if (!SCServerUtils.mutedPlayersUUID.contains(playerToUnmute.getStringUUID())) {
                                    Component messageToOperator = SCServerUtils.parseComponent(ctx.getSource().getPlayerOrException().level(), muteMessages.unmuteFailedNotMuted().get());
                                    ctx.getSource().sendFailure(messageToOperator);
                                    return 1;
                                }
                                SCServerUtils.playerDataManager.removeEntry(SCServerUtils.playerDataManager.getPlayerDataByUUID(playerToUnmute.getStringUUID()));


                                Component messageComponent = SCServerUtils.parseComponent(playerToUnmute.level(), muteMessages.muteEnds().get());
                                playerToUnmute.sendSystemMessage(messageComponent);

                                Component messageToOperator = SCServerUtils.parseComponent(ctx.getSource().getLevel(), String.format(muteMessages.unmuteSuccess().get(), playerToUnmute.getName().getString()));
                                ctx.getSource().sendSuccess(() -> messageToOperator, true);
                                return 1;
                            } else {
                                ctx.getSource().sendFailure(Component.literal("No player with this username was found."));
                                return 1;
                            }
                        })
                ));
    }
}