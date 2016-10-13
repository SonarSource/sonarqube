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
package org.sonar.server.permission.ws;

import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.utils.Paging;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQuery;
import org.sonar.db.permission.CountPerProjectPermission;
import org.sonarqube.ws.client.permission.SearchProjectPermissionsWsRequest;

import static java.util.Collections.singletonList;
import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.server.component.ResourceTypeFunctions.RESOURCE_TYPE_TO_QUALIFIER;
import static org.sonar.server.permission.ws.ProjectWsRef.newOptionalWsProjectRef;
import static org.sonar.server.permission.ws.SearchProjectPermissionsData.newBuilder;

public class SearchProjectPermissionsDataLoader {
  private final DbClient dbClient;
  private final PermissionWsSupport wsSupport;
  private final String[] rootQualifiers;

  public SearchProjectPermissionsDataLoader(DbClient dbClient, PermissionWsSupport wsSupport, ResourceTypes resourceTypes) {
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
    this.rootQualifiers = Collections2.transform(resourceTypes.getRoots(), RESOURCE_TYPE_TO_QUALIFIER).toArray(new String[resourceTypes.getRoots().size()]);
  }

  SearchProjectPermissionsData load(DbSession dbSession, SearchProjectPermissionsWsRequest request) {
    SearchProjectPermissionsData.Builder data = newBuilder();
    int countRootComponents = countRootComponents(dbSession, request);
    List<ComponentDto> rootComponents = searchRootComponents(dbSession, request, paging(request, countRootComponents));
    List<Long> rootComponentIds = Lists.transform(rootComponents, ComponentDto::getId);

    data.rootComponents(rootComponents)
      .paging(paging(request, countRootComponents))
      .userCountByProjectIdAndPermission(userCountByRootComponentIdAndPermission(dbSession, rootComponentIds))
      .groupCountByProjectIdAndPermission(groupCountByRootComponentIdAndPermission(dbSession, rootComponentIds));

    return data.build();
  }

  private static Paging paging(SearchProjectPermissionsWsRequest request, int total) {
    return forPageIndex(request.getPage())
      .withPageSize(request.getPageSize())
      .andTotal(total);
  }

  private int countRootComponents(DbSession dbSession, SearchProjectPermissionsWsRequest request) {
    return dbClient.componentDao().countByQuery(dbSession, toDbQuery(request));
  }

  private List<ComponentDto> searchRootComponents(DbSession dbSession, SearchProjectPermissionsWsRequest request, Paging paging) {
    Optional<ProjectWsRef> project = newOptionalWsProjectRef(request.getProjectId(), request.getProjectKey());

    if (project.isPresent()) {
      return singletonList(wsSupport.getRootComponentOrModule(dbSession, project.get()));
    }

    return dbClient.componentDao().selectByQuery(dbSession, toDbQuery(request), paging.offset(), paging.pageSize());
  }

  private ComponentQuery toDbQuery(SearchProjectPermissionsWsRequest wsRequest) {
    return ComponentQuery.builder()
      .setQualifiers(qualifiers(wsRequest.getQualifier()))
      .setNameOrKeyQuery(wsRequest.getQuery())
      .build();
  }

  private String[] qualifiers(@Nullable String requestQualifier) {
    return requestQualifier == null
      ? rootQualifiers
      : (new String[] {requestQualifier});
  }

  private Table<Long, String, Integer> userCountByRootComponentIdAndPermission(DbSession dbSession, List<Long> rootComponentIds) {
    final Table<Long, String, Integer> userCountByRootComponentIdAndPermission = TreeBasedTable.create();

    dbClient.userPermissionDao().countUsersByProjectPermission(dbSession, rootComponentIds).forEach(
      row -> userCountByRootComponentIdAndPermission.put(row.getComponentId(), row.getPermission(), row.getCount()));

    return userCountByRootComponentIdAndPermission;
  }

  private Table<Long, String, Integer> groupCountByRootComponentIdAndPermission(DbSession dbSession, List<Long> rootComponentIds) {
    final Table<Long, String, Integer> userCountByRootComponentIdAndPermission = TreeBasedTable.create();

    dbClient.groupPermissionDao().groupsCountByComponentIdAndPermission(dbSession, rootComponentIds, context -> {
      CountPerProjectPermission row = (CountPerProjectPermission) context.getResultObject();
      userCountByRootComponentIdAndPermission.put(row.getComponentId(), row.getPermission(), row.getCount());
    });

    return userCountByRootComponentIdAndPermission;
  }
}
