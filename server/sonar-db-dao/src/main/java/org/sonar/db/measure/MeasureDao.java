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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class MeasureDao implements Dao {

  private final System2 system2;

  public MeasureDao(System2 system2) {
    this.system2 = system2;
  }

  public int insert(DbSession dbSession, MeasureDto dto) {
    return mapper(dbSession).insert(dto, system2.now());
  }

  public int update(DbSession dbSession, MeasureDto dto) {
    return mapper(dbSession).update(dto, system2.now());
  }

  public Optional<MeasureDto> selectMeasure(DbSession dbSession, String componentUuid) {
    List<MeasureDto> measures = mapper(dbSession).selectByComponentUuids(List.of(componentUuid));
    if (!measures.isEmpty()) {
      // component_uuid column is unique. List can't have more than 1 item.
      return Optional.of(measures.get(0));
    }
    return Optional.empty();
  }

  public Set<MeasureHash> selectBranchMeasureHashes(DbSession dbSession, String branchUuid) {
    return mapper(dbSession).selectBranchMeasureHashes(branchUuid);
  }

  private static MeasureMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(MeasureMapper.class);
  }
}
