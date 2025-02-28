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
package org.sonar.server.issue.notification;

import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.rule.RuleType;
import org.sonar.api.utils.System2;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.issue.notification.IssuesChangesNotificationBuilderTesting.newProject;

public class IssuesChangesNotificationSerializerTest {

  private IssuesChangesNotificationSerializer underTest = new IssuesChangesNotificationSerializer();

  @Test
  public void should_serialize_issues_from_rules_with_analysis_change() {
    long now = System2.INSTANCE.now();
    IssuesChangesNotificationBuilder.AnalysisChange change = new IssuesChangesNotificationBuilder.AnalysisChange(now);
    Set<IssuesChangesNotificationBuilder.ChangedIssue> issues = IntStream.range(1, 4)
      .mapToObj(i -> new IssuesChangesNotificationBuilder.ChangedIssue.Builder("issue_key_" + i)
        .setNewStatus("foo")
        .setAssignee(null)
        .setRule(newRule("repository", "key" + i, RuleType.fromDbConstant(i), "name" + i))
        .setProject(newProject(i + ""))
        .build())
      .collect(toSet());
    IssuesChangesNotificationBuilder builder = new IssuesChangesNotificationBuilder(issues, change);

    IssuesChangesNotification serialized = underTest.serialize(builder);
    IssuesChangesNotificationBuilder deserialized = underTest.from(serialized);

    assertThat(deserialized.getChange()).isEqualTo(builder.getChange());
    assertThat(deserialized.getIssues()).isEqualTo(builder.getIssues());
  }

  @Test
  public void should_serialize_issues_from_rules() {
    Set<IssuesChangesNotificationBuilder.ChangedIssue> issues = IntStream.range(1, 4)
      .mapToObj(i -> new IssuesChangesNotificationBuilder.ChangedIssue.Builder("issue_key_" + i)
        .setNewStatus("foo")
        .setAssignee(null)
        .setRule(newRule("repository", "key" + i, RuleType.fromDbConstant(i), "name" + i))
        .setProject(newProject(i + ""))
        .build())
      .collect(toSet());
    IssuesChangesNotificationBuilder builder = new IssuesChangesNotificationBuilder(issues,
      new IssuesChangesNotificationBuilder.UserChange(new Random().nextLong(), new IssuesChangesNotificationBuilder.User("user_uuid", "user_login", null)));

    IssuesChangesNotification serialized = underTest.serialize(builder);
    IssuesChangesNotificationBuilder deserialized = underTest.from(serialized);

    assertThat(deserialized.getChange()).isEqualTo(builder.getChange());
    assertThat(deserialized.getIssues()).isEqualTo(builder.getIssues());
  }

  @Test
  public void should_serialize_issues_from_different_external_rules() {
    Set<IssuesChangesNotificationBuilder.ChangedIssue> issues = IntStream.range(0, 3)
      .mapToObj(i -> new IssuesChangesNotificationBuilder.ChangedIssue.Builder("issue_key_" + i)
        .setNewStatus("foo")
        .setAssignee(null)
        .setRule(newRule("repository", "key" + i, null, "name"))
        .setProject(newProject(i + ""))
        .build())
      .collect(toSet());
    IssuesChangesNotificationBuilder builder = new IssuesChangesNotificationBuilder(issues,
      new IssuesChangesNotificationBuilder.UserChange(new Random().nextLong(), new IssuesChangesNotificationBuilder.User("user_uuid", "user_login", null)));

    IssuesChangesNotification serialized = underTest.serialize(builder);
    IssuesChangesNotificationBuilder deserialized = underTest.from(serialized);

    assertThat(deserialized.getChange()).isEqualTo(builder.getChange());
    assertThat(deserialized.getIssues()).isEqualTo(builder.getIssues());
  }

  @Test
  public void should_serialize_issues_from_same_external_rules() {
    Set<IssuesChangesNotificationBuilder.ChangedIssue> issues = IntStream.range(0, 3)
      .mapToObj(i -> new IssuesChangesNotificationBuilder.ChangedIssue.Builder("issue_key_" + i)
        .setNewStatus("foo")
        .setAssignee(null)
        .setRule(newRule("repository", "key", null, "name"))
        .setProject(newProject(i + ""))
        .build())
      .collect(toSet());
    IssuesChangesNotificationBuilder builder = new IssuesChangesNotificationBuilder(issues,
      new IssuesChangesNotificationBuilder.UserChange(new Random().nextLong(), new IssuesChangesNotificationBuilder.User("user_uuid", "user_login", null)));

    IssuesChangesNotification serialized = underTest.serialize(builder);
    IssuesChangesNotificationBuilder deserialized = underTest.from(serialized);

    assertThat(deserialized.getChange()).isEqualTo(builder.getChange());
    assertThat(deserialized.getIssues()).isEqualTo(builder.getIssues());
  }

  @Test
  public void should_serialize_hotspot() {
    Set<IssuesChangesNotificationBuilder.ChangedIssue> issues = IntStream.range(0, 3)
      .mapToObj(i -> new IssuesChangesNotificationBuilder.ChangedIssue.Builder("issue_key_" + i)
        .setNewStatus("foo")
        .setAssignee(null)
        .setRule(newRule("repository", "key", RuleType.SECURITY_HOTSPOT, "name"))
        .setProject(newProject(i + ""))
        .build())
      .collect(toSet());
    IssuesChangesNotificationBuilder builder = new IssuesChangesNotificationBuilder(issues,
      new IssuesChangesNotificationBuilder.UserChange(new Random().nextLong(), new IssuesChangesNotificationBuilder.User("user_uuid", "user_login", null)));

    IssuesChangesNotification serialized = underTest.serialize(builder);
    IssuesChangesNotificationBuilder deserialized = underTest.from(serialized);

    assertThat(deserialized.getChange()).isEqualTo(builder.getChange());
    assertThat(deserialized.getIssues()).isEqualTo(builder.getIssues());
  }

  private static IssuesChangesNotificationBuilder.Rule newRule(String repository, String key, RuleType type, String name) {
    return new IssuesChangesNotificationBuilder.Rule(RuleKey.of(repository, key), type, name);
  }
}
