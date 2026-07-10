package bl4ckscor3.mod.scserverutils.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import bl4ckscor3.mod.scserverutils.SCServerUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Scoreboard;

public class RulesCommand {
	private RulesCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, int permissionLevel) {
		dispatcher.register(alias("rule", permissionLevel));
		dispatcher.register(alias("rules", permissionLevel));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> alias(String name, int permissionLevel) {
		//@formatter:off
		return Commands.literal(name)
				.requires(SCServerUtils.getPermissionCheckFromLevel(permissionLevel))
				.executes(ctx -> {
					//@formatter:on
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					Scoreboard scoreboard = player.level().getScoreboard();

					scoreboard.getOrCreatePlayerScore(player, scoreboard.getObjective("rules")).increment();
					return 0;
				});
	}
}
