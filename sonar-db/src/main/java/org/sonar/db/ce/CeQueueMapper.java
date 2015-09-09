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
package org.sonar.db.ce;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;

public interface CeQueueMapper {

  List<CeQueueDto> selectByComponentUuid(@Param("componentUuid") String componentUuid);

  List<CeQueueDto> selectAllInAscOrder();

  List<String> selectEligibleForPeek();

  @CheckForNull
  CeQueueDto selectByUuid(@Param("uuid") String uuid);

  int countByStatus(@Param("status") CeQueueDto.Status status);

  int countAll();

  void insert(CeQueueDto dto);

  void resetAllToPendingStatus(@Param("updatedAt") long updatedAt);

  int updateIfStatus(@Param("uuid") String uuid,
    @Param("newStatus") CeQueueDto.Status newStatus,
    @Nullable @Param("startedAt") Long startedAt,
    @Param("updatedAt") long updatedAt,
    @Param("oldStatus") CeQueueDto.Status oldStatus);

  void deleteByUuid(@Param("uuid") String uuid);
}
