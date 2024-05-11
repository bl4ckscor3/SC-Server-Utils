package bl4ckscor3.mod.scserverutils.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import bl4ckscor3.mod.scserverutils.DeathLogger;
import bl4ckscor3.mod.scserverutils.SCServerUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class DeathLogCommand {
	public static final SimpleCommandExceptionType ERROR_READING_INVENTORY = new SimpleCommandExceptionType(Component.literal("There was an error reading the inventory from disk."));
	private static final SuggestionProvider<CommandSourceStack> DEATHS = (ctx, builder) -> {
		List<String> suggestions = new ArrayList<>();

		Arrays.stream(DeathLogger.DEATH_LOGS.toFile().listFiles()).filter(File::isDirectory).forEach(dir -> {
			Arrays.stream(dir.listFiles()).filter(file -> file.getName().endsWith(".nbt")).forEach(file -> suggestions.add(dir.getName() + "_" + file.getName().replace(".nbt", "")));
		});

		return SharedSuggestionProvider.suggest(suggestions.stream(), builder);
	};

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		//@formatter:off
		dispatcher.register(Commands.literal("deathlog")
				.requires(commandSource -> commandSource.hasPermission(2))
				.then(Commands.argument("death", StringArgumentType.word())
						.suggests(DEATHS)
						.then(Commands.literal("apply")
								.then(Commands.argument("player", EntityArgument.player())
										.executes(DeathLogCommand::applyDeathInventory)))));
		//@formatter:on
	}

	private static int applyDeathInventory(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		try {
			String relativeLogLocation = ctx.getArgument("death", String.class).replaceFirst("_", "/") + ".nbt";
			Path path = DeathLogger.DEATH_LOGS.resolve(relativeLogLocation);
			Player player = EntityArgument.getPlayer(ctx, "player");

			SCServerUtils.LOGGER.info("Old inventory: {}", player.getInventory().save(new ListTag()));
			player.getInventory().load(NbtIo.read(path).getList("inventory", Tag.TAG_COMPOUND));
			ctx.getSource().sendSuccess(() -> Component.translatable("Replaced the inventory of %s with the inventory of death %s", ChatFormatting.GRAY + player.getName().getString(), ChatFormatting.GRAY + relativeLogLocation), true);
		}
		catch (IOException e) {
			throw ERROR_READING_INVENTORY.create();
		}

		return 1;
	}
}
