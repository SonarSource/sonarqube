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
package org.sonar.batch.phases;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.batch.bootstrap.AnalysisMode;
import org.sonar.batch.bootstrap.ServerClient;

public class UpdateStatusJob implements BatchComponent {

  private static final Logger LOG = LoggerFactory.getLogger(UpdateStatusJob.class);

  private ServerClient server;
  // TODO remove this component
  private Snapshot snapshot;
  private Settings settings;
  private Project project;
  private AnalysisMode analysisMode;

  public UpdateStatusJob(Settings settings, ServerClient server,
    Project project, Snapshot snapshot, AnalysisMode analysisMode) {
    this.server = server;
    this.project = project;
    this.snapshot = snapshot;
    this.settings = settings;
    this.analysisMode = analysisMode;
  }

  public void execute() {
    uploadReport();
    logSuccess(LoggerFactory.getLogger(getClass()));
  }

  @VisibleForTesting
  void uploadReport() {
    if (analysisMode.isPreview()) {
      // If this is a preview analysis then we should not upload reports
      return;
    }
    String url = "/batch_bootstrap/evict?project=" + project.getId();
    try {
      LOG.debug("Upload report");
      server.request(url, "POST");
    } catch (Exception e) {
      throw new IllegalStateException("Unable to evict preview database: " + url, e);
    }
    url = "/batch/upload_report?project=" + project.getEffectiveKey() + "&snapshot=" + snapshot.getId();
    try {
      LOG.debug("Publish results");
      server.request(url, "POST");
    } catch (Exception e) {
      throw new IllegalStateException("Unable to publish results: " + url, e);
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
        baseUrl = server.getURL();
      }
      if (!baseUrl.endsWith("/")) {
        baseUrl += "/";
      }
      String url = baseUrl + "dashboard/index/" + project.getKey();
      logger.info("ANALYSIS SUCCESSFUL, you will be able to browse it at {}", url);
    }
  }
}
