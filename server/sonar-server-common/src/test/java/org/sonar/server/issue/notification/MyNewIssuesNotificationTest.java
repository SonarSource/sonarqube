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
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;
import org.sonar.server.issue.notification.NewIssuesNotification.DetailsSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.server.issue.notification.AbstractNewIssuesEmailTemplate.FIELD_ASSIGNEE;

public class MyNewIssuesNotificationTest {

  private MyNewIssuesNotification underTest = new MyNewIssuesNotification(mock(Durations.class), mock(DetailsSupplier.class));

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

}
