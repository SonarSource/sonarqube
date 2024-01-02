/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.authentication;

import com.google.common.annotations.VisibleForTesting;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import javax.annotation.concurrent.Immutable;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.server.authentication.event.AuthenticationEvent.Source;
import org.sonar.server.authentication.event.AuthenticationException;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.jsonwebtoken.impl.crypto.MacProvider.generateKey;
import static java.util.Objects.requireNonNull;
import static org.sonar.process.ProcessProperties.Property.AUTH_JWT_SECRET;

/**
 * This class can be used to encode or decode a JWT token
 */
@ServerSide
public class JwtSerializer implements Startable {

  private static final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS256;

  static final String LAST_REFRESH_TIME_PARAM = "lastRefreshTime";

  private final Configuration config;
  private final System2 system2;

  private SecretKey secretKey;

  public JwtSerializer(Configuration config, System2 system2) {
    this.config = config;
    this.system2 = system2;
  }

  @VisibleForTesting
  SecretKey getSecretKey() {
    return secretKey;
  }

  @Override
  public void start() {
    Optional<String> encodedKey = config.get(AUTH_JWT_SECRET.getKey());
    this.secretKey = encodedKey
      .map(JwtSerializer::decodeSecretKeyProperty)
      .orElseGet(JwtSerializer::generateSecretKey);
  }

  String encode(JwtSession jwtSession) {
    checkIsStarted();
    return Jwts.builder()
      .addClaims(jwtSession.getProperties())
      .claim(LAST_REFRESH_TIME_PARAM, system2.now())
      .setId(jwtSession.getSessionTokenUuid())
      .setSubject(jwtSession.getUserLogin())
      .setIssuedAt(new Date(system2.now()))
      .setExpiration(new Date(jwtSession.getExpirationTime()))
      .signWith(secretKey, SIGNATURE_ALGORITHM)
      .compact();
  }

  Optional<Claims> decode(String token) {
    checkIsStarted();
    Claims claims = null;
    try {
      claims = Jwts.parserBuilder()
        .setSigningKey(secretKey)
        .build()
        .parseClaimsJws(token)
        .getBody();
      requireNonNull(claims.getId(), "Token id hasn't been found");
      requireNonNull(claims.getSubject(), "Token subject hasn't been found");
      requireNonNull(claims.getExpiration(), "Token expiration date hasn't been found");
      requireNonNull(claims.getIssuedAt(), "Token creation date hasn't been found");
      return Optional.of(claims);
    } catch (UnsupportedJwtException | ExpiredJwtException | SignatureException e) {
      return Optional.empty();
    } catch (Exception e) {
      throw AuthenticationException.newBuilder()
        .setSource(Source.jwt())
        .setLogin(claims == null ? null : claims.getSubject())
        .setMessage(e.getMessage())
        .build();
    }
  }

  String refresh(Claims token, long expirationTime) {
    checkIsStarted();
    return Jwts.builder()
      .setClaims(token)
      .claim(LAST_REFRESH_TIME_PARAM, system2.now())
      .setExpiration(new Date(expirationTime))
      .signWith(secretKey, SIGNATURE_ALGORITHM)
      .compact();
  }

  private static SecretKey generateSecretKey() {
    return generateKey(SIGNATURE_ALGORITHM);
  }

  private static SecretKey decodeSecretKeyProperty(String base64SecretKey) {
    byte[] decodedKey = Base64.getDecoder().decode(base64SecretKey);
    return new SecretKeySpec(decodedKey, 0, decodedKey.length, SIGNATURE_ALGORITHM.getJcaName());
  }

  private void checkIsStarted() {
    checkNotNull(secretKey, "%s not started", getClass().getName());
  }

  @Override
  public void stop() {
    secretKey = null;
  }

  @Immutable
  static class JwtSession {

    private final String userLogin;
    private final String sessionTokenUuid;
    private final long expirationTime;
    private final Map<String, Object> properties;

    JwtSession(String userLogin, String sessionTokenUuid, long expirationTime) {
      this(userLogin, sessionTokenUuid, expirationTime, Collections.emptyMap());
    }

    JwtSession(String userLogin, String sessionTokenUuid, long expirationTime, Map<String, Object> properties) {
      this.userLogin = requireNonNull(userLogin, "User login cannot be null");
      this.sessionTokenUuid = requireNonNull(sessionTokenUuid, "Session token UUID cannot be null");
      this.expirationTime = expirationTime;
      this.properties = properties;
    }

    String getUserLogin() {
      return userLogin;
    }

    String getSessionTokenUuid() {
      return sessionTokenUuid;
    }

    long getExpirationTime() {
      return expirationTime;
    }

    Map<String, Object> getProperties() {
      return properties;
    }
  }
}
