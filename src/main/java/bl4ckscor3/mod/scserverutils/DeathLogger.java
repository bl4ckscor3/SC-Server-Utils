package bl4ckscor3.mod.scserverutils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public class DeathLogger {
	public static final Path DEATH_LOGS = FMLPaths.getOrCreateGameRelativePath(Paths.get(Configuration.instance.deathLog.savePath().get()));
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

	private DeathLogger() {}

	public static void onLivingDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			Path playerPath = playerPath(player);
			File saveFolder = playerPath.toFile();

			if (!saveFolder.exists())
				saveFolder.mkdirs();

			Path filePath = playerPath.resolve(DATE_FORMAT.format(Date.from(Instant.now())) + ".nbt");

			try {
				DeathInfo deathInfo = DeathInfo.of(player, event.getSource());

				NbtIo.write((CompoundTag) DeathInfo.CODEC.encodeStart(NbtOps.INSTANCE, deathInfo).getOrThrow(), filePath);
			}
			catch (Exception e) {
				SCServerUtils.LOGGER.error("Error trying to save death log " + filePath, e);
			}
		}
	}

	public static CompoundTag getDeath(String log) throws IOException {
		return NbtIo.read(DeathLogger.DEATH_LOGS.resolve(log));
	}

	public static Path playerPath(Player player) {
		return DEATH_LOGS.resolve(player.getGameProfile().getName());
	}
}
