/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonar.scanner.repository.settings.SettingsLoader;
import org.sonar.scanner.util.BatchUtils;
import org.sonarqube.ws.WsBatch;
import org.sonarqube.ws.WsBatch.WsProjectResponse;
import org.sonarqube.ws.WsBatch.WsProjectResponse.FileDataByPath;
import org.sonarqube.ws.WsComponents;
import org.sonarqube.ws.WsComponents.Component;
import org.sonarqube.ws.WsComponents.TreeWsResponse;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsResponse;

public class DefaultServerSideProjectDataLoader implements ServerSideProjectDataLoader {
  private static final Logger LOG = LoggerFactory.getLogger(DefaultServerSideProjectDataLoader.class);
  private static final String BATCH_PROJECT_URL = "/batch/project.protobuf";
  private final ScannerWsClient wsClient;
  private final SettingsLoader settingsLoader;

  public DefaultServerSideProjectDataLoader(ScannerWsClient wsClient, SettingsLoader settingsLoader) {
    this.wsClient = wsClient;
    this.settingsLoader = settingsLoader;
  }

  @Override
  public ServerSideProjectData load(String projectKey, boolean issuesMode) {

    Set<String> moduleKeys;
    try {
      moduleKeys = loadModuleKeys(projectKey);
    } catch (HttpException e) {
      if (e.code() == HttpURLConnection.HTTP_NOT_FOUND) {
        LOG.debug("No project found for key '{}' on server - continuing without it", projectKey);
        return new ServerSideProjectData();
      }
      throw e;
    }

    Table<String, String, String> settingsPerModule = HashBasedTable.create();
    loadSettings(projectKey, settingsPerModule);
    for (String moduleKey : moduleKeys) {
      loadSettings(moduleKey, settingsPerModule);
    }

    Table<String, String, FileData> fileDataTable = HashBasedTable.create();
    Date lastAnalysisDate;
    GetRequest request = new GetRequest(getBatchProjectUrl(projectKey, issuesMode));
    try (WsResponse response = wsClient.call(request); InputStream stream = response.contentStream()) {
      WsProjectResponse projectResponse = WsProjectResponse.parseFrom(stream);

      lastAnalysisDate = new Date(projectResponse.getLastAnalysisDate());

      Map<String, FileDataByPath> fileDataByModuleAndPath = projectResponse.getFileDataByModuleAndPath();
      for (Map.Entry<String, FileDataByPath> e1 : fileDataByModuleAndPath.entrySet()) {
        for (Map.Entry<String, WsBatch.WsProjectResponse.FileData> e2 : e1.getValue().getFileDataByPath().entrySet()) {
          FileData fd = new FileData(e2.getValue().getHash(), e2.getValue().getRevision());
          fileDataTable.put(e1.getKey(), e2.getKey(), fd);
        }
      }

    } catch (Exception e) {
      throw new IllegalStateException("Unable to file metadata", e);
    }

    return new ServerSideProjectData(moduleKeys, settingsPerModule, fileDataTable, lastAnalysisDate);
  }

  private void loadSettings(String projectKey, Table<String, String, String> settingsPerModule) {
    for (Map.Entry<String, String> entry : settingsLoader.load(projectKey).entrySet()) {
      settingsPerModule.put(projectKey, entry.getKey(), entry.getValue());
    }
  }

  private Set<String> loadModuleKeys(String projectKey) {
    Set<String> moduleKeys = new LinkedHashSet<>();
    TreeWsResponse treeResponse = null;
    int page = 1;
    do {
      GetRequest componentRequest = new GetRequest("/api/components/tree.protobuf?qualifiers=BRC&ps=500&p=" + page + "&baseComponentKey=" + BatchUtils.encodeForUrl(projectKey));
      try (WsResponse response = wsClient.call(componentRequest); InputStream stream = response.contentStream()) {
        treeResponse = WsComponents.TreeWsResponse.parseFrom(stream);
        treeResponse.getComponentsList().stream().map(Component::getKey).forEach(moduleKeys::add);
      } catch (IOException e) {
        throw new IllegalStateException("Unable to load component tree", e);
      }
      page++;
    } while (treeResponse.getPaging().getPageIndex() < (treeResponse.getPaging().getTotal() / treeResponse.getPaging().getPageSize() + 1));
    return moduleKeys;
  }

  private static String getBatchProjectUrl(String projectKey, boolean issuesMode) {
    StringBuilder builder = new StringBuilder();

    builder.append(BATCH_PROJECT_URL)
      .append("?key=").append(BatchUtils.encodeForUrl(projectKey));
    if (issuesMode) {
      builder.append("&issues_mode=true");
    }
    return builder.toString();
  }

}
