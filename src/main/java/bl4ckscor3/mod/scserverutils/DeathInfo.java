package bl4ckscor3.mod.scserverutils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.storage.LevelData;

public record DeathInfo(String uuid, Cause cause, GlobalPos position, ListTag inventory, Optional<BlockPos> respawnPosition) {

	//@formatter:off
	public static final Codec<DeathInfo> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					Codec.STRING.fieldOf("uuid").forGetter(DeathInfo::uuid),
					Cause.CODEC.fieldOf("cause").forGetter(DeathInfo::cause),
					GlobalPos.CODEC.fieldOf("position").forGetter(DeathInfo::position),
					CompoundTag.CODEC.listOf().xmap(
							list -> {
								ListTag listTag = new ListTag();

								listTag.addAll(list);
								return listTag;
							},
							listTag -> {
								List<CompoundTag> list = new ArrayList<>();

								listTag.forEach(tag -> list.add((CompoundTag) tag));
								return list;
							}).fieldOf("inventory").forGetter(DeathInfo::inventory),
					BlockPos.CODEC.optionalFieldOf("respawn_position").forGetter(DeathInfo::respawnPosition))
			.apply(instance, DeathInfo::new));
	//@formatter:on
	public static DeathInfo of(ServerPlayer player, DamageSource source) {
		String uuid = player.getStringUUID();
		Cause cause = Cause.of(source);
		GlobalPos pos = GlobalPos.of(player.level().dimension(), player.blockPosition());
		Optional<BlockPos> respawnPosition = Optional.ofNullable(player.getRespawnConfig()).map(ServerPlayer.RespawnConfig::respawnData).map(LevelData.RespawnData::pos);

		return new DeathInfo(uuid, cause, pos, DeathLogger.saveInventory(player.getInventory()), respawnPosition);
	}

	public static record Cause(Identifier type, Optional<Identifier> directEntity, Optional<Identifier> causingEntity) {

		//@formatter:off
		public static final Codec<Cause> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						Identifier.CODEC.fieldOf("type").forGetter(Cause::type),
						Identifier.CODEC.optionalFieldOf("direct_entity").forGetter(Cause::directEntity),
						Identifier.CODEC.optionalFieldOf("causing_entity").forGetter(Cause::causingEntity))
				.apply(instance, Cause::new));
		//@formatter:on
		public static Cause of(DamageSource source) {
			Optional<Identifier> directEntity = Optional.empty();
			Optional<Identifier> causingEntity = Optional.empty();

			if (source.getDirectEntity() != null)
				directEntity = Optional.of(BuiltInRegistries.ENTITY_TYPE.getKey(source.getDirectEntity().getType()));

			if (source.getEntity() != null)
				causingEntity = Optional.of(BuiltInRegistries.ENTITY_TYPE.getKey(source.getEntity().getType()));

			return new Cause(source.typeHolder().unwrapKey().get().identifier(), directEntity, causingEntity);
		}
	}
}
