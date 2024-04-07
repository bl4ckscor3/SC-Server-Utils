package bl4ckscor3.mod.scserverutils.commands;

import java.util.Collection;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import bl4ckscor3.mod.scserverutils.SCServerUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.PlayerEnderChestContainer;

public class EnderchestCommand {
	private static final Component ENDER_CHEST_CONTAINER_NAME = Component.translatable("container.enderchest");

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(alias("enderchest"));
		dispatcher.register(alias("echest"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> alias(String name) {
		//@formatter:off
		return Commands.literal(name)
				.requires(commandSource -> commandSource.hasPermission(2))
				.then(Commands.argument("player", GameProfileArgument.gameProfile())
						.executes(ctx -> {
							//@formatter:on
							CommandSourceStack cmdSource = ctx.getSource();

							try {
								cmdSource.getEntityOrException();
							}
							catch (CommandSyntaxException e) {
								throw SCServerUtils.NOT_PLAYER_EXCEPTION.create();
							}

							Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "player");

							if (!profiles.isEmpty()) {
								ServerPlayer source = cmdSource.getPlayerOrException();
								GameProfile targetProfile = profiles.iterator().next();
								ServerPlayer target = source.getServer().getPlayerList().getPlayer(targetProfile.getId());
								String targetName = targetProfile.getName();

								targetName += targetName.endsWith("s") ? "'" : "'s";

								if (target != null) {
									PlayerEnderChestContainer enderChestInv = target.getEnderChestInventory();

									if (enderChestInv != null) {
										source.openMenu(new SimpleMenuProvider((id, ownInv, player) -> new ChestMenu(MenuType.GENERIC_9x3, id, ownInv, enderChestInv, 3) {
											@Override
											public boolean stillValid(Player player) {
												return true;
											}
										}, Component.literal(targetName + " ").append(ENDER_CHEST_CONTAINER_NAME)));
									}
								}
								else
									cmdSource.sendSuccess(() -> Component.literal("Player is not online."), true);
							}
							else
								cmdSource.sendSuccess(() -> Component.literal("Couldn't find the given player."), true);

							return 1;
						}));
	}
}
