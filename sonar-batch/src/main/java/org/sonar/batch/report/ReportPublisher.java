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

import org.sonar.batch.analysis.DefaultAnalysisMode;

import org.sonar.batch.util.BatchUtils;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.annotations.VisibleForTesting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.ZipUtils;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.batch.scan.ImmutableProjectReactor;
import static java.lang.String.format;

@BatchSide
public class ReportPublisher implements Startable {

  private static final Logger LOG = LoggerFactory.getLogger(ReportPublisher.class);
  public static final String KEEP_REPORT_PROP_KEY = "sonar.batch.keepReport";
  public static final String DUMP_REPORT_PROP_KEY = "sonar.batch.dumpReportDir";

  private final ServerClient serverClient;
  private final Server server;
  private final Settings settings;
  private final ImmutableProjectReactor projectReactor;
  private final DefaultAnalysisMode analysisMode;
  private final TempFolder temp;

  private ReportPublisherStep[] publishers;

  private File reportDir;
  private BatchReportWriter writer;

  public ReportPublisher(Settings settings, ServerClient serverClient, Server server,
    ImmutableProjectReactor projectReactor, DefaultAnalysisMode analysisMode, TempFolder temp, ReportPublisherStep[] publishers) {
    this.serverClient = serverClient;
    this.server = server;
    this.projectReactor = projectReactor;
    this.settings = settings;
    this.analysisMode = analysisMode;
    this.temp = temp;
    this.publishers = publishers;
  }

  @Override
  public void start() {
    reportDir = new File(projectReactor.getRoot().getWorkDir(), "batch-report");
    writer = new BatchReportWriter(reportDir);
  }

  @Override
  public void stop() {
    if (!settings.getBoolean(KEEP_REPORT_PROP_KEY)) {
      FileUtils.deleteQuietly(reportDir);
    } else {
      LOG.info("Batch report generated in " + reportDir);
    }
  }

  public File getReportDir() {
    return reportDir;
  }

  public BatchReportWriter getWriter() {
    return writer;
  }

  public void execute() {
    // If this is a issues mode analysis then we should not upload reports
    if (!analysisMode.isIssues()) {
      File report = prepareReport();
      if (!analysisMode.isMediumTest()) {
        sendOrDumpReport(report);
      }
    }
    logSuccess(LoggerFactory.getLogger(getClass()));
  }

  private File prepareReport() {
    try {
      long startTime = System.currentTimeMillis();
      for (ReportPublisherStep publisher : publishers) {
        publisher.publish(writer);
      }
      long stopTime = System.currentTimeMillis();
      LOG.info("Analysis reports generated in " + (stopTime - startTime) + "ms, dir size=" + FileUtils.byteCountToDisplaySize(FileUtils.sizeOfDirectory(reportDir)));

      startTime = System.currentTimeMillis();
      File reportZip = temp.newFile("batch-report", ".zip");
      ZipUtils.zipDir(reportDir, reportZip);
      stopTime = System.currentTimeMillis();
      LOG.info("Analysis reports compressed in " + (stopTime - startTime) + "ms, zip size=" + FileUtils.byteCountToDisplaySize(FileUtils.sizeOf(reportZip)));
      return reportZip;
    } catch (IOException e) {
      throw new IllegalStateException("Unable to prepare batch report", e);
    }
  }

  @VisibleForTesting
  void sendOrDumpReport(File report) {
    ProjectDefinition projectDefinition = projectReactor.getRoot();
    String effectiveKey = projectDefinition.getKeyWithBranch();
    String relativeUrl = "/api/computation/submit_report?projectKey=" + effectiveKey + "&projectName=" + BatchUtils.encodeForUrl(projectDefinition.getName());

    String dumpDirLocation = settings.getString(DUMP_REPORT_PROP_KEY);
    if (dumpDirLocation == null) {
      uploadMultiPartReport(report, relativeUrl);
    } else {
      dumpReport(dumpDirLocation, effectiveKey, relativeUrl, report);
    }
  }

  private void uploadMultiPartReport(File report, String relativeUrl) {
    LOG.debug("Publish results");
    long startTime = System.currentTimeMillis();
    URL url;
    try {
      url = new URL(serverClient.getURL() + relativeUrl);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Invalid URL", e);
    }
    HttpRequest request = HttpRequest.post(url);
    request.trustAllCerts();
    request.trustAllHosts();
    request.header("User-Agent", format("SonarQube %s", server.getVersion()));
    request.basic(serverClient.getLogin(), serverClient.getPassword());
    request.part("report", null, "application/octet-stream", report);
    if (!request.ok()) {
      int responseCode = request.code();
      if (responseCode == 401) {
        throw new IllegalStateException(format(serverClient.getMessageWhenNotAuthorized(), CoreProperties.LOGIN, CoreProperties.PASSWORD));
      }
      if (responseCode == 403) {
        // SONAR-4397 Details are in response content
        throw new IllegalStateException(request.body());
      }
      throw new IllegalStateException(format("Fail to execute request [code=%s, url=%s]: %s", responseCode, url, request.body()));
    }
    long stopTime = System.currentTimeMillis();
    LOG.info("Analysis reports sent to server in " + (stopTime - startTime) + "ms");
  }

  private void dumpReport(String dumpDirLocation, String projectKey, String relativeUrl, File report) {
    LOG.debug("Dump report to file");
    try {
      dumpReportImpl(dumpDirLocation, projectKey, relativeUrl, report);
    } catch (IOException | URISyntaxException e) {
      LOG.error("Failed to dump report to directory " + dumpDirLocation, e);
    }
  }

  private static void dumpReportImpl(String dumpDirLocation, String projectKey, String relativeUrl, File report) throws IOException, URISyntaxException {
    File dumpDir = new File(dumpDirLocation);
    if (!dumpDir.exists() || !dumpDir.isDirectory()) {
      LOG.warn("Report dump directory '{}' does not exist or is not a directory", dumpDirLocation);
      return;
    }
    long dateTime = new Date().getTime();
    File dumpedZip = new File(dumpDir, format("batch-report_%s_%s.zip", projectKey, dateTime));
    FileUtils.copyFile(report, new FileOutputStream(dumpedZip));
    File dumpedMetadata = new File(dumpDir, format("batch-report_%s_%s.txt", projectKey, dateTime));
    FileUtils.write(dumpedMetadata, relativeUrl);
    LOG.info("Batch report dumped to {}", dumpedZip.getAbsolutePath());
  }

  @VisibleForTesting
  void logSuccess(Logger logger) {
    if (analysisMode.isIssues() || analysisMode.isMediumTest()) {
      logger.info("ANALYSIS SUCCESSFUL");

    } else {
      String baseUrl = settings.getString(CoreProperties.SERVER_BASE_URL);
      if (baseUrl.equals(settings.getDefaultValue(CoreProperties.SERVER_BASE_URL))) {
        // If server base URL was not configured in Sonar server then is is better to take URL configured on batch side
        baseUrl = serverClient.getURL();
      }
      if (!baseUrl.endsWith("/")) {
        baseUrl += "/";
      }
      String effectiveKey = projectReactor.getRoot().getKeyWithBranch();
      String url = baseUrl + "dashboard/index/" + effectiveKey;
      logger.info("ANALYSIS SUCCESSFUL, you can browse {}", url);
      logger.info("Note that you will be able to access the updated dashboard once the server has processed the submitted analysis report.");
    }
  }
}
