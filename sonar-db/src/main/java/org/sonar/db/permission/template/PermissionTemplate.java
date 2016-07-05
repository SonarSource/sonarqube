/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateGroupDto;
import org.sonar.db.permission.template.PermissionTemplateUserDto;

public class PermissionTemplate {
  private final PermissionTemplateDto template;
  private final List<PermissionTemplateUserDto> userPermissions;
  private final List<PermissionTemplateGroupDto> groupPermissions;
  private final List<PermissionTemplateCharacteristicDto> characteristics;

  public PermissionTemplate(PermissionTemplateDto template,
    List<PermissionTemplateUserDto> userPermissions,
    List<PermissionTemplateGroupDto> groupPermissions,
    List<PermissionTemplateCharacteristicDto> characteristics) {
    this.template = template;
    this.userPermissions = userPermissions;
    this.groupPermissions = groupPermissions;
    this.characteristics = characteristics;
  }

  public PermissionTemplateDto getTemplate() {
    return template;
  }

  public List<PermissionTemplateUserDto> getUserPermissions() {
    return userPermissions;
  }

  public List<PermissionTemplateGroupDto> getGroupPermissions() {
    return groupPermissions;
  }

  public List<PermissionTemplateCharacteristicDto> getCharacteristics() {
    return characteristics;
  }
}
