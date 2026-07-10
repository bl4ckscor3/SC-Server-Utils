package bl4ckscor3.mod.scserverutils.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import bl4ckscor3.mod.scserverutils.configuration.spawnprotection.SpawnProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

@Mixin(Entity.class)
public abstract class EntityMixin {
	@Shadow
	public abstract EntityType<?> getType();

	@Inject(method = "adjustSpawnLocation", at = @At("HEAD"), cancellable = true)
	private void scserverutils$respectY(ServerLevel level, BlockPos pos, CallbackInfoReturnable<BlockPos> cir) {
		SpawnProtection spawnProtection = Configuration.instance.spawnProtection;

		if (spawnProtection.spawnLocationY().get() && !spawnProtection.ignoredEntityTypes().get().contains(getType().builtInRegistryHolder().getKey().identifier().toString())) {
			BlockPos spawnPos = level.getRespawnData().pos();

			if (spawnPos.getX() == pos.getX() && spawnPos.getZ() == pos.getZ())
				cir.setReturnValue(spawnPos.above().immutable());
		}
	}
}
