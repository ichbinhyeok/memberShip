package org.example.membership.config;

import lombok.RequiredArgsConstructor;
import org.example.membership.config.MyWasInstanceHolder;
import org.example.membership.entity.WasInstance;
import org.example.membership.repository.jpa.WasInstanceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;
@Component
@RequiredArgsConstructor
public class WasInstanceRegistrar implements ApplicationRunner {

    private final WasInstanceRepository wasInstanceRepository;
    private final MyWasInstanceHolder myWasInstanceHolder;

    @Value("${was.index}")
    private int index;

    @Override
    public void run(ApplicationArguments args) {
        UUID uuid = UUID.randomUUID();
        WasInstance instance = new WasInstance();
        instance.setId(uuid);
        instance.setIndex(index);
        wasInstanceRepository.save(instance);

        myWasInstanceHolder.setMyUuid(uuid);
        myWasInstanceHolder.setMyIndex(index);
    }
}
