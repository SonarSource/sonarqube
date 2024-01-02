/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.provisioning;

import java.util.Set;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.GithubPermissionsMappingNewValue;

public class GithubPermissionsMappingDao implements Dao {

  private final AuditPersister auditPersister;

  public GithubPermissionsMappingDao(AuditPersister auditPersister) {
    this.auditPersister = auditPersister;
  }

  public Set<GithubPermissionsMappingDto> findAll(DbSession dbSession) {
    return mapper(dbSession).selectAll();
  }

  public Set<GithubPermissionsMappingDto> findAllForGithubRole(DbSession dbSession, String githubRole) {
    return mapper(dbSession).selectAllForGithubRole(githubRole);
  }

  public void insert(DbSession dbSession, GithubPermissionsMappingDto githubPermissionsMappingDto) {
    mapper(dbSession).insert(githubPermissionsMappingDto);
    auditPersister.addGithubPermissionsMapping(dbSession, toNewValueForAuditLogs(githubPermissionsMappingDto.githubRole(), githubPermissionsMappingDto.sonarqubePermission()));
  }

  public void delete(DbSession dbSession, String githubRole, String sonarqubePermission) {
    mapper(dbSession).delete(githubRole, sonarqubePermission);
    auditPersister.deleteGithubPermissionsMapping(dbSession, toNewValueForAuditLogs(githubRole, sonarqubePermission));
  }

  public void deleteAllPermissionsForRole(DbSession dbSession, String githubRole) {
    mapper(dbSession).deleteAllPermissionsForRole(githubRole);
    auditPersister.deleteGithubPermissionsMapping(dbSession, GithubPermissionsMappingNewValue.withAllPermissions(githubRole));
  }

  private static GithubPermissionsMappingNewValue toNewValueForAuditLogs(String githubRole, String sonarqubePermission) {
    return new GithubPermissionsMappingNewValue(githubRole, sonarqubePermission);
  }

  private static GithubPermissionsMappingMapper mapper(DbSession session) {
    return session.getMapper(GithubPermissionsMappingMapper.class);
  }

}
