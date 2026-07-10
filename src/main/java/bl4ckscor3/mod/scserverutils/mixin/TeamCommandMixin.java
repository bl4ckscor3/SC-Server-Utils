package bl4ckscor3.mod.scserverutils.mixin;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import bl4ckscor3.mod.scserverutils.SCServerUtils;
import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import bl4ckscor3.mod.scserverutils.configuration.TeamPermissionLevel;
import net.minecraft.server.commands.TeamCommand;

@Mixin(TeamCommand.class)
public class TeamCommandMixin {
	@ModifyArg(method = "register", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;requires(Ljava/util/function/Predicate;)Lcom/mojang/brigadier/builder/ArgumentBuilder;"), index = 0)
	private static Predicate scserverutils$relaxTeamCommandPermission(Predicate previousPermissionLevel) {
		TeamPermissionLevel teamPermissionLevel = Configuration.instance.teamPermissionLevel;

		return teamPermissionLevel.enabled().get() ? SCServerUtils.getPermissionCheckFromLevel(teamPermissionLevel.permissionLevel().get()) : previousPermissionLevel;
	}
}
