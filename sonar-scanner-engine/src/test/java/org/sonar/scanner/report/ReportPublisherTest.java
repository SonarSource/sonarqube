/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.event.Level;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.impl.utils.JUnitTempFolder;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.platform.Server;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.TempFolder;
import org.sonar.scanner.bootstrap.DefaultScannerWsClient;
import org.sonar.scanner.bootstrap.GlobalAnalysisMode;
import org.sonar.scanner.ci.CiConfiguration;
import org.sonar.scanner.ci.DevOpsPlatformInfo;
import org.sonar.scanner.fs.InputModuleHierarchy;
import org.sonar.scanner.protocol.output.FileStructure;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.scan.ScanProperties;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonarqube.ws.Ce;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.MockWsResponse;
import org.sonarqube.ws.client.WsRequest;
import org.sonarqube.ws.client.WsResponse;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.scanner.report.ReportPublisher.SUPPORT_OF_32_BIT_JRE_IS_DEPRECATED_MESSAGE;
import static org.sonar.scanner.scan.branch.BranchType.BRANCH;
import static org.sonar.scanner.scan.branch.BranchType.PULL_REQUEST;

public class ReportPublisherTest {

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public JUnitTempFolder reportTempFolder = new JUnitTempFolder();

  private GlobalAnalysisMode mode = mock(GlobalAnalysisMode.class);
  private ScanProperties properties = mock(ScanProperties.class);
  private DefaultScannerWsClient wsClient = mock(DefaultScannerWsClient.class, Mockito.RETURNS_DEEP_STUBS);
  private Server server = mock(Server.class);
  private InputModuleHierarchy moduleHierarchy = mock(InputModuleHierarchy.class);
  private DefaultInputModule root;
  private AnalysisContextReportPublisher contextPublisher = mock(AnalysisContextReportPublisher.class);
  private BranchConfiguration branchConfiguration = mock(BranchConfiguration.class);
  private CeTaskReportDataHolder reportMetadataHolder = mock(CeTaskReportDataHolder.class);
  private CiConfiguration ciConfiguration = mock(CiConfiguration.class);
  private ReportPublisher underTest;
  private AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);
  private FileStructure fileStructure;
  private JavaArchitectureInformationProvider javaArchitectureInformationProvider = mock(JavaArchitectureInformationProvider.class);

  @Before
  public void setUp() {
    logTester.setLevel(Level.DEBUG);
    root = new DefaultInputModule(
      ProjectDefinition.create().setKey("org.sonarsource.sonarqube:sonarqube").setBaseDir(reportTempFolder.newDir()).setWorkDir(reportTempFolder.getRoot()));
    when(moduleHierarchy.root()).thenReturn(root);
    fileStructure = new FileStructure(reportTempFolder.getRoot());
    when(server.getPublicRootUrl()).thenReturn("https://localhost");
    when(server.getVersion()).thenReturn("6.4");
    when(properties.metadataFilePath()).thenReturn(reportTempFolder.newDir().toPath()
      .resolve("folder")
      .resolve("report-task.txt"));
    underTest = new ReportPublisher(properties, wsClient, server, contextPublisher, moduleHierarchy, mode, reportTempFolder,
      new ReportPublisherStep[0], branchConfiguration, reportMetadataHolder, analysisWarnings, javaArchitectureInformationProvider, fileStructure, ciConfiguration);
  }

  @Test
  public void checks_if_component_has_issues() {
    underTest.getWriter().writeComponentIssues(1, List.of(ScannerReport.Issue.newBuilder().build()));

    assertThat(underTest.getReader().hasIssues(1)).isTrue();
    assertThat(underTest.getReader().hasIssues(2)).isFalse();
  }

  @Test
  public void use_write_timeout_from_properties() {
    when(properties.reportPublishTimeout()).thenReturn(60);

    MockWsResponse submitMockResponse = new MockWsResponse();
    submitMockResponse.setContent(Ce.SubmitResponse.newBuilder().setTaskId("task-1234").build().toByteArray());
    when(wsClient.call(any())).thenReturn(submitMockResponse);

    underTest.start();
    underTest.execute();

    verify(wsClient).call(argThat(req -> (req).getWriteTimeOutInMs().orElse(0) == 60_000));
  }

  @Test
  public void should_not_log_success_when_should_wait_for_QG() {
    when(properties.shouldWaitForQualityGate()).thenReturn(true);

    MockWsResponse submitMockResponse = new MockWsResponse();
    submitMockResponse.setContent(Ce.SubmitResponse.newBuilder().setTaskId("task-1234").build().toByteArray());
    when(wsClient.call(any())).thenReturn(submitMockResponse);

    underTest.start();
    underTest.execute();

    assertThat(logTester.logs()).noneMatch(s -> s.contains("ANALYSIS SUCCESSFUL"));
  }

  @Test
  public void dump_information_about_report_uploading() throws IOException {
    underTest.prepareAndDumpMetadata("TASK-123");

    assertThat(readFileToString(properties.metadataFilePath().toFile(), StandardCharsets.UTF_8)).isEqualTo(
      "projectKey=org.sonarsource.sonarqube:sonarqube\n" +
        "serverUrl=https://localhost\n" +
        "serverVersion=6.4\n" +
        "dashboardUrl=https://localhost/dashboard?id=org.sonarsource.sonarqube%3Asonarqube\n" +
        "ceTaskId=TASK-123\n" +
        "ceTaskUrl=https://localhost/api/ce/task?id=TASK-123\n");
  }

  @Test
  public void upload_error_message() {
    HttpException ex = new HttpException("url", 404, "{\"errors\":[{\"msg\":\"Organization with key 'MyOrg' does not exist\"}]}");
    WsResponse response = mock(WsResponse.class);
    when(response.failIfNotSuccessful()).thenThrow(ex);
    when(wsClient.call(any(WsRequest.class))).thenThrow(new IllegalStateException("timeout"));

    assertThatThrownBy(() -> underTest.upload(reportTempFolder.newFile()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to upload report: timeout");
  }

  @Test
  public void parse_upload_error_message() {
    HttpException ex = new HttpException("url", 404, "{\"errors\":[{\"msg\":\"Organization with key 'MyOrg' does not exist\"}]}");
    WsResponse response = mock(WsResponse.class);
    when(response.failIfNotSuccessful()).thenThrow(ex);
    when(wsClient.call(any(WsRequest.class))).thenReturn(response);

    assertThatThrownBy(() -> underTest.upload(reportTempFolder.newFile()))
      .isInstanceOf(MessageException.class)
      .hasMessage("Server failed to process report. Please check server logs: Organization with key 'MyOrg' does not exist");
  }

  @Test
  public void dump_public_url_if_defined_for_main_branch() throws IOException {
    when(server.getPublicRootUrl()).thenReturn("https://publicserver/sonarqube");

    underTest.prepareAndDumpMetadata("TASK-123");

    assertThat(readFileToString(properties.metadataFilePath().toFile(), StandardCharsets.UTF_8)).isEqualTo(
      "projectKey=org.sonarsource.sonarqube:sonarqube\n" +
        "serverUrl=https://publicserver/sonarqube\n" +
        "serverVersion=6.4\n" +
        "dashboardUrl=https://publicserver/sonarqube/dashboard?id=org.sonarsource.sonarqube%3Asonarqube\n" +
        "ceTaskId=TASK-123\n" +
        "ceTaskUrl=https://publicserver/sonarqube/api/ce/task?id=TASK-123\n");
  }

  @Test
  public void dump_public_url_if_defined_for_branches() throws IOException {
    when(server.getPublicRootUrl()).thenReturn("https://publicserver/sonarqube");
    when(branchConfiguration.branchType()).thenReturn(BRANCH);
    when(branchConfiguration.branchName()).thenReturn("branch-6.7");
    ReportPublisher underTest = new ReportPublisher(properties, wsClient, server, contextPublisher, moduleHierarchy, mode, mock(TempFolder.class),
      new ReportPublisherStep[0], branchConfiguration, reportMetadataHolder, analysisWarnings, javaArchitectureInformationProvider, fileStructure, ciConfiguration);

    underTest.prepareAndDumpMetadata("TASK-123");

    assertThat(readFileToString(properties.metadataFilePath().toFile(), StandardCharsets.UTF_8)).isEqualTo(
      "projectKey=org.sonarsource.sonarqube:sonarqube\n" +
        "serverUrl=https://publicserver/sonarqube\n" +
        "serverVersion=6.4\n" +
        "dashboardUrl=https://publicserver/sonarqube/dashboard?id=org.sonarsource.sonarqube%3Asonarqube&branch=branch-6.7\n" +
        "ceTaskId=TASK-123\n" +
        "ceTaskUrl=https://publicserver/sonarqube/api/ce/task?id=TASK-123\n");
  }

  @Test
  public void dump_public_url_if_defined_for_pull_request() throws IOException {
    when(server.getPublicRootUrl()).thenReturn("https://publicserver/sonarqube");
    when(branchConfiguration.branchName()).thenReturn("Bitbucket cloud Widget");
    when(branchConfiguration.branchType()).thenReturn(PULL_REQUEST);
    when(branchConfiguration.pullRequestKey()).thenReturn("105");

    ReportPublisher underTest = new ReportPublisher(properties, wsClient, server, contextPublisher, moduleHierarchy, mode, mock(TempFolder.class),
      new ReportPublisherStep[0], branchConfiguration, reportMetadataHolder, analysisWarnings, javaArchitectureInformationProvider, fileStructure, ciConfiguration);

    underTest.prepareAndDumpMetadata("TASK-123");

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

    assertThatThrownBy(() -> underTest.start())
      .isInstanceOf(MessageException.class)
      .hasMessage("Failed to parse public URL set in SonarQube server: invalid");
  }

  @Test
  public void should_not_dump_information_when_medium_test_enabled() {
    when(mode.isMediumTest()).thenReturn(true);
    underTest.start();
    underTest.execute();

    assertThat(logTester.logs(Level.INFO))
      .contains("ANALYSIS SUCCESSFUL")
      .doesNotContain("dashboard/index");

    assertThat(properties.metadataFilePath()).doesNotExist();
  }

  @Test
  public void should_upload_and_dump_information() {
    when(reportMetadataHolder.getDashboardUrl()).thenReturn("https://publicserver/sonarqube/dashboard?id=org.sonarsource.sonarqube%3Asonarqube");
    when(reportMetadataHolder.getCeTaskUrl()).thenReturn("https://publicserver/sonarqube/api/ce/task?id=TASK-123");

    MockWsResponse submitMockResponse = new MockWsResponse();
    submitMockResponse.setContent(Ce.SubmitResponse.newBuilder().setTaskId("task-1234").build().toByteArray());
    when(wsClient.call(any())).thenReturn(submitMockResponse);
    underTest.start();
    underTest.execute();

    assertThat(properties.metadataFilePath()).exists();
    assertThat(logTester.logs(Level.DEBUG))
      .contains("Report metadata written to " + properties.metadataFilePath());
    assertThat(logTester.logs(Level.INFO))
      .contains("ANALYSIS SUCCESSFUL, you can find the results at: https://publicserver/sonarqube/dashboard?id=org.sonarsource.sonarqube%3Asonarqube")
      .contains("More about the report processing at https://publicserver/sonarqube/api/ce/task?id=TASK-123");
  }

  @Test
  public void dump_information_to_custom_path() {
    underTest.prepareAndDumpMetadata("TASK-123");

    assertThat(properties.metadataFilePath()).exists();
    assertThat(logTester.logs(Level.DEBUG)).contains("Report metadata written to " + properties.metadataFilePath());
  }

  @Test
  public void should_not_delete_report_if_property_is_set() throws IOException {
    when(properties.shouldKeepReport()).thenReturn(true);

    underTest.start();
    underTest.stop();
    assertThat(fileStructure.root()).isDirectory();
  }

  @Test
  public void should_delete_report_by_default() throws IOException {
    underTest.start();
    underTest.stop();
    assertThat(fileStructure.root()).doesNotExist();
  }

  @Test
  public void test_ws_parameters() throws Exception {
    WsResponse response = mock(WsResponse.class);

    PipedOutputStream out = new PipedOutputStream();
    PipedInputStream in = new PipedInputStream(out);
    Ce.SubmitResponse.newBuilder().build().writeTo(out);
    out.close();

    when(response.failIfNotSuccessful()).thenReturn(response);
    when(response.contentStream()).thenReturn(in);

    when(wsClient.call(any(WsRequest.class))).thenReturn(response);
    underTest.upload(reportTempFolder.newFile());

    ArgumentCaptor<WsRequest> capture = ArgumentCaptor.forClass(WsRequest.class);
    verify(wsClient).call(capture.capture());

    WsRequest wsRequest = capture.getValue();
    assertThat(wsRequest.getParameters().getKeys()).containsOnly("projectKey");
    assertThat(wsRequest.getParameters().getValue("projectKey")).isEqualTo("org.sonarsource.sonarqube:sonarqube");
    assertThat(wsRequest.getParameters().getValues("characteristic")).isEmpty();
  }

  @Test
  public void test_send_branches_characteristics() throws Exception {
    String branchName = "feature";
    when(branchConfiguration.branchName()).thenReturn(branchName);
    when(branchConfiguration.branchType()).thenReturn(BRANCH);

    WsResponse response = mock(WsResponse.class);

    PipedOutputStream out = new PipedOutputStream();
    PipedInputStream in = new PipedInputStream(out);
    Ce.SubmitResponse.newBuilder().build().writeTo(out);
    out.close();

    when(response.failIfNotSuccessful()).thenReturn(response);
    when(response.contentStream()).thenReturn(in);

    when(wsClient.call(any(WsRequest.class))).thenReturn(response);
    underTest.upload(reportTempFolder.newFile());

    ArgumentCaptor<WsRequest> capture = ArgumentCaptor.forClass(WsRequest.class);
    verify(wsClient).call(capture.capture());

    WsRequest wsRequest = capture.getValue();
    assertThat(wsRequest.getParameters().getKeys()).hasSize(2);
    assertThat(wsRequest.getParameters().getValues("projectKey")).containsExactly("org.sonarsource.sonarqube:sonarqube");
    assertThat(wsRequest.getParameters().getValues("characteristic"))
      .containsExactlyInAnyOrder("branch=" + branchName, "branchType=" + BRANCH.name());
  }

  @Test
  public void send_pull_request_characteristic() throws Exception {
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
    underTest.upload(reportTempFolder.newFile());

    ArgumentCaptor<WsRequest> capture = ArgumentCaptor.forClass(WsRequest.class);
    verify(wsClient).call(capture.capture());

    WsRequest wsRequest = capture.getValue();
    assertThat(wsRequest.getParameters().getKeys()).hasSize(2);
    assertThat(wsRequest.getParameters().getValues("projectKey")).containsExactly("org.sonarsource.sonarqube:sonarqube");
    assertThat(wsRequest.getParameters().getValues("characteristic"))
      .containsExactlyInAnyOrder("pullRequest=" + pullRequestId);
  }

  @Test
  public void upload_whenDevOpsPlatformInformationPresentInCiConfiguration_shouldUploadDevOpsPlatformInfoAsCharacteristic() throws Exception {
    String branchName = "feature";
    String pullRequestId = "pr-123";
    DevOpsPlatformInfo devOpsPlatformInfo = new DevOpsPlatformInfo("https://devops.example.com", "projectId");

    when(branchConfiguration.branchName()).thenReturn(branchName);
    when(branchConfiguration.branchType()).thenReturn(PULL_REQUEST);
    when(branchConfiguration.pullRequestKey()).thenReturn(pullRequestId);
    when(ciConfiguration.getDevOpsPlatformInfo()).thenReturn(Optional.of(devOpsPlatformInfo));

    WsResponse response = mock(WsResponse.class);

    PipedOutputStream out = new PipedOutputStream();
    PipedInputStream in = new PipedInputStream(out);
    Ce.SubmitResponse.newBuilder().build().writeTo(out);
    out.close();

    when(response.failIfNotSuccessful()).thenReturn(response);
    when(response.contentStream()).thenReturn(in);

    when(wsClient.call(any(WsRequest.class))).thenReturn(response);
    underTest.upload(reportTempFolder.newFile());

    ArgumentCaptor<WsRequest> capture = ArgumentCaptor.forClass(WsRequest.class);
    verify(wsClient).call(capture.capture());

    WsRequest wsRequest = capture.getValue();
    assertThat(wsRequest.getParameters().getValues("characteristic"))
      .contains(
        "devOpsPlatformUrl=" + devOpsPlatformInfo.getUrl(),
        "devOpsPlatformProjectIdentifier=" + devOpsPlatformInfo.getProjectIdentifier());
  }

  @Test
  public void test_do_not_log_or_add_warning_if_using_64bit_jre() {
    when(javaArchitectureInformationProvider.is64bitJavaVersion()).thenReturn(true);
    when(mode.isMediumTest()).thenReturn(true);
    underTest.start();
    underTest.execute();

    assertThat(logTester.logs(Level.WARN)).isEmpty();

    verifyNoInteractions(analysisWarnings);
  }

  @Test
  public void test_log_and_add_warning_if_using_non64bit_jre() {
    when(javaArchitectureInformationProvider.is64bitJavaVersion()).thenReturn(false);
    when(mode.isMediumTest()).thenReturn(true);
    underTest.start();
    underTest.execute();

    assertThat(logTester.logs(Level.WARN)).containsOnly(SUPPORT_OF_32_BIT_JRE_IS_DEPRECATED_MESSAGE);
    verify(analysisWarnings).addUnique(SUPPORT_OF_32_BIT_JRE_IS_DEPRECATED_MESSAGE);
  }

}
