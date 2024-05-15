package bl4ckscor3.mod.scserverutils.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PlayerHeadItem;

public class PlayerHeadCommand {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, int permissionLevel) {
		//@formatter:off
		dispatcher.register(Commands.literal("playerhead")
				.requires(commandSource -> commandSource.hasPermission(permissionLevel))
				.then(Commands.argument("player", StringArgumentType.word())
						.executes(ctx -> {
							//@formatter:on
							ServerPlayer player = ctx.getSource().getPlayerOrException();
							ItemStack head = new ItemStack(Items.PLAYER_HEAD);

							head.getOrCreateTag().put(PlayerHeadItem.TAG_SKULL_OWNER, NbtUtils.writeGameProfile(new CompoundTag(), new GameProfile(Util.NIL_UUID, ctx.getArgument("player", String.class))));
							player.addItem(head);
							return 1;
						})));
	}
}
