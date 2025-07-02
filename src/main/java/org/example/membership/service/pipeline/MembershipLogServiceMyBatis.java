package org.example.membership.service.pipeline;

import lombok.RequiredArgsConstructor;
import org.example.membership.dto.MembershipLogRequest;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.entity.MembershipLog;
import org.example.membership.entity.User;
import org.example.membership.repository.mybatis.MembershipLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipLogServiceMyBatis {

    private final MembershipLogMapper membershipLogMapper;

    @Transactional
    public void saveAll(List<MembershipLogRequest> logs) {
        membershipLogMapper.bulkInsertRequests(logs);
    }

    @Transactional
    public void insertMembershipLog(User user, MembershipLevel previousLevel) {
        MembershipLog log = new MembershipLog();
        log.setUser(user);
        log.setPreviousLevel(previousLevel);
        log.setNewLevel(user.getMembershipLevel());
        log.setChangeReason("badge count: " + user.getMembershipLevel());
        log.setChangedAt(java.time.LocalDateTime.now());
        membershipLogMapper.insert(log);
    }
}
