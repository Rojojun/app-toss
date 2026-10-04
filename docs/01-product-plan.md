# 공동 생필품 관리 미니앱 기획 v0.1

서비스명: **살림잇다** (영문 SalimLink, appName `salim-link`)

작성일: 2026-09-29

백엔드 구현 기준 문서: [API 계약](./03-api-contract.md) · [사용자 전달·인증 흐름](./02-user-handoff.md)

## 1. 목적과 첫 출시 범위

함께 생활하는 사람들이 생필품의 남은 수량을 공유하고 중복 구매를 줄인다. 앱인토스의 비게임 미니앱으로 제공한다. 첫 출시는 가족 4명이 하나의 공유 공간에서 물품을 등록하고, 수량을 바꾸고, 부족 상태를 확인하는 경험을 완결한다.

가족은 법적 관계나 토스의 가족관계 인증을 뜻하지 않는다. 이 문서에서 가족은 초대를 수락해 같은 `Household`에 참여한 사용자 집합이다.

첫 출시 필수 기능:

1. 토스 사용자 식별키(anonKey)로 사용자 식별(로그인 화면 없음), 우리 API용 단기 인증 토큰 발급
2. 공유 공간 생성, 초대 링크 발급, 초대 수락 및 멤버 조회
3. 물품 등록·조회·수정, 수량 변경, 부족 기준 설정
4. 수량 변경 이력과 부족 상태 표시
5. 사용자가 신청한 부족 알림을 위한 동의·발송 흐름. 앱인토스 문구 검수 승인이 발송의 선행 조건이다.

첫 출시 범위 밖: 토스쇼핑 특정 상품 연결, 상품 인기 순위·추천, 자동 구매 감지, 결제, 바코드·영수증 인식, 정확한 소비량 예측. 토스쇼핑 이동은 목적지 규격·실기기 동작·심사 결과를 확인한 후 별도 기능으로 결정한다.

## 2. 사용자 시나리오

아들이 `우리 집 생필품` 공간을 만들고 부모와 동생에게 각각 초대 링크를 보낸다. 세 사람은 토스에서 미니앱을 열고 초대를 확인한 뒤 수락한다. 모두 치약의 현재 수량을 볼 수 있다. 엄마가 치약 수량을 2개에서 1개로 변경한다. 치약의 부족 기준이 1개라면 물품은 `부족`으로 표시된다. 사전에 부족 알림을 신청하고 동의한 멤버에게만 검수된 재고 사실 알림을 보낸다. 멤버가 치약을 3개로 바꾸면 부족 상태가 해제된다.

## 3. 데이터와 책임

| 개념 | 책임 | 주요 필드 |
| --- | --- | --- |
| `User` | 서비스 사용자의 내부 식별 | `id`, 앱별 `anonKey`(유일), `status`, `createdAt` |
| `Household` | 공유 공간 | `id`, `name`, `createdBy`, `createdAt` |
| `Membership` | 사용자와 공간의 참여 관계 | `householdId`, `userId`, `role`, `joinedAt` |
| `Invitation` | 초대 수락 권한의 제한 | `id`, `householdId`, `tokenHash`, `expiresAt`, `maxUses`, `usedCount`, `revokedAt` |
| `Item` | 물품과 현재 재고 | `id`, `householdId`, `name`, `categoryIcon`, `unit`, `quantity`, `lowStockThreshold`, `version` |
| `StockChange` | 수량 변경 이력 | `id`, `itemId`, `actorUserId`, `beforeQuantity`, `afterQuantity`, `createdAt` |
| `AlertPreference` | 멤버별 부족 알림 신청 상태 | `householdId`, `userId`, `enabled` |
| `LowStockEvent` | 부족 상태에 진입한 사실 | `id`, `itemId`, `itemVersion`, `occurredAt` |
| `AlertDelivery` | 수신자별 발송 상태와 중복 방지 | `eventId`, `userId`, `status`, `attemptedAt` |

`User`는 토스의 앱별 사용자 식별키(`anonKey`)로 연결한다. 일회용 인증 코드와 우리 API용 인증 토큰은 사용자 식별자가 아니다. 앱 재진입 때 새 코드와 토큰을 받아도 같은 `anonKey`이면 같은 내부 `User.id`를 찾는다. 이름, 전화번호, 가족관계는 첫 출시에서 받지 않는다. `Membership`은 수락한 사람에게만 생성하며 현재 참여 관계는 조회 시점에 DB에서 확인한다. `Invitation`은 인증이 아니라 공간에 들어갈 수 있는 초대 권한이므로, 사용자 확인과 초대 수락을 모두 요구한다.

## 4. 핵심 규칙

### 공유 공간과 멤버

- 한 사용자는 여러 공간에 참여할 수 있다. 한 공간에 같은 사용자가 중복 가입할 수 없다.
- 첫 버전의 역할은 `OWNER`와 `MEMBER`이다. 소유자는 초대 발급·취소와 공간 이름 변경이 가능하다. 두 역할 모두 물품과 수량을 수정할 수 있다.
- 초대 링크는 추측하기 어려운 불투명 토큰을 사용한다. 만료, 사용 횟수, 취소 상태를 서버에서 검사한다. `C1J4KL`처럼 짧은 표시 코드는 단독 인증 수단으로 사용하지 않는다.
- 초대 URL에 토큰을 담되 토큰 자체와 가족 정보를 로그·분석 이벤트에 남기지 않는다. 링크를 연 것만으로 가입시키지 않고, 공간 이름을 보여준 뒤 사용자가 수락한다.
- 멤버가 공간을 떠난 뒤에는 그 공간의 물품에 접근할 수 없다. 소유자 이탈과 공간 삭제는 후속 정책으로 확정한다.

### 물품과 수량

- 물품의 수량은 0 이상의 정수다. 단위는 `개`, `팩`처럼 물품별로 지정한다. 부족 조건은 `quantity <= lowStockThreshold`이다.
- 모든 변경은 서버에서 멤버 권한을 검사하고, `Item` 갱신과 `StockChange` 기록을 한 트랜잭션으로 처리한다.
- 동시 수정으로 이전 수량을 덮어쓰지 않도록 `version`을 확인한다. 충돌하면 최신 수량을 돌려주고 다시 변경하도록 안내한다.
- 목록의 아이콘은 미리 정한 물품 카테고리에서 선택한다. 특정 상품·브랜드의 사진이나 인기 순위를 첫 버전에 넣지 않는다.

### 알림

- 수량이 `부족 아님 → 부족`으로 바뀔 때 한 번만 부족 이벤트를 만든다. 부족 상태가 유지되는 동안 같은 알림을 반복하지 않는다. 수량이 기준보다 높아진 뒤 다시 부족해지면 새 이벤트가 된다.
- 알림 신청과 앱인토스 알림 동의가 확인된 멤버에게만 발송한다. 동의를 거부해도 공동 목록은 쓸 수 있다.
- 기능성 알림은 재고 사실만 전한다. 예: 제목 `치약 재고`, 본문 `치약이 1개 남았어요.` 구매 유도, 브랜드 추천, 할인 정보는 포함하지 않는다.
- 서버는 앱인토스의 승인된 기능성 템플릿으로 발송한다. 문구 검수 승인이 나기 전에는 외부 푸시 없이 미니앱 내부 부족 상태만 제공한다.
- 발송 실패와 재시도는 수신자별로 기록한다. `(eventId, userId)`를 유일하게 관리해 같은 이벤트·수신자에게 중복 발송하지 않도록 한다.

## 5. 백엔드 API 초안

인증된 요청은 우리 서버가 발급한 단기 인증 토큰의 내부 `User.id`로 사용자를 식별한다. 별도 `AuthSession` 테이블은 첫 버전에 두지 않는다. `userId`를 요청 본문에서 신뢰하지 않는다. 모든 `householdId` 접근에서 현재 `Membership`을 DB에서 확인한다.

| 순서 | API | 동작 |
| --- | --- | --- |
| 1 | `POST /auth/toss/exchange` | 미니앱의 토스 인가 코드를 서버에서 교환하고 우리 API용 단기 인증 토큰 발급 |
| 2 | `GET /me` | 현재 사용자와 참여 공간 조회 |
| 3 | `POST /households` | 공간 생성, 소유자 멤버십 생성 |
| 4 | `GET /households/{id}` | 공간과 멤버 조회 |
| 5 | `POST /households/{id}/invitations` | 초대 발급 |
| 6 | `GET /invitations/{token}` | 수락 전 공간 정보와 초대 상태 조회 |
| 7 | `POST /invitations/{token}/accept` | 초대 사용과 멤버십 생성 |
| 8 | `GET /households/{id}/items` | 물품 목록과 부족 상태 조회 |
| 9 | `POST /households/{id}/items` | 물품 등록 |
| 10 | `PATCH /households/{id}/items/{itemId}` | 물품 정보와 기준 수정 |
| 11 | `POST /households/{id}/items/{itemId}/stock-changes` | 예상 `version`과 새 수량을 받아 원자적으로 변경 |
| 12 | `PUT /households/{id}/alert-preference` | 현재 멤버의 부족 알림 신청 상태 변경 |

HTTP 상태와 오류 코드는 API 계약 작성 단계에서 고정한다. 초대 만료·횟수 소진, 비회원 접근, 수량 충돌, 알림 비동의는 서로 다른 오류로 반환한다.

## 6. 백엔드부터 진행하는 순서

1. 도메인 규칙과 PostgreSQL 스키마 확정: 멤버십 유일성, 수량 범위, 버전, 초대 사용 횟수.
2. 순수 도메인 동작 구현: 초대 수락, 수량 변경, 부족 상태 전이. 경계 사례를 테스트한다.
3. API와 권한 구현: 인증 토큰으로 `User`를 확인하고 매 요청 현재 `Membership`을 검증한다. 토스 연동 전에는 테스트용 인증 어댑터로 API 계약을 검증할 수 있다.
4. 토스 사용자 식별키 어댑터 연결: 인증 코드 교환과 사용자 키 매핑, 실기기 테스트.
5. 기능성 알림 연결: 동의 UI와 문구 검수 이후 서버 발송·중복 방지 검증.
6. 앱인토스 미니앱 UI와 초대 링크 연결, 비게임 출시 가이드에 따라 실기기 검수.

## 7. 완료 기준과 외부 확인 항목

백엔드 1차 완료 기준: 초대받지 않은 사용자는 공간에 들어갈 수 없고, 수락한 네 명은 같은 목록을 보며, 동시 수량 변경에서 값이 유실되지 않고, 부족 진입당 이벤트가 한 번 기록된다.

외부 확인 항목: 앱인토스 콘솔·mTLS 인증서, 토스 앱 QR 테스트, 기능성 알림 문구 승인, 비게임 미니앱 출시 검수. 문서와 자동 테스트만으로 외부 기능이 승인·작동한다고 주장하지 않는다.

## 공식 근거

- [비게임 출시 가이드](https://developers-apps-in-toss.toss.im/checklist/app-nongame)
- [서비스 오픈 정책](https://developers-apps-in-toss.toss.im/intro/guide)
- [사용자 식별키 인증 코드 발급](https://developers-apps-in-toss.toss.im/guide/authentication/anonymous-key-auth-code)
- [미니앱 공유 링크](https://developers-apps-in-toss.toss.im/documentation/common/growth/share/miniapp-share-link)
- [스마트 발송 가이드](https://developers-apps-in-toss.toss.im/guide/marketing/smart-message)
- [2026-09-10 광고성 푸시 종료 공지](https://toss.im/apps-in-toss/blog/update-26-09-10)
