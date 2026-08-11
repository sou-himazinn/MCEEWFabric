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

record EewAlert(
        boolean warning,
        String flag,
        String status,
        String reportTime,
        String originTime,
        String reportNumber,
        String latitude,
        String longitude,
        String region,
        String magnitude,
        String depth,
        String intensity
) {
    Map<String, String> values() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("flag", flag);
        values.put("status", status);
        values.put("report_time", reportTime);
        values.put("origin_time", originTime);
        values.put("num", reportNumber);
        values.put("lat", latitude);
        values.put("lon", longitude);
        values.put("region", region);
        values.put("mag", magnitude);
        values.put("depth", depth);
        values.put("shindo", intensity);
        return values;
    }
}
