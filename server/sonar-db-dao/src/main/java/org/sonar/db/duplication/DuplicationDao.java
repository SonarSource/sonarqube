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
package org.sonar.db.duplication;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class DuplicationDao implements Dao {

  /**
   * @param analysisUuid snapshot id of the project from the previous analysis (islast=true)
   */
  public List<DuplicationUnitDto> selectCandidates(DbSession session, @Nullable String analysisUuid, String language, Collection<String> hashes) {
    return executeLargeInputs(
      hashes,
      partition -> session.getMapper(DuplicationMapper.class).selectCandidates(analysisUuid, language, partition));
  }

  /**
   * Insert rows in the table DUPLICATIONS_INDEX.
   * Note that generated ids are not returned.
   */
  public void insert(DbSession session, DuplicationUnitDto dto) {
    session.getMapper(DuplicationMapper.class).batchInsert(dto);
  }
  
  public List<DuplicationUnitDto> selectComponent(DbSession session, String componentUuid, String analysisUuid) {
    return session.getMapper(DuplicationMapper.class).selectComponent(componentUuid, analysisUuid);
  }

}
