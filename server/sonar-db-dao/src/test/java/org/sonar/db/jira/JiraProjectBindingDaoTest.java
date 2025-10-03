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
package org.sonar.db.jira;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JiraProjectBindingDaoTest {

  private final System2 system2 = mock(System2.class);
  private final UuidFactory uuidFactory = mock(UuidFactory.class);

  @RegisterExtension
  private final DbTester db = DbTester.create(system2);

  private final JiraProjectBindingDao underTest = new JiraProjectBindingDao(system2, uuidFactory);

  @Test
  void insertOrUpdate_shouldInsertNewBinding() {
    when(system2.now()).thenReturn(1000L);
    when(uuidFactory.create()).thenReturn("uuid-1");

    var dto = new JiraProjectBindingDto()
      .setSonarProjectId("sonar-project-1")
      .setJiraOrganizationBindingId("jira-org-binding-1")
      .setJiraProjectKey("PROJ-1");

    var result = underTest.insertOrUpdate(db.getSession(), dto);

    assertThat(result.getId()).isEqualTo("uuid-1");
    assertThat(result.getSonarProjectId()).isEqualTo("sonar-project-1");
    assertThat(result.getJiraOrganizationBindingId()).isEqualTo("jira-org-binding-1");
    assertThat(result.getJiraProjectKey()).isEqualTo("PROJ-1");
    assertThat(result.getCreatedAt()).isEqualTo(1000L);
    assertThat(result.getUpdatedAt()).isEqualTo(1000L);

    var jiraProjectBindingDto = underTest.selectById(db.getSession(), "uuid-1");
    assertThat(jiraProjectBindingDto).isPresent();
    assertThat(jiraProjectBindingDto.get().getId()).isEqualTo("uuid-1");
  }

  @Test
  void insertOrUpdate_shouldUpdateExistingBinding() {
    when(system2.now()).thenReturn(1000L, 2000L);
    when(uuidFactory.create()).thenReturn("uuid-1");

    var dto = new JiraProjectBindingDto()
      .setSonarProjectId("sonar-project-1")
      .setJiraOrganizationBindingId("jira-org-1")
      .setJiraProjectKey("PROJ-1");

    underTest.insertOrUpdate(db.getSession(), dto);

    // Update the binding
    dto.setJiraOrganizationBindingId("jira-org-2");
    dto.setJiraProjectKey("PROJ-2");

    var updated = underTest.insertOrUpdate(db.getSession(), dto);

    assertThat(updated.getJiraOrganizationBindingId()).isEqualTo("jira-org-2");
    assertThat(updated.getJiraProjectKey()).isEqualTo("PROJ-2");
    assertThat(updated.getUpdatedAt()).isEqualTo(2000L);
    assertThat(updated.getCreatedAt()).isEqualTo(1000L); // Should remain unchanged

    var jiraProjectBindingDto = underTest.selectBySonarProjectId(db.getSession(), "sonar-project-1");
    assertThat(jiraProjectBindingDto).isPresent();
    assertThat(jiraProjectBindingDto.get().getJiraProjectKey()).isEqualTo("PROJ-2");
  }

  @Test
  void selectById_shouldReturnEmpty_whenNotFound() {
    var result = underTest.selectById(db.getSession(), "non-existent");

    assertThat(result).isEmpty();
  }

  @Test
  void selectById_shouldReturnBinding_whenExists() {
    when(system2.now()).thenReturn(1000L);
    when(uuidFactory.create()).thenReturn("uuid-1");

    var dto = new JiraProjectBindingDto()
      .setSonarProjectId("sonar-project-1")
      .setJiraOrganizationBindingId("jira-org-1")
      .setJiraProjectKey("PROJ-1");

    underTest.insertOrUpdate(db.getSession(), dto);

    var result = underTest.selectById(db.getSession(), "uuid-1");

    assertThat(result).isPresent();
    assertThat(result.get().getSonarProjectId()).isEqualTo("sonar-project-1");
  }

  @Test
  void selectBySonarProjectId_shouldReturnEmpty_whenNotFound() {
    var result = underTest.selectBySonarProjectId(db.getSession(), "non-existent");

    assertThat(result).isEmpty();
  }

  @Test
  void selectBySonarProjectId_shouldReturnBinding_whenExists() {
    when(system2.now()).thenReturn(1000L);
    when(uuidFactory.create()).thenReturn("uuid-1");

    var dto = new JiraProjectBindingDto()
      .setSonarProjectId("sonar-project-1")
      .setJiraOrganizationBindingId("jira-org-1")
      .setJiraProjectKey("PROJ-1");

    underTest.insertOrUpdate(db.getSession(), dto);

    var result = underTest.selectBySonarProjectId(db.getSession(), "sonar-project-1");

    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo("uuid-1");
    assertThat(result.get().getJiraProjectKey()).isEqualTo("PROJ-1");
  }

  @Test
  void deleteBySonarProjectId_shouldDeleteBinding() {
    when(system2.now()).thenReturn(1000L);
    when(uuidFactory.create()).thenReturn("uuid-1");
    var dto = new JiraProjectBindingDto()
      .setSonarProjectId("sonar-project-1")
      .setJiraOrganizationBindingId("jira-org-1")
      .setJiraProjectKey("PROJ-1");
    underTest.insertOrUpdate(db.getSession(), dto);
    assertThat(underTest.selectBySonarProjectId(db.getSession(), "sonar-project-1")).isPresent();

    underTest.deleteBySonarProjectId(db.getSession(), "sonar-project-1");

    assertThat(underTest.selectBySonarProjectId(db.getSession(), "sonar-project-1")).isEmpty();
  }

  @Test
  void deleteBySonarProjectId_shouldDoNothing_whenNotExists() {
    assertDoesNotThrow(() -> underTest.deleteBySonarProjectId(db.getSession(), "non-existent"));
  }
}
