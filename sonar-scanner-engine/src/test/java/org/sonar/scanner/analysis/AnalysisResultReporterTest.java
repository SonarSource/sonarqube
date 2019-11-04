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
package org.sonar.scanner.analysis;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.scanner.bootstrap.GlobalAnalysisMode;
import org.sonar.scanner.report.CeTaskReportDataHolder;
import org.sonar.scanner.scan.ScanProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AnalysisResultReporterTest {
  @Rule
  public LogTester logTester = new LogTester();

  private CeTaskReportDataHolder ceTaskReportDataHolder = mock(CeTaskReportDataHolder.class);
  private GlobalAnalysisMode analysisMode = mock(GlobalAnalysisMode.class);
  private ScanProperties scanProperties = mock(ScanProperties.class);

  private AnalysisResultReporter underTest = new AnalysisResultReporter(analysisMode, ceTaskReportDataHolder, scanProperties);

  @Test
  public void should_log_simple_success_message() {
    when(analysisMode.isMediumTest()).thenReturn(true);
    underTest.report();

    assertThat(logTester.logs(LoggerLevel.INFO))
      .containsOnly("ANALYSIS SUCCESSFUL");
  }

  @Test
  public void should_log_success_message_with_urls() {
    String ceTaskUrl = "http://sonarqube/taskurl";
    String dashboardUrl = "http://sonarqube/dashboardurl";
    when(ceTaskReportDataHolder.getCeTaskUrl()).thenReturn(ceTaskUrl);
    when(ceTaskReportDataHolder.getDashboardUrl()).thenReturn(dashboardUrl);

    underTest.report();

    assertThat(logTester.logs(LoggerLevel.INFO))
      .containsExactly(
        "ANALYSIS SUCCESSFUL, you can browse " + dashboardUrl,
        "Note that you will be able to access the updated dashboard once the server has processed the submitted analysis report",
        "More about the report processing at " + ceTaskUrl);
  }

  @Test
  public void should_log_short_success_message_with_urls_if_quality_gate_wait_enabled() {
    String ceTaskUrl = "http://sonarqube/taskurl";
    String dashboardUrl = "http://sonarqube/dashboardurl";
    when(ceTaskReportDataHolder.getCeTaskUrl()).thenReturn(ceTaskUrl);
    when(ceTaskReportDataHolder.getDashboardUrl()).thenReturn(dashboardUrl);

    when(scanProperties.shouldWaitForQualityGate()).thenReturn(true);

    underTest.report();

    assertThat(logTester.logs(LoggerLevel.INFO))
      .containsExactly("ANALYSIS SUCCESSFUL, you can browse " + dashboardUrl);
  }
}
