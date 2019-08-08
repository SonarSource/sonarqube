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
package org.sonar.server.batch;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.scanner.protocol.input.FileData;
import org.sonar.scanner.protocol.input.MultiModuleProjectRepository;
import org.sonar.scanner.protocol.input.ProjectRepositories;
import org.sonar.scanner.protocol.input.SingleProjectRepository;
import org.sonarqube.ws.Batch.WsProjectResponse;
import org.sonarqube.ws.Batch.WsProjectResponse.FileData.Builder;

import static java.util.Optional.ofNullable;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PULL_REQUEST_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ProjectAction implements BatchWsAction {

  private static final String PARAM_KEY = "key";
  private static final String PARAM_PROFILE = "profile";
  private static final String PARAM_BRANCH = "branch";
  private static final String PARAM_PULL_REQUEST = "pullRequest";

  private final ProjectDataLoader projectDataLoader;

  public ProjectAction(ProjectDataLoader projectDataLoader) {
    this.projectDataLoader = projectDataLoader;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("project")
      .setDescription("Return project repository")
      .setResponseExample(getClass().getResource("project-example.json"))
      .setSince("4.5")
      .setChangelog(new Change("7.6", String.format("The use of module keys in parameter '%s' is deprecated", PARAM_KEY)))
      .setChangelog(new Change("7.6", "Stop returning settings"))
      .setChangelog(new Change("7.7", "Stop supporting preview mode, removed timestamp and last analysis date"))
      .setInternal(true)
      .setHandler(this);

    action
      .createParam(PARAM_KEY)
      .setRequired(true)
      .setDescription("Project or module key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action
      .createParam(PARAM_PROFILE)
      .setDescription("Profile name")
      .setExampleValue("SonarQube Way");

    action
      .createParam(PARAM_BRANCH)
      .setSince("6.6")
      .setDescription("Branch key")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001);

    action
      .createParam(PARAM_PULL_REQUEST)
      .setSince("7.1")
      .setDescription("Pull request id")
      .setExampleValue(KEY_PULL_REQUEST_EXAMPLE_001);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    ProjectRepositories data = projectDataLoader.load(ProjectDataQuery.create()
      .setProjectKey(wsRequest.mandatoryParam(PARAM_KEY))
      .setProfileName(wsRequest.param(PARAM_PROFILE))
      .setBranch(wsRequest.param(PARAM_BRANCH))
      .setPullRequest(wsRequest.param(PARAM_PULL_REQUEST)));

    WsProjectResponse projectResponse = buildResponse(data);
    writeProtobuf(projectResponse, wsRequest, wsResponse);
  }

  private static WsProjectResponse buildResponse(ProjectRepositories data) {
    WsProjectResponse.Builder response = WsProjectResponse.newBuilder();
    if (data instanceof SingleProjectRepository) {
      response.putAllFileDataByPath(buildFileDataByPath((SingleProjectRepository) data));
    } else {
      response.putAllFileDataByModuleAndPath(buildFileDataByModuleAndPath((MultiModuleProjectRepository) data));
    }

    return response.build();
  }

  private static Map<String, WsProjectResponse.FileDataByPath> buildFileDataByModuleAndPath(MultiModuleProjectRepository data) {
    return data.repositoriesByModule().entrySet()
      .stream()
      .map(entry -> Maps.immutableEntry(entry.getKey(), buildFileDataByPath(entry.getValue().fileData())))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static Map<String, WsProjectResponse.FileData> buildFileDataByPath(SingleProjectRepository data) {
    return data.fileData().entrySet()
      .stream()
      .collect(Collectors.toMap(Map.Entry::getKey, e -> toFileDataResponse(e.getValue())));
  }

  private static WsProjectResponse.FileDataByPath buildFileDataByPath(Map<String, FileData> fileDataByPath) {
    WsProjectResponse.FileDataByPath.Builder response = WsProjectResponse.FileDataByPath.newBuilder();
    fileDataByPath.forEach((key, value) -> response.putFileDataByPath(key, toFileDataResponse(value)));
    return response.build();
  }

  private static WsProjectResponse.FileData toFileDataResponse(FileData fileData) {
    Builder fileDataBuilder = WsProjectResponse.FileData.newBuilder();
    ofNullable(fileData.hash()).ifPresent(fileDataBuilder::setHash);
    ofNullable(fileData.revision()).ifPresent(fileDataBuilder::setRevision);
    return fileDataBuilder.build();
  }
}
