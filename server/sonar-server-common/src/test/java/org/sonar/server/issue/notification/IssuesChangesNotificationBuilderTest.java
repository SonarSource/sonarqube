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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.AnalysisChange;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.ChangedIssue;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Project;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Rule;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.User;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.UserChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(DataProviderRunner.class)
public class IssuesChangesNotificationBuilderTest {

  @Test
  public void IssuesChangesNotificationBuilder_getters() {
    Set<ChangedIssue> changedIssues = IntStream.range(0, 10).mapToObj(i -> new ChangedIssue.Builder("key" + i)
      .setRule(newRule("repository", "key", RuleType.CODE_SMELL, "name"))
      .setProject(new Project.Builder("uuid" + i).setKey("key").setProjectName("name").setBranchName("branch-name").build())
      .setNewStatus("status")
      .setNewResolution("resolution")
      .setAssignee(new User("uuid" + i, "login", "name"))
      .build())
      .collect(Collectors.toSet());

    AnalysisChange analysisChange = new AnalysisChange(1_000_000_000L);
    IssuesChangesNotificationBuilder builder = new IssuesChangesNotificationBuilder(changedIssues, analysisChange);
    assertThat(builder.getIssues()).isEqualTo(changedIssues);
    assertThat(builder.getChange()).isEqualTo(analysisChange);
  }

  @Test
  public void fail_if_changed_issues_empty() {
    AnalysisChange analysisChange = new AnalysisChange(1_000_000_000L);
    Set<ChangedIssue> issues = Collections.emptySet();
    assertThatThrownBy(() -> new IssuesChangesNotificationBuilder(issues, analysisChange))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("issues can't be empty");
  }

  @Test
  public void fail_if_change_is_null() {
    Set<ChangedIssue> changedIssues = IntStream.range(0, 10).mapToObj(i -> new ChangedIssue.Builder("key" + i)
      .setRule(newRule("repository", "key", RuleType.CODE_SMELL, "name"))
      .setProject(new Project.Builder("uuid" + i).setKey("key").setProjectName("name").setBranchName("branch-name").build())
      .setNewStatus("status")
      .setNewResolution("resolution")
      .setAssignee(new User("uuid" + i, "login", "name"))
      .build())
      .collect(Collectors.toSet());
    assertThatThrownBy(() -> new IssuesChangesNotificationBuilder(changedIssues, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("change can't be null");
  }

  @Test
  public void UserChange_toString() {
    long date = 1_000_000_000L;
    UserChange userChange = new UserChange(date, new User("user_uuid", "user_login", null));

    assertThat(userChange)
      .hasToString("UserChange{date=1000000000, user=User{uuid='user_uuid', login='user_login', name='null'}}");
  }

  @Test
  public void UserChange_equals() {
    long now = System2.INSTANCE.now();
    String uuid_1 = "uuid-1";
    String login_1 = "login-1";
    String name_1 = "name-1";
    UserChange userChange1 = new UserChange(now, new User(uuid_1, login_1, name_1));
    UserChange userChange2 = new UserChange(now, new User(uuid_1, login_1, name_1));

    assertThat(userChange1)
      .isEqualTo(userChange2)
      .isEqualTo(userChange1);
  }

  @DataProvider
  public static Object[][] userData() {
    return new Object[][] {
      {new UserChange(1L, new User("uuid-2", "login-1", "name-1"))},
      {new UserChange(1L, new User("uuid-1", "login-2", "name-1"))},
      {new UserChange(1L, new User("uuid-1", "login-1", "name-2"))},
      {new UserChange(1L, new User("uuid-2", "login-2", "name-1"))},
      {new UserChange(1L, new User("uuid-1", "login-2", "name-2"))},
      {new UserChange(1L, new User("uuid-2", "login-2", "name-2"))},
      {new UserChange(1L, new User("uuid-1", "login-2", null))},
      {new UserChange(1L, new User("uuid-2", "login-1", null))},
      {new UserChange(1L, new User("uuid-2", "login-2", null))},
      {null},
      {new Object()},
    };
  }

  @Test
  @UseDataProvider("userData")
  public void UserChange_not_equal(Object object) {
    long now = System2.INSTANCE.now();
    String uuid_1 = "uuid-1";
    String login_1 = "login-1";
    String name_1 = "name-1";
    UserChange userChange1 = new UserChange(now, new User(uuid_1, login_1, name_1));

    assertThat(userChange1).isNotEqualTo(object);
  }

  @Test
  public void UserChange_isAuthorLogin() {
    long now = System2.INSTANCE.now();
    String uuid = "uuid-1";
    String login = "login-1";
    String name = "name-1";
    UserChange userChange = new UserChange(now, new User(uuid, login, name));

    assertThat(userChange.isAuthorLogin("other-login")).isFalse();
    assertThat(userChange.isAuthorLogin("login-1")).isTrue();
  }

  @Test
  public void UserChange_getUser() {
    long now = System2.INSTANCE.now();
    String uuid = "uuid-1";
    String login = "login-1";
    String name = "name-1";
    UserChange userChange = new UserChange(now, new User(uuid, login, name));

    assertThat(userChange.getUser()).isNotNull();
    assertThat(userChange.getUser().getLogin()).isEqualTo(login);
    assertThat(userChange.getUser().getName()).hasValue(name);
    assertThat(userChange.getUser().getUuid()).isEqualTo(uuid);
  }

  @Test
  public void AnalysisChange_toString() {
    long date = 1_000_000_000L;
    AnalysisChange userChange = new AnalysisChange(date);

    assertThat(userChange).hasToString("AnalysisChange{1000000000}");
  }

  @Test
  public void AnalysisChange_equals() {
    AnalysisChange analysisChange1 = new AnalysisChange(1_000_000_000L);
    AnalysisChange analysisChange2 = new AnalysisChange(1_000_000_000L);

    assertThat(analysisChange1)
      .isEqualTo(analysisChange2)
      .isEqualTo(analysisChange1);
  }

  @Test
  public void AnalysisChange_not_equals() {
    AnalysisChange analysisChange1 = new AnalysisChange(1_000_000_000L);
    AnalysisChange analysisChange2 = new AnalysisChange(2_000_000_000L);

    assertThat(analysisChange1).isNotEqualTo(analysisChange2);
  }

  @Test
  public void AnalysisChange_not_equal_with_null() {
    AnalysisChange analysisChange1 = new AnalysisChange(1_000_000_000L);

    assertThat(analysisChange1).isNotNull();
  }

  @Test
  public void AnalysisChange_not_equal_with_Object() {
    AnalysisChange analysisChange1 = new AnalysisChange(1_000_000_000L);

    assertThat(analysisChange1).isNotEqualTo(new Object());
  }

  @Test
  public void AnalysisChange_isAuthorLogin() {
    AnalysisChange analysisChange1 = new AnalysisChange(1_000_000_000L);
    assertThat(analysisChange1.isAuthorLogin("login")).isFalse();
  }

  @Test
  public void Project_toString() {
    Project project = new Project.Builder("uuid")
      .setKey("key")
      .setProjectName("name")
      .setBranchName("branch-name")
      .build();

    assertThat(project)
      .hasToString("Project{uuid='uuid', key='key', projectName='name', branchName='branch-name'}");
  }

  @Test
  public void Project_equals() {
    Project project1 = new Project.Builder("uuid")
      .setKey("key")
      .setProjectName("name")
      .setBranchName("branch-name")
      .build();

    Project project2 = new Project.Builder("uuid")
      .setKey("key")
      .setProjectName("name")
      .setBranchName("branch-name")
      .build();

    assertThat(project1)
      .isEqualTo(project2)
      .isEqualTo(project1);
  }

  @DataProvider
  public static Object[][] projectData() {
    return new Object[][] {
      {new Project.Builder("uuid2").setKey("key1").setProjectName("name1").setBranchName("branch-name1").build()},
      {new Project.Builder("uuid1").setKey("key2").setProjectName("name1").setBranchName("branch-name1").build()},
      {new Project.Builder("uuid1").setKey("key1").setProjectName("name2").setBranchName("branch-name1").build()},
      {new Project.Builder("uuid1").setKey("key1").setProjectName("name1").setBranchName("branch-name2").build()},
      {new Project.Builder("uuid2").setKey("key2").setProjectName("name1").setBranchName("branch-name1").build()},
      {new Project.Builder("uuid1").setKey("key2").setProjectName("name2").setBranchName("branch-name1").build()},
      {new Project.Builder("uuid1").setKey("key1").setProjectName("name2").setBranchName("branch-name2").build()},
      {new Project.Builder("uuid2").setKey("key2").setProjectName("name2").setBranchName("branch-name1").build()},
      {new Project.Builder("uuid2").setKey("key2").setProjectName("name2").setBranchName("branch-name2").build()},
      {null},
      {new Object()},
    };
  }

  @Test
  @UseDataProvider("projectData")
  public void Project_not_equal(Object object) {
    Project project1 = new Project.Builder("uuid1")
      .setKey("key1")
      .setProjectName("name1")
      .setBranchName("branch-name1")
      .build();

    assertThat(project1).isNotEqualTo(object);
  }

  @Test
  public void Project_getters() {
    Project project1 = new Project.Builder("uuid")
      .setKey("key")
      .setProjectName("name")
      .setBranchName("branch-name")
      .build();

    assertThat(project1.getKey()).isEqualTo("key");
    assertThat(project1.getProjectName()).isEqualTo("name");
    assertThat(project1.getUuid()).isEqualTo("uuid");
    assertThat(project1.getBranchName()).hasValue("branch-name");
  }

  @Test
  public void Rule_toString() {
    Rule rule = newRule("repository", "key", RuleType.CODE_SMELL, "name");

    assertThat(rule)
      .hasToString("Rule{key=repository:key, type=CODE_SMELL, name='name'}");
  }

  @Test
  public void Rule_equals() {
    Rule rule1 = newRule("repository", "key", RuleType.CODE_SMELL, "name");
    Rule rule2 = newRule("repository", "key", RuleType.CODE_SMELL, "name");

    assertThat(rule1)
      .isEqualTo(rule2)
      .isEqualTo(rule1);
  }

  @DataProvider
  public static Object[][] ruleData() {
    return new Object[][] {
      {newRule("repository2", "key1", RuleType.CODE_SMELL, "name1")},
      {newRule("repository1", "key2", RuleType.CODE_SMELL, "name1")},
      {newRule("repository1", "key1", RuleType.BUG, "name1")},
      {newRule("repository1", "key1", RuleType.CODE_SMELL, "name2")},
      {newRule("repository2", "key2", RuleType.CODE_SMELL, "name1")},
      {newRule("repository1", "key2", RuleType.BUG, "name1")},
      {newRule("repository1", "key1", RuleType.BUG, "name2")},
      {newRule("repository2", "key2", RuleType.BUG, "name2")},
      {newRule("repository1", "key1", null, "name1")},
      {null},
      {new Object()},
    };
  }

  @Test
  @UseDataProvider("ruleData")
  public void Rule_not_equal(Object object) {
    Rule rule = newRule("repository1", "key1", RuleType.CODE_SMELL, "name1");

    assertThat(rule.equals(object)).isFalse();
  }

  @Test
  public void Rule_getters() {
    Rule rule = newRule("repository", "key", RuleType.CODE_SMELL, "name");

    assertThat(rule.getKey()).isEqualTo(RuleKey.of("repository", "key"));
    assertThat(rule.getName()).isEqualTo("name");
    assertThat(rule.getRuleType()).isEqualTo(RuleType.CODE_SMELL);
  }

  @Test
  public void ChangedIssue_toString() {
    ChangedIssue changedIssue = new ChangedIssue.Builder("key")
      .setRule(newRule("repository", "key", RuleType.CODE_SMELL, "name"))
      .setProject(new Project.Builder("uuid").setKey("key").setProjectName("name").setBranchName("branch-name").build())
      .setNewStatus("status")
      .setNewResolution("resolution")
      .setAssignee(new User("uuid", "login", "name"))
      .build();

    assertThat(changedIssue)
      .hasToString("ChangedIssue{key='key', newStatus='status', newResolution='resolution', " +
        "assignee=User{uuid='uuid', login='login', name='name'}, " +
        "rule=Rule{key=repository:key, type=CODE_SMELL, name='name'}, " +
        "project=Project{uuid='uuid', key='key', projectName='name', branchName='branch-name'}}");
  }

  @Test
  public void ChangedIssue_equals() {
    ChangedIssue changedIssue1 = new ChangedIssue.Builder("key")
      .setRule(newRule("repository", "key", RuleType.CODE_SMELL, "name"))
      .setProject(new Project.Builder("uuid").setKey("key").setProjectName("name").setBranchName("branch-name").build())
      .setNewStatus("status")
      .setNewResolution("resolution")
      .setAssignee(new User("uuid", "login", "name"))
      .build();
    ChangedIssue changedIssue2 = new ChangedIssue.Builder("key")
      .setRule(newRule("repository", "key", RuleType.CODE_SMELL, "name"))
      .setProject(new Project.Builder("uuid").setKey("key").setProjectName("name").setBranchName("branch-name").build())
      .setNewStatus("status")
      .setNewResolution("resolution")
      .setAssignee(new User("uuid", "login", "name"))
      .build();

    assertThat(changedIssue1)
      .isEqualTo(changedIssue2)
      .isEqualTo(changedIssue1);
  }

  @DataProvider
  public static Object[][] changedIssueData() {
    return new Object[][] {
      {new ChangedIssue.Builder("key1")
        .setRule(newRule("repository", "key", RuleType.CODE_SMELL, "name"))
        .setProject(new Project.Builder("uuid").setKey("key").setProjectName("name").setBranchName("branch-name").build())
        .setNewStatus("status")
        .setNewResolution("resolution")
        .setAssignee(new User("uuid", "login", "name"))
        .build()},
      {new ChangedIssue.Builder("key")
        .setRule(newRule("repository1", "key", RuleType.CODE_SMELL, "name"))
        .setProject(new Project.Builder("uuid").setKey("key").setProjectName("name").setBranchName("branch-name").build())
        .setNewStatus("status")
        .setNewResolution("resolution")
        .setAssignee(new User("uuid", "login", "name"))
        .build()},
      {new ChangedIssue.Builder("key")
        .setRule(newRule("repository", "key", RuleType.CODE_SMELL, "name"))
        .setProject(new Project.Builder("uuid1").setKey("key").setProjectName("name").setBranchName("branch-name").build())
        .setNewStatus("status")
        .setNewResolution("resolution")
        .setAssignee(new User("uuid", "login", "name"))
        .build()},
      {new ChangedIssue.Builder("key")
        .setRule(newRule("repository", "key", RuleType.CODE_SMELL, "name"))
        .setProject(new Project.Builder("uuid").setKey("key").setProjectName("name").setBranchName("branch-name").build())
        .setNewStatus("status1")
        .setNewResolution("resolution")
        .setAssignee(new User("uuid", "login", "name"))
        .build()},
      {new ChangedIssue.Builder("key")
        .setRule(newRule("repository", "key", RuleType.CODE_SMELL, "name"))
        .setProject(new Project.Builder("uuid").setKey("key").setProjectName("name").setBranchName("branch-name").build())
        .setNewStatus("status")
        .setNewResolution("resolution1")
        .setAssignee(new User("uuid", "login", "name"))
        .build()},
      {new ChangedIssue.Builder("key")
        .setRule(newRule("repository", "key", RuleType.CODE_SMELL, "name"))
        .setProject(new Project.Builder("uuid").setKey("key").setProjectName("name").setBranchName("branch-name").build())
        .setNewStatus("status")
        .setNewResolution("resolution")
        .setAssignee(new User("uuid1", "login", "name"))
        .build()},
      {null},
      {new Object()},
    };
  }

  @Test
  @UseDataProvider("changedIssueData")
  public void ChangedIssue_not_equal(Object object) {
    ChangedIssue changedIssue = new ChangedIssue.Builder("key")
      .setRule(newRule("repository", "key", RuleType.CODE_SMELL, "name"))
      .setProject(new Project.Builder("uuid").setKey("key").setProjectName("name").setBranchName("branch-name").build())
      .setNewStatus("status")
      .setNewResolution("resolution")
      .setAssignee(new User("uuid", "login", "name"))
      .build();

    assertThat(changedIssue).isNotEqualTo(object);
  }

  @Test
  public void ChangedIssue_getters() {
    Project project = new Project.Builder("uuid").setKey("key").setProjectName("name").setBranchName("branch-name").build();
    Rule rule = newRule("repository", "key", RuleType.CODE_SMELL, "name");
    User user = new User("uuid", "login", "name");
    ChangedIssue changedIssue = new ChangedIssue.Builder("key")
      .setRule(rule)
      .setProject(project)
      .setNewStatus("status")
      .setNewResolution("resolution")
      .setAssignee(user)
      .build();

    assertThat(changedIssue.getKey()).isEqualTo("key");
    assertThat(changedIssue.getNewStatus()).isEqualTo("status");
    assertThat(changedIssue.getAssignee()).hasValue(user);
    assertThat(changedIssue.getNewResolution()).hasValue("resolution");
    assertThat(changedIssue.getProject()).isEqualTo(project);
    assertThat(changedIssue.getRule()).isEqualTo(rule);
  }

  private static Rule newRule(String repository, String key, RuleType type, String name) {
    return new Rule(RuleKey.of(repository, key), type, name);
  }

}
