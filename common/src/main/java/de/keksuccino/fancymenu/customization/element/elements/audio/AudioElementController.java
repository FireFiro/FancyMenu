package de.keksuccino.fancymenu.customization.element.elements.audio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.file.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.*;

//TODO übernehmen
public class AudioElementController {

    public static final File METAS_FILE = new File(FancyMenu.MOD_DIR, "/audio_element_controller_metas.json");

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, AudioElementMeta> METAS = new HashMap<>();

    private static boolean initialized = false;

    protected static void read() {

        METAS.clear();

        try {

            METAS_FILE.createNewFile();

            List<String> jsonList = FileUtils.readTextLinesFrom(METAS_FILE);
            StringBuilder json = new StringBuilder();
            jsonList.forEach(json::append);
            String jsonString = json.toString();

            if (jsonString.isBlank()) return;

            List<AudioElementMeta> metasList = GSON.fromJson(jsonString, new TypeToken<List<AudioElementMeta>>(){}.getType());
            for (AudioElementMeta meta : metasList) {
                METAS.put(Objects.requireNonNull(meta.element_identifier), meta);
            }

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to read Audio element controller metas from file!", ex);
        }

    }

    protected static void write() {

        if (!initialized) read();
        initialized = true;

        try {

            METAS_FILE.createNewFile();

            List<AudioElementMeta> metasList = new ArrayList<>(METAS.values());
            String json = GSON.toJson(metasList);

            FileUtils.writeTextToFile(METAS_FILE, false, Objects.requireNonNull(json));

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to write Audio element controller metas to file!", ex);
        }

    }

    public static void syncChanges() {
        if (!initialized) read();
        initialized = true;
        write();
    }

    @Nullable
    public static AudioElementMeta getMeta(@NotNull String elementIdentifier) {
        if (!initialized) read();
        initialized = true;
        return METAS.get(elementIdentifier);
    }

    public static void putMeta(@NotNull String elementIdentifier, @NotNull AudioElementMeta meta) {
        if (!initialized) read();
        initialized = true;
        METAS.put(elementIdentifier, meta);
        syncChanges();
    }

    public static class AudioElementMeta {

        public String element_identifier;
        public float volume;

        public AudioElementMeta(@NotNull String element_identifier, float volume) {
            this.element_identifier = element_identifier;
            this.volume = volume;
        }

    }

}
