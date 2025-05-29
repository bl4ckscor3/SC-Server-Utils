package bl4ckscor3.mod.scserverutils.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import bl4ckscor3.mod.scserverutils.SCServerUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.fml.loading.FMLPaths;

public class AreaCommand {
	private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType((maxBlocks, blockCount) -> Component.translatableEscape("commands.clone.toobig", maxBlocks, blockCount));
	private static final Dynamic2CommandExceptionType ERROR_AREA_NOT_WRITTEN = new Dynamic2CommandExceptionType((id, msg) -> Component.literal("Unable to write area " + id + ": " + msg));
	private static final Dynamic2CommandExceptionType ERROR_AREA_NOT_READABLE = new Dynamic2CommandExceptionType((id, msg) -> Component.literal("Unable to read area " + id + ": " + msg));
	private static final DynamicCommandExceptionType ERROR_AREA_IMPORT_FAILED = new DynamicCommandExceptionType(id -> Component.literal("Importing " + id + " failed. Maybe the area doesn't exist? "));
	private static final String FOLDER_NAME = SCServerUtils.MODID + "/areas";
	private static final Path AREAS_PATH = FMLPaths.getOrCreateGameRelativePath(Paths.get(FOLDER_NAME));
	private static final SuggestionProvider<CommandSourceStack> AREAS = (ctx, builder) -> {
		List<String> suggestions = new ArrayList<>();

		Arrays.stream(AREAS_PATH.toFile().listFiles()).filter(File::isDirectory).forEach(dir -> {
			Arrays.stream(dir.listFiles()).filter(file -> file.getName().endsWith(".nbt")).forEach(file -> suggestions.add(dir.getName() + ":" + file.getName().replace(".nbt", "")));
		});

		return SharedSuggestionProvider.suggest(suggestions.stream(), builder);
	};

	public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, int permissionLevel) {
		//@formatter:off
		dispatcher.register(Commands.literal("area")
				.requires(commandSource -> commandSource.hasPermission(permissionLevel))
				.then(Commands.argument("id", ResourceLocationArgument.id())
						.suggests(AREAS)
						.then(Commands.literal("export")
								.then(Commands.argument("from", BlockPosArgument.blockPos())
										.then(Commands.argument("to", BlockPosArgument.blockPos())
												.executes(c -> exportArea(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "from"), BlockPosArgument.getLoadedBlockPos(c, "to"), ResourceLocationArgument.getId(c, "id"))))))
						.then(Commands.literal("import")
								.executes(c -> importArea(c.getSource(), ResourceLocationArgument.getId(c, "id"))))));
		//@formatter:on
	}

	private static int importArea(CommandSourceStack source, ResourceLocation id) throws CommandSyntaxException {
		CompoundTag tag;

		try {
			tag = NbtIo.read(AREAS_PATH.resolve(id.getNamespace()).resolve(id.getPath() + ".nbt"));
		}
		catch (final IOException exception) {
			throw ERROR_AREA_NOT_READABLE.create(id, exception.getMessage());
		}

		DataResult<Area> parsed = Area.CODEC.parse(NbtOps.INSTANCE, tag);

		if (parsed.isError())
			throw ERROR_AREA_IMPORT_FAILED.create(id);

		Area area = parsed.result().get();
		ServerLevel level = source.getServer().getLevel(area.origin.dimension());

		level.getServer().executeIfPossible(() -> {
			List<BlockInfo> blocks = area.blocks();
			List<BlockInfo> reverseBlocks = Lists.reverse(blocks);
			List<BlockInfo> blockEntities = new ArrayList<>();

			for (BlockInfo blockInfo : reverseBlocks) {
				level.setBlock(blockInfo.pos(), Blocks.BARRIER.defaultBlockState(), 2);
			}

			for (BlockInfo blockInfo : blocks) {
				BlockPos pos = blockInfo.pos();

				level.setBlock(pos, blockInfo.state(), 2);

				if (blockInfo.blockEntityTag().isPresent())
					blockEntities.add(blockInfo);
			}

			for (BlockInfo blockInfo : blockEntities) {
				BlockPos pos = blockInfo.pos();
				BlockEntity be = level.getBlockEntity(pos);

				if (be != null) {
					be.loadWithComponents(blockInfo.blockEntityTag().get(), level.registryAccess());
					be.setChanged();
				}

				level.setBlock(pos, blockInfo.state(), 2);
			}

			for (final BlockInfo blockInfo : reverseBlocks) {
				level.updateNeighborsAt(blockInfo.pos(), blockInfo.state().getBlock());
			}

			GlobalPos origin = area.origin();
			BlockPos pos = origin.pos();
			ResourceLocation dimension = origin.dimension().location();

			source.sendSuccess(() -> Component.literal("Imported area and placed it in the world at ").append(Component.literal(pos.toShortString() + " in " + dimension).setStyle(Style.EMPTY.withClickEvent(new ClickEvent.SuggestCommand("/execute in " + dimension + " run tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ())).withColor(ChatFormatting.GREEN))), true);
		});
		return 1;
	}

	private static int exportArea(CommandSourceStack source, BlockPos from, BlockPos to, ResourceLocation id) throws CommandSyntaxException {
		ServerLevel level = source.getLevel();
		BoundingBox area = BoundingBox.fromCorners(from, to);
		int blockCount = area.getXSpan() * area.getYSpan() * area.getZSpan();
		int maxBlocks = level.getGameRules().getInt(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT);

		if (blockCount > maxBlocks)
			throw ERROR_AREA_TOO_LARGE.create(maxBlocks, blockCount);
		else if (level.hasChunksAt(from, to)) {
			List<BlockInfo> blocks = new ArrayList<>();
			List<BlockInfo> blockEntityBlocks = new ArrayList<>();
			List<BlockInfo> nonSolidBlocks = new ArrayList<>();

			for (int z = area.minZ(); z <= area.maxZ(); ++z) {
				for (int y = area.minY(); y <= area.maxY(); ++y) {
					for (int x = area.minX(); x <= area.maxX(); ++x) {
						BlockPos pos = new BlockPos(x, y, z);
						BlockInWorld blockInWorld = new BlockInWorld(level, pos, false);
						BlockState state = blockInWorld.getState();
						BlockEntity be = level.getBlockEntity(pos);

						if (be != null)
							blockEntityBlocks.add(new BlockInfo(pos, state, Optional.of(be.saveWithoutMetadata(source.registryAccess()))));
						else if (!state.isSolidRender() && !state.isCollisionShapeFullBlock(level, pos))
							nonSolidBlocks.add(new BlockInfo(pos, state, Optional.empty()));
						else
							blocks.add(new BlockInfo(pos, state, Optional.empty()));
					}
				}
			}

			GlobalPos origin = GlobalPos.of(level.dimension(), new BlockPos(area.minX(), area.minY(), area.minZ()));
			List<BlockInfo> allBlocks = new ArrayList<>(blocks);

			allBlocks.addAll(blockEntityBlocks);
			allBlocks.addAll(nonSolidBlocks);

			try {
				CompoundTag tag = (CompoundTag) Area.CODEC.encode(new Area(origin, allBlocks), NbtOps.INSTANCE, new CompoundTag()).result().get();
				Path exportPath = AREAS_PATH.resolve(id.getNamespace()).resolve(id.getPath() + ".nbt");

				Files.createDirectories(exportPath.getParent());
				NbtIo.write(tag, exportPath);
			}
			catch (final IOException exception) {
				throw ERROR_AREA_NOT_WRITTEN.create(id, exception.getMessage());
			}

			source.sendSuccess(() -> Component.literal("Area exported as " + id), true);
			return 1;
		}
		else
			throw BlockPosArgument.ERROR_NOT_LOADED.create();
	}

	//@formatter:off
    public record Area(GlobalPos origin, List<BlockInfo> blocks) {
        public static final Codec<Area> CODEC = RecordCodecBuilder.create(i -> i.group(
            GlobalPos.CODEC.fieldOf("origin").forGetter(Area::origin),
            BlockInfo.LIST_CODEC.fieldOf("blocks").forGetter(Area::blocks)
        ).apply(i, Area::new));
        public static final StreamCodec<FriendlyByteBuf, Area> STREAM_CODEC = StreamCodec.composite(
            GlobalPos.STREAM_CODEC, Area::origin,
            BlockInfo.LIST_STREAM_CODEC, Area::blocks,
            Area::new
        );
    }

    public record BlockInfo(BlockPos pos, BlockState state, Optional<CompoundTag> blockEntityTag) {
        public static final Codec<BlockInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(BlockInfo::pos),
            BlockState.CODEC.fieldOf("state").forGetter(BlockInfo::state),
            CompoundTag.CODEC.optionalFieldOf("block_entity_tag").forGetter(BlockInfo::blockEntityTag)
        ).apply(i, BlockInfo::new));
        public static final Codec<List<BlockInfo>> LIST_CODEC = CODEC.listOf();
        public static final StreamCodec<FriendlyByteBuf, BlockInfo> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, BlockInfo::pos,
            ByteBufCodecs.VAR_INT, blockInfo -> Block.getId(blockInfo.state),
            ByteBufCodecs.TRUSTED_COMPOUND_TAG.apply(ByteBufCodecs::optional), BlockInfo::blockEntityTag,
            (pos, stateId, tag) -> new BlockInfo(pos, Block.stateById(stateId), tag)
        );
        public static final StreamCodec<FriendlyByteBuf, List<BlockInfo>> LIST_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs.list());
    }
}
