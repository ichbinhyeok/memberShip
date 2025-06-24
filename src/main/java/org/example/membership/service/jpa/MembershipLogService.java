package org.example.membership.service.jpa;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.entity.MembershipLog;
import org.example.membership.entity.User;
import org.example.membership.repository.jpa.MembershipLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MembershipLogService {

    private final MembershipLogRepository membershipLogRepository;

    @Transactional
    public void insertMembershipLog(User user, MembershipLevel previousLevel) {
        MembershipLog log = new MembershipLog();
        log.setUser(user);
        log.setPreviousLevel(previousLevel);
        log.setNewLevel(user.getMembershipLevel());
        log.setChangeReason("badge count: " + user.getMembershipLevel());
        membershipLogRepository.save(log);
    }
}
