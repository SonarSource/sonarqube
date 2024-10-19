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
package org.sonar.server.permission.ws.template;

import java.util.function.Function;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonarqube.ws.Permissions.PermissionTemplate;

import static java.util.Optional.ofNullable;

public class PermissionTemplateDtoToPermissionTemplateResponse {

  private PermissionTemplateDtoToPermissionTemplateResponse() {
    // prevent instantiation
  }

  public static PermissionTemplate toPermissionTemplateResponse(PermissionTemplateDto dto) {
    return Singleton.INSTANCE.apply(dto);
  }

  private enum Singleton implements Function<PermissionTemplateDto, PermissionTemplate> {
    INSTANCE;
    @Override
    public PermissionTemplate apply(PermissionTemplateDto permissionTemplate) {
      PermissionTemplate.Builder permissionTemplateBuilder = PermissionTemplate.newBuilder()
        .setId(permissionTemplate.getUuid())
        .setName(permissionTemplate.getName())
        .setCreatedAt(DateUtils.formatDateTime(permissionTemplate.getCreatedAt()))
        .setUpdatedAt(DateUtils.formatDateTime(permissionTemplate.getUpdatedAt()));
      ofNullable(permissionTemplate.getDescription()).ifPresent(permissionTemplateBuilder::setDescription);
      ofNullable(permissionTemplate.getKeyPattern()).ifPresent(permissionTemplateBuilder::setProjectKeyPattern);
      return permissionTemplateBuilder.build();
    }
  }
}
