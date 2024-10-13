package bl4ckscor3.mod.scserverutils.mixin.sc;

import java.util.Objects;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.api.IModuleInventory;
import net.geforcemods.securitycraft.components.ListModuleData;
import net.geforcemods.securitycraft.misc.ModuleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@Mixin(IModuleInventory.class)
public interface IModuleInventoryMixin {
	@Shadow
	boolean isModuleEnabled(ModuleType type);

	@Shadow
	Level myLevel();

	@Shadow
	ItemStack getModule(ModuleType module);

	@Inject(method = "isAllowed(Lnet/minecraft/world/entity/Entity;)Z", at = @At("TAIL"), cancellable = true)
	default void scserverutils$checkAdvancement(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (!Configuration.instance.advancementAllowlists.enabled().get())
			return;

		if (!isModuleEnabled(ModuleType.ALLOWLIST)) {
			cir.setReturnValue(false); //Avoid a second module check and lookup from the call to isAllowed(String)
			return;
		}

		if (entity instanceof ServerPlayer player) {
			ListModuleData listModuleData = getModule(ModuleType.ALLOWLIST).get(SCContent.LIST_MODULE_DATA);

			if (listModuleData == null) {
				cir.setReturnValue(false); //Avoid a second module check and lookup from the call to isAllowed(String)
				return;
			}

			ServerAdvancementManager advancementManager = myLevel().getServer().getAdvancements();
			//@formatter:off
			boolean hasAnyAdvancement = listModuleData.players()
					.stream()
					.filter(playerName -> playerName.contains(":"))
					.map(ResourceLocation::parse)
					.map(advancementManager::get)
					.filter(Objects::nonNull)
					.anyMatch(advancement -> player.getAdvancements().getOrStartProgress(advancement).isDone());
			//@formatter:on

			if (hasAnyAdvancement)
				cir.setReturnValue(true);
			else { //Avoid a second module check and lookup from the call to isAllowed(String)
				String name = entity.getName().getString();

				cir.setReturnValue(listModuleData.affectEveryone() || listModuleData.isTeamOfPlayerOnList(myLevel(), name) || listModuleData.isPlayerOnList(name));
			}
		}
	}
}
