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
package org.sonar.db.component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.ibatis.annotations.Param;

public interface BranchMapper {

  void insert(@Param("dto") BranchDto dto, @Param("now") long now);

  int update(@Param("dto") BranchDto dto, @Param("now") long now);

  int updateBranchName(@Param("branchUuid") String branchUuid, @Param("newBranchName") String newBranchName, @Param("now") long now);

  int updateExcludeFromPurge(@Param("uuid") String uuid, @Param("excludeFromPurge") boolean excludeFromPurge,
    @Param("now") long now);

  BranchDto selectByKey(@Param("projectUuid") String projectUuid, @Param("key") String key, @Param("branchType") BranchType branchType);

  List<BranchDto> selectByKeys(@Param("projectUuid") String projectUuid, @Param("keys") Collection<String> branchKeys);

  BranchDto selectByUuid(@Param("uuid") String uuid);

  Collection<BranchDto> selectByProjectUuid(@Param("projectUuid") String projectUuid);

  List<PrBranchAnalyzedLanguageCountByProjectDto> countPrBranchAnalyzedLanguageByProjectUuid();

  List<BranchDto> selectByBranchKeys(@Param("branchKeyByProjectUuid") Map<String, String> branchKeyByProjectUuid);

  List<BranchDto> selectByPullRequestKeys(@Param("prKeyByProjectUuid") Map<String, String> prKeyByProjectUuid);

  List<BranchDto> selectByUuids(@Param("uuids") Collection<String> uuids);

  List<String> selectProjectUuidsWithIssuesNeedSync(@Param("projectUuids") Collection<String> uuids);

  long countByTypeAndCreationDate(@Param("branchType") String branchType, @Param("sinceDate") long sinceDate);

  short hasAnyBranchWhereNeedIssueSync(@Param("needIssueSync") boolean needIssueSync);

  List<BranchDto> selectBranchNeedingIssueSync();

  List<BranchDto> selectBranchNeedingIssueSyncForProject(@Param("projectUuid") String projectUuid);

  long updateAllNeedIssueSync(@Param("now") long now);

  long updateAllNeedIssueSyncForProject(@Param("projectUuid") String projectUuid, @Param("now") long now);

  long updateNeedIssueSync(@Param("uuid") String uuid, @Param("needIssueSync") boolean needIssueSync, @Param("now") long now);

  long updateIsMain(@Param("uuid") String uuid, @Param("isMain") boolean isMain, @Param("now") long now);

  short doAnyOfComponentsNeedIssueSync(@Param("componentKeys") List<String> components);

  Optional<BranchDto> selectMainBranchByProjectUuid(String projectUuid);

  List<BranchDto> selectMainBranchesByProjectUuids(@Param("projectUuids") Collection<String> projectUuids);

  List<BranchMeasuresDto> selectBranchMeasuresWithCaycMetric(long yesterday);

  List<BranchDto> selectMainBranches();

  List<BranchDto> selectMainBranchesAssociatedToDefaultQualityProfile();
}
