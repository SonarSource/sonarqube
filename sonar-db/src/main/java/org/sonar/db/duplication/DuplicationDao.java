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
package org.sonar.db.duplication;

import com.google.common.base.Function;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;

public class DuplicationDao implements Dao {

  /**
   * @param projectSnapshotId snapshot id of the project from the previous analysis (islast=true)
   */
  public List<DuplicationUnitDto> selectCandidates(final DbSession session, @Nullable final Long projectSnapshotId, final String language, Collection<String> hashes) {
    return DatabaseUtils.executeLargeInputs(hashes, new Function<List<String>, List<DuplicationUnitDto>>() {
      @Override
      public List<DuplicationUnitDto> apply(@Nonnull List<String> partition) {
        return session.getMapper(DuplicationMapper.class).selectCandidates(projectSnapshotId, language, partition);
      }
    });
  }

  /**
   * Insert rows in the table DUPLICATIONS_INDEX.
   * Note that generated ids are not returned.
   */
  public void insert(DbSession session, DuplicationUnitDto dto) {
    session.getMapper(DuplicationMapper.class).batchInsert(dto);
  }

}
