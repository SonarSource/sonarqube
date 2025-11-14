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
package org.sonar.db.project;

import javax.annotation.Nullable;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.ProjectBadgeTokenNewValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ProjectBadgeTokenDaoIT {

  private final System2 system2 = new TestSystem2().setNow(1000L);

  @RegisterExtension
  private final DbTester db = DbTester.create(system2);

  private final AuditPersister auditPersister = spy(AuditPersister.class);
  private final UuidFactory uuidFactory = mock(UuidFactory.class);

  private final ProjectBadgeTokenDao projectBadgeTokenDao = new ProjectBadgeTokenDao(system2, auditPersister, uuidFactory);

  @Test
  void should_insert_and_select_by_project_uuid() {
    when(uuidFactory.create()).thenReturn("generated_uuid_1");
    ProjectDto projectDto = new ProjectDto().setUuid("project_uuid_1");

    ProjectBadgeTokenDto insertedProjectBadgeToken = projectBadgeTokenDao.insert(db.getSession(), "token", projectDto, "userUuid",
      "userLogin");
    assertProjectBadgeToken(insertedProjectBadgeToken, "token");

    ProjectBadgeTokenDto selectedProjectBadgeToken = projectBadgeTokenDao.selectTokenByProject(db.getSession(), projectDto);
    assertProjectBadgeToken(selectedProjectBadgeToken, "token");
  }

  @Test
  void token_insertion_is_log_in_audit() {
    when(uuidFactory.create()).thenReturn("generated_uuid_1");
    ProjectDto projectDto = new ProjectDto().setUuid("project_uuid_1");

    ProjectBadgeTokenDto insertedProjectBadgeToken = projectBadgeTokenDao.insert(db.getSession(), "token", projectDto, "user-uuid", "user" +
      "-login");
    assertProjectBadgeToken(insertedProjectBadgeToken, "token");

    ArgumentCaptor<ProjectBadgeTokenNewValue> captor = ArgumentCaptor.forClass(ProjectBadgeTokenNewValue.class);

    verify(auditPersister).addProjectBadgeToken(eq(db.getSession()), captor.capture());
    verifyNoMoreInteractions(auditPersister);

    Assertions.assertThat(captor.getValue()).hasToString("{\"userUuid\": \"user-uuid\", \"userLogin\": \"user-login\" }");
  }

  @Test
  void upsert_existing_token_and_select_by_project_uuid() {
    when(uuidFactory.create()).thenReturn("generated_uuid_1");
    ProjectDto projectDto = new ProjectDto().setUuid("project_uuid_1");

    // first insert
    ProjectBadgeTokenDto insertedProjectBadgeToken = projectBadgeTokenDao.insert(db.getSession(), "token", projectDto, "user-uuid", "user" +
      "-login");
    assertProjectBadgeToken(insertedProjectBadgeToken, "token");

    // renew
    projectBadgeTokenDao.upsert(db.getSession(), "new-token", projectDto, "user-uuid", "user-login");
    ProjectBadgeTokenDto selectedProjectBadgeToken = projectBadgeTokenDao.selectTokenByProject(db.getSession(), projectDto);
    assertProjectBadgeToken(selectedProjectBadgeToken, "new-token");
  }

  @Test
  void upsert_non_existing_token_and_select_by_project_uuid() {
    when(uuidFactory.create()).thenReturn("generated_uuid_1");
    ProjectDto projectDto = new ProjectDto().setUuid("project_uuid_1");

    // renew
    projectBadgeTokenDao.upsert(db.getSession(), "new-token", projectDto, "user-uuid", "user-login");
    ProjectBadgeTokenDto selectedProjectBadgeToken = projectBadgeTokenDao.selectTokenByProject(db.getSession(), projectDto);
    assertProjectBadgeToken(selectedProjectBadgeToken, "new-token");
  }


  @Test
  void token_upsert_is_log_in_audit() {
    when(uuidFactory.create()).thenReturn("generated_uuid_1");
    ProjectDto projectDto = new ProjectDto().setUuid("project_uuid_1");

    // fist insert
    projectBadgeTokenDao.insert(db.getSession(), "token", projectDto, "user-uuid", "user-login");
    ArgumentCaptor<ProjectBadgeTokenNewValue> captor = ArgumentCaptor.forClass(ProjectBadgeTokenNewValue.class);
    verify(auditPersister).addProjectBadgeToken(eq(db.getSession()), captor.capture());

    // upsert
    projectBadgeTokenDao.upsert(db.getSession(), "new-token", projectDto, "user-uuid", "user-login");
    ProjectBadgeTokenDto selectedProjectBadgeToken = projectBadgeTokenDao.selectTokenByProject(db.getSession(), projectDto);
    assertProjectBadgeToken(selectedProjectBadgeToken, "new-token");

    verify(auditPersister).updateProjectBadgeToken(eq(db.getSession()), captor.capture());
    verifyNoMoreInteractions(auditPersister);

    Assertions.assertThat(captor.getValue()).hasToString("{\"userUuid\": \"user-uuid\", \"userLogin\": \"user-login\" }");
  }

  private void assertProjectBadgeToken(@Nullable ProjectBadgeTokenDto projectBadgeTokenDto, String expectedToken) {
    assertThat(projectBadgeTokenDto).isNotNull();
    assertThat(projectBadgeTokenDto.getToken()).isEqualTo(expectedToken);
    assertThat(projectBadgeTokenDto.getProjectUuid()).isEqualTo("project_uuid_1");
    assertThat(projectBadgeTokenDto.getUuid()).isEqualTo("generated_uuid_1");
    assertThat(projectBadgeTokenDto.getCreatedAt()).isEqualTo(1000L);
    assertThat(projectBadgeTokenDto.getCreatedAt()).isEqualTo(1000L);
  }

}
