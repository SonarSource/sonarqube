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
package org.sonar.server.permission.ws.template;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import java.util.List;
import java.util.Locale;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.core.i18n.I18n;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.template.CountByTemplateAndPermissionDto;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.common.permission.DefaultTemplatesResolver;
import org.sonar.server.common.permission.DefaultTemplatesResolver.ResolvedDefaultTemplates;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Permissions;
import org.sonarqube.ws.Permissions.Permission;
import org.sonarqube.ws.Permissions.PermissionTemplate;
import org.sonarqube.ws.Permissions.SearchTemplatesWsResponse;
import org.sonarqube.ws.Permissions.SearchTemplatesWsResponse.TemplateIdQualifier;

import static java.util.Optional.ofNullable;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdmin;
import static org.sonar.server.permission.ws.template.SearchTemplatesData.builder;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchTemplatesAction implements PermissionsWsAction {
  private static final String PROPERTY_PREFIX = "projects_role.";
  private static final String DESCRIPTION_SUFFIX = ".desc";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final I18n i18n;
  private final DefaultTemplatesResolver defaultTemplatesResolver;
  private final PermissionService permissionService;

  public SearchTemplatesAction(DbClient dbClient, UserSession userSession, I18n i18n, DefaultTemplatesResolver defaultTemplatesResolver,
    PermissionService permissionService) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.i18n = i18n;
    this.defaultTemplatesResolver = defaultTemplatesResolver;
    this.permissionService = permissionService;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction("search_templates")
      .setDescription("List permission templates.<br />" +
        "Requires the following permission: 'Administer System'.")
      .setResponseExample(getClass().getResource("search_templates-example-without-views.json"))
      .setSince("5.2")
      .addSearchQuery("defau", "permission template names")
      .setHandler(this);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      SearchTemplatesRequest request = new SearchTemplatesRequest().setQuery(wsRequest.param(Param.TEXT_QUERY));
      checkGlobalAdmin(userSession);

      SearchTemplatesWsResponse searchTemplatesWsResponse = buildResponse(load(dbSession, request));
      writeProtobuf(searchTemplatesWsResponse, wsRequest, wsResponse);
    }
  }

  private static void buildDefaultTemplatesResponse(SearchTemplatesWsResponse.Builder response, SearchTemplatesData data) {
    TemplateIdQualifier.Builder templateUuidQualifierBuilder = TemplateIdQualifier.newBuilder();

    ResolvedDefaultTemplates resolvedDefaultTemplates = data.defaultTemplates();
    response.addDefaultTemplates(templateUuidQualifierBuilder
      .setQualifier(ComponentQualifiers.PROJECT)
      .setTemplateId(resolvedDefaultTemplates.getProject()));

    resolvedDefaultTemplates.getApplication()
      .ifPresent(viewDefaultTemplate -> response.addDefaultTemplates(
        templateUuidQualifierBuilder
          .clear()
          .setQualifier(ComponentQualifiers.APP)
          .setTemplateId(viewDefaultTemplate)));

    resolvedDefaultTemplates.getPortfolio()
      .ifPresent(viewDefaultTemplate -> response.addDefaultTemplates(
        templateUuidQualifierBuilder
          .clear()
          .setQualifier(ComponentQualifiers.VIEW)
          .setTemplateId(viewDefaultTemplate)));
  }

  private void buildTemplatesResponse(Permissions.SearchTemplatesWsResponse.Builder response, SearchTemplatesData data) {
    Permission.Builder permissionResponse = Permission.newBuilder();
    PermissionTemplate.Builder templateBuilder = PermissionTemplate.newBuilder();

    for (PermissionTemplateDto templateDto : data.templates()) {
      templateBuilder
        .clear()
        .setId(templateDto.getUuid())
        .setName(templateDto.getName())
        .setCreatedAt(formatDateTime(templateDto.getCreatedAt()))
        .setUpdatedAt(formatDateTime(templateDto.getUpdatedAt()));
      ofNullable(templateDto.getKeyPattern()).ifPresent(templateBuilder::setProjectKeyPattern);
      ofNullable(templateDto.getDescription()).ifPresent(templateBuilder::setDescription);
      for (String permission : permissionService.getAllProjectPermissions()) {
        templateBuilder.addPermissions(
          permissionResponse
            .clear()
            .setKey(permission)
            .setUsersCount(data.userCount(templateDto.getUuid(), permission))
            .setGroupsCount(data.groupCount(templateDto.getUuid(), permission))
            .setWithProjectCreator(data.withProjectCreator(templateDto.getUuid(), permission)));
      }
      response.addPermissionTemplates(templateBuilder);
    }
  }

  private Permissions.SearchTemplatesWsResponse buildResponse(SearchTemplatesData data) {
    SearchTemplatesWsResponse.Builder response = SearchTemplatesWsResponse.newBuilder();

    buildTemplatesResponse(response, data);
    buildDefaultTemplatesResponse(response, data);
    buildPermissionsResponse(response);

    return response.build();
  }

  private void buildPermissionsResponse(SearchTemplatesWsResponse.Builder response) {
    Permission.Builder permissionResponse = Permission.newBuilder();
    for (String permissionKey : permissionService.getAllProjectPermissions()) {
      response.addPermissions(
        permissionResponse
          .clear()
          .setKey(permissionKey)
          .setName(i18nName(permissionKey))
          .setDescription(i18nDescriptionMessage(permissionKey)));
    }
  }

  private String i18nDescriptionMessage(String permissionKey) {
    return i18n.message(Locale.ENGLISH, PROPERTY_PREFIX + permissionKey + DESCRIPTION_SUFFIX, "");
  }

  private String i18nName(String permissionKey) {
    return i18n.message(Locale.ENGLISH, PROPERTY_PREFIX + permissionKey, permissionKey);
  }

  private SearchTemplatesData load(DbSession dbSession, SearchTemplatesRequest request) {
    SearchTemplatesData.Builder data = builder();
    List<PermissionTemplateDto> templates = searchTemplates(dbSession, request);
    List<String> templateUuids = templates.stream().map(PermissionTemplateDto::getUuid).toList();

    ResolvedDefaultTemplates resolvedDefaultTemplates = defaultTemplatesResolver.resolve(dbSession);
    data.templates(templates)
      .defaultTemplates(resolvedDefaultTemplates)
      .userCountByTemplateUuidAndPermission(userCountByTemplateUuidAndPermission(dbSession, templateUuids))
      .groupCountByTemplateUuidAndPermission(groupCountByTemplateUuidAndPermission(dbSession, templateUuids))
      .withProjectCreatorByTemplateUuidAndPermission(withProjectCreatorsByTemplateUuidAndPermission(dbSession, templateUuids));

    return data.build();
  }

  private List<PermissionTemplateDto> searchTemplates(DbSession dbSession, SearchTemplatesRequest request) {
    return dbClient.permissionTemplateDao().selectAll(dbSession, request.getQuery());
  }

  private Table<String, String, Integer> userCountByTemplateUuidAndPermission(DbSession dbSession, List<String> templateUuids) {
    final Table<String, String, Integer> userCountByTemplateUuidAndPermission = TreeBasedTable.create();

    dbClient.permissionTemplateDao().usersCountByTemplateUuidAndPermission(dbSession, templateUuids, context -> {
      CountByTemplateAndPermissionDto row = context.getResultObject();
      userCountByTemplateUuidAndPermission.put(row.getTemplateUuid(), row.getPermission(), row.getCount());
    });

    return userCountByTemplateUuidAndPermission;
  }

  private Table<String, String, Integer> groupCountByTemplateUuidAndPermission(DbSession dbSession, List<String> templateUuids) {
    final Table<String, String, Integer> userCountByTemplateUuidAndPermission = TreeBasedTable.create();

    dbClient.permissionTemplateDao().groupsCountByTemplateUuidAndPermission(dbSession, templateUuids, context -> {
      CountByTemplateAndPermissionDto row = context.getResultObject();
      userCountByTemplateUuidAndPermission.put(row.getTemplateUuid(), row.getPermission(), row.getCount());
    });

    return userCountByTemplateUuidAndPermission;
  }

  private Table<String, String, Boolean> withProjectCreatorsByTemplateUuidAndPermission(DbSession dbSession, List<String> templateUuids) {
    final Table<String, String, Boolean> templatePermissionsByTemplateUuidAndPermission = TreeBasedTable.create();

    List<PermissionTemplateCharacteristicDto> templatePermissions = dbClient.permissionTemplateCharacteristicDao().selectByTemplateUuids(dbSession, templateUuids);
    templatePermissions.stream()
      .forEach(templatePermission -> templatePermissionsByTemplateUuidAndPermission.put(templatePermission.getTemplateUuid(), templatePermission.getPermission(),
        templatePermission.getWithProjectCreator()));

    return templatePermissionsByTemplateUuidAndPermission;
  }

  private static class SearchTemplatesRequest {
    private String query;

    @CheckForNull
    public String getQuery() {
      return query;
    }

    public SearchTemplatesRequest setQuery(@Nullable String query) {
      this.query = query;
      return this;
    }
  }
}
