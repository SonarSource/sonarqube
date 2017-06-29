/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ProjectAnalysisInfoTest {
  @Test
  public void testSimpleDate() {
    MapSettings settings = new MapSettings();
    settings.appendProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2017-01-01");
    settings.appendProperty(CoreProperties.PROJECT_VERSION_PROPERTY, "version");
    System2 system = mock(System2.class);
    ProjectAnalysisInfo info = new ProjectAnalysisInfo(settings.asConfig(), system);
    info.start();
    LocalDate date = LocalDate.of(2017, 1, 1);

    assertThat(info.analysisDate()).isEqualTo(Date.from(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
    assertThat(info.analysisVersion()).isEqualTo("version");
  }
}
