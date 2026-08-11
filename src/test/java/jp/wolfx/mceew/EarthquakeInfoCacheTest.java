package jp.wolfx.mceew;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class EarthquakeInfoCacheTest {
    @Test
    void jmaSnapshotsAreClassifiedByMd5() {
        EarthquakeInfoCache cache = new EarthquakeInfoCache();
        EarthquakeInfoCache.JmaSnapshot first = snapshot("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        EarthquakeInfoCache.JmaSnapshot second = snapshot("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");

        assertEquals(EarthquakeInfoCache.UpdateResult.FIRST_VALUE, cache.updateJma(first));
        assertEquals(EarthquakeInfoCache.UpdateResult.UNCHANGED, cache.updateJma(first));
        assertEquals(EarthquakeInfoCache.UpdateResult.CHANGED, cache.updateJma(second));
    }

    private static EarthquakeInfoCache.JmaSnapshot snapshot(String md5) {
        return new EarthquakeInfoCache.JmaSnapshot(
                md5,
                "2026/01/01 00:00:00",
                "能登半島沖",
                "5.0",
                "10km",
                "37.0",
                "137.0",
                "4",
                "なし"
        );
    }
}
