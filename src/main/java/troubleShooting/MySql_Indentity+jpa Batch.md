MySQL + JPA IDENTITY 전략의 배치 인서트 한계와 실무 대안 적용기 (등급 갱신 시스템 사례 기반)

1. 문제 상황 요약

Spring Data JPA 기반의 등급 갱신 시스템에서 DataGenerationService를 통해 사용자 3만 건, 주문 30만 건의 더미 데이터를 생성하는 과정에서 saveAll() 호출 시 insert 쿼리가 하나씩 개별 실행되는 현상이 발생했다.

기대했던 결과

INSERT INTO users (name, membership_level, created_at) VALUES (...), (...), (...);

실제 로그

INSERT INTO users (...) VALUES (...);  -- N번 반복

JPA 설정은 아래와 같이 모두 적용되어 있었다:

spring.jpa.properties.hibernate.jdbc.batch_size=100
spring.jpa.properties.hibernate.order_inserts=true

그럼에도 불구하고, Hibernate는 쿼리를 배치로 묶지 않고 매 건 즉시 insert하였다.

2. 테스트 환경

항목

값

DB

MySQL 8.x

ORM

Spring Data JPA + Hibernate

ID 전략

@GeneratedValue(strategy = GenerationType.IDENTITY)

데이터량

사용자 30,000건 / 주문 300,000건

시스템

등급 갱신 서비스용 더미 데이터 생성 배치

3. 원인 분석

3.1 Hibernate 배치 인서트 전제 조건

Hibernate의 batch insert가 동작하려면 아래 조건을 모두 충족해야 한다:

동일 테이블에 대한 insert일 것

flush 이전까지 쿼리를 모을 수 있을 것

엔티티의 ID를 insert 이전에 사전 할당할 수 있을 것

3.2 IDENTITY 전략의 구조적 한계와 동작 원리

@GeneratedValue(strategy = GenerationType.IDENTITY)는 다음과 같은 구조적 제약을 가진다:

IDENTITY는 DB가 insert 이후에 PK를 생성해 반환한다 (auto-increment)

JPA는 persist/save 시점에 엔티티의 ID가 필요하기 때문에 즉시 insert 쿼리를 실행한다

따라서 Hibernate는 쿼리를 모을 수 없고, flush 타이밍 이전에 insert가 강제 실행됨

결과적으로 Hibernate의 batch insert 기능은 비활성화되며, saveAll() 호출 시에도 N개의 insert가 순차 실행된다

📌 즉, IDENTITY 전략을 사용하는 순간부터 JPA의 batch insert는 구조적으로 봉인된다.

4. 해결 방안 비교

전략

설명

장점

단점

SEQUENCE

DB 시퀀스로 ID 사전 할당

batch 가능

MySQL 미지원

TABLE

ID 전용 테이블 사용

batch 가능

동시성 약함, 성능 저하 가능

JDBC

PreparedStatement.addBatch() 직접 구현

빠름, 제어 가능

코드 복잡도 증가, JPA 일관성 깨짐

MyBatis

Mapper XML로 batch 처리

SQL 제어 유리

Mapper 관리 필요

5. 실제 적용: FastDataGenerationService (JDBC 기반)

등급 갱신 시스템에서는 더미 데이터를 빠르게 생성하기 위해 순수 JDBC로 FastDataGenerationService를 별도로 구현했다.

PreparedStatement ps = conn.prepareStatement(
"INSERT INTO users (name, membership_level, created_at) VALUES (?, ?, ?)"
);
for (int i = 0; i < count; i++) {
ps.setString(1, name);
ps.setString(2, "SILVER");
ps.setTimestamp(3, Timestamp.valueOf(createdAt));
ps.addBatch();
if (i % BATCH_SIZE == 0) {
ps.executeBatch();
ps.clearBatch();
}
}
ps.executeBatch();
conn.commit();

✅ 결과

TPS 수십 배 향상

multi-values insert 쿼리 형태로 실행

전체 처리 시간 대폭 단축

JPA 환경보다 테스트 반복 생산성과 피드백 루프가 빠르게 개선됨

6. 결론 및 교훈

MySQL에서 @GeneratedValue(strategy = GenerationType.IDENTITY)를 사용하는 경우,
Hibernate의 batch insert는 구조적으로 작동하지 않는다. 이는 단순히 설정 문제로 해결할 수 없으며, ID를 즉시 알 수 없는 전략적 제약 때문이다.

대안 적용 원칙 (등급 갱신 시스템 기준)

테스트용 더미 데이터 생성에는 JDBC 기반의 FastDataGenerationService를 별도로 분리 적용

실제 등급 갱신 로직(JPA 기반)은 정합성과 로그 기록의 신뢰성에 집중하고, 속도보다 트랜잭션 안전성 중심으로 운영