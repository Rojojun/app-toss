# 운영 MySQL → PostgreSQL 데이터 이관

기존 MySQL과 백업은 검증·전환이 끝날 때까지 유지한다. 최초 백업은 리허설용이며, 최종 전환 시 기존 앱의 쓰기를 중지하고 새 백업으로 같은 절차를 반복한다. 검증 전에는 홈서버 앱을 실행하지 않는다.

## 변환 도구

`scripts/convert_mysql_dump.py`는 이 프로젝트의 `mysqldump --hex-blob` 백업 전용 변환기다. DB에 접속하거나 원본 파일을 수정하지 않는다.

```bash
python3 scripts/convert_mysql_dump.py /보관경로/mysql.sql /보관경로/postgres-data.sql --instant-zone UTC
```

- 먼저 운영 앱의 시간대와 Hibernate 시간대 설정을 확인한다. `--instant-zone`은 MySQL DATETIME에 저장된 Java Instant 값의 원래 시간대다. 덤프의 `SET TIME_ZONE`만으로 DATETIME의 원래 시간대를 확정할 수 없다.
- 바이너리 UUID를 같은 바이트의 PostgreSQL UUID로, bit를 boolean으로 변환한다.
- 생성일(LocalDateTime)은 timestamp로 보존하고 Instant 필드는 지정한 시간대를 적용해 timestamptz로 변환한다.
- 알 수 없는 테이블·컬럼·리터럴·불완전한 덤프는 거부한다. 출력 파일은 권한 600으로 생성하고 기존 파일은 덮어쓰지 않는다.
- MySQL의 `flyway_schema_history`는 이관하지 않는다. PostgreSQL DB에는 PostgreSQL용 V1을 **Flyway로 먼저 적용**해야 한다. 기존 MySQL DB에서 변경된 V1을 실행하거나 repair하지 않는다.

## 적용 및 검증 순서

1. 새 PostgreSQL DB에 Flyway 적용을 완료한다. 이 단계에서 알림 스케줄러가 있는 운영 앱을 실행하지 않는다.
2. 운영 데이터가 비어 있는 대상에 `psql -v ON_ERROR_STOP=1`로 변환 SQL을 적용한다.
3. 변환 SQL은 전체 대상 테이블에 잠금을 잡고 빈 테이블인지 확인한다. 한 트랜잭션으로 삽입하고 테이블별 건수와 참조 관계를 검증한다. 실패하면 롤백한다. 기존 데이터를 삭제하지 않는다.
4. 원본과 모든 컬럼을 비교한다. 특히 UUID, 익명 사용자 키, 초대 토큰 해시, 수량/버전, 시간/NULL을 확인한다. 데이터 본문은 로그에 출력하지 않는다.
5. 최종 백업의 이관과 검증 후 PostgreSQL 설정으로 앱 한 개를 시작한다. 토스 인증, 기존 공간·물품·이력, 초대, 수량 변경을 확인한다.
6. 이후 AWS 정리 여부를 결정한다. 앱이 새 DB에 쓰기 시작한 후에는 이전 MySQL로 단순히 되돌리면 새 데이터가 누락되므로 별도 복구 계획이 필요하다.

## 현재 리허설

2026-11-19 07:33 UTC 완료 표시가 있는 백업을 로컬 PostgreSQL 17에 적용했다. PostgreSQL용 V1 SQL 적용, 변환 SQL 실행, 9개 업무 테이블의 모든 행/컬럼 비교가 통과했다. Instant 시간대는 UTC 가정이며 운영 설정 확인이 남았다. 이 검증은 Flyway/JPA 앱 시작 검증이나 홈서버 이관 완료를 의미하지 않는다.

| 테이블 | 행 수 |
|---|---:|
| app_user | 10 |
| household | 4 |
| household_member | 4 |
| invitation | 2 |
| item | 3 |
| stock_change | 6 |
| alert_preference | 0 |
| low_stock_event | 0 |
| out_of_stock_alert | 0 |

## 홈서버 적용 결과

사용자가 홈서버 PostgreSQL 17.12에서 Flyway V1 적용 성공과 데이터 SQL의 COMMIT 완료를 확인했다. 건수·참조 관계 검사는 SQL 안에서 수행했다. 별도 로컬 환경에서는 같은 데이터 사본으로 앱 이미지 기동과 인증된 조회까지 확인했다. 홈서버 앱 실행 및 토스 실연동 검증은 아직 남아 있다.
