package org.example.membership;

import org.example.membership.entity.User;
import org.example.membership.repository.jpa.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@Transactional
class
SavePerformanceTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveOneByOne() {
        long start = System.currentTimeMillis();

        for (int i = 0; i < 3000; i++) {
            User user = new User("User" + i);
            userRepository.save(user);
        }

        long end = System.currentTimeMillis();
        System.out.println("save() 3000번: " + (end - start) + "ms");
    }

    @Test
    void saveAllOnce() {
        long start = System.currentTimeMillis();

        List<User> users = new ArrayList<>();
        for (int i = 0; i < 3000; i++) {
            users.add(new User("User" + i));
        }
        userRepository.saveAll(users);

        long end = System.currentTimeMillis();
        System.out.println("saveAll() 1번: " + (end - start) + "ms");
    }
}
