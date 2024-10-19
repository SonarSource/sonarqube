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
package org.sonar.auth.github;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

public class ExpiringAppInstallationTokenTest {

  private static final Clock CLOCK = Clock.fixed(Instant.parse("1970-01-11T10:00:00.000Z"), ZoneId.of("+3"));
  private static final String DATE = "2024-08-28T10:44:51Z";

  @Test
  void test_value() {
    AppInstallationToken underTest = new ExpiringAppInstallationToken(CLOCK, "foo", DATE);

    assertThat(underTest.toString())
      .isEqualTo(underTest.getValue())
      .isEqualTo("foo");
    assertThat(underTest.getAuthorizationHeaderPrefix()).isEqualTo("Token");
  }

  @Test
  void test_equals_hashCode() {
    AppInstallationToken foo = new ExpiringAppInstallationToken(CLOCK, "foo", DATE);

    assertThat(foo.equals(foo)).isTrue();
    assertThat(foo.equals(null)).isFalse();
    assertThat(foo.equals(new ExpiringAppInstallationToken(CLOCK, "foo", DATE))).isTrue();
    assertThat(foo.equals(new ExpiringAppInstallationToken(CLOCK, "bar", DATE))).isFalse();
    assertThat(foo.equals("foo")).isFalse();

    assertThat(foo).hasSameHashCodeAs(new ExpiringAppInstallationToken(CLOCK, "foo", DATE));
  }

  @ParameterizedTest
  @MethodSource("dateAndExpiredProvider")
  void isExpired(String expirationDate, boolean expectedExpired) {
    System.out.println(ZonedDateTime.ofInstant(CLOCK.instant(), CLOCK.getZone()));
    ExpiringAppInstallationToken appInstallationToken = new ExpiringAppInstallationToken(CLOCK, "foo", expirationDate);

    assertThat(appInstallationToken.isExpired()).isEqualTo(expectedExpired);
  }

  public static Stream<Arguments> dateAndExpiredProvider() {
    return Stream.of(
      Arguments.of("1970-01-11T12:00+03:00", true),
      Arguments.of("1970-01-11T12:59+03:00", true),
      Arguments.of("1970-01-11T13:00+03:00", true),
      Arguments.of("1970-01-11T13:01+03:00", false),
      Arguments.of("1970-01-11T14:00+03:00", false),
      Arguments.of("1970-01-11T14:01+04:00", false),
      Arguments.of("1970-01-11T14:00+04:00", true)
      );
  }

}
