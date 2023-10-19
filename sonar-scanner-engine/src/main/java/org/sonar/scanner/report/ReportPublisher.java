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

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Startable;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.ZipUtils;
import org.sonar.core.ce.CeTaskCharacteristics;
import org.sonar.scanner.bootstrap.DefaultScannerWsClient;
import org.sonar.scanner.bootstrap.GlobalAnalysisMode;
import org.sonar.scanner.ci.CiConfiguration;
import org.sonar.scanner.fs.InputModuleHierarchy;
import org.sonar.scanner.protocol.output.FileStructure;
import org.sonar.scanner.protocol.output.ScannerReportReader;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.scan.ScanProperties;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchType;
import org.sonarqube.ws.Ce;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.RequestWithPayload.Part;
import org.sonarqube.ws.client.WsResponse;

import static java.net.URLEncoder.encode;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.sonar.core.ce.CeTaskCharacteristics.BRANCH_TYPE;
import static org.sonar.core.ce.CeTaskCharacteristics.DEVOPS_PLATFORM_PROJECT_IDENTIFIER;
import static org.sonar.core.ce.CeTaskCharacteristics.DEVOPS_PLATFORM_URL;
import static org.sonar.core.util.FileUtils.deleteQuietly;
import static org.sonar.core.util.FileUtils.humanReadableByteCountSI;
import static org.sonar.scanner.scan.branch.BranchType.PULL_REQUEST;

public class ReportPublisher implements Startable {
  private static final Logger LOG = LoggerFactory.getLogger(ReportPublisher.class);
  private static final String CHARACTERISTIC = "characteristic";
  private static final String DASHBOARD = "dashboard";
  private static final String BRANCH = "branch";
  private static final String ID = "id";

  @VisibleForTesting
  static final String SUPPORT_OF_32_BIT_JRE_IS_DEPRECATED_MESSAGE = "You are using a 32 bits JRE. "
    + "The support of 32 bits JRE is deprecated and a future version of the scanner will remove it completely.";

  private final DefaultScannerWsClient wsClient;
  private final AnalysisContextReportPublisher contextPublisher;
  private final InputModuleHierarchy moduleHierarchy;
  private final GlobalAnalysisMode analysisMode;
  private final TempFolder temp;
  private final ReportPublisherStep[] publishers;
  private final Server server;
  private final BranchConfiguration branchConfiguration;
  private final ScanProperties properties;
  private final CeTaskReportDataHolder ceTaskReportDataHolder;

  private final Path reportDir;
  private final ScannerReportWriter writer;
  private final ScannerReportReader reader;
  private final AnalysisWarnings analysisWarnings;
  private final JavaArchitectureInformationProvider javaArchitectureInformationProvider;

  private final CiConfiguration ciConfiguration;

  public ReportPublisher(ScanProperties properties, DefaultScannerWsClient wsClient, Server server, AnalysisContextReportPublisher contextPublisher,
    InputModuleHierarchy moduleHierarchy, GlobalAnalysisMode analysisMode, TempFolder temp, ReportPublisherStep[] publishers, BranchConfiguration branchConfiguration,
    CeTaskReportDataHolder ceTaskReportDataHolder, AnalysisWarnings analysisWarnings,
    JavaArchitectureInformationProvider javaArchitectureInformationProvider, FileStructure fileStructure, CiConfiguration ciConfiguration) {
    this.wsClient = wsClient;
    this.server = server;
    this.contextPublisher = contextPublisher;
    this.moduleHierarchy = moduleHierarchy;
    this.analysisMode = analysisMode;
    this.temp = temp;
    this.publishers = publishers;
    this.branchConfiguration = branchConfiguration;
    this.properties = properties;
    this.ceTaskReportDataHolder = ceTaskReportDataHolder;
    this.reportDir = fileStructure.root().toPath();
    this.analysisWarnings = analysisWarnings;
    this.javaArchitectureInformationProvider = javaArchitectureInformationProvider;
    this.writer = new ScannerReportWriter(fileStructure);
    this.reader = new ScannerReportReader(fileStructure);
    this.ciConfiguration = ciConfiguration;
  }

  @Override
  public void start() {
    contextPublisher.init(writer);

    if (!analysisMode.isMediumTest()) {
      String publicUrl = server.getPublicRootUrl();
      if (HttpUrl.parse(publicUrl) == null) {
        throw MessageException.of("Failed to parse public URL set in SonarQube server: " + publicUrl);
      }
    }
  }

  @Override
  public void stop() {
    if (!properties.shouldKeepReport()) {
      deleteQuietly(reportDir);
    }
  }

  public Path getReportDir() {
    return reportDir;
  }

  public ScannerReportWriter getWriter() {
    return writer;
  }

  public ScannerReportReader getReader() {
    return reader;
  }

  public void execute() {
    logDeprecationWarningIf32bitJava();
    File report = generateReportFile();
    if (properties.shouldKeepReport()) {
      LOG.info("Analysis report generated in " + reportDir);
    }
    if (!analysisMode.isMediumTest()) {
      String taskId = upload(report);
      prepareAndDumpMetadata(taskId);
    }

    logSuccess();
  }

  private void logDeprecationWarningIf32bitJava() {
    if (!javaArchitectureInformationProvider.is64bitJavaVersion()) {
      analysisWarnings.addUnique(SUPPORT_OF_32_BIT_JRE_IS_DEPRECATED_MESSAGE);
      LOG.warn(SUPPORT_OF_32_BIT_JRE_IS_DEPRECATED_MESSAGE);
    }
  }

  private File generateReportFile() {
    try {
      long startTime = System.currentTimeMillis();
      for (ReportPublisherStep publisher : publishers) {
        publisher.publish(writer);
      }
      long stopTime = System.currentTimeMillis();
      LOG.info("Analysis report generated in {}ms, dir size={}", stopTime - startTime, humanReadableByteCountSI(FileUtils.sizeOfDirectory(reportDir.toFile())));

      startTime = System.currentTimeMillis();
      File reportZip = temp.newFile("scanner-report", ".zip");
      ZipUtils.zipDir(reportDir.toFile(), reportZip);
      stopTime = System.currentTimeMillis();
      LOG.info("Analysis report compressed in {}ms, zip size={}", stopTime - startTime, humanReadableByteCountSI(FileUtils.sizeOf(reportZip)));
      return reportZip;
    } catch (IOException e) {
      throw new IllegalStateException("Unable to prepare analysis report", e);
    }
  }

  private void logSuccess() {
    if (analysisMode.isMediumTest()) {
      LOG.info("ANALYSIS SUCCESSFUL");
    } else if (!properties.shouldWaitForQualityGate()) {
      LOG.info("ANALYSIS SUCCESSFUL, you can find the results at: {}", ceTaskReportDataHolder.getDashboardUrl());
      LOG.info("Note that you will be able to access the updated dashboard once the server has processed the submitted analysis report");
      LOG.info("More about the report processing at {}", ceTaskReportDataHolder.getCeTaskUrl());
    }
  }

  /**
   * Uploads the report file to server and returns the generated task id
   */
  String upload(File report) {
    LOG.debug("Upload report");
    long startTime = System.currentTimeMillis();
    Part filePart = new Part(MediaTypes.ZIP, report);
    PostRequest post = new PostRequest("api/ce/submit")
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam("projectKey", moduleHierarchy.root().key())
      .setParam("projectName", moduleHierarchy.root().getOriginalName())
      .setPart("report", filePart);

    ciConfiguration.getDevOpsPlatformInfo().ifPresent(devOpsPlatformInfo -> {
      post.setParam(CHARACTERISTIC, buildCharacteristicParam(DEVOPS_PLATFORM_URL ,devOpsPlatformInfo.getUrl()));
      post.setParam(CHARACTERISTIC, buildCharacteristicParam(DEVOPS_PLATFORM_PROJECT_IDENTIFIER, devOpsPlatformInfo.getProjectIdentifier()));
    });
    String branchName = branchConfiguration.branchName();
    if (branchName != null) {
      if (branchConfiguration.branchType() != PULL_REQUEST) {
        post.setParam(CHARACTERISTIC, buildCharacteristicParam(CeTaskCharacteristics.BRANCH, branchName));
        post.setParam(CHARACTERISTIC, buildCharacteristicParam(BRANCH_TYPE, branchConfiguration.branchType().name()));
      } else {
        post.setParam(CHARACTERISTIC, buildCharacteristicParam(CeTaskCharacteristics.PULL_REQUEST, branchConfiguration.pullRequestKey()));
      }
    }

    WsResponse response;
    try {
      post.setWriteTimeOutInMs(properties.reportPublishTimeout() * 1000);
      response = wsClient.call(post);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to upload report: " + e.getMessage(), e);
    }

    try {
      response.failIfNotSuccessful();
    } catch (HttpException e) {
      throw MessageException.of(String.format("Server failed to process report. Please check server logs: %s", DefaultScannerWsClient.createErrorMessage(e)));
    }
    try (InputStream protobuf = response.contentStream()) {
      return Ce.SubmitResponse.parser().parseFrom(protobuf).getTaskId();
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      long stopTime = System.currentTimeMillis();
      LOG.info("Analysis report uploaded in " + (stopTime - startTime) + "ms");
    }
  }

  private static String buildCharacteristicParam(String characteristic, String value) {
    return characteristic + "=" + value;
  }

  void prepareAndDumpMetadata(String taskId) {
    Map<String, String> metadata = new LinkedHashMap<>();

    metadata.put("projectKey", moduleHierarchy.root().key());
    metadata.put("serverUrl", server.getPublicRootUrl());
    metadata.put("serverVersion", server.getVersion());
    properties.branch().ifPresent(branch -> metadata.put("branch", branch));

    URL dashboardUrl = buildDashboardUrl(server.getPublicRootUrl(), moduleHierarchy.root().key());
    metadata.put("dashboardUrl", dashboardUrl.toExternalForm());

    URL taskUrl = HttpUrl.parse(server.getPublicRootUrl()).newBuilder()
      .addPathSegment("api").addPathSegment("ce").addPathSegment("task")
      .addQueryParameter(ID, taskId)
      .build()
      .url();
    metadata.put("ceTaskId", taskId);
    metadata.put("ceTaskUrl", taskUrl.toExternalForm());

    ceTaskReportDataHolder.init(taskId, taskUrl.toExternalForm(), dashboardUrl.toExternalForm());
    dumpMetadata(metadata);
  }

  private URL buildDashboardUrl(String publicUrl, String effectiveKey) {
    HttpUrl httpUrl = HttpUrl.parse(publicUrl);

    if (onPullRequest(branchConfiguration)) {
      return httpUrl.newBuilder()
        .addPathSegment(DASHBOARD)
        .addEncodedQueryParameter(ID, encoded(effectiveKey))
        .addEncodedQueryParameter("pullRequest", encoded(branchConfiguration.pullRequestKey()))
        .build()
        .url();
    }

    if (onBranch(branchConfiguration)) {
      return httpUrl.newBuilder()
        .addPathSegment(DASHBOARD)
        .addEncodedQueryParameter(ID, encoded(effectiveKey))
        .addEncodedQueryParameter(BRANCH, encoded(branchConfiguration.branchName()))
        .build()
        .url();
    }

    if (onMainBranch(branchConfiguration)) {
      return httpUrl.newBuilder()
        .addPathSegment(DASHBOARD)
        .addEncodedQueryParameter(ID, encoded(effectiveKey))
        .build()
        .url();
    }

    return httpUrl.newBuilder().build().url();
  }

  private static boolean onPullRequest(BranchConfiguration branchConfiguration) {
    return branchConfiguration.branchName() != null && (branchConfiguration.branchType() == PULL_REQUEST);
  }

  private static boolean onBranch(BranchConfiguration branchConfiguration) {
    return branchConfiguration.branchName() != null && (branchConfiguration.branchType() == BranchType.BRANCH);
  }

  private static boolean onMainBranch(BranchConfiguration branchConfiguration) {
    return branchConfiguration.branchName() == null;
  }

  private static String encoded(@Nullable String queryParameter) {
    if (isBlank(queryParameter)) {
      return EMPTY;
    }
    try {
      return encode(queryParameter, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Unable to urlencode " + queryParameter, e);
    }
  }

  private void dumpMetadata(Map<String, String> metadata) {
    Path file = properties.metadataFilePath();
    try {
      Files.createDirectories(file.getParent());
      try (Writer output = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
          output.write(entry.getKey());
          output.write("=");
          output.write(entry.getValue());
          output.write("\n");
        }
      }

      LOG.debug("Report metadata written to {}", file);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to dump " + file, e);
    }
  }
}
