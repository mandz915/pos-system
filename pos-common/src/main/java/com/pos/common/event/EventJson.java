package com.pos.common.event;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** JSON (de)serialisation of Kafka event payloads. Unknown fields are ignored so events can evolve. */
public final class EventJson {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private EventJson() {
    }

    public static String write(Object event) {
        return MAPPER.writeValueAsString(event);
    }

    public static <T> T read(String json, Class<T> type) {
        return MAPPER.readValue(json, type);
    }
}
