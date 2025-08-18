package org.example.membership.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Spring Boot 3.x 버전 업데이트 이후 trailing-slash 매칭 정책이 변경되었을 수 있으므로
     * 명시적으로 해당 설정을 추가하여 '/path/'와 '/path'를 동일하게 처리하도록 함.
     * 이것이 Swagger UI의 /v3/api-docs/ 요청이 404 오류를 내는 문제를 해결함.
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseTrailingSlashMatch(true);
    }
}