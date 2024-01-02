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

import java.util.function.Consumer;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.UserPermissionNewValue;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateDto;

public class UserPermissionDaoWithPersisterTest {
  private final AuditPersister auditPersister = mock(AuditPersister.class);

  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE, auditPersister);

  private final ArgumentCaptor<UserPermissionNewValue> newValueCaptor = ArgumentCaptor.forClass(UserPermissionNewValue.class);
  private final DbSession dbSession = db.getSession();
  private final UserPermissionDao underTest = db.getDbClient().userPermissionDao();

  @Test
  public void userGlobalPermissionInsertAndDeleteArePersisted() {
    UserDto user = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), SYSTEM_ADMIN, user.getUuid(), null);
    underTest.insert(dbSession, dto, null, user, null);

    verify(auditPersister).addUserPermission(eq(dbSession), newValueCaptor.capture());
    UserPermissionNewValue newValue = newValueCaptor.getValue();
    assertNewValue(newValue, dto.getUuid(), user.getUuid(), user.getLogin(), null, dto.getPermission(), null, null, null);
    assertThat(newValue.toString()).doesNotContain("projectUuid");

    underTest.deleteGlobalPermission(dbSession, user, SYSTEM_ADMIN);

    verify(auditPersister).deleteUserPermission(eq(dbSession), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();
    assertNewValue(newValue, null, user.getUuid(), user.getLogin(), null, dto.getPermission(), null, null, null);
    assertThat(newValue.toString()).doesNotContain("permissionUuid");
  }

  @Test
  public void userGlobalPermissionInsertWithTemplateIsPersisted() {
    PermissionTemplateDto templateDto = newPermissionTemplateDto();
    db.getDbClient().permissionTemplateDao().insert(db.getSession(), templateDto);
    UserDto user = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), SYSTEM_ADMIN, user.getUuid(), null);
    underTest.insert(dbSession, dto, null, user, templateDto);

    verify(auditPersister).addUserPermission(eq(dbSession), newValueCaptor.capture());
    UserPermissionNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(UserPermissionNewValue::getPermissionUuid, UserPermissionNewValue::getUserUuid, UserPermissionNewValue::getUserLogin,
        UserPermissionNewValue::getComponentUuid, UserPermissionNewValue::getPermission, UserPermissionNewValue::getComponentName,
        UserPermissionNewValue::getQualifier,
        UserPermissionNewValue::getPermissionTemplateId, UserPermissionNewValue::getPermissionTemplateName)
      .containsExactly(dto.getUuid(), user.getUuid(), user.getLogin(), null, dto.getPermission(), null, null,
        templateDto.getUuid(), templateDto.getName());
    assertThat(newValue.toString()).doesNotContain("projectUuid");
  }

  @Test
  public void userProjectPermissionInsertAndDeleteArePersisted() {
    UserDto user = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    ComponentDto project = db.components().insertPrivateProject();
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), SYSTEM_ADMIN, user.getUuid(), project.uuid());
    underTest.insert(dbSession, dto, project, user, null);

    verify(auditPersister).addUserPermission(eq(dbSession), newValueCaptor.capture());
    UserPermissionNewValue newValue = newValueCaptor.getValue();
    assertNewValue(newValue, dto.getUuid(), user.getUuid(), user.getLogin(), project.uuid(), dto.getPermission(),
      project.getKey(), project.name(), "TRK");
    assertThat(newValue.toString()).contains("componentUuid");

    underTest.deleteProjectPermission(dbSession, user, SYSTEM_ADMIN, project);

    verify(auditPersister).deleteUserPermission(eq(dbSession), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();
    assertNewValue(newValue, null, user.getUuid(), user.getLogin(), project.uuid(), dto.getPermission(),
      project.getKey(), project.name(), "TRK");
    assertThat(newValue.toString()).doesNotContain("permissionUuid");
  }

  @Test
  public void userProjectPermissionDeleteWithoutAffectedRowsIsNotPersisted() {
    UserDto user = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    ComponentDto project = db.components().insertPrivateProject();

    underTest.deleteProjectPermission(dbSession, user, SYSTEM_ADMIN, project);

    verify(auditPersister).addUser(any(), any());
    verify(auditPersister).addComponent(any(), any());
    verifyNoMoreInteractions(auditPersister);
  }

  @Test
  public void userPortfolioPermissionIsPersisted() {
    UserDto user = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    ComponentDto portfolio = db.components().insertPublicPortfolio();
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), SYSTEM_ADMIN, user.getUuid(), portfolio.uuid());
    underTest.insert(dbSession, dto, portfolio, user, null);

    verify(auditPersister).addUserPermission(eq(dbSession), newValueCaptor.capture());
    UserPermissionNewValue newValue = newValueCaptor.getValue();
    assertNewValue(newValue, dto.getUuid(), user.getUuid(), user.getLogin(), portfolio.uuid(), dto.getPermission(),
      portfolio.getKey(), portfolio.name(), "VW");
    assertThat(newValue.toString()).contains("componentUuid");
  }

  @Test
  public void userApplicationPermissionIsPersisted() {
    UserDto user = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    ComponentDto application = db.components().insertPublicApplication();
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), SYSTEM_ADMIN, user.getUuid(), application.uuid());
    underTest.insert(dbSession, dto, application, user, null);

    verify(auditPersister).addUserPermission(eq(dbSession), newValueCaptor.capture());
    UserPermissionNewValue newValue = newValueCaptor.getValue();
    assertNewValue(newValue, dto.getUuid(), user.getUuid(), user.getLogin(), application.uuid(), dto.getPermission(),
      application.getKey(), application.name(), "APP");
    assertThat(newValue.toString()).contains("componentUuid");
  }

  @Test
  public void deleteUserPermissionOfAnyUserIsPersisted() {
    UserDto user = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    ComponentDto project = db.components().insertPrivateProject();
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), SCAN_EXECUTION, user.getUuid(), project.uuid());
    underTest.insert(dbSession, dto, project, user, null);
    underTest.deleteProjectPermissionOfAnyUser(dbSession, SCAN_EXECUTION, project);

    verify(auditPersister).deleteUserPermission(eq(dbSession), newValueCaptor.capture());
    UserPermissionNewValue newValue = newValueCaptor.getValue();
    assertNewValue(newValue, null, null, null, project.uuid(), dto.getPermission(),
      project.getKey(), project.name(), "TRK");
    assertThat(newValue.toString()).doesNotContain("userUuid");
  }

  @Test
  public void deleteUserPermissionOfAnyUserWithoutAffectedRowsIsNotPersisted() {
    ComponentDto project = db.components().insertPrivateProject();

    underTest.deleteProjectPermissionOfAnyUser(dbSession, SCAN_EXECUTION, project);

    verify(auditPersister).addComponent(any(), any());
    verifyNoMoreInteractions(auditPersister);
  }

  @Test
  public void deleteUserPermissionByUserUuidIsPersisted() {
    UserDto user = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    ComponentDto project = db.components().insertPrivateProject();
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), SYSTEM_ADMIN, user.getUuid(), project.uuid());
    underTest.insert(dbSession, dto, project, user, null);
    underTest.deleteByUserUuid(dbSession, user);

    verify(auditPersister).deleteUserPermission(eq(dbSession), newValueCaptor.capture());
    UserPermissionNewValue newValue = newValueCaptor.getValue();
    assertNewValue(newValue, null, user.getUuid(), user.getLogin(), null, null, null,
      null, null);
    assertThat(newValue.toString()).contains("userUuid");
  }

  @Test
  public void deleteUserPermissionByUserUuidWithoutAffectedRowsIsNotPersisted() {
    UserDto user = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));

    underTest.deleteByUserUuid(dbSession, user);

    verify(auditPersister).addUser(any(), any());
    verifyNoMoreInteractions(auditPersister);
  }

  private void assertNewValue(UserPermissionNewValue newValue, String permissionUuid, String userUuid, String userLogin, String componentUuid,
    String permission, String componentKey, String componentName, String qualifier) {
    assertThat(newValue)
      .extracting(UserPermissionNewValue::getPermissionUuid, UserPermissionNewValue::getUserUuid, UserPermissionNewValue::getUserLogin,
        UserPermissionNewValue::getComponentUuid, UserPermissionNewValue::getPermission,
        UserPermissionNewValue::getComponentKey, UserPermissionNewValue::getComponentName, UserPermissionNewValue::getQualifier)
      .containsExactly(permissionUuid, userUuid, userLogin, componentUuid, permission, componentKey, componentName, qualifier);
  }

  private UserDto insertUser(Consumer<UserDto> populateUserDto) {
    return db.users().insertUser(populateUserDto);
  }
}
