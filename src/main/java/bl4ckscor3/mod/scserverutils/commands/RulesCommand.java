package bl4ckscor3.mod.scserverutils.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Scoreboard;

public class RulesCommand {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(alias("rule"));
		dispatcher.register(alias("rules"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> alias(String name) {
		//@formatter:off
		return Commands.literal(name)
				.requires(player -> player.hasPermission(0))
				.executes(ctx -> {
					//@formatter:on
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					Scoreboard scoreboard = player.getScoreboard();

					scoreboard.getOrCreatePlayerScore(player, scoreboard.getObjective("rules")).increment();
					return 0;
				});
	}
}
