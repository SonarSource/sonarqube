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
package org.sonar.scanner.repository;

import com.google.common.base.Throwables;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonar.scanner.util.ScannerUtils;
import org.sonarqube.ws.Batch.WsProjectResponse;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsResponse;

public class DefaultProjectRepositoriesLoader implements ProjectRepositoriesLoader {
  private static final Logger LOG = LoggerFactory.getLogger(DefaultProjectRepositoriesLoader.class);
  private static final String BATCH_PROJECT_URL = "/batch/project.protobuf";
  private final ScannerWsClient wsClient;

  public DefaultProjectRepositoriesLoader(ScannerWsClient wsClient) {
    this.wsClient = wsClient;
  }

  @Override
  public ProjectRepositories load(String projectKey, @Nullable String branchBase) {
    GetRequest request = new GetRequest(getUrl(projectKey, branchBase));
    try (WsResponse response = wsClient.call(request)) {
      try (InputStream is = response.contentStream()) {
        return processStream(is);
      } catch (IOException e) {
        throw new IllegalStateException("Couldn't load project repository for " + projectKey, e);
      }
    } catch (RuntimeException e) {
      if (shouldThrow(e)) {
        throw e;
      }

      LOG.debug("Project repository not available - continuing without it");
      return new SingleProjectRepository();
    }
  }

  private static String getUrl(String projectKey, @Nullable String branchBase) {
    StringBuilder builder = new StringBuilder();

    builder.append(BATCH_PROJECT_URL)
      .append("?key=").append(ScannerUtils.encodeForUrl(projectKey));
    if (branchBase != null) {
      builder.append("&branch=").append(branchBase);
    }
    return builder.toString();
  }

  private static boolean shouldThrow(Exception e) {
    for (Throwable t : Throwables.getCausalChain(e)) {
      if (t instanceof HttpException && ((HttpException) t).code() == HttpURLConnection.HTTP_NOT_FOUND) {
        return false;
      }
    }

    return true;
  }

  private static ProjectRepositories processStream(InputStream is) throws IOException {
    WsProjectResponse response = WsProjectResponse.parseFrom(is);
    if (response.getFileDataByModuleAndPathCount() == 0) {
      return new SingleProjectRepository(constructFileDataMap(response.getFileDataByPathMap()));
    } else {
      final Map<String, SingleProjectRepository> repositoriesPerModule = new HashMap<>();
      response.getFileDataByModuleAndPathMap().keySet().forEach(moduleKey -> {
        WsProjectResponse.FileDataByPath filePaths = response.getFileDataByModuleAndPathMap().get(moduleKey);
        repositoriesPerModule.put(moduleKey, new SingleProjectRepository(
          constructFileDataMap(filePaths.getFileDataByPathMap())));
      });
      return new MultiModuleProjectRepository(repositoriesPerModule);
    }

  }

  private static Map<String, FileData> constructFileDataMap(Map<String, WsProjectResponse.FileData> content) {
    Map<String, FileData> fileDataMap = new HashMap<>();
    content.forEach((key, value) -> {
      FileData fd = new FileData(value.getHash(), value.getRevision());
      fileDataMap.put(key, fd);
    });

    return fileDataMap;
  }
}
