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

import java.util.Date;
import org.apache.commons.lang.math.RandomUtils;
import org.sonar.core.util.Uuids;
import org.sonar.db.permission.PermissionsTestHelper;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.RandomStringUtils.randomAscii;

public class PermissionTemplateTesting {
  public static PermissionTemplateDto newPermissionTemplateDto() {
    return new PermissionTemplateDto()
      .setName(randomAlphanumeric(60))
      .setDescription(randomAscii(500))
      .setOrganizationUuid(randomAlphanumeric(40))
      .setUuid(Uuids.create())
      .setCreatedAt(new Date())
      .setUpdatedAt(new Date());
  }

  public static PermissionTemplateUserDto newPermissionTemplateUserDto() {
    return new PermissionTemplateUserDto()
      .setPermission(PermissionsTestHelper.ALL_PERMISSIONS.toArray(new String[0])[RandomUtils.nextInt(PermissionsTestHelper.ALL_PERMISSIONS.size())])
      .setCreatedAt(new Date())
      .setUpdatedAt(new Date());
  }

  public static PermissionTemplateGroupDto newPermissionTemplateGroupDto() {
    return new PermissionTemplateGroupDto()
      .setPermission(PermissionsTestHelper.ALL_PERMISSIONS.toArray(new String[0])[RandomUtils.nextInt(PermissionsTestHelper.ALL_PERMISSIONS.size())])
      .setCreatedAt(new Date())
      .setUpdatedAt(new Date());
  }

  public static PermissionTemplateCharacteristicDto newPermissionTemplateCharacteristicDto() {
    return new PermissionTemplateCharacteristicDto()
      .setPermission(PermissionsTestHelper.ALL_PERMISSIONS.toArray(new String[0])[RandomUtils.nextInt(PermissionsTestHelper.ALL_PERMISSIONS.size())])
      .setWithProjectCreator(RandomUtils.nextBoolean())
      .setCreatedAt(System.currentTimeMillis())
      .setUpdatedAt(System.currentTimeMillis());
  }

}
