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
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

public interface CeActivityMapper {

  List<String> selectUuidsOfRecentlyCreatedByIsLastKey(@Param("isLastKey") String isLastKey, RowBounds rowBounds);

  @CheckForNull
  CeActivityDto selectByUuid(@Param("uuid") String uuid);

  List<CeActivityDto> selectByComponentUuid(@Param("componentUuid") String componentUuid);

  List<CeActivityDto> selectByQuery(@Param("query") CeActivityQuery query, RowBounds rowBounds);

  List<CeActivityDto> selectOlderThan(@Param("beforeDate") long beforeDate);

  int countByQuery(@Param("query") CeActivityQuery query);

  void insert(CeActivityDto dto);

  void updateIsLastToFalseForLastKey(@Param("isLastKey") String isLastKey, @Param("updatedAt") long updatedAt);

  void updateIsLastToTrueForUuid(@Param("uuid") String uuid, @Param("updatedAt") long updatedAt);

  void deleteByUuid(@Param("uuid") String uuid);
}
