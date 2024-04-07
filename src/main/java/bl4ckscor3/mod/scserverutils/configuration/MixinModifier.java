package bl4ckscor3.mod.scserverutils.configuration;

import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;

/**
 * Marks a config category as being able to modify mixin class loading
 */
public interface MixinModifier {
	/**
	 * Whether this config category is enabled
	 *
	 * @return The config value of the "enabled" config setting
	 */
	public BooleanValue enabled();

	/**
	 * Checks whether a mixin class is affected by this config category. If the config category is disabled, mixin classes for
	 * which this method return true will not be loaded
	 *
	 * @param mixinClassName The mixin class name to check. This is only the class name, and not fully qualified.
	 * @return true if the mixin class is affected, false otherwise
	 */
	public boolean hasMixinClass(String mixinClassName);
}