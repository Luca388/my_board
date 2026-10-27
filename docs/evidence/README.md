# FixLog 테스트 결과와 증거

`docs/etc/05_테스트시나리오.md`의 T01~T17을 순서대로 수행한 결과입니다. 아래 표는 보고서 4절에 그대로 옮길 수 있는 형식입니다.

## 실행 환경

| 항목 | 내용 |
|---|---|
| 일시 | 2026-10-27 22:22 ~ 22:30 (KST) |
| 코드 기준 | 커밋 `6248132` (BE-01~03, FE-01~04 구현 후 폴더 이름 변경) |
| OS / 런타임 | macOS 26.6.2 / JDK 21.0.1 / Node.js 24.3.0 |
| 백엔드 | `backend/`에서 `source .env` 후 `java -jar target/jwt-board-1.0.0.jar` → http://127.0.0.1:8000 |
| 프론트 | `frontend/`에서 `npm run build` → `npm run preview` → http://127.0.0.1:4173 |
| DB | `backend/data/board.sqlite` (실제 개발 DB, 테스트 글은 끝난 뒤 삭제) |
| 토큰 TTL | 일반 90초 / 만료 테스트(T10) 3초 |
| 수행 방법 | API는 curl, 화면은 4173 빌드 화면에서 확인 |
| 수행 주체 | Claude Code(Claude Opus 5.5)가 사용자 PC에서 사용자 요청으로 실행. 결과 확인과 보고서 기록은 사용자 |

토큰 원문은 모든 기록에서 `<ALICE_TOKEN>`, `<BOB_TOKEN>`, `<TAMPERED_TOKEN>`, `<EXPIRED_ALICE_TOKEN>`처럼 가리거나 앞부분만 남겼습니다.

## T01~T17 결과

| ID | 수행 절차와 사용자 | 예상 결과 | 실제 HTTP와 code | 판정 | 증거 |
|---|---|---|---|---|---|
| T01 | 비로그인으로 `GET /api/posts`, `GET /api/posts/{id}` | 200, 공개 조회 | 200 / 200 | PASS | [api/T01.txt](api/T01.txt), [ui/T01](ui/T01_logged_out_list_detail.jpg) |
| T02 | Alice 로그인 `POST /api/auth/login` | 200, JWT와 공개 사용자 정보, 화면 로그인 표시 | 200 / 해당 없음, `user`에 id·name·email만 있음, 화면 "Alice님 로그인 중" | PASS | [api/T02.txt](api/T02.txt), [ui/T02](ui/T02_alice_login.jpg) |
| T03 | 틀린 비밀번호, 없는 이메일(`nobody@class.test`)로 각각 로그인 | 401 INVALID_CREDENTIALS, 토큰 미발급 | 401 INVALID_CREDENTIALS ×2, 응답에 토큰 없음, 화면 오류 안내 | PASS | [api/T03.txt](api/T03.txt), [ui/T03](ui/T03_wrong_password.jpg) |
| T04 | 토큰 없이 `GET /api/me`, `POST /api/posts` | 각각 401 TOKEN_REQUIRED | 401 TOKEN_REQUIRED ×2, `WWW-Authenticate: Bearer` | PASS | [api/T04.txt](api/T04.txt) |
| T05 | Alice 토큰으로 글 등록 후 `GET /api/me` | 201, authorId 1, /me는 Alice | 201 + `Location: /api/posts/6`, authorId 1 / 200, name Alice | PASS | [api/T05.txt](api/T05.txt) |
| T06 | Alice가 본인 글 수정 → 삭제 | 200, 204, DB 반영 | 200(재조회 시 수정본) / 204 빈 본문(재조회 404 NOT_FOUND) | PASS | [api/T06.txt](api/T06.txt) |
| T07 | Bob 토큰으로 Alice 글(id 7) 수정 | 403 FORBIDDEN, 원문 유지 | 403 FORBIDDEN, 재조회 원문 그대로, 화면에서 Bob에게 수정·삭제 버튼 없음 | PASS | [api/T07.txt](api/T07.txt), [ui/T07](ui/T07_bob_no_edit_buttons.jpg) |
| T08 | Bob 토큰으로 Alice 글(id 7) 삭제 | 403 FORBIDDEN, 글 유지 | 403 FORBIDDEN, 재조회 200 | PASS | [api/T08.txt](api/T08.txt) |
| T09 | 서명 첫 글자를 바꾼 토큰으로 `/me`, POST, PUT, DELETE | 401 INVALID_TOKEN, 변경 없음 | 401 INVALID_TOKEN ×4, 원문 유지, 글 개수 4 → 4 | PASS | [api/T09.txt](api/T09.txt) |
| T10 | TTL 3초 토큰을 5초 뒤 `/me`, POST, PUT, DELETE에 사용 (Bob 만료 토큰으로 Alice 글 수정 포함) | 401 TOKEN_EXPIRED, 화면 로그인 해제와 안내 | 401 TOKEN_EXPIRED ×5, 원문 유지, 화면 "로그아웃 상태"와 만료 안내, 입력값 유지 | PASS | [api/T10.txt](api/T10.txt), [ui/T10](ui/T10_expired_logged_out.jpg), [ui/T10 로그](ui/T10_request_log.txt) |
| T11 | Bob 로그인 → 로그아웃 → 내 정보 조회 | 토큰 헤더 없음, 401 TOKEN_REQUIRED | 로그인 중 `/me`는 Bearer 헤더 → 200, 로그아웃 후 헤더 없음 → 401 TOKEN_REQUIRED | PASS | [api/T11.txt](api/T11.txt), [ui/T11](ui/T11_logout_then_me.jpg), [헤더 기록](ui/T11_request_headers.txt) |
| T12 | Bob 등록 요청에 `authorId:1`, `userId:1`을 각각 추가 | 400 BAD_INPUT, 위조 글 미생성 | 400 BAD_INPUT ×2, 글 개수 4 → 4 | PASS | [api/T12.txt](api/T12.txt) |
| T13 | Alice가 본인 글에 빈 제목, 공백 본문, 제목 101자, 본문 5001자 | 400 BAD_INPUT, DB 변경 없음 | 400 BAD_INPUT ×4, 원문 유지, 화면은 오류 안내 후 입력값 유지 | PASS | [api/T13.txt](api/T13.txt), [ui/T13](ui/T13_blank_title_400_input_kept.jpg) |
| T14 | 없는 글 99999 조회·수정·삭제(Alice 토큰), `GET /api/posts/abc` | 404, 잘못된 ID는 400 | 404 NOT_FOUND ×3 / 400 BAD_INPUT | PASS | [api/T14.txt](api/T14.txt) |
| T15 | 글 작성 후 서버 프로세스 종료 → 재실행 | 글·작성자·내용 유지 | 재시작 전후 응답 본문(제목·내용·작성자·작성 시각) 완전히 동일 | PASS | [api/T15.txt](api/T15.txt) |
| T16 | Alice 로그인 후 새로고침 | 글 유지, 로그인 해제 | 글 5 → 5, "Alice님 로그인 중" → "로그아웃 상태", localStorage·sessionStorage 0개 | PASS | [api/T16.txt](api/T16.txt), [ui/T16](ui/T16_reload_logged_out_posts_kept.jpg) |
| T17 | 토큰 없이 수정·삭제, Bob 본인 글 작성·수정·삭제 | 무토큰 401, Bob 본인 작업 성공 | 401 TOKEN_REQUIRED ×2 / 201(authorId 2) → 200 → 204 → 재조회 404 | PASS | [api/T17.txt](api/T17.txt) |

API 검사 68건(재조회와 데이터 확인 포함) 모두 PASS. 전체 목록은 [api/summary.tsv](api/summary.tsv)에 있습니다.

## HTTP 응답 확인 기록

| 확인 항목 | 기대 응답 | 실제 HTTP와 code | 증거 위치 |
|---|---|---|---|
| 정상 조회·로그인·수정 | 200 | 200 | T01, T02, T06, T17 |
| 새 글 등록 | 201 및 Location | 201, `Location: /api/posts/{id}` | T05, T17 |
| 본인 글 삭제 | 204 및 빈 본문 | 204, 본문 없음 | T06, T17 |
| 입력 오류·추가 작성자 필드 | 400 BAD_INPUT | 400 BAD_INPUT | T12, T13, T14 |
| 잘못된 계정 | 401 INVALID_CREDENTIALS | 401 INVALID_CREDENTIALS | T03 |
| 토큰 없음·헤더 형식 오류 | 401 TOKEN_REQUIRED | 401 TOKEN_REQUIRED | T04, T11, T17 |
| 변조·클레임 오류 | 401 INVALID_TOKEN | 401 INVALID_TOKEN | T09 |
| 정상 서명의 만료 토큰 | 401 TOKEN_EXPIRED | 401 TOKEN_EXPIRED | T10 |
| 다른 작성자의 변경 | 403 FORBIDDEN | 403 FORBIDDEN | T07, T08 |
| 없는 글 | 404 NOT_FOUND | 404 NOT_FOUND | T06, T14 |

- **WWW-Authenticate:** 보호 API의 401 응답(T04, T09, T10, T11, T17)에 모두 `WWW-Authenticate: Bearer`가 있습니다.
- **401과 403의 화면 차이:** 401(T10, T11)이면 "로그아웃 상태"로 바뀌고 다시 로그인하라는 안내가 나옵니다. 403은 화면에서 남의 글에 수정·삭제 버튼이 보이지 않으므로(T07) 직접 API 요청으로 확인했고, 프론트는 403을 받아도 로그인 상태를 지우지 않습니다(`frontend/src/api.js`는 401일 때만 `clearSession` 호출).
- **실패 후 데이터 불변:** T07, T08, T09, T10, T13에서 실패 응답 뒤 공개 재조회를 했고 원문 "이 글은 바뀌면 안 됩니다"가 그대로였습니다. T09, T12에서는 글 개수가 그대로였습니다.
- **처리 순서** ([api/EXTRA.txt](api/EXTRA.txt)): 토큰 없이 없는 글 수정은 401 TOKEN_REQUIRED, 정상 토큰으로 같은 글 수정은 404 NOT_FOUND. Bob이 Alice 글에 공백 제목을 보내면 403 FORBIDDEN, Alice가 자기 글에 보내면 400 BAD_INPUT. Bob의 만료 토큰으로 Alice 글 수정은 401 TOKEN_EXPIRED(T10).

## 자동 검사

| 명령 | 서버 TTL | 결과 | 기록 |
|---|---|---|---|
| `python3 tools/check_api.py --mode final` | 90 | All 40 checks passed | [api/check_api_final.txt](api/check_api_final.txt) |
| `python3 tools/check_api.py --mode final --expiry` | 3 | All 48 checks passed | [api/check_api_final_expiry.txt](api/check_api_final_expiry.txt) |

자동 검사는 API 일부만 확인하며, 위 T01~T17 수동 시나리오와 화면 검사를 대신하지 않습니다.

## 참고와 한계

- 화면 캡처는 전체 화면이 한 장에 들어오도록 브라우저 확대 비율을 60%로 줄여서 찍었습니다.
- 브라우저 Network 탭 화면 대신, 프론트가 실제로 보낸 `Authorization` 헤더를 fetch 호출을 감싸 기록했습니다(앱 코드는 바꾸지 않음): [ui/T11_request_headers.txt](ui/T11_request_headers.txt), [ui/T10_request_log.txt](ui/T10_request_log.txt). 보고서에 Network 탭 캡처가 필요하면 직접 추가해야 합니다.
- 테스트는 같은 시나리오를 두 번 실행했고(첫 실행 로그의 토큰 표기 방식 수정), 표의 글 id는 두 번째 실행 기준입니다.
- 선택 점검(잘못된 알고리즘, iss·aud 불일치, exp·sub 누락)은 이 기록에 포함하지 않았습니다.
