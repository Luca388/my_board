// FE-01: 로그인 상태는 이 탭의 메모리에만 보관합니다.
// localStorage·sessionStorage에 저장하지 않으므로 새로고침하면 로그아웃됩니다.
let session = { accessToken: "", user: null };
export const getSession = () => session;
export function clearSession() {
  session = { accessToken: "", user: null };
}
function authorizationHeaders() {
  // FE-02: 토큰이 있을 때만 Bearer 헤더를 붙입니다. "Bearer undefined"를 만들지 않습니다.
  return session.accessToken
    ? { Authorization: `Bearer ${session.accessToken}` }
    : {};
}
// FE-03: 서버의 JSON code별 화면 안내 문구
const messages = {
  BAD_INPUT: "입력을 확인하세요. 제목은 1~100자, 내용은 1~5000자이며 공백만 입력할 수 없습니다.",
  INVALID_CREDENTIALS: "이메일 또는 비밀번호가 올바르지 않습니다.",
  TOKEN_REQUIRED: "로그인이 필요합니다. 로그인한 뒤 다시 시도하세요.",
  INVALID_TOKEN: "로그인 정보가 유효하지 않습니다. 다시 로그인하세요.",
  TOKEN_EXPIRED: "로그인 시간이 만료되었습니다. 다시 로그인하세요.",
  FORBIDDEN: "본인이 작성한 글만 수정·삭제할 수 있습니다.",
  NOT_FOUND: "게시글이 없습니다. 이미 삭제되었을 수 있습니다.",
};
export async function request(path, options = {}) {
  const response = await fetch(`/api${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...authorizationHeaders(),
      ...options.headers,
    },
  });
  // 204 No Content는 본문이 없으므로 JSON으로 파싱하지 않습니다.
  // 오류 본문이 JSON이 아닐 때(프록시 오류 등)도 화면이 멈추지 않도록 빈 객체로 둡니다.
  const data =
    response.status === 204 ? null : await response.json().catch(() => ({}));
  if (!response.ok) {
    // FE-03: 401이면 토큰과 사용자 상태를 지웁니다. 403은 로그인 상태를 유지합니다.
    if (response.status === 401) clearSession();
    const error = new Error(
      messages[data.code] || data.message || data.code || `HTTP ${response.status}`,
    );
    error.status = response.status;
    error.code = data.code;
    throw error;
  }
  return data;
}
export async function login(email, password) {
  // FE-01: 이전 로그인 상태를 지우고, 성공한 경우에만 토큰과 사용자를 저장합니다.
  clearSession();
  const data = await request("/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
  session = { accessToken: data.accessToken, user: data.user };
  return data.user;
}
export function canEdit(post) {
  // FE-04: 로그인 사용자와 작성자를 비교해 버튼 표시만 결정합니다.
  // 실제 권한은 서버의 assertOwner가 다시 검사합니다.
  return Boolean(session.user && post && session.user.id === post.authorId);
}
export const api = {
  list: () => request("/posts"),
  detail: (id) => request(`/posts/${id}`),
  create: (input) =>
    request("/posts", { method: "POST", body: JSON.stringify(input) }),
  update: (id, input) =>
    request(`/posts/${id}`, { method: "PUT", body: JSON.stringify(input) }),
  remove: (id) => request(`/posts/${id}`, { method: "DELETE" }),
  me: () => request("/me"),
};
