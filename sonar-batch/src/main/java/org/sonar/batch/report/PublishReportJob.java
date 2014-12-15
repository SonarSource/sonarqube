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

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.Server;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.ZipUtils;
import org.sonar.batch.bootstrap.AnalysisMode;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.batch.index.ResourceCache;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class PublishReportJob implements BatchComponent {

  private static final Logger LOG = LoggerFactory.getLogger(PublishReportJob.class);

  private final ServerClient serverClient;
  private final Server server;
  private final Settings settings;
  private final Project project;
  private final AnalysisMode analysisMode;
  private final ResourceCache resourceCache;
  private final TempFolder temp;

  private ReportPublisher[] publishers;

  public PublishReportJob(Settings settings, ServerClient serverClient, Server server,
    Project project, AnalysisMode analysisMode, TempFolder temp, ResourceCache resourceCache, ReportPublisher[] publishers) {
    this.serverClient = serverClient;
    this.server = server;
    this.project = project;
    this.settings = settings;
    this.analysisMode = analysisMode;
    this.temp = temp;
    this.resourceCache = resourceCache;
    this.publishers = publishers;
  }

  public PublishReportJob(Settings settings, ServerClient serverClient, Server server,
    Project project, AnalysisMode analysisMode, TempFolder temp, ResourceCache resourceCache) {
    this(settings, serverClient, server, project, analysisMode, temp, resourceCache, new ReportPublisher[0]);
  }

  public void execute() {
    // If this is a preview analysis then we should not upload reports
    if (!analysisMode.isPreview()) {
      File report = prepareReport();
      uploadMultiPartReport(report);
    }
    logSuccess(LoggerFactory.getLogger(getClass()));
  }

  private File prepareReport() {
    try {
      File reportDir = temp.newDir("batch-report");
      for (ReportPublisher publisher : publishers) {
        publisher.export(reportDir);
      }

      File reportZip = temp.newFile("batch-report", ".zip");
      ZipUtils.zipDir(reportDir, reportZip);
      FileUtils.deleteDirectory(reportDir);
      return reportZip;
    } catch (IOException e) {
      throw new IllegalStateException("Unable to prepare batch report", e);
    }
  }

  @VisibleForTesting
  void uploadMultiPartReport(File report) {
    LOG.debug("Publish results");
    URL url;
    try {
      int snapshotId = resourceCache.get(project.getEffectiveKey()).snapshotId();
      url = new URL(serverClient.getURL() + "/batch/upload_report?project=" + project.getEffectiveKey() + "&snapshot=" + snapshotId);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Invalid URL", e);
    }
    HttpRequest request = HttpRequest.post(url);
    request.trustAllCerts();
    request.trustAllHosts();
    request.header("User-Agent", String.format("SonarQube %s", server.getVersion()));
    request.basic(serverClient.getLogin(), serverClient.getPassword());
    request.part("report", report);
    if (!request.ok()) {
      int responseCode = request.code();
      if (responseCode == 401) {
        throw new IllegalStateException(String.format(serverClient.getMessageWhenNotAuthorized(), CoreProperties.LOGIN, CoreProperties.PASSWORD));
      }
      if (responseCode == 403) {
        // SONAR-4397 Details are in response content
        throw new IllegalStateException(request.body());
      }
      throw new IllegalStateException(String.format("Fail to execute request [code=%s, url=%s]: %s", responseCode, url, request.body()));
    }
  }

  @VisibleForTesting
  void logSuccess(Logger logger) {
    if (analysisMode.isPreview()) {
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
      String url = baseUrl + "dashboard/index/" + project.getKey();
      logger.info("ANALYSIS SUCCESSFUL, you can browse {}", url);
      logger.info("Note that you will be able to access the updated dashboard once the server has processed the submitted analysis report.");
    }
  }
}
