package bl4ckscor3.mod.scserverutils.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class UnmuteCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, int permissionLevel) {
        dispatcher.register(Commands.literal("unmute")
                .requires(source -> source.hasPermission(permissionLevel))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> {

                            // code run by the command
                            ServerPlayer playerToUnmute = EntityArgument.getPlayer(ctx, "player");

                            if (!playerToUnmute.getTags().contains("muted")) {
                                // the player isn't muted
                                ctx.getSource().sendFailure(Component.literal("§cThis player is not muted."));
                                return 1;
                            }

                            playerToUnmute.removeTag("muted");
                            playerToUnmute.sendSystemMessage(Component.literal("§cYou were unmuted my an operator. You can now send messages in chat. Don't break the rules anymore!"));

                            ctx.getSource().sendSuccess(() -> Component.literal("&a" + playerToUnmute.getName().toString() + " was successfully unmuted."), true);
                            return 1;
                        })
                )
        );
    }
}
