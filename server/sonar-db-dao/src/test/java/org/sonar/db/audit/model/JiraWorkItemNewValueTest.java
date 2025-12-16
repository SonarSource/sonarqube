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
package org.sonar.db.audit.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JiraWorkItemNewValueTest {

  private static final String WORK_ITEM_ID = "work-item-id-123";
  private static final String JIRA_ISSUE_ID = "jira-issue-10001";
  private static final String JIRA_ISSUE_KEY = "JIRA-456";
  private static final String JIRA_ISSUE_URL = "https://jira.example.com/browse/JIRA-456";
  private static final String JIRA_PROJECT_BINDING_ID = "00000000-0000-0000-0000-000000000001";

  @Test
  void toString_shouldFormatAllFieldsCorrectly() {
    var result = new JiraWorkItemNewValue(
      WORK_ITEM_ID,
      JIRA_ISSUE_ID,
      JIRA_ISSUE_KEY,
      JIRA_ISSUE_URL,
      JIRA_PROJECT_BINDING_ID
    ).toString();

    assertThat(result).isEqualTo(
      "{\"workItemId\": \"work-item-id-123\", \"jiraIssueId\": \"jira-issue-10001\", \"jiraIssueKey\": \"JIRA-456\", \"jiraIssueUrl\": \"https://jira.example.com/browse/JIRA-456\", \"jiraProjectBindingId\": \"00000000-0000-0000-0000-000000000001\" }");
  }

  @Test
  void getters_shouldReturnCorrectValues() {
    var newValue = new JiraWorkItemNewValue(WORK_ITEM_ID, JIRA_ISSUE_ID, JIRA_ISSUE_KEY, JIRA_ISSUE_URL, JIRA_PROJECT_BINDING_ID);

    assertThat(newValue.getWorkItemId()).isEqualTo(WORK_ITEM_ID);
    assertThat(newValue.getJiraIssueId()).isEqualTo(JIRA_ISSUE_ID);
    assertThat(newValue.getJiraIssueKey()).isEqualTo(JIRA_ISSUE_KEY);
    assertThat(newValue.getJiraIssueUrl()).isEqualTo(JIRA_ISSUE_URL);
    assertThat(newValue.getJiraProjectBindingId()).isEqualTo(JIRA_PROJECT_BINDING_ID);
  }
}
