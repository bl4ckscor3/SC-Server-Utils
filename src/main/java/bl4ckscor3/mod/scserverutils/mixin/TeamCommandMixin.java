package bl4ckscor3.mod.scserverutils.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import bl4ckscor3.mod.scserverutils.configuration.TeamPermissionLevel;
import net.minecraft.server.commands.TeamCommand;

@Mixin(TeamCommand.class)
public class TeamCommandMixin {
	@ModifyConstant(method = "lambda$register$0", constant = @Constant(intValue = 2))
	private static int scserverutils$relaxTeamCommandPermission(int previousPermissionLevel) {
		TeamPermissionLevel teamPermissionLevel = Configuration.instance.teamPermissionLevel;

		return teamPermissionLevel.enabled().get() ? teamPermissionLevel.permissionLevel().get() : previousPermissionLevel;
	}
}
