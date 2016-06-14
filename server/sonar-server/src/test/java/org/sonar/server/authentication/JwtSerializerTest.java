/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.authentication.JwtSerializer.JwtSession;

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
import org.sonar.api.config.Settings;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.server.exceptions.UnauthorizedException;

public class JwtSerializerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  static final String SECRET_KEY = "HrPSavOYLNNrwTY+SOqpChr7OwvbR/zbDLdVXRN0+Eg=";

  static final String USER_LOGIN = "john";

  Settings settings = new Settings();
  System2 system2 = System2.INSTANCE;
  UuidFactory uuidFactory = UuidFactoryImpl.INSTANCE;

  JwtSerializer underTest = new JwtSerializer(settings, system2, uuidFactory);

  @Test
  public void generate_token() throws Exception {
    setDefaultSecretKey();
    underTest.start();

    String token = underTest.encode(new JwtSession(USER_LOGIN, 10));

    assertThat(token).isNotEmpty();
  }

  @Test
  public void generate_token_with_expiration_date() throws Exception {
    setDefaultSecretKey();
    underTest.start();
    Date now = new Date();

    String token = underTest.encode(new JwtSession(USER_LOGIN, 10));

    assertThat(token).isNotEmpty();
    Claims claims = underTest.decode(token).get();
    // Check expiration date it set to more than 9 seconds in the futur
    assertThat(claims.getExpiration()).isAfterOrEqualsTo(new Date(now.getTime() + 9 * 1000));
  }

  @Test
  public void generate_token_with_property() throws Exception {
    setDefaultSecretKey();
    underTest.start();

    String token = underTest.encode(new JwtSession(USER_LOGIN, 10, ImmutableMap.of("custom", "property")));

    assertThat(token).isNotEmpty();
    Claims claims = underTest.decode(token).get();
    assertThat(claims.get("custom")).isEqualTo("property");
  }

  @Test
  public void decode_token() throws Exception {
    setDefaultSecretKey();
    underTest.start();
    Date now = new Date();

    String token = underTest.encode(new JwtSession(USER_LOGIN, 20 * 60));

    Claims claims = underTest.decode(token).get();
    assertThat(claims.getId()).isNotEmpty();
    assertThat(claims.getSubject()).isEqualTo(USER_LOGIN);
    assertThat(claims.getExpiration()).isNotNull();
    assertThat(claims.getIssuedAt()).isNotNull();
    // Check expiration date it set to more than 19 minutes in the futur
    assertThat(claims.getExpiration()).isAfterOrEqualsTo(new Date(now.getTime() + 19 * 60 * 1000));
  }

  @Test
  public void return_no_token_when_expiration_date_is_reached() throws Exception {
    setDefaultSecretKey();
    underTest.start();

    String token = Jwts.builder()
      .setId("123")
      .setIssuedAt(new Date(system2.now()))
      .setExpiration(new Date(system2.now()))
      .signWith(SignatureAlgorithm.HS256, decodeSecretKey(SECRET_KEY))
      .compact();

    assertThat(underTest.decode(token)).isEmpty();
  }

  @Test
  public void return_no_token_when_secret_key_has_changed() throws Exception {
    setDefaultSecretKey();
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
  public void fail_to_decode_token_when_no_id() throws Exception {
    setDefaultSecretKey();
    underTest.start();

    String token = Jwts.builder()
      .setSubject(USER_LOGIN)
      .setIssuer("sonarqube")
      .setIssuedAt(new Date(system2.now()))
      .setExpiration(new Date(system2.now() + 20 * 60 * 1000))
      .signWith(SignatureAlgorithm.HS256, decodeSecretKey(SECRET_KEY))
      .compact();

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Token id hasn't been found");
    underTest.decode(token);
  }

  @Test
  public void fail_to_decode_token_when_no_subject() throws Exception {
    setDefaultSecretKey();
    underTest.start();

    String token = Jwts.builder()
      .setId("123")
      .setIssuer("sonarqube")
      .setIssuedAt(new Date(system2.now()))
      .setExpiration(new Date(system2.now() + 20 * 60 * 1000))
      .signWith(SignatureAlgorithm.HS256, decodeSecretKey(SECRET_KEY))
      .compact();

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Token subject hasn't been found");
    underTest.decode(token);
  }

  @Test
  public void fail_to_decode_token_when_no_expiration_date() throws Exception {
    setDefaultSecretKey();
    underTest.start();

    String token = Jwts.builder()
      .setId("123")
      .setIssuer("sonarqube")
      .setSubject(USER_LOGIN)
      .setIssuedAt(new Date(system2.now()))
      .signWith(SignatureAlgorithm.HS256, decodeSecretKey(SECRET_KEY))
      .compact();

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Token expiration date hasn't been found");
    underTest.decode(token);
  }

  @Test
  public void fail_to_decode_token_when_no_creation_date() throws Exception {
    setDefaultSecretKey();
    underTest.start();

    String token = Jwts.builder()
      .setId("123")
      .setSubject(USER_LOGIN)
      .setExpiration(new Date(system2.now() + 20 * 60 * 1000))
      .signWith(SignatureAlgorithm.HS256, decodeSecretKey(SECRET_KEY))
      .compact();

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Token creation date hasn't been found");
    underTest.decode(token);
  }

  @Test
  public void generate_new_secret_key_in_start() throws Exception {
    settings.setProperty("sonar.jwt.base64hs256secretKey", (String) null);

    underTest.start();

    assertThat(settings.getString("sonar.auth.jwtBase64Hs256Secret")).isNotEmpty();
  }

  @Test
  public void does_not_generate_new_secret_key_in_start_if_already_exists() throws Exception {
    setDefaultSecretKey();

    underTest.start();

    assertThat(settings.getString("sonar.auth.jwtBase64Hs256Secret")).isEqualTo(SECRET_KEY);
  }

  @Test
  public void refresh_token() throws Exception {
    setDefaultSecretKey();
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
  public void refresh_token_generate_a_new_hash() throws Exception {
    setDefaultSecretKey();
    underTest.start();
    String token = underTest.encode(new JwtSession(USER_LOGIN, 30));
    Optional<Claims> claims = underTest.decode(token);

    String newToken = underTest.refresh(claims.get(), 45);

    assertThat(newToken).isNotEqualTo(token);
  }

  @Test
  public void encode_fail_when_not_started() throws Exception {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("org.sonar.server.authentication.JwtSerializer not started");

    underTest.encode(new JwtSession(USER_LOGIN, 10));
  }

  @Test
  public void decode_fail_when_not_started() throws Exception {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("org.sonar.server.authentication.JwtSerializer not started");

    underTest.decode("token");
  }

  @Test
  public void refresh_fail_when_not_started() throws Exception {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("org.sonar.server.authentication.JwtSerializer not started");

    underTest.refresh(new DefaultClaims(), 10);
  }

  private void setDefaultSecretKey() {
    settings.setProperty("sonar.auth.jwtBase64Hs256Secret", SECRET_KEY);
  }

  private SecretKey decodeSecretKey(String encodedKey) {
    byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
    return new SecretKeySpec(decodedKey, 0, decodedKey.length, SignatureAlgorithm.HS256.getJcaName());
  }
}
