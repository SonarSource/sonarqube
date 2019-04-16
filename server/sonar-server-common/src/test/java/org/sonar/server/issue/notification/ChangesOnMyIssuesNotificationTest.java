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

import com.google.common.collect.ImmutableSet;
import java.util.Random;
import org.junit.Test;
import org.sonar.api.notifications.Notification;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.AnalysisChange;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.ChangedIssue;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.User;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.UserChange;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ChangesOnMyIssuesNotificationTest {
  @Test
  public void key_is_ChangesOnMyIssues() {
    ChangesOnMyIssuesNotification underTest = new ChangesOnMyIssuesNotification(
      new UserChange(new Random().nextLong(), new User(randomAlphabetic(2), randomAlphabetic(3), randomAlphabetic(4))),
      ImmutableSet.of());

    assertThat(underTest.getType()).isEqualTo("ChangesOnMyIssues");
  }

  @Test
  public void equals_is_based_on_change_and_issues() {
    AnalysisChange analysisChange = new AnalysisChange(new Random().nextLong());
    ChangedIssue changedIssue = IssuesChangesNotificationBuilderTesting.newChangedIssue("doo", IssuesChangesNotificationBuilderTesting.newProject("prj"), IssuesChangesNotificationBuilderTesting.newRule("rul"));
    ChangesOnMyIssuesNotification underTest = new ChangesOnMyIssuesNotification(analysisChange, ImmutableSet.of(changedIssue));

    assertThat(underTest)
      .isEqualTo(new ChangesOnMyIssuesNotification(analysisChange, ImmutableSet.of(changedIssue)))
      .isNotEqualTo(mock(Notification.class))
      .isNotEqualTo(null)
      .isNotEqualTo(new ChangesOnMyIssuesNotification(new AnalysisChange(analysisChange.getDate() + 10), ImmutableSet.of(changedIssue)))
      .isNotEqualTo(new ChangesOnMyIssuesNotification(analysisChange, ImmutableSet.of()));
  }

  @Test
  public void hashcode_is_based_on_change_and_issues() {
    AnalysisChange analysisChange = new AnalysisChange(new Random().nextLong());
    ChangedIssue changedIssue = IssuesChangesNotificationBuilderTesting.newChangedIssue("doo", IssuesChangesNotificationBuilderTesting.newProject("prj"), IssuesChangesNotificationBuilderTesting.newRule("rul"));
    ChangesOnMyIssuesNotification underTest = new ChangesOnMyIssuesNotification(analysisChange, ImmutableSet.of(changedIssue));

    assertThat(underTest.hashCode())
      .isEqualTo(new ChangesOnMyIssuesNotification(analysisChange, ImmutableSet.of(changedIssue)).hashCode())
      .isNotEqualTo(mock(Notification.class).hashCode())
      .isNotEqualTo(null)
      .isNotEqualTo(new ChangesOnMyIssuesNotification(new AnalysisChange(analysisChange.getDate() + 10), ImmutableSet.of(changedIssue)).hashCode())
      .isNotEqualTo(new ChangesOnMyIssuesNotification(analysisChange, ImmutableSet.of())).hashCode();
  }

}
