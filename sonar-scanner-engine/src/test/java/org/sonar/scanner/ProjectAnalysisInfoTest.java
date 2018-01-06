/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ProjectAnalysisInfoTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testSimpleDateTime() {
    MapSettings settings = new MapSettings();
    settings.appendProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2017-01-01T12:13:14+0200");
    settings.appendProperty(CoreProperties.PROJECT_VERSION_PROPERTY, "version");
    Clock clock = mock(Clock.class);
    ProjectAnalysisInfo info = new ProjectAnalysisInfo(settings.asConfig(), clock);
    info.start();
    OffsetDateTime date = OffsetDateTime.of(2017, 1, 1, 12, 13, 14, 0, ZoneOffset.ofHours(2));

    assertThat(info.analysisDate()).isEqualTo(Date.from(date.toInstant()));
    assertThat(info.analysisVersion()).isEqualTo("version");
  }

  @Test
  public void testSimpleDate() {
    MapSettings settings = new MapSettings();
    settings.appendProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2017-01-01");
    Clock clock = mock(Clock.class);
    ProjectAnalysisInfo info = new ProjectAnalysisInfo(settings.asConfig(), clock);
    info.start();
    LocalDate date = LocalDate.of(2017, 1, 1);

    assertThat(info.analysisDate()).isEqualTo(Date.from(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
  }

  @Test
  public void emptyDate() {
    MapSettings settings = new MapSettings();
    settings.appendProperty(CoreProperties.PROJECT_DATE_PROPERTY, "");
    settings.appendProperty(CoreProperties.PROJECT_VERSION_PROPERTY, "version");
    Clock clock = mock(Clock.class);
    ProjectAnalysisInfo info = new ProjectAnalysisInfo(settings.asConfig(), clock);

    thrown.expect(RuntimeException.class);

    info.start();
  }
}
