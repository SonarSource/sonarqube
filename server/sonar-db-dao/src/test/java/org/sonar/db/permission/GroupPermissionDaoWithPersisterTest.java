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
package org.sonar.db.permission;

import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.GroupPermissionNewValue;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.GroupDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.sonar.api.web.UserRole.ADMIN;

public class GroupPermissionDaoWithPersisterTest {
  private final AuditPersister auditPersister = mock(AuditPersister.class);

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final SequenceUuidFactory uuidFactory = new SequenceUuidFactory();
  private final ArgumentCaptor<GroupPermissionNewValue> newValueCaptor = ArgumentCaptor.forClass(GroupPermissionNewValue.class);
  private final DbSession dbSession = db.getSession();
  private final GroupPermissionDao underTest = new GroupPermissionDao(auditPersister);

  private GroupDto group;
  private ComponentDto project;
  private GroupPermissionDto dto;

  @Test
  public void groupGlobalPermissionInsertAndDeleteArePersisted() {
    addGroupPermissionWithoutComponent();

    verify(auditPersister).addGroupPermission(eq(dbSession), newValueCaptor.capture());
    GroupPermissionNewValue newValue = newValueCaptor.getValue();
    assertNewValue(newValue, dto.getUuid(), group.getUuid(), group.getName(), null, dto.getRole(), null, null, null);
    assertThat(newValue).hasToString("{\"permissionUuid\": \"1\", \"permission\": \"admin\", \"groupUuid\": \"guuid\", \"groupName\": \"gname\" }");

    underTest.delete(dbSession, ADMIN, group.getUuid(), group.getName(), null, null);

    verify(auditPersister).deleteGroupPermission(eq(dbSession), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();
    assertNewValue(newValue, null, group.getUuid(), group.getName(), null, ADMIN, null, null, null);
    assertThat(newValue).hasToString("{\"permission\": \"admin\", \"groupUuid\": \"guuid\", \"groupName\": \"gname\" }");
  }

  @Test
  public void groupGlobalPermissionDeleteWithoutAffectedRowsIsNotPersisted() {
    underTest.delete(dbSession, ADMIN, "group-uuid", "group-name", null, null);

    verifyNoInteractions(auditPersister);
  }

  @Test
  public void groupProjectPermissionDeleteByComponentIsPersisted() {
    addGroupPermission();

    verify(auditPersister).addGroupPermission(eq(dbSession), newValueCaptor.capture());
    GroupPermissionNewValue newValue = newValueCaptor.getValue();
    assertNewValue(newValue, dto.getUuid(), group.getUuid(), group.getName(), project.uuid(), dto.getRole(), project.getKey(),
      project.name(), "TRK");
    assertThat(newValue).hasToString("{\"permissionUuid\": \"1\", \"permission\": \"admin\", \"groupUuid\": \"guuid\", \"groupName\": \"gname\"," +
      " \"componentUuid\": \"cuuid\", \"componentKey\": \"cKey\", \"componentName\": \"cname\", \"qualifier\": \"project\" }");

    underTest.deleteByRootComponentUuid(dbSession, project);

    verify(auditPersister).deleteGroupPermission(eq(dbSession), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();
    assertNewValue(newValue, null, null, null, project.uuid(), null, project.getKey(), project.name(), "TRK");
    assertThat(newValue).hasToString("{\"componentUuid\": \"cuuid\", \"componentKey\": \"cKey\", \"componentName\": \"cname\", \"qualifier\": \"project\" }");
  }

  @Test
  public void groupProjectPermissionDeleteByComponentWithoutAffectedRowsIsNotPersisted() {
    ComponentDto project = db.components().insertPrivateProject();

    underTest.deleteByRootComponentUuid(dbSession, project);

    verifyNoInteractions(auditPersister);
  }

  @Test
  public void groupProjectPermissionDeleteByComponentAndGroupIsPersisted() {
    addGroupPermissionWithoutGroup();

    verify(auditPersister).addGroupPermission(eq(dbSession), newValueCaptor.capture());
    GroupPermissionNewValue newValue = newValueCaptor.getValue();
     assertNewValue(newValue, dto.getUuid(), null, null, project.uuid(), dto.getRole(), project.getKey(), project.name(), "TRK");
    assertThat(newValue).hasToString("{\"permissionUuid\": \"1\", \"permission\": \"admin\", \"componentUuid\": \"cuuid\", "
      + "\"componentKey\": \"cKey\", \"componentName\": \"cname\", \"qualifier\": \"project\" }");

    underTest.deleteByRootComponentUuidForAnyOne(dbSession, project);

    verify(auditPersister).deleteGroupPermission(eq(dbSession), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();
    assertNewValue(newValue, null, null, null, project.uuid(), null, project.getKey(), project.name(), "TRK");
    assertThat(newValue).hasToString("{\"componentUuid\": \"cuuid\", \"componentKey\": \"cKey\", " +
      "\"componentName\": \"cname\", \"qualifier\": \"project\" }");
  }

  @Test
  public void groupProjectPermissionDeleteByComponentAndGroupWithoutAffectedRowsIsNotPersisted() {
    ComponentDto project = db.components().insertPrivateProject();

    underTest.deleteByRootComponentUuidForAnyOne(dbSession, project);

    verifyNoInteractions(auditPersister);
  }

  @Test
  public void groupProjectPermissionDeleteByComponentAndPermissionIsPersisted() {
    addGroupPermission();

    verify(auditPersister).addGroupPermission(eq(dbSession), newValueCaptor.capture());
    GroupPermissionNewValue newValue = newValueCaptor.getValue();
    assertNewValue(newValue, dto.getUuid(), group.getUuid(), group.getName(), project.uuid(), dto.getRole(), project.getKey(), project.name(), "TRK");
    assertThat(newValue).hasToString("{\"permissionUuid\": \"1\", \"permission\": \"admin\", \"groupUuid\": \"guuid\", \"groupName\": \"gname\", "
      + "\"componentUuid\": \"cuuid\", \"componentKey\": \"cKey\", \"componentName\": \"cname\", \"qualifier\": \"project\" }");

    underTest.deleteByRootComponentUuidAndPermission(dbSession, dto.getRole(), project);

    verify(auditPersister).deleteGroupPermission(eq(dbSession), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();
    assertNewValue(newValue, null, null, null, project.uuid(), ADMIN, project.getKey(), project.name(), "TRK");
    assertThat(newValue).hasToString("{\"permission\": \"admin\", \"componentUuid\": \"cuuid\", \"componentKey\": \"cKey\"," +
      " \"componentName\": \"cname\", \"qualifier\": \"project\" }");
  }

  @Test
  public void groupProjectPermissionDeleteByComponentAndPermissionWithoutAffectedRowsIsNotPersisted() {
    GroupDto group = db.users().insertGroup();
    ComponentDto project = db.components().insertPrivateProject();
    GroupPermissionDto dto = getGroupPermission(group, project);

    underTest.deleteByRootComponentUuidAndPermission(dbSession, dto.getRole(), project);

    verifyNoInteractions(auditPersister);
  }

  private void assertNewValue(GroupPermissionNewValue newValue, String uuid, String groupUuid, String groupName, String cUuid, String permission,
    String componentKey, String cName, String qualifier) {
    assertThat(newValue)
      .extracting("permissionUuid", "groupUuid", "groupName", "componentUuid", "permission", "componentKey", "componentName", "qualifier")
      .containsExactly(uuid, groupUuid, groupName, cUuid, permission, componentKey, cName, qualifier);
  }

  private void addGroupPermission() {
    group = db.users().insertGroup(g -> g.setUuid("guuid").setName("gname"));
    project = db.components().insertPrivateProject(c -> c.setUuid("cuuid").setName("cname").setKey("cKey"));
    dto = getGroupPermission(group, project);
    underTest.insert(dbSession, dto, project, null);
  }

  private void addGroupPermissionWithoutGroup() {
    project = db.components().insertPrivateProject(c -> c.setUuid("cuuid").setName("cname").setKey("cKey"));
    dto = getGroupPermission(project);
    underTest.insert(dbSession, dto, project, null);
  }

  private void addGroupPermissionWithoutComponent() {
    group = db.users().insertGroup(g -> g.setUuid("guuid").setName("gname"));
    dto = getGroupPermission(group);
    underTest.insert(dbSession, dto, null, null);
  }

  private GroupPermissionDto getGroupPermission(@Nullable GroupDto group, @Nullable ComponentDto project) {
    return new GroupPermissionDto()
      .setUuid(uuidFactory.create())
      .setGroupUuid(group != null ? group.getUuid() : null)
      .setGroupName(group != null ? group.getName() : null)
      .setRole(ADMIN)
      .setComponentUuid(project != null ? project.uuid() : null)
      .setComponentName(project != null ? project.name(): null);
  }

  private GroupPermissionDto getGroupPermission(GroupDto group) {
    return getGroupPermission(group, null);
  }

  private GroupPermissionDto getGroupPermission(ComponentDto project) {
    return getGroupPermission(null, project);
  }
}
