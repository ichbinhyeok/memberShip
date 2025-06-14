package org.example.membership.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.domain.user.User;
import org.example.membership.domain.user.mybatis.UserMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "예시 API", description = "Swagger 예시 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/example")
public class ExampleController {

    private final SqlSessionFactory sqlSessionFactory;

    @Operation(summary = "예시 API", description = "Swagger 문서화 예시를 위한 API입니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{id}")
    public String example(
        @Parameter(description = "예시 ID", required = true) @PathVariable Long id
    ) {
        return "Example response for id: " + id;
    }

    @GetMapping("/test-batch-insert")
    public void testBatchInsert() {
        try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH, false)) {
            UserMapper mapper = session.getMapper(UserMapper.class);

            for (int i = 0; i < 10000; i++) {
                User user = new User();
                user.setName("test-user-" + i);
                user.setMembershipLevel(MembershipLevel.SILVER);
                user.setCreatedAt(LocalDateTime.now());
                mapper.insert(user); // 반드시 insert만 테스트
            }

            session.flushStatements(); // 실행!
            session.commit();
        }
    }


} 