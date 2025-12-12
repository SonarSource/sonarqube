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

class JiraProjectBindingNewValueTest {

  private static final String BINDING_ID = "binding-id-123";
  private static final String SONAR_PROJECT_ID = "sonar-project-id-456";
  private static final String JIRA_ORG_BINDING_ID = "00000000-0000-0000-0000-000000000001";
  private static final String JIRA_PROJECT_KEY = "JIRA-123";

  @Test
  void toString_shouldFormatAllFieldsCorrectly() {
    var result = new JiraProjectBindingNewValue(
      BINDING_ID,
      SONAR_PROJECT_ID,
      JIRA_ORG_BINDING_ID,
      JIRA_PROJECT_KEY
    ).toString();

    assertThat(result).isEqualTo("{\"bindingId\": \"binding-id-123\", \"sonarProjectId\": \"sonar-project-id-456\", \"jiraOrganizationBindingId\": \"00000000-0000-0000-0000-000000000001\", \"jiraProjectKey\": \"JIRA-123\" }");
  }

  @Test
  void getters_shouldReturnCorrectValues() {
    var newValue = new JiraProjectBindingNewValue(BINDING_ID, SONAR_PROJECT_ID, JIRA_ORG_BINDING_ID, JIRA_PROJECT_KEY);

    assertThat(newValue.getBindingId()).isEqualTo(BINDING_ID);
    assertThat(newValue.getSonarProjectId()).isEqualTo(SONAR_PROJECT_ID);
    assertThat(newValue.getJiraOrganizationBindingId()).isEqualTo(JIRA_ORG_BINDING_ID);
    assertThat(newValue.getJiraProjectKey()).isEqualTo(JIRA_PROJECT_KEY);
  }
}

