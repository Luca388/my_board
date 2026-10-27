# 웹서비스설계 JWT 게시판 과제

React 또는 Vue 중 하나와 Express 또는 FastAPI 또는 Spring Boot 중 하나를 선택해 JWT 인증 게시판을 완성합니다. 프론트 2종 × 백엔드 3종의 총 6가지 조합을 지원합니다. 모든 조합은 동일한 API 규격과 평가 기준을 사용합니다. 한 가지 조합만 제출합니다.

완성한 서비스는 본인 PC에서 프론트 빌드 결과와 백엔드를 함께 실행해야 합니다. GitHub는 코드·README·보고서 제출용입니다. 교수자가 저장소를 clone한 뒤 같은 절차로 로컬에서 재현할 수 있어야 합니다. 외부 서버·도메인·클라우드 배포는 요구하지 않습니다.

## 먼저 읽을 문서

1. `docs/01_과제공지.pdf`: 배포용 공지문. 같은 이름의 Markdown·Word 원본도 제공하며 요구사항은 동일합니다.
2. `docs/02_API명세.md`: 모든 조합의 공통 계약, HTTP 상태·JSON code·응답 헤더
3. `docs/03_구현체크리스트.md`: 준비부터 GitHub 제출까지 10단계의 상세 구현·검증 체크리스트
4. `docs/04_보고서양식.docx`: 작성 가능한 보고서 양식, Markdown 원본도 제공
5. `docs/05_테스트시나리오.md`: 수동 검증 및 보고서 증거
6. `docs/06_제출용_README.md`: 학생 저장소의 루트 README 작성 양식
7. `docs/07_로컬실행안내.md`: 운영체제별 설치와 로컬 배포·실행·종료 절차

## 템플릿 선택

| 프론트 | 백엔드 | 데이터 저장 |
|---|---|---|
| frontend-react | backend-express | SQLite 파일, sql.js |
| frontend-react | backend-fastapi | SQLite 파일, Python 표준 라이브러리 |
| frontend-react | backend-spring | SQLite 파일, Spring JDBC |
| frontend-vue | backend-express | SQLite 파일, sql.js |
| frontend-vue | backend-fastapi | SQLite 파일, Python 표준 라이브러리 |
| frontend-vue | backend-spring | SQLite 파일, Spring JDBC |

수업 코드를 이어 쓰고 싶다면 Vue + Express를 선택할 수 있습니다. 다른 조합도 동일하게 평가합니다. 선택한 폴더 2개를 복사해서 작업하거나 전체 압축을 풀고 해당 폴더에서 실행하세요. 폴더 이름을 바꾸어도 되며 문서의 경로를 함께 고치세요.

## 빠른 시작

선택한 백엔드 README대로 먼저 실행합니다. 모든 백엔드는 `http://127.0.0.1:8000`을 사용하므로 한 번에 하나만 실행합니다.

다른 터미널에서 선택한 프론트 폴더로 이동해 실행합니다.

```sh
npm ci
npm run dev
```

브라우저에서 `http://127.0.0.1:5173`을 엽니다. 프론트의 `/api` 요청은 Vite 개발 프록시가 8000번 백엔드로 전달합니다. 개발 실습에서는 별도 CORS 설정이 필요하지 않습니다. 포트 변경 시 `vite.config.js`의 프록시 목적지도 함께 수정하세요.

모든 조합에서 **SQLite 파일 DB를 필수로 사용**합니다. 기본 파일은 선택한 백엔드 폴더의 `data/board.sqlite`이며 자동 생성됩니다.

처음에는 게시글이 없습니다. 글 등록 → 목록 → 상세 → 수정 → 삭제를 먼저 확인하세요. 두 테스트 계정은 서버 시작 시 준비되며 게시글은 DB 파일에 유지됩니다.

- Alice: `alice@class.test` / `Classroom!2026`
- Bob: `bob@class.test` / `Classroom!2026`

## 로컬 배포와 최종 실행

백엔드를 켜 둔 상태에서 프론트 개발 서버를 Ctrl+C로 종료하고, 선택한 프론트 폴더에서 실행합니다.

```sh
npm run build
npm run preview
```

http://127.0.0.1:4173 에서 빌드된 서비스를 확인합니다. `/api`는 8000번 백엔드로 프록시됩니다. 개발 중에는 `npm run dev`와 5173번 주소를 사용하고, 제출 전에는 빌드 결과와 4173번 주소로 전체 기능을 검증하세요. 의존성 설치에는 인터넷이 필요하지만, 제공 서비스 실행에는 클라우드 API나 별도 DB 서버가 필요하지 않습니다.

## 시작 코드와 제출 완성본의 차이

시작 코드의 로그인 기능은 미완성이므로 정상 계정으로 API에 로그인해도 501 NOT_IMPLEMENTED가 반환됩니다. 프론트 로그인 버튼도 구현 안내 메시지를 표시합니다. 이는 의도된 상태입니다.

시작 코드에서는 `currentUserId`가 데모 ID 1을 반환하고 `assertOwner`가 아무 검사도 하지 않습니다. 따라서 로그인하지 않아도 글을 쓸 수 있고, 모든 글을 수정·삭제할 수 있습니다. 학생은 이 임시 동작을 실제 JWT 검증과 권한 검사로 교체해야 합니다. 시작 코드를 완성본으로 제출하지 마세요. 로컬 실습용이며 공개 서버에 그대로 배포하지 않습니다.

## 실행 환경

- 모든 프론트와 Express: Node.js 20.19 이상. 실습에서는 22.12 이상 LTS도 사용 가능합니다.
- FastAPI: Python 3.10 이상. Python 3.12에서 검증합니다.
- Spring Boot: JDK 17 이상, 제공 Maven Wrapper 사용. 최초 실행에는 인터넷 연결이 필요합니다.
- 각 폴더에 고정한 직접 의존성 버전과 lock 파일을 사용하세요. `npm ci`는 package-lock.json을 사용합니다.

## 자체 확인

선택한 백엔드를 켜 둔 뒤 과제 루트에서 실행합니다. Python 추가 패키지는 필요 없습니다.

```sh
python3 tools/check_api.py --mode starter
# JWT와 권한 구현을 마친 뒤
python3 tools/check_api.py --mode final
# 만료 검사 포함: 별도 터미널에서 TOKEN_TTL=3으로 서버를 재시작한 뒤
python3 tools/check_api.py --mode final --expiry
```

Windows에서는 `python3` 대신 `py`를 사용할 수 있습니다. starter 검사는 시작 코드가 실행되는지만 확인합니다. final 검사는 핵심 API 동작을 확인하며, 보고서의 수동 시나리오와 화면 검사를 대신하지 않습니다. 검사는 임시 글을 생성하고 정상 완료 시 삭제합니다. 실패 시 남은 시험 글은 화면에서 삭제하세요.

## 제출

GitHub 저장소에 선택한 프론트·백엔드 소스, 루트 README.md, 작성한 보고서 PDF와 테스트 증거를 올립니다. 저장소 URL과 제출 기준 커밋 SHA를 LMS에 제출합니다. 마감일은 LMS 안내를 따릅니다.

`docs/06_제출용_README.md`를 학생 저장소의 루트 README.md로 복사하고 실제 서비스 내용으로 작성하세요. README에는 실행 방법, 구현 완료 범위, 보고서·테스트 증거 링크, AI 활용 요약이 필요합니다. 보고서는 `docs/report.pdf`, 증거는 `docs/evidence/`에 정리할 수 있습니다.

교수자가 열람·복제할 수 있는 공개 또는 비공개 저장소를 사용합니다. 새 폴더에 clone하여 README만으로 실행하고 GitHub에서 문서 링크가 열리는지 확인하세요. `node_modules`, `.venv`, `target`, `.env`, 실제 JWT, DB 파일은 커밋하지 않습니다. 초기 DB가 없어도 자동으로 실행되어야 합니다. 학생 배포용 ZIP은 시작 자료이며, 완성 과제는 GitHub로 제출합니다.
