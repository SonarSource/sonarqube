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

import org.sonar.api.server.ServerSide;
import org.sonar.db.qualitygate.ProjectQgateAssociation;
import org.sonar.db.qualitygate.ProjectQgateAssociationDao;
import org.sonar.db.qualitygate.ProjectQgateAssociationDto;
import org.sonar.db.qualitygate.ProjectQgateAssociationQuery;
import org.sonar.db.qualitygate.QualityGateDao;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.NotFoundException;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

@ServerSide
public class QgateProjectFinder {

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

  private final QualityGateDao qualitygateDao;
  private final ProjectQgateAssociationDao associationDao;

  public QgateProjectFinder(QualityGateDao qualitygateDao, ProjectQgateAssociationDao associationDao) {
    this.qualitygateDao = qualitygateDao;
    this.associationDao = associationDao;
  }

  public Association find(ProjectQgateAssociationQuery query) {
    Long gateId = validateId(query.gateId());
    int pageSize = query.pageSize();
    int pageIndex = query.pageIndex();

    int offset = (pageIndex - 1) * pageSize;
    // Add one to page size in order to be able to know if there's more results or not
    int limit = pageSize + 1;
    List<ProjectQgateAssociationDto> dtos = associationDao.selectProjects(query, gateId, offset, limit);
    boolean hasMoreResults = false;
    if (dtos.size() == limit) {
      hasMoreResults = true;
      // Removed last entry as it's only need to know if there more results or not
      dtos.remove(dtos.size() - 1);
    }
    return new Association(toProjectAssociations(dtos), hasMoreResults);
  }

  private Long validateId(String gateId) {
    QualityGateDto qualityGateDto = qualitygateDao.selectById(Long.valueOf(gateId));
    if (qualityGateDto == null) {
      throw new NotFoundException("Quality gate '" + gateId + "' does not exists.");
    }
    return qualityGateDto.getId();
  }

  private List<ProjectQgateAssociation> toProjectAssociations(List<ProjectQgateAssociationDto> dtos) {
    List<ProjectQgateAssociation> groups = newArrayList();
    for (ProjectQgateAssociationDto groupMembershipDto : dtos) {
      groups.add(groupMembershipDto.toQgateAssociation());
    }
    return groups;
  }
}
