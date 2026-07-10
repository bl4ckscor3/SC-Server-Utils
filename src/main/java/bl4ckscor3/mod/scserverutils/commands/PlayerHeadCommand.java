package bl4ckscor3.mod.scserverutils.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import bl4ckscor3.mod.scserverutils.SCServerUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

public class PlayerHeadCommand {
	private PlayerHeadCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, int permissionLevel) {
		//@formatter:off
		dispatcher.register(Commands.literal("playerhead")
				.requires(SCServerUtils.getPermissionCheckFromLevel(permissionLevel))
				.then(Commands.argument("player", StringArgumentType.word())
						.executes(ctx -> {
							//@formatter:on
							ServerPlayer player = ctx.getSource().getPlayerOrException();
							ItemStack head = new ItemStack(Items.PLAYER_HEAD);

							head.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(ctx.getArgument("player", String.class)));
							player.addItem(head);
							return 1;
						})));
	}
}
