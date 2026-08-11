/*
 * MCEEW - Japan Earthquake Early Warning notifications for Fabric.
 * Copyright (C) 2026 MCEEW contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */
package jp.wolfx.mceew;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class MceewLanguage {
    private static final Map<String, Messages> LANGUAGES = Map.of(
            "ja_jp", japanese(),
            "en_us", english()
    );

    private final Messages messages;

    private MceewLanguage(Messages messages) {
        this.messages = messages;
    }

    static MceewLanguage of(String language) {
        return new MceewLanguage(LANGUAGES.getOrDefault(
                language == null ? "ja_jp" : language.toLowerCase(Locale.ROOT),
                LANGUAGES.get("ja_jp")
        ));
    }

    String commandVersion(String version) {
        return "[MCEEW] v" + version;
    }

    String helpInfo() {
        return messages.helpInfo;
    }

    String helpTest() {
        return messages.helpTest;
    }

    String helpReload() {
        return messages.helpReload;
    }

    String reloaded() {
        return messages.reloaded;
    }

    String reloadFailed() {
        return messages.reloadFailed;
    }

    String noInfo() {
        return messages.noInfo;
    }

    String testNotice() {
        return messages.testNotice;
    }

    String flag(boolean warning) {
        return warning ? messages.warning : messages.forecast;
    }

    String training() {
        return messages.training;
    }

    String assumption() {
        return messages.assumption;
    }

    String finalReport() {
        return messages.finalReport;
    }

    String cancel() {
        return messages.cancel;
    }

    String alertTitle(EewAlert alert) {
        return fill(messages.alertTitle, alert.values());
    }

    String alertSubtitle(EewAlert alert) {
        return fill(messages.alertSubtitle, alert.values());
    }

    String alertChat(EewAlert alert) {
        return fill(messages.alertChat, alert.values());
    }

    String earthquakeInfo(EarthquakeInfoCache.JmaSnapshot snapshot) {
        return fill(messages.earthquakeInfo, snapshot.values());
    }

    private static String fill(String template, Map<String, String> values) {
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String value = entry.getKey().equals("shindo")
                    ? colorIntensity(entry.getValue())
                    : entry.getValue();
            result = result.replace("%" + entry.getKey() + "%", value);
        }
        return result;
    }

    private static String colorIntensity(String intensity) {
        String color = switch (intensity) {
            case "1" -> "7";
            case "2" -> "b";
            case "3" -> "9";
            case "4" -> "a";
            case "5弱", "5-" -> "e";
            case "5強", "5+" -> "6";
            case "6弱", "6-" -> "c";
            case "6強", "6+" -> "4";
            case "7" -> "d";
            default -> "f";
        };
        return "\u00a7" + color + intensity;
    }

    private static Messages japanese() {
        return new Messages(
                "/eew info - 最新の地震情報を表示します",
                "/eew test forecast|alert - テスト通知を送信します",
                "/eew reload - 設定を再読み込みします",
                "設定を再読み込みしました。",
                "設定の再読み込みに失敗しました。ログを確認してください。",
                "地震情報はまだ取得されていません。",
                "これは緊急地震速報のテストです。",
                "警報",
                "予報",
                "訓練",
                "仮定震源",
                "最終報",
                "取消",
                "緊急地震速報（%flag%）",
                "\u00a7e%region%で震度%shindo%\u00a7eの地震",
                "緊急地震速報（%flag%） | 第%num%報 %status%\n"
                        + " \u00a7e%origin_time% \u00a7f発生\n"
                        + " \u00a7f震央: \u00a7e%region%（北緯: \u00a7e%lat%度 東経: \u00a7e%lon%度）\n"
                        + " \u00a7fマグニチュード: \u00a7e%mag%\n"
                        + " \u00a7f深さ: \u00a7e%depth%\n"
                        + " \u00a7f最大震度: \u00a7r%shindo%\n"
                        + " \u00a7f更新時間: \u00a7e%report_time%",
                "\u00a7e地震情報\n"
                        + " \u00a7e%origin_time% \u00a7f発生\n"
                        + " \u00a7f震央: \u00a7e%region%（北緯: \u00a7e%lat%度 東経: \u00a7e%lon%度）\n"
                        + " \u00a7fマグニチュード: \u00a7e%mag%\n"
                        + " \u00a7f深さ: \u00a7e%depth%\n"
                        + " \u00a7f最大震度: \u00a7r%shindo%\n"
                        + " \u00a7f津波情報: \u00a7e%info%"
        );
    }

    private static Messages english() {
        return new Messages(
                "/eew info - Show the latest earthquake information",
                "/eew test forecast|alert - Send a test notification",
                "/eew reload - Reload the configuration",
                "Configuration reloaded.",
                "Configuration reload failed. Check the log.",
                "Earthquake information has not been received yet.",
                "This is an Earthquake Early Warning test.",
                "Warning",
                "Forecast",
                "Training",
                "Assumed hypocenter",
                "Final report",
                "Cancelled",
                "Earthquake Early Warning (%flag%)",
                "\u00a7eEarthquake near %region%, max intensity %shindo%\u00a7e",
                "Earthquake Early Warning (%flag%) | Report %num% %status%\n"
                        + " \u00a7eOrigin time: %origin_time%\n"
                        + " \u00a7fEpicenter: \u00a7e%region% (\u00a7e%lat% N, \u00a7e%lon% E)\n"
                        + " \u00a7fMagnitude: \u00a7e%mag%\n"
                        + " \u00a7fDepth: \u00a7e%depth%\n"
                        + " \u00a7fMax intensity: \u00a7r%shindo%\n"
                        + " \u00a7fReport time: \u00a7e%report_time%",
                "\u00a7eEarthquake Information\n"
                        + " \u00a7eOrigin time: %origin_time%\n"
                        + " \u00a7fEpicenter: \u00a7e%region% (\u00a7e%lat% N, \u00a7e%lon% E)\n"
                        + " \u00a7fMagnitude: \u00a7e%mag%\n"
                        + " \u00a7fDepth: \u00a7e%depth%\n"
                        + " \u00a7fMax intensity: \u00a7r%shindo%\n"
                        + " \u00a7fTsunami information: \u00a7e%info%"
        );
    }

    private static final class Messages {
        final String helpInfo;
        final String helpTest;
        final String helpReload;
        final String reloaded;
        final String reloadFailed;
        final String noInfo;
        final String testNotice;
        final String warning;
        final String forecast;
        final String training;
        final String assumption;
        final String finalReport;
        final String cancel;
        final String alertTitle;
        final String alertSubtitle;
        final String alertChat;
        final String earthquakeInfo;

        Messages(
                String helpInfo,
                String helpTest,
                String helpReload,
                String reloaded,
                String reloadFailed,
                String noInfo,
                String testNotice,
                String warning,
                String forecast,
                String training,
                String assumption,
                String finalReport,
                String cancel,
                String alertTitle,
                String alertSubtitle,
                String alertChat,
                String earthquakeInfo
        ) {
            this.helpInfo = helpInfo;
            this.helpTest = helpTest;
            this.helpReload = helpReload;
            this.reloaded = reloaded;
            this.reloadFailed = reloadFailed;
            this.noInfo = noInfo;
            this.testNotice = testNotice;
            this.warning = warning;
            this.forecast = forecast;
            this.training = training;
            this.assumption = assumption;
            this.finalReport = finalReport;
            this.cancel = cancel;
            this.alertTitle = alertTitle;
            this.alertSubtitle = alertSubtitle;
            this.alertChat = alertChat;
            this.earthquakeInfo = earthquakeInfo;
        }
    }
}
