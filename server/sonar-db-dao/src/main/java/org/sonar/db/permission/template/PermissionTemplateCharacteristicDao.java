/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class PermissionTemplateCharacteristicDao implements Dao {

  public List<PermissionTemplateCharacteristicDto> selectByTemplateIds(DbSession dbSession, List<Long> templateIds) {
    return executeLargeInputs(templateIds, partitionOfTemplateIds -> mapper(dbSession).selectByTemplateIds(partitionOfTemplateIds));
  }

  public Optional<PermissionTemplateCharacteristicDto> selectByPermissionAndTemplateId(DbSession dbSession, String permission, long templateId) {
    PermissionTemplateCharacteristicDto dto = mapper(dbSession).selectByPermissionAndTemplateId(permission, templateId);
    return Optional.ofNullable(dto);
  }

  public PermissionTemplateCharacteristicDto insert(DbSession dbSession, PermissionTemplateCharacteristicDto dto) {
    checkArgument(dto.getCreatedAt() != 0L && dto.getUpdatedAt() != 0L);
    mapper(dbSession).insert(dto);
    return dto;
  }

  public PermissionTemplateCharacteristicDto update(DbSession dbSession, PermissionTemplateCharacteristicDto templatePermissionDto) {
    requireNonNull(templatePermissionDto.getId());
    mapper(dbSession).update(templatePermissionDto);
    return templatePermissionDto;
  }

  private static PermissionTemplateCharacteristicMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(PermissionTemplateCharacteristicMapper.class);
  }
}
