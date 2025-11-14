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
package org.sonar.db.permission.template;

import java.util.List;
import java.util.Optional;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.PermissionTemplateNewValue;
import org.sonar.db.permission.ProjectPermission;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class PermissionTemplateCharacteristicDao implements Dao {
  private final AuditPersister auditPersister;

  public PermissionTemplateCharacteristicDao(AuditPersister auditPersister) {
    this.auditPersister = auditPersister;
  }

  public List<PermissionTemplateCharacteristicDto> selectByTemplateUuids(DbSession dbSession, List<String> templateUuids) {
    return executeLargeInputs(templateUuids, partitionOfTemplateUuids -> mapper(dbSession).selectByTemplateUuids(partitionOfTemplateUuids));
  }

  public Optional<PermissionTemplateCharacteristicDto> selectByPermissionAndTemplateId(DbSession dbSession, ProjectPermission permission, String templateUuid) {
    return selectByPermissionAndTemplateId(dbSession, permission.getKey(), templateUuid);
  }

  public Optional<PermissionTemplateCharacteristicDto> selectByPermissionAndTemplateId(DbSession dbSession, String permission, String templateUuid) {
    PermissionTemplateCharacteristicDto dto = mapper(dbSession).selectByPermissionAndTemplateUuid(permission, templateUuid);
    return Optional.ofNullable(dto);
  }

  public PermissionTemplateCharacteristicDto insert(DbSession dbSession, PermissionTemplateCharacteristicDto dto, String templateName) {
    checkArgument(dto.getCreatedAt() != 0L && dto.getUpdatedAt() != 0L);
    mapper(dbSession).insert(dto);

    auditPersister.addCharacteristicToPermissionTemplate(dbSession, new PermissionTemplateNewValue(dto.getTemplateUuid(),
      dto.getPermission(), templateName, dto.getWithProjectCreator()));

    return dto;
  }

  public PermissionTemplateCharacteristicDto update(DbSession dbSession, PermissionTemplateCharacteristicDto templatePermissionDto,
    String templateName) {
    requireNonNull(templatePermissionDto.getUuid());
    mapper(dbSession).update(templatePermissionDto);

    auditPersister.updateCharacteristicInPermissionTemplate(dbSession, new PermissionTemplateNewValue(templatePermissionDto.getTemplateUuid(),
      templatePermissionDto.getPermission(), templateName, templatePermissionDto.getWithProjectCreator()));

    return templatePermissionDto;
  }

  private static PermissionTemplateCharacteristicMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(PermissionTemplateCharacteristicMapper.class);
  }
}
