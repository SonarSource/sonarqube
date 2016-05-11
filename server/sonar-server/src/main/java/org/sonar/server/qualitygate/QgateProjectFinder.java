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
package org.sonar.server.qualitygate;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.Paging;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualitygate.ProjectQgateAssociation;
import org.sonar.db.qualitygate.ProjectQgateAssociationDao;
import org.sonar.db.qualitygate.ProjectQgateAssociationDto;
import org.sonar.db.qualitygate.ProjectQgateAssociationQuery;
import org.sonar.db.qualitygate.QualityGateDao;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.FluentIterable.from;
import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.server.ws.WsUtils.checkFound;

@ServerSide
public class QgateProjectFinder {

  private final DbClient dbClient;
  private final QualityGateDao qualitygateDao;
  private final ProjectQgateAssociationDao associationDao;
  private final UserSession userSession;

  public QgateProjectFinder(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.qualitygateDao = dbClient.qualityGateDao();
    this.associationDao = dbClient.projectQgateAssociationDao();
  }

  public Association find(ProjectQgateAssociationQuery query) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      getQualityGateId(dbSession, query.gateId());
      List<ProjectQgateAssociationDto> projects = associationDao.selectProjects(dbSession, query);
      List<ProjectQgateAssociationDto> authorizedProjects = keepAuthorizedProjects(dbSession, projects);

      Paging paging = forPageIndex(query.pageIndex())
        .withPageSize(query.pageSize())
        .andTotal(authorizedProjects.size());
      return new Association(toProjectAssociations(getPaginatedProjects(authorizedProjects, paging)), paging.hasNextPage());
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private Long getQualityGateId(DbSession dbSession, String gateId) {
    return checkFound(qualitygateDao.selectById(dbSession, Long.valueOf(gateId)), "Quality gate '" + gateId + "' does not exists.").getId();
  }

  private static List<ProjectQgateAssociationDto> getPaginatedProjects(List<ProjectQgateAssociationDto> projects, Paging paging) {
    return from(projects)
      .skip(paging.offset())
      .limit(paging.pageSize())
      .toList();
  }

  private static List<ProjectQgateAssociation> toProjectAssociations(List<ProjectQgateAssociationDto> dtos) {
    return from(dtos).transform(ToProjectAssociation.INSTANCE).toList();
  }

  private List<ProjectQgateAssociationDto> keepAuthorizedProjects(DbSession dbSession, List<ProjectQgateAssociationDto> projects) {
    List<Long> projectIds = from(projects).transform(ToProjectId.INSTANCE).toList();
    Collection<Long> authorizedProjectIds = dbClient.authorizationDao().keepAuthorizedProjectIds(dbSession, projectIds, userSession.getUserId(), UserRole.USER);
    return from(projects).filter(new MatchProjectId(authorizedProjectIds)).toList();
  }

  public static class Association {
    private List<ProjectQgateAssociation> projects;
    private boolean hasMoreResults;

    private Association(List<ProjectQgateAssociation> projects, boolean hasMoreResults) {
      this.projects = projects;
      this.hasMoreResults = hasMoreResults;
    }

    public List<ProjectQgateAssociation> projects() {
      return projects;
    }

    public boolean hasMoreResults() {
      return hasMoreResults;
    }
  }

  private enum ToProjectId implements Function<ProjectQgateAssociationDto, Long> {
    INSTANCE;

    @Override
    public Long apply(@Nonnull ProjectQgateAssociationDto input) {
      return input.getId();
    }
  }

  private static class MatchProjectId implements Predicate<ProjectQgateAssociationDto> {
    private final Collection<Long> projectIds;

    private MatchProjectId(Collection<Long> projectIds) {
      this.projectIds = projectIds;
    }

    @Override
    public boolean apply(@Nonnull ProjectQgateAssociationDto input) {
      return projectIds.contains(input.getId());
    }
  }

  private enum ToProjectAssociation implements Function<ProjectQgateAssociationDto, ProjectQgateAssociation> {
    INSTANCE;

    @Override
    public ProjectQgateAssociation apply(@Nonnull ProjectQgateAssociationDto input) {
      return input.toQgateAssociation();
    }
  }

}
