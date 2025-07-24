package org.example.membership.config;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MyWasInstanceHolder {
    private UUID myUuid;

    public void setMyUuid(UUID uuid) {
        this.myUuid = uuid;
    }

    public UUID getMyUuid() {
        return myUuid;
    }
}
