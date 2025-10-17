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
package org.sonar.db.jira.dao;

import java.util.Optional;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.jira.dto.JiraProjectBindingDto;
import org.sonar.db.jira.JiraProjectBindingMapper;

public class JiraProjectBindingDao implements Dao {

  private final System2 system2;
  private final UuidFactory uuidFactory;

  public JiraProjectBindingDao(System2 system2, UuidFactory uuidFactory) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  public JiraProjectBindingDto insertOrUpdate(DbSession dbSession, JiraProjectBindingDto dto) {
    long now = system2.now();
    var mapper = getMapper(dbSession);

    if (mapper.update(dto, now) == 0) {
      var id = uuidFactory.create();
      mapper.insert(dto, id, now);
      dto
        .setId(id)
        .setCreatedAt(now);
    }
    dto.setUpdatedAt(now);

    return dto;
  }

  public Optional<JiraProjectBindingDto> selectById(DbSession dbSession, String id) {
    return Optional.ofNullable(getMapper(dbSession).selectById(id));
  }

  public Optional<JiraProjectBindingDto> selectBySonarProjectId(DbSession dbSession, String sonarProjectId) {
    return Optional.ofNullable(getMapper(dbSession).selectBySonarProjectId(sonarProjectId));
  }

  public void deleteBySonarProjectId(DbSession dbSession, String sonarProjectId) {
    getMapper(dbSession).deleteBySonarProjectId(sonarProjectId);
  }

  public int countAll(DbSession dbSession) {
    return getMapper(dbSession).countAll();
  }

  private static JiraProjectBindingMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(JiraProjectBindingMapper.class);
  }
}
