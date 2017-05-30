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
package org.sonar.server.projectlink.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentLinkDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.WsProjectLinks;
import org.sonarqube.ws.WsProjectLinks.CreateWsResponse;
import org.sonarqube.ws.client.projectlinks.CreateWsRequest;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.core.util.Slug.slugify;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.projectlinks.ProjectLinksWsParameters.ACTION_CREATE;
import static org.sonarqube.ws.client.projectlinks.ProjectLinksWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.projectlinks.ProjectLinksWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.projectlinks.ProjectLinksWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.projectlinks.ProjectLinksWsParameters.PARAM_URL;

public class CreateAction implements ProjectLinksWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  private static final int LINK_NAME_MAX_LENGTH = 128;
  private static final int LINK_URL_MAX_LENGTH = 2048;
  private static final int LINK_TYPE_MAX_LENGTH = 20;

  public CreateAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
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
      .setDescription("Link name")
      .setExampleValue("Custom");

    action.createParam(PARAM_URL)
      .setRequired(true)
      .setDescription("Link url")
      .setExampleValue("http://example.com");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    CreateWsRequest searchWsRequest = toCreateWsRequest(request);
    CreateWsResponse createWsResponse = doHandle(searchWsRequest);
    writeProtobuf(createWsResponse, request, response);
  }

  private CreateWsResponse doHandle(CreateWsRequest createWsRequest) {
    validateRequest(createWsRequest);

    String name = createWsRequest.getName();
    String url = createWsRequest.getUrl();

    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto component = getComponentByUuidOrKey(dbSession, createWsRequest);

      userSession.checkComponentPermission(UserRole.ADMIN, component);

      ComponentLinkDto link = new ComponentLinkDto()
        .setComponentUuid(component.uuid())
        .setName(name)
        .setHref(url)
        .setType(nameToType(name));
      dbClient.componentLinkDao().insert(dbSession, link);

      dbSession.commit();
      return buildResponse(link);
    }
  }

  private static CreateWsResponse buildResponse(ComponentLinkDto link) {
    return CreateWsResponse.newBuilder().setLink(WsProjectLinks.Link.newBuilder()
      .setId(String.valueOf(link.getId()))
      .setName(link.getName())
      .setType(link.getType())
      .setUrl(link.getHref()))
      .build();
  }

  private ComponentDto getComponentByUuidOrKey(DbSession dbSession, CreateWsRequest request) {
    return componentFinder.getRootComponentByUuidOrKey(
      dbSession,
      request.getProjectId(),
      request.getProjectKey(),
      ComponentFinder.ParamNames.PROJECT_ID_AND_KEY);
  }

  private static CreateWsRequest toCreateWsRequest(Request request) {
    return new CreateWsRequest()
      .setProjectId(request.param(PARAM_PROJECT_ID))
      .setProjectKey(request.param(PARAM_PROJECT_KEY))
      .setName(request.mandatoryParam(PARAM_NAME))
      .setUrl(request.mandatoryParam(PARAM_URL));
  }

  private static void validateRequest(CreateWsRequest request) {
    checkArgument(request.getName().length() <= LINK_NAME_MAX_LENGTH, "Link name cannot be longer than %s characters", LINK_NAME_MAX_LENGTH);
    checkArgument(request.getUrl().length() <= LINK_URL_MAX_LENGTH, "Link url cannot be longer than %s characters", LINK_URL_MAX_LENGTH);
  }

  private static String nameToType(String name) {
    String slugified = slugify(name);
    return slugified.substring(0, Math.min(slugified.length(), LINK_TYPE_MAX_LENGTH));
  }
}
