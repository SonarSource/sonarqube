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
package org.sonar.db.provisioning;

import java.util.Set;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.DevOpsPermissionsMappingNewValue;

public class DevOpsPermissionsMappingDao implements Dao {

  private final AuditPersister auditPersister;

  public DevOpsPermissionsMappingDao(AuditPersister auditPersister) {
    this.auditPersister = auditPersister;
  }

  public Set<DevOpsPermissionsMappingDto> findAll(DbSession dbSession, String devOpsPlatform) {
    return mapper(dbSession).selectAll(devOpsPlatform);
  }

  public Set<DevOpsPermissionsMappingDto> findAllForDevopsRole(DbSession dbSession, String devOpsPlatform, String role) {
    return mapper(dbSession).selectAllForRole(devOpsPlatform, role);
  }

  public void insert(DbSession dbSession, DevOpsPermissionsMappingDto devOpsPermissionsMappingDto) {
    mapper(dbSession).insert(devOpsPermissionsMappingDto);
    DevOpsPermissionsMappingNewValue newValueForAuditLogs = toNewValueForAuditLogs(
      devOpsPermissionsMappingDto.devOpsPlatform(),
      devOpsPermissionsMappingDto.role(),
      devOpsPermissionsMappingDto.sonarqubePermission()
    );
    auditPersister.addDevOpsPermissionsMapping(dbSession, newValueForAuditLogs);
  }

  public void delete(DbSession dbSession, String devOpsPlatform, String role, String sonarqubePermission) {
    mapper(dbSession).delete(devOpsPlatform, role, sonarqubePermission);
    auditPersister.deleteDevOpsPermissionsMapping(dbSession, toNewValueForAuditLogs(devOpsPlatform, role, sonarqubePermission));
  }

  public void deleteAllPermissionsForRole(DbSession dbSession, String devOpsPlatform, String role) {
    mapper(dbSession).deleteAllPermissionsForRole(devOpsPlatform, role);
    auditPersister.deleteDevOpsPermissionsMapping(dbSession, DevOpsPermissionsMappingNewValue.withAllPermissions(devOpsPlatform, role));
  }

  private static DevOpsPermissionsMappingNewValue toNewValueForAuditLogs(String devOpsPlatform, String role, String sonarqubePermission) {
    return new DevOpsPermissionsMappingNewValue(devOpsPlatform, role, sonarqubePermission);
  }

  private static DevOpsPermissionsMappingMapper mapper(DbSession session) {
    return session.getMapper(DevOpsPermissionsMappingMapper.class);
  }

}
