package bl4ckscor3.mod.scserverutils.commands;

import java.util.Collection;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

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

	private EnderchestCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, int permissionLevel) {
		dispatcher.register(alias("enderchest", permissionLevel));
		dispatcher.register(alias("echest", permissionLevel));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> alias(String name, int permissionLevel) {
		//@formatter:off
		return Commands.literal(name)
				.requires(commandSource -> commandSource.hasPermission(permissionLevel))
				.then(Commands.argument("player", GameProfileArgument.gameProfile())
						.executes(ctx -> {
							//@formatter:on
							CommandSourceStack cmdSource = ctx.getSource();
							ServerPlayer source = cmdSource.getPlayerOrException();
							Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "player");

							if (!profiles.isEmpty()) {
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
									cmdSource.sendSuccess(() -> Component.literal("Player is not online"), true);
							}
							else
								cmdSource.sendSuccess(() -> Component.literal("Couldn't find the given player"), true);

							return 1;
						}));
	}
}
