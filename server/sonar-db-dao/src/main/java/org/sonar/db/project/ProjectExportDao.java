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
package org.sonar.db.project;

import java.util.List;
import org.apache.ibatis.cursor.Cursor;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ProjectLinkDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.rule.RuleDto;

public class ProjectExportDao implements Dao {

  public List<BranchDto> selectBranchesForExport(DbSession dbSession, String projectUuid) {
    return mapper(dbSession).selectBranchesForExport(projectUuid);
  }

  public List<PropertyDto> selectPropertiesForExport(DbSession dbSession, String projectUuid) {
    return mapper(dbSession).selectPropertiesForExport(projectUuid);
  }

  public List<ProjectLinkDto> selectLinksForExport(DbSession dbSession, String projectUuid) {
    return mapper(dbSession).selectLinksForExport(projectUuid);
  }

  public List<NewCodePeriodDto> selectNewCodePeriodsForExport(DbSession dbSession, String projectUuid) {
    return mapper(dbSession).selectNewCodePeriodsForExport(projectUuid);
  }

  public Cursor<RuleDto> scrollAdhocRulesForExport(DbSession dbSession, String projectUuid) {
    return mapper(dbSession).scrollAdhocRulesForExport(projectUuid);
  }

  public Cursor<IssueDto> scrollIssueForExport(DbSession dbSession, String projectUuid) {
    return mapper(dbSession).scrollIssueForExport(projectUuid);
  }

  private static ProjectExportMapper mapper(DbSession session) {
    return session.getMapper(ProjectExportMapper.class);
  }

}
