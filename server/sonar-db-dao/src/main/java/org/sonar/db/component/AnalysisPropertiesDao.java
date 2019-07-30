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
package org.sonar.db.component;

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static java.util.Objects.requireNonNull;

public class AnalysisPropertiesDao implements Dao {
  private static final int VARCHAR_MAXSIZE = 4000;

  private final System2 system2;

  public AnalysisPropertiesDao(System2 system2) {
    this.system2 = system2;
  }

  public List<AnalysisPropertyDto> selectByAnalysisUuid(DbSession session, String analysisUuid) {
    requireNonNull(analysisUuid);
    return getMapper(session).selectByAnalysisUuid(analysisUuid);
  }

  public void insert(DbSession session, List<AnalysisPropertyDto> analysisPropertyDto) {
    analysisPropertyDto.forEach(a -> insert(session, a));
  }

  public void insert(DbSession session, AnalysisPropertyDto analysisPropertyDto) {
    requireNonNull(analysisPropertyDto.getUuid(), "uuid cannot be null");
    requireNonNull(analysisPropertyDto.getKey(), "key cannot be null");
    requireNonNull(analysisPropertyDto.getAnalysisUuid(), "analysis uuid cannot be null");
    requireNonNull(analysisPropertyDto.getValue(), "value cannot be null");

    String value = analysisPropertyDto.getValue();
    long now = system2.now();

    if (isEmpty(value)) {
      getMapper(session).insertAsEmpty(analysisPropertyDto, now);
    } else if (mustBeStoredInClob(value)) {
      getMapper(session).insertAsClob(analysisPropertyDto, now);
    } else {
      getMapper(session).insertAsText(analysisPropertyDto, now);
    }
  }

  private static boolean mustBeStoredInClob(String value) {
    return value.length() > VARCHAR_MAXSIZE;
  }

  private static boolean isEmpty(@Nullable String str) {
    return str == null || str.isEmpty();
  }

  private static AnalysisPropertiesMapper getMapper(DbSession session) {
    return session.getMapper(AnalysisPropertiesMapper.class);
  }
}
