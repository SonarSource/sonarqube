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
package org.sonar.server.project.ws;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.List;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentLinkDto;
import org.sonar.db.component.ComponentQuery;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.measure.MeasureQuery;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.client.project.SearchMyProjectsRequest;

import static java.util.Objects.requireNonNull;
import static org.sonar.api.utils.Paging.offset;
import static org.sonar.server.project.ws.SearchMyProjectsData.builder;

public class SearchMyProjectsDataLoader {
  private final UserSession userSession;
  private final DbClient dbClient;

  public SearchMyProjectsDataLoader(UserSession userSession, DbClient dbClient) {
    this.userSession = userSession;
    this.dbClient = dbClient;
  }

  SearchMyProjectsData load(SearchMyProjectsRequest request) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      SearchMyProjectsData.Builder data = builder();
      ProjectsResult searchResult = searchProjects(dbSession, request);
      List<ComponentDto> projects = searchResult.projects;
      List<String> projectUuids = Lists.transform(projects, ComponentDto::projectUuid);
      List<ComponentLinkDto> projectLinks = dbClient.componentLinkDao().selectByComponentUuids(dbSession, projectUuids);
      List<SnapshotDto> snapshots = dbClient.snapshotDao().selectLastAnalysesByRootComponentUuids(dbSession, projectUuids);
      MetricDto gateStatusMetric = dbClient.metricDao().selectOrFailByKey(dbSession, CoreMetrics.ALERT_STATUS_KEY);
      MeasureQuery measureQuery = MeasureQuery.builder()
        .setProjectUuids(projectUuids)
        .setMetricId(gateStatusMetric.getId())
        .build();
      List<MeasureDto> qualityGates = dbClient.measureDao().selectByQuery(dbSession, measureQuery);

      data.setProjects(projects)
        .setProjectLinks(projectLinks)
        .setSnapshots(snapshots)
        .setQualityGates(qualityGates)
        .setTotalNbOfProjects(searchResult.total);

      return data.build();
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  @VisibleForTesting
  ProjectsResult searchProjects(DbSession dbSession, SearchMyProjectsRequest request) {
    long userId = requireNonNull(userSession.getUserId(), "Current user must be authenticated");

    List<Long> componentIds = dbClient.roleDao().selectComponentIdsByPermissionAndUserId(dbSession, UserRole.ADMIN, userId);
    ComponentQuery dbQuery = ComponentQuery.builder()
      .setQualifiers(Qualifiers.PROJECT)
      .setNameOrKeyQuery(request.getQuery())
      .setComponentIds(ImmutableSet.copyOf(componentIds))
      .build();

    return new ProjectsResult(
      dbClient.componentDao().selectByQuery(dbSession, dbQuery, offset(request.getPage(), request.getPageSize()), request.getPageSize()),
      dbClient.componentDao().countByQuery(dbSession, dbQuery));
  }

  private static class ProjectsResult {
    private final List<ComponentDto> projects;
    private final int total;

    private ProjectsResult(List<ComponentDto> projects, int total) {
      this.projects = projects;
      this.total = total;
    }
  }

}
