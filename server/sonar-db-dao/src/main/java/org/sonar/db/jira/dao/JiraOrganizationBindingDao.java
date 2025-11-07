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
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.jira.JiraOrganizationBindingMapper;
import org.sonar.db.jira.dto.JiraOrganizationBindingDto;

public class JiraOrganizationBindingDao implements Dao {

  private final System2 system2;

  public JiraOrganizationBindingDao(System2 system2) {
    this.system2 = system2;
  }

  public Optional<JiraOrganizationBindingDto> selectById(DbSession dbSession, String id) {
    return Optional.ofNullable(getMapper(dbSession).selectById(id));
  }

  public Optional<JiraOrganizationBindingDto> selectBySonarOrganizationUuid(DbSession dbSession,
      String sonarOrganizationUuid) {
    return Optional.ofNullable(getMapper(dbSession).selectBySonarOrganizationUuid(sonarOrganizationUuid));
  }

  public JiraOrganizationBindingDto insert(DbSession dbSession, JiraOrganizationBindingDto dto) {
    long now = system2.now();
    dto.setCreatedAt(now);
    dto.setUpdatedAt(now);
    getMapper(dbSession).insert(dto);
    return dto;
  }

  public JiraOrganizationBindingDto update(DbSession dbSession, JiraOrganizationBindingDto dto) {
    long now = system2.now();
    dto.setUpdatedAt(now);
    getMapper(dbSession).update(dto);
    return dto;
  }

  public int deleteBySonarOrganizationUuid(DbSession dbSession, String sonarOrganizationUuid) {
    var mapper = getMapper(dbSession);
    var totalCount = mapper.countJiraRelatedDataBySonarOrganizationUuid(sonarOrganizationUuid);
    mapper.deleteBySonarOrganizationUuid(sonarOrganizationUuid);
    return totalCount;
  }

  public int countAll(DbSession dbSession) {
    return getMapper(dbSession).countAll();
  }

  private static JiraOrganizationBindingMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(JiraOrganizationBindingMapper.class);
  }
}
