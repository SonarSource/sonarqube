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
package org.sonar.server.projectlink.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectLinkDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.ProjectLinks;
import org.sonarqube.ws.ProjectLinks.CreateWsResponse;

import static org.sonar.core.util.Slug.slugify;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.projectlink.ws.ProjectLinksWs.checkProject;
import static org.sonar.server.projectlink.ws.ProjectLinksWsParameters.ACTION_CREATE;
import static org.sonar.server.projectlink.ws.ProjectLinksWsParameters.PARAM_NAME;
import static org.sonar.server.projectlink.ws.ProjectLinksWsParameters.PARAM_PROJECT_ID;
import static org.sonar.server.projectlink.ws.ProjectLinksWsParameters.PARAM_PROJECT_KEY;
import static org.sonar.server.projectlink.ws.ProjectLinksWsParameters.PARAM_URL;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class CreateAction implements ProjectLinksWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;
  private final UuidFactory uuidFactory;

  private static final int LINK_NAME_MAX_LENGTH = 128;
  private static final int LINK_URL_MAX_LENGTH = 2048;
  private static final int LINK_TYPE_MAX_LENGTH = 20;

  public CreateAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder, UuidFactory uuidFactory) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
    this.uuidFactory = uuidFactory;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_CREATE)
      .setDescription("Create a new project link.<br>" +
        "Requires 'Administer' permission on the specified project, " +
        "or global 'Administer' permission.")
      .setHandler(this)
      .setPost(true)
      .setResponseExample(getClass().getResource("create-example.json"))
      .setSince("6.1");

    action.createParam(PARAM_PROJECT_ID)
      .setDescription("Project id")
      .setExampleValue(UUID_EXAMPLE_01);

    action.createParam(PARAM_PROJECT_KEY)
      .setDescription("Project key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.createParam(PARAM_NAME)
      .setRequired(true)
      .setMaximumLength(LINK_NAME_MAX_LENGTH)
      .setDescription("Link name")
      .setExampleValue("Custom");

    action.createParam(PARAM_URL)
      .setRequired(true)
      .setMaximumLength(LINK_URL_MAX_LENGTH)
      .setDescription("Link url")
      .setExampleValue("http://example.com");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    CreateRequest searchWsRequest = toCreateWsRequest(request);
    CreateWsResponse createWsResponse = doHandle(searchWsRequest);
    writeProtobuf(createWsResponse, request, response);
  }

  private CreateWsResponse doHandle(CreateRequest createWsRequest) {
    String name = createWsRequest.getName();
    String url = createWsRequest.getUrl();

    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto component = checkProject(getComponentByUuidOrKey(dbSession, createWsRequest));

      userSession.checkComponentPermission(UserRole.ADMIN, component);

      ProjectLinkDto link = new ProjectLinkDto()
        .setUuid(uuidFactory.create())
        .setProjectUuid(component.uuid())
        .setName(name)
        .setHref(url)
        .setType(nameToType(name));
      dbClient.projectLinkDao().insert(dbSession, link);

      dbSession.commit();
      return buildResponse(link);
    }
  }

  private static CreateWsResponse buildResponse(ProjectLinkDto link) {
    return CreateWsResponse.newBuilder().setLink(ProjectLinks.Link.newBuilder()
      .setId(String.valueOf(link.getUuid()))
      .setName(link.getName())
      .setType(link.getType())
      .setUrl(link.getHref()))
      .build();
  }

  private ComponentDto getComponentByUuidOrKey(DbSession dbSession, CreateRequest request) {
    return componentFinder.getByUuidOrKey(
      dbSession,
      request.getProjectId(),
      request.getProjectKey(),
      ComponentFinder.ParamNames.PROJECT_ID_AND_KEY);
  }

  private static CreateRequest toCreateWsRequest(Request request) {
    return new CreateRequest()
      .setProjectId(request.param(PARAM_PROJECT_ID))
      .setProjectKey(request.param(PARAM_PROJECT_KEY))
      .setName(request.mandatoryParam(PARAM_NAME))
      .setUrl(request.mandatoryParam(PARAM_URL));
  }

  private static String nameToType(String name) {
    String slugified = slugify(name);
    return slugified.substring(0, Math.min(slugified.length(), LINK_TYPE_MAX_LENGTH));
  }

  private static class CreateRequest {

    private String name;
    private String projectId;
    private String projectKey;
    private String url;

    public CreateRequest setName(String name) {
      this.name = name;
      return this;
    }

    public String getName() {
      return name;
    }

    public CreateRequest setProjectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public String getProjectId() {
      return projectId;
    }

    public CreateRequest setProjectKey(String projectKey) {
      this.projectKey = projectKey;
      return this;
    }

    public String getProjectKey() {
      return projectKey;
    }

    public CreateRequest setUrl(String url) {
      this.url = url;
      return this;
    }

    public String getUrl() {
      return url;
    }
  }
}
