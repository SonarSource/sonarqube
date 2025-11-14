/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;

public class QualityGateDao implements Dao {

  private final UuidFactory uuidFactory;

  public QualityGateDao(UuidFactory uuidFactory) {
    this.uuidFactory = uuidFactory;
  }

  public QualityGateDto insert(DbSession session, QualityGateDto newQualityGate) {
    newQualityGate.setUuid(uuidFactory.create());
    mapper(session).insertQualityGate(newQualityGate.setCreatedAt(new Date()));

    return newQualityGate;
  }

  public Collection<QualityGateDto> selectAll(DbSession session) {
    return mapper(session).selectAll();
  }

  @CheckForNull
  public QualityGateDto selectByName(DbSession session, String name) {
    return mapper(session).selectByName(name);
  }

  public List<QualityGateDto> selectByNames(DbSession session, Collection<String> names) {
    return mapper(session).selectByNames(names);
  }

  @CheckForNull
  public QualityGateDto selectByUuid(DbSession session, String uuid) {
    return mapper(session).selectByUuid(uuid);
  }

  @CheckForNull
  public QualityGateDto selectDefault(DbSession session) {
    return mapper(session).selectDefault();
  }

  public void delete(QualityGateDto qGate, DbSession session) {
    mapper(session).delete(qGate.getUuid());
  }

  public void deleteByUuids(DbSession session, Collection<String> uuids) {
    QualityGateMapper mapper = mapper(session);
    DatabaseUtils.executeLargeUpdates(uuids, mapper::deleteByUuids);
  }

  public void update(QualityGateDto qGate, DbSession session) {
    mapper(session).update(qGate.setUpdatedAt(new Date()));
  }

  public void ensureOnlySonarWayQualityGatesAreBuiltIn(DbSession dbSession, String... builtInName) {
    mapper(dbSession).ensureOnlySonarWayQualityGatesAreBuiltIn(List.of(builtInName));
  }

  public void selectQualityGateFindings(DbSession dbSession, String qualityGateUuid, ResultHandler<QualityGateFindingDto> handler) {
    mapper(dbSession).selectQualityGateFindings(qualityGateUuid, handler);
  }

  public List<QualityGateDto> selectBuiltIn(DbSession dbSession) {
    return mapper(dbSession).selectBuiltIn();
  }

  private static QualityGateMapper mapper(DbSession session) {
    return session.getMapper(QualityGateMapper.class);
  }

  @CheckForNull
  public QualityGateDto selectByProjectUuid(DbSession dbSession, String projectUuid) {
    return mapper(dbSession).selectByProjectUuid(projectUuid);
  }
}
