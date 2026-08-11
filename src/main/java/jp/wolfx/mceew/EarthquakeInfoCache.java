/*
 * MCEEW - Japan Earthquake Early Warning notifications for Fabric.
 * Copyright (C) 2026 MCEEW contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */
package jp.wolfx.mceew;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

final class EarthquakeInfoCache {
    private volatile JmaSnapshot jma;

    enum UpdateResult {
        FIRST_VALUE,
        UNCHANGED,
        CHANGED;

        boolean shouldNotify(boolean enabled) {
            return enabled && this == CHANGED;
        }
    }

    JmaSnapshot getJma() {
        return jma;
    }

    synchronized UpdateResult updateJma(JmaSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        UpdateResult result;
        if (jma == null) {
            result = UpdateResult.FIRST_VALUE;
        } else if (Objects.equals(jma.md5(), snapshot.md5())) {
            result = UpdateResult.UNCHANGED;
        } else {
            result = UpdateResult.CHANGED;
        }
        jma = snapshot;
        return result;
    }

    record JmaSnapshot(
            String md5,
            String originTime,
            String region,
            String magnitude,
            String depth,
            String latitude,
            String longitude,
            String intensity,
            String info
    ) {
        Map<String, String> values() {
            Map<String, String> values = new LinkedHashMap<>();
            values.put("origin_time", originTime);
            values.put("region", region);
            values.put("mag", magnitude);
            values.put("depth", depth);
            values.put("lat", latitude);
            values.put("lon", longitude);
            values.put("shindo", intensity);
            values.put("info", info);
            return values;
        }
    }
}
