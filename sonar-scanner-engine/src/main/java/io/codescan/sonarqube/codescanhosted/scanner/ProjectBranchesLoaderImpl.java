/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package io.codescan.sonarqube.codescanhosted.scanner;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.impl.utils.ScannerUtils;
import org.sonar.scanner.http.ScannerWsClient;
import org.sonar.scanner.protocol.GsonHelper;
import org.sonar.scanner.scan.branch.BranchInfo;
import org.sonar.scanner.scan.branch.BranchType;
import org.sonar.scanner.scan.branch.ProjectBranches;
import org.sonar.scanner.scan.branch.ProjectBranchesLoader;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;

public class ProjectBranchesLoaderImpl implements ProjectBranchesLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectBranchesLoaderImpl.class);

    private final ScannerWsClient client;

    public ProjectBranchesLoaderImpl(ScannerWsClient client) {
        this.client = client;
    }

    @Override
    public ProjectBranches load(String projectKey) {
        return new ProjectBranches(this.getProjectBranches(projectKey));
    }

    private List<BranchInfo> getProjectBranches(String projectKey) {
        List<BranchInfo> ret = Collections.emptyList();
        GetRequest request = new GetRequest(getUrl(projectKey));
        try {
            WsResponse response = this.client.call(request);
            ret = parse(response);
        } catch (Exception e) {
            LOG.debug("Error parsing branch response: " + e.getMessage());
        }

        return ret;
    }

    private static String getUrl(String projectKey) {
        return "/api/project_branches/list?project=" + ScannerUtils.encodeForUrl(projectKey);
    }

    private static List<BranchInfo> parse(WsResponse response) {
        Reader content = response.contentReader();
        List<BranchInfo> ret;
        try {
            WsProjectBranchesResponse branchResponse = GsonHelper.create()
                    .fromJson(content, WsProjectBranchesResponse.class);
            ret = branchResponse.branches
                    .stream()
                    .map(branch -> new BranchInfo(branch.name, parseBranchType(branch.type), branch.isMain,
                            branch.mergeBranch))
                    .collect(Collectors.toList());
        } finally {
            IOUtils.closeQuietly(content);
        }
        return ret;
    }

    private static BranchType parseBranchType(String branchType) {
        switch (branchType) {
            case "LONG":
            case "BRANCH":
                return BranchType.BRANCH;
            case "SHORT":
            case "PULL_REQUEST":
                return BranchType.PULL_REQUEST;
            default:
                throw new IllegalArgumentException("Invalid branch type: " + branchType);
        }
    }

    private static class WsProjectBranchesResponse {

        private List<WsProjectBranch> branches = new ArrayList<>();
    }

    private static class WsProjectBranch {

        private String name;
        private String type;
        private boolean isMain;
        private String mergeBranch;
    }
}
