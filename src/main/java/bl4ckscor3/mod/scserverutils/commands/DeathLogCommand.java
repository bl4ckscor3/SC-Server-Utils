package bl4ckscor3.mod.scserverutils.commands;

import java.io.File;
import java.io.IOException;
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
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class DeathLogCommand {
	public static final SimpleCommandExceptionType ERROR_READING_DEATH_LOG = new SimpleCommandExceptionType(Component.literal("There was an error reading the death log from disk."));
	private static final SuggestionProvider<CommandSourceStack> DEATHS = (ctx, builder) -> {
		List<String> suggestions = new ArrayList<>();

		Arrays.stream(DeathLogger.DEATH_LOGS.toFile().listFiles()).filter(File::isDirectory).forEach(dir -> {
			Arrays.stream(dir.listFiles()).filter(file -> file.getName().endsWith(".nbt")).forEach(file -> suggestions.add(dir.getName() + "." + file.getName().replace(".nbt", "")));
		});

		return SharedSuggestionProvider.suggest(suggestions.stream(), builder);
	};

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		//@formatter:off
		dispatcher.register(Commands.literal("deathlog")
				.requires(commandSource -> commandSource.hasPermission(2))
				.then(Commands.argument("death", StringArgumentType.word())
						.suggests(DEATHS)
						.then(Commands.literal("view")
								.executes(DeathLogCommand::viewDeathLog))
						.then(Commands.literal("apply")
								.then(Commands.argument("player", EntityArgument.player())
										.executes(DeathLogCommand::applyDeathInventory)))));
		//@formatter:on
	}

	private static int viewDeathLog(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		try {
			String relativeLogLocation = getLogLocation(ctx);
			CompoundTag death = DeathLogger.getDeath(relativeLogLocation);
			CompoundTag damageSource = death.getCompound("cause");
			CommandSourceStack cmdSource = ctx.getSource();

			sendMessage(cmdSource, "Player UUID: %s", death.getString("uuid"));
			sendMessage(cmdSource, "Damage Source:", "");
			sendMessage(cmdSource, "- Type: %s", damageSource.getString("type"));

			if (damageSource.contains("direct_entity"))
				sendMessage(cmdSource, "- Direct Entity: %s", damageSource.getString("direct_entity"));

			if (damageSource.contains("causing_entity"))
				sendMessage(cmdSource, "- Causing Entity: %s", damageSource.getString("causing_entity"));

			sendMessage(cmdSource, "Position: %s", prettyString(GlobalPos.CODEC.decode(NbtOps.INSTANCE, death.getCompound("position")).get().left().get().getFirst()));
			//TODO: Add inventory here and make it clickable to open a UI

			if (damageSource.contains("respawn_position"))
				sendMessage(cmdSource, "\tRespawn Position: %s", NbtUtils.readBlockPos(death.getCompound("respawn_position")).toShortString());
		}
		catch (IOException e) {
			throw ERROR_READING_DEATH_LOG.create();
		}

		return 1;
	}

	private static int applyDeathInventory(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		try {
			String relativeLogLocation = getLogLocation(ctx);
			CompoundTag death = DeathLogger.getDeath(relativeLogLocation);
			Player player = EntityArgument.getPlayer(ctx, "player");

			SCServerUtils.LOGGER.info("Old inventory: {}", player.getInventory().save(new ListTag()));
			player.getInventory().load(death.getList("inventory", Tag.TAG_COMPOUND));
			ctx.getSource().sendSuccess(() -> Component.translatable("Replaced the inventory of %s with the inventory of death %s", ChatFormatting.GRAY + player.getName().getString(), ChatFormatting.GRAY + relativeLogLocation), true);
		}
		catch (IOException e) {
			throw ERROR_READING_DEATH_LOG.create();
		}

		return 1;
	}

	private static String getLogLocation(CommandContext<CommandSourceStack> ctx) {
		return ctx.getArgument("death", String.class).replaceFirst("\\.", "/") + ".nbt";
	}

	private static void sendMessage(CommandSourceStack cmdSource, String message, String arg) {
		cmdSource.sendSystemMessage(Component.translatable(message, Component.translatable(arg).withStyle(ChatFormatting.GREEN)));
	}

	private static String prettyString(GlobalPos pos) {
		return "In " + ChatFormatting.GOLD + pos.dimension().location() + ChatFormatting.GREEN + " at " + ChatFormatting.GOLD + pos.pos().toShortString();
	}
}
