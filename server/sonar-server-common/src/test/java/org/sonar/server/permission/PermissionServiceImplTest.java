/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.permission;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.core.platform.EditionProvider.Edition;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.GroupPermissionDao;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.permission.UserPermissionDao;
import org.sonar.db.permission.UserPermissionDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentTypesRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

  private final ComponentTypesRule resourceTypesRule = new ComponentTypesRule().setRootQualifiers("APP", "VW");

  @Mock
  private PlatformEditionProvider editionProvider;
  @Mock
  private DbClient dbClient;

  @Test
  void globalPermissions_must_be_ordered() {
    PermissionServiceImpl underTest = new PermissionServiceImpl(resourceTypesRule, dbClient);
    assertThat(underTest.getGlobalPermissions())
      .extracting(GlobalPermission::getKey)
      .containsExactlyInAnyOrder("admin", "gateadmin", "profileadmin", "provisioning", "scan", "applicationcreator", "portfoliocreator");
  }

  @ParameterizedTest
  @MethodSource("editionsAndExpectedGlobalPermissionKeys")
  void globalPermissions_depend_on_edition(Edition edition, Set<String> expectedKeys) {
    when(editionProvider.get()).thenReturn(Optional.of(edition));
    PermissionServiceImpl underTest = new PermissionServiceImpl(resourceTypesRule, editionProvider, dbClient);
    assertThat(underTest.getGlobalPermissions())
      .extracting(GlobalPermission::getKey)
      .containsExactlyInAnyOrderElementsOf(expectedKeys);
  }

  private static Stream<Arguments> editionsAndExpectedGlobalPermissionKeys() {
    Set<String> withoutArchitectureAdmin = Set.of("admin", "gateadmin", "profileadmin", "provisioning", "scan", "applicationcreator", "portfoliocreator");
    Set<String> withArchitectureAdmin = Set.of("admin", "gateadmin", "profileadmin", "architectureadmin", "provisioning", "scan", "applicationcreator", "portfoliocreator");
    return Stream.of(
      Arguments.of(Edition.COMMUNITY, withoutArchitectureAdmin),
      Arguments.of(Edition.DEVELOPER, withoutArchitectureAdmin),
      Arguments.of(Edition.ENTERPRISE, withArchitectureAdmin),
      Arguments.of(Edition.DATACENTER, withArchitectureAdmin));
  }

  @ParameterizedTest
  @MethodSource("editionsAndExpectedProjectPermissionKeys")
  void projectPermissions_depend_on_edition(Edition edition, Set<String> expectedKeys) {
    when(editionProvider.get()).thenReturn(Optional.of(edition));
    PermissionServiceImpl underTest = new PermissionServiceImpl(resourceTypesRule, editionProvider, dbClient);
    assertThat(underTest.getAllProjectPermissions())
      .extracting(ProjectPermission::getKey)
      .containsExactlyInAnyOrderElementsOf(expectedKeys);
  }

  private static Stream<Arguments> editionsAndExpectedProjectPermissionKeys() {
    Set<String> withoutArchitectureAdmin = Set.of("admin", "codeviewer", "issueadmin", "securityhotspotadmin", "scan", "user");
    Set<String> withArchitectureAdmin = Set.of("admin", "codeviewer", "issueadmin", "securityhotspotadmin", "architectureadmin", "scan", "user");
    return Stream.of(
      Arguments.of(Edition.COMMUNITY, withoutArchitectureAdmin),
      Arguments.of(Edition.DEVELOPER, withArchitectureAdmin),
      Arguments.of(Edition.ENTERPRISE, withArchitectureAdmin),
      Arguments.of(Edition.DATACENTER, withArchitectureAdmin));
  }

  @Test
  void findGroupPermissions_filters_out_disabled_permissions_in_community_build() {
    when(editionProvider.get()).thenReturn(Optional.of(Edition.COMMUNITY));
    GroupPermissionDao groupPermissionDao = mock(GroupPermissionDao.class);
    when(dbClient.groupPermissionDao()).thenReturn(groupPermissionDao);
    GroupDto group = new GroupDto().setUuid("group-uuid").setName("group");
    GroupPermissionDto scanPermission = new GroupPermissionDto().setGroupUuid("group-uuid").setRole("scan");
    GroupPermissionDto architectureAdminPermission = new GroupPermissionDto().setGroupUuid("group-uuid").setRole("architectureadmin");
    when(groupPermissionDao.selectByGroupUuids(any(DbSession.class), anyList(), isNull()))
      .thenReturn(List.of(scanPermission, architectureAdminPermission));

    PermissionServiceImpl underTest = new PermissionServiceImpl(resourceTypesRule, editionProvider, dbClient);
    List<GroupPermissionDto> result = underTest.findGroupPermissions(mock(DbSession.class), List.of(group), null);

    assertThat(result).extracting(GroupPermissionDto::getRole).containsExactly("scan");
  }

  @Test
  void findUserPermissions_filters_out_disabled_permissions_in_community_build() {
    when(editionProvider.get()).thenReturn(Optional.of(Edition.COMMUNITY));
    UserPermissionDao userPermissionDao = mock(UserPermissionDao.class);
    when(dbClient.userPermissionDao()).thenReturn(userPermissionDao);
    UserDto user = new UserDto().setUuid("user-uuid").setLogin("user");
    UserPermissionDto scanPermission = new UserPermissionDto("uuid1", "scan", "user-uuid", null);
    UserPermissionDto architectureAdminPermission = new UserPermissionDto("uuid2", "architectureadmin", "user-uuid", null);
    when(userPermissionDao.selectUserPermissionsByQuery(any(DbSession.class), any(), anyList()))
      .thenReturn(List.of(scanPermission, architectureAdminPermission));

    PermissionServiceImpl underTest = new PermissionServiceImpl(resourceTypesRule, editionProvider, dbClient);
    List<UserPermissionDto> result = underTest.findUserPermissions(mock(DbSession.class), List.of(user), null);

    assertThat(result).extracting(UserPermissionDto::getPermission).containsExactly("scan");
  }
}
