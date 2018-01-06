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
package org.sonar.db.qualityprofile;

import java.util.Collection;
import java.util.List;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;

import static com.google.common.base.Preconditions.checkState;

public class QProfileChangeDao implements Dao {

  private final System2 system2;
  private final UuidFactory uuidFactory;

  public QProfileChangeDao(System2 system2, UuidFactory uuidFactory) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  public void insert(DbSession dbSession, QProfileChangeDto dto) {
    checkState(dto.getCreatedAt() == 0L, "Date of QProfileChangeDto must be set by DAO only. Got %s.", dto.getCreatedAt());
    dto.setUuid(uuidFactory.create());
    dto.setCreatedAt(system2.now());
    mapper(dbSession).insert(dto);
  }

  public List<QProfileChangeDto> selectByQuery(DbSession dbSession, QProfileChangeQuery query) {
    return mapper(dbSession).selectByQuery(query);
  }

  /**
   * Note: offset and limit of the query object will be ignored
   */
  public int countByQuery(DbSession dbSession, QProfileChangeQuery query) {
    return mapper(dbSession).countByQuery(query);
  }

  public void deleteByRulesProfileUuids(DbSession dbSession, Collection<String> ruleProfileUuids) {
    QProfileChangeMapper mapper = mapper(dbSession);
    DatabaseUtils.executeLargeUpdates(ruleProfileUuids, mapper::deleteByRuleProfileUuids);
  }

  private static QProfileChangeMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(QProfileChangeMapper.class);
  }
}
