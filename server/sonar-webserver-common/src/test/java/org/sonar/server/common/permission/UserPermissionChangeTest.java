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
package org.sonar.server.common.permission;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.user.UserIdDto;
import org.sonar.server.permission.PermissionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserPermissionChangeTest {

  @Test
  void toString_shouldReturnStringRepresentation() {
    PermissionService permissionService = mock();
    when(permissionService.getAllProjectPermissions()).thenReturn(List.of(ProjectPermission.ADMIN, ProjectPermission.SCAN));

    EntityDto entityDto = mock();
    when(entityDto.getName()).thenReturn("entityName");
    UserPermissionChange userPermissionChange = new UserPermissionChange(Operation.ADD, "scan", entityDto, new UserIdDto("uuid1", "login1"), permissionService);

    assertThat(userPermissionChange).hasToString("UserPermissionChange[userId=login='login1', operation=ADD, permission='scan', entity=entityName]");
  }

}
