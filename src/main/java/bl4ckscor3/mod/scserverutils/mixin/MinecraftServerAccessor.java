package bl4ckscor3.mod.scserverutils.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.server.MinecraftServer;

@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor {
	@Accessor
	public void setTicksUntilAutosave(int ticksUntilAutosave);
}
