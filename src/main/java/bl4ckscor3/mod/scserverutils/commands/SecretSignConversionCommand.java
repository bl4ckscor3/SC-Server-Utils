package bl4ckscor3.mod.scserverutils.commands;

import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import bl4ckscor3.mod.scserverutils.SCServerUtils;
import net.geforcemods.securitycraft.SCContent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.FillCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;

public class SecretSignConversionCommand {
	private static final SimpleCommandExceptionType ERROR_FILL_FAILED = new SimpleCommandExceptionType(Component.translatableWithFallback("commands.securitycraft.convert.fill.failed", "There are no convertible blocks in the given area"));
	private static final BiMap<Block, Block> SIGN_MAP = HashBiMap.create(Map.ofEntries(
		Map.entry(Blocks.OAK_SIGN, SCContent.SECRET_OAK_SIGN.get()),
		Map.entry(Blocks.SPRUCE_SIGN, SCContent.SECRET_SPRUCE_SIGN.get()),
		Map.entry(Blocks.BIRCH_SIGN, SCContent.SECRET_BIRCH_SIGN.get()),
		Map.entry(Blocks.JUNGLE_SIGN, SCContent.SECRET_JUNGLE_SIGN.get()),
		Map.entry(Blocks.ACACIA_SIGN, SCContent.SECRET_ACACIA_SIGN.get()),
		Map.entry(Blocks.DARK_OAK_SIGN, SCContent.SECRET_DARK_OAK_SIGN.get()),
		Map.entry(Blocks.MANGROVE_SIGN, SCContent.SECRET_MANGROVE_SIGN.get()),
		Map.entry(Blocks.CHERRY_SIGN, SCContent.SECRET_CHERRY_SIGN.get()),
		Map.entry(Blocks.PALE_OAK_SIGN, SCContent.SECRET_PALE_OAK_SIGN.get()),
		Map.entry(Blocks.BAMBOO_SIGN, SCContent.SECRET_BAMBOO_SIGN.get()),
		Map.entry(Blocks.CRIMSON_SIGN, SCContent.SECRET_CRIMSON_SIGN.get()),
		Map.entry(Blocks.WARPED_SIGN, SCContent.SECRET_WARPED_SIGN.get()),
		Map.entry(Blocks.OAK_WALL_SIGN, SCContent.SECRET_OAK_WALL_SIGN.get()),
		Map.entry(Blocks.SPRUCE_WALL_SIGN, SCContent.SECRET_SPRUCE_WALL_SIGN.get()),
		Map.entry(Blocks.BIRCH_WALL_SIGN, SCContent.SECRET_BIRCH_WALL_SIGN.get()),
		Map.entry(Blocks.JUNGLE_WALL_SIGN, SCContent.SECRET_JUNGLE_WALL_SIGN.get()),
		Map.entry(Blocks.ACACIA_WALL_SIGN, SCContent.SECRET_ACACIA_WALL_SIGN.get()),
		Map.entry(Blocks.DARK_OAK_WALL_SIGN, SCContent.SECRET_DARK_OAK_WALL_SIGN.get()),
		Map.entry(Blocks.MANGROVE_WALL_SIGN, SCContent.SECRET_MANGROVE_WALL_SIGN.get()),
		Map.entry(Blocks.CHERRY_WALL_SIGN, SCContent.SECRET_CHERRY_WALL_SIGN.get()),
		Map.entry(Blocks.PALE_OAK_WALL_SIGN, SCContent.SECRET_PALE_OAK_WALL_SIGN.get()),
		Map.entry(Blocks.BAMBOO_WALL_SIGN, SCContent.SECRET_BAMBOO_WALL_SIGN.get()),
		Map.entry(Blocks.CRIMSON_WALL_SIGN, SCContent.SECRET_CRIMSON_WALL_SIGN.get()),
		Map.entry(Blocks.WARPED_WALL_SIGN, SCContent.SECRET_WARPED_WALL_SIGN.get()),
		Map.entry(Blocks.OAK_HANGING_SIGN, SCContent.SECRET_OAK_HANGING_SIGN.get()),
		Map.entry(Blocks.SPRUCE_HANGING_SIGN, SCContent.SECRET_SPRUCE_HANGING_SIGN.get()),
		Map.entry(Blocks.BIRCH_HANGING_SIGN, SCContent.SECRET_BIRCH_HANGING_SIGN.get()),
		Map.entry(Blocks.JUNGLE_HANGING_SIGN, SCContent.SECRET_JUNGLE_HANGING_SIGN.get()),
		Map.entry(Blocks.ACACIA_HANGING_SIGN, SCContent.SECRET_ACACIA_HANGING_SIGN.get()),
		Map.entry(Blocks.DARK_OAK_HANGING_SIGN, SCContent.SECRET_DARK_OAK_HANGING_SIGN.get()),
		Map.entry(Blocks.MANGROVE_HANGING_SIGN, SCContent.SECRET_MANGROVE_HANGING_SIGN.get()),
		Map.entry(Blocks.CHERRY_HANGING_SIGN, SCContent.SECRET_CHERRY_HANGING_SIGN.get()),
		Map.entry(Blocks.PALE_OAK_HANGING_SIGN, SCContent.SECRET_PALE_OAK_HANGING_SIGN.get()),
		Map.entry(Blocks.BAMBOO_HANGING_SIGN, SCContent.SECRET_BAMBOO_HANGING_SIGN.get()),
		Map.entry(Blocks.CRIMSON_HANGING_SIGN, SCContent.SECRET_CRIMSON_HANGING_SIGN.get()),
		Map.entry(Blocks.WARPED_HANGING_SIGN, SCContent.SECRET_WARPED_HANGING_SIGN.get()),
		Map.entry(Blocks.OAK_WALL_HANGING_SIGN, SCContent.SECRET_OAK_WALL_HANGING_SIGN.get()),
		Map.entry(Blocks.SPRUCE_WALL_HANGING_SIGN, SCContent.SECRET_SPRUCE_WALL_HANGING_SIGN.get()),
		Map.entry(Blocks.BIRCH_WALL_HANGING_SIGN, SCContent.SECRET_BIRCH_WALL_HANGING_SIGN.get()),
		Map.entry(Blocks.JUNGLE_WALL_HANGING_SIGN, SCContent.SECRET_JUNGLE_WALL_HANGING_SIGN.get()),
		Map.entry(Blocks.ACACIA_WALL_HANGING_SIGN, SCContent.SECRET_ACACIA_WALL_HANGING_SIGN.get()),
		Map.entry(Blocks.DARK_OAK_WALL_HANGING_SIGN, SCContent.SECRET_DARK_OAK_WALL_HANGING_SIGN.get()),
		Map.entry(Blocks.MANGROVE_WALL_HANGING_SIGN, SCContent.SECRET_MANGROVE_WALL_HANGING_SIGN.get()),
		Map.entry(Blocks.CHERRY_WALL_HANGING_SIGN, SCContent.SECRET_CHERRY_WALL_HANGING_SIGN.get()),
		Map.entry(Blocks.PALE_OAK_WALL_HANGING_SIGN, SCContent.SECRET_PALE_OAK_WALL_HANGING_SIGN.get()),
		Map.entry(Blocks.BAMBOO_WALL_HANGING_SIGN, SCContent.SECRET_BAMBOO_WALL_HANGING_SIGN.get()),
		Map.entry(Blocks.CRIMSON_WALL_HANGING_SIGN, SCContent.SECRET_CRIMSON_WALL_HANGING_SIGN.get()),
		Map.entry(Blocks.WARPED_WALL_HANGING_SIGN, SCContent.SECRET_WARPED_WALL_HANGING_SIGN.get())
	));

	private SecretSignConversionCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, int permissionLevel) {
		dispatcher.register(Commands.literal("secretsign")
			.requires(ctx -> ctx.hasPermission(permissionLevel))
			.then(Commands.literal("secret")
				.then(Commands.argument("from", BlockPosArgument.blockPos())
					.then(Commands.argument("to", BlockPosArgument.blockPos())
						.executes(ctx -> SecretSignConversionCommand.fill(ctx, SIGN_MAP)))))
			.then(Commands.literal("unsecret")
				.then(Commands.argument("from", BlockPosArgument.blockPos())
					.then(Commands.argument("to", BlockPosArgument.blockPos())
						.executes(ctx -> SecretSignConversionCommand.fill(ctx, SIGN_MAP.inverse()))))));
	}

	private static int fill(CommandContext<CommandSourceStack> ctx, BiMap<Block, Block> signMap) throws CommandSyntaxException {
		BoundingBox area = BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos(ctx, "from"), BlockPosArgument.getLoadedBlockPos(ctx, "to"));
		CommandSourceStack source = ctx.getSource();
		ServerLevel level = source.getLevel();
		int blockCount = area.getXSpan() * area.getYSpan() * area.getZSpan();
		int commandModificationBlockLimit = level.getGameRules().getInt(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT);

		if (blockCount > commandModificationBlockLimit)
			throw FillCommand.ERROR_AREA_TOO_LARGE.create(commandModificationBlockLimit, blockCount);
		else {
			int blocksModified = 0;

			for (BlockPos pos : BlockPos.betweenClosed(area.minX(), area.minY(), area.minZ(), area.maxX(), area.maxY(), area.maxZ())) {
				BlockState state = level.getBlockState(pos);

				if (convert(state, level, pos, signMap))
					blocksModified++;
			}

			if (blocksModified == 0)
				throw ERROR_FILL_FAILED.create();
			else {
				int finalBlocksModified = blocksModified;

				source.sendSuccess(() -> Component.translatableWithFallback("commands.securitycraft.convert.fill.success", "Successfully converted %s block(s)", finalBlocksModified), true);
				return blocksModified;
			}
		}
	}

	private static boolean convert(BlockState state, Level level, BlockPos pos, BiMap<Block, Block> signMap) {
		Block block = state.getBlock();

		if (signMap.containsKey(block)) {
			BlockEntity be = level.getBlockEntity(pos);
			CompoundTag tag = be.saveWithFullMetadata(level.registryAccess());

			level.setBlockAndUpdate(pos, copyProperties(state, signMap.get(block)));
			be = level.getBlockEntity(pos);

			if (be != null) {
				try (final ProblemReporter.ScopedCollector scopedCollector = new ProblemReporter.ScopedCollector(SCServerUtils.LOGGER)) {
					final ValueInput valueInput = TagValueInput.create(scopedCollector.forChild(be.problemPath()), level.registryAccess(), tag);
					be.loadWithComponents(valueInput);
				}
			}

			return true;
		}

		return false;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static BlockState copyProperties(BlockState oldState, Block newBlock) {
		BlockState defaultBlockState = newBlock.defaultBlockState();

		for (Property property : oldState.getProperties()) {
			if (defaultBlockState.hasProperty(property))
				defaultBlockState = defaultBlockState.setValue(property, oldState.getValue(property));
		}

		return defaultBlockState;
	}
}
