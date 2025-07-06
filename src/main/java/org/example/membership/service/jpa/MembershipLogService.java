package org.example.membership.service.jpa;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.entity.MembershipLog;
import org.example.membership.entity.User;
import org.example.membership.repository.jpa.MembershipLogRepository;
import org.example.membership.repository.jpa.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MembershipLogService {

    private final MembershipLogRepository membershipLogRepository;
    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void insertMembershipLog(User user, MembershipLevel previousLevel) {
        MembershipLog log = new MembershipLog();
        log.setUser(user);
        log.setPreviousLevel(previousLevel);
        log.setNewLevel(user.getMembershipLevel());
        log.setChangeReason("badge count: " + user.getMembershipLevel());
        membershipLogRepository.save(log);
    }

    @Transactional
    public void bulkInsertMembershipLogs() {
        List<User> users = userRepository.findAll();

        final int BATCH_SIZE = 1000;
        int count = 0;

        for (User user : users) {
            insertMembershipLog(user, user.getMembershipLevel());
            count++;
            flushAndClearIfNeeded(count, BATCH_SIZE);
        }

        entityManager.flush();
        entityManager.clear();
    }

    private void flushAndClearIfNeeded(int count, int batchSize) {
        if (count % batchSize == 0) {
            entityManager.flush();
            entityManager.clear();
        }
    }
}
