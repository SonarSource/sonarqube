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

class JiraOrganizationBindingNewValueTest {

  private static final String BINDING_ID = "binding-id-123";
  private static final String SONAR_ORGANIZATION_UUID = "00000000-0000-0000-0000-000000000001";
  private static final String JIRA_CLOUD_ID = "cloud-id-456";
  private static final String JIRA_INSTANCE_URL = "https://jira.example.com";

  @Test
  void toString_shouldFormatAllFieldsCorrectly() {
    var result = new JiraOrganizationBindingNewValue(
      "binding-id-123",
      "00000000-0000-0000-0000-000000000001",
      "cloud-id-456",
      "https://jira.example.com",
      true
    ).toString();

    assertThat(result).isEqualTo(
      "{\"bindingId\": \"binding-id-123\", \"sonarOrganizationUuid\": \"00000000-0000-0000-0000-000000000001\", \"jiraCloudId\": \"cloud-id-456\", \"jiraInstanceUrl\": \"https://jira.example.com\", \"isTokenShared\": true }");
  }

  @Test
  void toString_withFalseIsTokenShared_shouldIncludeIsTokenSharedField() {
    var result = new JiraOrganizationBindingNewValue(
      "binding-id-123",
      "00000000-0000-0000-0000-000000000001",
      "cloud-id-456",
      "https://jira.example.com",
      false
    ).toString();

    assertThat(result).isEqualTo(
      "{\"bindingId\": \"binding-id-123\", \"sonarOrganizationUuid\": \"00000000-0000-0000-0000-000000000001\", \"jiraCloudId\": \"cloud-id-456\", \"jiraInstanceUrl\": \"https://jira.example.com\", \"isTokenShared\": false }");
  }

  @Test
  void getters_shouldReturnCorrectValues() {
    var newValue = new JiraOrganizationBindingNewValue(BINDING_ID, SONAR_ORGANIZATION_UUID, JIRA_CLOUD_ID, JIRA_INSTANCE_URL, true);
    assertThat(newValue.getBindingId()).isEqualTo(BINDING_ID);
    assertThat(newValue.getSonarOrganizationUuid()).isEqualTo(SONAR_ORGANIZATION_UUID);
    assertThat(newValue.getJiraCloudId()).isEqualTo(JIRA_CLOUD_ID);
    assertThat(newValue.getJiraInstanceUrl()).isEqualTo(JIRA_INSTANCE_URL);
    assertThat(newValue.getIsTokenShared()).isTrue();
  }
}
