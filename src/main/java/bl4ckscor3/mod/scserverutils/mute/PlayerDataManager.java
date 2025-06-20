package bl4ckscor3.mod.scserverutils.mute;

import bl4ckscor3.mod.scserverutils.SCServerUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class PlayerDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "mutes.json";

    private final List<PlayerMuteData> entries = new ArrayList<>();
    private final Path dataFile;

    public PlayerDataManager(Path worldPath) {
        this.dataFile = worldPath.resolve(FILE_NAME);
        load(false);
    }

    public List<PlayerMuteData> getEntries() {
        return entries;
    }

    public void addEntry(PlayerMuteData entry) {
        entries.add(entry);
        SCServerUtils.mutedPlayersUUID.add(entry.uuid);
    }

    public void removeEntry(PlayerMuteData entry) {
        entries.remove(entry);
        SCServerUtils.mutedPlayersUUID.remove(entry.uuid);
    }

    public void load(boolean fileNotFound) {
        if (Files.exists(dataFile)) {
            try (Reader reader = Files.newBufferedReader(dataFile)) {
                Type type = new TypeToken<List<PlayerMuteData>>(){}.getType();
                List<PlayerMuteData> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    entries.clear();
                    entries.addAll(loaded);
                }
            } catch (IOException e) {
                System.err.println("Failed to load mutes datas: " + e.getMessage());
            }
        } else {
            if (fileNotFound) {
                System.err.println("Failed to create mutes storage");
                return;
            } else {
                fileNotFound = true;
                try {
                    Files.writeString(dataFile, "[]", StandardOpenOption.CREATE_NEW);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public PlayerMuteData getPlayerDataByName (String playerName) {
        for (PlayerMuteData data: entries) {
            if (data.name.equals(playerName)) {
                return data;
            }
        }
        return null;
    }

    public PlayerMuteData getPlayerDataByUUID (String playerUUID) {
        for (PlayerMuteData data: entries) {
            if (data.uuid.equals(playerUUID)) {
                return data;
            }
        }
        return null;
    }

    public void save() {
        try {
            try (Writer writer = Files.newBufferedWriter(dataFile)) {
                GSON.toJson(entries, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save mutes datas: " + e.getMessage());
        }
    }

}
