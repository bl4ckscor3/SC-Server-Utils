package bl4ckscor3.mod.scserverutils.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.server.MinecraftServer;

@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor {
	public void setTicksUntilAutosave(int ticksUntilAutosave);
}
