package bl4ckscor3.mod.scserverutils.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;

import com.mojang.brigadier.CommandDispatcher;

import bl4ckscor3.mod.scserverutils.SCServerUtils;
import bl4ckscor3.mod.scserverutils.commands.AreaCommand;
import bl4ckscor3.mod.scserverutils.commands.DeathLogCommand;
import bl4ckscor3.mod.scserverutils.commands.EnderchestCommand;
import bl4ckscor3.mod.scserverutils.commands.InvseeCommand;
import bl4ckscor3.mod.scserverutils.commands.PlayerHeadCommand;
import bl4ckscor3.mod.scserverutils.commands.RulesCommand;
import bl4ckscor3.mod.scserverutils.commands.SecretSignConversionCommand;
import bl4ckscor3.mod.scserverutils.configuration.spawnprotection.BlockBypass;
import bl4ckscor3.mod.scserverutils.configuration.spawnprotection.Dimension;
import bl4ckscor3.mod.scserverutils.configuration.spawnprotection.Effects;
import bl4ckscor3.mod.scserverutils.configuration.spawnprotection.Messages;
import bl4ckscor3.mod.scserverutils.configuration.spawnprotection.MobSpawning;
import bl4ckscor3.mod.scserverutils.configuration.spawnprotection.PvpPrevention;
import bl4ckscor3.mod.scserverutils.configuration.spawnprotection.RiftStabilizer;
import bl4ckscor3.mod.scserverutils.configuration.spawnprotection.SpawnProtection;
import net.geforcemods.securitycraft.blockentities.RiftStabilizerBlockEntity.TeleportationType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.EntitySpawnReason;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public class Configuration {
	public static final ModConfigSpec SPEC;
	public static final Configuration instance;
	public AdvancementAllowlists advancementAllowlists;
	public AutosaveInterval autosaveInterval;
	public List<CommandConfig> commands = new ArrayList<>();
	public CustomServerLinks customServerLinks;
	public DamageSourceLanguageFallback damageSourceLanguageFallback;
	public DeathLog deathLog;
	public PhantomSpawns phantomSpawns;
	public SDLink sdLink;
	public SpawnProtection spawnProtection;
	public SuppressDestroyMismatchLog suppressDestroyMismatchLog;
	public TeamPermissionLevel teamPermissionLevel;

	static {
		Pair<Configuration, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Configuration::new);

		SPEC = pair.getRight();
		instance = pair.getLeft();
	}

	Configuration(ModConfigSpec.Builder builder) {
		advancementAllowlists = pushPop(builder, "Advancement-activated allowlists", "Enables allowlists to allow players who have a specific advancement by adding an advancement to its list", () -> new AdvancementAllowlists(enabled(builder)));
		autosaveInterval = pushPop(builder, "Autosave interval", "Changes the interval at which the game automatically saves everything", () ->
			new AutosaveInterval(
				enabled(builder),
				builder.comment("The interval in seconds").defineInRange("interval", 60, 5, Integer.MAX_VALUE),
				builder.comment("Removes the \"Gathered mod list to write to world save world\" log message").define("remove_neoforge_log_message", true))
		);
		pushPop(builder, "Commands", "Configure commands of this mod", () -> {
			addCommandConfig(builder, "area", 2, () -> AreaCommand::register);
			addCommandConfig(builder, "deathlog", 2, () -> DeathLogCommand::register);
			addCommandConfig(builder, "enderchest", 2, () -> EnderchestCommand::register);
			addCommandConfig(builder, "invsee", 2, () -> InvseeCommand::register);
			addCommandConfig(builder, "playerhead", 1, () -> PlayerHeadCommand::register);
			addCommandConfig(builder, "rules", 0, () -> RulesCommand::register);
			addCommandConfig(builder, "secretsign", 2, () -> SecretSignConversionCommand::register);
			return null;
		});
		customServerLinks = pushPop(builder, "Custom server links", "Server links to send to a connecting player", () ->
			new CustomServerLinks(
				enabled(builder),
				builder.comment("Each entry is a triple of link|translation_key|fallback")
					.defineList("server_links", List.of(), () -> "", String.class::isInstance))
		);
		damageSourceLanguageFallback = pushPop(builder, "Damage source language fallback", "Adds a fallback to the \"/trigger kill_self\" death message so people without the resource pack see the correct message", () -> new DamageSourceLanguageFallback(enabled(builder)));
		deathLog = pushPop(builder, "Death logging", "Logs all players' deaths as they happen, containing complete inventory info etc.", () ->
			new DeathLog(
				enabled(builder),
				builder.comment("The path where death logs are saved, relative to the game directory.").define("save_path", SCServerUtils.MODID + "/death_logs"))
		);
		sdLink = pushPop(builder, "Simple Discord Link", "Additional settings for Simple Discord Link", () ->
			new SDLink(
				builder.comment("Whether to log commands sent by the server or command blocks.").define("log_server_commands", false)
			)
		);
		spawnProtection = pushPop(builder, "Spawn protection", "Assorted settings regarding spawn protection", () ->
			new SpawnProtection(
				builder.comment("Disables snow accumulation in spawn protection").define("no_snow", true),
				builder.comment("Enables respecting the y position of the spawn location").define("respect_spawn_y", true),
				pushPop(builder, "Block bypass", "Blocks that players will be able to rightclick in spawn protection", () ->
					new BlockBypass(
						enabled(builder),
						builder
							.comment("Which blocks players should be able to rightclick while in spawn protection. One entry corresponds to one block, and is formatted like a registry name, visible with F3+H")
							.defineList("blocks", List.of("minecraft:ender_chest", "minecraft:lectern", "securitycraft:reinforced_lectern"), () -> "", String.class::isInstance))
				),
				pushPop(builder, "Effects", "Effects to give players in spawn protection", () ->
					new Effects(
						enabled(builder),
						builder.comment("Which effects players should have while in spawn protection. One entry corresponds to one effect, and is formatted like this:",
								"effect_namespace:effect_path|duration|amplifier",
								"Example: The entry \"minecraft:slowness|20|1\" defines slowness 1 for 1 second (20 ticks = 1 second).")
							.defineList("effects", List.of("minecraft:regeneration|-1|3", "minecraft:speed|-1|3"), () -> "", String.class::isInstance),
						builder.comment("Whether to add the effects in the nether as well").define("in_nether", false))
				),
				pushPop(builder, "Messages", "Messages relating to spawn protection", () ->
					new Messages(
						builder.comment("Message sent when entering spawn protection").define("enter", "{translate:\"scserverutils.enter_spawn_protection\",fallback:\"PvP is no longer active.\",with:[],type:\"translatable\",color:\"green\"}"),
						builder.comment("Message sent when leaving spawn protection").define("leave", "{translate:\"scserverutils.leave_spawn_protection\",fallback:\"PvP is now on!\",with:[],type:\"translatable\",color:\"red\"}")
					)
				),
				pushPop(builder, "Mob spawning", "Disables mob spawns in spawn protection", () ->
					new MobSpawning(
						enabled(builder),
						builder
							.comment("Types of mob spawns that are allowed to spawn a mob within spawn protection. Allowed values:",
								Arrays.stream(EntitySpawnReason.values()).map(Enum::name).toList().toString())
							.defineList("allowed_reasons",
								List.of(
										EntitySpawnReason.COMMAND,
										EntitySpawnReason.LOAD,
										EntitySpawnReason.MOB_SUMMONED,
										EntitySpawnReason.SPAWN_ITEM_USE)
									.stream()
									.map(Enum::name)
									.toList(),
								() -> "",
								String.class::isInstance),
						builder
							.comment("Entity types for which verbose logging is enabled when they try to spawn")
							.defineList("verbose_logging_for", List.of(), () -> "", String.class::isInstance))
				),
				addSpawnProtectionConfig(builder, "nether", "Nether"),
				addSpawnProtectionConfig(builder, "end", "End"),
				pushPop(builder, "PvP prevention", "Disables pvp in spawn protection", () ->
					new PvpPrevention(
						enabled(builder),
						builder.comment("Whether to also disable PvP in the nether spawn protection, which needs to be enabled for this setting to take effect").define("disable_in_nether", false))
				),
				pushPop(builder, "Rift stabilizer", "Rift stabilizer functionality in spawn protection", () ->
					new RiftStabilizer(
						enabled(builder),
						builder
							.comment("Types of teleportations that are disallowed to happen when trying to teleport from within spawn protection. Disallowed values:",
								Arrays.stream(TeleportationType.values()).map(Enum::name).toList().toString())
							.defineList("disallowed_teleportation_types_from_spawn", List.of(), () -> "", String.class::isInstance),
						builder
							.comment("Types of teleportations that are disallowed to happen when trying to teleport to spawn protection. Disallowed values:",
								Arrays.stream(TeleportationType.values()).map(Enum::name).toList().toString())
							.defineList("disallowed_teleportation_types_to_spawn", List.of(), () -> "", String.class::isInstance),
						builder
							.comment("What minimum permission level is needed to bypass anything this config section disallows.")
							.defineInRange("bypass_permission_level", 1, 0, 4),
						builder
							.comment("The message sent when teleportation is being actively disallowed.")
							.define("message", "{translate:\"scserverutils.teleportation_disallowed\",fallback:\"You cannot teleport within spawn protection.\",with:[],type:\"translatable\",color:\"red\"}"))
				)
			)
		);
		phantomSpawns = pushPop(builder, "Phantom spawns", "Makes it possible to change how many phantoms spawn when the game wants to spawn them.", () ->
			new PhantomSpawns(
				enabled(builder),
				builder.comment("The minimum amount of phantoms to spawn").defineInRange("min_spawns", 0, 0, Integer.MAX_VALUE),
				builder.comment("The maximum amount of phantoms to spawn").defineInRange("max_spawns", 1, 0, Integer.MAX_VALUE),
				builder.comment("Whether to disable phantoms spawning for players in spawn protection").define("disable_in_spawn_protection", true))
		);
		suppressDestroyMismatchLog = pushPop(builder, "Suppress destroy mismatch log", "Removes the \"Mismatch in destroy block pos\" log message to reduce console spam", () ->
			new SuppressDestroyMismatchLog(
				enabled(builder),
				builder.comment("Whether to only disable this message when the destroy position is within spawn protection.").define("only_in_spawn_protection", true))
		);
		teamPermissionLevel = pushPop(builder, "Team command permission level", "Allows changing the permission level for the /team command", () ->
			new TeamPermissionLevel(
				enabled(builder),
				permissionLevel(builder, "team", 1))
		);
	}

	private <T> T pushPop(ModConfigSpec.Builder builder, String categoryName, String categoryComment, Supplier<T> categorySetup) {
		if (categoryComment != null)
			builder.comment(categoryComment);

		T t;

		builder.push(categoryName);
		t = categorySetup.get();
		builder.pop();
		return t;
	}

	private BooleanValue enabled(ModConfigSpec.Builder builder) {
		return enabled(builder, "feature");
	}

	private BooleanValue enabled(ModConfigSpec.Builder builder, String thing) {
		return builder.comment("Whether this " + thing + " is enabled").define("enabled", true);
	}

	private IntValue permissionLevel(ModConfigSpec.Builder builder, String commandName, int defaultPermissionLevel) {
		return builder.comment("The minimum permission level needed for the /" + commandName + " command").defineInRange("permission_level", defaultPermissionLevel, 0, 4);
	}

	private void addCommandConfig(ModConfigSpec.Builder builder, String commandName, int defaultPermissionLevel, Supplier<BiConsumer<CommandDispatcher<CommandSourceStack>, Integer>> registrar) {
		pushPop(builder, commandName, null, () -> {
			commands.add(new CommandConfig(
				enabled(builder, "command"),
				permissionLevel(builder, commandName, defaultPermissionLevel),
				registrar));
			return null;
		});
	}

	private Dimension addSpawnProtectionConfig(ModConfigSpec.Builder builder, String dimensionName, String upperCasedDimensionName) {
		return pushPop(builder, upperCasedDimensionName, "Adds spawn protection to the " + dimensionName, () ->
			new Dimension(
				enabled(builder),
				builder.comment("The square radius in blocks that is under spawn protection.").defineInRange("radius", 32, 0, Integer.MAX_VALUE),
				builder.comment("The X coordinate of the nether spawn's origin").defineInRange("x_origin", 0, Integer.MIN_VALUE, Integer.MAX_VALUE),
				builder.comment("The Z coordinate of the nether spawn's origin").defineInRange("z_origin", 0, Integer.MIN_VALUE, Integer.MAX_VALUE),
				builder.comment("Tag that, when added to a player, makes that player bypass " + dimensionName + " spawn protection").define("bypass_tag", "bypasses_" + dimensionName + "_spawn_protection")
			)
		);
	}
}
