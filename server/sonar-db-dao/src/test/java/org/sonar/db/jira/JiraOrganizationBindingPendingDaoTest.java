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
import org.sonar.db.DbTester;
import org.sonar.db.jira.dao.JiraOrganizationBindingPendingDao;
import org.sonar.db.jira.dto.JiraOrganizationBindingPendingDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JiraOrganizationBindingPendingDaoTest {

  private final System2 system2 = mock(System2.class);

  @RegisterExtension
  private final DbTester db = DbTester.create(system2);

  private final JiraOrganizationBindingPendingDao underTest = new JiraOrganizationBindingPendingDao(system2);

  @Test
  void insert_shouldInsertNewPending() {
    var now = 1000L;
    when(system2.now()).thenReturn(now);
    var dto = new JiraOrganizationBindingPendingDto()
      .setId("pending-1")
      .setSonarOrganizationUuid("org-uuid-1")
      .setJiraAccessToken("access-token")
      .setJiraAccessTokenExpiresAt(2000L)
      .setJiraRefreshToken("refresh-token")
      .setJiraRefreshTokenCreatedAt(1000L)
      .setJiraRefreshTokenUpdatedAt(1000L)
      .setUpdatedBy("user-uuid");

    var result = underTest.insert(db.getSession(), dto);

    assertThat(result.getId()).isEqualTo("pending-1");
    assertThat(result.getSonarOrganizationUuid()).isEqualTo("org-uuid-1");
    assertThat(result.getCreatedAt()).isEqualTo(now);
    assertThat(result.getUpdatedAt()).isEqualTo(now);
    assertThat(result.getJiraRefreshToken()).isEqualTo("refresh-token");
    assertThat(result.getJiraRefreshTokenCreatedAt()).isEqualTo(1000L);
    assertThat(result.getJiraRefreshTokenUpdatedAt()).isEqualTo(1000L);
    assertThat(result.getJiraAccessToken()).isEqualTo("access-token");
    assertThat(result.getJiraAccessTokenExpiresAt()).isEqualTo(2000L);
  }

  @Test
  void selectBySonarOrganizationUuid_shouldReturnEmpty_whenNotFound() {
    assertThat(underTest.selectBySonarOrganizationUuid(db.getSession(), "non-existent")).isEmpty();
  }

  @Test
  void selectBySonarOrganizationUuid_shouldReturnPending_whenExists() {
    when(system2.now()).thenReturn(1000L);
    var dto = new JiraOrganizationBindingPendingDto()
      .setId("pending-1")
      .setSonarOrganizationUuid("org-uuid-1")
      .setJiraAccessToken("access-token")
      .setJiraAccessTokenExpiresAt(2000L)
      .setJiraRefreshToken("refresh-token")
      .setJiraRefreshTokenCreatedAt(1000L)
      .setJiraRefreshTokenUpdatedAt(1000L)
      .setUpdatedBy("user-uuid");
    underTest.insert(db.getSession(), dto);

    var result = underTest.selectBySonarOrganizationUuid(db.getSession(), "org-uuid-1");

    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo("pending-1");
    assertThat(result.get().getSonarOrganizationUuid()).isEqualTo("org-uuid-1");
  }

  @Test
  void deleteBySonarOrganizationUuid_shouldDeletePending() {
    when(system2.now()).thenReturn(1000L);
    var dto = new JiraOrganizationBindingPendingDto()
      .setId("pending-1")
      .setSonarOrganizationUuid("org-uuid-1")
      .setJiraAccessToken("access-token")
      .setJiraAccessTokenExpiresAt(2000L)
      .setJiraRefreshToken("refresh-token")
      .setJiraRefreshTokenCreatedAt(1000L)
      .setJiraRefreshTokenUpdatedAt(1000L)
      .setUpdatedBy("user-uuid");
    underTest.insert(db.getSession(), dto);

    int deleted = underTest.deleteBySonarOrganizationUuid(db.getSession(), "org-uuid-1");

    assertThat(deleted).isEqualTo(1);
    assertThat(underTest.selectBySonarOrganizationUuid(db.getSession(), "org-uuid-1")).isEmpty();
  }

  @Test
  void deleteBySonarOrganizationUuid_shouldReturnZero_whenNotFound() {
    int deleted = underTest.deleteBySonarOrganizationUuid(db.getSession(), "non-existent");

    assertThat(deleted).isZero();
  }
}
