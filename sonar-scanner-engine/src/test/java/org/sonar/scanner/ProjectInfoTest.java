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
package org.sonar.scanner;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import javax.annotation.Nullable;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.MessageException;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(DataProviderRunner.class)
public class ProjectInfoTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MapSettings settings = new MapSettings();
  private Clock clock = mock(Clock.class);
  private ProjectInfo underTest = new ProjectInfo(settings.asConfig(), clock);

  @Test
  public void testSimpleDateTime() {
    OffsetDateTime date = OffsetDateTime.of(2017, 1, 1, 12, 13, 14, 0, ZoneOffset.ofHours(2));
    settings.appendProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2017-01-01T12:13:14+0200");
    settings.appendProperty(CoreProperties.PROJECT_VERSION_PROPERTY, "version");

    underTest.start();

    assertThat(underTest.getAnalysisDate()).isEqualTo(Date.from(date.toInstant()));
    assertThat(underTest.getProjectVersion()).contains("version");
  }

  @Test
  public void testSimpleDate() {
    LocalDate date = LocalDate.of(2017, 1, 1);
    settings.appendProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2017-01-01");

    underTest.start();

    assertThat(underTest.getAnalysisDate())
      .isEqualTo(Date.from(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
  }

  @Test
  public void emptyDate() {
    settings.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "");
    settings.setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, "version");

    expectedException.expect(RuntimeException.class);

    underTest.start();
  }

  @Test
  public void fail_with_too_long_version() {
    String version = randomAlphabetic(101);
    settings.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2017-01-01");
    settings.setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, version);

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("\"" + version + "\" is not a valid project version. " +
      "The maximum length is 100 characters.");

    underTest.start();
  }

  @Test
  public void fail_with_too_long_buildString() {
    String buildString = randomAlphabetic(101);
    settings.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2017-01-01");
    settings.setProperty(CoreProperties.BUILD_STRING_PROPERTY, buildString);

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("\"" + buildString + "\" is not a valid buildString. " +
      "The maximum length is 100 characters.");

    underTest.start();
  }

  @Test
  @UseDataProvider("emptyOrNullString")
  public void getProjectVersion_is_empty_if_property_is_empty_or_null(@Nullable String projectVersion) {
    settings.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2017-01-01");
    settings.setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, projectVersion);

    underTest.start();

    assertThat(underTest.getProjectVersion()).isEmpty();
  }

  @Test
  public void getProjectVersion_contains_value_of_property() {
    String value = RandomStringUtils.randomAlphabetic(10);
    settings.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2017-01-01");
    settings.setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, value);

    underTest.start();

    assertThat(underTest.getProjectVersion()).contains(value);
  }

  @Test
  @UseDataProvider("emptyOrNullString")
  public void getBuildString_is_empty_if_property_is_empty_or_null(@Nullable String buildString) {
    settings.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2017-01-01");
    settings.setProperty(CoreProperties.BUILD_STRING_PROPERTY, buildString);

    underTest.start();

    assertThat(underTest.getBuildString()).isEmpty();
  }

  @Test
  public void getBuildString_contains_value_of_property() {
    String value = RandomStringUtils.randomAlphabetic(10);
    settings.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2017-01-01");
    settings.setProperty(CoreProperties.BUILD_STRING_PROPERTY, value);

    underTest.start();

    assertThat(underTest.getBuildString()).contains(value);
  }

  @DataProvider
  public static Object[][] emptyOrNullString() {
    return new Object[][] {
      {""},
      {null},
    };
  }
}
