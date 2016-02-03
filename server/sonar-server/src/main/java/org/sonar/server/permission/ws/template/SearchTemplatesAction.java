/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.permission.ws.template;

import org.sonar.api.i18n.I18n;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.core.permission.ProjectPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.WsPermissions;
import org.sonarqube.ws.WsPermissions.Permission;
import org.sonarqube.ws.WsPermissions.PermissionTemplate;
import org.sonarqube.ws.WsPermissions.SearchTemplatesWsResponse;
import org.sonarqube.ws.WsPermissions.SearchTemplatesWsResponse.TemplateIdQualifier;
import org.sonarqube.ws.client.permission.SearchTemplatesWsRequest;

import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchTemplatesAction implements PermissionsWsAction {
  private static final String PROPERTY_PREFIX = "projects_role.";
  private static final String DESCRIPTION_SUFFIX = ".desc";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final I18n i18n;
  private final SearchTemplatesDataLoader dataLoader;

  public SearchTemplatesAction(DbClient dbClient, UserSession userSession, I18n i18n, SearchTemplatesDataLoader dataLoader) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.i18n = i18n;
    this.dataLoader = dataLoader;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction("search_templates")
      .setDescription("List permission templates.<br />" +
        "It requires to be authenticated.")
      .setResponseExample(getClass().getResource("search_templates-example.json"))
      .setSince("5.2")
      .addSearchQuery("defau", "permission template names")
      .setHandler(this);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    userSession.checkLoggedIn();

    SearchTemplatesWsResponse searchTemplatesWsResponse = doHandle(toSearchTemplatesWsRequest(wsRequest));
    writeProtobuf(searchTemplatesWsResponse, wsRequest, wsResponse);
  }

  private SearchTemplatesWsResponse doHandle(SearchTemplatesWsRequest wsRequest) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      SearchTemplatesData data = dataLoader.load(wsRequest);
      return buildResponse(data);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private static SearchTemplatesWsRequest toSearchTemplatesWsRequest(Request request) {
    return new SearchTemplatesWsRequest().setQuery(request.param(Param.TEXT_QUERY));
  }

  private WsPermissions.SearchTemplatesWsResponse buildResponse(SearchTemplatesData data) {
    SearchTemplatesWsResponse.Builder response = SearchTemplatesWsResponse.newBuilder();

    buildTemplatesResponse(response, data);
    buildDefaultTemplatesResponse(response, data);
    buildPermissionsResponse(response);

    return response.build();
  }

  private static void buildDefaultTemplatesResponse(SearchTemplatesWsResponse.Builder response, SearchTemplatesData data) {
    TemplateIdQualifier.Builder templateUuidQualifierBuilder = TemplateIdQualifier.newBuilder();
    for (DefaultPermissionTemplateFinder.TemplateUuidQualifier templateUuidQualifier : data.defaultTempltes()) {
      response.addDefaultTemplates(templateUuidQualifierBuilder
        .clear()
        .setQualifier(templateUuidQualifier.getQualifier())
        .setTemplateId(templateUuidQualifier.getTemplateUuid()));
    }
  }

  private static void buildTemplatesResponse(WsPermissions.SearchTemplatesWsResponse.Builder response, SearchTemplatesData data) {
    Permission.Builder permissionResponse = Permission.newBuilder();
    PermissionTemplate.Builder templateBuilder = PermissionTemplate.newBuilder();

    for (PermissionTemplateDto templateDto : data.templates()) {
      templateBuilder
        .clear()
        .setId(templateDto.getUuid())
        .setName(templateDto.getName())
        .setCreatedAt(formatDateTime(templateDto.getCreatedAt()))
        .setUpdatedAt(formatDateTime(templateDto.getUpdatedAt()));
      if (templateDto.getKeyPattern() != null) {
        templateBuilder.setProjectKeyPattern(templateDto.getKeyPattern());
      }
      if (templateDto.getDescription() != null) {
        templateBuilder.setDescription(templateDto.getDescription());
      }
      for (String permission : data.permissions(templateDto.getId())) {
        templateBuilder.addPermissions(
          permissionResponse
            .clear()
            .setKey(permission)
            .setUsersCount(data.userCount(templateDto.getId(), permission))
            .setGroupsCount(data.groupCount(templateDto.getId(), permission)));
      }
      response.addPermissionTemplates(templateBuilder);
    }
  }

  private void buildPermissionsResponse(SearchTemplatesWsResponse.Builder response) {
    Permission.Builder permissionResponse = Permission.newBuilder();
    for (String permissionKey : ProjectPermissions.ALL) {
      response.addPermissions(
        permissionResponse
          .clear()
          .setKey(permissionKey)
          .setName(i18nName(permissionKey))
          .setDescription(i18nDescriptionMessage(permissionKey)));
    }
  }

  private String i18nDescriptionMessage(String permissionKey) {
    return i18n.message(userSession.locale(), PROPERTY_PREFIX + permissionKey + DESCRIPTION_SUFFIX, "");
  }

  private String i18nName(String permissionKey) {
    return i18n.message(userSession.locale(), PROPERTY_PREFIX + permissionKey, permissionKey);
  }
}
