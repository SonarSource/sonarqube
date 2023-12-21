/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.db.qualitygate;

import java.util.Collection;
import java.util.Date;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class QualityGateConditionDao implements Dao {

  public void insert(QualityGateConditionDto newQualityGate, DbSession session) {
    mapper(session).insert(newQualityGate.setCreatedAt(new Date()));
  }

  public Collection<QualityGateConditionDto> selectForQualityGate(DbSession session, String qGateUuid) {
    return mapper(session).selectForQualityGate(qGateUuid);
  }

  public QualityGateConditionDto selectByUuid(String uuid, DbSession session) {
    return mapper(session).selectByUuid(uuid);
  }

  public void delete(QualityGateConditionDto qGate, DbSession session) {
    mapper(session).delete(qGate.getUuid());
  }

  public void update(QualityGateConditionDto qGate, DbSession session) {
    mapper(session).update(qGate.setUpdatedAt(new Date()));
  }

  public void deleteConditionsWithInvalidMetrics(DbSession session) {
    mapper(session).deleteConditionsWithInvalidMetrics();
  }

  private static QualityGateConditionMapper mapper(DbSession session) {
    return session.getMapper(QualityGateConditionMapper.class);
  }

  public void deleteQGateCondition(QualityGateDto qGate, DbSession session) {
    mapper(session).deleteQualityGateConditionsByQualityGateUuid(qGate.getUuid());
  }
}
