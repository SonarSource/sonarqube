/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import javax.annotation.CheckForNull;
import org.apache.ibatis.annotations.Param;
import org.sonar.db.Pagination;
import org.sonar.db.component.SnapshotDao.ProjectUuidFromDatePair;

public interface SnapshotMapper {

  List<SnapshotDto> selectByUuids(@Param("uuids") List<String> uuids);

  void insert(SnapshotDto snapshot);

  @CheckForNull
  SnapshotDto selectLastSnapshotByComponentUuid(@Param("componentUuid") String componentUuid);

  @CheckForNull
  SnapshotDto selectLastSnapshotByRootComponentUuid(@Param("rootComponentUuid") String rootComponentUuid);

  List<SnapshotDto> selectLastSnapshotsByRootComponentUuids(@Param("rootComponentUuids") Collection<String> rootComponentUuids);

  List<SnapshotDto> selectSnapshotsByQuery(@Param("query") SnapshotQuery query);

  List<SnapshotDto> selectOldestSnapshots(@Param("rootComponentUuid") String rootComponentUuid, @Param("status") String status, @Param("pagination") Pagination pagination);

  List<ViewsSnapshotDto> selectSnapshotBefore(@Param("rootComponentUuid") String rootComponentUuid, @Param("date") long date);

  void unsetIsLastFlagForRootComponentUuid(@Param("rootComponentUuid") String rootComponentUuid);

  void setIsLastFlagForAnalysisUuid(@Param("analysisUuid") String analysisUuid);

  void update(SnapshotDto analysis);

  List<SnapshotDto> selectFinishedByProjectUuidsAndFromDates(@Param("projectUuidFromDatePairs") List<ProjectUuidFromDatePair> pairs);

  @CheckForNull
  Long selectLastAnalysisDateByProject(String projectUuid);

  List<ProjectLastAnalysisDateDto> selectLastAnalysisDateByProjectUuids(@Param("projectUuids") Collection<String> projectUuids);
}
