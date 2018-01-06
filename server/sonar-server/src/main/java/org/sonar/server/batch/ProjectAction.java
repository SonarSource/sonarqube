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
package org.sonar.server.batch;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.scanner.protocol.input.FileData;
import org.sonar.scanner.protocol.input.ProjectRepositories;
import org.sonarqube.ws.Batch.WsProjectResponse;
import org.sonarqube.ws.Batch.WsProjectResponse.FileData.Builder;

import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ProjectAction implements BatchWsAction {

  private static final String PARAM_KEY = "key";
  private static final String PARAM_PROFILE = "profile";
  private static final String PARAM_ISSUES_MODE = "issues_mode";
  private static final String PARAM_BRANCH = "branch";

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
      .createParam(PARAM_ISSUES_MODE)
      .setDescription("Issues mode or not")
      .setDefaultValue(false)
      .setBooleanPossibleValues();

    action
      .createParam(PARAM_BRANCH)
      .setSince("6.6")
      .setDescription("Branch key")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    ProjectRepositories data = projectDataLoader.load(ProjectDataQuery.create()
      .setModuleKey(wsRequest.mandatoryParam(PARAM_KEY))
      .setProfileName(wsRequest.param(PARAM_PROFILE))
      .setIssuesMode(wsRequest.mandatoryParamAsBoolean(PARAM_ISSUES_MODE))
      .setBranch(wsRequest.param(PARAM_BRANCH)));

    WsProjectResponse projectResponse = buildResponse(data);
    writeProtobuf(projectResponse, wsRequest, wsResponse);
  }

  private static WsProjectResponse buildResponse(ProjectRepositories data) {
    WsProjectResponse.Builder response = WsProjectResponse.newBuilder();
    setNullable(data.lastAnalysisDate(), response::setLastAnalysisDate, Date::getTime);
    response.setTimestamp(data.timestamp());
    response.getMutableFileDataByModuleAndPath()
      .putAll(buildFileDataByModuleAndPath(data));
    response.getMutableSettingsByModule()
      .putAll(buildSettingsByModule(data));

    return response.build();
  }

  private static Map<String, WsProjectResponse.FileDataByPath> buildFileDataByModuleAndPath(ProjectRepositories data) {
    Map<String, WsProjectResponse.FileDataByPath> fileDataByModuleAndPathResponse = new HashMap<>();
    for (Map.Entry<String, Map<String, FileData>> moduleAndFileDataByPathEntry : data.fileDataByModuleAndPath().entrySet()) {
      fileDataByModuleAndPathResponse.put(
        moduleAndFileDataByPathEntry.getKey(),
        buildFileDataByPath(moduleAndFileDataByPathEntry.getValue()));
    }

    return fileDataByModuleAndPathResponse;
  }

  private static WsProjectResponse.FileDataByPath buildFileDataByPath(Map<String, FileData> fileDataByPath) {
    WsProjectResponse.FileDataByPath.Builder response = WsProjectResponse.FileDataByPath.newBuilder();
    Map<String, WsProjectResponse.FileData> fileDataByPathResponse = response.getMutableFileDataByPath();

    for (Map.Entry<String, FileData> pathFileDataEntry : fileDataByPath.entrySet()) {
      fileDataByPathResponse.put(
        pathFileDataEntry.getKey(),
        toFileDataResponse(pathFileDataEntry.getValue()));
    }

    return response.build();
  }

  private static Map<String, WsProjectResponse.Settings> buildSettingsByModule(ProjectRepositories data) {
    Map<String, WsProjectResponse.Settings> settingsByModuleResponse = new HashMap<>();
    for (Map.Entry<String, Map<String, String>> moduleSettingsEntry : data.settings().entrySet()) {
      settingsByModuleResponse.put(
        moduleSettingsEntry.getKey(),
        toSettingsResponse(moduleSettingsEntry.getValue()));
    }

    return settingsByModuleResponse;
  }

  private static WsProjectResponse.Settings toSettingsResponse(Map<String, String> settings) {
    WsProjectResponse.Settings.Builder settingsResponse = WsProjectResponse.Settings
      .newBuilder();
    settingsResponse
      .getMutableSettings()
      .putAll(settings);

    return settingsResponse.build();
  }

  private static WsProjectResponse.FileData toFileDataResponse(FileData fileData) {
    Builder fileDataBuilder = WsProjectResponse.FileData.newBuilder();
    setNullable(fileData.hash(), fileDataBuilder::setHash);
    setNullable(fileData.revision(), fileDataBuilder::setRevision);
    return fileDataBuilder.build();
  }
}
