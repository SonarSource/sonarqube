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
package org.sonar.scanner.report;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.ZipUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.bootstrap.GlobalAnalysisMode;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonarqube.ws.Ce;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsResponse;

import static org.sonar.core.config.ScannerProperties.BRANCH_NAME;
import static org.sonar.core.config.ScannerProperties.ORGANIZATION;
import static org.sonar.core.util.FileUtils.deleteQuietly;

@ScannerSide
public class ReportPublisher implements Startable {

  private static final Logger LOG = Loggers.get(ReportPublisher.class);

  public static final String KEEP_REPORT_PROP_KEY = "sonar.scanner.keepReport";
  public static final String VERBOSE_KEY = "sonar.verbose";
  public static final String METADATA_DUMP_FILENAME = "report-task.txt";
  private static final String CHARACTERISTIC = "characteristic";

  private final Configuration settings;
  private final ScannerWsClient wsClient;
  private final AnalysisContextReportPublisher contextPublisher;
  private final InputModuleHierarchy moduleHierarchy;
  private final GlobalAnalysisMode analysisMode;
  private final TempFolder temp;
  private final ReportPublisherStep[] publishers;
  private final Server server;
  private final BranchConfiguration branchConfiguration;

  private Path reportDir;
  private ScannerReportWriter writer;

  public ReportPublisher(Configuration settings, ScannerWsClient wsClient, Server server, AnalysisContextReportPublisher contextPublisher,
    InputModuleHierarchy moduleHierarchy, GlobalAnalysisMode analysisMode, TempFolder temp, ReportPublisherStep[] publishers, BranchConfiguration branchConfiguration) {
    this.settings = settings;
    this.wsClient = wsClient;
    this.server = server;
    this.contextPublisher = contextPublisher;
    this.moduleHierarchy = moduleHierarchy;
    this.analysisMode = analysisMode;
    this.temp = temp;
    this.publishers = publishers;
    this.branchConfiguration = branchConfiguration;
  }

  @Override
  public void start() {
    reportDir = moduleHierarchy.root().getWorkDir().resolve("scanner-report");
    writer = new ScannerReportWriter(reportDir.toFile());
    contextPublisher.init(writer);

    if (!analysisMode.isIssues() && !analysisMode.isMediumTest()) {
      String publicUrl = server.getPublicRootUrl();
      if (HttpUrl.parse(publicUrl) == null) {
        throw MessageException.of("Failed to parse public URL set in SonarQube server: " + publicUrl);
      }
    }
  }

  @Override
  public void stop() {
    if (!shouldKeepReport()) {
      deleteQuietly(reportDir);
    }
  }

  public Path getReportDir() {
    return reportDir;
  }

  public ScannerReportWriter getWriter() {
    return writer;
  }

  public void execute() {
    // If this is a issues mode analysis then we should not upload reports
    String taskId = null;
    if (!analysisMode.isIssues()) {
      File report = generateReportFile();
      if (shouldKeepReport()) {
        LOG.info("Analysis report generated in " + reportDir);
      }
      if (!analysisMode.isMediumTest()) {
        taskId = upload(report);
      }
    }
    logSuccess(taskId);
  }

  private boolean shouldKeepReport() {
    return settings.getBoolean(KEEP_REPORT_PROP_KEY).orElse(false) || settings.getBoolean(VERBOSE_KEY).orElse(false);
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
      LOG.info("Analysis reports compressed in {}ms, zip size={}", stopTime - startTime, FileUtils.byteCountToDisplaySize(FileUtils.sizeOf(reportZip)));
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
      .setParam("organization", settings.get(ORGANIZATION).orElse(null))
      .setParam("projectKey", moduleHierarchy.root().key())
      .setParam("projectName", moduleHierarchy.root().getOriginalName())
      .setParam("projectBranch", moduleHierarchy.root().getBranch())
      .setPart("report", filePart);

    String branchName = branchConfiguration.branchName();
    if (branchName != null) {
      post.setParam(CHARACTERISTIC, "branch=" + branchName);
      post.setParam(CHARACTERISTIC, "branchType=" + branchConfiguration.branchType().name());
    }

    WsResponse response;
    try {
      response = wsClient.call(post).failIfNotSuccessful();
    } catch (HttpException e) {
      throw MessageException.of(String.format("Failed to upload report - %d: %s", e.code(), ScannerWsClient.tryParseAsJsonError(e.content())));
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
      String publicUrl = server.getPublicRootUrl();
      HttpUrl httpUrl = HttpUrl.parse(publicUrl);

      Map<String, String> metadata = new LinkedHashMap<>();
      String effectiveKey = moduleHierarchy.root().getKeyWithBranch();
      settings.get(ORGANIZATION).ifPresent(org -> metadata.put("organization", org));
      metadata.put("projectKey", effectiveKey);
      metadata.put("serverUrl", publicUrl);
      metadata.put("serverVersion", server.getVersion());
      settings.get(BRANCH_NAME).ifPresent(branch -> metadata.put("branch", branch));

      URL dashboardUrl = httpUrl.newBuilder()
        .addPathSegment("dashboard").addPathSegment("index").addPathSegment(effectiveKey)
        .build()
        .url();
      metadata.put("dashboardUrl", dashboardUrl.toExternalForm());

      URL taskUrl = HttpUrl.parse(publicUrl).newBuilder()
        .addPathSegment("api").addPathSegment("ce").addPathSegment("task")
        .addQueryParameter("id", taskId)
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

  private void dumpMetadata(Map<String, String> metadata) {
    Path file = moduleHierarchy.root().getWorkDir().resolve(METADATA_DUMP_FILENAME);
    try (Writer output = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
      for (Map.Entry<String, String> entry : metadata.entrySet()) {
        output.write(entry.getKey());
        output.write("=");
        output.write(entry.getValue());
        output.write("\n");
      }

      LOG.debug("Report metadata written to {}", file);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to dump " + file, e);
    }
  }
}
