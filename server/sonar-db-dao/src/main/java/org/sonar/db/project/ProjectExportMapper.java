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
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.cursor.Cursor;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ProjectLinkDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.rule.RuleDto;

public interface ProjectExportMapper {

  List<BranchDto> selectBranchesForExport(@Param("projectUuid") String projectUuid);

  List<PropertyDto> selectPropertiesForExport(@Param("projectUuid") String projectUuid);

  List<ProjectLinkDto> selectLinksForExport(@Param("projectUuid") String projectUuid);

  List<NewCodePeriodDto> selectNewCodePeriodsForExport(@Param("projectUuid") String projectUuid);

  Cursor<RuleDto> scrollAdhocRulesForExport(@Param("projectUuid") String projectUuid);

  Cursor<IssueDto> scrollIssueForExport(@Param("projectUuid") String projectUuid);

}
