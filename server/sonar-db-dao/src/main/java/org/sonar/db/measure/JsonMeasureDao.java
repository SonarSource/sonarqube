/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.measure;

import java.util.Optional;
import java.util.Set;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class JsonMeasureDao implements Dao {

  private final System2 system2;

  public JsonMeasureDao(System2 system2) {
    this.system2 = system2;
  }

  public int insert(DbSession dbSession, JsonMeasureDto dto) {
    return mapper(dbSession).insert(dto, system2.now());
  }

  public int update(DbSession dbSession, JsonMeasureDto dto) {
    return mapper(dbSession).update(dto, system2.now());
  }

  public Optional<JsonMeasureDto> selectByComponentUuid(DbSession dbSession, String componentUuid) {
    return Optional.ofNullable(mapper(dbSession).selectByComponentUuid(componentUuid));
  }

  public Set<JsonMeasureHash> selectBranchMeasureHashes(DbSession dbSession, String branchUuid) {
    return mapper(dbSession).selectBranchMeasureHashes(branchUuid);
  }

  private static JsonMeasureMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(JsonMeasureMapper.class);
  }
}
