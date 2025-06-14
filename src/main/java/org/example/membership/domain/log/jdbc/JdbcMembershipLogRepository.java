package org.example.membership.domain.log.jdbc;

import lombok.RequiredArgsConstructor;
import org.example.membership.domain.log.MembershipLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JdbcMembershipLogRepository {

    private final JdbcTemplate jdbcTemplate;
    private static final String INSERT_SQL =
            "INSERT INTO membership_log (user_id, previous_level, new_level, change_reason, changed_at) " +
                    "VALUES (?, ?, ?, ?, ?)";
    private static final int BATCH_SIZE = 1000;

    public void batchInsert(List<MembershipLog> logs) {
        jdbcTemplate.batchUpdate(
                INSERT_SQL,
                logs,
                BATCH_SIZE,
                (ps, log) -> {
                    ps.setLong(1, log.getUser().getId());
                    ps.setString(2, log.getPreviousLevel().name());
                    ps.setString(3, log.getNewLevel().name());
                    ps.setString(4, log.getChangeReason());
                    ps.setTimestamp(5, Timestamp.valueOf(log.getChangedAt()));
                }
        );
    }
}