/*
 * MCEEW - Japan Earthquake Early Warning notifications for Fabric.
 * Copyright (C) 2026 MCEEW contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */
package jp.wolfx.mceew;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import jp.wolfx.mceew.websocket.WebSocketConnectionManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class MceewMod implements ModInitializer {
    public static final String MOD_ID = "mceew";
    private static final Logger LOGGER = LoggerFactory.getLogger("MCEEW");
    private static final ZoneId JAPAN = ZoneId.of("Asia/Tokyo");
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "MCEEW WebSocket Scheduler");
                thread.setDaemon(true);
                return thread;
            });
    private final EarthquakeInfoCache earthquakeInfoCache = new EarthquakeInfoCache();

    private volatile MceewConfig config;
    private volatile MceewLanguage language;
    private volatile MinecraftServer server;
    private volatile WebSocketConnectionManager webSocketManager;

    @Override
    public void onInitialize() {
        reloadLocalConfiguration();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                registerCommands(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(startedServer -> {
            server = startedServer;
            startWebSocket();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(stoppingServer -> {
            stopWebSocket();
            server = null;
        });
        LOGGER.info("MCEEW Fabric initialized for Japan JMA EEW only.");
    }

    private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("eew")
                .executes(context -> sendHelp(context.getSource()))
                .then(CommandManager.literal("info")
                        .executes(context -> sendLatestInfo(context.getSource())))
                .then(CommandManager.literal("reload")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> reloadCommand(context.getSource())))
                .then(CommandManager.literal("test")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> sendTest(context.getSource(), false))
                        .then(CommandManager.literal("forecast")
                                .executes(context -> sendTest(context.getSource(), false)))
                        .then(CommandManager.literal("alert")
                                .executes(context -> sendTest(context.getSource(), true)))));
        dispatcher.register(CommandManager.literal("mceew")
                .executes(context -> sendHelp(context.getSource()))
                .then(CommandManager.literal("info")
                        .executes(context -> sendLatestInfo(context.getSource())))
                .then(CommandManager.literal("reload")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> reloadCommand(context.getSource())))
                .then(CommandManager.literal("test")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> sendTest(context.getSource(), false))
                        .then(CommandManager.literal("forecast")
                                .executes(context -> sendTest(context.getSource(), false)))
                        .then(CommandManager.literal("alert")
                                .executes(context -> sendTest(context.getSource(), true)))));
    }

    private int sendHelp(ServerCommandSource source) {
        source.sendFeedback(() -> infoText(language.commandVersion(version())), false);
        source.sendFeedback(() -> infoText(language.helpInfo()), false);
        source.sendFeedback(() -> infoText(language.helpTest()), false);
        source.sendFeedback(() -> infoText(language.helpReload()), false);
        return 1;
    }

    private int sendLatestInfo(ServerCommandSource source) {
        EarthquakeInfoCache.JmaSnapshot snapshot = earthquakeInfoCache.getJma();
        if (snapshot == null) {
            source.sendError(Text.literal(language.noInfo()));
            return 0;
        }
        sendLinesToSource(source, language.earthquakeInfo(snapshot));
        return 1;
    }

    private int reloadCommand(ServerCommandSource source) {
        try {
            reloadLocalConfiguration();
            restartWebSocket();
            source.sendFeedback(() -> infoText(language.reloaded()), false);
            return 1;
        } catch (RuntimeException error) {
            LOGGER.warn("Failed to reload MCEEW.", error);
            source.sendError(Text.literal(language.reloadFailed()));
            return 0;
        }
    }

    private int sendTest(ServerCommandSource source, boolean warning) {
        EewAlert alert = new EewAlert(
                warning,
                language.flag(warning),
                warning ? language.finalReport() : "",
                "2026/01/01 16:14:18",
                "2026/01/01 16:10:08",
                warning ? "46" : "6",
                warning ? "37.6" : "35.4",
                warning ? "137.2" : "140.6",
                warning ? "能登半島沖" : "千葉県東方沖",
                warning ? "7.4" : "4.7",
                "10km",
                warning ? "7" : "3"
        );
        broadcastAlert(alert);
        source.sendFeedback(() -> infoText(language.testNotice()), false);
        return 1;
    }

    private void reloadLocalConfiguration() {
        config = MceewConfig.load(LOGGER);
        language = MceewLanguage.of(config.language);
    }

    private void startWebSocket() {
        MceewConfig activeConfig = config;
        if (!activeConfig.enableEew && !activeConfig.enableEarthquakeInfo) {
            LOGGER.info("MCEEW WebSocket is disabled by configuration.");
            return;
        }
        webSocketManager = new WebSocketConnectionManager(
                listener -> HTTP_CLIENT.newWebSocketBuilder()
                        .buildAsync(URI.create(activeConfig.webSocketUrl), listener),
                (task, delay, unit) -> {
                    ScheduledFuture<?> future = scheduler.schedule(task, delay, unit);
                    return () -> future.cancel(false);
                },
                this::handleWebSocketMessage,
                LOGGER,
                5,
                TimeUnit.SECONDS
        );
        webSocketManager.start();
    }

    private void restartWebSocket() {
        stopWebSocket();
        if (server != null) {
            startWebSocket();
        }
    }

    private void stopWebSocket() {
        WebSocketConnectionManager manager = webSocketManager;
        webSocketManager = null;
        if (manager != null) {
            manager.stop();
        }
    }

    private void handleWebSocketMessage(String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            String type = string(json, "type", "");
            if ("heartbeat".equals(type)) {
                return;
            }
            if ("jma_eew".equals(type) && config.enableEew) {
                handleJmaEew(json);
            } else if ("jma_eqlist".equals(type) && config.enableEarthquakeInfo) {
                handleJmaEarthquakeInfo(json);
            }
        } catch (Exception error) {
            LOGGER.warn("Ignored malformed MCEEW WebSocket message.", error);
        }
    }

    private void handleJmaEew(JsonObject data) {
        String reportTime = string(data, "AnnouncedTime", "");
        if (!isFresh(reportTime, "yyyy/MM/dd HH:mm:ss")) {
            return;
        }
        boolean warning = string(data, "Title", "").contains("警報");
        List<String> statuses = new ArrayList<>();
        if (bool(data, "isTraining")) {
            statuses.add(language.training());
        }
        if (bool(data, "isAssumption")) {
            statuses.add(language.assumption());
        }
        if (bool(data, "isFinal")) {
            statuses.add(language.finalReport());
        }
        if (bool(data, "isCancel")) {
            statuses.clear();
            statuses.add(language.cancel());
        }
        EewAlert alert = new EewAlert(
                warning,
                language.flag(warning),
                String.join(" ", statuses),
                formatJapanTime("yyyy/MM/dd HH:mm:ss", reportTime),
                formatJapanTime("yyyy/MM/dd HH:mm:ss", string(data, "OriginTime", "")),
                string(data, "Serial", ""),
                string(data, "Latitude", ""),
                string(data, "Longitude", ""),
                string(data, "Hypocenter", ""),
                firstString(data, "Magnitude", "Magunitude"),
                string(data, "Depth", "") + "km",
                normalizeIntensity(string(data, "MaxIntensity", ""))
        );
        runOnServerThread(() -> broadcastAlert(alert));
    }

    private void handleJmaEarthquakeInfo(JsonObject data) {
        JsonObject latest = data.getAsJsonObject("No1");
        if (latest == null) {
            return;
        }
        EarthquakeInfoCache.JmaSnapshot snapshot = new EarthquakeInfoCache.JmaSnapshot(
                string(data, "md5", string(latest, "md5", "")),
                formatJapanTime("yyyy/MM/dd HH:mm:ss", string(latest, "time_full", "")),
                string(latest, "location", ""),
                string(latest, "magnitude", ""),
                string(latest, "depth", ""),
                string(latest, "latitude", ""),
                string(latest, "longitude", ""),
                normalizeIntensity(string(latest, "shindo", "")),
                string(latest, "info", "")
        );
        EarthquakeInfoCache.UpdateResult result = earthquakeInfoCache.updateJma(snapshot);
        if (result.shouldNotify(config.enableEarthquakeInfo)) {
            runOnServerThread(() -> broadcastChat(language.earthquakeInfo(snapshot), Formatting.YELLOW));
        }
    }

    private void broadcastAlert(EewAlert alert) {
        if (config.broadcastChat) {
            broadcastChat(language.alertChat(alert), alert.warning() ? Formatting.RED : Formatting.YELLOW);
        }
        if (config.showTitle) {
            broadcastTitle(alert);
        }
        if (config.playSound) {
            playAlertSound();
        }
    }

    private void broadcastChat(String message, Formatting color) {
        MinecraftServer activeServer = server;
        if (activeServer == null) {
            return;
        }
        for (String line : message.split("\\n")) {
            LOGGER.info(stripPlain(line));
            Text text = coloredText(line, color);
            for (ServerPlayerEntity player : activeServer.getPlayerManager().getPlayerList()) {
                player.sendMessage(text);
            }
        }
    }

    private void broadcastTitle(EewAlert alert) {
        MinecraftServer activeServer = server;
        if (activeServer == null) {
            return;
        }
        Text title = coloredText(
                language.alertTitle(alert),
                alert.warning() ? Formatting.RED : Formatting.YELLOW
        );
        Text subtitle = coloredText(language.alertSubtitle(alert), Formatting.YELLOW);
        for (ServerPlayerEntity player : activeServer.getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 70, 20));
            player.networkHandler.sendPacket(new TitleS2CPacket(title));
            player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
        }
    }

    private void playAlertSound() {
        MinecraftServer activeServer = server;
        if (activeServer == null) {
            return;
        }
        Identifier soundId;
        try {
            soundId = Identifier.of(config.sound.id);
        } catch (Exception error) {
            LOGGER.warn("Invalid sound id '{}'.", config.sound.id);
            return;
        }
        SoundEvent soundEvent = SoundEvent.of(soundId);
        for (ServerPlayerEntity player : activeServer.getPlayerManager().getPlayerList()) {
            player.playSoundToPlayer(
                    soundEvent,
                    SoundCategory.MASTER,
                    config.sound.volume,
                    config.sound.pitch
            );
        }
    }

    private void runOnServerThread(Runnable task) {
        MinecraftServer activeServer = server;
        if (activeServer == null) {
            return;
        }
        activeServer.execute(task);
    }

    private boolean isFresh(String time, String pattern) {
        try {
            LocalDateTime reportTime = LocalDateTime.parse(time, DateTimeFormatter.ofPattern(pattern));
            ZonedDateTime report = reportTime.atZone(JAPAN);
            long minutes = Math.abs(Duration.between(report, ZonedDateTime.now(JAPAN)).toMinutes());
            return minutes <= config.freshAlertMinutes;
        } catch (Exception error) {
            return true;
        }
    }

    private String formatJapanTime(String inputPattern, String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            LocalDateTime time = LocalDateTime.parse(value, DateTimeFormatter.ofPattern(inputPattern));
            return time.atZone(JAPAN).format(DateTimeFormatter.ofPattern(config.timeFormat));
        } catch (Exception error) {
            return value;
        }
    }

    private static String normalizeIntensity(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("5-", "5弱")
                .replace("5+", "5強")
                .replace("6-", "6弱")
                .replace("6+", "6強");
    }

    private static String string(JsonObject object, String key, String fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        return object.get(key).getAsString();
    }

    private static String firstString(JsonObject object, String firstKey, String secondKey) {
        String first = string(object, firstKey, "");
        return first.isBlank() ? string(object, secondKey, "") : first;
    }

    private static boolean bool(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() && object.get(key).getAsBoolean();
    }

    private static Text infoText(String value) {
        return Text.literal(value).formatted(Formatting.GREEN);
    }

    private static void sendLinesToSource(ServerCommandSource source, String message) {
        for (String line : message.split("\\n")) {
            source.sendFeedback(() -> coloredText(line, Formatting.YELLOW), false);
        }
    }

    private static Text coloredText(String value, Formatting fallback) {
        MutableText result = Text.empty();
        Formatting current = fallback;
        int segmentStart = 0;
        for (int index = 0; index < value.length() - 1; index++) {
            if (value.charAt(index) != '\u00a7') {
                continue;
            }
            Formatting next = Formatting.byCode(value.charAt(index + 1));
            if (next == null) {
                continue;
            }
            appendColoredSegment(result, value.substring(segmentStart, index), current);
            current = next == Formatting.RESET ? Formatting.WHITE : next;
            index++;
            segmentStart = index + 1;
        }
        appendColoredSegment(result, value.substring(segmentStart), current);
        return result;
    }

    private static void appendColoredSegment(MutableText result, String value, Formatting formatting) {
        if (!value.isEmpty()) {
            result.append(Text.literal(value).formatted(formatting));
        }
    }

    private static String stripPlain(String line) {
        return line.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
    }

    private static String version() {
        return FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }
}
