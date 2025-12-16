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
package org.sonar.db.jira.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JiraWorkItemDtoTest {

  @Test
  void setters_and_getters() {
    var dto = new JiraWorkItemDto()
      .setId("uuid-1")
      .setJiraIssueId("jira-issue-123")
      .setJiraIssueKey("PROJ-123")
      .setJiraIssueUrl("https://jira.example.com/browse/PROJ-123")
      .setJiraProjectBindingId("binding-uuid-1")
      .setCreatedBy("user-uuid-1")
      .setCreatedAt(1000L)
      .setUpdatedAt(2000L);

    assertThat(dto.getId()).isEqualTo("uuid-1");
    assertThat(dto.getJiraIssueId()).isEqualTo("jira-issue-123");
    assertThat(dto.getJiraIssueKey()).isEqualTo("PROJ-123");
    assertThat(dto.getJiraIssueUrl()).isEqualTo("https://jira.example.com/browse/PROJ-123");
    assertThat(dto.getJiraProjectBindingId()).isEqualTo("binding-uuid-1");
    assertThat(dto.getCreatedBy()).isEqualTo("user-uuid-1");
    assertThat(dto.getCreatedAt()).isEqualTo(1000L);
    assertThat(dto.getUpdatedAt()).isEqualTo(2000L);
  }

  @Test
  void setters_with_null_id() {
    var dto = new JiraWorkItemDto()
      .setId(null)
      .setJiraIssueId("jira-issue-123")
      .setJiraIssueKey("PROJ-123")
      .setJiraIssueUrl("https://jira.example.com/browse/PROJ-123")
      .setJiraProjectBindingId("binding-uuid-1");

    assertThat(dto.getId()).isNull();
  }

  @Test
  void toString_does_not_fail_if_empty() {
    var dto = new JiraWorkItemDto();

    assertThat(dto.toString()).isNotEmpty();
  }

  @Test
  void toString_contains_all_fields() {
    var dto = new JiraWorkItemDto()
      .setId("uuid-1")
      .setJiraIssueId("jira-issue-123")
      .setJiraIssueKey("PROJ-123")
      .setJiraIssueUrl("https://jira.example.com/browse/PROJ-123")
      .setJiraProjectBindingId("binding-uuid-1")
      .setCreatedBy("user-uuid-1")
      .setCreatedAt(1000L)
      .setUpdatedAt(2000L);

    var toString = dto.toString();

    assertThat(toString).isEqualToIgnoringWhitespace("""
      org.sonar.db.jira.dto.JiraWorkItemDto[
      createdAt='1000',
      createdBy='user-uuid-1',
      id='uuid-1',
      jiraIssueId='jira-issue-123',
      jiraIssueKey='PROJ-123',
      jiraIssueUrl='https://jira.example.com/browse/PROJ-123',
      jiraProjectBindingId='binding-uuid-1',
      updatedAt='2000'
      ]
      """);
  }

}
