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

import com.google.common.collect.ImmutableMap;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.server.authentication.JwtSerializer.JwtSession;
import org.sonar.server.authentication.event.AuthenticationEvent.Source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.authentication.event.AuthenticationExceptionMatcher.authenticationException;

public class JwtSerializerTest {

  private static final String A_SECRET_KEY = "HrPSavOYLNNrwTY+SOqpChr7OwvbR/zbDLdVXRN0+Eg=";
  private static final String USER_LOGIN = "john";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MapSettings settings = new MapSettings();
  private System2 system2 = System2.INSTANCE;
  private UuidFactory uuidFactory = UuidFactoryImpl.INSTANCE;
  private JwtSerializer underTest = new JwtSerializer(settings.asConfig(), system2, uuidFactory);

  @Test
  public void generate_token() {
    setSecretKey(A_SECRET_KEY);
    underTest.start();

    String token = underTest.encode(new JwtSession(USER_LOGIN, 10));

    assertThat(token).isNotEmpty();
  }

  @Test
  public void generate_token_with_expiration_date() {
    setSecretKey(A_SECRET_KEY);

    underTest.start();
    Date now = new Date();

    long expirationTimeInSeconds = 10L;
    String token = underTest.encode(new JwtSession(USER_LOGIN, expirationTimeInSeconds));

    assertThat(token).isNotEmpty();
    Claims claims = underTest.decode(token).get();
    assertThat(claims.getExpiration().getTime()).isGreaterThanOrEqualTo(now.getTime() + expirationTimeInSeconds * 1000L - 1000L);
  }

  @Test
  public void generate_token_with_big_expiration_date() {
    setSecretKey(A_SECRET_KEY);
    underTest.start();
    Date now = new Date();

    long oneYearInSeconds = 12 * 30 * 24 * 60 * 60L;
    String token = underTest.encode(new JwtSession(USER_LOGIN, oneYearInSeconds));

    assertThat(token).isNotEmpty();
    Claims claims = underTest.decode(token).get();
    // Check expiration date it set to one year in the future
    assertThat(claims.getExpiration().getTime()).isGreaterThanOrEqualTo(now.getTime() + oneYearInSeconds * 1000L - 1000L);
  }

  @Test
  public void generate_token_with_property() {
    setSecretKey(A_SECRET_KEY);
    underTest.start();

    String token = underTest.encode(new JwtSession(USER_LOGIN, 10, ImmutableMap.of("custom", "property")));

    assertThat(token).isNotEmpty();
    Claims claims = underTest.decode(token).get();
    assertThat(claims.get("custom")).isEqualTo("property");
  }

  @Test
  public void decode_token() {
    setSecretKey(A_SECRET_KEY);
    underTest.start();
    Date now = new Date();

    String token = underTest.encode(new JwtSession(USER_LOGIN, 20 * 60));

    Claims claims = underTest.decode(token).get();
    assertThat(claims.getId()).isNotEmpty();
    assertThat(claims.getSubject()).isEqualTo(USER_LOGIN);
    assertThat(claims.getExpiration()).isNotNull();
    assertThat(claims.getIssuedAt()).isNotNull();
    // Check expiration date it set to more than 19 minutes in the future
    assertThat(claims.getExpiration()).isAfterOrEqualsTo(new Date(now.getTime() + 19 * 60 * 1000));
  }

  @Test
  public void return_no_token_when_expiration_date_is_reached() {
    setSecretKey(A_SECRET_KEY);
    underTest.start();

    String token = Jwts.builder()
      .setId("123")
      .setIssuedAt(new Date(system2.now()))
      .setExpiration(new Date(system2.now()))
      .signWith(SignatureAlgorithm.HS256, decodeSecretKey(A_SECRET_KEY))
      .compact();

    assertThat(underTest.decode(token)).isEmpty();
  }

  @Test
  public void return_no_token_when_secret_key_has_changed() {
    setSecretKey(A_SECRET_KEY);
    underTest.start();

    String token = Jwts.builder()
      .setId("123")
      .setSubject(USER_LOGIN)
      .setIssuedAt(new Date(system2.now()))
      .setExpiration(new Date(system2.now() + 20 * 60 * 1000))
      .signWith(SignatureAlgorithm.HS256, decodeSecretKey("LyWgHktP0FuHB2K+kMs3KWMCJyFHVZDdDSqpIxAMVaQ="))
      .compact();

    assertThat(underTest.decode(token)).isEmpty();
  }

  @Test
  public void fail_to_decode_token_when_no_id() {
    setSecretKey(A_SECRET_KEY);
    underTest.start();

    String token = Jwts.builder()
      .setSubject(USER_LOGIN)
      .setIssuer("sonarqube")
      .setIssuedAt(new Date(system2.now()))
      .setExpiration(new Date(system2.now() + 20 * 60 * 1000))
      .signWith(SignatureAlgorithm.HS256, decodeSecretKey(A_SECRET_KEY))
      .compact();

    expectedException.expect(authenticationException().from(Source.jwt()).withLogin(USER_LOGIN).andNoPublicMessage());
    expectedException.expectMessage("Token id hasn't been found");
    underTest.decode(token);
  }

  @Test
  public void fail_to_decode_token_when_no_subject() {
    setSecretKey(A_SECRET_KEY);
    underTest.start();

    String token = Jwts.builder()
      .setId("123")
      .setIssuer("sonarqube")
      .setIssuedAt(new Date(system2.now()))
      .setExpiration(new Date(system2.now() + 20 * 60 * 1000))
      .signWith(SignatureAlgorithm.HS256, decodeSecretKey(A_SECRET_KEY))
      .compact();

    expectedException.expect(authenticationException().from(Source.jwt()).withoutLogin().andNoPublicMessage());
    expectedException.expectMessage("Token subject hasn't been found");
    underTest.decode(token);
  }

  @Test
  public void fail_to_decode_token_when_no_expiration_date() {
    setSecretKey(A_SECRET_KEY);
    underTest.start();

    String token = Jwts.builder()
      .setId("123")
      .setIssuer("sonarqube")
      .setSubject(USER_LOGIN)
      .setIssuedAt(new Date(system2.now()))
      .signWith(SignatureAlgorithm.HS256, decodeSecretKey(A_SECRET_KEY))
      .compact();

    expectedException.expect(authenticationException().from(Source.jwt()).withLogin(USER_LOGIN).andNoPublicMessage());
    expectedException.expectMessage("Token expiration date hasn't been found");
    underTest.decode(token);
  }

  @Test
  public void fail_to_decode_token_when_no_creation_date() {
    setSecretKey(A_SECRET_KEY);
    underTest.start();

    String token = Jwts.builder()
      .setId("123")
      .setSubject(USER_LOGIN)
      .setExpiration(new Date(system2.now() + 20 * 60 * 1000))
      .signWith(SignatureAlgorithm.HS256, decodeSecretKey(A_SECRET_KEY))
      .compact();

    expectedException.expect(authenticationException().from(Source.jwt()).withLogin(USER_LOGIN).andNoPublicMessage());
    expectedException.expectMessage("Token creation date hasn't been found");
    underTest.decode(token);
  }

  @Test
  public void generate_new_secret_key_if_not_set_by_settings() {
    assertThat(underTest.getSecretKey()).isNull();

    underTest.start();

    assertThat(underTest.getSecretKey()).isNotNull();
    assertThat(underTest.getSecretKey().getAlgorithm()).isEqualTo(SignatureAlgorithm.HS256.getJcaName());
  }

  @Test
  public void load_secret_key_from_settings() {
    setSecretKey(A_SECRET_KEY);

    underTest.start();

    assertThat(settings.getString("sonar.auth.jwtBase64Hs256Secret")).isEqualTo(A_SECRET_KEY);
  }

  @Test
  public void refresh_token() {
    setSecretKey(A_SECRET_KEY);
    underTest.start();

    Date now = new Date();
    Date createdAt = DateUtils.parseDate("2016-01-01");
    // Expired in 10 minutes
    Date expiredAt = new Date(now.getTime() + 10 * 60 * 1000);
    Claims token = new DefaultClaims()
      .setId("id")
      .setSubject("subject")
      .setIssuer("sonarqube")
      .setIssuedAt(createdAt)
      .setExpiration(expiredAt);
    token.put("key", "value");

    // Refresh the token with a higher expiration time
    String encodedToken = underTest.refresh(token, 20 * 60);

    Claims result = underTest.decode(encodedToken).get();
    assertThat(result.getId()).isEqualTo("id");
    assertThat(result.getSubject()).isEqualTo("subject");
    assertThat(result.getIssuer()).isEqualTo("sonarqube");
    assertThat(result.getIssuedAt()).isEqualTo(createdAt);
    assertThat(result.get("key")).isEqualTo("value");
    // Expiration date has been changed
    assertThat(result.getExpiration()).isNotEqualTo(expiredAt)
      .isAfterOrEqualsTo(new Date(now.getTime() + 19 * 1000));
  }

  @Test
  public void refresh_token_generate_a_new_hash() {
    setSecretKey(A_SECRET_KEY);
    underTest.start();
    String token = underTest.encode(new JwtSession(USER_LOGIN, 30));
    Optional<Claims> claims = underTest.decode(token);

    String newToken = underTest.refresh(claims.get(), 45);

    assertThat(newToken).isNotEqualTo(token);
  }

  @Test
  public void encode_fail_when_not_started() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("org.sonar.server.authentication.JwtSerializer not started");

    underTest.encode(new JwtSession(USER_LOGIN, 10));
  }

  @Test
  public void decode_fail_when_not_started() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("org.sonar.server.authentication.JwtSerializer not started");

    underTest.decode("token");
  }

  @Test
  public void refresh_fail_when_not_started() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("org.sonar.server.authentication.JwtSerializer not started");

    underTest.refresh(new DefaultClaims(), 10);
  }

  private SecretKey decodeSecretKey(String encodedKey) {
    byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
    return new SecretKeySpec(decodedKey, 0, decodedKey.length, SignatureAlgorithm.HS256.getJcaName());
  }

  private void setSecretKey(String s) {
    settings.setProperty("sonar.auth.jwtBase64Hs256Secret", s);
  }
}
