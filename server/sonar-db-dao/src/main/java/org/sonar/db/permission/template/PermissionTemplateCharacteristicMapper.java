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
import org.apache.ibatis.annotations.Param;

public interface PermissionTemplateCharacteristicMapper {

  PermissionTemplateCharacteristicDto selectById(@Param("id") long id);

  List<PermissionTemplateCharacteristicDto> selectByTemplateIds(@Param("templateIds") List<Long> templateId);

  PermissionTemplateCharacteristicDto selectByPermissionAndTemplateId(@Param("permission") String permission, @Param("templateId") long templateId);

  void insert(PermissionTemplateCharacteristicDto templatePermissionDto);

  void update(PermissionTemplateCharacteristicDto templatePermissionDto);

  void deleteByTemplateId(long id);

  void deleteByTemplateIds(@Param("templateIds") List<Long> subList);
}
