/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.issue.notification;

import org.junit.Test;
import org.sonar.api.utils.Durations;
import org.sonar.db.DbClient;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.server.issue.notification.AbstractNewIssuesEmailTemplate.FIELD_ASSIGNEE;

public class MyNewIssuesNotificationTest {

  private MyNewIssuesNotification underTest = new MyNewIssuesNotification(mock(DbClient.class), mock(Durations.class));

  @Test
  public void set_assignee() {
    UserDto user = UserTesting.newUserDto();

    underTest.setAssignee(user);

    assertThat(underTest.getFieldValue(FIELD_ASSIGNEE))
      .isEqualTo(underTest.getAssignee())
      .isEqualTo(user.getLogin());
  }

  @Test
  public void set_with_a_specific_type() {
    assertThat(underTest.getType()).isEqualTo(MyNewIssuesNotification.MY_NEW_ISSUES_NOTIF_TYPE);
  }

  @Test
  public void getProjectKey_returns_null_if_setProject_has_no_been_called() {
    assertThat(underTest.getProjectKey()).isNull();
  }

  @Test
  public void getProjectKey_returns_projectKey_if_setProject_has_been_called() {
    String projectKey = randomAlphabetic(5);
    String projectName = randomAlphabetic(6);
    String branchName = randomAlphabetic(7);
    String pullRequest = randomAlphabetic(8);
    underTest.setProject(projectKey, projectName, branchName, pullRequest);

    assertThat(underTest.getProjectKey()).isEqualTo(projectKey);
  }
}
