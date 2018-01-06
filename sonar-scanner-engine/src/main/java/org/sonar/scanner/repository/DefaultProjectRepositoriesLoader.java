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
package org.sonar.scanner.repository;

import com.google.common.base.Throwables;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonar.scanner.util.ScannerUtils;
import org.sonarqube.ws.Batch;
import org.sonarqube.ws.Batch.WsProjectResponse;
import org.sonarqube.ws.Batch.WsProjectResponse.FileDataByPath;
import org.sonarqube.ws.Batch.WsProjectResponse.Settings;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsResponse;

public class DefaultProjectRepositoriesLoader implements ProjectRepositoriesLoader {
  private static final Logger LOG = LoggerFactory.getLogger(DefaultProjectRepositoriesLoader.class);
  private static final String BATCH_PROJECT_URL = "/batch/project.protobuf";
  private ScannerWsClient wsClient;

  public DefaultProjectRepositoriesLoader(ScannerWsClient wsClient) {
    this.wsClient = wsClient;
  }

  @Override
  public ProjectRepositories load(String projectKey, boolean issuesMode, @Nullable String branchBase) {
    GetRequest request = new GetRequest(getUrl(projectKey, issuesMode, branchBase));
    try (WsResponse response = wsClient.call(request)) {
      InputStream is = response.contentStream();
      return processStream(is, projectKey);
    } catch (RuntimeException e) {
      if (shouldThrow(e)) {
        throw e;
      }

      LOG.debug("Project repository not available - continuing without it");
      return new ProjectRepositories();
    }
  }

  private static String getUrl(String projectKey, boolean issuesMode, @Nullable String branchBase) {
    StringBuilder builder = new StringBuilder();

    builder.append(BATCH_PROJECT_URL)
      .append("?key=").append(ScannerUtils.encodeForUrl(projectKey));
    if (issuesMode) {
      builder.append("&issues_mode=true");
    }
    if (branchBase != null) {
      builder.append("&branch=").append(branchBase);
    }
    return builder.toString();
  }

  private static boolean shouldThrow(Exception e) {
    for (Throwable t : Throwables.getCausalChain(e)) {
      if (t instanceof HttpException) {
        HttpException http = (HttpException) t;
        return http.code() != HttpURLConnection.HTTP_NOT_FOUND;
      }
      if (t instanceof MessageException) {
        return true;
      }
    }

    return false;
  }

  private static ProjectRepositories processStream(InputStream is, String projectKey) {
    try {
      WsProjectResponse response = WsProjectResponse.parseFrom(is);

      Table<String, String, FileData> fileDataTable = HashBasedTable.create();
      Table<String, String, String> settings = HashBasedTable.create();

      Map<String, Settings> settingsByModule = response.getSettingsByModule();
      for (Map.Entry<String, Settings> e1 : settingsByModule.entrySet()) {
        for (Map.Entry<String, String> e2 : e1.getValue().getSettings().entrySet()) {
          settings.put(e1.getKey(), e2.getKey(), e2.getValue());
        }
      }

      Map<String, FileDataByPath> fileDataByModuleAndPath = response.getFileDataByModuleAndPath();
      for (Map.Entry<String, FileDataByPath> e1 : fileDataByModuleAndPath.entrySet()) {
        for (Map.Entry<String, Batch.WsProjectResponse.FileData> e2 : e1.getValue().getFileDataByPath().entrySet()) {
          FileData fd = new FileData(e2.getValue().getHash(), e2.getValue().getRevision());
          fileDataTable.put(e1.getKey(), e2.getKey(), fd);
        }
      }

      return new ProjectRepositories(settings, fileDataTable, new Date(response.getLastAnalysisDate()));
    } catch (IOException e) {
      throw new IllegalStateException("Couldn't load project repository for " + projectKey, e);
    } finally {
      IOUtils.closeQuietly(is);
    }
  }
}
