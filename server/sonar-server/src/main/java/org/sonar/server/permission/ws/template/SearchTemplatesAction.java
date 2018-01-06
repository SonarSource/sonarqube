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
package org.sonar.server.permission.ws.template;

import java.util.List;
import java.util.Locale;

import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.core.permission.ProjectPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.DefaultTemplates;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.template.CountByTemplateAndPermissionDto;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.permission.ws.PermissionWsSupport;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Permissions;
import org.sonarqube.ws.Permissions.Permission;
import org.sonarqube.ws.Permissions.PermissionTemplate;
import org.sonarqube.ws.Permissions.SearchTemplatesWsResponse;
import org.sonarqube.ws.Permissions.SearchTemplatesWsResponse.TemplateIdQualifier;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdmin;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createOrganizationParameter;
import static org.sonar.server.permission.ws.template.SearchTemplatesData.builder;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION;

public class SearchTemplatesAction implements PermissionsWsAction {
  private static final String PROPERTY_PREFIX = "projects_role.";
  private static final String DESCRIPTION_SUFFIX = ".desc";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final I18n i18n;
  private final PermissionWsSupport support;
  private final DefaultTemplatesResolver defaultTemplatesResolver;

  public SearchTemplatesAction(DbClient dbClient, UserSession userSession, I18n i18n, PermissionWsSupport support, DefaultTemplatesResolver defaultTemplatesResolver) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.i18n = i18n;
    this.support = support;
    this.defaultTemplatesResolver = defaultTemplatesResolver;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("search_templates")
      .setDescription("List permission templates.<br />" +
        "Requires the following permission: 'Administer System'.")
      .setResponseExample(getClass().getResource("search_templates-example.json"))
      .setSince("5.2")
      .addSearchQuery("defau", "permission template names")
      .setHandler(this);

    createOrganizationParameter(action).setSince("6.2");
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto org = support.findOrganization(dbSession, wsRequest.param(PARAM_ORGANIZATION));
      SearchTemplatesRequest request = new SearchTemplatesRequest()
        .setOrganizationUuid(org.getUuid())
        .setQuery(wsRequest.param(Param.TEXT_QUERY));
      checkGlobalAdmin(userSession, request.getOrganizationUuid());

      SearchTemplatesWsResponse searchTemplatesWsResponse = buildResponse(load(dbSession, request));
      writeProtobuf(searchTemplatesWsResponse, wsRequest, wsResponse);
    }
  }

  private static void buildDefaultTemplatesResponse(SearchTemplatesWsResponse.Builder response, SearchTemplatesData data) {
    TemplateIdQualifier.Builder templateUuidQualifierBuilder = TemplateIdQualifier.newBuilder();

    DefaultTemplatesResolverImpl.ResolvedDefaultTemplates resolvedDefaultTemplates = data.defaultTemplates();
    response.addDefaultTemplates(templateUuidQualifierBuilder
      .setQualifier(Qualifiers.PROJECT)
      .setTemplateId(resolvedDefaultTemplates.getProject()));

    resolvedDefaultTemplates.getView()
      .ifPresent(viewDefaultTemplate -> response.addDefaultTemplates(
        templateUuidQualifierBuilder
          .clear()
          .setQualifier(Qualifiers.VIEW)
          .setTemplateId(viewDefaultTemplate)));
  }

  private static void buildTemplatesResponse(Permissions.SearchTemplatesWsResponse.Builder response, SearchTemplatesData data) {
    Permission.Builder permissionResponse = Permission.newBuilder();
    PermissionTemplate.Builder templateBuilder = PermissionTemplate.newBuilder();

    for (PermissionTemplateDto templateDto : data.templates()) {
      templateBuilder
        .clear()
        .setId(templateDto.getUuid())
        .setName(templateDto.getName())
        .setCreatedAt(formatDateTime(templateDto.getCreatedAt()))
        .setUpdatedAt(formatDateTime(templateDto.getUpdatedAt()));
      setNullable(templateDto.getKeyPattern(), templateBuilder::setProjectKeyPattern);
      setNullable(templateDto.getDescription(), templateBuilder::setDescription);
      for (String permission : ProjectPermissions.ALL) {
        templateBuilder.addPermissions(
          permissionResponse
            .clear()
            .setKey(permission)
            .setUsersCount(data.userCount(templateDto.getId(), permission))
            .setGroupsCount(data.groupCount(templateDto.getId(), permission))
            .setWithProjectCreator(data.withProjectCreator(templateDto.getId(), permission)));
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
    return i18n.message(Locale.ENGLISH, PROPERTY_PREFIX + permissionKey + DESCRIPTION_SUFFIX, "");
  }

  private String i18nName(String permissionKey) {
    return i18n.message(Locale.ENGLISH, PROPERTY_PREFIX + permissionKey, permissionKey);
  }

  private SearchTemplatesData load(DbSession dbSession, SearchTemplatesRequest request) {
    SearchTemplatesData.Builder data = builder();
    List<PermissionTemplateDto> templates = searchTemplates(dbSession, request);
    List<Long> templateIds = Lists.transform(templates, PermissionTemplateDto::getId);

    DefaultTemplates defaultTemplates = checkFoundWithOptional(
            dbClient.organizationDao().getDefaultTemplates(dbSession, request.getOrganizationUuid()),
            "No Default templates for organization with uuid '%s'", request.getOrganizationUuid());
    DefaultTemplatesResolver.ResolvedDefaultTemplates resolvedDefaultTemplates = defaultTemplatesResolver.resolve(defaultTemplates);

    data.templates(templates)
            .defaultTemplates(resolvedDefaultTemplates)
            .userCountByTemplateIdAndPermission(userCountByTemplateIdAndPermission(dbSession, templateIds))
            .groupCountByTemplateIdAndPermission(groupCountByTemplateIdAndPermission(dbSession, templateIds))
            .withProjectCreatorByTemplateIdAndPermission(withProjectCreatorsByTemplateIdAndPermission(dbSession, templateIds));

    return data.build();
  }

  private List<PermissionTemplateDto> searchTemplates(DbSession dbSession, SearchTemplatesRequest request) {
    return dbClient.permissionTemplateDao().selectAll(dbSession, request.getOrganizationUuid(), request.getQuery());
  }

  private Table<Long, String, Integer> userCountByTemplateIdAndPermission(DbSession dbSession, List<Long> templateIds) {
    final Table<Long, String, Integer> userCountByTemplateIdAndPermission = TreeBasedTable.create();

    dbClient.permissionTemplateDao().usersCountByTemplateIdAndPermission(dbSession, templateIds, context -> {
      CountByTemplateAndPermissionDto row = context.getResultObject();
      userCountByTemplateIdAndPermission.put(row.getTemplateId(), row.getPermission(), row.getCount());
    });

    return userCountByTemplateIdAndPermission;
  }

  private Table<Long, String, Integer> groupCountByTemplateIdAndPermission(DbSession dbSession, List<Long> templateIds) {
    final Table<Long, String, Integer> userCountByTemplateIdAndPermission = TreeBasedTable.create();

    dbClient.permissionTemplateDao().groupsCountByTemplateIdAndPermission(dbSession, templateIds, context -> {
      CountByTemplateAndPermissionDto row = context.getResultObject();
      userCountByTemplateIdAndPermission.put(row.getTemplateId(), row.getPermission(), row.getCount());
    });

    return userCountByTemplateIdAndPermission;
  }

  private Table<Long, String, Boolean> withProjectCreatorsByTemplateIdAndPermission(DbSession dbSession, List<Long> templateIds) {
    final Table<Long, String, Boolean> templatePermissionsByTemplateIdAndPermission = TreeBasedTable.create();

    List<PermissionTemplateCharacteristicDto> templatePermissions = dbClient.permissionTemplateCharacteristicDao().selectByTemplateIds(dbSession, templateIds);
    templatePermissions.stream()
            .forEach(templatePermission -> templatePermissionsByTemplateIdAndPermission.put(templatePermission.getTemplateId(), templatePermission.getPermission(),
                    templatePermission.getWithProjectCreator()));

    return templatePermissionsByTemplateIdAndPermission;
  }

  private static class SearchTemplatesRequest {
    private String query;
    private String organizationUuid;

    @CheckForNull
    public String getQuery() {
      return query;
    }

    public SearchTemplatesRequest setQuery(@Nullable String query) {
      this.query = query;
      return this;
    }

    public String getOrganizationUuid() {
      return organizationUuid;
    }

    public SearchTemplatesRequest setOrganizationUuid(String s) {
      this.organizationUuid = s;
      return this;
    }
  }
}
