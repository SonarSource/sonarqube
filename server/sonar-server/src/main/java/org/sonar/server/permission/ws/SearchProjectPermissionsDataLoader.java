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

package org.sonar.server.permission.ws;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.Request;
import org.sonar.api.utils.Paging;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.CountByProjectAndPermissionDto;

import static java.util.Collections.singletonList;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.server.permission.ws.ResourceTypeToQualifier.RESOURCE_TYPE_TO_QUALIFIER;
import static org.sonar.server.permission.ws.SearchProjectPermissionsData.newBuilder;

public class SearchProjectPermissionsDataLoader {
  private final DbClient dbClient;
  private final PermissionDependenciesFinder finder;
  private final Collection<String> rootQualifiers;

  public SearchProjectPermissionsDataLoader(DbClient dbClient, PermissionDependenciesFinder finder, ResourceTypes resourceTypes) {
    this.dbClient = dbClient;
    this.finder = finder;
    this.rootQualifiers = Collections2.transform(resourceTypes.getRoots(), RESOURCE_TYPE_TO_QUALIFIER);
  }

  SearchProjectPermissionsData load(Request wsRequest) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      SearchProjectPermissionsData.Builder data = newBuilder();
      int countRootComponents = countRootComponents(dbSession, rootQualifiers, wsRequest);
      List<ComponentDto> rootComponents = searchRootComponents(dbSession, wsRequest, paging(wsRequest, countRootComponents));
      List<Long> rootComponentIds = Lists.transform(rootComponents, ComponentToIdFunction.INSTANCE);

      data.rootComponents(rootComponents)
        .paging(paging(wsRequest, countRootComponents))
        .userCountByProjectIdAndPermission(userCountByRootComponentIdAndPermission(dbSession, rootComponentIds))
        .groupCountByProjectIdAndPermission(groupCountByRootComponentIdAndPermission(dbSession, rootComponentIds));

      return data.build();
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private static Paging paging(Request wsRequest, int total) {
    return forPageIndex(wsRequest.mandatoryParamAsInt(PAGE))
      .withPageSize(wsRequest.mandatoryParamAsInt(PAGE_SIZE))
      .andTotal(total);
  }

  private int countRootComponents(DbSession dbSession, Collection<String> qualifiers, Request wsRequest) {
    return dbClient.componentDao().countRootComponents(dbSession, qualifiers, wsRequest.param(TEXT_QUERY));
  }

  private List<ComponentDto> searchRootComponents(DbSession dbSession, Request wsRequest, Paging paging) {
    String query = wsRequest.param(TEXT_QUERY);
    Optional<WsProjectRef> project = WsProjectRef.optionalFromRequest(wsRequest);

    if (project.isPresent()) {
      return singletonList(finder.getProject(dbSession, project.get()));
    }

    return dbClient.componentDao().selectComponents(dbSession, rootQualifiers, paging.offset(), paging.pageSize(), query);
  }

  private Table<Long, String, Integer> userCountByRootComponentIdAndPermission(DbSession dbSession, List<Long> rootComponentIds) {
    final Table<Long, String, Integer> userCountByRootComponentIdAndPermission = TreeBasedTable.create();

    dbClient.permissionDao().usersCountByComponentIdAndPermission(dbSession, rootComponentIds, new ResultHandler() {
      @Override
      public void handleResult(ResultContext context) {
        CountByProjectAndPermissionDto row = (CountByProjectAndPermissionDto) context.getResultObject();
        userCountByRootComponentIdAndPermission.put(row.getComponentId(), row.getPermission(), row.getCount());
      }
    });

    return userCountByRootComponentIdAndPermission;
  }

  private Table<Long, String, Integer> groupCountByRootComponentIdAndPermission(DbSession dbSession, List<Long> rootComponentIds) {
    final Table<Long, String, Integer> userCountByRootComponentIdAndPermission = TreeBasedTable.create();

    dbClient.permissionDao().groupsCountByComponentIdAndPermission(dbSession, rootComponentIds, new ResultHandler() {
      @Override
      public void handleResult(ResultContext context) {
        CountByProjectAndPermissionDto row = (CountByProjectAndPermissionDto) context.getResultObject();
        userCountByRootComponentIdAndPermission.put(row.getComponentId(), row.getPermission(), row.getCount());
      }
    });

    return userCountByRootComponentIdAndPermission;
  }

  private enum ComponentToIdFunction implements Function<ComponentDto, Long> {
    INSTANCE;

    @Override
    public Long apply(@Nonnull ComponentDto component) {
      return component.getId();
    }
  }
}
