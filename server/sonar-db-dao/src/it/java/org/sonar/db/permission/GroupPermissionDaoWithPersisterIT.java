/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.permission;

import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.GroupPermissionNewValue;
import org.sonar.db.component.ProjectData;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.GroupDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.sonar.core.util.SequenceUuidFactory.UUID_1;
import static org.sonar.db.permission.ProjectPermission.ADMIN;

class GroupPermissionDaoWithPersisterIT {
  private final AuditPersister auditPersister = mock(AuditPersister.class);

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final SequenceUuidFactory uuidFactory = new SequenceUuidFactory();
  private final ArgumentCaptor<GroupPermissionNewValue> newValueCaptor = ArgumentCaptor.forClass(GroupPermissionNewValue.class);
  private final DbSession dbSession = db.getSession();
  private final GroupPermissionDao underTest = new GroupPermissionDao(auditPersister);

  private GroupDto group;
  private ProjectData project;
  private GroupPermissionDto dto;

  @Test
  void groupGlobalPermissionInsertAndDeleteArePersisted() {
    addGroupPermissionWithoutComponent();

    verify(auditPersister).addGroupPermission(eq(dbSession), newValueCaptor.capture());
    GroupPermissionNewValue newValue = newValueCaptor.getValue();
    assertNewValue(newValue, dto.getUuid(), group.getUuid(), group.getName(), null, dto.getRole(), null, null, null);
    assertThat(newValue).hasToString("{\"permissionUuid\": \"" + UUID_1 + "\", \"permission\": \"admin\", \"groupUuid\": \"guuid\", " +
      "\"groupName\": \"gname\" }");

    underTest.delete(dbSession, ADMIN, group.getUuid(), group.getName(), null);

    verify(auditPersister).deleteGroupPermission(eq(dbSession), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();
    assertNewValue(newValue, null, group.getUuid(), group.getName(), null, ADMIN, null, null, null);
    assertThat(newValue).hasToString("{\"permission\": \"admin\", \"groupUuid\": \"guuid\", \"groupName\": \"gname\" }");
  }

  @Test
  void groupGlobalPermissionDeleteWithoutAffectedRowsIsNotPersisted() {
    underTest.delete(dbSession, ADMIN, "group-uuid", "group-name", null);

    verifyNoInteractions(auditPersister);
  }

  @Test
  void groupProjectPermissionDeleteByComponentIsPersisted() {
    addGroupPermission();

    verify(auditPersister).addGroupPermission(eq(dbSession), newValueCaptor.capture());
    GroupPermissionNewValue newValue = newValueCaptor.getValue();

    assertNewValue(newValue, dto.getUuid(), group.getUuid(), group.getName(), project.projectUuid(), dto.getRole(), project.projectKey(),
      project.getProjectDto().getName(), "TRK");
    assertThat(newValue).hasToString("{\"permissionUuid\": \"" + UUID_1 + "\", \"permission\": \"admin\", \"groupUuid\": \"guuid\", " +
      "\"groupName\": " +
      "\"gname\"," +
      " \"componentUuid\": \"projectUuid\", \"componentKey\": \"cKey\", \"componentName\": \"cname\", \"qualifier\": \"project\" }");

    underTest.deleteByEntityUuid(dbSession, project.getProjectDto());

    verify(auditPersister).deleteGroupPermission(eq(dbSession), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();

    assertNewValue(newValue, null, null, null, project.projectUuid(), (String) null, project.projectKey(), project.getProjectDto().getName(), "TRK");
    assertThat(newValue).hasToString("{\"componentUuid\": \"projectUuid\", \"componentKey\": \"cKey\", \"componentName\": \"cname\", " +
      "\"qualifier\": \"project\" }");
  }

  @Test
  void groupProjectPermissionDeleteByComponentWithoutAffectedRowsIsNotPersisted() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    underTest.deleteByEntityUuid(dbSession, project);

    verifyNoInteractions(auditPersister);
  }

  @Test
  void groupProjectPermissionDeleteByComponentAndGroupIsPersisted() {
    addGroupPermissionWithoutGroup();

    verify(auditPersister).addGroupPermission(eq(dbSession), newValueCaptor.capture());
    GroupPermissionNewValue newValue = newValueCaptor.getValue();

    assertNewValue(newValue, dto.getUuid(), null, null, project.projectUuid(), dto.getRole(), project.projectKey(),
      project.getProjectDto().getName(), "TRK");
    assertThat(newValue).hasToString("{\"permissionUuid\": \"" + UUID_1 + "\", \"permission\": \"admin\", \"componentUuid\": " +
      "\"projectUuid\", "
      + "\"componentKey\": \"cKey\", \"componentName\": \"cname\", \"qualifier\": \"project\" }");

    underTest.deleteByEntityUuidForAnyOne(dbSession, project.getProjectDto());

    verify(auditPersister).deleteGroupPermission(eq(dbSession), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();

    assertNewValue(newValue, null, null, null, project.projectUuid(), (String) null, project.projectKey(), project.getProjectDto().getName(), "TRK");
    assertThat(newValue).hasToString("{\"componentUuid\": \"projectUuid\", \"componentKey\": \"cKey\", " +
      "\"componentName\": \"cname\", \"qualifier\": \"project\" }");
  }

  @Test
  void groupProjectPermissionDeleteByComponentAndGroupWithoutAffectedRowsIsNotPersisted() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    underTest.deleteByEntityUuidForAnyOne(dbSession, project);

    verifyNoInteractions(auditPersister);
  }

  @Test
  void groupProjectPermissionDeleteByComponentAndPermissionIsPersisted() {
    addGroupPermission();

    verify(auditPersister).addGroupPermission(eq(dbSession), newValueCaptor.capture());
    GroupPermissionNewValue newValue = newValueCaptor.getValue();
    assertNewValue(newValue, dto.getUuid(), group.getUuid(), group.getName(), project.projectUuid(), dto.getRole(), project.projectKey(),
      project.getProjectDto().getName(), "TRK");
    assertThat(newValue).hasToString("{\"permissionUuid\": \"" + UUID_1 + "\", \"permission\": \"admin\", \"groupUuid\": \"guuid\", " +
      "\"groupName\": " +
      "\"gname\", "
      + "\"componentUuid\": \"projectUuid\", \"componentKey\": \"cKey\", \"componentName\": \"cname\", \"qualifier\": \"project\" }");

    underTest.deleteByEntityAndPermission(dbSession, dto.getRole(), project.getProjectDto());

    verify(auditPersister).deleteGroupPermission(eq(dbSession), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();
    assertNewValue(newValue, null, null, null, project.projectUuid(), ADMIN, project.projectKey(), project.getProjectDto().getName(),
      "TRK");
    assertThat(newValue).hasToString("{\"permission\": \"admin\", \"componentUuid\": \"projectUuid\", \"componentKey\": \"cKey\"," +
      " \"componentName\": \"cname\", \"qualifier\": \"project\" }");
  }

  @Test
  void groupProjectPermissionDeleteByComponentAndPermissionWithoutAffectedRowsIsNotPersisted() {
    GroupDto group = db.users().insertGroup();
    ProjectData project = db.components().insertPrivateProject();
    GroupPermissionDto dto = getGroupPermission(group, project.getProjectDto());

    underTest.deleteByEntityAndPermission(dbSession, dto.getRole(), project.getProjectDto());

    verifyNoInteractions(auditPersister);
  }

  private void assertNewValue(GroupPermissionNewValue newValue, String uuid, String groupUuid, String groupName, String cUuid,
    ProjectPermission permission,
    String componentKey, String cName, String qualifier) {
    assertNewValue(newValue, uuid, groupUuid, groupName, cUuid, permission.getKey(), componentKey, cName, qualifier);
  }

  private void assertNewValue(GroupPermissionNewValue newValue, String uuid, String groupUuid, String groupName, String cUuid,
    String permission,
    String componentKey, String cName, String qualifier) {
    assertThat(newValue)
      .extracting("permissionUuid", "groupUuid", "groupName", "componentUuid", "permission", "componentKey", "componentName", "qualifier")
      .containsExactly(uuid, groupUuid, groupName, cUuid, permission, componentKey, cName, qualifier);
  }

  private void addGroupPermission() {
    group = db.users().insertGroup(g -> g.setUuid("guuid").setName("gname"));
    project = db.components().insertPrivateProject(c -> c.setName("cname").setKey("cKey"), p -> p.setUuid("projectUuid"));
    dto = getGroupPermission(group, project.getProjectDto());
    underTest.insert(dbSession, dto, project.getProjectDto(), null);
  }

  private void addGroupPermissionWithoutGroup() {
    project = db.components().insertPrivateProject(c -> c.setName("cname").setKey("cKey"), p -> p.setUuid("projectUuid"));
    dto = getGroupPermission(project.getProjectDto());
    underTest.insert(dbSession, dto, project.getProjectDto(), null);
  }

  private void addGroupPermissionWithoutComponent() {
    group = db.users().insertGroup(g -> g.setUuid("guuid").setName("gname"));
    dto = getGroupPermission(group);
    underTest.insert(dbSession, dto, null, null);
  }

  private GroupPermissionDto getGroupPermission(@Nullable GroupDto group, @Nullable ProjectDto project) {
    return new GroupPermissionDto()
      .setUuid(uuidFactory.create())
      .setGroupUuid(group != null ? group.getUuid() : null)
      .setGroupName(group != null ? group.getName() : null)
      .setRole(ADMIN)
      .setEntityUuid(project != null ? project.getUuid() : null)
      .setEntityName(project != null ? project.getName() : null);
  }

  private GroupPermissionDto getGroupPermission(GroupDto group) {
    return getGroupPermission(group, null);
  }

  private GroupPermissionDto getGroupPermission(ProjectDto project) {
    return getGroupPermission(null, project);
  }
}
