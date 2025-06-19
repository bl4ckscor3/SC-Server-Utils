package bl4ckscor3.mod.scserverutils.commands;

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
                        .executes(ctx -> {

                            // code run by the command
                            ServerPlayer playerToMute = EntityArgument.getPlayer(ctx, "player");
                            if (playerToMute.getTags().contains("muted")) {
                                // player already muted
                                ctx.getSource().sendFailure(Component.literal("§cThis player is already muted. Use /unmute to remove the punishment."));
                                return 1;
                            }
                            playerToMute.addTag("muted");
                            playerToMute.sendSystemMessage(Component.literal("§cYou were muted my an operator. You can't send messages in chat anymore, until your punishment is revoked."));
                            ctx.getSource().sendSuccess(() -> Component.literal("&a" + playerToMute.getName().toString() + " was successfully muted."), true);

                            return 1;
                        }).then(Commands.argument("reason", StringArgumentType.greedyString()).executes(ctx -> {
                            ServerPlayer playerToMute = EntityArgument.getPlayer(ctx, "player");
                            String reason = StringArgumentType.getString(ctx, "reason");
                            playerToMute.sendSystemMessage(Component.literal("§cReason: " + reason + "."));
                            return 1;
                        }))));
    }
}