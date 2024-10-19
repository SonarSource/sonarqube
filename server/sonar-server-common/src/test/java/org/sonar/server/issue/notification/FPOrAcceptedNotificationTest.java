/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.AnalysisChange;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.ChangedIssue;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Project;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Rule;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.issue.notification.FPOrAcceptedNotification.FpPrAccepted.FP;
import static org.sonar.server.issue.notification.FPOrAcceptedNotification.FpPrAccepted.ACCEPTED;
import static org.sonar.server.issue.notification.IssuesChangesNotificationBuilderTesting.newRandomNotAHotspotRule;

public class FPOrAcceptedNotificationTest {
  @Test
  public void equals_is_based_on_issues_change_and_resolution() {
    Rule rule = newRandomNotAHotspotRule("rule_name");
    Project project = new Project.Builder("prj_uuid").setKey("prj_key").setProjectName("prj_name").build();
    Set<ChangedIssue> changedIssues = IntStream.range(0, 2 + new Random().nextInt(5))
      .mapToObj(i -> new ChangedIssue.Builder("key_" + i)
        .setNewStatus("status")
        .setRule(rule)
        .setProject(project)
        .build())
      .collect(Collectors.toSet());
    AnalysisChange change = new AnalysisChange(12);
    User user = new User("uuid", "login", null);
    FPOrAcceptedNotification underTest = new FPOrAcceptedNotification(change, changedIssues, ACCEPTED);

    assertThat(underTest)
      .isEqualTo(new FPOrAcceptedNotification(change, changedIssues, ACCEPTED))
      .isEqualTo(new FPOrAcceptedNotification(change, ImmutableSet.copyOf(changedIssues), ACCEPTED))
      .isNotEqualTo(new Object())
      .isNotNull()
      .isNotEqualTo(new FPOrAcceptedNotification(change, Collections.emptySet(), ACCEPTED))
      .isNotEqualTo(new FPOrAcceptedNotification(change, ImmutableSet.of(changedIssues.iterator().next()), ACCEPTED))
      .isNotEqualTo(new FPOrAcceptedNotification(new AnalysisChange(14), changedIssues, ACCEPTED))
      .isNotEqualTo(new FPOrAcceptedNotification(new IssuesChangesNotificationBuilder.UserChange(12, user), changedIssues, ACCEPTED))
      .isNotEqualTo(new FPOrAcceptedNotification(change, changedIssues, FP));
  }

  @Test
  public void hashcode_is_based_on_issues_change_and_resolution() {
    Rule rule = newRandomNotAHotspotRule("rule_name");
    Project project = new Project.Builder("prj_uuid").setKey("prj_key").setProjectName("prj_name").build();
    Set<ChangedIssue> changedIssues = IntStream.range(0, 2 + new Random().nextInt(5))
      .mapToObj(i -> new ChangedIssue.Builder("key_" + i)
        .setNewStatus("status")
        .setRule(rule)
        .setProject(project)
        .build())
      .collect(Collectors.toSet());
    AnalysisChange change = new AnalysisChange(12);
    User user = new User("uuid", "login", null);
    FPOrAcceptedNotification underTest = new FPOrAcceptedNotification(change, changedIssues, ACCEPTED);

    assertThat(underTest.hashCode())
      .isEqualTo(new FPOrAcceptedNotification(change, changedIssues, ACCEPTED).hashCode())
      .isEqualTo(new FPOrAcceptedNotification(change, ImmutableSet.copyOf(changedIssues), ACCEPTED).hashCode())
      .isNotEqualTo(new Object().hashCode())
      .isNotEqualTo(new FPOrAcceptedNotification(change, Collections.emptySet(), ACCEPTED).hashCode())
      .isNotEqualTo(new FPOrAcceptedNotification(change, ImmutableSet.of(changedIssues.iterator().next()), ACCEPTED).hashCode())
      .isNotEqualTo(new FPOrAcceptedNotification(new AnalysisChange(14), changedIssues, ACCEPTED).hashCode())
      .isNotEqualTo(new FPOrAcceptedNotification(new IssuesChangesNotificationBuilder.UserChange(12, user), changedIssues, ACCEPTED).hashCode())
      .isNotEqualTo(new FPOrAcceptedNotification(change, changedIssues, FP)).hashCode();
  }
}
