package org.example.membership.service;

import lombok.RequiredArgsConstructor;
import org.example.membership.service.mybatis.MyBatisMembershipRenewalService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MixedMembershipService {

    private final JpaMembershipRenewalService jpaService;
    private final MyBatisMembershipRenewalService myBatisService;

    public void renewAll(LocalDate targetDate) {
        // JPA 기반 갱신 후 로그는 MyBatis로 처리
        jpaService.renewMembershipLevelJpaUpdateInsertForeach(targetDate);
    }

    public void runStatistics(LocalDate targetDate) {
        jpaService.renewMembershipLevelJpaUpdateInsertForeach(targetDate);
    }

    public void runMembership(LocalDate targetDate) {
        jpaService.renewMembershipLevelJpaUpdateInsertForeach(targetDate);
    }

    public void runCoupon(LocalDate targetDate) {
        myBatisService.renewMembershipLevel(targetDate);
    }
}
