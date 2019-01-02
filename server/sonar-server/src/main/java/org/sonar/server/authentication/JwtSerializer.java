/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
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
import org.sonar.core.util.UuidFactory;
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

  private final Configuration config;
  private final System2 system2;
  private final UuidFactory uuidFactory;

  private SecretKey secretKey;

  public JwtSerializer(Configuration config, System2 system2, UuidFactory uuidFactory) {
    this.config = config;
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  @VisibleForTesting
  SecretKey getSecretKey() {
    return secretKey;
  }

  @Override
  public void start() {
    Optional<String> encodedKey = config.get(AUTH_JWT_SECRET.getKey());
    if (encodedKey.isPresent()) {
      this.secretKey = decodeSecretKeyProperty(encodedKey.get());
    } else {
      this.secretKey = generateSecretKey();
    }
  }

  String encode(JwtSession jwtSession) {
    checkIsStarted();
    long now = system2.now();
    JwtBuilder jwtBuilder = Jwts.builder()
      .setId(uuidFactory.create())
      .setSubject(jwtSession.getUserLogin())
      .setIssuedAt(new Date(now))
      .setExpiration(new Date(now + jwtSession.getExpirationTimeInSeconds() * 1000))
      .signWith(SIGNATURE_ALGORITHM, secretKey);
    for (Map.Entry<String, Object> entry : jwtSession.getProperties().entrySet()) {
      jwtBuilder.claim(entry.getKey(), entry.getValue());
    }
    return jwtBuilder.compact();
  }

  Optional<Claims> decode(String token) {
    checkIsStarted();
    Claims claims = null;
    try {
      claims = Jwts.parser()
        .setSigningKey(secretKey)
        .parseClaimsJws(token)
        .getBody();
      requireNonNull(claims.getId(), "Token id hasn't been found");
      requireNonNull(claims.getSubject(), "Token subject hasn't been found");
      requireNonNull(claims.getExpiration(), "Token expiration date hasn't been found");
      requireNonNull(claims.getIssuedAt(), "Token creation date hasn't been found");
      return Optional.of(claims);
    } catch (ExpiredJwtException | SignatureException e) {
      return Optional.empty();
    } catch (Exception e) {
      throw AuthenticationException.newBuilder()
        .setSource(Source.jwt())
        .setLogin(claims == null ? null : claims.getSubject())
        .setMessage(e.getMessage())
        .build();
    }
  }

  String refresh(Claims token, int expirationTimeInSeconds) {
    checkIsStarted();
    long now = system2.now();
    JwtBuilder jwtBuilder = Jwts.builder();
    for (Map.Entry<String, Object> entry : token.entrySet()) {
      jwtBuilder.claim(entry.getKey(), entry.getValue());
    }
    jwtBuilder.setExpiration(new Date(now + expirationTimeInSeconds * 1_000L))
      .signWith(SIGNATURE_ALGORITHM, secretKey);
    return jwtBuilder.compact();
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
    private final long expirationTimeInSeconds;
    private final Map<String, Object> properties;

    JwtSession(String userLogin, long expirationTimeInSeconds) {
      this(userLogin, expirationTimeInSeconds, Collections.emptyMap());
    }

    JwtSession(String userLogin, long expirationTimeInSeconds, Map<String, Object> properties) {
      this.userLogin = requireNonNull(userLogin, "User login cannot be null");
      this.expirationTimeInSeconds = expirationTimeInSeconds;
      this.properties = properties;
    }

    String getUserLogin() {
      return userLogin;
    }

    long getExpirationTimeInSeconds() {
      return expirationTimeInSeconds;
    }

    Map<String, Object> getProperties() {
      return properties;
    }
  }
}
