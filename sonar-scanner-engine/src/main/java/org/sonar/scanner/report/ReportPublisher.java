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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
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
import org.picocontainer.Startable;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.ZipUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.bootstrap.GlobalAnalysisMode;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonar.scanner.protocol.output.ScannerReportReader;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.scan.ScanProperties;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonarqube.ws.Ce;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsResponse;

import static java.net.URLEncoder.encode;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.sonar.core.util.FileUtils.deleteQuietly;
import static org.sonar.scanner.scan.branch.BranchType.LONG;
import static org.sonar.scanner.scan.branch.BranchType.PULL_REQUEST;
import static org.sonar.scanner.scan.branch.BranchType.SHORT;

public class ReportPublisher implements Startable {

  private static final Logger LOG = Loggers.get(ReportPublisher.class);
  private static final String CHARACTERISTIC = "characteristic";
  private static final String DASHBOARD = "dashboard";
  private static final String BRANCH = "branch";
  private static final String ID = "id";
  private static final String RESOLVED = "resolved";

  private final ScannerWsClient wsClient;
  private final AnalysisContextReportPublisher contextPublisher;
  private final InputModuleHierarchy moduleHierarchy;
  private final GlobalAnalysisMode analysisMode;
  private final TempFolder temp;
  private final ReportPublisherStep[] publishers;
  private final Server server;
  private final BranchConfiguration branchConfiguration;
  private final ScanProperties properties;

  private Path reportDir;
  private ScannerReportWriter writer;
  private ScannerReportReader reader;

  public ReportPublisher(ScanProperties properties, ScannerWsClient wsClient, Server server, AnalysisContextReportPublisher contextPublisher,
    InputModuleHierarchy moduleHierarchy, GlobalAnalysisMode analysisMode, TempFolder temp, ReportPublisherStep[] publishers, BranchConfiguration branchConfiguration) {
    this.wsClient = wsClient;
    this.server = server;
    this.contextPublisher = contextPublisher;
    this.moduleHierarchy = moduleHierarchy;
    this.analysisMode = analysisMode;
    this.temp = temp;
    this.publishers = publishers;
    this.branchConfiguration = branchConfiguration;
    this.properties = properties;
  }

  @Override
  public void start() {
    reportDir = moduleHierarchy.root().getWorkDir().resolve("scanner-report");
    writer = new ScannerReportWriter(reportDir.toFile());
    reader = new ScannerReportReader(reportDir.toFile());
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
    String taskId = null;
    File report = generateReportFile();
    if (properties.shouldKeepReport()) {
      LOG.info("Analysis report generated in " + reportDir);
    }
    if (!analysisMode.isMediumTest()) {
      taskId = upload(report);
    }
    logSuccess(taskId);
  }

  private File generateReportFile() {
    try {
      long startTime = System.currentTimeMillis();
      for (ReportPublisherStep publisher : publishers) {
        publisher.publish(writer);
      }
      long stopTime = System.currentTimeMillis();
      LOG.info("Analysis report generated in {}ms, dir size={}", stopTime - startTime, FileUtils.byteCountToDisplaySize(FileUtils.sizeOfDirectory(reportDir.toFile())));

      startTime = System.currentTimeMillis();
      File reportZip = temp.newFile("scanner-report", ".zip");
      ZipUtils.zipDir(reportDir.toFile(), reportZip);
      stopTime = System.currentTimeMillis();
      LOG.info("Analysis report compressed in {}ms, zip size={}", stopTime - startTime, FileUtils.byteCountToDisplaySize(FileUtils.sizeOf(reportZip)));
      return reportZip;
    } catch (IOException e) {
      throw new IllegalStateException("Unable to prepare analysis report", e);
    }
  }

  /**
   * Uploads the report file to server and returns the generated task id
   */
  @VisibleForTesting
  String upload(File report) {
    LOG.debug("Upload report");
    long startTime = System.currentTimeMillis();
    PostRequest.Part filePart = new PostRequest.Part(MediaTypes.ZIP, report);
    PostRequest post = new PostRequest("api/ce/submit")
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam("organization", properties.organizationKey().orElse(null))
      .setParam("projectKey", moduleHierarchy.root().key())
      .setParam("projectName", moduleHierarchy.root().getOriginalName())
      .setParam("projectBranch", moduleHierarchy.root().getBranch())
      .setPart("report", filePart);

    String branchName = branchConfiguration.branchName();
    if (branchName != null) {
      if (branchConfiguration.branchType() != PULL_REQUEST) {
        post.setParam(CHARACTERISTIC, "branch=" + branchName);
        post.setParam(CHARACTERISTIC, "branchType=" + branchConfiguration.branchType().name());
      } else {
        post.setParam(CHARACTERISTIC, "pullRequest=" + branchConfiguration.pullRequestKey());
      }
    }

    WsResponse response;
    try {
      response = wsClient.call(post).failIfNotSuccessful();
    } catch (HttpException e) {
      throw MessageException.of(String.format("Failed to upload report - %s", ScannerWsClient.createErrorMessage(e)));
    }

    try (InputStream protobuf = response.contentStream()) {
      return Ce.SubmitResponse.parser().parseFrom(protobuf).getTaskId();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    } finally {
      long stopTime = System.currentTimeMillis();
      LOG.info("Analysis report uploaded in " + (stopTime - startTime) + "ms");
    }
  }

  @VisibleForTesting
  void logSuccess(@Nullable String taskId) {
    if (taskId == null) {
      LOG.info("ANALYSIS SUCCESSFUL");
    } else {

      Map<String, String> metadata = new LinkedHashMap<>();
      String effectiveKey = moduleHierarchy.root().getKeyWithBranch();
      properties.organizationKey().ifPresent(org -> metadata.put("organization", org));
      metadata.put("projectKey", effectiveKey);
      metadata.put("serverUrl", server.getPublicRootUrl());
      metadata.put("serverVersion", server.getVersion());
      properties.branch().ifPresent(branch -> metadata.put("branch", branch));

      URL dashboardUrl = buildDashboardUrl(server.getPublicRootUrl(), effectiveKey);
      metadata.put("dashboardUrl", dashboardUrl.toExternalForm());

      URL taskUrl = HttpUrl.parse(server.getPublicRootUrl()).newBuilder()
        .addPathSegment("api").addPathSegment("ce").addPathSegment("task")
        .addQueryParameter(ID, taskId)
        .build()
        .url();
      metadata.put("ceTaskId", taskId);
      metadata.put("ceTaskUrl", taskUrl.toExternalForm());

      LOG.info("ANALYSIS SUCCESSFUL, you can browse {}", dashboardUrl);
      LOG.info("Note that you will be able to access the updated dashboard once the server has processed the submitted analysis report");
      LOG.info("More about the report processing at {}", taskUrl);

      dumpMetadata(metadata);
    }
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

    if (onLongLivingBranch(branchConfiguration)) {
      return httpUrl.newBuilder()
        .addPathSegment(DASHBOARD)
        .addEncodedQueryParameter(ID, encoded(effectiveKey))
        .addEncodedQueryParameter(BRANCH, encoded(branchConfiguration.branchName()))
        .build()
        .url();
    }

    if (onShortLivingBranch(branchConfiguration)) {
      return httpUrl.newBuilder()
        .addPathSegment(DASHBOARD)
        .addEncodedQueryParameter(ID, encoded(effectiveKey))
        .addEncodedQueryParameter(BRANCH, encoded(branchConfiguration.branchName()))
        .addQueryParameter(RESOLVED, "false")
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

  private static boolean onShortLivingBranch(BranchConfiguration branchConfiguration) {
    return branchConfiguration.branchName() != null && (branchConfiguration.branchType() == SHORT);
  }

  private static boolean onLongLivingBranch(BranchConfiguration branchConfiguration) {
    return branchConfiguration.branchName() != null && (branchConfiguration.branchType() == LONG);
  }

  private static boolean onMainBranch(BranchConfiguration branchConfiguration) {
    return branchConfiguration.branchName() == null;
  }

  private static String encoded(@Nullable String queryParameter) {
    if (isBlank(queryParameter)) {
      return EMPTY;
    }
    try {
      return encode(queryParameter, "UTF-8");
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
