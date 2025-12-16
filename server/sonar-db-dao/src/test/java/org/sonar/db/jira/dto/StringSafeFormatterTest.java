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

class StringSafeFormatterTest {

  @Test
  void toString_shouldMaskSecrets_jiraOrganizationBindingDto() {
    var dto = new JiraOrganizationBindingDto()
      .setId("my-id")
      .setCreatedAt(42)
      .setUpdatedAt(43)
      .setSonarOrganizationUuid("org-uuid")
      .setJiraInstanceUrl("sonar.com")
      .setJiraCloudId("cloud-9")
      .setJiraAccessToken("secret")
      .setJiraAccessTokenExpiresAt(500)
      .setJiraRefreshToken("very secret")
      .setJiraRefreshTokenCreatedAt(501)
      .setJiraRefreshTokenUpdatedAt(502)
      .setUpdatedBy("me")
      .setIsTokenShared(true);

    var toString = dto.toString();

    assertThat(toString)
      .isEqualToIgnoringWhitespace("""
        org.sonar.db.jira.dto.JiraOrganizationBindingDto[
        createdAt='42',
        id='my-id',
        isTokenShared='true',
        jiraAccessToken='***',
        jiraAccessTokenExpiresAt='***',
        jiraCloudId='cloud-9',
        jiraInstanceUrl='sonar.com',
        jiraRefreshToken='***',
        jiraRefreshTokenCreatedAt='***',
        jiraRefreshTokenUpdatedAt='***',
        sonarOrganizationUuid='org-uuid',
        updatedAt='43',
        updatedBy='me'
        ]
        """);
  }

  @Test
  void toString_shouldMaskSecrets_jiraOrganizationBindingPendingDto() {
    var dto = new JiraOrganizationBindingPendingDto()
      .setId("my-id")
      .setCreatedAt(42)
      .setUpdatedAt(43)
      .setSonarOrganizationUuid("org-uuid")
      .setJiraAccessToken("secret")
      .setJiraAccessTokenExpiresAt(500)
      .setJiraRefreshToken("very secret")
      .setJiraRefreshTokenCreatedAt(501)
      .setJiraRefreshTokenUpdatedAt(502)
      .setUpdatedBy("me");


    var toString = dto.toString();

    assertThat(toString)
      .isEqualToIgnoringWhitespace("""
        org.sonar.db.jira.dto.JiraOrganizationBindingPendingDto[
        createdAt='42',
        id='my-id',
        jiraAccessToken='***',
        jiraAccessTokenExpiresAt='***',
        jiraRefreshToken='***',
        jiraRefreshTokenCreatedAt='***',
        jiraRefreshTokenUpdatedAt='***',
        sonarOrganizationUuid='org-uuid',
        updatedAt='43',
        updatedBy='me'
        ]
        """);
  }

  @Test
  void toString_shouldMaskSecrets_atlassianAuthenticationDetailsDto() {
    var dto = new AtlassianAuthenticationDetailsDto()
      .setClientId("test-client-id")
      .setSecret("super-secret-value")
      .setCreatedAt(1000L)
      .setUpdatedAt(2000L);

    var toString = dto.toString();

    assertThat(toString)
      .isEqualToIgnoringWhitespace("""
        org.sonar.db.jira.dto.AtlassianAuthenticationDetailsDto[
        clientId='test-client-id',
        createdAt='1000',
        secret='***',
        updatedAt='2000'
        ]
        """);
  }

  @Test
  void toString_masks_secret_atlassianAuthenticationDetailsDto_even_when_null() {
    var dto = new AtlassianAuthenticationDetailsDto()
      .setClientId("test-client-id")
      .setSecret(null);

    var toString = dto.toString();

    assertThat(toString)
      .isEqualToIgnoringWhitespace("""
        org.sonar.db.jira.dto.AtlassianAuthenticationDetailsDto[
        clientId='test-client-id',
        createdAt='0',
        secret='***',
        updatedAt='0'
        ]
        """);
  }
}
