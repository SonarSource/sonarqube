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

import java.util.Date;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.issue.notification.NewIssuesNotification.DetailsSupplier;
import org.sonar.server.issue.notification.NewIssuesNotification.RuleDefinition;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.ASSIGNEE;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.COMPONENT;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.ISSUE;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.RULE;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.TAG;

public class NewIssuesNotificationIT {

  @Rule
  public DbTester db = DbTester.create();

  private DetailsSupplier detailsSupplier = mock(DetailsSupplier.class);
  private NewIssuesNotification underTest = new NewIssuesNotification(detailsSupplier);

  @Test
  public void set_project_without_branch() {
    underTest.setProject("project-key", "project-long-name", null, null);

    assertThat(underTest.getFieldValue(NewIssuesEmailTemplate.FIELD_PROJECT_NAME)).isEqualTo("project-long-name");
    assertThat(underTest.getFieldValue(NewIssuesEmailTemplate.FIELD_PROJECT_KEY)).isEqualTo("project-key");
    assertThat(underTest.getFieldValue(NewIssuesEmailTemplate.FIELD_BRANCH)).isNull();
  }

  @Test
  public void set_project_with_branch() {
    underTest.setProject("project-key", "project-long-name", "feature", null);

    assertThat(underTest.getFieldValue(NewIssuesEmailTemplate.FIELD_PROJECT_NAME)).isEqualTo("project-long-name");
    assertThat(underTest.getFieldValue(NewIssuesEmailTemplate.FIELD_PROJECT_KEY)).isEqualTo("project-key");
    assertThat(underTest.getFieldValue(NewIssuesEmailTemplate.FIELD_BRANCH)).isEqualTo("feature");
  }

  @Test
  public void set_project_with_pull_request() {
    underTest.setProject("project-key", "project-long-name", null, "pr-123");

    assertThat(underTest.getFieldValue(NewIssuesEmailTemplate.FIELD_PROJECT_NAME)).isEqualTo("project-long-name");
    assertThat(underTest.getFieldValue(NewIssuesEmailTemplate.FIELD_PROJECT_KEY)).isEqualTo("project-key");
    assertThat(underTest.getFieldValue(NewIssuesEmailTemplate.FIELD_PULL_REQUEST)).isEqualTo("pr-123");
  }

  @Test
  public void set_project_version() {
    String version = randomAlphanumeric(5);

    underTest.setProjectVersion(version);

    assertThat(underTest.getFieldValue(NewIssuesEmailTemplate.FIELD_PROJECT_VERSION)).isEqualTo(version);
  }

  @Test
  public void set_project_version_supports_null() {
    underTest.setProjectVersion(null);

    assertThat(underTest.getFieldValue(NewIssuesEmailTemplate.FIELD_PROJECT_VERSION)).isNull();
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

  @Test
  public void getProjectKey_returns_value_of_field_projectKey() {
    String projectKey = randomAlphabetic(5);
    underTest.setFieldValue("projectKey", projectKey);

    assertThat(underTest.getProjectKey()).isEqualTo(projectKey);
  }

  @Test
  public void set_date() {
    Date date = new Date();

    underTest.setAnalysisDate(date);

    assertThat(underTest.getFieldValue(NewIssuesEmailTemplate.FIELD_PROJECT_DATE)).isEqualTo(DateUtils.formatDateTime(date));
  }

  @Test
  public void set_statistics() {
    UserDto maynard = db.users().insertUser(u -> u.setLogin("maynard"));
    UserDto keenan = db.users().insertUser(u -> u.setLogin("keenan"));
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "path"));
    ComponentDto file = db.components().insertComponent(newFileDto(directory));
    RuleDto rule1 = db.rules().insert(r -> r.setRepositoryKey("SonarQube").setRuleKey("rule1-the-world").setName("Rule the World").setLanguage("Java"));
    RuleDto rule2 = db.rules().insert(r -> r.setRepositoryKey("SonarQube").setRuleKey("rule1-the-universe").setName("Rule the Universe").setLanguage("Clojure"));
    IssueDto issue1 = db.issues().insert(rule1, project, file, i -> i.setType(BUG).setAssigneeUuid(maynard.getUuid()).setTags(asList("bug", "owasp")));
    IssueDto issue2 = db.issues().insert(rule2, project, directory, i -> i.setType(CODE_SMELL).setAssigneeUuid(keenan.getUuid()).setTags(singletonList("owasp")));

    NewIssuesStatistics.Stats stats = new NewIssuesStatistics.Stats(i -> true);
    IntStream.rangeClosed(1, 5).forEach(i -> stats.add(issue1.toDefaultIssue()));
    IntStream.rangeClosed(1, 3).forEach(i -> stats.add(issue2.toDefaultIssue()));
    mockDetailsSupplierComponents(project, directory, file);
    mockDetailsSupplierRules(rule1, rule2);
    mockDetailsSupplierAssignees(maynard, keenan);

    underTest.setStatistics(project.longName(), stats);

    assertThat(underTest.getFieldValue(ISSUE + ".count")).isEqualTo("8");
    assertThat(underTest.getFieldValue(ASSIGNEE + ".1.label")).isEqualTo(maynard.getName());
    assertThat(underTest.getFieldValue(ASSIGNEE + ".1.count")).isEqualTo("5");
    assertThat(underTest.getFieldValue(ASSIGNEE + ".2.label")).isEqualTo(keenan.getName());
    assertThat(underTest.getFieldValue(ASSIGNEE + ".2.count")).isEqualTo("3");
    assertThat(underTest.getFieldValue(TAG + ".1.label")).isEqualTo("owasp");
    assertThat(underTest.getFieldValue(TAG + ".1.count")).isEqualTo("8");
    assertThat(underTest.getFieldValue(TAG + ".2.label")).isEqualTo("bug");
    assertThat(underTest.getFieldValue(TAG + ".2.count")).isEqualTo("5");
    assertThat(underTest.getFieldValue(COMPONENT + ".1.label")).isEqualTo(file.name());
    assertThat(underTest.getFieldValue(COMPONENT + ".1.count")).isEqualTo("5");
    assertThat(underTest.getFieldValue(COMPONENT + ".2.label")).isEqualTo(directory.name());
    assertThat(underTest.getFieldValue(COMPONENT + ".2.count")).isEqualTo("3");
    assertThat(underTest.getFieldValue(RULE + ".1.label")).isEqualTo("Rule the World (Java)");
    assertThat(underTest.getFieldValue(RULE + ".1.count")).isEqualTo("5");
    assertThat(underTest.getFieldValue(RULE + ".2.label")).isEqualTo("Rule the Universe (Clojure)");
    assertThat(underTest.getFieldValue(RULE + ".2.count")).isEqualTo("3");
    assertThat(underTest.getDefaultMessage()).startsWith("8 new issues on " + project.longName());
  }

  @Test
  public void set_statistics_when_no_issues_created_on_current_analysis() {
    UserDto maynard = db.users().insertUser(u -> u.setLogin("maynard"));
    UserDto keenan = db.users().insertUser(u -> u.setLogin("keenan"));
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "path"));
    ComponentDto file = db.components().insertComponent(newFileDto(directory));
    RuleDto rule1 = db.rules().insert(r -> r.setRepositoryKey("SonarQube").setRuleKey("rule1-the-world").setName("Rule the World").setLanguage("Java"));
    RuleDto rule2 = db.rules().insert(r -> r.setRepositoryKey("SonarQube").setRuleKey("rule1-the-universe").setName("Rule the Universe").setLanguage("Clojure"));
    IssueDto issue1 = db.issues().insert(rule1, project, file, i -> i.setType(BUG).setAssigneeUuid(maynard.getUuid()).setTags(asList("bug", "owasp")));
    IssueDto issue2 = db.issues().insert(rule2, project, directory, i -> i.setType(CODE_SMELL).setAssigneeUuid(keenan.getUuid()).setTags(singletonList("owasp")));

    NewIssuesStatistics.Stats stats = new NewIssuesStatistics.Stats(i -> false);
    IntStream.rangeClosed(1, 5).forEach(i -> stats.add(issue1.toDefaultIssue()));
    IntStream.rangeClosed(1, 3).forEach(i -> stats.add(issue2.toDefaultIssue()));
    mockDetailsSupplierComponents(project, directory, file);
    mockDetailsSupplierRules(rule1, rule2);
    mockDetailsSupplierAssignees(maynard, keenan);

    underTest.setStatistics(project.longName(), stats);

    assertThat(underTest.getFieldValue(ISSUE + ".count")).isEqualTo("0");
    assertThat(underTest.getFieldValue(ASSIGNEE + ".1.label")).isNull();
    assertThat(underTest.getFieldValue(ASSIGNEE + ".1.count")).isNull();
    assertThat(underTest.getFieldValue(ASSIGNEE + ".2.label")).isNull();
    assertThat(underTest.getFieldValue(ASSIGNEE + ".2.count")).isNull();
    assertThat(underTest.getFieldValue(TAG + ".1.label")).isNull();
    assertThat(underTest.getFieldValue(TAG + ".1.count")).isNull();
    assertThat(underTest.getFieldValue(TAG + ".2.label")).isNull();
    assertThat(underTest.getFieldValue(TAG + ".2.count")).isNull();
    assertThat(underTest.getFieldValue(COMPONENT + ".1.label")).isNull();
    assertThat(underTest.getFieldValue(COMPONENT + ".1.count")).isNull();
    assertThat(underTest.getFieldValue(COMPONENT + ".2.label")).isNull();
    assertThat(underTest.getFieldValue(COMPONENT + ".2.count")).isNull();
    assertThat(underTest.getFieldValue(RULE + ".1.label")).isNull();
    assertThat(underTest.getFieldValue(RULE + ".1.count")).isNull();
    assertThat(underTest.getFieldValue(RULE + ".2.label")).isNull();
    assertThat(underTest.getFieldValue(RULE + ".2.count")).isNull();
    assertThat(underTest.getDefaultMessage()).startsWith("0 new issues on " + project.longName());
  }

  @Test
  public void set_statistics_when_some_issues_are_no_created_on_current_analysis() {

    UserDto maynard = db.users().insertUser(u -> u.setLogin("maynard"));
    UserDto keenan = db.users().insertUser(u -> u.setLogin("keenan"));
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "path"));
    ComponentDto file = db.components().insertComponent(newFileDto(directory));
    RuleDto rule1 = db.rules().insert(r -> r.setRepositoryKey("SonarQube").setRuleKey("rule1-the-world").setName("Rule the World").setLanguage("Java"));
    RuleDto rule2 = db.rules().insert(r -> r.setRepositoryKey("SonarQube").setRuleKey("rule1-the-universe").setName("Rule the Universe").setLanguage("Clojure"));
    IssueDto issue1 = db.issues().insert(rule1, project, file, i -> i.setType(BUG).setAssigneeUuid(maynard.getUuid()).setTags(asList("bug", "owasp")));
    IssueDto issue2 = db.issues().insert(rule2, project, directory, i -> i.setType(CODE_SMELL).setAssigneeUuid(keenan.getUuid()).setTags(singletonList("owasp")));

    NewIssuesStatistics.Stats stats = new NewIssuesStatistics.Stats(i -> i.key().equals(issue2.getKey()));
    IntStream.rangeClosed(1, 5).forEach(i -> stats.add(issue1.toDefaultIssue()));
    IntStream.rangeClosed(1, 3).forEach(i -> stats.add(issue2.toDefaultIssue()));
    mockDetailsSupplierComponents(project, directory, file);
    mockDetailsSupplierRules(rule1, rule2);
    mockDetailsSupplierAssignees(maynard, keenan);

    underTest.setStatistics(project.longName(), stats);

    assertThat(underTest.getFieldValue(ISSUE + ".count")).isEqualTo("3");
    assertThat(underTest.getFieldValue(ASSIGNEE + ".1.label")).isEqualTo(keenan.getName());
    assertThat(underTest.getFieldValue(ASSIGNEE + ".1.count")).isEqualTo("3");
    assertThat(underTest.getFieldValue(ASSIGNEE + ".2.label")).isNull();
    assertThat(underTest.getFieldValue(ASSIGNEE + ".2.count")).isNull();
    assertThat(underTest.getFieldValue(TAG + ".1.label")).isEqualTo("owasp");
    assertThat(underTest.getFieldValue(TAG + ".1.count")).isEqualTo("3");
    assertThat(underTest.getFieldValue(TAG + ".2.label")).isNull();
    assertThat(underTest.getFieldValue(TAG + ".2.count")).isNull();
    assertThat(underTest.getFieldValue(COMPONENT + ".1.label")).isEqualTo(directory.name());
    assertThat(underTest.getFieldValue(COMPONENT + ".1.count")).isEqualTo("3");
    assertThat(underTest.getFieldValue(COMPONENT + ".2.label")).isNull();
    assertThat(underTest.getFieldValue(COMPONENT + ".2.count")).isNull();
    assertThat(underTest.getFieldValue(RULE + ".1.label")).isEqualTo("Rule the Universe (Clojure)");
    assertThat(underTest.getFieldValue(RULE + ".1.count")).isEqualTo("3");
    assertThat(underTest.getFieldValue(RULE + ".2.label")).isNull();
    assertThat(underTest.getFieldValue(RULE + ".2.count")).isNull();
    assertThat(underTest.getDefaultMessage()).startsWith("3 new issues on " + project.longName());
  }

  private void mockDetailsSupplierAssignees(UserDto... users) {
    for (UserDto user : users) {
      when(detailsSupplier.getUserNameByUuid(user.getUuid())).thenReturn(Optional.of(user.getName()));
    }
  }

  private void mockDetailsSupplierRules(RuleDto... rules) {
    for (RuleDto rule : rules) {
      when(detailsSupplier.getRuleDefinitionByRuleKey(rule.getKey()))
        .thenReturn(Optional.of(new RuleDefinition(rule.getName(), rule.getLanguage())));
    }
  }

  private void mockDetailsSupplierComponents(ComponentDto... components) {
    for (ComponentDto component : components) {
      when(detailsSupplier.getComponentNameByUuid(component.uuid())).thenReturn(Optional.of(component.name()));
    }
  }

  @Test
  public void set_assignee() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insert();
    UserDto user = db.users().insertUser();
    IssueDto issue = db.issues().insert(rule, project, file, i -> i.setAssigneeUuid(user.getUuid()));
    NewIssuesStatistics.Stats stats = new NewIssuesStatistics.Stats(i -> true);
    IntStream.rangeClosed(1, 5).forEach(i -> stats.add(issue.toDefaultIssue()));
    mockDetailsSupplierRules(rule);
    mockDetailsSupplierAssignees(user);
    mockDetailsSupplierComponents(project, file);

    underTest.setStatistics(project.longName(), stats);

    assertThat(underTest.getFieldValue(ASSIGNEE + ".1.label")).isEqualTo(user.getName());
    assertThat(underTest.getFieldValue(ASSIGNEE + ".1.count")).isEqualTo("5");
  }

  @Test
  public void add_only_5_assignees_with_biggest_issue_counts() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();
    UserDto user4 = db.users().insertUser();
    UserDto user5 = db.users().insertUser();
    UserDto user6 = db.users().insertUser();
    UserDto user7 = db.users().insertUser();
    UserDto user8 = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insert();
    NewIssuesStatistics.Stats stats = new NewIssuesStatistics.Stats(i -> true);
    IntStream.rangeClosed(1, 10).forEach(i -> stats.add(db.issues().insert(rule, project, file, issue -> issue.setAssigneeUuid(user1.getUuid())).toDefaultIssue()));
    IntStream.rangeClosed(1, 9).forEach(i -> stats.add(db.issues().insert(rule, project, file, issue -> issue.setAssigneeUuid(user2.getUuid())).toDefaultIssue()));
    IntStream.rangeClosed(1, 8).forEach(i -> stats.add(db.issues().insert(rule, project, file, issue -> issue.setAssigneeUuid(user3.getUuid())).toDefaultIssue()));
    IntStream.rangeClosed(1, 7).forEach(i -> stats.add(db.issues().insert(rule, project, file, issue -> issue.setAssigneeUuid(user4.getUuid())).toDefaultIssue()));
    IntStream.rangeClosed(1, 6).forEach(i -> stats.add(db.issues().insert(rule, project, file, issue -> issue.setAssigneeUuid(user5.getUuid())).toDefaultIssue()));
    IntStream.rangeClosed(1, 5).forEach(i -> stats.add(db.issues().insert(rule, project, file, issue -> issue.setAssigneeUuid(user6.getUuid())).toDefaultIssue()));
    IntStream.rangeClosed(1, 4).forEach(i -> stats.add(db.issues().insert(rule, project, file, issue -> issue.setAssigneeUuid(user7.getUuid())).toDefaultIssue()));
    IntStream.rangeClosed(1, 3).forEach(i -> stats.add(db.issues().insert(rule, project, file, issue -> issue.setAssigneeUuid(user8.getUuid())).toDefaultIssue()));
    mockDetailsSupplierAssignees(user1, user2, user3, user4, user5, user6, user7, user8);
    mockDetailsSupplierComponents(project, file);
    mockDetailsSupplierRules(rule);

    underTest.setStatistics(project.longName(), stats);

    assertThat(underTest.getFieldValue(ASSIGNEE + ".1.label")).isEqualTo(user1.getName());
    assertThat(underTest.getFieldValue(ASSIGNEE + ".1.count")).isEqualTo("10");
    assertThat(underTest.getFieldValue(ASSIGNEE + ".2.label")).isEqualTo(user2.getName());
    assertThat(underTest.getFieldValue(ASSIGNEE + ".2.count")).isEqualTo("9");
    assertThat(underTest.getFieldValue(ASSIGNEE + ".3.label")).isEqualTo(user3.getName());
    assertThat(underTest.getFieldValue(ASSIGNEE + ".3.count")).isEqualTo("8");
    assertThat(underTest.getFieldValue(ASSIGNEE + ".4.label")).isEqualTo(user4.getName());
    assertThat(underTest.getFieldValue(ASSIGNEE + ".4.count")).isEqualTo("7");
    assertThat(underTest.getFieldValue(ASSIGNEE + ".5.label")).isEqualTo(user5.getName());
    assertThat(underTest.getFieldValue(ASSIGNEE + ".5.count")).isEqualTo("6");
    assertThat(underTest.getFieldValue(ASSIGNEE + ".6.label")).isNull();
    assertThat(underTest.getFieldValue(ASSIGNEE + ".6.count")).isNull();
  }

  @Test
  public void add_only_5_components_with_biggest_issue_counts() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    RuleDto rule = db.rules().insert();
    NewIssuesStatistics.Stats stats = new NewIssuesStatistics.Stats(i -> true);
    ComponentDto file1 = db.components().insertComponent(newFileDto(project));
    IntStream.rangeClosed(1, 10).forEach(i -> stats.add(db.issues().insert(rule, project, file1).toDefaultIssue()));
    ComponentDto file2 = db.components().insertComponent(newFileDto(project));
    IntStream.rangeClosed(1, 9).forEach(i -> stats.add(db.issues().insert(rule, project, file2).toDefaultIssue()));
    ComponentDto file3 = db.components().insertComponent(newFileDto(project));
    IntStream.rangeClosed(1, 8).forEach(i -> stats.add(db.issues().insert(rule, project, file3).toDefaultIssue()));
    ComponentDto file4 = db.components().insertComponent(newFileDto(project));
    IntStream.rangeClosed(1, 7).forEach(i -> stats.add(db.issues().insert(rule, project, file4).toDefaultIssue()));
    ComponentDto file5 = db.components().insertComponent(newFileDto(project));
    IntStream.rangeClosed(1, 6).forEach(i -> stats.add(db.issues().insert(rule, project, file5).toDefaultIssue()));
    ComponentDto file6 = db.components().insertComponent(newFileDto(project));
    IntStream.rangeClosed(1, 5).forEach(i -> stats.add(db.issues().insert(rule, project, file6).toDefaultIssue()));
    ComponentDto file7 = db.components().insertComponent(newFileDto(project));
    IntStream.rangeClosed(1, 4).forEach(i -> stats.add(db.issues().insert(rule, project, file7).toDefaultIssue()));
    ComponentDto file8 = db.components().insertComponent(newFileDto(project));
    IntStream.rangeClosed(1, 3).forEach(i -> stats.add(db.issues().insert(rule, project, file8).toDefaultIssue()));
    mockDetailsSupplierComponents(project, file1, file2, file3, file4, file5, file6, file7, file8);
    mockDetailsSupplierRules(rule);

    underTest.setStatistics(project.longName(), stats);

    assertThat(underTest.getFieldValue(COMPONENT + ".1.label")).isEqualTo(file1.name());
    assertThat(underTest.getFieldValue(COMPONENT + ".1.count")).isEqualTo("10");
    assertThat(underTest.getFieldValue(COMPONENT + ".2.label")).isEqualTo(file2.name());
    assertThat(underTest.getFieldValue(COMPONENT + ".2.count")).isEqualTo("9");
    assertThat(underTest.getFieldValue(COMPONENT + ".3.label")).isEqualTo(file3.name());
    assertThat(underTest.getFieldValue(COMPONENT + ".3.count")).isEqualTo("8");
    assertThat(underTest.getFieldValue(COMPONENT + ".4.label")).isEqualTo(file4.name());
    assertThat(underTest.getFieldValue(COMPONENT + ".4.count")).isEqualTo("7");
    assertThat(underTest.getFieldValue(COMPONENT + ".5.label")).isEqualTo(file5.name());
    assertThat(underTest.getFieldValue(COMPONENT + ".5.count")).isEqualTo("6");
    assertThat(underTest.getFieldValue(COMPONENT + ".6.label")).isNull();
    assertThat(underTest.getFieldValue(COMPONENT + ".6.count")).isNull();
  }

  @Test
  public void add_only_5_rules_with_biggest_issue_counts() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    NewIssuesStatistics.Stats stats = new NewIssuesStatistics.Stats(i -> true);
    RuleDto rule1 = db.rules().insert(r -> r.setLanguage("Java"));
    IntStream.rangeClosed(1, 10).forEach(i -> stats.add(db.issues().insert(rule1, project, file).toDefaultIssue()));
    RuleDto rule2 = db.rules().insert(r -> r.setLanguage("Java"));
    IntStream.rangeClosed(1, 9).forEach(i -> stats.add(db.issues().insert(rule2, project, file).toDefaultIssue()));
    RuleDto rule3 = db.rules().insert(r -> r.setLanguage("Java"));
    IntStream.rangeClosed(1, 8).forEach(i -> stats.add(db.issues().insert(rule3, project, file).toDefaultIssue()));
    RuleDto rule4 = db.rules().insert(r -> r.setLanguage("Java"));
    IntStream.rangeClosed(1, 7).forEach(i -> stats.add(db.issues().insert(rule4, project, file).toDefaultIssue()));
    RuleDto rule5 = db.rules().insert(r -> r.setLanguage("Java"));
    IntStream.rangeClosed(1, 6).forEach(i -> stats.add(db.issues().insert(rule5, project, file).toDefaultIssue()));
    RuleDto rule6 = db.rules().insert(r -> r.setLanguage("Java"));
    IntStream.rangeClosed(1, 5).forEach(i -> stats.add(db.issues().insert(rule6, project, file).toDefaultIssue()));
    RuleDto rule7 = db.rules().insert(r -> r.setLanguage("Java"));
    IntStream.rangeClosed(1, 4).forEach(i -> stats.add(db.issues().insert(rule7, project, file).toDefaultIssue()));
    RuleDto rule8 = db.rules().insert(r -> r.setLanguage("Java"));
    IntStream.rangeClosed(1, 3).forEach(i -> stats.add(db.issues().insert(rule8, project, file).toDefaultIssue()));
    mockDetailsSupplierComponents(project, file);
    mockDetailsSupplierRules(rule1, rule2, rule3, rule4, rule5, rule6, rule7, rule8);

    underTest.setStatistics(project.longName(), stats);

    String javaSuffix = " (Java)";
    assertThat(underTest.getFieldValue(RULE + ".1.label")).isEqualTo(rule1.getName() + javaSuffix);
    assertThat(underTest.getFieldValue(RULE + ".1.count")).isEqualTo("10");
    assertThat(underTest.getFieldValue(RULE + ".2.label")).isEqualTo(rule2.getName() + javaSuffix);
    assertThat(underTest.getFieldValue(RULE + ".2.count")).isEqualTo("9");
    assertThat(underTest.getFieldValue(RULE + ".3.label")).isEqualTo(rule3.getName() + javaSuffix);
    assertThat(underTest.getFieldValue(RULE + ".3.count")).isEqualTo("8");
    assertThat(underTest.getFieldValue(RULE + ".4.label")).isEqualTo(rule4.getName() + javaSuffix);
    assertThat(underTest.getFieldValue(RULE + ".4.count")).isEqualTo("7");
    assertThat(underTest.getFieldValue(RULE + ".5.label")).isEqualTo(rule5.getName() + javaSuffix);
    assertThat(underTest.getFieldValue(RULE + ".5.count")).isEqualTo("6");
    assertThat(underTest.getFieldValue(RULE + ".6.label")).isNull();
    assertThat(underTest.getFieldValue(RULE + ".6.count")).isNull();
  }

  @Test
  public void RuleDefinition_implements_equals_base_on_name_and_language() {
    String name = randomAlphabetic(5);
    String language = randomAlphabetic(6);
    RuleDefinition underTest = new RuleDefinition(name, language);

    assertThat(underTest)
      .isEqualTo(underTest)
      .isEqualTo(new RuleDefinition(name, language))
      .isNotEqualTo(new RuleDefinition(language, name))
      .isNotEqualTo(new RuleDefinition(randomAlphabetic(7), name))
      .isNotEqualTo(new RuleDefinition(language, randomAlphabetic(7)))
      .isNotEqualTo(new RuleDefinition(language, null))
      .isNotNull()
      .isNotEqualTo(new Object());
  }

  @Test
  public void RuleDefinition_implements_hashcode_base_on_name_and_language() {
    String name = randomAlphabetic(5);
    String language = randomAlphabetic(6);
    RuleDefinition underTest = new RuleDefinition(name, language);

    assertThat(underTest)
      .hasSameHashCodeAs(underTest)
      .hasSameHashCodeAs(new RuleDefinition(name, language));

    assertThat(underTest.hashCode())
      .isNotEqualTo(new RuleDefinition(language, name).hashCode())
      .isNotEqualTo(new RuleDefinition(randomAlphabetic(7), name).hashCode())
      .isNotEqualTo(new RuleDefinition(language, randomAlphabetic(7)).hashCode())
      .isNotEqualTo(new RuleDefinition(language, null).hashCode())
      .isNotEqualTo(new Object().hashCode());
  }
}
