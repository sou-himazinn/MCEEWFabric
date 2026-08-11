package jp.wolfx.mceew.websocket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class WebSocketConnectionManagerTest {
    @Test
    void bootstrapQueriesOnlyRequestJapanJmaEarthquakeInfo() {
        assertEquals(1, WebSocketConnectionManager.BOOTSTRAP_QUERIES.size());
        assertEquals(WebSocketConnectionManager.JMA_QUERY,
                WebSocketConnectionManager.BOOTSTRAP_QUERIES.getFirst());
    }
}
