/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.core.component.db;

import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.ibatis.annotations.Param;
import org.sonar.core.component.SnapshotDto;
import org.sonar.core.component.SnapshotQuery;

public interface SnapshotMapper {

  @CheckForNull
  SnapshotDto selectByKey(long id);

  void insert(SnapshotDto snapshot);

  @CheckForNull
  SnapshotDto selectLastSnapshot(Long resourceId);

  List<SnapshotDto> selectSnapshotsByQuery(@Param("query") SnapshotQuery query);

  List<SnapshotDto> selectPreviousVersionSnapshots(@Param(value = "componentId") Long componentId, @Param(value = "lastVersion") String lastVersion);

  List<SnapshotDto> selectSnapshotAndChildrenOfScope(@Param(value = "snapshot") Long resourceId, @Param(value = "scope") String scope);

  int updateSnapshotAndChildrenLastFlagAndStatus(@Param(value = "root") Long rootId, @Param(value = "pathRootId") Long pathRootId,
    @Param(value = "path") String path, @Param(value = "isLast") boolean isLast, @Param(value = "status") String status);

  int updateSnapshotAndChildrenLastFlag(@Param(value = "root") Long rootId, @Param(value = "pathRootId") Long pathRootId,
    @Param(value = "path") String path, @Param(value = "isLast") boolean isLast);
}
