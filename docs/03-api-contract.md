# 가족 생필품 API 계약 초안 v0.1

작성일: 2026-10-01  
대상: 앱인토스 미니앱 ↔ `family-share` 백엔드  
기계 판독 명세: [openapi.yaml](./openapi.yaml)

## 이 문서의 상태

이 문서는 API 계약이다. Postman에서 `openapi.yaml`을 Import하면 요청 목록과 스키마를 볼 수 있다. 아래 21개 요청은 모두 서버에 구현되어 있고, 점검용 `GET /health`(인증 없음)가 더 있다. 부족 알림 발송기(`AlertDelivery`)는 문구 승인 전이라 구현하지 않았다.

기획 기준은 [제품 기획](./01-product-plan.md)과 [사용자 전달·인증 흐름](./02-user-handoff.md)이다. 이번 초안은 백엔드 구현과 API 단독 테스트에 필요한 입력, 출력, 권한, 오류를 고정한다. 실제 토스 API 연결, 푸시 발송, 토스쇼핑 이동은 이 명세가 동작한다는 뜻이 아니다.

## 공통 계약

- 기본 경로: `/api/v1`. 예시 서버 `http://localhost:8080`은 Postman 로컬 테스트용이다.
- JSON 요청·응답. 성공은 리소스 또는 명령 결과를 바로 반환한다. 빈 성공 응답은 `204`.
- 미니앱에 진입할 때 `POST /auth/toss/exchange`를 호출해 토스가 확인한 사용자 식별키로 우리 앱 사용자를 조회하거나 등록한다. 이후 API는 `Authorization: Bearer <우리 API 토큰>`으로 인증한다. 초대 미리보기는 공개한다.
- 토큰의 subject는 내부 `app_user.id`다. 가족 접근권은 토큰에 저장하지 않고 요청마다 현재 멤버십을 DB에서 확인한다. 요청 본문의 `userId`와 `anonKey`를 신원으로 사용하지 않는다.
- 토스 사용자 식별키 인증 코드는 서버에서 교환해 `anonKey`를 확인한다. 기존 `anonKey`면 같은 내부 유저를 반환한다. 유저 생성은 DB 유일 제약과 동시 요청 처리가 필요하다.
- `UUID`는 문자열, 날짜 시각은 UTC 오프셋이 있는 ISO 8601 문자열로 보낸다.
- 오류는 `{ "code": "...", "message": "...", "details": {} }` 형식이다. `message`는 표시용이며 분기는 `code`로 한다.
- 다른 가족의 리소스는 존재 여부를 드러내지 않도록 `404`로 처리한다. 같은 가족 안에서 소유자 권한이 부족하면 `403`이다.
- 이 버전에서 수량은 음수가 아닌 정수다. 부족 판정은 `quantity <= lowStockThreshold`이다.

## API 순서와 권한

| 순서 | 요청 | 권한 | 핵심 결과 |
| --- | --- | --- | --- |
| 1 | `POST /auth/toss/exchange` | 공개 | 매 진입 시 토스 사용자 검증, 기존 앱 사용자 조회 또는 신규 등록, API 토큰 발급 |
| 2 | `GET /me` | 로그인 | 내 정보와 현재 참여 공간 |
| 3 | `POST /households` | 로그인 | 공간과 `OWNER` 멤버십 생성 |
| 4 | `GET /households/{householdId}` | 멤버 | 공간 및 멤버 조회 |
| 5 | `PATCH /households/{householdId}` | 소유자 | 공간 이름 변경 |
| 6 | `DELETE /households/{householdId}/membership` | 멤버 | 본인 멤버십 탈퇴 |
| 7 | `POST /households/{householdId}/invitations` | 소유자 | 만료·횟수가 제한된 초대 발급 |
| 8 | `GET /invitations/{token}` | 공개 | 수락 전 공간 이름과 초대 가능 여부 |
| 9 | `POST /invitations/{token}/accept` | 로그인 | 멤버십 생성 또는 기존 멤버십 반환 |
| 10 | `DELETE /households/{householdId}/invitations/{invitationId}` | 소유자 | 초대 취소 |
| 11 | `DELETE /households/{householdId}/members/{userId}` | 소유자 | 일반 멤버 내보내기 |
| 12 | `GET /households/{householdId}/items` | 멤버 | 물품·현재 수량·부족 여부 |
| 13 | `POST /households/{householdId}/items` | 멤버 | 물품 생성 |
| 14 | `PATCH /households/{householdId}/items/{itemId}` | 멤버 | 물품 정보·부족 기준 수정 |
| 15 | `POST /households/{householdId}/items/{itemId}/stock-changes` | 멤버 | 예상 버전 확인 후 수량 변경·이력 기록 |
| 16 | `GET /households/{householdId}/items/{itemId}/stock-changes` | 멤버 | 최신 수량 변경 이력 |
| 17 | `GET /households/{householdId}/alert-preference` | 멤버 | 내 부족 알림 신청 상태 |
| 18 | `PUT /households/{householdId}/alert-preference` | 멤버 | 내 신청 상태 설정 |
| 19 | `POST /households/{householdId}/ownership-transfer` | 소유자 | 다른 멤버에게 소유권 이전 |
| 20 | `DELETE /households/{householdId}` | 소유자 | 소유자 혼자 남았을 때 공간 해산 |
| 21 | `PATCH /households/{householdId}/membership` | 멤버 | 이 공간에서 쓰는 내 이름(닉네임) 변경 |

`GET /me`의 `households`가 공간 목록을 대신한다. 첫 버전에는 별도의 `GET /households`를 두지 않는다. 물품 삭제는 이 계약에서 보류한다.

### 닉네임

- 사용자 식별키 방식에는 이름이 없으므로, **공간마다** 멤버가 쓰는 이름(1~20자)을 둔다. 공간을 만들 때(`POST /households`)와 초대를 수락할 때(`POST /invitations/{token}/accept`) 필수로 받는다. 이미 멤버인 사용자의 재수락은 이름을 바꾸지 않는다.
- 수량 변경 이력에는 **변경한 시점의 닉네임**(`actorNickname`)을 함께 저장한다. 이후 이름을 바꾸거나 공간을 나가도 이력의 이름은 그대로다.

### 소유권과 공간 해산 정책

- 소유자는 다른 멤버가 있으면 공간을 나갈 수 없다. 먼저 `ownership-transfer`로 소유권을 넘기거나(`409 OWNER_TRANSFER_REQUIRED`), 혼자 남은 뒤 공간을 해산한다.
- 공간 해산(`DELETE /households/{householdId}`)은 소유자만, 소유자 혼자 남았을 때만 가능하다. 다른 멤버가 있으면 `409 HOUSEHOLD_NOT_EMPTY`다. 해산하면 공간의 물품·수량 변경 이력·초대·알림 신청·부족 이벤트·멤버십이 함께 삭제된다.
- 소유권 이전 대상은 현재 멤버여야 하고(아니면 `404`), 본인에게는 이전할 수 없다(`400 VALIDATION_ERROR`).
- 사용자 상태가 `INACTIVE`가 되면 그 사용자의 기존 API 토큰은 다음 요청에서 바로 거부된다(`401`). 다시 교환하면 `403 USER_INACTIVE`다. 사용자 식별키 방식에는 토스 로그인의 연결 끊기 콜백이 없어서, 비활성화는 운영자가 직접 한다.
- 소유자가 사라지는 경우(계정 정리 등)의 승계 정책은 이 버전에 없다. 소유권 이전과 해산은 소유자가 직접 호출한다.

### 진입 시 기존 사용자 확인과 신규 등록

`POST /auth/toss/exchange`가 확인과 등록을 함께 처리한다. 미니앱 진입 때 `User.createAnonymousKeyAuthCode()`로 받은 새 `code`를 보내면 서버가 토스에서 교환해 확인한 `anonKey`로 `app_user`를 조회한다. 별도의 로그인 화면이나 약관 동의 화면은 없다.

- 이미 등록된 사용자: 기존 `app_user.id`를 그대로 사용하고 `200`과 새 API Bearer 토큰을 반환한다.
- 처음 온 사용자: `app_user`를 한 번 생성하고 `201`과 API Bearer 토큰을 반환한다.
- 두 경우 모두 이후 요청은 같은 인증 방식으로 처리한다. 가족 공간 유무와 현재 멤버십은 `GET /me`에서 조회한다.

따라서 별도의 공개 `POST /user/register?anonKey=...`를 호출하지 않는다. 이 API는 토스가 검증하지 않은 식별값을 클라이언트에서 받게 된다. 내부 `AppUserService`의 등록 동작은 토스 사용자 확인 직후 실행되는 조회 후 생성(find-or-create)으로 둔다.

## Postman으로 먼저 검증할 시나리오

1. 앱 진입 때마다 `POST /auth/toss/exchange`를 호출한다. 기존 사용자면 `200`, 신규면 `201`이어도 응답의 `accessToken`을 Collection Bearer Token으로 설정한다. 토스 연동 전에는 이 API를 실제로 호출할 수 없으므로, **개발 전용 인증 어댑터**가 같은 내부 사용자 토큰을 발급하도록 별도로 구현할 수 있다. 개발용 토큰 발급 경로를 운영에 노출하지 않는다.
2. 사용자 A로 공간을 만든다. 응답의 `household.id`를 `householdId` 변수로 저장한다.
3. A로 초대를 발급한다. 응답의 `token`을 `inviteToken` 변수로 저장한다.
4. 사용자 B의 토큰으로 초대 미리보기를 조회하고 수락한다. B의 `GET /me`에 공간이 나타나는지 확인한다.
5. A가 물품을 수량 2, 부족 기준 1로 만든다. `item.id`, `item.version`을 저장한다.
6. B가 `expectedVersion`을 보내 수량을 1로 바꾼다. 응답의 `lowStock=true`와 변경 이력을 확인한다.
7. 같은 이전 버전으로 다시 변경하면 `409 VERSION_CONFLICT`와 최신 물품 상태를 받는다.
8. A가 B를 내보낸 뒤 B의 물품 목록 조회가 `404`가 되는지 확인한다. B의 토큰이 살아 있어도 현재 멤버십이 권한의 기준이다.
9. B를 새 초대로 다시 가입시킨 뒤 `DELETE /households/{householdId}/membership`으로 스스로 나가게 한다. 이후 B의 물품 목록 조회가 `404`인지 확인한다.

## 오류와 경계 규칙

| 상황 | HTTP | `code` | 처리 |
| --- | --- | --- | --- |
| 필드 형식·범위 오류 | 400 | `VALIDATION_ERROR` | `details`에 필드 오류 제공 |
| 토큰 없음·만료·무효 | 401 | `UNAUTHENTICATED` | 인증 코드를 새로 받아 다시 교환 |
| 토스 인증 코드 무효·재사용 | 401 | `TOSS_AUTH_FAILED` | 새 인증 코드 요청 |
| 비활성 앱 사용자 | 403 | `USER_INACTIVE` | 요청 차단 |
| 소유자 전용 기능을 일반 멤버가 요청 | 403 | `OWNER_REQUIRED` | 권한 안내 |
| 공간·물품이 없거나 접근 불가 | 404 | `NOT_FOUND` | 존재 여부를 노출하지 않음 |
| 초대 없음 | 404 | `INVITATION_NOT_FOUND` | 토큰 값은 오류와 로그에 포함하지 않음 |
| 초대 만료·취소·사용 횟수 소진 | 410 | `INVITATION_UNAVAILABLE` | 새 초대 요청 |
| 예상 버전 불일치 | 409 | `VERSION_CONFLICT` | `details.currentItem`으로 새 화면 표시 |
| 소유자가 나가기·내보내기 대상이 됨 | 409 | `OWNER_TRANSFER_REQUIRED` | 소유권을 이전하거나 공간을 해산한 뒤 처리 |
| 다른 멤버가 있는 공간을 해산 | 409 | `HOUSEHOLD_NOT_EMPTY` | 멤버를 내보내거나 소유권을 이전 |

초대 수락은 **유효한 초대**에 대해 같은 사용자가 재시도하면 기존 멤버십을 `200`으로 반환하고 사용 횟수를 다시 차감하지 않는다. 새로 수락하면 `201`이다. 사용 횟수 증가와 멤버십 생성은 한 트랜잭션에서 처리한다. 수량 변경도 물품 갱신과 이력 기록을 한 트랜잭션에서 처리한다. 부족 이벤트는 `부족 아님 → 부족` 전이에만 생성한다. 기준값 수정으로 이 전이가 발생해도 같은 규칙을 적용한다.

일반 멤버는 스스로 공간에서 나갈 수 있고, 소유자는 소유권을 넘기기 전에는 나갈 수 없다. `DELETE /households/{householdId}/membership`은 인증된 사용자의 현재 멤버십만 제거한다.

물품을 처음 등록할 때 이미 부족 상태라면 목록에는 바로 `lowStock=true`로 표시하되, 최초 등록 자체로 외부 알림을 보내지는 않는다. `newQuantity`가 현재 수량과 같으면 변경 이력을 만들지 않고 `400 VALIDATION_ERROR`로 돌려준다.

## API 목록에 포함되지 않는 MVP 동작

이 명세의 18개 요청은 미니앱/Postman에서 서버로 호출하는 HTTP API다. 제품 흐름을 완성하려면 다음 서버 내부 동작도 구현해야 하며, 사용자가 직접 호출하는 별도 endpoint는 필요 없다.

- 수량 변경이나 부족 기준 변경에서 `부족 아님 → 부족` 전이가 생기면, 같은 DB 트랜잭션에서 `LowStockEvent`를 기록하거나 발송 outbox에 넣는다.
- 비동기 발송기가 해당 공간 멤버 중 서비스 알림 신청과 앱인토스 발송 동의가 확인된 사람만 골라 승인된 기능성 메시지를 보낸다. `(eventId, userId)` 기준 발송 기록으로 재시도 중복을 막는다.
- 발송 실패·재시도·성공을 `AlertDelivery`로 기록한다. 외부 발송은 문구 승인 전에는 활성화하지 않는다.
- 미니앱은 `User.createAnonymousKeyAuthCode()`와 공유 링크 기능을 사용한다. 인증 코드 교환은 첫 API 안에서 서버가 수행한다.

따라서 목록은 **클라이언트가 호출할 MVP HTTP API 초안**으로는 거의 완결됐다. 실제 기능 완성 기준에는 위 알림 처리기와 토스 연동이 포함된다. 물품 삭제는 현재 MVP 기획의 필수 목록(등록·조회·수정)에 없어 제외했으며, 잘못 등록한 물품을 사용자가 직접 지우는 UX까지 MVP에 넣으려면 삭제 API와 이력 보존 규칙을 추가해야 한다.

## 아직 결정할 제품·구현 선택

- API 토큰의 만료 시간과 웹뷰 저장 방식. 초안에는 `expiresInSeconds=3600` 예시만 둔다.
- 초대 링크를 한 사람당 하나씩 발급할지, `maxUses=3` 링크 하나를 공유할지. API는 양쪽을 허용한다.
- 물품 이름의 중복 허용 여부. 현재 초안은 허용하며 물품 ID로 구별한다.
- 알림 동의 확인 방식과 발송 문구 승인. `enabled=true`는 앱 내부 신청일 뿐 실제 발송 승인이나 토스 동의를 대신하지 않는다.
- 토스쇼핑 목적지·링크 규격과 심사 결과. 확인 전까지 구매 이동 API는 포함하지 않는다.

## 출처

- 프로젝트 문서: [제품 기획](./01-product-plan.md), [사용자 전달·인증 흐름](./02-user-handoff.md)
- 사용자 식별키 인증 코드 발급과 서버 교환: https://developers-apps-in-toss.toss.im/guide/authentication/anonymous-key-auth-code
