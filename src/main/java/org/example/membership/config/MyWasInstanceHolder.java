package org.example.membership.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Getter
@Component
public class MyWasInstanceHolder {
    @Setter
    private UUID myUuid;
    @Value("${was.total:1}")
    private int totalWas;
    @Value("${was.index:0}")
    private int myIndex;

    // [WAS Sharding Logic]
    public boolean isMyUser(Long userId) {
        if (userId == null) return true;
        return userId % totalWas == myIndex;
    }
}