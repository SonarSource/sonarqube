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
package org.sonar.db.jira;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbTester;
import org.sonar.db.jira.dao.JiraOrganizationBindingDao;
import org.sonar.db.jira.dao.JiraProjectBindingDao;
import org.sonar.db.jira.dao.JiraSelectedWorkTypeDao;
import org.sonar.db.jira.dao.JiraWorkItemDao;
import org.sonar.db.jira.dto.JiraOrganizationBindingDto;
import org.sonar.db.jira.dto.JiraProjectBindingDto;
import org.sonar.db.jira.dto.JiraSelectedWorkTypeDto;
import org.sonar.db.jira.dto.JiraWorkItemDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JiraOrganizationBindingDaoTest {

  private final System2 system2 = mock(System2.class);
  private final UuidFactory uuidFactory = mock(UuidFactory.class);

  @RegisterExtension
  private final DbTester db = DbTester.create(system2);

  private final JiraOrganizationBindingDao underTest = new JiraOrganizationBindingDao(system2);
  private final JiraProjectBindingDao projectBindingDao = new JiraProjectBindingDao(system2, uuidFactory);
  private final JiraWorkItemDao workItemDao = new JiraWorkItemDao(system2, uuidFactory);
  private final JiraSelectedWorkTypeDao selectedWorkTypeDao = new JiraSelectedWorkTypeDao(system2, uuidFactory);

  @Test
  void insert_shouldInsertNewBinding() {
    var now = 1000L;
    when(system2.now()).thenReturn(now);
    var dto = new JiraOrganizationBindingDto()
      .setId("binding-1")
      .setSonarOrganizationUuid("org-uuid-1")
      .setJiraInstanceUrl("https://jira.example.com")
      .setJiraCloudId("cloud-123")
      .setJiraAccessToken("access-token")
      .setJiraAccessTokenExpiresAt(2000L)
      .setJiraRefreshToken("refresh-token")
      .setJiraRefreshTokenCreatedAt(1000L)
      .setJiraRefreshTokenUpdatedAt(1000L)
      .setUpdatedBy("user-uuid")
      .setIsTokenShared(true);

    var result = underTest.insert(db.getSession(), dto);

    assertThat(result.getId()).isEqualTo("binding-1");
    assertThat(result.getSonarOrganizationUuid()).isEqualTo("org-uuid-1");
    assertThat(result.getJiraInstanceUrl()).isEqualTo("https://jira.example.com");
    assertThat(result.getJiraCloudId()).isEqualTo("cloud-123");
    assertThat(result.getCreatedAt()).isEqualTo(now);
    assertThat(result.getUpdatedAt()).isEqualTo(now);
    assertThat(result.getJiraRefreshToken()).isEqualTo("refresh-token");
    assertThat(result.getJiraRefreshTokenCreatedAt()).isEqualTo(1000L);
    assertThat(result.getJiraRefreshTokenUpdatedAt()).isEqualTo(1000L);
    assertThat(result.getJiraAccessToken()).isEqualTo("access-token");
    assertThat(result.getJiraAccessTokenExpiresAt()).isEqualTo(2000L);
    assertThat(result.isTokenShared()).isTrue();
  }

  @Test
  void update_shouldUpdateExistingBinding() {
    when(system2.now()).thenReturn(1000L, 2000L);
    var dto = new JiraOrganizationBindingDto()
      .setId("binding-1")
      .setSonarOrganizationUuid("org-uuid-1")
      .setJiraInstanceUrl("https://jira.example.com")
      .setJiraCloudId("cloud-123")
      .setJiraAccessToken("access-token")
      .setJiraAccessTokenExpiresAt(2000L)
      .setJiraRefreshToken("refresh-token")
      .setJiraRefreshTokenCreatedAt(1000L)
      .setJiraRefreshTokenUpdatedAt(1000L)
      .setUpdatedBy("user-uuid")
      .setIsTokenShared(false);

    underTest.insert(db.getSession(), dto);

    // Update the dto
    dto.setJiraAccessToken("new-access-token");
    dto.setJiraAccessTokenExpiresAt(3000L);
    dto.setIsTokenShared(true);

    var updated = underTest.update(db.getSession(), dto);

    assertThat(updated.getJiraAccessToken()).isEqualTo("new-access-token");
    assertThat(updated.getJiraAccessTokenExpiresAt()).isEqualTo(3000L);
    assertThat(updated.getCreatedAt()).isEqualTo(1000L);
    assertThat(updated.getUpdatedAt()).isEqualTo(2000L);
    assertThat(updated.isTokenShared()).isTrue();

    var found = underTest.selectById(db.getSession(), "binding-1");
    assertThat(found).isPresent();
    assertThat(found.get().getJiraAccessToken()).isEqualTo("new-access-token");
    assertThat(found.get().getUpdatedAt()).isEqualTo(2000L);
    assertThat(found.get().isTokenShared()).isTrue();
  }

  @Test
  void selectById_shouldReturnEmpty_whenNotFound() {
    assertThat(underTest.selectById(db.getSession(), "non-existent")).isEmpty();
  }

  @Test
  void selectById_shouldReturnBinding_whenExists() {
    when(system2.now()).thenReturn(1000L);
    insertOrganization("binding-1", "org-uuid-1");

    var result = underTest.selectById(db.getSession(), "binding-1");

    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo("binding-1");
    assertThat(result.get().getSonarOrganizationUuid()).isEqualTo("org-uuid-1");
  }

  @Test
  void selectBySonarOrganizationUuid_shouldReturnEmpty_whenNotFound() {
    assertThat(underTest.selectBySonarOrganizationUuid(db.getSession(), "non-existent")).isEmpty();
  }

  @Test
  void selectBySonarOrganizationUuid_shouldReturnBinding_whenExists() {
    when(system2.now()).thenReturn(1000L);
    insertOrganization("binding-1", "org-uuid-1");

    var result = underTest.selectBySonarOrganizationUuid(db.getSession(), "org-uuid-1");

    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo("binding-1");
    assertThat(result.get().getSonarOrganizationUuid()).isEqualTo("org-uuid-1");
  }

  @Test
  void deleteBySonarOrganizationUuid_shouldDeleteBinding() {
    when(system2.now()).thenReturn(1000L);
    insertOrganization("binding-1", "org-uuid-1");

    var deletedCount = underTest.deleteBySonarOrganizationUuid(db.getSession(), "org-uuid-1");

    assertThat(deletedCount).isEqualTo(1);
    assertThat(underTest.selectBySonarOrganizationUuid(db.getSession(), "org-uuid-1")).isEmpty();
  }

  @Test
  void deleteBySonarOrganizationUuid_shouldReturnZero_whenNotFound() {
    var deletedCount = underTest.deleteBySonarOrganizationUuid(db.getSession(), "non-existent");

    assertThat(deletedCount).isZero();
    assertThat(underTest.selectBySonarOrganizationUuid(db.getSession(), "non-existent")).isEmpty();
  }

  @Test
  void deleteBySonarOrganizationUuid_shouldPerformCascadeDelete_withAllRelatedData() {
    when(system2.now()).thenReturn(1000L);
    when(uuidFactory.create()).thenReturn("project-1", "work-item-1", "linked-resource-1", "work-type-1");

    insertOrganization("org-binding-1", "org-uuid-1");
    var project1 = insertProject("org-binding-1", "sonar-project-1");
    var workItem1 = insertWorkItem(project1.getId(), "issue-1");
    workItemDao.insertLinkedResource(db.getSession(), workItem1.getId(), "resource-1", "ISSUE");
    insertWorkType(project1.getId(), "10001");

    // Verify that the data exist before deletion
    assertThat(underTest.selectById(db.getSession(), "org-binding-1")).isPresent();
    assertThat(projectBindingDao.selectById(db.getSession(), project1.getId())).isPresent();
    assertThat(workItemDao.findById(db.getSession(), workItem1.getId())).isPresent();
    assertThat(selectedWorkTypeDao.findByJiraProjectBindingId(db.getSession(), project1.getId())).hasSize(1);

    var deletedCount = underTest.deleteBySonarOrganizationUuid(db.getSession(), "org-uuid-1");

    // Verify count: 1 org + 1 project + 1 work item + 1 linked resource + 1 work type = 5
    assertThat(deletedCount).isEqualTo(5);

    // Verify all data is deleted
    assertThat(underTest.selectById(db.getSession(), "org-binding-1")).isEmpty();
    assertThat(projectBindingDao.selectById(db.getSession(), project1.getId())).isEmpty();
    assertThat(workItemDao.findById(db.getSession(), workItem1.getId())).isEmpty();
    assertThat(selectedWorkTypeDao.findByJiraProjectBindingId(db.getSession(), project1.getId())).isEmpty();
  }

  @Test
  void deleteBySonarOrganizationUuid_shouldDeleteOnlySpecificOrganizationData_whenMultipleOrganizationsExist() {
    when(system2.now()).thenReturn(1000L);
    when(uuidFactory.create()).thenReturn("project-1", "project-2", "work-item-1", "work-item-2");

    insertOrganization("org-1", "org-uuid-1");
    insertOrganization("org-2", "org-uuid-2");
    var project1 = insertProject("org-1", "sonar-project-1");
    var project2 = insertProject("org-2", "sonar-project-2");
    var workItem1 = insertWorkItem(project1.getId(), "issue-1");
    var workItem2 = insertWorkItem(project2.getId(), "issue-2");

    var deletedCount = underTest.deleteBySonarOrganizationUuid(db.getSession(), "org-uuid-1");

    // Should count only org1 data: 1 org + 1 project + 1 work item = 3
    assertThat(deletedCount).isEqualTo(3);

    // Verify org1 data is deleted
    assertThat(underTest.selectById(db.getSession(), "org-1")).isEmpty();
    assertThat(projectBindingDao.selectById(db.getSession(), project1.getId())).isEmpty();
    assertThat(workItemDao.findById(db.getSession(), workItem1.getId())).isEmpty();

    // Verify org2 data still exists
    assertThat(underTest.selectById(db.getSession(), "org-2")).isPresent();
    assertThat(projectBindingDao.selectById(db.getSession(), project2.getId())).isPresent();
    assertThat(workItemDao.findById(db.getSession(), workItem2.getId())).isPresent();
  }

  private JiraOrganizationBindingDto insertOrganization(String id, String sonarOrgUuid) {
    return underTest.insert(
      db.getSession(),
      new JiraOrganizationBindingDto()
        .setId(id)
        .setSonarOrganizationUuid(sonarOrgUuid)
        .setJiraInstanceUrl("https://jira.example.com"));
  }

  private JiraProjectBindingDto insertProject(String orgBindingId, String sonarProjectId) {
    return projectBindingDao.insertOrUpdate(
      db.getSession(),
      new JiraProjectBindingDto()
        .setSonarProjectId(sonarProjectId)
        .setJiraOrganizationBindingId(orgBindingId)
        .setJiraProjectKey("PROJ-KEY"));
  }

  private JiraWorkItemDto insertWorkItem(String projectBindingId, String issueId) {
    return workItemDao.insertOrUpdate(
      db.getSession(),
      new JiraWorkItemDto()
        .setJiraProjectBindingId(projectBindingId)
        .setJiraIssueId(issueId)
        .setJiraIssueKey("ISSUE-KEY")
        .setJiraIssueUrl("https://jira.example.com/browse/ISSUE-KEY"));
  }

  private void insertWorkType(String projectBindingId, String workTypeId) {
    selectedWorkTypeDao.saveAll(
      db.getSession(),
      List.of(new JiraSelectedWorkTypeDto()
        .setJiraProjectBindingId(projectBindingId)
        .setWorkTypeId(workTypeId)));
  }

  @Test
  void countAll_shouldReturnZero_whenNoBindings() {
    int count = underTest.countAll(db.getSession());

    assertThat(count).isZero();
  }

  @Test
  void countAll_shouldReturnCorrectCount_whenBindingsExist() {
    when(system2.now()).thenReturn(1000L);
    insertOrganization("binding-1", "org-uuid-1");
    insertOrganization("binding-2", "org-uuid-2");
    assertThat(underTest.countAll(db.getSession())).isEqualTo(2);

    underTest.deleteBySonarOrganizationUuid(db.getSession(), "org-uuid-1");

    assertThat(underTest.countAll(db.getSession())).isEqualTo(1);
  }
}
