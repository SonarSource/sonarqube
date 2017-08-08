/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.scanner.scan;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonar.scanner.protocol.GsonHelper;
import org.sonar.scanner.util.ScannerUtils;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;

public class DefaultProjectBranchesLoader implements ProjectBranchesLoader {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultProjectBranchesLoader.class);
  private static final String PROJECT_BRANCHES_URL = "/api/project_branches/list";

  private final ScannerWsClient wsClient;

  public DefaultProjectBranchesLoader(ScannerWsClient wsClient) {
    this.wsClient = wsClient;
  }

  @Override
  public ProjectBranches load(String projectKey, Configuration settings) {
    GetRequest request = new GetRequest(getUrl(projectKey));
    try {
      WsResponse call = wsClient.call(request);
      ProjectBranches.create(settings, parseResponse(call));
    } catch (RuntimeException e) {
      LOG.debug("Project branches not available - continuing without it");
    } catch (IOException e) {
      LOG.debug("Project branches could not be parsed - continuing without it");
    }
    return ProjectBranches.create(settings, Collections.emptyList());
  }

  private static String getUrl(String projectKey) {
    return PROJECT_BRANCHES_URL + "?project=" + ScannerUtils.encodeForUrl(projectKey);
  }

  private static class WsProjectBranch {
    private String name;
    private String type;
  }

  private static class WsProjectBranchesResponse {
    private List<WsProjectBranch> branches = new ArrayList<>();
  }

  private static List<ProjectBranches.BranchInfo> parseResponse(WsResponse response) throws IOException {
    try (Reader reader = response.contentReader()) {
      WsProjectBranchesResponse branchesResponse = GsonHelper.create().fromJson(reader, WsProjectBranchesResponse.class);
      return branchesResponse.branches.stream()
        .map(branch -> new ProjectBranches.BranchInfo(branch.name, branch.type.equals("LONG")))
        .collect(Collectors.toList());
    }
  }
}
