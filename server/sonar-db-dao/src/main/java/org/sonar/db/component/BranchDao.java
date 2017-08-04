/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class BranchDao implements Dao {

  private final System2 system2;

  public BranchDao(System2 system2) {
    this.system2 = system2;
  }

  public void insert(DbSession dbSession, BranchDto dto) {
    mapper(dbSession).insert(dto, system2.now());
  }

  public void upsert(DbSession dbSession, BranchDto dto) {
    BranchMapper mapper = mapper(dbSession);
    long now = system2.now();
    if (mapper.update(dto, now) == 0) {
      mapper.insert(dto, now);
    }
  }

  public Optional<BranchDto> selectByKey(DbSession dbSession, String projectUuid, BranchKeyType keyType, @Nullable String key) {
    String keyInDb = BranchDto.convertKeyToDb(key);
    return Optional.ofNullable(mapper(dbSession).selectByKey(projectUuid, keyType, keyInDb));
  }

  public Collection<BranchDto> selectByComponent(DbSession dbSession, ComponentDto component) {
    String projectUuid = component.getMainBranchProjectUuid();
    if (projectUuid == null) {
      projectUuid = component.projectUuid();
    }
    return mapper(dbSession).selectByProjectUuid(projectUuid);
  }

  private static BranchMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(BranchMapper.class);
  }
}
