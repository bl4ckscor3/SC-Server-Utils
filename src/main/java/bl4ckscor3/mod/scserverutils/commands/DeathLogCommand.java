package bl4ckscor3.mod.scserverutils.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import bl4ckscor3.mod.scserverutils.DeathInfo;
import bl4ckscor3.mod.scserverutils.DeathInfo.Cause;
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
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class DeathLogCommand {
	public static final SimpleCommandExceptionType ERROR_READING_DEATH_LOG = new SimpleCommandExceptionType(Component.literal("There was an error reading the death log from disk."));
	private static final SuggestionProvider<CommandSourceStack> DEATHS = (ctx, builder) -> {
		List<String> suggestions = new ArrayList<>();

		Arrays.stream(DeathLogger.DEATH_LOGS.toFile().listFiles()).filter(File::isDirectory).forEach(dir -> {
			Arrays.stream(dir.listFiles()).filter(file -> file.getName().endsWith(".nbt")).forEach(file -> suggestions.add(dir.getName() + "." + file.getName().replace(".nbt", "")));
		});

		return SharedSuggestionProvider.suggest(suggestions.stream(), builder);
	};

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, int permissionLevel) {
		//@formatter:off
		dispatcher.register(Commands.literal("deathlog")
				.requires(commandSource -> commandSource.hasPermission(permissionLevel))
				.then(Commands.argument("death", StringArgumentType.word())
						.suggests(DEATHS)
						.then(Commands.literal("view")
								.executes(DeathLogCommand::viewDeathLog))
						.then(Commands.literal("info")
								.executes(DeathLogCommand::showDeathLogInfo))
						.then(Commands.literal("apply")
								.then(Commands.argument("player", EntityArgument.player())
										.executes(DeathLogCommand::applyDeathInventory)))));
		//@formatter:on
	}

	private static int viewDeathLog(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		try {
			String relativeLogLocation = getLogLocation(ctx);
			DeathInfo deathInfo = DeathInfo.CODEC.decode(NbtOps.INSTANCE, DeathLogger.getDeath(relativeLogLocation)).get().orThrow().getFirst();

			ctx.getSource().getPlayerOrException().openMenu(new SimpleMenuProvider((id, ownInv, player) -> new ChestMenu(MenuType.GENERIC_9x5, id, ownInv, createInventoryForChestMenu(deathInfo), 5) {
				@Override
				public boolean stillValid(Player player) {
					return true;
				}
			}, Component.literal(relativeLogLocation)));
		}
		catch (IOException e) {
			throw ERROR_READING_DEATH_LOG.create();
		}

		return 1;
	}

	private static int showDeathLogInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		try {
			String relativeLogLocation = getLogLocation(ctx);
			DeathInfo deathInfo = DeathInfo.CODEC.decode(NbtOps.INSTANCE, DeathLogger.getDeath(relativeLogLocation)).get().orThrow().getFirst();
			Cause cause = deathInfo.cause();
			GlobalPos position = deathInfo.position();
			CommandSourceStack cmdSource = ctx.getSource();
			MutableComponent viewInventoryText = Component.translatable("[%s]", Component.translatable("View Inventory").withStyle(ChatFormatting.LIGHT_PURPLE));

			cmdSource.sendSystemMessage(Component.translatable(relativeLogLocation + ":").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
			sendMessage(cmdSource, "Player UUID: %s", deathInfo.uuid(), style -> clickToCopy(style, deathInfo.uuid()));
			sendMessage(cmdSource, "Damage Source:", "");
			sendMessage(cmdSource, "- Type: %s", cause.type().toString());
			cause.directEntity().ifPresent(directEntity -> sendMessage(cmdSource, "- Direct Entity: %s", directEntity.toString()));
			cause.causingEntity().ifPresent(causingEntity -> sendMessage(cmdSource, "- Causing Entity: %s", causingEntity.toString()));
			sendMessage(cmdSource, "Position: %s", "In " + ChatFormatting.GOLD + position.dimension().location() + ChatFormatting.GREEN + " at " + ChatFormatting.GOLD + position.pos().toShortString(), style -> clickToTeleportToPosition(style, position));
			deathInfo.respawnPosition().ifPresent(respawnPosition -> sendMessage(cmdSource, "Respawn Position: %s", respawnPosition.toShortString()));
			viewInventoryText.setStyle(viewInventoryText.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/deathlog %s view", ctx.getArgument("death", String.class)))));
			cmdSource.sendSystemMessage(viewInventoryText);
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

	private static Style clickToCopy(Style style, String copyThis) {
		style = style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, copyThis));
		return style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy UUID")));
	}

	private static Style clickToTeleportToPosition(Style style, GlobalPos pos) {
		style = style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/execute in %s run tp @s %s", pos.dimension().location(), pos.pos().toShortString().replace(",", ""))));
		return style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to teleport to this position")));
	}

	private static void sendMessage(CommandSourceStack cmdSource, String message, String arg) {
		sendMessage(cmdSource, message, arg, UnaryOperator.identity());
	}

	private static void sendMessage(CommandSourceStack cmdSource, String message, String arg, UnaryOperator<Style> style) {
		MutableComponent component = Component.translatable(arg);

		component.setStyle(style.apply(component.getStyle().withColor(ChatFormatting.GREEN)));
		cmdSource.sendSystemMessage(Component.translatable(message, component));
	}

	private static Container createInventoryForChestMenu(DeathInfo deathInfo) {
		ItemStack[] stacks = new ItemStack[45];
		ListTag inventory = deathInfo.inventory();

		Arrays.fill(stacks, ItemStack.EMPTY);

		for (int i = 0; i < inventory.size(); i++) {
			CompoundTag entry = inventory.getCompound(i);
			int slot = entry.getByte("Slot") & 255;

			//offhand
			if (slot >= 150)
				slot = slot - 150 + 40; //150 is the offset the slot gets saved at, and there are 36 inventory + 4 armor slots before the offhand slot
			//armor
			else if (slot >= 100)
				slot = slot - 100 + 36; //100 is the offset the slots get saved at, and there are 36 inventory slots before the armor slots

			stacks[slot] = ItemStack.of(entry);
		}

		return new SimpleContainer(stacks);
	}
}
