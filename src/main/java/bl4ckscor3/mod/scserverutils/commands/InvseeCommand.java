package bl4ckscor3.mod.scserverutils.commands;

import java.util.Collection;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import bl4ckscor3.mod.scserverutils.SCServerUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;

public class InvseeCommand {
	private static final Component INVENTORY_CONTAINER_NAME = Component.translatable("container.inventory");

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		//@formatter:off
		dispatcher.register(Commands.literal("invsee")
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
									Inventory playerInv = target.getInventory();

									if (playerInv != null)
										source.openMenu(new SimpleMenuProvider((id, ownInv, player) -> new ChestMenu(MenuType.GENERIC_9x4, id, ownInv, playerInv, 4), Component.literal(targetName + " ").append(INVENTORY_CONTAINER_NAME)));
								}
								else
									cmdSource.sendSuccess(() -> Component.literal("Player is not online."), true);
							}
							else
								cmdSource.sendSuccess(() -> Component.literal("Couldn't find the given player."), true);

							return 1;
						})));
	}
}
