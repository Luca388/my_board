# AGENTS.md — FixLog (웹서비스설계 JWT 게시판 과제)

AI 에이전트(Claude Code 등)가 이 저장소에서 작업을 이어갈 때 먼저 읽는 문서입니다.

## 작업 규칙 (가장 중요)

- **채점 대상 과제이며 학생이 직접 구현합니다.** 사용자가 명시적으로 요청하지 않으면 TODO 코드(BE-01~03, FE-01~04)를 대신 작성하지 마세요. 개념 설명, 명세 위치 안내, 사용자가 쓴 코드 리뷰·디버깅으로 돕습니다.
- 보고서에 "본인 작성 / AI 도움"을 구분해 적어야 하므로, AI가 코드를 제안하거나 수정했다면 어느 파일·함수인지 사용자에게 분명히 알려 주세요.
- 실제 `JWT_SECRET`, `.env`, `data/board.sqlite`, 토큰 원문을 커밋하거나 문서·캡처에 넣지 마세요.
- 저장소는 **공개(public)** 입니다: https://github.com/Luca388/my_board
- 사용자와는 한국어로 대화합니다.

## 프로젝트 개요

- 서비스: **FixLog** — 개발 중 겪은 오류와 해결 방법을 기록·공유하는 게시판
- 조합: `frontend-react` (Vite + React) + `backend-spring` (Spring Boot 3.5, JDK 17+, JJWT 0.13) + SQLite 파일 DB
- 요구사항: 누구나 글 읽기, 로그인한 사용자만 작성, 본인 글만 수정·삭제. JWT(HS256) 발급·검증.
- 테스트 계정: `alice@class.test`(id 1) / `bob@class.test`(id 2), 비밀번호 `Classroom!2026`

## 폴더 구조

```
backend-spring/src/main/java/edu/classroom/board/
  AuthService.java     ← 학생 구현: BE-01 issueToken, BE-02 currentUserId, BE-03 assertOwner
  BoardController.java ← 라우트(제공). 인증 → 글 존재 → 권한 → 입력검증 순서로 이미 호출함
  Errors.java          ← ApiError → {code,message} JSON, 401이면 WWW-Authenticate: Bearer (제공)
  Users.java           ← 테스트 계정, PBKDF2 비밀번호 검증 (제공)
  PostStore.java, DatabaseConfig.java ← SQLite (제공)
backend-spring/src/main/resources/application.properties ← app.jwt.* 가 환경변수와 연결됨
frontend-react/src/
  api.js   ← 학생 구현: FE-01 login, FE-02 authorizationHeaders, FE-03 401 처리, FE-04 canEdit
  main.jsx ← 화면. 데모 배너 교체, 비로그인 작성 안내 등
tools/check_api.py ← 자동 API 검사
docs/etc/          ← 교수자 배포 과제 문서 (공지·API 명세·체크리스트·보고서 양식·테스트 시나리오·README 양식)
docs/evidence/     ← 테스트 증거(캡처 등) 넣을 곳
docs/report.pdf    ← 최종 보고서 (아직 없음)
```

꼭 참고할 문서: `docs/etc/02_API명세.md`(HTTP 상태·code 계약), `docs/etc/03_구현체크리스트.md`, `docs/etc/05_테스트시나리오.md`(T01~T17), `docs/etc/04_보고서양식.md`.

## 실행 방법

새 Mac에서 처음 할 일:

```bash
# 1) 키 생성 후 backend-spring/.env 에 저장 (.env는 gitignore 대상, 커밋 금지)
openssl rand -hex 32
# backend-spring/.env 내용:
#   export JWT_SECRET=<생성한 64자리 hex>
#   export TOKEN_TTL=90

# 2) 백엔드 (backend-spring 폴더에서) → http://127.0.0.1:8000
source .env && ./mvnw spring-boot:run

# 3) 프론트 개발 서버 (frontend-react 폴더에서, 다른 터미널) → http://127.0.0.1:5173
npm ci && npm run dev

# 4) 최종 확인용 빌드 (frontend-react) → http://127.0.0.1:4173
npm run build && npm run preview

# 5) API 자동 검사 (저장소 루트에서)
python3 tools/check_api.py --mode starter   # 시작 코드 확인
python3 tools/check_api.py --mode final     # 구현 후
python3 tools/check_api.py --mode final --expiry   # TOKEN_TTL=3 으로 서버 재시작 후
```

DB 초기화: 서버 종료 후 `backend-spring/data/board.sqlite` 삭제 → 재실행 시 자동 생성.

## 현재 진행 상황 (2026-10-27 기준)

- [x] 조합 선택(React + Spring), 주제 FixLog 확정
- [x] 시작 코드 상태 확인: `check_api.py --mode starter` 14개 통과, 로그인은 의도대로 501
- [x] `.gitignore` 정리, 과제 문서를 `docs/etc/`로 이동
- [x] 이전 세션에서 AI가 전체 구현을 시도했으나 **사용자 요청으로 시작 코드로 원복함**. 현재 TODO는 모두 미구현 상태.
- [ ] `JWT_SECRET` 생성 및 `.env` 설정 (새 Mac에서 다시 해야 함)
- [ ] BE-01~03 구현 → FE-01~04 구현 → 테스트·증거 → README·보고서 → 제출

## 구현 시 핵심 계약 (요약, 상세는 02_API명세.md)

- JWT: HS256 고정, `sub`(사용자 id 문자열), `exp`, `iss=webservice-class`, `aud=board-api`, TTL 기본 90초
- 서버 시작 시 빈 키·예시 키(`REPLACE_WITH_RANDOM_64_HEX_CHARACTERS`)·64자리 hex가 아닌 키는 거부
- 401 `TOKEN_REQUIRED`(토큰 없음/Bearer 형식 오류), 401 `INVALID_TOKEN`(변조·클레임·없는 사용자), 401 `TOKEN_EXPIRED`(정상 서명 만료), 403 `FORBIDDEN`(타인 글), 404 `NOT_FOUND`, 400 `BAD_INPUT`
- 참고: JJWT는 `exp`가 없으면 만료 검사를 건너뛰므로 `exp` 존재를 따로 확인해야 함
- 프론트: 토큰은 메모리에만(localStorage 금지), 401이면 로그인 상태 해제, 403이면 유지, 204는 JSON 파싱 금지

## 제출물

- GitHub: 소스, 루트 `README.md`(`docs/etc/06_제출용_README.md` 기반), `docs/report.pdf`, `docs/evidence/`
- LMS: 저장소 URL + 제출 기준 커밋 SHA
- 교수자 GitHub 계정은 과제 문서에 없음(수업/LMS에서 안내). 현재 공개 저장소라 계정 없이도 열람 가능.
- 보고서 AI 활용 절: Claude Code(Claude Opus 5.5)로 과제 요구사항 정리·주제 선정·저장소 정리를 도움받음. 이후 AI 도움은 대표 사례로 기록.
