package jp.wolfx.mceew;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MceewLanguageTest {
    @Test
    void japaneseMessagesUseNaturalJapanese() {
        MceewLanguage language = MceewLanguage.of("ja_jp");
        String text = language.helpInfo() + language.testNotice();

        assertTrue(text.contains("最新の地震情報"));
        assertFalse(text.contains("莠"));
        assertFalse(text.contains("ﾂ"));
    }

    @Test
    void englishMessagesAreAvailable() {
        MceewLanguage language = MceewLanguage.of("en_us");

        assertTrue(language.helpInfo().contains("latest earthquake information"));
    }

    @Test
    void earthquakeMessagesKeepSeverityAndDetailColors() {
        MceewLanguage language = MceewLanguage.of("ja_jp");
        EewAlert alert = new EewAlert(
                true,
                "警報",
                "",
                "2026/01/01 16:14:18",
                "2026/01/01 16:10:08",
                "46",
                "37.6",
                "137.2",
                "能登半島沖",
                "7.4",
                "10km",
                "7"
        );

        String chat = language.alertChat(alert);
        String subtitle = language.alertSubtitle(alert);

        assertTrue(chat.contains("\u00a7f震央"));
        assertTrue(chat.contains("\u00a7d7"));
        assertTrue(subtitle.contains("\u00a7e"));
        assertTrue(subtitle.contains("\u00a7d7"));
    }
}
