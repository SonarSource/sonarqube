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
package org.sonar.db.jira.dao;

import java.util.List;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.jira.JiraSelectedWorkTypeMapper;
import org.sonar.db.jira.dto.JiraSelectedWorkTypeDto;

public class JiraSelectedWorkTypeDao implements Dao {

  private final System2 system2;
  private final UuidFactory uuidFactory;

  public JiraSelectedWorkTypeDao(System2 system2, UuidFactory uuidFactory) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  public List<JiraSelectedWorkTypeDto> findByJiraProjectBindingId(DbSession dbSession, String jiraProjectBindingId) {
    return getMapper(dbSession).findByJiraProjectBindingId(jiraProjectBindingId);
  }

  public int deleteByJiraProjectBindingId(DbSession dbSession, String jiraProjectBindingId) {
    return getMapper(dbSession).deleteByJiraProjectBindingId(jiraProjectBindingId);
  }

  public void saveAll(DbSession dbSession, List<JiraSelectedWorkTypeDto> dtos) {
    if (dtos.isEmpty()) {
      return;
    }

    long now = system2.now();
    var mapper = getMapper(dbSession);

    for (var dto : dtos) {
      dto.setId(uuidFactory.create())
        .setCreatedAt(now)
        .setUpdatedAt(now);
    }
    mapper.saveAll(dtos);
  }

  private static JiraSelectedWorkTypeMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(JiraSelectedWorkTypeMapper.class);
  }

}
