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
package org.sonar.db.rule;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class RuleRepositoryDao implements Dao {

  private final System2 system2;

  public RuleRepositoryDao(System2 system2) {
    this.system2 = system2;
  }

  /**
   * @return a non-null list ordered by key (as implemented by database, order may
   * depend on case sensitivity)
   */
  public List<RuleRepositoryDto> selectAll(DbSession dbSession) {
    return dbSession.getMapper(RuleRepositoryMapper.class).selectAll();
  }

  /**
   * @return a non-null list ordered by key (as implemented by database, order may
   * depend on case sensitivity)
   */
  public List<RuleRepositoryDto> selectByLanguage(DbSession dbSession, String language) {
    return dbSession.getMapper(RuleRepositoryMapper.class).selectByLanguage(language);
  }

  public Optional<RuleRepositoryDto> selectByKey(DbSession dbSession, String key) {
    return Optional.ofNullable(dbSession.getMapper(RuleRepositoryMapper.class).selectByKey(key));
  }

  public void truncate(DbSession dbSession) {
    dbSession.getMapper(RuleRepositoryMapper.class).truncate();
  }

  public void insert(DbSession dbSession, Collection<RuleRepositoryDto> dtos) {
    RuleRepositoryMapper mapper = dbSession.getMapper(RuleRepositoryMapper.class);
    long now = system2.now();
    for (RuleRepositoryDto dto : dtos) {
      mapper.insert(dto, now);
    }
  }
}
