package com.airdodge.mastodon.connector.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum MastodonSseType {

    CREATE("create"),
    UPDATE("update"),
    STATUS_UPDATE("status.update"),
    DELETE("delete"),
    UNKNOWN("unknown");

    private final String value;

    MastodonSseType(String value) {
        this.value = value;
    }

    @JsonCreator
    public static MastodonSseType fromValue(String value) {
        for (MastodonSseType it : MastodonSseType.values()) {
            if (it.value.equalsIgnoreCase(value)) {
                return it;
            } else if (STATUS_UPDATE.value.equalsIgnoreCase(value)) {
                return STATUS_UPDATE;
            }
        }
        return UNKNOWN;
    }
}
