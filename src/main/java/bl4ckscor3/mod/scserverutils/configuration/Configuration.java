package bl4ckscor3.mod.scserverutils.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;

import com.mojang.brigadier.CommandDispatcher;

import bl4ckscor3.mod.scserverutils.SCServerUtils;
import bl4ckscor3.mod.scserverutils.commands.DeathLogCommand;
import bl4ckscor3.mod.scserverutils.commands.EnderchestCommand;
import bl4ckscor3.mod.scserverutils.commands.InvseeCommand;
import bl4ckscor3.mod.scserverutils.commands.PlayerHeadCommand;
import bl4ckscor3.mod.scserverutils.commands.RulesCommand;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public class Configuration {
	public static final ModConfigSpec SPEC;
	public static final Configuration instance;
	public AttributeLogFix attributeLogFix;
	public AutosaveInterval autosaveInterval;
	public List<CommandConfig> commands = new ArrayList<>();
	public DamageSourceLanguageFallback damageSourceLanguageFallback;
	public DeathLog deathLog;
	public NetherSpawnProtection netherSpawnProtection;
	public NoSpawnProtectionSnow noSpawnProtectionSnow;
	public NoSpawnProtectionSpawns noSpawnProtectionSpawns;
	public PhantomSpawns phantomSpawns;
	public SpawnProtectionEffects spawnProtectionEffects;
	public SpawnProtectionPvpPrevention spawnProtectionPvpPrevention;
	public SuppressDestroyMismatchLog suppressDestroyMismatchLog;
	public TeamPermissionLevel teamPermissionLevel;

	static {
		Pair<Configuration, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Configuration::new);

		SPEC = pair.getRight();
		instance = pair.getLeft();
	}

	Configuration(ModConfigSpec.Builder builder) {
		pushPop(builder, "Attribute log removal", "Removes the \"Unknown Attribute\" log message to reduce console spam", () -> {
			attributeLogFix = new AttributeLogFix(enabled(builder));
		});
		pushPop(builder, "Autosave interval", "Changes the interval at which the game automatically saves everything", () -> {
			autosaveInterval = new AutosaveInterval( //@formatter:off
					enabled(builder),
					builder.comment("The interval in seconds").defineInRange("interval", 60, 5, Integer.MAX_VALUE),
					builder.comment("Removes the \"Gathered mod list to write to world save world\" log message").define("remove_neoforge_log_message", true));
					//@formatter:on
		});
		pushPop(builder, "Commands", "Configure commands of this mod", () -> {
			addCommandConfig(builder, "deathlog", 2, () -> DeathLogCommand::register);
			addCommandConfig(builder, "enderchest", 2, () -> EnderchestCommand::register);
			addCommandConfig(builder, "invsee", 2, () -> InvseeCommand::register);
			addCommandConfig(builder, "playerhead", 1, () -> PlayerHeadCommand::register);
			addCommandConfig(builder, "rules", 0, () -> RulesCommand::register);
		});
		pushPop(builder, "Damage source language fallback", "Adds a fallback to the \"/trigger kill_self\" death message so people without the resource pack see the correct message", () -> {
			damageSourceLanguageFallback = new DamageSourceLanguageFallback(enabled(builder));
		});
		pushPop(builder, "Death logging", "Logs all players' deaths as they happen, containing complete inventory info etc.", () -> {
			deathLog = new DeathLog( //@formatter:off
					enabled(builder),
					builder.comment("The path where death logs are saved, relative to the game directory.").define("save_path", SCServerUtils.MODID + "/death_logs"));
					//@formatter:on
		});
		pushPop(builder, "Nether spawn protection", "Adds spawn protection to the nether", () -> {
			netherSpawnProtection = new NetherSpawnProtection( //@formatter:off
					enabled(builder),
					builder.comment("The square radius in blocks that is under spawn protection.").defineInRange("radius", 32, 0, Integer.MAX_VALUE),
					builder.comment("The X coordinate of the nether spawn's origin").defineInRange("x_origin", 0, Integer.MIN_VALUE, Integer.MAX_VALUE),
					builder.comment("The Z coordinate of the nether spawn's origin").defineInRange("z_origin", 0, Integer.MIN_VALUE, Integer.MAX_VALUE));
					//@formatter:on
		});
		pushPop(builder, "No snow in spawn protection", "Disables snow accumulation in spawn protection", () -> {
			noSpawnProtectionSnow = new NoSpawnProtectionSnow(enabled(builder));
		});
		pushPop(builder, "No mobs in spawn protection", "Disables mob spawns in spawn protection", () -> {
			noSpawnProtectionSpawns = new NoSpawnProtectionSpawns(enabled(builder));
		});
		pushPop(builder, "Phantom spawns", "Makes it possible to change how many phantoms spawn when the game wants to spawn them.", () -> {
			phantomSpawns = new PhantomSpawns( //@formatter:off
					enabled(builder),
					builder.comment("The minimum amount of phantoms to spawn").defineInRange("min_spawns", 0, 0, Integer.MAX_VALUE),
					builder.comment("The maximum amount of phantoms to spawn").defineInRange("max_spawns", 1, 0, Integer.MAX_VALUE));
					//@formatter:on
		});
		pushPop(builder, "Spawn protection effects", "Effects to give players in spawn protection", () -> {
			spawnProtectionEffects = new SpawnProtectionEffects( //@formatter:off
					enabled(builder),
					builder.comment("Which effects players should have while in spawn protection. One entry corresponds to one effect, and is formatted like this:",
						"effect_namespace:effect_path|duration|amplifier",
						"Example: The entry \"minecraft:slowness|20|1\" defines slowness 1 for 1 second (20 ticks = 1 second).")
						.defineList("effects", List.of("minecraft:regeneration|-1|3", "minecraft:speed|-1|3"), () -> "", String.class::isInstance),
					builder.comment("Whether to add the effects in the nether as well").define("in_nether", false));
					//@formatter:on
		});
		pushPop(builder, "Spawn protection PvP prevention", "Disables pvp in spawn protection", () -> {
			spawnProtectionPvpPrevention = new SpawnProtectionPvpPrevention( //@formatter:off
					enabled(builder),
					builder.comment("Whether to also disable PvP in the nether spawn protection, which needs to be enabled for this setting to take effect").define("disable_in_nether", false));
					//@formatter:on
		});
		pushPop(builder, "Suppress destroy mismatch log", "Removes the \"Mismatch in destroy block pos\" log message to reduce console spam", () -> {
			suppressDestroyMismatchLog = new SuppressDestroyMismatchLog( //@formatter:off
					enabled(builder),
					builder.comment("Whether to only disable this message when the destroy position is within spawn protection.").define("only_in_spawn_protection", true));
		});
		pushPop(builder, "Team command permission level", "Allows changing the permission level for the /team command", () -> {
			teamPermissionLevel = new TeamPermissionLevel( //@formatter:off
					enabled(builder),
					permissionLevel(builder, "team", 1));
					//@formatter:on
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
		return enabled(builder, "feature");
	}

	private BooleanValue enabled(ModConfigSpec.Builder builder, String thing) {
		return builder.comment("Whether this " + thing + " is enabled").define("enabled", true);
	}

	private IntValue permissionLevel(ModConfigSpec.Builder builder, String commandName, int defaultPermissionLevel) {
		return builder.comment("The minimum permission level needed for the /" + commandName + " command").defineInRange("permission_level", defaultPermissionLevel, 0, 5);
	}

	private void addCommandConfig(ModConfigSpec.Builder builder, String commandName, int defaultPermissionLevel, Supplier<BiConsumer<CommandDispatcher<CommandSourceStack>, Integer>> registrar) {
		pushPop(builder, commandName, null, () -> {
			//@formatter:off
			commands.add(new CommandConfig(
					enabled(builder, "command"),
					permissionLevel(builder, commandName, defaultPermissionLevel),
					registrar));
			//@formatter:on
		});
	}
}
