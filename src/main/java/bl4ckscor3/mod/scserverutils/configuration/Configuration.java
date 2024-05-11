package bl4ckscor3.mod.scserverutils.configuration;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;

import bl4ckscor3.mod.scserverutils.SCServerUtils;
import bl4ckscor3.mod.scserverutils.SCServerUtilsMixinPlugin;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;

public class Configuration {
	public static Configuration instance;
	private static boolean initialized = false;
	public AttributeLogFix attributeLogFix;
	public AutosaveInterval autosaveInterval;
	public Commands commands;
	public DamageSourceLanguageFallback damageSourceLanguageFallback;
	public DeathLog deathLog;
	public PhantomSpawns phantomSpawns;
	public TeamPermissionLevel teamPermissionLevel;

	public static void init() {
		if (!initialized) {
			ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
			String fileName = "scserverutils-common.toml";
			CommentedFileConfig fileConfig;
			ModConfigSpec configSpec;

			instance = new Configuration(builder);
			configSpec = builder.build();
			//@formatter:off
			fileConfig = CommentedFileConfig
					.builder(FMLPaths.CONFIGDIR.get().resolve(fileName))
					.preserveInsertionOrder()
					.writingMode(WritingMode.REPLACE)
					.build();
			//@formatter:on
			fileConfig.load();
			fileConfig.save();
			configSpec.setConfig(fileConfig);
			initialized = true;
		}
	}

	Configuration(ModConfigSpec.Builder builder) {
		pushPop(builder, "Attribute log removal", "Removes the \"Unknown Attribute\" log message to reduce console spam", () -> {
			attributeLogFix = new AttributeLogFix(enabled(builder));
			SCServerUtilsMixinPlugin.addMixinModifier(attributeLogFix);
		});
		pushPop(builder, "Autosave interval", "Changes the interval at which the game automatically saves everything", () -> {
			autosaveInterval = new AutosaveInterval( //@formatter:off
					enabled(builder),
					builder.comment("The interval in seconds").defineInRange("interval", 60, 5, Integer.MAX_VALUE),
					builder.comment("Removes the \"Gathered mod list to write to world save world\" log message").define("remove_neoforge_log_message", true));
					//@formatter:on
			SCServerUtilsMixinPlugin.addMixinModifier(autosaveInterval);
		});
		pushPop(builder, "Commands", "Enable or disable commands of the mod", () -> {
			commands = new Commands(Commands.createConfig(builder, "enderchest"), Commands.createConfig(builder, "invsee"), Commands.createConfig(builder, "rules"));
		});
		pushPop(builder, "Damage source language fallback", "Adds a fallback to the \"/trigger kill_self\" death message so people without the resource pack see the correct message", () -> {
			damageSourceLanguageFallback = new DamageSourceLanguageFallback(enabled(builder));
			SCServerUtilsMixinPlugin.addMixinModifier(damageSourceLanguageFallback);
		});
		pushPop(builder, "Death logging", "Logs all players' deaths as they happen, containing complete inventory info etc.", () -> {
			deathLog = new DeathLog( //@formatter:off
					enabled(builder),
					builder.comment("The path where death logs are saved, relative to the game directory.").define("save_path", SCServerUtils.MODID + "/death_logs")
					);
					//@formatter:on
		});
		pushPop(builder, "Phantom spawns", "Makes it possible to change how many phantoms spawn when the game wants to spawn them.", () -> {
			phantomSpawns = new PhantomSpawns( //@formatter:off
					enabled(builder),
					builder.comment("The minimum amount of phantoms to spawn").defineInRange("min_spawns", 0, 0, Integer.MAX_VALUE),
					builder.comment("The maximum amount of phantoms to spawn").defineInRange("max_spawns", 1, 0, Integer.MAX_VALUE));
					//@formatter:on
		});
		pushPop(builder, "Team command permission level", "Allows changing the permission level for the /team command", () -> {
			teamPermissionLevel = new TeamPermissionLevel( //@formatter:off
					enabled(builder),
					builder.comment("The minimum permission level needed for the /team command").defineInRange("permission_level", 1, 0, 5));
					//@formatter:on
			SCServerUtilsMixinPlugin.addMixinModifier(teamPermissionLevel);
		});
	}

	private void pushPop(ModConfigSpec.Builder builder, String categoryName, String categoryComment, Runnable categorySetup) {
		if (categoryComment != null)
			builder.comment(categoryComment);

		builder.push(categoryName);
		categorySetup.run();
		builder.pop();
	}

	private BooleanValue enabled(ModConfigSpec.Builder builder) {
		return builder.comment("Whether this feature is enabled").define("enabled", true);
	}
}
