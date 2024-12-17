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
package org.sonar.scanner;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import javax.annotation.Nullable;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.MessageException;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ProjectInfoTest {

  private final MapSettings settings = new MapSettings();
  private final Clock clock = mock(Clock.class);
  private final ProjectInfo underTest = new ProjectInfo(settings.asConfig(), clock);

  @Test
  void testSimpleDateTime() {
    OffsetDateTime date = OffsetDateTime.of(2017, 1, 1, 12, 13, 14, 0, ZoneOffset.ofHours(2));
    settings.appendProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2017-01-01T12:13:14+0200");
    settings.appendProperty(CoreProperties.PROJECT_VERSION_PROPERTY, "version");

    underTest.start();

    assertThat(underTest.getAnalysisDate()).isEqualTo(Date.from(date.toInstant()));
    assertThat(underTest.getProjectVersion()).contains("version");
  }

  @Test
  void testSimpleDate() {
    LocalDate date = LocalDate.of(2017, 1, 1);
    settings.appendProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2017-01-01");

    underTest.start();

    assertThat(underTest.getAnalysisDate())
      .isEqualTo(Date.from(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
  }

  @Test
  void emptyDate() {
    settings.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "");
    settings.setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, "version");

    assertThatThrownBy(underTest::start)
      .isInstanceOf(RuntimeException.class);
  }

  @Test
  void fail_with_too_long_version() {
    String version = secure().nextAlphabetic(101);
    settings.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2017-01-01");
    settings.setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, version);

    assertThatThrownBy(underTest::start)
      .isInstanceOf(MessageException.class)
      .hasMessage("\"" + version + "\" is not a valid project version. " +
        "The maximum length is 100 characters.");
  }

  @Test
  void fail_with_too_long_buildString() {
    String buildString = secure().nextAlphabetic(101);
    settings.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2017-01-01");
    settings.setProperty(CoreProperties.BUILD_STRING_PROPERTY, buildString);

    assertThatThrownBy(underTest::start)
      .isInstanceOf(MessageException.class)
      .hasMessage("\"" + buildString + "\" is not a valid buildString. " +
        "The maximum length is 100 characters.");
  }

  @ParameterizedTest
  @NullAndEmptySource
  void getProjectVersion_is_empty_if_property_is_empty_or_null(@Nullable String projectVersion) {
    settings.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2017-01-01");
    settings.setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, projectVersion);

    underTest.start();

    assertThat(underTest.getProjectVersion()).isEmpty();
  }

  @Test
  void getProjectVersion_contains_value_of_property() {
    String value = RandomStringUtils.secure().nextAlphabetic(10);
    settings.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2017-01-01");
    settings.setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, value);

    underTest.start();

    assertThat(underTest.getProjectVersion()).contains(value);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void getBuildString_is_empty_if_property_is_empty_or_null(@Nullable String buildString) {
    settings.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2017-01-01");
    settings.setProperty(CoreProperties.BUILD_STRING_PROPERTY, buildString);

    underTest.start();

    assertThat(underTest.getBuildString()).isEmpty();
  }

  @Test
  void getBuildString_contains_value_of_property() {
    String value = RandomStringUtils.secure().nextAlphabetic(10);
    settings.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2017-01-01");
    settings.setProperty(CoreProperties.BUILD_STRING_PROPERTY, value);

    underTest.start();

    assertThat(underTest.getBuildString()).contains(value);
  }
}
