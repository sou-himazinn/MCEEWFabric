/*
 * MCEEW - Japan Earthquake Early Warning notifications for Fabric.
 * Copyright (C) 2026 MCEEW contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */
package jp.wolfx.mceew;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;

final class MceewConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "mceew.json";

    String language = "ja_jp";
    String webSocketUrl = "wss://ws-api.wolfx.jp/all_eew";
    boolean enableEew = true;
    boolean enableEarthquakeInfo = true;
    boolean broadcastChat = true;
    boolean showTitle = true;
    boolean playSound = true;
    int freshAlertMinutes = 10;
    String timeFormat = "yyyy/MM/dd HH:mm:ss";
    Sound sound = new Sound();

    static MceewConfig load(Logger logger) {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        try {
            Files.createDirectories(configPath.getParent());
            if (Files.notExists(configPath)) {
                MceewConfig defaults = new MceewConfig();
                write(configPath, defaults);
                return defaults;
            }
            try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                MceewConfig config = GSON.fromJson(reader, MceewConfig.class);
                if (config == null) {
                    throw new IOException("Configuration was empty");
                }
                config.repair();
                write(configPath, config);
                return config;
            }
        } catch (Exception error) {
            logger.warn("Failed to load MCEEW configuration. A default configuration will be used.", error);
            backupInvalidConfig(configPath, logger);
            MceewConfig fallback = new MceewConfig();
            try {
                write(configPath, fallback);
            } catch (IOException writeError) {
                throw new UncheckedIOException(writeError);
            }
            return fallback;
        }
    }

    private static void write(Path path, MceewConfig config) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(config, writer);
        }
    }

    private static void backupInvalidConfig(Path configPath, Logger logger) {
        if (Files.notExists(configPath)) {
            return;
        }
        Path backup = configPath.resolveSibling("mceew.invalid." + Instant.now().toEpochMilli() + ".json");
        try {
            Files.move(configPath, backup);
            logger.warn("Invalid MCEEW configuration was moved to {}.", backup.getFileName());
        } catch (IOException error) {
            logger.warn("Could not back up the invalid MCEEW configuration.", error);
        }
    }

    private void repair() {
        if (!"en_us".equalsIgnoreCase(language) && !"ja_jp".equalsIgnoreCase(language)) {
            language = Locale.getDefault().getLanguage().equals(Locale.JAPANESE.getLanguage())
                    ? "ja_jp"
                    : "en_us";
        }
        if (webSocketUrl == null || webSocketUrl.isBlank()) {
            webSocketUrl = "wss://ws-api.wolfx.jp/all_eew";
        }
        if (freshAlertMinutes < 1) {
            freshAlertMinutes = 10;
        }
        if (timeFormat == null || timeFormat.isBlank()) {
            timeFormat = "yyyy/MM/dd HH:mm:ss";
        }
        if (sound == null) {
            sound = new Sound();
        }
        sound.repair();
    }

    static final class Sound {
        String id = "minecraft:block.note_block.pling";
        float volume = 1.0F;
        float pitch = 1.0F;

        private void repair() {
            if (id == null || id.isBlank()) {
                id = "minecraft:block.note_block.pling";
            }
            if (!Float.isFinite(volume) || volume <= 0.0F) {
                volume = 1.0F;
            }
            if (!Float.isFinite(pitch) || pitch <= 0.0F) {
                pitch = 1.0F;
            }
        }
    }
}
