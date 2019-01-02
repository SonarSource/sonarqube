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
package org.sonar.db.event;

import java.util.List;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;

public interface EventMapper {

  EventDto selectByUuid(String uuid);

  List<EventDto> selectByComponentUuid(String componentUuid);

  List<EventDto> selectByAnalysisUuid(String analysisUuid);

  List<EventDto> selectByAnalysisUuids(@Param("analysisUuids") List<String> list);

  List<EventDto> selectVersions(@Param("componentUuid") String componentUuid);

  void insert(EventDto dto);

  void update(@Param("uuid") String uuid, @Param("name") @Nullable String name, @Param("description") @Nullable String description);

  void deleteById(long id);

  void deleteByUuid(String uuid);
}
