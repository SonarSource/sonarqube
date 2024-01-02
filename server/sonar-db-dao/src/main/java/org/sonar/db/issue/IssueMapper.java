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
package org.sonar.db.issue;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.db.Pagination;
import org.sonar.db.component.ComponentDto;

public interface IssueMapper {

  IssueDto selectByKey(String key);

  Set<String> selectComponentUuidsOfOpenIssuesForProjectUuid(String projectUuid);

  List<IssueDto> selectByKeys(List<String> keys);

  Set<String> selectIssueKeysByComponentUuid(@Param("componentUuid") String componentUuid);

  Set<String> selectIssueKeysByComponentUuidWithFilters(@Param("componentUuid") String componentUuid,
    @Param("includingRepositories") List<String> includingRepositories,
    @Param("excludingRepositories") List<String> excludingRepositories,
    @Param("languages") List<String> languages, @Param("pagination") Pagination pagination);

  Set<String> selectIssueKeysByComponentUuidAndChangedSinceDate(@Param("componentUuid") String componentUuid,
    @Param("changedSince") long changedSince,
    @Param("includingRepositories") List<String> includingRepositories,
    @Param("excludingRepositories") List<String> excludingRepositories,
    @Param("languages") List<String> languages, @Param("pagination") Pagination pagination);

  List<IssueDto> selectByKeysIfNotUpdatedAt(@Param("keys") List<String> keys, @Param("updatedAt") long updatedAt);

  List<PrIssueDto> selectOpenByComponentUuids(List<String> componentUuids);

  void insert(IssueDto issue);

  int update(IssueDto issue);

  void insertAsNewCodeOnReferenceBranch(NewCodeReferenceIssueDto issue);

  void insertIssueImpact(@Param("issueKey") String issueKey, @Param("dto") ImpactDto issue);

  void deleteAsNewCodeOnReferenceBranch(String issueKey);

  int updateIfBeforeSelectedDate(IssueDto issue);

  void scrollNonClosedByComponentUuid(@Param("componentUuid") String componentUuid, ResultHandler<IssueDto> handler);

  void scrollClosedByComponentUuid(@Param("componentUuid") String componentUuid, @Param("closeDateAfter") long closeDateAfter, ResultHandler<IssueDto> handler);

  Cursor<IndexedIssueDto> scrollIssuesForIndexation(@Nullable @Param("branchUuid") String branchUuid, @Nullable @Param("issueKeys") Collection<String> issueKeys);

  Collection<IssueGroupDto> selectIssueGroupsByComponent(@Param("component") ComponentDto component, @Param("leakPeriodBeginningDate") long leakPeriodBeginningDate);

  List<IssueDto> selectByBranch(@Param("keys") Set<String> keys, @Nullable @Param("changedSince") Long changedSince);

  List<String> selectRecentlyClosedIssues(@Param("queryParams") IssueQueryParams issueQueryParams);

  List<String> selectIssueKeysByQuery(@Param("query") IssueListQuery issueListQuery, @Param("pagination") Pagination pagination);

  void deleteIssueImpacts(String issueKey);
}
