package bl4ckscor3.mod.scserverutils.commands;

import java.util.Optional;

import com.mojang.authlib.properties.PropertyMap;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

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
				.requires(commandSource -> commandSource.hasPermission(permissionLevel))
				.then(Commands.argument("player", StringArgumentType.word())
						.executes(ctx -> {
							//@formatter:on
							ServerPlayer player = ctx.getSource().getPlayerOrException();
							ItemStack head = new ItemStack(Items.PLAYER_HEAD);

							head.set(DataComponents.PROFILE, new ResolvableProfile(Optional.of(ctx.getArgument("player", String.class)), Optional.empty(), new PropertyMap()));
							player.addItem(head);
							return 1;
						})));
	}
}
