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
package org.sonar.db.qualitygate;

import java.util.Collection;
import java.util.Date;
import javax.annotation.CheckForNull;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;

public class QualityGateDao implements Dao {

  public QualityGateDto insert(DbSession session, QualityGateDto newQualityGate) {
    mapper(session).insertQualityGate(newQualityGate.setCreatedAt(new Date()));

    return newQualityGate;
  }

  public void associate(DbSession dbSession, String uuid, OrganizationDto organization, QualityGateDto qualityGate) {
    mapper(dbSession).insertOrgQualityGate(uuid, organization.getUuid(), qualityGate.getUuid());
  }

  public Collection<QualityGateDto> selectAll(DbSession session) {
    return mapper(session).selectAll();
  }

  @CheckForNull
  public QualityGateDto selectByName(DbSession session, String name) {
    return mapper(session).selectByName(name);
  }

  @CheckForNull
  public QualityGateDto selectById(DbSession session, long id) {
    return mapper(session).selectById(id);
  }

  public QGateWithOrgDto selectByOrganizationAndUuid(DbSession dbSession, OrganizationDto organization, String qualityGateUuid) {
    return mapper(dbSession).selectByUuidAndOrganization(qualityGateUuid, organization.getUuid());
  }

  public void delete(QualityGateDto qGate, DbSession session) {
    mapper(session).delete(qGate.getId());
  }

  public void update(QualityGateDto qGate, DbSession session) {
    mapper(session).update(qGate.setUpdatedAt(new Date()));
  }

  public void ensureOneBuiltInQualityGate(DbSession dbSession, String builtInName) {
    mapper(dbSession).ensureOneBuiltInQualityGate(builtInName);
  }

  public QualityGateDto selectBuiltIn(DbSession dbSession) {
    return mapper(dbSession).selectBuiltIn();
  }

  private static QualityGateMapper mapper(DbSession session) {
    return session.getMapper(QualityGateMapper.class);
  }
}
