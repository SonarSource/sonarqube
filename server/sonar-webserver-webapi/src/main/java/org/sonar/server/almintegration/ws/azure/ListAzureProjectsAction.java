/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.almintegration.ws.azure;

import java.util.List;
import java.util.Optional;
import org.sonar.alm.client.azure.AzureDevOpsHttpClient;
import org.sonar.alm.client.azure.GsonAzureProject;
import org.sonar.alm.client.azure.GsonAzureProjectList;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.almintegration.ws.AlmIntegrationsWsAction;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.AlmIntegrations.AzureProject;
import org.sonarqube.ws.AlmIntegrations.ListAzureProjectsWsResponse;

import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ListAzureProjectsAction implements AlmIntegrationsWsAction {

  private static final String PARAM_ALM_SETTING = "almSetting";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final AzureDevOpsHttpClient azureDevOpsHttpClient;

  public ListAzureProjectsAction(DbClient dbClient, UserSession userSession, AzureDevOpsHttpClient azureDevOpsHttpClient) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.azureDevOpsHttpClient = azureDevOpsHttpClient;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("list_azure_projects")
      .setDescription("List Azure projects<br/>" +
        "Requires the 'Create Projects' permission")
      .setPost(false)
      .setSince("8.6")
      .setResponseExample(getClass().getResource("example-list_azure_projects.json"))
      .setHandler(this);

    action.createParam(PARAM_ALM_SETTING)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("DevOps Platform setting key");
  }

  @Override
  public void handle(Request request, Response response) {

    ListAzureProjectsWsResponse wsResponse = doHandle(request);
    writeProtobuf(wsResponse, request, response);
  }

  private ListAzureProjectsWsResponse doHandle(Request request) {

    try (DbSession dbSession = dbClient.openSession(false)) {
      userSession.checkLoggedIn().checkPermission(PROVISION_PROJECTS);

      String almSettingKey = request.mandatoryParam(PARAM_ALM_SETTING);
      String userUuid = requireNonNull(userSession.getUuid(), "User UUID is not null");
      AlmSettingDto almSettingDto = dbClient.almSettingDao().selectByKey(dbSession, almSettingKey)
        .orElseThrow(() -> new NotFoundException(String.format("DevOps Platform Setting '%s' not found", almSettingKey)));
      Optional<AlmPatDto> almPatDto = dbClient.almPatDao().selectByUserAndAlmSetting(dbSession, userUuid, almSettingDto);
      String pat = almPatDto.map(AlmPatDto::getPersonalAccessToken).orElseThrow(() -> new IllegalArgumentException("No personal access token found"));

      String url = requireNonNull(almSettingDto.getUrl(), "URL cannot be null");
      GsonAzureProjectList projectList = azureDevOpsHttpClient.getProjects(url, pat);

      List<AzureProject> values = projectList.getValues().stream()
        .map(ListAzureProjectsAction::toAzureProject)
        .sorted(comparing(AzureProject::getName, String::compareToIgnoreCase))
        .toList();
      ListAzureProjectsWsResponse.Builder builder = ListAzureProjectsWsResponse.newBuilder()
        .addAllProjects(values);
      return builder.build();
    }
  }

  private static AzureProject toAzureProject(GsonAzureProject project) {
    return AzureProject.newBuilder()
      .setName(project.getName())
      .setDescription(Optional.ofNullable(project.getDescription()).orElse(""))
      .build();
  }

}
