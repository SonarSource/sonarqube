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
package org.sonar.scanner.source;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ZeroCoverageSensorTest {

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void dontForceCoverageInIncrementalMode() {
    AnalysisMode analysisMode = mock(AnalysisMode.class);
    when(analysisMode.isIncremental()).thenReturn(true);
    ZeroCoverageSensor zeroCoverageSensor = new ZeroCoverageSensor(null, analysisMode);
    zeroCoverageSensor.execute(null);
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("Incremental mode: not forcing coverage to zero");
  }

}
