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
package org.sonar.server.permission.ws.template;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import java.util.List;
import java.util.Locale;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.core.i18n.I18n;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.permission.ProjectPermission;
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
import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.db.Pagination.forPage;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdmin;
import static org.sonar.server.permission.ws.template.SearchTemplatesData.builder;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchTemplatesAction implements PermissionsWsAction {
  private static final String PROPERTY_PREFIX = "projects_role.";
  private static final String DESCRIPTION_SUFFIX = ".desc";
  private static final int DEFAULT_PAGE_SIZE = 100;
  private static final int RESULTS_MAX_SIZE = 500;

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
    WebService.NewAction action = context.createAction("search_templates")
      .setDescription("List permission templates.<br />" +
        "Requires the following permission: 'Administer System'.")
      .setResponseExample(getClass().getResource("search_templates-example-without-views.json"))
      .setSince("5.2")
      .addSearchQuery("defau", "permission template names")
      .setHandler(this)
      .setChangelog(new Change("2026.2", "Add optional pagination support to search_templates API."));

    action.createParam(Param.PAGE)
      .setDescription("1-based page number")
      .setExampleValue("2");

    action.createParam(Param.PAGE_SIZE)
      .setDescription("Page size. Must be greater than or equal to 0 and less or equal than " + RESULTS_MAX_SIZE + ". " +
        "If this and p param are not provided, pagination is disabled and all results are returned. " +
        "If pageSize=0, no results are returned but the response will contain the total count of matching templates.")
      .setExampleValue("100");
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      SearchTemplatesRequest request = new SearchTemplatesRequest()
        .setQuery(wsRequest.param(Param.TEXT_QUERY))
        .setPage(wsRequest.paramAsInt(Param.PAGE))
        .setPageSize(wsRequest.paramAsInt(Param.PAGE_SIZE));

      validatePaginationParameters(request);
      checkGlobalAdmin(userSession);

      SearchTemplatesWsResponse searchTemplatesWsResponse = buildResponse(load(dbSession, request));
      writeProtobuf(searchTemplatesWsResponse, wsRequest, wsResponse);
    }
  }

  private static void validatePaginationParameters(SearchTemplatesRequest request) {
    if (request.getPageSize() != null) {
      if (request.getPageSize() < 0) {
        throw new IllegalArgumentException("Page size must be >= 0");
      }
      if (request.getPageSize() > RESULTS_MAX_SIZE) {
        throw new IllegalArgumentException("Page size must not exceed " + RESULTS_MAX_SIZE);
      }
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
      for (ProjectPermission permission : permissionService.getAllProjectPermissions()) {
        templateBuilder.addPermissions(
          permissionResponse
            .clear()
            .setKey(permission.getKey())
            .setUsersCount(data.userCount(templateDto.getUuid(), permission.getKey()))
            .setGroupsCount(data.groupCount(templateDto.getUuid(), permission.getKey()))
            .setWithProjectCreator(data.withProjectCreator(templateDto.getUuid(), permission.getKey())));
      }
      response.addPermissionTemplates(templateBuilder);
    }
  }

  private Permissions.SearchTemplatesWsResponse buildResponse(SearchTemplatesData data) {
    SearchTemplatesWsResponse.Builder response = SearchTemplatesWsResponse.newBuilder();

    buildTemplatesResponse(response, data);
    buildDefaultTemplatesResponse(response, data);
    buildPermissionsResponse(response);
    if (data.paging() != null) {
      Paging paging = data.paging();
      response.getPagingBuilder()
        .setPageIndex(paging.pageIndex())
        .setPageSize(paging.pageSize())
        .setTotal(paging.total())
        .build();
    } else if (data.pagingPageIndex() != null) {
      // Handle pageSize=0 case (stored as separate values, not in Paging object)
      response.getPagingBuilder()
        .setPageIndex(data.pagingPageIndex())
        .setPageSize(data.pagingPageSize())
        .setTotal(data.pagingTotal())
        .build();
    }

    return response.build();
  }

  private void buildPermissionsResponse(SearchTemplatesWsResponse.Builder response) {
    Permission.Builder permissionResponse = Permission.newBuilder();
    for (ProjectPermission permissionKey : permissionService.getAllProjectPermissions()) {
      response.addPermissions(
        permissionResponse
          .clear()
          .setKey(permissionKey.getKey())
          .setName(i18nName(permissionKey.getKey()))
          .setDescription(i18nDescriptionMessage(permissionKey.getKey())));
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

    // Only add pagination info if at least one pagination parameter was provided
    if (isPaginationRequested(request)) {
      int total = dbClient.permissionTemplateDao().countAll(dbSession, request.getQuery());
      int pageSize = getPageSizeOrDefault(request);
      int page = getPageOrDefault(request);

      // Special case: pageSize=0 means only return count, no results
      // We can't use Paging.forPageIndex().withPageSize(0) as it requires >= 1
      // So we store the values directly in SearchTemplatesData
      if (pageSize == 0) {
        data.pagingValues(page, pageSize, total);
      } else {
        Paging paging = forPageIndex(page).withPageSize(pageSize).andTotal(total);
        data.paging(paging);
      }
    }

    return data.build();
  }

  private List<PermissionTemplateDto> searchTemplates(DbSession dbSession, SearchTemplatesRequest request) {
    if (isPaginationRequested(request)) {
      int pageSize = getPageSizeOrDefault(request);
      if (pageSize == 0) {
        return List.of();
      }
      return dbClient.permissionTemplateDao().selectAll(dbSession, request.getQuery(),
        forPage(getPageOrDefault(request)).andSize(pageSize));
    } else {
      // When pagination is not provided, fetch all results
      return dbClient.permissionTemplateDao().selectAll(dbSession, request.getQuery(), null);
    }
  }

  private static boolean isPaginationRequested(SearchTemplatesRequest request) {
    return request.getPage() != null || request.getPageSize() != null;
  }

  private static int getPageOrDefault(SearchTemplatesRequest request) {
    return request.getPage() != null ? request.getPage().intValue() : 1;
  }

  private static int getPageSizeOrDefault(SearchTemplatesRequest request) {
    return request.getPageSize() != null ? request.getPageSize().intValue() : DEFAULT_PAGE_SIZE;
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
    private Integer page;
    private Integer pageSize;

    public SearchTemplatesRequest() {
      // For deserialization
    }

    @CheckForNull
    public String getQuery() {
      return query;
    }

    public SearchTemplatesRequest setQuery(@Nullable String query) {
      this.query = query;
      return this;
    }

    @CheckForNull
    public Integer getPage() {
      return page;
    }

    public SearchTemplatesRequest setPage(@Nullable Integer page) {
      this.page = page;
      return this;
    }

    @CheckForNull
    public Integer getPageSize() {
      return pageSize;
    }

    public SearchTemplatesRequest setPageSize(@Nullable Integer pageSize) {
      this.pageSize = pageSize;
      return this;
    }
  }
}
