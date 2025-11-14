/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import io.jsonwebtoken.impl.DefaultClaimsBuilder;
import io.jsonwebtoken.security.MacAlgorithm;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.server.authentication.JwtSerializer.JwtSession;
import org.sonar.server.authentication.event.AuthenticationException;

import static org.apache.commons.lang3.time.DateUtils.addMinutes;
import static org.apache.commons.lang3.time.DateUtils.addYears;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;

public class JwtSerializerTest {

  private static final MacAlgorithm SIGNATURE_ALGORITHM = Jwts.SIG.HS256;

  private static final String A_SECRET_KEY = "HrPSavOYLNNrwTY+SOqpChr7OwvbR/zbDLdVXRN0+Eg=";
  private static final String USER_LOGIN = "john";
  private static final String SESSION_TOKEN_UUID = "ABCD";

  private final MapSettings settings = new MapSettings();
  private final System2 system2 = System2.INSTANCE;
  private final JwtSerializer underTest = new JwtSerializer(settings.asConfig(), system2);

  @Test
  public void generate_token() {
    setSecretKey(A_SECRET_KEY);
    underTest.start();

    String token = underTest.encode(new JwtSession(USER_LOGIN, SESSION_TOKEN_UUID, addMinutes(new Date(), 20).getTime()));

    assertThat(token).isNotEmpty();
  }

  @Test
  public void generate_token_with_expiration_date() {
    setSecretKey(A_SECRET_KEY);

    underTest.start();

    String token = underTest.encode(new JwtSession(USER_LOGIN, SESSION_TOKEN_UUID, addMinutes(new Date(), 20).getTime()));

    assertThat(token).isNotEmpty();
    Claims claims = underTest.decode(token).get();
    assertThat(claims.getExpiration().getTime())
      .isGreaterThanOrEqualTo(addMinutes(new Date(), 19).getTime());
  }

  @Test
  public void generate_token_with_big_expiration_date() {
    setSecretKey(A_SECRET_KEY);
    underTest.start();

    long oneYearLater = addYears(new Date(), 1).getTime();
    String token = underTest.encode(new JwtSession(USER_LOGIN, SESSION_TOKEN_UUID, oneYearLater));

    assertThat(token).isNotEmpty();
    Claims claims = underTest.decode(token).get();
    // Check expiration date it set to one year in the future
    assertThat(claims.getExpiration().getTime()).isGreaterThanOrEqualTo(oneYearLater - 1000L);
  }

  @Test
  public void generate_token_with_property() {
    setSecretKey(A_SECRET_KEY);
    underTest.start();

    String token = underTest.encode(new JwtSession(USER_LOGIN, SESSION_TOKEN_UUID, addMinutes(new Date(), 20).getTime(), ImmutableMap.of("custom", "property")));

    assertThat(token).isNotEmpty();
    Claims claims = underTest.decode(token).get();
    assertThat(claims).containsEntry("custom", "property");
  }

  @Test
  public void decode_token() {
    setSecretKey(A_SECRET_KEY);
    underTest.start();

    String token = underTest.encode(new JwtSession(USER_LOGIN, SESSION_TOKEN_UUID, addMinutes(new Date(), 20).getTime()));

    Claims claims = underTest.decode(token).get();
    assertThat(claims.getId()).isEqualTo(SESSION_TOKEN_UUID);
    assertThat(claims.getSubject()).isEqualTo(USER_LOGIN);
    assertThat(claims.getExpiration()).isNotNull();
    assertThat(claims.getIssuedAt()).isNotNull();
    // Check expiration date it set to more than 19 minutes in the future
    assertThat(claims.getExpiration()).isAfterOrEqualTo(addMinutes(new Date(), 19));
  }

  @Test
  public void return_no_token_when_expiration_date_is_reached() {
    setSecretKey(A_SECRET_KEY);
    underTest.start();

    String token = Jwts.builder()
      .id("123")
      .issuedAt(new Date(system2.now()))
      .expiration(addMinutes(new Date(), -20))
      .signWith(decodeSecretKey(A_SECRET_KEY), SIGNATURE_ALGORITHM)
      .compact();

    assertThat(underTest.decode(token)).isEmpty();
  }

  @Test
  public void return_no_token_when_secret_key_has_changed() {
    setSecretKey(A_SECRET_KEY);
    underTest.start();

    String token = Jwts.builder()
      .id("123")
      .subject(USER_LOGIN)
      .issuedAt(new Date(system2.now()))
      .expiration(addMinutes(new Date(), 20))
      .signWith(decodeSecretKey("LyWgHktP0FuHB2K+kMs3KWMCJyFHVZDdDSqpIxAMVaQ="), SIGNATURE_ALGORITHM)
      .compact();

    assertThat(underTest.decode(token)).isEmpty();
  }

  @Test
  public void return_no_token_if_none_algorithm() {
    setSecretKey(A_SECRET_KEY);
    underTest.start();

    String token = Jwts.builder()
      .id("123")
      .subject(USER_LOGIN)
      .issuedAt(new Date(system2.now()))
      .expiration(addMinutes(new Date(), 20))
      .compact();

    assertThat(underTest.decode(token)).isEmpty();
  }

  @Test
  public void fail_to_decode_token_when_no_id() {
    setSecretKey(A_SECRET_KEY);
    underTest.start();

    String token = Jwts.builder()
      .subject(USER_LOGIN)
      .issuer("sonarqube")
      .issuedAt(new Date(system2.now()))
      .expiration(addMinutes(new Date(), 20))
      .signWith(decodeSecretKey(A_SECRET_KEY), SIGNATURE_ALGORITHM)
      .compact();

    assertThatThrownBy(() -> underTest.decode(token))
      .hasMessage("Token id hasn't been found")
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", Source.jwt())
      .hasFieldOrPropertyWithValue("login", USER_LOGIN);
  }

  @Test
  public void fail_to_decode_token_when_no_subject() {
    setSecretKey(A_SECRET_KEY);
    underTest.start();

    String token = Jwts.builder()
      .id("123")
      .issuer("sonarqube")
      .issuedAt(new Date(system2.now()))
      .expiration(addMinutes(new Date(), 20))
      .signWith(decodeSecretKey(A_SECRET_KEY), SIGNATURE_ALGORITHM)
      .compact();

    assertThatThrownBy(() -> underTest.decode(token))
      .hasMessage("Token subject hasn't been found")
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", Source.jwt());
  }

  @Test
  public void fail_to_decode_token_when_no_expiration_date() {
    setSecretKey(A_SECRET_KEY);
    underTest.start();

    String token = Jwts.builder()
      .id("123")
      .issuer("sonarqube")
      .subject(USER_LOGIN)
      .issuedAt(new Date(system2.now()))
      .signWith(decodeSecretKey(A_SECRET_KEY), SIGNATURE_ALGORITHM)
      .compact();

    assertThatThrownBy(() -> underTest.decode(token))
      .hasMessage("Token expiration date hasn't been found")
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", Source.jwt())
      .hasFieldOrPropertyWithValue("login", USER_LOGIN);
  }

  @Test
  public void fail_to_decode_token_when_no_creation_date() {
    setSecretKey(A_SECRET_KEY);
    underTest.start();

    String token = Jwts.builder()
      .id("123")
      .subject(USER_LOGIN)
      .expiration(addMinutes(new Date(), 20))
      .signWith(decodeSecretKey(A_SECRET_KEY), SIGNATURE_ALGORITHM)
      .compact();

    assertThatThrownBy(() -> underTest.decode(token))
      .hasMessage("Token creation date hasn't been found")
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", Source.jwt())
      .hasFieldOrPropertyWithValue("login", USER_LOGIN);
  }

  @Test
  public void generate_new_secret_key_if_not_set_by_settings() {
    assertThat(underTest.getSecretKey()).isNull();

    underTest.start();

    assertThat(underTest.getSecretKey()).isNotNull();
    assertThat(underTest.getSecretKey().getAlgorithm()).isEqualTo("HmacSHA256");
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
    Date expiredAt = addMinutes(new Date(), 10);
    Date lastRefreshDate = addMinutes(new Date(), -4);
    Claims token = new DefaultClaimsBuilder()
      .setId("id")
      .subject("subject")
      .issuer("sonarqube")
      .issuedAt(createdAt)
      .expiration(expiredAt)
      .add("lastRefreshTime", lastRefreshDate.getTime())
      .add("key", "value")
      .build();

    // Refresh the token with a higher expiration time
    String encodedToken = underTest.refresh(token, addMinutes(new Date(), 20).getTime());

    Claims result = underTest.decode(encodedToken).get();
    assertThat(result.getId()).isEqualTo("id");
    assertThat(result.getSubject()).isEqualTo("subject");
    assertThat(result.getIssuer()).isEqualTo("sonarqube");
    assertThat(result.getIssuedAt()).isEqualTo(createdAt);
    assertThat(((long) result.get("lastRefreshTime"))).isGreaterThanOrEqualTo(now.getTime());
    assertThat(result).containsEntry("key", "value");
    // Expiration date has been changed
    assertThat(result.getExpiration()).isNotEqualTo(expiredAt)
      .isAfterOrEqualTo(addMinutes(new Date(), 19));
  }

  @Test
  public void refresh_token_generate_a_new_hash() {
    setSecretKey(A_SECRET_KEY);
    underTest.start();
    String token = underTest.encode(new JwtSession(USER_LOGIN, SESSION_TOKEN_UUID, addMinutes(new Date(), 20).getTime()));
    Optional<Claims> claims = underTest.decode(token);

    String newToken = underTest.refresh(claims.get(), addMinutes(new Date(), 45).getTime());

    assertThat(newToken).isNotEqualTo(token);
  }

  @Test
  public void encode_fail_when_not_started() {
    JwtSession jwtSession = new JwtSession(USER_LOGIN, SESSION_TOKEN_UUID, addMinutes(new Date(), 10).getTime());
    assertThatThrownBy(() -> underTest.encode(jwtSession))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("org.sonar.server.authentication.JwtSerializer not started");
  }

  @Test
  public void decode_fail_when_not_started() {
    assertThatThrownBy(() -> underTest.decode("token"))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("org.sonar.server.authentication.JwtSerializer not started");
  }

  @Test
  public void refresh_fail_when_not_started() {
    Claims claims = new DefaultClaimsBuilder().build();
    long time = addMinutes(new Date(), 10).getTime();

    assertThatThrownBy(() -> underTest.refresh(claims, time))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("org.sonar.server.authentication.JwtSerializer not started");
  }

  private SecretKey decodeSecretKey(String encodedKey) {
    byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
    return new SecretKeySpec(decodedKey, 0, decodedKey.length, "HmacSHA256");
  }

  private void setSecretKey(String s) {
    settings.setProperty("sonar.auth.jwtBase64Hs256Secret", s);
  }
}
