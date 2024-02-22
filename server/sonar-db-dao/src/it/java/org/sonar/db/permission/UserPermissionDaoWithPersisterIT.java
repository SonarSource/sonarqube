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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.UserPermissionNewValue;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.db.permission.GlobalPermission.SCAN;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateDto;

class UserPermissionDaoWithPersisterIT {
  private final AuditPersister auditPersister = mock(AuditPersister.class);

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE, auditPersister);

  private final ArgumentCaptor<UserPermissionNewValue> newValueCaptor = ArgumentCaptor.forClass(UserPermissionNewValue.class);
  private final DbSession dbSession = db.getSession();
  private final UserPermissionDao underTest = db.getDbClient().userPermissionDao();

  @Test
  void userGlobalPermissionInsertAndDeleteArePersisted() {
    UserDto user = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), ADMINISTER.getKey(), user.getUuid(), null);
    underTest.insert(dbSession, dto, null, user, null);

    verify(auditPersister).addUserPermission(eq(dbSession), newValueCaptor.capture());
    UserPermissionNewValue newValue = newValueCaptor.getValue();
    assertNewValue(newValue, dto.getUuid(), user.getUuid(), user.getLogin(), null, dto.getPermission(), null, null, null);
    assertThat(newValue.toString()).doesNotContain("projectUuid");

    underTest.deleteGlobalPermission(dbSession, user, ADMINISTER.getKey());

    verify(auditPersister).deleteUserPermission(eq(dbSession), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();
    assertNewValue(newValue, null, user.getUuid(), user.getLogin(), null, dto.getPermission(), null, null, null);
    assertThat(newValue.toString()).doesNotContain("permissionUuid");
  }

  @Test
  void userGlobalPermissionInsertWithTemplateIsPersisted() {
    PermissionTemplateDto templateDto = newPermissionTemplateDto();
    db.getDbClient().permissionTemplateDao().insert(db.getSession(), templateDto);
    UserDto user = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), ADMINISTER.getKey(), user.getUuid(), null);
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
  void userProjectPermissionInsertAndDeleteArePersisted() {
    UserDto user = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), ADMINISTER.getKey(), user.getUuid(), project.getUuid());
    underTest.insert(dbSession, dto, project, user, null);

    verify(auditPersister).addUserPermission(eq(dbSession), newValueCaptor.capture());
    UserPermissionNewValue newValue = newValueCaptor.getValue();
    assertNewValue(newValue, dto.getUuid(), user.getUuid(), user.getLogin(), project.getUuid(), dto.getPermission(),
      project.getKey(), project.getName(), "TRK");
    assertThat(newValue.toString()).contains("componentUuid");

    underTest.deleteEntityPermission(dbSession, user, ADMINISTER.getKey(), project);

    verify(auditPersister).deleteUserPermission(eq(dbSession), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();
    assertNewValue(newValue, null, user.getUuid(), user.getLogin(), project.getUuid(), dto.getPermission(),
      project.getKey(), project.getName(), "TRK");
    assertThat(newValue.toString()).doesNotContain("permissionUuid");
  }

  @Test
  void userProjectPermissionDeleteWithoutAffectedRowsIsNotPersisted() {
    UserDto user = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    underTest.deleteEntityPermission(dbSession, user, ADMINISTER.getKey(), project);

    verify(auditPersister).addUser(any(), any());
    verify(auditPersister).addComponent(any(), any());
    verifyNoMoreInteractions(auditPersister);
  }

  @Test
  void userPortfolioPermissionIsPersisted() {
    UserDto user = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    PortfolioDto portfolio = db.components().insertPublicPortfolioDto();
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), ADMINISTER.getKey(), user.getUuid(), portfolio.getUuid());
    underTest.insert(dbSession, dto, portfolio, user, null);

    verify(auditPersister).addUserPermission(eq(dbSession), newValueCaptor.capture());
    UserPermissionNewValue newValue = newValueCaptor.getValue();
    assertNewValue(newValue, dto.getUuid(), user.getUuid(), user.getLogin(), portfolio.getUuid(), dto.getPermission(),
      portfolio.getKey(), portfolio.getName(), "VW");
    assertThat(newValue.toString()).contains("componentUuid");
  }

  @Test
  void userApplicationPermissionIsPersisted() {
    UserDto user = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    ProjectDto application = db.components().insertPublicApplication().getProjectDto();
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), ADMINISTER.getKey(), user.getUuid(), application.getUuid());
    underTest.insert(dbSession, dto, application, user, null);

    verify(auditPersister).addUserPermission(eq(dbSession), newValueCaptor.capture());
    UserPermissionNewValue newValue = newValueCaptor.getValue();
    assertNewValue(newValue, dto.getUuid(), user.getUuid(), user.getLogin(), application.getUuid(), dto.getPermission(),
      application.getKey(), application.getName(), "APP");
    assertThat(newValue.toString()).contains("componentUuid");
  }

  @Test
  void deleteUserPermissionOfAnyUserIsPersisted() {
    UserDto user = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), SCAN.getKey(), user.getUuid(), project.getUuid());
    underTest.insert(dbSession, dto, project, user, null);
    underTest.deleteEntityPermissionOfAnyUser(dbSession, SCAN.getKey(), project);

    verify(auditPersister).deleteUserPermission(eq(dbSession), newValueCaptor.capture());
    UserPermissionNewValue newValue = newValueCaptor.getValue();
    assertNewValue(newValue, null, null, null, project.getUuid(), dto.getPermission(),
      project.getKey(), project.getName(), "TRK");
    assertThat(newValue.toString()).doesNotContain("userUuid");
  }

  @Test
  void deleteUserPermissionOfAnyUserWithoutAffectedRowsIsNotPersisted() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    underTest.deleteEntityPermissionOfAnyUser(dbSession, SCAN.getKey(), project);

    verify(auditPersister).addComponent(any(), any());
    verifyNoMoreInteractions(auditPersister);
  }

  @Test
  void deleteUserPermissionByUserUuidIsPersisted() {
    UserDto user = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    EntityDto project = db.components().insertPrivateProject().getProjectDto();
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), ADMINISTER.getKey(), user.getUuid(), project.getUuid());
    underTest.insert(dbSession, dto, project, user, null);
    underTest.deleteByUserUuid(dbSession, user);

    verify(auditPersister).deleteUserPermission(eq(dbSession), newValueCaptor.capture());
    UserPermissionNewValue newValue = newValueCaptor.getValue();
    assertNewValue(newValue, null, user.getUuid(), user.getLogin(), null, null, null,
      null, null);
    assertThat(newValue.toString()).contains("userUuid");
  }

  @Test
  void deleteUserPermissionByUserUuidWithoutAffectedRowsIsNotPersisted() {
    UserDto user = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));

    underTest.deleteByUserUuid(dbSession, user);

    verify(auditPersister).addUser(any(), any());
    verifyNoMoreInteractions(auditPersister);
  }

  private void assertNewValue(UserPermissionNewValue newValue, String permissionUuid, String userUuid, String userLogin,
    String componentUuid,
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
