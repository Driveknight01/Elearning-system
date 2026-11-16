package com.elearning.security;

import com.elearning.common.exception.AppException;
import com.elearning.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Verifies Google Sign-In ID Tokens (RS256) against Google's published JWK set
 * for both Web React (Google Identity Services) and Android Native (Credential Manager / Google Sign-In).
 */
@Slf4j
@Component
public class GoogleOAuth2Verifier {

    private static final Set<String> VALID_ISSUERS = Set.of(
            "https://accounts.google.com",
            "accounts.google.com"
    );
    private static final String EXPECTED_ALGORITHM = "RS256";
    private static final long MIN_KEY_ROTATION_REFRESH_INTERVAL_SECONDS = 30L;

    private final GoogleOAuth2Properties properties;
    private final ObjectMapper objectMapper;
    private final Supplier<String> jwksFetcher;

    private volatile CachedJwks cachedJwks = new CachedJwks(Map.of(), Instant.EPOCH, Instant.EPOCH);
    private final Object refreshLock = new Object();

    @Autowired
    public GoogleOAuth2Verifier(GoogleOAuth2Properties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, createDefaultJwksFetcher(properties));
    }

    GoogleOAuth2Verifier(
            GoogleOAuth2Properties properties,
            ObjectMapper objectMapper,
            Supplier<String> jwksFetcher
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.jwksFetcher = jwksFetcher;
    }

    /**
     * Factory method for integration/unit tests providing an in-memory JWKS JSON supplier.
     */
    public static GoogleOAuth2Verifier withJwksSupplier(
            GoogleOAuth2Properties properties,
            ObjectMapper objectMapper,
            Supplier<String> jwksFetcher
    ) {
        return new GoogleOAuth2Verifier(properties, objectMapper, jwksFetcher);
    }

    /**
     * Verifies the cryptographic signature and OpenID Connect claims of a Google ID token.
     *
     * @param idToken raw Google ID token sent from Web or Android client
     * @return verified Google identity claims
     */
    public GoogleUserInfo verify(String idToken) {
        if (!StringUtils.hasText(idToken)) {
            throw new AppException(ErrorCode.INVALID_TOKEN, "Google ID token không được để trống");
        }
        if (!properties.isConfigured()) {
            throw new AppException(
                    ErrorCode.INVALID_REQUEST,
                    "Google Client ID chưa được cấu hình (app.oauth2.google-client-id)"
            );
        }

        Claims claims = parseAndVerifyJwt(idToken.trim());
        validateClaims(claims);

        String subject = claims.getSubject();
        String email = claims.get("email", String.class).trim().toLowerCase();
        String name = claims.get("name", String.class);
        String fullName = StringUtils.hasText(name) ? name.trim() : email.substring(0, email.indexOf('@'));
        String avatarUrl = claims.get("picture", String.class);

        return new GoogleUserInfo(
                subject,
                email,
                true,
                fullName,
                StringUtils.hasText(avatarUrl) ? avatarUrl.trim() : null
        );
    }

    private Claims parseAndVerifyJwt(String idToken) {
        Locator<Key> keyLocator = this::resolveSigningKey;
        try {
            return Jwts.parser()
                    .keyLocator(keyLocator)
                    .clockSkewSeconds(60)
                    .build()
                    .parseSignedClaims(idToken)
                    .getPayload();
        } catch (AppException ex) {
            throw ex;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Failed to verify Google ID token: {}", ex.getMessage());
            throw new AppException(ErrorCode.INVALID_TOKEN, "Google ID token không hợp lệ hoặc đã hết hạn");
        }
    }

    private Key resolveSigningKey(Header header) {
        if (!(header instanceof JwsHeader jwsHeader)) {
            throw new UnsupportedJwtException("Google ID token must be a signed JWS");
        }
        if (!EXPECTED_ALGORITHM.equalsIgnoreCase(jwsHeader.getAlgorithm())) {
            throw new UnsupportedJwtException("Unsupported Google ID token algorithm: " + jwsHeader.getAlgorithm());
        }
        String kid = jwsHeader.getKeyId();
        if (!StringUtils.hasText(kid)) {
            throw new UnsupportedJwtException("Missing key ID (kid) in Google ID token");
        }

        PublicKey key = getPublicKey(kid);
        if (key == null) {
            throw new UnsupportedJwtException("No matching Google public key found for kid: " + kid);
        }
        return key;
    }

    private void validateClaims(Claims claims) {
        String issuer = claims.getIssuer();
        if (!StringUtils.hasText(issuer) || !VALID_ISSUERS.contains(issuer)) {
            throw new AppException(ErrorCode.INVALID_TOKEN, "Google ID token issuer không hợp lệ");
        }

        Set<String> audiences = claims.getAudience();
        Set<String> allowedClientIds = properties.getAllowedClientIds();
        if (audiences == null || audiences.isEmpty() || Collections.disjoint(audiences, allowedClientIds)) {
            throw new AppException(ErrorCode.INVALID_TOKEN, "Google ID token không khớp với Client ID của ứng dụng");
        }

        if (audiences.size() > 1) {
            String azp = claims.get("azp", String.class);
            if (StringUtils.hasText(azp) && !allowedClientIds.contains(azp)) {
                throw new AppException(ErrorCode.INVALID_TOKEN, "Authorized party (azp) của Google ID token không hợp lệ");
            }
        }

        if (!isEmailVerified(claims.get("email_verified"))) {
            throw new AppException(ErrorCode.INVALID_TOKEN, "Tài khoản Google chưa xác minh địa chỉ email");
        }

        if (!StringUtils.hasText(claims.getSubject())) {
            throw new AppException(ErrorCode.INVALID_TOKEN, "Google ID token thiếu thông tin định danh người dùng");
        }

        String email = claims.get("email", String.class);
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            throw new AppException(ErrorCode.INVALID_TOKEN, "Google ID token thiếu địa chỉ email hợp lệ");
        }
    }

    private boolean isEmailVerified(Object rawClaim) {
        if (rawClaim instanceof Boolean boolVal) {
            return boolVal;
        }
        if (rawClaim instanceof String strVal) {
            return Boolean.parseBoolean(strVal);
        }
        return false;
    }

    private PublicKey getPublicKey(String kid) {
        Instant now = Instant.now();
        CachedJwks current = this.cachedJwks;
        if (!current.isExpired(now)) {
            PublicKey cached = current.keys().get(kid);
            if (cached != null) {
                return cached;
            }
        }

        synchronized (refreshLock) {
            now = Instant.now();
            current = this.cachedJwks;
            boolean expired = current.isExpired(now);
            if (!expired) {
                PublicKey cached = current.keys().get(kid);
                if (cached != null) {
                    return cached;
                }
                if (now.isBefore(current.lastForcedRefreshAt().plusSeconds(MIN_KEY_ROTATION_REFRESH_INTERVAL_SECONDS))) {
                    return null;
                }
            }

            Map<String, PublicKey> refreshed = fetchAndParseJwks();
            Instant lastForcedRefreshAt = expired ? Instant.EPOCH : now;
            this.cachedJwks = new CachedJwks(
                    refreshed,
                    now.plusSeconds(Math.max(60L, properties.getJwksCacheSeconds())),
                    lastForcedRefreshAt
            );
            return refreshed.get(kid);
        }
    }

    private Map<String, PublicKey> fetchAndParseJwks() {
        String responseBody;
        try {
            responseBody = jwksFetcher.get();
        } catch (RestClientException ex) {
            log.error("Failed to fetch Google JWKS from {}: {}", properties.getJwksUri(), ex.getMessage());
            throw new AppException(ErrorCode.UNAUTHORIZED, "Không thể kết nối máy chủ xác thực Google lúc này");
        }

        if (!StringUtils.hasText(responseBody)) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Phản hồi khóa công khai từ Google trống");
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode keysNode = root.get("keys");
            if (keysNode == null || !keysNode.isArray()) {
                throw new IllegalStateException("Missing 'keys' array in Google JWKS payload");
            }

            Map<String, PublicKey> result = new HashMap<>();
            KeyFactory rsaFactory = KeyFactory.getInstance("RSA");

            for (JsonNode keyNode : keysNode) {
                String kty = textOrNull(keyNode, "kty");
                String kid = textOrNull(keyNode, "kid");
                String n = textOrNull(keyNode, "n");
                String e = textOrNull(keyNode, "e");
                String use = textOrNull(keyNode, "use");
                String alg = textOrNull(keyNode, "alg");

                if (!"RSA".equalsIgnoreCase(kty) || !StringUtils.hasText(kid)
                        || !StringUtils.hasText(n) || !StringUtils.hasText(e)) {
                    continue;
                }
                if (StringUtils.hasText(use) && !"sig".equalsIgnoreCase(use)) {
                    continue;
                }
                if (StringUtils.hasText(alg) && !EXPECTED_ALGORITHM.equalsIgnoreCase(alg)) {
                    continue;
                }

                BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(n));
                BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(e));
                RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
                result.put(kid, rsaFactory.generatePublic(spec));
            }
            return Collections.unmodifiableMap(result);
        } catch (AppException ex) {
            throw ex;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException | java.io.IOException ex) {
            log.error("Failed to parse Google JWKS payload", ex);
            throw new AppException(ErrorCode.UNAUTHORIZED, "Không thể xử lý khóa công khai xác thực Google");
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return (child != null && child.isTextual()) ? child.asText() : null;
    }

    private static Supplier<String> createDefaultJwksFetcher(GoogleOAuth2Properties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        RestClient restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();

        return () -> restClient.get()
                .uri(properties.getJwksUri())
                .retrieve()
                .body(String.class);
    }

    private record CachedJwks(
            Map<String, PublicKey> keys,
            Instant expiresAt,
            Instant lastForcedRefreshAt
    ) {
        boolean isExpired(Instant now) {
            return keys.isEmpty() || !now.isBefore(expiresAt);
        }
    }
}
