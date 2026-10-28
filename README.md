# FixLog — 개발 오류 해결 기록

웹서비스설계 JWT 게시판 과제입니다. 개발하다 만난 오류와 해결 방법을 기록하고 공유하는 게시판으로, 누구나 글을 읽을 수 있고 로그인한 사용자만 글을 쓰며 본인 글만 수정·삭제할 수 있습니다.

## 작성자와 서비스 소개

- 학번 / 이름: [작성]
- 서비스 목적과 대상 사용자: 개발을 공부하는 학생과 개발자가 직접 겪은 오류의 증상, 원인, 해결 방법을 기록하고 서로 공유하는 서비스입니다. 다른 사람의 기록은 로그인 없이 읽을 수 있지만, 기록을 남기려면 로그인해야 합니다. 기록의 작성자를 신뢰할 수 있도록 작성자는 서버가 JWT로 확인하며, 본인이 쓴 기록만 수정·삭제할 수 있습니다.
- 선택한 기술: React + Spring Boot
- 주요 기능: 공개 목록·상세 조회, 로그인과 JWT 발급, 내 정보 조회, 로그인 사용자의 글 작성, 본인 글 수정·삭제, 토큰 만료·로그아웃 처리, 응답 코드별 오류 안내

## 보고서와 테스트 증거

- [구현 보고서 PDF](docs/report.pdf)
- [테스트 증거](docs/evidence/)

## 프로젝트 구조

```text
.
├── README.md
├── .gitignore
├── AGENTS.md, CLAUDE.md   # AI 도구용 작업 메모
├── frontend/              # React (Vite) 프론트
│   └── src/api.js, src/main.jsx
├── backend/               # Spring Boot 백엔드
│   ├── env.example.txt    # 환경설정 예시
│   └── src/main/java/edu/classroom/board/AuthService.java 등
├── tools/
│   ├── check_api.py       # 과제 제공 API 자동 검사
│   └── postman/           # T01~T17 Postman 컬렉션
└── docs/
    ├── report.pdf
    ├── evidence/          # 테스트 캡처
    └── etc/               # 과제 제공 문서
```

과제 템플릿의 `frontend-react`, `backend-spring` 폴더를 각각 `frontend`, `backend`로 이름만 바꿨습니다.

## 실행 환경

| 항목 | 사용 버전 |
|---|---|
| 운영체제 | macOS 26.6.2 |
| 프론트 런타임 Node.js | 24.3.0 (요구: 20.19 이상) |
| 백엔드 런타임 | JDK 21.0.1 (요구: 17 이상), Maven Wrapper 포함 |
| 프레임워크 | React 19.3.0, Vite 8.3.4 / Spring Boot 3.5.16, JJWT 0.13.0 |
| 데이터 저장 | SQLite (sqlite-jdbc 3.51.3.0) |

## 설치와 실행

백엔드와 프론트를 각각 다른 터미널에서 실행합니다.

### 1 저장소 복제

```sh
git clone https://github.com/Luca388/my_board.git
cd my_board
```

### 2 백엔드 준비와 실행

`backend` 폴더에서 JWT 키를 만들고 서버를 실행합니다. 키는 32바이트 난수를 표현한 64자리 16진수이며, 각자 새로 만들어 씁니다. 실제 키는 저장소에 올리지 않습니다(`.env`는 `.gitignore` 대상).

macOS / Linux:

```sh
cd backend
printf 'export JWT_SECRET=%s\nexport TOKEN_TTL=90\n' "$(node -e "console.log(require('node:crypto').randomBytes(32).toString('hex'))")" > .env
source .env && ./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
cd backend
$env:JWT_SECRET = node -e "console.log(require('node:crypto').randomBytes(32).toString('hex'))"
$env:TOKEN_TTL = "90"
.\mvnw.cmd spring-boot:run
```

- 환경설정 예시 파일: `backend/env.example.txt`
- JWT_SECRET 설정 방법: 위 명령으로 새 키를 만들어 환경변수로 주입합니다. Spring은 `.env`를 자동으로 읽지 않으므로 macOS/Linux에서는 서버를 실행하는 터미널에서 `source .env`를 먼저 실행해야 합니다. 새 터미널을 열면 다시 `source .env`가 필요합니다.
- 키가 없거나, 과제 예시 키이거나, 64자리 16진수가 아니면 서버가 시작되지 않습니다(의도된 동작).
- TOKEN_TTL: 기본 90초입니다. 만료 테스트는 `source .env && TOKEN_TTL=3 ./mvnw spring-boot:run`으로 실행합니다.
- 백엔드 주소: http://127.0.0.1:8000
- 정상 실행 확인 방법: `curl http://127.0.0.1:8000/api/health` → `{"status":"ok","stack":"spring"}`

### 3 프론트 빌드와 로컬 실행

백엔드를 켜 둔 상태에서 새 터미널을 열고, 저장소 루트에서 실행합니다.

```sh
cd frontend
npm ci
npm run build
npm run preview
```

- 최종 확인 주소: http://127.0.0.1:4173
- 개발 중 실행: `npm run dev`, http://127.0.0.1:5173
- API 연결 방식과 포트 변경 방법: 브라우저는 `/api`로 요청하고, Vite가 이를 http://127.0.0.1:8000 으로 프록시합니다. 백엔드 포트를 바꾸면 `frontend/vite.config.js`의 `server.proxy`와 `preview.proxy`를 함께 바꿉니다.
- 종료: 프론트와 백엔드를 실행한 각 터미널에서 Ctrl+C
- 코드 변경 후 재실행: 프론트는 `npm run build` 후 `npm run preview`를 다시 실행하고, 백엔드는 Ctrl+C 후 `source .env && ./mvnw spring-boot:run`으로 다시 실행합니다.

### 4 테스트 계정과 DB

| 사용자 | 이메일 | 실습용 비밀번호 |
|---|---|---|
| Alice | alice@class.test | Classroom!2026 |
| Bob | bob@class.test | Classroom!2026 |

- SQLite 파일 위치와 자동 생성 방식: `backend/data/board.sqlite`. 서버를 시작하면 폴더, 파일, `posts` 테이블이 자동으로 만들어집니다. 테스트 계정은 서버 시작 시 메모리에 준비되며 DB에 저장하지 않습니다.
- DB 초기화 방법: 서버를 종료한 뒤 `backend/data/board.sqlite`를 삭제하고 다시 실행합니다.
- 서버 재시작 후 데이터 유지 확인 결과: 글을 작성한 뒤 서버를 종료·재실행해도 같은 글, 작성자, 작성 시각이 그대로 조회되었습니다(T15).

위 계정은 과제 전용 테스트 계정입니다. 실제 서비스 계정 정보나 서버 비밀 키는 기록하지 않습니다.

## 구현 완료 범위

| 기능 | 완료 여부 | 주요 파일 또는 함수 |
|---|---|---|
| 공개 목록과 상세 | 완료 | `BoardController.list()`, `get()` (제공 코드) |
| 로그인과 JWT 발급 | 완료 | `AuthService` 생성자, `issueToken()` / `api.js` `login()` |
| JWT 검증과 내 정보 조회 | 완료 | `AuthService.currentUserId()`, `BoardController.me()` |
| 글 작성과 작성자 저장 | 완료 | `BoardController.create()` (작성자는 토큰의 사용자 id) |
| 본인 글 수정·삭제 | 완료 | `AuthService.assertOwner()` / `api.js` `canEdit()` |
| 토큰 만료와 로그아웃 | 완료 | `api.js` `request()`, `clearSession()` |
| 입력 오류와 권한 오류 처리 | 완료 | `BoardController.fields()` (제공 코드), `api.js` 오류 안내, `main.jsx` |

알려진 문제와 미완성 기능:
- 이미 로그인한 상태에서 다른 계정 로그인에 실패하면 기존 로그인도 해제됩니다.
- 남의 글에는 수정·삭제 버튼이 표시되지 않아 화면에서 403이 발생하지 않습니다. 403 처리는 API 직접 요청으로 확인했습니다.
- `Authorization: Bearer  <토큰>`(공백 두 칸)처럼 Bearer 뒤 형식이 어긋난 헤더는 401 TOKEN_REQUIRED가 아니라 401 INVALID_TOKEN으로 응답합니다.
- `Content-Type` 헤더 없이 요청하면 Spring 기본 동작으로 415가 반환됩니다(프론트는 항상 JSON 헤더를 보냄).

## 테스트 방법과 결과 요약

- 실행한 검사 명령과 실행 위치: Postman 컬렉션 `tools/postman/FixLog_T01-T17.postman_collection.json`으로 API를 시험하고(각 요청에 상태 코드·JSON code 자동 판정 포함), 화면 동작은 `npm run preview`로 띄운 http://127.0.0.1:4173 에서 확인했습니다.
- Alice/Bob 권한 구분 확인 결과: Bob의 토큰으로 Alice 글을 수정·삭제하면 403 FORBIDDEN이며 재조회 시 원문이 그대로였습니다. Bob은 본인 글을 작성 201, 수정 200, 삭제 204로 처리할 수 있었습니다.
- HTTP 상태와 오류 code 확인: 200·201·204와 400 BAD_INPUT, 401 INVALID_CREDENTIALS·TOKEN_REQUIRED·INVALID_TOKEN·TOKEN_EXPIRED, 403 FORBIDDEN, 404 NOT_FOUND가 명세대로 반환되었습니다(보고서 4절).
- 응답 헤더와 본문 확인: 등록 201에 `Location: /api/posts/{id}`, 보호 API 401에 `WWW-Authenticate: Bearer`, 삭제 204는 본문이 비어 있었습니다.
- 토큰 없음·변조·만료 확인 결과: 토큰 없음 401 TOKEN_REQUIRED, 서명을 바꾼 토큰 401 INVALID_TOKEN, TTL 3초로 발급 후 만료된 토큰 401 TOKEN_EXPIRED였고, 만료 시 화면은 로그아웃 상태로 바뀌었습니다.
- 전체 T01~T17 결과와 증거: [보고서 4절](docs/report.pdf), [테스트 캡처](docs/evidence/)

## 핵심 구현 설명

1. **로그인과 발급:** 서버 시작 시 `AuthService` 생성자가 `JWT_SECRET`을 검사하고 32바이트 서명 키를 만듭니다. 로그인 요청은 비밀번호 확인 후 `issueToken()`이 HS256 토큰(sub, iss, aud, exp)을 발급합니다.
2. **토큰 보관과 전송:** 프론트 `api.js`의 `login()`이 토큰과 사용자 정보를 메모리 변수에만 저장합니다(localStorage 미사용, 새로고침하면 로그아웃). `authorizationHeaders()`가 토큰이 있을 때만 `Authorization: Bearer` 헤더를 붙입니다.
3. **서버 검증:** `currentUserId()`가 헤더 형식, 서명·발급자·사용 대상·만료, 알고리즘(HS256), exp 존재, sub와 실제 계정을 확인한 뒤 검증된 사용자 id만 넘깁니다.
4. **작성자 권한:** 수정·삭제는 인증 → 글 존재 확인 → `assertOwner()` 작성자 비교 → 입력 검증 순서로 처리하며, 작성자가 다르면 403입니다. 프론트 `canEdit()`은 버튼 표시만 담당합니다.

상세 코드 설명은 보고서 3절에 있습니다.

## AI 활용 요약과 참고 자료

- AI 사용 여부: 사용
- 도구와 사용 목적: Claude Code (Claude Opus 5.5). JWT 인증·프론트 연결 코드 생성, 서버 시작 오류 디버깅, Postman 테스트 컬렉션 생성, 과제 요구사항 정리와 보고서·README 초안 작성
- 반영 위치와 직접 수정·검증한 내용: `AuthService.java`, `api.js`, `main.jsx` 등은 AI가 작성한 코드를 그대로 반영했고, 동작을 질문해 이해한 뒤 T01~T17을 직접 수행해 검증했습니다.
- 상세 활용 기록: 보고서 5절
- 참고한 수업 자료·외부 코드·공식 문서: 과제 제공 문서(`docs/etc/`의 과제 공지, API 명세, 구현 체크리스트, 테스트 시나리오, 로컬 실행 안내), 과제 시작 템플릿

## 제출 확인

- [ ] README의 안내 문구를 실제 내용으로 교체했다.
- [ ] 새 폴더에 clone하여 README만으로 설치·환경설정·빌드·로컬 실행했다.
- [ ] 4173번 빌드 화면에서 API 연결과 전체 필수 기능을 확인했다.
- [ ] GitHub에서 보고서와 증거 링크가 열린다.
- [ ] 교수자가 저장소를 열람·복제할 수 있다.
- [ ] 비밀 키·실제 토큰·DB·node_modules·.venv·target을 커밋하지 않았다.
- [ ] 최종 push 후 저장소 URL과 제출 기준 커밋 SHA를 LMS에 제출했다.
