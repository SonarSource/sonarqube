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
package org.sonar.db.component;

import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

public interface SnapshotMapper {

  @CheckForNull
  SnapshotDto selectByKey(long id);

  List<SnapshotDto> selectByIds(@Param("ids") List<Long> ids);

  void insert(SnapshotDto snapshot);

  @CheckForNull
  SnapshotDto selectLastSnapshot(@Param("componentUuid") String componentUuid);

  int countLastSnapshotByComponentUuid(String componentUuid);

  List<SnapshotDto> selectSnapshotsByQuery(@Param("query") SnapshotQuery query);

  List<SnapshotDto> selectPreviousVersionSnapshots(@Param("componentUuid") String componentUuid, @Param("lastVersion") String lastVersion);

  List<SnapshotDto> selectOldestSnapshots(@Param("componentUuid") String componentUuid, RowBounds rowBounds);

  List<SnapshotDto> selectSnapshotAndChildrenOfScope(@Param("snapshot") Long resourceId, @Param("scope") String scope);

  int updateSnapshotAndChildrenLastFlagAndStatus(@Param("root") Long rootId, @Param("pathRootId") Long pathRootId,
    @Param("path") String path, @Param("isLast") boolean isLast, @Param("status") String status);

  int updateSnapshotAndChildrenLastFlag(@Param("root") Long rootId, @Param("pathRootId") Long pathRootId,
    @Param("path") String path, @Param("isLast") boolean isLast);

  List<ViewsSnapshotDto> selectSnapshotBefore(@Param("componentUuid") String componentUuid, @Param("date") long date);

}
