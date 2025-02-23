/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.sca;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class ScaIssuesDao implements Dao {

  private static ScaIssuesMapper mapper(DbSession session) {
    return session.getMapper(ScaIssuesMapper.class);
  }

  public void insert(DbSession session, ScaIssueDto scaIssueDto) {
    mapper(session).insert(scaIssueDto);
  }

  public Optional<ScaIssueDto> selectByUuid(DbSession dbSession, String uuid) {
    return Optional.ofNullable(mapper(dbSession).selectByUuid(uuid));
  }

  public List<ScaIssueDto> selectByUuids(DbSession dbSession, Collection<String> uuids) {
    return mapper(dbSession).selectByUuids(uuids);
  }

  public Optional<String> selectUuidByValue(DbSession dbSession, ScaIssueIdentity scaIssueIdentity) {
    return mapper(dbSession).selectUuidByValue(scaIssueIdentity);
  }
}
