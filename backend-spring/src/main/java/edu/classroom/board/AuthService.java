package edu.classroom.board;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;

@Service
public class AuthService {
  private static final String EXAMPLE_SECRET = "REPLACE_WITH_RANDOM_64_HEX_CHARACTERS";
  // "Bearer " 뒤에 점으로 구분된 base64url 세 부분(header.payload.signature)
  private static final Pattern BEARER =
      Pattern.compile("Bearer ([A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+)");
  // sub는 양의 정수 문자열이어야 합니다.
  private static final Pattern USER_ID = Pattern.compile("[1-9][0-9]{0,15}");

  private final Users users;
  private final SecretKey key;
  private final String issuer;
  private final String audience;
  private final int ttl;

  public AuthService(
      Users users,
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.issuer}") String issuer,
      @Value("${app.jwt.audience}") String audience,
      @Value("${app.jwt.ttl}") int ttl) {
    // 서버 시작 시 빈 키·예시 키·형식 오류를 거부합니다. 예외가 나면 서버가 시작되지 않습니다.
    if (secret == null || secret.isBlank())
      throw new IllegalStateException("JWT_SECRET 환경변수가 비어 있습니다.");
    if (secret.equals(EXAMPLE_SECRET))
      throw new IllegalStateException("예시 키는 사용할 수 없습니다. JWT_SECRET을 새로 생성하세요.");
    if (!secret.matches("[0-9a-fA-F]{64}"))
      throw new IllegalStateException("JWT_SECRET은 64자리 16진수여야 합니다.");
    if (ttl <= 0) throw new IllegalStateException("TOKEN_TTL은 양의 정수여야 합니다.");

    this.users = users;
    // 64자리 hex 문자열 → 32바이트 → HS256 서명 키
    this.key = Keys.hmacShaKeyFor(HexFormat.of().parseHex(secret));
    this.issuer = issuer;
    this.audience = audience;
    this.ttl = ttl;
  }

  // BE-01: 계정 확인이 끝난 사용자에게 HS256 JWT를 발급합니다. 비밀번호는 넣지 않습니다.
  public String issueToken(Users.PublicUser user) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(String.valueOf(user.id()))
        .issuer(issuer)
        .audience().add(audience).and()
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(ttl)))
        .signWith(key, Jwts.SIG.HS256)
        .compact();
  }

  // BE-02: Bearer 추출 → 서명/알고리즘/만료/클레임 검증 → 실제 사용자 확인.
  // 검증을 모두 통과한 사용자 ID만 반환합니다.
  public long currentUserId(HttpServletRequest request) {
    // 1) 헤더가 없거나 "Bearer "로 시작하지 않으면 토큰이 없는 것으로 봅니다.
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer "))
      throw new ApiError(401, "TOKEN_REQUIRED", "로그인이 필요합니다.");
    Matcher m = BEARER.matcher(header);
    if (!m.matches()) throw invalidToken();

    // 2) 서명·iss·aud·exp를 라이브러리로 검증합니다. decode만 하지 않습니다.
    Claims claims;
    try {
      Jws<Claims> jws =
          Jwts.parser()
              .verifyWith(key)
              .requireIssuer(issuer)
              .requireAudience(audience)
              .build()
              .parseSignedClaims(m.group(1));
      // 3) 허용 알고리즘을 HS256으로 고정합니다.
      if (!Jwts.SIG.HS256.getId().equals(jws.getHeader().getAlgorithm())) throw invalidToken();
      claims = jws.getPayload();
    } catch (ExpiredJwtException e) {
      // 서명은 정상이지만 exp가 지난 토큰
      throw new ApiError(401, "TOKEN_EXPIRED", "로그인 시간이 만료되었습니다. 다시 로그인하세요.");
    } catch (JwtException | IllegalArgumentException e) {
      // 서명 불일치, 형식 오류, iss/aud 불일치 등
      throw invalidToken();
    }

    // 4) JJWT는 exp가 없으면 만료 검사를 건너뛰므로 exp 존재를 직접 확인합니다.
    if (claims.getExpiration() == null) throw invalidToken();

    // 5) sub 형식과 실제 계정 존재를 확인합니다.
    String sub = claims.getSubject();
    if (sub == null || !USER_ID.matcher(sub).matches()) throw invalidToken();
    long userId = Long.parseLong(sub);
    if (users.find(userId) == null) throw invalidToken();
    return userId;
  }

  private ApiError invalidToken() {
    return new ApiError(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다. 다시 로그인하세요.");
  }

  // BE-03: 토큰으로 확인한 사용자와 DB에 저장된 작성자가 다르면 403입니다.
  public void assertOwner(long userId, long authorId) {
    if (userId != authorId)
      throw new ApiError(403, "FORBIDDEN", "본인이 작성한 글만 수정·삭제할 수 있습니다.");
  }
}
