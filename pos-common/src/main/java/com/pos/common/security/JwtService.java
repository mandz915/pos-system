package com.pos.common.security;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.List;
import java.util.Set;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.Getter;

/** Issues and validates HS256 JSON Web Tokens. */
public class JwtService {

    private final byte[] secret;
    @Getter
    private final Duration ttl;
    private final String issuer;

    public JwtService(JwtProperties properties) {
        byte[] bytes = properties.secret() == null ? new byte[0] : properties.secret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("pos.security.jwt.secret must be at least 32 characters long");
        }
        this.secret = bytes;
        this.ttl = properties.ttl() == null ? Duration.ofHours(8) : properties.ttl();
        this.issuer = properties.issuer() == null ? "pos-system" : properties.issuer();
    }

    public String issue(AuthenticatedUser user) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(user.username())
                .claim("uid", user.userId())
                .claim("roles", new ArrayList<>(user.roles()))
                .claim("store", user.storeId())
                .claim("tenant", user.tenantId())
                .jwtID(UUID.randomUUID().toString())
                .claim("iatms", now.toEpochMilli())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(ttl)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            jwt.sign(new MACSigner(secret));
        } catch (JOSEException e) {
            throw new IllegalStateException("Unable to sign token", e);
        }
        return jwt.serialize();
    }

    public AuthenticatedUser parse(String token) {
        return parseDetailed(token).user();
    }

    /** Validates the token and also returns what is needed to revoke it. */
    public ParsedToken parseDetailed(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm()) || !jwt.verify(new MACVerifier(secret))) {
                throw new InvalidTokenException("Invalid token signature");
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expiry = claims.getExpirationTime();
            if (expiry == null || expiry.before(new Date())) {
                throw new InvalidTokenException("Token expired");
            }
            if (!issuer.equals(claims.getIssuer())) {
                throw new InvalidTokenException("Unexpected token issuer");
            }
            Long userId = claims.getLongClaim("uid");
            if (userId == null) {
                throw new InvalidTokenException("Token has no user id");
            }
            Long tenantId = claims.getLongClaim("tenant");
            if (tenantId == null) {
                throw new InvalidTokenException("Token has no tenant");
            }
            List<String> roles = claims.getStringListClaim("roles");
            AuthenticatedUser user = new AuthenticatedUser(
                    userId,
                    claims.getSubject(),
                    roles == null ? Set.of() : Set.copyOf(roles),
                    claims.getLongClaim("store"),
                    tenantId);
            Long issuedMs = claims.getLongClaim("iatms");
            Instant issuedAt = issuedMs != null ? Instant.ofEpochMilli(issuedMs)
                    : claims.getIssueTime() == null ? Instant.EPOCH : claims.getIssueTime().toInstant();
            String jti = claims.getJWTID() == null ? "" : claims.getJWTID();
            return new ParsedToken(user, jti, issuedAt, expiry.toInstant());
        } catch (ParseException | JOSEException e) {
            throw new InvalidTokenException("Malformed token");
        }
    }
}
