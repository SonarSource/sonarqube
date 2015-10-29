/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.report;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.TempFolder;
import org.sonar.batch.analysis.DefaultAnalysisMode;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.batch.scan.ImmutableProjectReactor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ReportPublisherTest {

  private DefaultAnalysisMode mode;

  private ImmutableProjectReactor reactor;

  private Settings settings;

  private ProjectDefinition root;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setUp() {
    settings = new Settings();
    mode = mock(DefaultAnalysisMode.class);
    reactor = mock(ImmutableProjectReactor.class);
    root = ProjectDefinition.create().setKey("struts").setWorkDir(temp.getRoot());
    when(reactor.getRoot()).thenReturn(root);
  }

  @Test
  public void should_log_successful_analysis() {
    settings.setProperty(CoreProperties.SERVER_BASE_URL, "http://myserver/");
    ReportPublisher job = new ReportPublisher(settings, mock(ServerClient.class), mock(Server.class), mock(AnalysisContextReportPublisher.class), reactor, mode,
      mock(TempFolder.class), new ReportPublisherStep[0]);

    Logger logger = mock(Logger.class);
    job.logSuccess(logger, null);

    verify(logger).info("ANALYSIS SUCCESSFUL, you can browse {}", "http://myserver/dashboard/index/struts");
    verify(logger).info("Note that you will be able to access the updated dashboard once the server has processed the submitted analysis report.");
    verifyNoMoreInteractions(logger);
  }

  @Test
  public void should_log_successful_analysis_with_ce_task() {
    settings.setProperty(CoreProperties.SERVER_BASE_URL, "http://myserver/");
    ReportPublisher job = new ReportPublisher(settings, mock(ServerClient.class), mock(Server.class), mock(AnalysisContextReportPublisher.class), reactor, mode,
      mock(TempFolder.class), new ReportPublisherStep[0]);

    Logger logger = mock(Logger.class);
    job.logSuccess(logger, "abc123");

    verify(logger).info("ANALYSIS SUCCESSFUL, you can browse {}", "http://myserver/dashboard/index/struts");
    verify(logger).info("Note that you will be able to access the updated dashboard once the server has processed the submitted analysis report.");
    verify(logger).info("More about the report processing at {}", "http://myserver/api/ce/task?id=abc123");
    verifyNoMoreInteractions(logger);
  }

  @Test
  public void should_log_successful_issues_analysis() {
    when(mode.isIssues()).thenReturn(true);
    ReportPublisher job = new ReportPublisher(settings, mock(ServerClient.class), mock(Server.class), mock(AnalysisContextReportPublisher.class), reactor, mode,
      mock(TempFolder.class), new ReportPublisherStep[0]);

    Logger logger = mock(Logger.class);
    job.logSuccess(logger, null);

    verify(logger).info("ANALYSIS SUCCESSFUL");
    verifyNoMoreInteractions(logger);
  }

  @Test
  public void should_not_delete_report() throws IOException {
    settings.setProperty("sonar.verbose", true);
    Path reportDir = temp.getRoot().toPath().resolve("batch-report");
    Files.createDirectory(reportDir);
    ReportPublisher job = new ReportPublisher(settings, mock(ServerClient.class), mock(Server.class), mock(AnalysisContextReportPublisher.class), reactor, mode,
      mock(TempFolder.class), new ReportPublisherStep[0]);

    job.start();
    job.stop();
    assertThat(reportDir).isDirectory();
  }

  @Test
  public void should_delete_report() throws IOException {
    Path reportDir = temp.getRoot().toPath().resolve("batch-report");
    Files.createDirectory(reportDir);
    ReportPublisher job = new ReportPublisher(settings, mock(ServerClient.class), mock(Server.class), mock(AnalysisContextReportPublisher.class), reactor, mode,
      mock(TempFolder.class), new ReportPublisherStep[0]);

    job.start();
    job.stop();
    assertThat(reportDir).doesNotExist();
  }

}
