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

import java.util.Random;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.AnalysisChange;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.ChangedIssue;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Project;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Rule;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.User;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.UserChange;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;

public class IssuesChangesNotificationBuilderTesting {

  public static Rule ruleOf(RuleDto rule) {
    return new Rule(rule.getKey(), rule.getName());
  }

  public static Rule ruleOf(RuleDefinitionDto rule) {
    return new Rule(rule.getKey(), rule.getName());
  }

  public static User userOf(UserDto changeAuthor) {
    return new User(changeAuthor.getUuid(), changeAuthor.getLogin(), changeAuthor.getName());
  }

  public static Project projectBranchOf(DbTester db, ComponentDto branch) {
    BranchDto branchDto = db.getDbClient().branchDao().selectByUuid(db.getSession(), branch.uuid()).get();
    checkArgument(!branchDto.isMain(), "should be a branch");
    return new Project.Builder(branch.uuid())
      .setKey(branch.getKey())
      .setProjectName(branch.name())
      .setBranchName(branchDto.getKey())
      .build();
  }

  public static Project projectOf(ComponentDto project) {
    return new Project.Builder(project.uuid())
      .setKey(project.getKey())
      .setProjectName(project.name())
      .build();
  }

  static ChangedIssue newChangedIssue(String key, Project project, Rule rule) {
    return new ChangedIssue.Builder(key)
      .setNewStatus(randomAlphabetic(19))
      .setProject(project)
      .setRule(rule)
      .build();
  }

  static ChangedIssue newChangedIssue(String key, String status, Project project, String ruleName) {
    return newChangedIssue(key, status, project, newRule(ruleName));
  }

  static ChangedIssue newChangedIssue(String key, String status, Project project, Rule rule) {
    return new ChangedIssue.Builder(key)
      .setNewStatus(status)
      .setProject(project)
      .setRule(rule)
      .build();
  }

  static Rule newRule(String ruleName) {
    return new Rule(RuleKey.of(randomAlphabetic(6), randomAlphabetic(7)), ruleName);
  }

  static Project newProject(String uuid) {
    return new Project.Builder(uuid).setProjectName(uuid + "_name").setKey(uuid + "_key").build();
  }

  static Project newBranch(String uuid, String branchName) {
    return new Project.Builder(uuid).setProjectName(uuid + "_name").setKey(uuid + "_key").setBranchName(branchName).build();
  }

  static UserChange newUserChange() {
    return new UserChange(new Random().nextLong(), new User(randomAlphabetic(4), randomAlphabetic(5), randomAlphabetic(6)));
  }

  static AnalysisChange newAnalysisChange() {
    return new AnalysisChange(new Random().nextLong());
  }
}
