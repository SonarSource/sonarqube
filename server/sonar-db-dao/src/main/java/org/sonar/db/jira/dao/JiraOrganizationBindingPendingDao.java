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

import java.util.Optional;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.jira.dto.JiraOrganizationBindingPendingDto;
import org.sonar.db.jira.JiraOrganizationBindingPendingMapper;

public class JiraOrganizationBindingPendingDao implements Dao {

  private final System2 system2;

  public JiraOrganizationBindingPendingDao(System2 system2) {
    this.system2 = system2;
  }

  public Optional<JiraOrganizationBindingPendingDto> selectBySonarOrganizationUuid(DbSession dbSession, String sonarOrganizationUuid) {
    return Optional.ofNullable(getMapper(dbSession).selectBySonarOrganizationUuid(sonarOrganizationUuid));
  }

  public JiraOrganizationBindingPendingDto insert(DbSession dbSession, JiraOrganizationBindingPendingDto dto) {
    long now = system2.now();
    dto.setCreatedAt(now);
    dto.setUpdatedAt(now);
    getMapper(dbSession).insert(dto);
    return dto;
  }

  public int deleteBySonarOrganizationUuid(DbSession dbSession, String sonarOrganizationUuid) {
    return getMapper(dbSession).deleteBySonarOrganizationUuid(sonarOrganizationUuid);
  }

  public int countAll(DbSession dbSession) {
    return getMapper(dbSession).countAll();
  }

  private static JiraOrganizationBindingPendingMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(JiraOrganizationBindingPendingMapper.class);
  }
}
