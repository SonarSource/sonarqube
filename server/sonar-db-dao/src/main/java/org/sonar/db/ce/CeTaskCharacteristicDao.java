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

import java.util.Collection;
import java.util.List;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class CeTaskCharacteristicDao implements Dao {

  public void insert(DbSession dbSession, Collection<CeTaskCharacteristicDto> characteristics) {
    for (CeTaskCharacteristicDto dto : characteristics) {
      mapper(dbSession).insert(dto);
    }
  }

  public List<CeTaskCharacteristicDto> selectByTaskUuid(DbSession dbSession, String taskUuid) {
    return mapper(dbSession).selectByTaskUuid(taskUuid);
  }

  public List<CeTaskCharacteristicDto> selectByTaskUuids(DbSession dbSession, List<String> taskUuids) {
    return executeLargeInputs(taskUuids, uuid -> mapper(dbSession).selectByTaskUuids(uuid));
  }

  private static CeTaskCharacteristicMapper mapper(DbSession session) {
    return session.getMapper(CeTaskCharacteristicMapper.class);
  }
}
