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
package org.sonar.db.newcodeperiod;

import java.util.Optional;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static java.util.Objects.requireNonNull;

public class NewCodePeriodDao implements Dao {

  private final System2 system2;
  private final UuidFactory uuidFactory;

  public NewCodePeriodDao(System2 system2, UuidFactory uuidFactory) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  public Optional<NewCodePeriodDto> selectByUuid(DbSession dbSession, String uuid) {
    return mapper(dbSession).selectByUuid(uuid);
  }

  public NewCodePeriodDto selectGlobal(DbSession dbSession) {
    return mapper(dbSession).selectGlobal();
  }

  public void insert(DbSession dbSession, NewCodePeriodDto dto) {
    requireNonNull(dto.getType(), "Type of NewCodePeriod must be specified.");
    long currentTime = system2.now();
    mapper(dbSession).insert(dto.setCreatedAt(currentTime)
      .setUpdatedAt(currentTime)
      .setUuid(uuidFactory.create()));
  }

  public void upsert(DbSession dbSession, NewCodePeriodDto dto) {
    NewCodePeriodMapper mapper = mapper(dbSession);
    long currentTime = system2.now();
    dto.setUpdatedAt(currentTime);
    if (mapper.update(dto) == 0) {
      dto.setCreatedAt(currentTime);
      dto.setUuid(uuidFactory.create());
      mapper.insert(dto);
    }
  }

  public Optional<NewCodePeriodDto> selectByProject(DbSession dbSession, String projectUuid) {
    requireNonNull(projectUuid, "Project uuid must be specified.");
    return Optional.ofNullable(mapper(dbSession).selectByProject(projectUuid));

  }

  public Optional<NewCodePeriodDto> selectByBranch(DbSession dbSession, String projectUuid, String branchUuid) {
    requireNonNull(projectUuid, "Project uuid must be specified.");
    requireNonNull(branchUuid, "Branch uuid must be specified.");
    return Optional.ofNullable(mapper(dbSession).selectByBranch(projectUuid, branchUuid));
  }

  private static NewCodePeriodMapper mapper(DbSession session) {
    return session.getMapper(NewCodePeriodMapper.class);
  }

}
