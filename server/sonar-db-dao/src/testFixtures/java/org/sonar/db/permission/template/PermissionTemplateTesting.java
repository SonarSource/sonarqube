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

import java.security.SecureRandom;
import java.util.Date;
import java.util.Random;
import java.util.function.Consumer;
import org.sonar.core.util.Uuids;
import org.sonar.db.permission.PermissionsTestHelper;

import static java.util.Arrays.stream;
import static org.apache.commons.lang3.RandomStringUtils.randomAscii;
import static org.apache.commons.lang3.RandomStringUtils.secure;

public class PermissionTemplateTesting {

  private static final Random RANDOM = new SecureRandom();

  @SafeVarargs
  public static PermissionTemplateDto newPermissionTemplateDto(Consumer<PermissionTemplateDto>... populators) {
    PermissionTemplateDto dto = new PermissionTemplateDto()
      .setName(secure().nextAlphanumeric(60))
      .setDescription(randomAscii(500))
      .setUuid(Uuids.create())
      .setCreatedAt(new Date())
      .setUpdatedAt(new Date());
    stream(populators).forEach(p -> p.accept(dto));
    return dto;
  }

  public static PermissionTemplateUserDto newPermissionTemplateUserDto() {
    return new PermissionTemplateUserDto()
      .setPermission(PermissionsTestHelper.ALL_PERMISSIONS.toArray(new String[0])[RANDOM.nextInt(PermissionsTestHelper.ALL_PERMISSIONS.size())])
      .setCreatedAt(new Date())
      .setUpdatedAt(new Date());
  }

  public static PermissionTemplateGroupDto newPermissionTemplateGroupDto() {
    return new PermissionTemplateGroupDto()
      .setUuid(Uuids.createFast())
      .setPermission(PermissionsTestHelper.ALL_PERMISSIONS.toArray(new String[0])[RANDOM.nextInt(PermissionsTestHelper.ALL_PERMISSIONS.size())])
      .setCreatedAt(new Date())
      .setUpdatedAt(new Date());
  }

  public static PermissionTemplateCharacteristicDto newPermissionTemplateCharacteristicDto() {
    return new PermissionTemplateCharacteristicDto()
      .setUuid(Uuids.createFast())
      .setPermission(PermissionsTestHelper.ALL_PERMISSIONS.toArray(new String[0])[RANDOM.nextInt(PermissionsTestHelper.ALL_PERMISSIONS.size())])
      .setWithProjectCreator(RANDOM.nextBoolean())
      .setCreatedAt(System.currentTimeMillis())
      .setUpdatedAt(System.currentTimeMillis());
  }

}
