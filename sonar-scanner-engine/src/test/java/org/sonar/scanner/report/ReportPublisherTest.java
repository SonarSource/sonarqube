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
package org.sonar.scanner.report;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.scanner.bootstrap.GlobalAnalysisMode;
import org.sonar.scanner.bootstrap.DefaultScannerWsClient;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.scanner.fs.InputModuleHierarchy;
import org.sonar.scanner.scan.ScanProperties;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonarqube.ws.Ce;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsRequest;
import org.sonarqube.ws.client.WsResponse;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.scanner.scan.branch.BranchType.LONG;
import static org.sonar.scanner.scan.branch.BranchType.PULL_REQUEST;
import static org.sonar.scanner.scan.branch.BranchType.SHORT;

public class ReportPublisherTest {

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  GlobalAnalysisMode mode = mock(GlobalAnalysisMode.class);
  ScanProperties properties = mock(ScanProperties.class);
  DefaultScannerWsClient wsClient = mock(DefaultScannerWsClient.class, Mockito.RETURNS_DEEP_STUBS);
  Server server = mock(Server.class);
  InputModuleHierarchy moduleHierarchy = mock(InputModuleHierarchy.class);
  DefaultInputModule root;
  AnalysisContextReportPublisher contextPublisher = mock(AnalysisContextReportPublisher.class);
  BranchConfiguration branchConfiguration = mock(BranchConfiguration.class);
  ReportPublisher underTest = new ReportPublisher(properties, wsClient, server, contextPublisher, moduleHierarchy, mode, mock(TempFolder.class),
    new ReportPublisherStep[0], branchConfiguration);

  @Before
  public void setUp() throws IOException {
    root = new DefaultInputModule(ProjectDefinition.create().setKey("org.sonarsource.sonarqube:sonarqube").setBaseDir(temp.newFolder()).setWorkDir(temp.getRoot()));
    when(moduleHierarchy.root()).thenReturn(root);
    when(server.getPublicRootUrl()).thenReturn("https://localhost");
    when(server.getVersion()).thenReturn("6.4");
    when(properties.metadataFilePath()).thenReturn(temp.newFolder().toPath()
      .resolve("folder")
      .resolve("report-task.txt"));
  }

  @Test
  public void log_and_dump_information_about_report_uploading() throws IOException {
    when(properties.organizationKey()).thenReturn(Optional.of("MyOrg"));
    underTest.logSuccess("TASK-123");

    assertThat(logTester.logs(LoggerLevel.INFO))
      .contains("ANALYSIS SUCCESSFUL, you can browse https://localhost/dashboard?id=org.sonarsource.sonarqube%3Asonarqube")
      .contains("Note that you will be able to access the updated dashboard once the server has processed the submitted analysis report")
      .contains("More about the report processing at https://localhost/api/ce/task?id=TASK-123");

    assertThat(readFileToString(properties.metadataFilePath().toFile(), StandardCharsets.UTF_8)).isEqualTo(
      "organization=MyOrg\n" +
        "projectKey=org.sonarsource.sonarqube:sonarqube\n" +
        "serverUrl=https://localhost\n" +
        "serverVersion=6.4\n" +
        "dashboardUrl=https://localhost/dashboard?id=org.sonarsource.sonarqube%3Asonarqube\n" +
        "ceTaskId=TASK-123\n" +
        "ceTaskUrl=https://localhost/api/ce/task?id=TASK-123\n");
  }

  @Test
  public void parse_upload_error_message() throws IOException {
    HttpException ex = new HttpException("url", 404, "{\"errors\":[{\"msg\":\"Organization with key 'MyOrg' does not exist\"}]}");
    WsResponse response = mock(WsResponse.class);
    when(response.failIfNotSuccessful()).thenThrow(ex);
    when(wsClient.call(any(WsRequest.class))).thenReturn(response);

    exception.expect(MessageException.class);
    exception.expectMessage("Failed to upload report - Organization with key 'MyOrg' does not exist");
    underTest.upload(temp.newFile());
  }

  @Test
  public void log_public_url_if_defined_for_main_branch() throws IOException {
    when(server.getPublicRootUrl()).thenReturn("https://publicserver/sonarqube");

    underTest.logSuccess("TASK-123");

    assertThat(logTester.logs(LoggerLevel.INFO))
      .contains("ANALYSIS SUCCESSFUL, you can browse https://publicserver/sonarqube/dashboard?id=org.sonarsource.sonarqube%3Asonarqube")
      .contains("More about the report processing at https://publicserver/sonarqube/api/ce/task?id=TASK-123");

    assertThat(readFileToString(properties.metadataFilePath().toFile(), StandardCharsets.UTF_8)).isEqualTo(
      "projectKey=org.sonarsource.sonarqube:sonarqube\n" +
        "serverUrl=https://publicserver/sonarqube\n" +
        "serverVersion=6.4\n" +
        "dashboardUrl=https://publicserver/sonarqube/dashboard?id=org.sonarsource.sonarqube%3Asonarqube\n" +
        "ceTaskId=TASK-123\n" +
        "ceTaskUrl=https://publicserver/sonarqube/api/ce/task?id=TASK-123\n");
  }

  @Test
  public void log_public_url_if_defined_for_long_living_branches() throws IOException {
    when(server.getPublicRootUrl()).thenReturn("https://publicserver/sonarqube");
    when(branchConfiguration.branchType()).thenReturn(LONG);
    when(branchConfiguration.branchName()).thenReturn("branch-6.7");
    ReportPublisher underTest = new ReportPublisher(properties, wsClient, server, contextPublisher, moduleHierarchy, mode, mock(TempFolder.class),
      new ReportPublisherStep[0], branchConfiguration);

    underTest.logSuccess("TASK-123");
    assertThat(logTester.logs(LoggerLevel.INFO))
      .contains("ANALYSIS SUCCESSFUL, you can browse https://publicserver/sonarqube/dashboard?id=org.sonarsource.sonarqube%3Asonarqube&branch=branch-6.7")
      .contains("More about the report processing at https://publicserver/sonarqube/api/ce/task?id=TASK-123");

    assertThat(readFileToString(properties.metadataFilePath().toFile(), StandardCharsets.UTF_8)).isEqualTo(
      "projectKey=org.sonarsource.sonarqube:sonarqube\n" +
        "serverUrl=https://publicserver/sonarqube\n" +
        "serverVersion=6.4\n" +
        "dashboardUrl=https://publicserver/sonarqube/dashboard?id=org.sonarsource.sonarqube%3Asonarqube&branch=branch-6.7\n" +
        "ceTaskId=TASK-123\n" +
        "ceTaskUrl=https://publicserver/sonarqube/api/ce/task?id=TASK-123\n");
  }

  @Test
  public void log_public_url_if_defined_for_short_living_branches() throws IOException {
    when(server.getPublicRootUrl()).thenReturn("https://publicserver/sonarqube");
    when(branchConfiguration.branchType()).thenReturn(SHORT);
    when(branchConfiguration.branchName()).thenReturn("branch-6.7");
    ReportPublisher underTest = new ReportPublisher(properties, wsClient, server, contextPublisher, moduleHierarchy, mode, mock(TempFolder.class),
      new ReportPublisherStep[0], branchConfiguration);

    underTest.logSuccess("TASK-123");
    assertThat(logTester.logs(LoggerLevel.INFO))
      .contains("ANALYSIS SUCCESSFUL, you can browse https://publicserver/sonarqube/dashboard?id=org.sonarsource.sonarqube%3Asonarqube&branch=branch-6.7&resolved=false")
      .contains("More about the report processing at https://publicserver/sonarqube/api/ce/task?id=TASK-123");

    assertThat(readFileToString(properties.metadataFilePath().toFile(), StandardCharsets.UTF_8)).isEqualTo(
      "projectKey=org.sonarsource.sonarqube:sonarqube\n" +
        "serverUrl=https://publicserver/sonarqube\n" +
        "serverVersion=6.4\n" +
        "dashboardUrl=https://publicserver/sonarqube/dashboard?id=org.sonarsource.sonarqube%3Asonarqube&branch=branch-6.7&resolved=false\n" +
        "ceTaskId=TASK-123\n" +
        "ceTaskUrl=https://publicserver/sonarqube/api/ce/task?id=TASK-123\n");
  }

  @Test
  public void log_public_url_if_defined_for_pull_request() throws IOException {
    when(server.getPublicRootUrl()).thenReturn("https://publicserver/sonarqube");
    when(branchConfiguration.branchName()).thenReturn("Bitbucket cloud Widget");
    when(branchConfiguration.branchType()).thenReturn(PULL_REQUEST);
    when(branchConfiguration.pullRequestKey()).thenReturn("105");

    ReportPublisher underTest = new ReportPublisher(properties, wsClient, server, contextPublisher, moduleHierarchy, mode, mock(TempFolder.class),
      new ReportPublisherStep[0], branchConfiguration);

    underTest.logSuccess("TASK-123");

    assertThat(logTester.logs(LoggerLevel.INFO))
      .contains("ANALYSIS SUCCESSFUL, you can browse https://publicserver/sonarqube/dashboard?id=org.sonarsource.sonarqube%3Asonarqube&pullRequest=105")
      .contains("More about the report processing at https://publicserver/sonarqube/api/ce/task?id=TASK-123");

    assertThat(readFileToString(properties.metadataFilePath().toFile(), StandardCharsets.UTF_8)).isEqualTo(
      "projectKey=org.sonarsource.sonarqube:sonarqube\n" +
        "serverUrl=https://publicserver/sonarqube\n" +
        "serverVersion=6.4\n" +
        "dashboardUrl=https://publicserver/sonarqube/dashboard?id=org.sonarsource.sonarqube%3Asonarqube&pullRequest=105\n" +
        "ceTaskId=TASK-123\n" +
        "ceTaskUrl=https://publicserver/sonarqube/api/ce/task?id=TASK-123\n");
  }

  @Test
  public void fail_if_public_url_malformed() {
    when(server.getPublicRootUrl()).thenReturn("invalid");

    exception.expect(MessageException.class);
    exception.expectMessage("Failed to parse public URL set in SonarQube server: invalid");
    underTest.start();
  }

  @Test
  public void log_but_not_dump_information_when_report_is_not_uploaded() throws IOException {
    underTest.logSuccess(/* report not uploaded, no server task */null);

    assertThat(logTester.logs(LoggerLevel.INFO))
      .contains("ANALYSIS SUCCESSFUL")
      .doesNotContain("dashboard/index");

    assertThat(properties.metadataFilePath()).doesNotExist();
  }

  @Test
  public void log_and_dump_information_to_custom_path() throws IOException {
    underTest.logSuccess("TASK-123");

    assertThat(properties.metadataFilePath()).exists();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("Report metadata written to " + properties.metadataFilePath());
  }

  @Test
  public void should_not_delete_report_if_property_is_set() throws IOException {
    when(properties.shouldKeepReport()).thenReturn(true);
    Path reportDir = temp.getRoot().toPath().resolve("scanner-report");
    Files.createDirectory(reportDir);

    underTest.start();
    underTest.stop();
    assertThat(reportDir).isDirectory();
  }

  @Test
  public void should_delete_report_by_default() throws IOException {
    Path reportDir = temp.getRoot().toPath().resolve("scanner-report");
    Files.createDirectory(reportDir);

    underTest.start();
    underTest.stop();
    assertThat(reportDir).doesNotExist();
  }

  @Test
  public void test_ws_parameters() throws Exception {
    when(properties.organizationKey()).thenReturn(Optional.of("MyOrg"));

    WsResponse response = mock(WsResponse.class);

    PipedOutputStream out = new PipedOutputStream();
    PipedInputStream in = new PipedInputStream(out);
    Ce.SubmitResponse.newBuilder().build().writeTo(out);
    out.close();

    when(response.failIfNotSuccessful()).thenReturn(response);
    when(response.contentStream()).thenReturn(in);

    when(wsClient.call(any(WsRequest.class))).thenReturn(response);
    underTest.upload(temp.newFile());

    ArgumentCaptor<WsRequest> capture = ArgumentCaptor.forClass(WsRequest.class);
    verify(wsClient).call(capture.capture());

    WsRequest wsRequest = capture.getValue();
    assertThat(wsRequest.getParameters().getKeys()).containsOnly("organization", "projectKey");
    assertThat(wsRequest.getParameters().getValue("organization")).isEqualTo("MyOrg");
    assertThat(wsRequest.getParameters().getValue("projectKey")).isEqualTo("org.sonarsource.sonarqube:sonarqube");
  }

  @Test
  public void test_send_branches_characteristics() throws Exception {
    String orgName = "MyOrg";
    when(properties.organizationKey()).thenReturn(Optional.of(orgName));

    String branchName = "feature";
    when(branchConfiguration.branchName()).thenReturn(branchName);
    when(branchConfiguration.branchType()).thenReturn(SHORT);

    WsResponse response = mock(WsResponse.class);

    PipedOutputStream out = new PipedOutputStream();
    PipedInputStream in = new PipedInputStream(out);
    Ce.SubmitResponse.newBuilder().build().writeTo(out);
    out.close();

    when(response.failIfNotSuccessful()).thenReturn(response);
    when(response.contentStream()).thenReturn(in);

    when(wsClient.call(any(WsRequest.class))).thenReturn(response);
    underTest.upload(temp.newFile());

    ArgumentCaptor<WsRequest> capture = ArgumentCaptor.forClass(WsRequest.class);
    verify(wsClient).call(capture.capture());

    WsRequest wsRequest = capture.getValue();
    assertThat(wsRequest.getParameters().getKeys()).hasSize(3);
    assertThat(wsRequest.getParameters().getValues("organization")).containsExactly(orgName);
    assertThat(wsRequest.getParameters().getValues("projectKey")).containsExactly("org.sonarsource.sonarqube:sonarqube");
    assertThat(wsRequest.getParameters().getValues("characteristic"))
      .containsExactlyInAnyOrder("branch=" + branchName, "branchType=" + SHORT.name());
  }

  @Test
  public void send_pull_request_characteristic() throws Exception {
    String orgName = "MyOrg";
    when(properties.organizationKey()).thenReturn(Optional.of(orgName));

    String branchName = "feature";
    String pullRequestId = "pr-123";
    when(branchConfiguration.branchName()).thenReturn(branchName);
    when(branchConfiguration.branchType()).thenReturn(PULL_REQUEST);
    when(branchConfiguration.pullRequestKey()).thenReturn(pullRequestId);

    WsResponse response = mock(WsResponse.class);

    PipedOutputStream out = new PipedOutputStream();
    PipedInputStream in = new PipedInputStream(out);
    Ce.SubmitResponse.newBuilder().build().writeTo(out);
    out.close();

    when(response.failIfNotSuccessful()).thenReturn(response);
    when(response.contentStream()).thenReturn(in);

    when(wsClient.call(any(WsRequest.class))).thenReturn(response);
    underTest.upload(temp.newFile());

    ArgumentCaptor<WsRequest> capture = ArgumentCaptor.forClass(WsRequest.class);
    verify(wsClient).call(capture.capture());

    WsRequest wsRequest = capture.getValue();
    assertThat(wsRequest.getParameters().getKeys()).hasSize(3);
    assertThat(wsRequest.getParameters().getValues("organization")).containsExactly(orgName);
    assertThat(wsRequest.getParameters().getValues("projectKey")).containsExactly("org.sonarsource.sonarqube:sonarqube");
    assertThat(wsRequest.getParameters().getValues("characteristic"))
      .containsExactlyInAnyOrder("pullRequest=" + pullRequestId);
  }

}
