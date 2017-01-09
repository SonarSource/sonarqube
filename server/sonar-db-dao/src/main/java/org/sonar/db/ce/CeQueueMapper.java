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
package org.sonar.db.ce;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

public interface CeQueueMapper {

  List<CeQueueDto> selectByComponentUuid(@Param("componentUuid") String componentUuid);

  List<CeQueueDto> selectAllInAscOrder();

  List<CeQueueDto> selectByQueryInDescOrder(@Param("query") CeTaskQuery query, RowBounds rowBounds);

  int countByQuery(@Param("query") CeTaskQuery query);

  List<String> selectEligibleForPeek(RowBounds rowBounds);

  @CheckForNull
  CeQueueDto selectByUuid(@Param("uuid") String uuid);

  int countByStatusAndComponentUuid(@Param("status") CeQueueDto.Status status, @Nullable @Param("componentUuid") String componentUuid);

  void insert(CeQueueDto dto);

  void resetAllToPendingStatus(@Param("updatedAt") long updatedAt);

  int updateIfStatus(@Param("uuid") String uuid,
    @Param("newStatus") CeQueueDto.Status newStatus,
    @Nullable @Param("startedAt") Long startedAt,
    @Param("updatedAt") long updatedAt,
    @Param("oldStatus") CeQueueDto.Status oldStatus);

  void deleteByUuid(@Param("uuid") String uuid);
}
