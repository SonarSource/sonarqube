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
package org.sonar.db.ce;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;
import org.sonar.db.Pagination;

public interface CeActivityMapper {

  @CheckForNull
  CeActivityDto selectByUuid(@Param("uuid") String uuid);

  List<CeActivityDto> selectByComponentUuid(@Param("componentUuid") String componentUuid);

  List<CeActivityDto> selectByQuery(@Param("query") CeTaskQuery query, @Param("pagination") Pagination pagination);

  List<CeActivityDto> selectOlderThan(@Param("beforeDate") long beforeDate);

  int countLastByStatusAndComponentUuid(@Param("status") CeActivityDto.Status status, @Nullable @Param("componentUuid") String componentUuid);

  void insert(CeActivityDto dto);

  void updateIsLastToFalseForLastKey(@Param("isLastKey") String isLastKey, @Param("updatedAt") long updatedAt);

  void deleteByUuids(@Param("uuids") List<String> uuids);
}
