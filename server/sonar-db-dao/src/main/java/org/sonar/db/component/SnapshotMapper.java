/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.apache.ibatis.session.RowBounds;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.component.SnapshotDao.ComponentUuidFromDatePair;

public interface SnapshotMapper {

  List<SnapshotDto> selectByUuids(@Param("uuids") List<String> uuids);

  void insert(SnapshotDto snapshot);

  @CheckForNull
  SnapshotDto selectLastSnapshotByComponentUuid(@Param("componentUuid") String componentUuid);

  @CheckForNull
  SnapshotDto selectLastSnapshotByRootComponentUuid(@Param("componentUuid") String componentUuid);

  List<SnapshotDto> selectLastSnapshotsByRootComponentUuids(@Param("componentUuids") Collection<String> componentIds);

  List<SnapshotDto> selectSnapshotsByQuery(@Param("query") SnapshotQuery query);

  List<SnapshotDto> selectPreviousVersionSnapshots(@Param("componentUuid") String componentUuid, @Param("lastVersion") String lastVersion);

  List<SnapshotDto> selectOldestSnapshots(@Param("componentUuid") String componentUuid, RowBounds rowBounds);

  List<ViewsSnapshotDto> selectSnapshotBefore(@Param("componentUuid") String componentUuid, @Param("date") long date);

  void unsetIsLastFlagForComponentUuid(@Param("componentUuid") String componentUuid);

  void setIsLastFlagForAnalysisUuid(@Param("analysisUuid") String analysisUuid);

  void update(SnapshotDto analysis);

  List<SnapshotDto> selectFinishedByComponentUuidsAndFromDates(@Param("componentUuidFromDatePairs") List<ComponentUuidFromDatePair> pairs,
    @Param("ceStatus") CeActivityDto.Status ceStatus);

}
