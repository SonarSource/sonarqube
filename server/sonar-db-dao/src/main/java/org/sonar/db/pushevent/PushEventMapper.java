/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.pushevent;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.apache.ibatis.annotations.Param;

public interface PushEventMapper {

  void insert(PushEventDto event);

  @CheckForNull
  PushEventDto selectByUuid(String uuid);

  Set<String> selectUuidsOfExpiredEvents(@Param("timestamp") long timestamp);

  void deleteByUuids(@Param("pushEventUuids") List<String> pushEventUuids);

  LinkedList<PushEventDto> selectChunkByProjectUuids(@Param("projectUuids") Set<String> projectUuids,
    @Param("lastPullTimestamp") Long lastPullTimestamp,
    @Param("lastSeenUuid") String lastSeenUuid, @Param("count") long count);
}
