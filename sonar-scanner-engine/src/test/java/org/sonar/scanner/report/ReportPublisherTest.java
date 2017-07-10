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
package org.sonar.scanner.report;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.scanner.analysis.DefaultAnalysisMode;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonarqube.ws.WsCe;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsRequest;
import org.sonarqube.ws.client.WsResponse;

public class ReportPublisherTest {

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  DefaultAnalysisMode mode = mock(DefaultAnalysisMode.class);
  MapSettings settings = new MapSettings(new PropertyDefinitions(CorePropertyDefinitions.all()));
  ScannerWsClient wsClient;
  Server server = mock(Server.class);
  InputModuleHierarchy moduleHierarchy = mock(InputModuleHierarchy.class);
  DefaultInputModule root;
  AnalysisContextReportPublisher contextPublisher = mock(AnalysisContextReportPublisher.class);

  @Before
  public void setUp() {
    wsClient = mock(ScannerWsClient.class, Mockito.RETURNS_DEEP_STUBS);
    root = new DefaultInputModule(ProjectDefinition.create().setKey("struts").setWorkDir(temp.getRoot()));
    when(moduleHierarchy.root()).thenReturn(root);
    when(server.getPublicRootUrl()).thenReturn("https://localhost");
    when(server.getVersion()).thenReturn("6.4");
  }

  @Test
  public void log_and_dump_information_about_report_uploading() throws IOException {
    ReportPublisher underTest = new ReportPublisher(settings.asConfig(), wsClient, server, contextPublisher, moduleHierarchy, mode, mock(TempFolder.class),
      new ReportPublisherStep[0]);
    settings.setProperty(CoreProperties.PROJECT_ORGANIZATION_PROPERTY, "MyOrg");

    underTest.logSuccess("TASK-123");

    assertThat(logTester.logs(LoggerLevel.INFO))
      .contains("ANALYSIS SUCCESSFUL, you can browse https://localhost/dashboard/index/struts")
      .contains("Note that you will be able to access the updated dashboard once the server has processed the submitted analysis report")
      .contains("More about the report processing at https://localhost/api/ce/task?id=TASK-123");

    File detailsFile = new File(temp.getRoot(), "report-task.txt");
    assertThat(readFileToString(detailsFile)).isEqualTo(
      "organization=MyOrg\n" +
        "projectKey=struts\n" +
        "serverUrl=https://localhost\n" +
        "serverVersion=6.4\n" +
        "dashboardUrl=https://localhost/dashboard/index/struts\n" +
        "ceTaskId=TASK-123\n" +
        "ceTaskUrl=https://localhost/api/ce/task?id=TASK-123\n");
  }

  @Test
  public void parse_upload_error_message() throws IOException {
    ReportPublisher underTest = new ReportPublisher(settings.asConfig(), wsClient, server, contextPublisher, moduleHierarchy, mode, mock(TempFolder.class),
      new ReportPublisherStep[0]);
    HttpException ex = new HttpException("url", 404, "{\"errors\":[{\"msg\":\"Organization with key 'MyOrg' does not exist\"}]}");
    WsResponse response = mock(WsResponse.class);
    when(response.failIfNotSuccessful()).thenThrow(ex);
    when(wsClient.call(any(WsRequest.class))).thenReturn(response);

    exception.expect(MessageException.class);
    exception.expectMessage("Failed to upload report - 404: Organization with key 'MyOrg' does not exist");
    underTest.upload(temp.newFile());
  }

  @Test
  public void log_public_url_if_defined() throws IOException {
    when(server.getPublicRootUrl()).thenReturn("https://publicserver/sonarqube");
    ReportPublisher underTest = new ReportPublisher(settings.asConfig(), wsClient, server, contextPublisher, moduleHierarchy, mode, mock(TempFolder.class),
      new ReportPublisherStep[0]);

    underTest.logSuccess("TASK-123");

    assertThat(logTester.logs(LoggerLevel.INFO))
      .contains("ANALYSIS SUCCESSFUL, you can browse https://publicserver/sonarqube/dashboard/index/struts")
      .contains("More about the report processing at https://publicserver/sonarqube/api/ce/task?id=TASK-123");

    File detailsFile = new File(temp.getRoot(), "report-task.txt");
    assertThat(readFileToString(detailsFile)).isEqualTo(
      "projectKey=struts\n" +
        "serverUrl=https://publicserver/sonarqube\n" +
        "serverVersion=6.4\n" +
        "dashboardUrl=https://publicserver/sonarqube/dashboard/index/struts\n" +
        "ceTaskId=TASK-123\n" +
        "ceTaskUrl=https://publicserver/sonarqube/api/ce/task?id=TASK-123\n");
  }

  @Test
  public void fail_if_public_url_malformed() throws IOException {
    when(server.getPublicRootUrl()).thenReturn("invalid");
    ReportPublisher underTest = new ReportPublisher(settings.asConfig(), wsClient, server, contextPublisher, moduleHierarchy, mode, mock(TempFolder.class),
      new ReportPublisherStep[0]);

    exception.expect(MessageException.class);
    exception.expectMessage("Failed to parse public URL set in SonarQube server: invalid");
    underTest.start();
  }

  @Test
  public void log_but_not_dump_information_when_report_is_not_uploaded() {
    ReportPublisher underTest = new ReportPublisher(settings.asConfig(), wsClient, server, contextPublisher, moduleHierarchy, mode, mock(TempFolder.class),
      new ReportPublisherStep[0]);

    underTest.logSuccess(/* report not uploaded, no server task */null);

    assertThat(logTester.logs(LoggerLevel.INFO))
      .contains("ANALYSIS SUCCESSFUL")
      .doesNotContain("dashboard/index");

    File detailsFile = new File(temp.getRoot(), ReportPublisher.METADATA_DUMP_FILENAME);
    assertThat(detailsFile).doesNotExist();
  }

  @Test
  public void should_not_delete_report_if_property_is_set() throws IOException {
    settings.setProperty("sonar.batch.keepReport", true);
    Path reportDir = temp.getRoot().toPath().resolve("batch-report");
    Files.createDirectory(reportDir);
    ReportPublisher underTest = new ReportPublisher(settings.asConfig(), wsClient, server, contextPublisher, moduleHierarchy, mode, mock(TempFolder.class),
      new ReportPublisherStep[0]);

    underTest.start();
    underTest.stop();
    assertThat(reportDir).isDirectory();
  }

  @Test
  public void should_delete_report_by_default() throws IOException {
    Path reportDir = temp.getRoot().toPath().resolve("batch-report");
    Files.createDirectory(reportDir);
    ReportPublisher job = new ReportPublisher(settings.asConfig(), wsClient, server, contextPublisher, moduleHierarchy, mode, mock(TempFolder.class), new ReportPublisherStep[0]);

    job.start();
    job.stop();
    assertThat(reportDir).doesNotExist();
  }

  @Test
  public void test_ws_parameters() throws Exception {
    ReportPublisher underTest = new ReportPublisher(settings.asConfig(), wsClient, server, contextPublisher, moduleHierarchy, mode, mock(TempFolder.class),
      new ReportPublisherStep[0]);

    settings.setProperty(CoreProperties.PROJECT_ORGANIZATION_PROPERTY, "MyOrg");

    WsResponse response = mock(WsResponse.class);

    PipedOutputStream out = new PipedOutputStream();
    PipedInputStream in = new PipedInputStream(out);
    WsCe.SubmitResponse.newBuilder().build().writeTo(out);
    out.close();

    when(response.failIfNotSuccessful()).thenReturn(response);
    when(response.contentStream()).thenReturn(in);

    when(wsClient.call(any(WsRequest.class))).thenReturn(response);
    underTest.upload(temp.newFile());

    ArgumentCaptor<WsRequest> capture = ArgumentCaptor.forClass(WsRequest.class);
    verify(wsClient).call(capture.capture());

    WsRequest wsRequest = capture.getValue();
    assertThat(wsRequest.getParams()).containsOnly(
      entry("organization", "MyOrg"),
      entry("projectKey", "struts"));
  }

}
