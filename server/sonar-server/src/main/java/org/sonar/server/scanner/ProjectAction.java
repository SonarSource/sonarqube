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

package org.sonar.server.scanner;

import java.util.HashMap;
import java.util.Map;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.batch.protocol.input.FileData;
import org.sonar.batch.protocol.input.ProjectRepositories;
import org.sonarqube.ws.WsScanner.WsProjectResponse;

import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ProjectAction implements ScannerWsAction {

  private static final String PARAM_KEY = "key";
  private static final String PARAM_PROFILE = "profile";
  private static final String PARAM_PREVIEW = "preview";

  private final ProjectDataLoader projectDataLoader;

  public ProjectAction(ProjectDataLoader projectDataLoader) {
    this.projectDataLoader = projectDataLoader;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("project")
      .setDescription("Return project repository")
      .setSince("4.5")
      .setInternal(true)
      .setHandler(this);

    action
      .createParam(PARAM_KEY)
      .setRequired(true)
      .setDescription("Project or module key")
      .setExampleValue("org.codehaus.sonar:sonar");

    action
      .createParam(PARAM_PROFILE)
      .setDescription("Profile name")
      .setExampleValue("SonarQube Way");

    action
      .createParam(PARAM_PREVIEW)
      .setDescription("Preview mode or not")
      .setDefaultValue(false)
      .setBooleanPossibleValues();
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    ProjectRepositories data = projectDataLoader.load(ProjectDataQuery.create()
      .setModuleKey(wsRequest.mandatoryParam(PARAM_KEY))
      .setProfileName(wsRequest.param(PARAM_PROFILE))
      .setIssuesMode(wsRequest.mandatoryParamAsBoolean(PARAM_PREVIEW)));

    WsProjectResponse projectResponse = buildResponse(data);
    writeProtobuf(projectResponse, wsRequest, wsResponse);
  }

  private static WsProjectResponse buildResponse(ProjectRepositories data) {
    WsProjectResponse.Builder response = WsProjectResponse.newBuilder();
    setLastAnalysisDate(response, data);
    response.setTimestamp(data.timestamp());
    response.getMutableFileDataByModuleAndPatch()
      .putAll(buildFileDataByModuleAndPatch(data));
    response.getMutableSettingsByModule()
      .putAll(buildSettingsByModule(data));

    return response.build();
  }

  private static void setLastAnalysisDate(WsProjectResponse.Builder response, ProjectRepositories data) {
    if (data.lastAnalysisDate() != null) {
      response.setLastAnalysisDate(data.lastAnalysisDate().getTime());
    }
  }

  private static Map<String, WsProjectResponse.FileDataByPath> buildFileDataByModuleAndPatch(ProjectRepositories data) {
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
        toSettingsResponse(moduleSettingsEntry.getValue())
        );
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
    return WsProjectResponse.FileData.newBuilder()
      .setHash(fileData.hash())
      .setRevision(fileData.revision())
      .build();
  }

}
