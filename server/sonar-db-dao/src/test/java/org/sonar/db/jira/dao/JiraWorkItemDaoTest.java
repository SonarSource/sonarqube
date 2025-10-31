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
package org.sonar.db.jira.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbTester;
import org.sonar.db.jira.dto.JiraWorkItemDto;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JiraWorkItemDaoTest {

  private final System2 system2 = mock(System2.class);
  private final UuidFactory uuidFactory = mock(UuidFactory.class);

  @RegisterExtension
  private final DbTester db = DbTester.create(system2);

  private final JiraWorkItemDao underTest = new JiraWorkItemDao(system2, uuidFactory);

  @Test
  void insertOrUpdate_shouldInsertNewWorkItem() {
    when(system2.now()).thenReturn(1000L);
    when(uuidFactory.create()).thenReturn("work-item-uuid-1");

    var dto = new JiraWorkItemDto()
      .setJiraIssueId("jira-issue-123")
      .setJiraIssueKey("PROJ-123")
      .setJiraIssueUrl("https://jira.example.com/browse/PROJ-123")
      .setJiraProjectBindingId("binding-uuid-1");

    var result = underTest.insertOrUpdate(db.getSession(), dto);

    assertThat(result.getId()).isEqualTo("work-item-uuid-1");
    assertThat(result.getJiraIssueId()).isEqualTo("jira-issue-123");
    assertThat(result.getJiraIssueKey()).isEqualTo("PROJ-123");
    assertThat(result.getJiraIssueUrl()).isEqualTo("https://jira.example.com/browse/PROJ-123");
    assertThat(result.getJiraProjectBindingId()).isEqualTo("binding-uuid-1");
    assertThat(result.getCreatedAt()).isEqualTo(1000L);
    assertThat(result.getUpdatedAt()).isEqualTo(1000L);

    var workItem = underTest.findById(db.getSession(), "work-item-uuid-1");
    assertThat(workItem).isPresent();
    assertThat(workItem.get().getId()).isEqualTo("work-item-uuid-1");
  }

  @Test
  void insertOrUpdate_shouldUpdateExistingWorkItem() {
    when(system2.now()).thenReturn(1000L, 2000L);
    when(uuidFactory.create()).thenReturn("work-item-uuid-1");

    var dto = new JiraWorkItemDto()
      .setJiraIssueId("jira-issue-123")
      .setJiraIssueKey("PROJ-123")
      .setJiraIssueUrl("https://jira.example.com/browse/PROJ-123")
      .setJiraProjectBindingId("binding-uuid-1");

    underTest.insertOrUpdate(db.getSession(), dto);

    // Update the work item
    dto.setJiraIssueKey("PROJ-456");
    dto.setJiraIssueUrl("https://jira.example.com/browse/PROJ-456");

    var updated = underTest.insertOrUpdate(db.getSession(), dto);

    assertThat(updated.getJiraIssueKey()).isEqualTo("PROJ-456");
    assertThat(updated.getJiraIssueUrl()).isEqualTo("https://jira.example.com/browse/PROJ-456");
    assertThat(updated.getUpdatedAt()).isEqualTo(2000L);
    assertThat(updated.getCreatedAt()).isEqualTo(1000L); // Should remain unchanged

    var workItem = underTest.findById(db.getSession(), "work-item-uuid-1");
    assertThat(workItem).isPresent();
    assertThat(workItem.get().getJiraIssueKey()).isEqualTo("PROJ-456");
  }

  @Test
  void findById_shouldReturnEmpty_whenNotFound() {
    var result = underTest.findById(db.getSession(), "non-existent");

    assertThat(result).isEmpty();
  }

  @Test
  void findById_shouldReturnWorkItem_whenExists() {
    when(system2.now()).thenReturn(1000L);
    when(uuidFactory.create()).thenReturn("work-item-uuid-1");

    var dto = new JiraWorkItemDto()
      .setJiraIssueId("jira-issue-123")
      .setJiraIssueKey("PROJ-123")
      .setJiraIssueUrl("https://jira.example.com/browse/PROJ-123")
      .setJiraProjectBindingId("binding-uuid-1");

    underTest.insertOrUpdate(db.getSession(), dto);

    var result = underTest.findById(db.getSession(), "work-item-uuid-1");

    assertThat(result).isPresent();
    assertThat(result.get().getJiraIssueId()).isEqualTo("jira-issue-123");
  }

  @Test
  void deleteById_shouldDeleteWorkItem() {
    when(system2.now()).thenReturn(1000L);
    when(uuidFactory.create()).thenReturn("work-item-uuid-1");
    var dto = new JiraWorkItemDto()
      .setJiraIssueId("jira-issue-123")
      .setJiraIssueKey("PROJ-123")
      .setJiraIssueUrl("https://jira.example.com/browse/PROJ-123")
      .setJiraProjectBindingId("binding-uuid-1");
    underTest.insertOrUpdate(db.getSession(), dto);
    assertThat(underTest.findById(db.getSession(), "work-item-uuid-1")).isPresent();

    var count = underTest.deleteById(db.getSession(), "work-item-uuid-1");

    assertThat(count).isEqualTo(1);
    assertThat(underTest.findById(db.getSession(), "work-item-uuid-1")).isEmpty();
  }

  @Test
  void deleteById_shouldDoNothing_whenNotExists() {
    assertDoesNotThrow(() -> underTest.deleteById(db.getSession(), "non-existent"));
  }

  @Test
  void countAll_shouldReturnZero_whenNoWorkItems() {
    int count = underTest.countAll(db.getSession());

    assertThat(count).isZero();
  }

  @Test
  void countAll_shouldReturnCorrectCount_whenWorkItemsExist() {
    when(uuidFactory.create()).thenReturn("work-item-uuid-1", "work-item-uuid-2", "work-item-uuid-3");
    insertWorkItem("jira-issue-1", "PROJ-1", "binding-uuid-1");
    insertWorkItem("jira-issue-2", "PROJ-2", "binding-uuid-1");
    insertWorkItem("jira-issue-3", "PROJ-3", "binding-uuid-2");

    int count = underTest.countAll(db.getSession());

    assertThat(count).isEqualTo(3);
  }

  @Test
  void countAll_shouldReturnCorrectCount_afterDeletion() {
    when(uuidFactory.create()).thenReturn("work-item-uuid-1", "work-item-uuid-2");
    insertWorkItem("jira-issue-1", "PROJ-1", "binding-uuid-1");
    insertWorkItem("jira-issue-2", "PROJ-2", "binding-uuid-1");
    assertThat(underTest.countAll(db.getSession())).isEqualTo(2);

    underTest.deleteById(db.getSession(), "work-item-uuid-1");

    assertThat(underTest.countAll(db.getSession())).isEqualTo(1);
  }

  @Test
  void findByResource_shouldReturnEmpty_whenNoLinkedResources() {
    var result = underTest.findByResource(db.getSession(), "resource-1", "SONAR_ISSUE");

    assertThat(result).isEmpty();
  }

  @Test
  void findByResource_shouldReturnWorkItems_whenLinkedResourcesExist() {
    var resourceId = "work-item-with-linked-resource";
    when(system2.now()).thenReturn(1000L);
    when(uuidFactory.create())
      .thenReturn("work-item-uuid-1", "work-item-uuid-2", "work-item-uuid-3");

    // Insert work items
    insertWorkItem("jira-issue-1", "PROJ-1", "binding-uuid-1");
    insertWorkItem("jira-issue-2", "PROJ-2", "binding-uuid-1");
    insertWorkItem("jira-issue-3", "PROJ-3", "binding-uuid-2");

    // Link work items to resources
    insertLinkedResource("work-item-uuid-1", resourceId, "SONAR_ISSUE");
    insertLinkedResource("work-item-uuid-2", resourceId, "SONAR_ISSUE");

    var result = underTest.findByResource(db.getSession(), resourceId, "SONAR_ISSUE");

    assertThat(result).hasSize(2);
    assertThat(result)
      .extracting(JiraWorkItemDto::getId)
      .containsExactlyInAnyOrder("work-item-uuid-1", "work-item-uuid-2");
  }

  @Test
  void findByResource_shouldFilterByResourceType() {
    var resourceId = "resource-work-items-filter-by-type";
    when(system2.now()).thenReturn(1000L);
    when(uuidFactory.create()).thenReturn("work-item-uuid-1", "work-item-uuid-2");

    insertWorkItem("jira-issue-1", "PROJ-1", "binding-uuid-1");
    insertWorkItem("jira-issue-2", "PROJ-2", "binding-uuid-1");

    insertLinkedResource("work-item-uuid-1", resourceId, "SONAR_ISSUE");
    insertLinkedResource("work-item-uuid-2", resourceId, "DEPENDENCY_RISK");

    var result = underTest.findByResource(db.getSession(), resourceId, "SONAR_ISSUE");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo("work-item-uuid-1");
  }

  @Test
  void findByResource_shouldReturnEmpty_whenResourceIdDoesNotMatch() {
    var existingResourceId = "resource-work-items-existing";
    var nonExistingResourceId = "resource-work-items-non-existing";
    when(system2.now()).thenReturn(1000L);
    when(uuidFactory.create()).thenReturn("work-item-uuid-1");

    insertWorkItem("jira-issue-1", "PROJ-1", "binding-uuid-1");
    insertLinkedResource("work-item-uuid-1", existingResourceId, "SONAR_ISSUE");

    var result = underTest.findByResource(db.getSession(), nonExistingResourceId, "SONAR_ISSUE");

    assertThat(result).isEmpty();
  }

  private void insertWorkItem(String jiraIssueId, String jiraIssueKey, String jiraProjectBindingId) {
    underTest.insertOrUpdate(db.getSession(), new JiraWorkItemDto()
      .setJiraIssueId(jiraIssueId)
      .setJiraIssueKey(jiraIssueKey)
      .setJiraIssueUrl("https://jira.example.com/browse/" + jiraIssueKey)
      .setJiraProjectBindingId(jiraProjectBindingId));
  }

  private void insertLinkedResource(String workItemId, String resourceId, String resourceType) {
    db.executeInsert("jira_work_items_resources",
      "id", UUID.randomUUID().toString(),
      "jira_work_item_id", workItemId,
      "resource_id", resourceId,
      "resource_type", resourceType,
      "created_at", 1000L,
      "updated_at", 1000L
    );
  }
}
