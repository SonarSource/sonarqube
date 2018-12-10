/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.issue;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.db.component.ComponentDto;

public interface IssueMapper {

  IssueDto selectByKey(String key);

  Set<String> selectComponentUuidsOfOpenIssuesForProjectUuid(String projectUuid);

  Set<String> selectModuleAndDirComponentUuidsOfOpenIssuesForProjectUuid(String projectUuid);

  List<IssueDto> selectByKeys(List<String> keys);

  List<IssueDto> selectByKeysIfNotUpdatedAt(@Param("keys") List<String> keys, @Param("updatedAt") long updatedAt);

  List<ShortBranchIssueDto> selectOpenByComponentUuids(List<String> componentUuids);

  void insert(IssueDto issue);

  int update(IssueDto issue);

  int updateIfBeforeSelectedDate(IssueDto issue);

  void scrollNonClosedByComponentUuid(@Param("componentUuid") String componentUuid, ResultHandler<IssueDto> handler);

  void scrollClosedByComponentUuid(@Param("componentUuid") String componentUuid, @Param("closeDateAfter") long closeDateAfter, ResultHandler<IssueDto> handler);

  List<IssueDto> selectNonClosedByComponentUuidExcludingExternals(@Param("componentUuid") String componentUuid);

  List<IssueDto> selectNonClosedByModuleOrProject(@Param("projectUuid") String projectUuid, @Param("likeModuleUuidPath") String likeModuleUuidPath);

  Collection<IssueGroupDto> selectIssueGroupsByBaseComponent(
    @Param("baseComponent") ComponentDto baseComponent,
    @Param("leakPeriodBeginningDate") long leakPeriodBeginningDate);

}
