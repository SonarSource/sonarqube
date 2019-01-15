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
package org.sonar.db.component;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;

public interface BranchMapper {

  void insert(@Param("dto") BranchDto dto, @Param("now") long now);

  int update(@Param("dto") BranchDto dto, @Param("now") long now);

  int updateMainBranchName(@Param("projectUuid") String projectUuid, @Param("newBranchName") String newBranchName, @Param("now") long now);

  int updateManualBaseline(@Param("uuid") String uuid, @Nullable @Param("analysisUuid") String analysisUuid, @Param("now") long now);

  BranchDto selectByKey(@Param("projectUuid") String projectUuid, @Param("key") String key, @Param("keyType") KeyType keyType);

  BranchDto selectByUuid(@Param("uuid") String uuid);

  Collection<BranchDto> selectByProjectUuid(@Param("projectUuid") String projectUuid);

  List<BranchDto> selectByUuids(@Param("uuids") Collection<String> uuids);

  long countNonMainBranches();

  long countByTypeAndCreationDate(@Param("branchType")String branchType, @Param("sinceDate") long sinceDate);
}
