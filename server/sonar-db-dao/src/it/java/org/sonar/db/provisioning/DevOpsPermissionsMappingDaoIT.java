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
package org.sonar.db.provisioning;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.DevOpsPermissionsMappingNewValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.sonar.db.audit.model.DevOpsPermissionsMappingNewValue.ALL_PERMISSIONS;

class DevOpsPermissionsMappingDaoIT {

  private static final String MAPPING_UUID = "uuid";
  protected static final String DEV_OPS_PLATFORM = "github";

  private final AuditPersister auditPersister = mock();

  @RegisterExtension
  private final DbTester db = DbTester.create(auditPersister);

  private final ArgumentCaptor<DevOpsPermissionsMappingNewValue> newValueCaptor =
    ArgumentCaptor.forClass(DevOpsPermissionsMappingNewValue.class);

  private final DbSession dbSession = db.getSession();

  private final DevOpsPermissionsMappingDao underTest = db.getDbClient().githubPermissionsMappingDao();

  @BeforeEach
  public void setUp() {
    List<DevOpsPermissionsMappingDto> role1Mappings = List.of(
      new DevOpsPermissionsMappingDto("otherDop1", DEV_OPS_PLATFORM + "2", "GH_role_1", "SQ_role_1"),
      new DevOpsPermissionsMappingDto("otherDop2", DEV_OPS_PLATFORM + "2", "GH_role_2", "SQ_role_2"),
      new DevOpsPermissionsMappingDto("otherDop3", DEV_OPS_PLATFORM + "2", "GH_role_3", "SQ_role_3"));

    role1Mappings.forEach(mapping -> underTest.insert(dbSession, mapping));
    reset(auditPersister);
  }

  @Test
  void insert_savesGithubPermissionsMappingDto() {
    DevOpsPermissionsMappingDto devOpsPermissionsMappingDto = new DevOpsPermissionsMappingDto(MAPPING_UUID, DEV_OPS_PLATFORM, "GH_role", "SQ_role");

    underTest.insert(dbSession, devOpsPermissionsMappingDto);

    Set<DevOpsPermissionsMappingDto> savedGithubPermissionsMappings = underTest.findAll(dbSession, DEV_OPS_PLATFORM);
    assertThat(savedGithubPermissionsMappings).hasSize(1);
    DevOpsPermissionsMappingDto savedMapping = savedGithubPermissionsMappings.iterator().next();
    assertThat(savedMapping.uuid()).isEqualTo(devOpsPermissionsMappingDto.uuid());
    assertThat(savedMapping.role()).isEqualTo(devOpsPermissionsMappingDto.role());
    assertThat(savedMapping.sonarqubePermission()).isEqualTo(devOpsPermissionsMappingDto.sonarqubePermission());

    verify(auditPersister).addDevOpsPermissionsMapping(eq(dbSession), newValueCaptor.capture());
    assertThat(newValueCaptor.getValue().getDevOpsPlatform()).isEqualTo(DEV_OPS_PLATFORM);
    assertThat(newValueCaptor.getValue().getGithubRole()).isEqualTo(devOpsPermissionsMappingDto.role());
    assertThat(newValueCaptor.getValue().getSonarqubePermission()).isEqualTo(devOpsPermissionsMappingDto.sonarqubePermission());
  }

  @Test
  void delete_deletesGithubPermissionsMappingDto() {
    DevOpsPermissionsMappingDto devOpsPermissionsMappingDto = new DevOpsPermissionsMappingDto(MAPPING_UUID, DEV_OPS_PLATFORM, "GH_role", "SQ_role");

    underTest.insert(dbSession, devOpsPermissionsMappingDto);
    underTest.delete(dbSession, DEV_OPS_PLATFORM, "GH_role", "SQ_role");

    Set<DevOpsPermissionsMappingDto> savedGithubPermissionsMappings = underTest.findAll(dbSession, DEV_OPS_PLATFORM);
    assertThat(savedGithubPermissionsMappings).isEmpty();

    verify(auditPersister).deleteDevOpsPermissionsMapping(eq(dbSession), newValueCaptor.capture());
    assertThat(newValueCaptor.getValue().getDevOpsPlatform()).isEqualTo(DEV_OPS_PLATFORM);
    assertThat(newValueCaptor.getValue().getGithubRole()).isEqualTo("GH_role");
    assertThat(newValueCaptor.getValue().getSonarqubePermission()).isEqualTo("SQ_role");
  }

  @Test
  void deleteAllPermissionsForRole_deletesGithubPermissionsMappingDto() {
    List<DevOpsPermissionsMappingDto> role1Mappings = List.of(
      new DevOpsPermissionsMappingDto("1", DEV_OPS_PLATFORM, "GH_role_1", "SQ_role_1"),
      new DevOpsPermissionsMappingDto("2", DEV_OPS_PLATFORM, "GH_role_1", "SQ_role_2"),
      new DevOpsPermissionsMappingDto("3", DEV_OPS_PLATFORM, "GH_role_1", "SQ_role_3"));

    List<DevOpsPermissionsMappingDto> role2Mappings = List.of(
      new DevOpsPermissionsMappingDto("4", DEV_OPS_PLATFORM, "GH_role_2", "SQ_role_1"),
      new DevOpsPermissionsMappingDto("5", DEV_OPS_PLATFORM, "GH_role_2", "SQ_role_2"));

    role1Mappings.forEach(mapping -> underTest.insert(dbSession, mapping));
    role2Mappings.forEach(mapping -> underTest.insert(dbSession, mapping));

    underTest.deleteAllPermissionsForRole(dbSession, DEV_OPS_PLATFORM, "GH_role_1");

    Set<DevOpsPermissionsMappingDto> savedGithubPermissionsMappings = underTest.findAll(dbSession, DEV_OPS_PLATFORM);
    assertThat(savedGithubPermissionsMappings).containsExactlyInAnyOrderElementsOf(role2Mappings);

    verify(auditPersister).deleteDevOpsPermissionsMapping(eq(dbSession), newValueCaptor.capture());
    assertThat(newValueCaptor.getValue().getDevOpsPlatform()).isEqualTo(DEV_OPS_PLATFORM);
    assertThat(newValueCaptor.getValue().getGithubRole()).isEqualTo("GH_role_1");
    assertThat(newValueCaptor.getValue().getSonarqubePermission()).isEqualTo(ALL_PERMISSIONS);
  }

  @Test
  void findAll_shouldReturnAllDevOpsPermissionMappingOfDevOpsPlatform() {
    DevOpsPermissionsMappingDto mapping1 = new DevOpsPermissionsMappingDto(MAPPING_UUID, DEV_OPS_PLATFORM, "GH_role", "SQ_role");
    DevOpsPermissionsMappingDto mapping2 = new DevOpsPermissionsMappingDto(MAPPING_UUID + "2", DEV_OPS_PLATFORM, "GH_role2", "SQ_role");
    DevOpsPermissionsMappingDto mapping3 = new DevOpsPermissionsMappingDto(MAPPING_UUID + "3", DEV_OPS_PLATFORM + "2", "GH_role2", "SQ_role");

    underTest.insert(dbSession, mapping1);
    underTest.insert(dbSession, mapping2);
    underTest.insert(dbSession, mapping3);

    Set<DevOpsPermissionsMappingDto> all = underTest.findAll(dbSession, DEV_OPS_PLATFORM);

    assertThat(all).hasSize(2)
      .containsExactlyInAnyOrder(
        mapping1,
        mapping2);
  }

  @Test
  void findAllForGithubRole_shouldReturnPermissionsForTheRole() {
    DevOpsPermissionsMappingDto mapping1 = new DevOpsPermissionsMappingDto(MAPPING_UUID, DEV_OPS_PLATFORM, "GH_role", "SQ_role");
    DevOpsPermissionsMappingDto mapping2 = new DevOpsPermissionsMappingDto(MAPPING_UUID + "2", DEV_OPS_PLATFORM, "GH_role2", "SQ_role");
    DevOpsPermissionsMappingDto mapping3 = new DevOpsPermissionsMappingDto(MAPPING_UUID + "3", DEV_OPS_PLATFORM, "GH_role2", "SQ_role2");
    underTest.insert(dbSession, mapping1);
    underTest.insert(dbSession, mapping2);
    underTest.insert(dbSession, mapping3);

    Set<DevOpsPermissionsMappingDto> forRole2 = underTest.findAllForRole(dbSession, DEV_OPS_PLATFORM, "GH_role2");
    assertThat(forRole2).hasSize(2)
      .containsExactlyInAnyOrder(mapping2, mapping3);

  }

}
