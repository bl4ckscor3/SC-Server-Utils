package bl4ckscor3.mod.scserverutils.mixin.sc;

import java.util.Objects;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import bl4ckscor3.mod.scserverutils.SCServerUtils;
import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.api.IModuleInventory;
import net.geforcemods.securitycraft.components.ListModuleData;
import net.geforcemods.securitycraft.misc.ModuleType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
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
	default void scserverutils$checkSelector(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (!Configuration.instance.selectorAllowlists.enabled().get())
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

			CommandSourceStack commandSourceStack = player.level().getServer().createCommandSourceStack().withEntity(player);
			boolean matchesSelector = listModuleData.players()
				.stream()
				.filter(entry -> entry.startsWith("@"))
				.map(selector -> {
					try {
						return new EntitySelectorParser(new StringReader(selector), true).parse();
					}
					catch (CommandSyntaxException e) {
						SCServerUtils.LOGGER.warn("Invalid entity selector in allowlist: ", e);
						return null;
					}
				})
				.filter(Objects::nonNull)
				.anyMatch(selector -> {
					try {
						return selector.findEntities(commandSourceStack).contains(player);
					}
					catch (CommandSyntaxException e) {
						SCServerUtils.LOGGER.warn("Error finding entities with selector in allowlist: ", e);
						return false;
					}
				});

			if (matchesSelector)
				cir.setReturnValue(true);
			else { //Avoid a second module check and lookup from the call to isAllowed(String)
				String name = entity.getName().getString();

				cir.setReturnValue(listModuleData.affectEveryone() || listModuleData.isTeamOfPlayerOnList(myLevel(), name) || listModuleData.isPlayerOnList(name));
			}
		}
	}
}
