package bl4ckscor3.mod.scserverutils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import bl4ckscor3.mod.scserverutils.configuration.MixinModifier;

public class SCServerUtilsMixinPlugin implements IMixinConfigPlugin {
	private static List<MixinModifier> mixinModifiers = new ArrayList<>();

	@Override
	public void onLoad(String mixinPackage) {
		Configuration.init();
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		String strippedMixinClassName = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);

		for (MixinModifier mixinModifier : mixinModifiers) {
			if (mixinModifier.hasMixinClass(strippedMixinClassName) && !mixinModifier.enabled().get())
				return false;
		}

		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

	public static void addMixinModifier(MixinModifier hasMixinClasses) {
		mixinModifiers.add(hasMixinClasses);
	}
}
