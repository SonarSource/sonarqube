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
package org.sonar.server.almintegration.ws.bitbucketserver;

import java.util.List;
import java.util.Optional;
import org.sonar.alm.client.bitbucketserver.BitbucketServerRestClient;
import org.sonar.alm.client.bitbucketserver.Project;
import org.sonar.alm.client.bitbucketserver.ProjectList;
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
import org.sonarqube.ws.AlmIntegrations.AlmProject;
import org.sonarqube.ws.AlmIntegrations.ListBitbucketserverProjectsWsResponse;
import org.sonarqube.ws.Common;

import static java.util.Objects.requireNonNull;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ListBitbucketServerProjectsAction implements AlmIntegrationsWsAction {

  private static final String PARAM_ALM_SETTING = "almSetting";
  private static final String PARAM_START = "start";
  private static final String PARAM_PAGE_SIZE = "pageSize";
  private static final int DEFAULT_PAGE_SIZE = 25;
  private static final int MAX_PAGE_SIZE = 100;

  private final DbClient dbClient;
  private final UserSession userSession;
  private final BitbucketServerRestClient bitbucketServerRestClient;

  public ListBitbucketServerProjectsAction(DbClient dbClient, UserSession userSession, BitbucketServerRestClient bitbucketServerRestClient) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.bitbucketServerRestClient = bitbucketServerRestClient;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("list_bitbucketserver_projects")
      .setDescription("List the Bitbucket Server projects<br/>" +
        "Requires the 'Create Projects' permission")
      .setPost(false)
      .setSince("8.2")
      .setResponseExample(getClass().getResource("example-list_bitbucketserver_projects.json"))
      .setHandler(this);

    action.createParam(PARAM_ALM_SETTING)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("DevOps Platform setting key");

    action.createParam(PARAM_START)
      .setExampleValue(2154)
      .setDescription("Start number for the page (inclusive). If not passed, the first page is assumed.");

    action.createParam(PARAM_PAGE_SIZE)
      .setDefaultValue(DEFAULT_PAGE_SIZE)
      .setMaximumValue(MAX_PAGE_SIZE)
      .setDescription("Number of items to return.");
  }

  @Override
  public void handle(Request request, Response response) {
    ListBitbucketserverProjectsWsResponse wsResponse = doHandle(request);
    writeProtobuf(wsResponse, request, response);
  }

  private ListBitbucketserverProjectsWsResponse doHandle(Request request) {
    userSession.checkLoggedIn().checkPermission(PROVISION_PROJECTS);

    String almSettingKey = request.mandatoryParam(PARAM_ALM_SETTING);
    Integer start = request.paramAsInt(PARAM_START);
    int pageSize = Optional.ofNullable(request.paramAsInt(PARAM_PAGE_SIZE))
      // non-positive should fallback to default
      .filter(ps -> ps > 0)
      .orElse(DEFAULT_PAGE_SIZE);
    String userUuid = requireNonNull(userSession.getUuid(), "User UUID is not null");

    try (DbSession dbSession = dbClient.openSession(false)) {
      AlmSettingDto almSettingDto = dbClient.almSettingDao().selectByKey(dbSession, almSettingKey)
        .orElseThrow(() -> new NotFoundException(String.format("DevOps Platform Setting '%s' not found", almSettingKey)));
      Optional<AlmPatDto> almPatDto = dbClient.almPatDao().selectByUserAndAlmSetting(dbSession, userUuid, almSettingDto);
      String pat = almPatDto.map(AlmPatDto::getPersonalAccessToken).orElseThrow(() -> new IllegalArgumentException("No personal access token found"));

      String url = requireNonNull(almSettingDto.getUrl(), "URL cannot be null");
      ProjectList projectList = bitbucketServerRestClient.getProjects(url, pat, start, pageSize);
      List<AlmProject> values = projectList.getValues().stream().map(ListBitbucketServerProjectsAction::toAlmProject).toList();

      return ListBitbucketserverProjectsWsResponse.newBuilder()
        .setIsLastPage(projectList.isLastPage())
        .setNextPageStart(projectList.getNextPageStart())
        .setPaging(Common.Paging.newBuilder()
          .setPageSize(projectList.getSize())
          .build())
        .addAllProjects(values)
        .build();
    }
  }

  private static AlmProject toAlmProject(Project project) {
    return AlmProject.newBuilder()
      .setKey(project.getKey())
      .setName(project.getName())
      .build();
  }

}
