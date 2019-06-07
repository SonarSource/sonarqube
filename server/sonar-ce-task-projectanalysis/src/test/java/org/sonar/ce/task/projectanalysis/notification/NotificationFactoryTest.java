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
package org.sonar.ce.task.projectanalysis.notification;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Durations;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.issue.DumbRule;
import org.sonar.ce.task.projectanalysis.issue.RuleRepositoryRule;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.component.BranchType;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;
import org.sonar.server.issue.notification.IssuesChangesNotification;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.AnalysisChange;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.ChangedIssue;
import org.sonar.server.issue.notification.IssuesChangesNotificationSerializer;
import org.sonar.server.issue.notification.MyNewIssuesNotification;
import org.sonar.server.issue.notification.NewIssuesNotification;
import org.sonar.server.issue.notification.NewIssuesNotification.DetailsSupplier;
import org.sonar.server.issue.notification.NewIssuesNotification.RuleDefinition;

import static java.util.Collections.emptyMap;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

@RunWith(DataProviderRunner.class)
public class NotificationFactoryTest {
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public RuleRepositoryRule ruleRepository = new RuleRepositoryRule();
  @Rule
  public AnalysisMetadataHolderRule analysisMetadata = new AnalysisMetadataHolderRule();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Durations durations = new Durations();
  private IssuesChangesNotificationSerializer issuesChangesSerializer = mock(IssuesChangesNotificationSerializer.class);
  private NotificationFactory underTest = new NotificationFactory(treeRootHolder, analysisMetadata, ruleRepository, durations, issuesChangesSerializer);

  @Test
  public void newMyNewIssuesNotification_throws_NPE_if_assigneesByUuid_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("assigneesByUuid can't be null");

    underTest.newMyNewIssuesNotification(null);
  }

  @Test
  public void newNewIssuesNotification_throws_NPE_if_assigneesByUuid_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("assigneesByUuid can't be null");

    underTest.newNewIssuesNotification(null);
  }

  @Test
  public void newMyNewIssuesNotification_returns_MyNewIssuesNotification_object_with_the_constructor_Durations() {
    MyNewIssuesNotification notification = underTest.newMyNewIssuesNotification(emptyMap());

    assertThat(readDurationsField(notification)).isSameAs(durations);
  }

  @Test
  public void newNewIssuesNotification_returns_NewIssuesNotification_object_with_the_constructor_Durations() {
    NewIssuesNotification notification = underTest.newNewIssuesNotification(emptyMap());

    assertThat(readDurationsField(notification)).isSameAs(durations);
  }

  @Test
  public void newMyNewIssuesNotification_DetailsSupplier_getUserNameByUuid_fails_with_NPE_if_uuid_is_null() {
    MyNewIssuesNotification underTest = this.underTest.newMyNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("uuid can't be null");

    detailsSupplier.getUserNameByUuid(null);
  }

  @Test
  public void newMyNewIssuesNotification_DetailsSupplier_getUserNameByUuid_always_returns_empty_if_map_argument_is_empty() {
    MyNewIssuesNotification underTest = this.underTest.newMyNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);
    assertThat(detailsSupplier.getUserNameByUuid("foo")).isEmpty();
  }

  @Test
  public void newMyNewIssuesNotification_DetailsSupplier_getUserNameByUuid_returns_name_of_user_from_map_argument() {
    Set<UserDto> users = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> UserTesting.newUserDto().setLogin("user" + i))
      .collect(Collectors.toSet());

    MyNewIssuesNotification underTest = this.underTest.newMyNewIssuesNotification(
      users.stream().collect(uniqueIndex(UserDto::getUuid)));

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);
    assertThat(detailsSupplier.getUserNameByUuid("foo")).isEmpty();
    users
      .forEach(user -> assertThat(detailsSupplier.getUserNameByUuid(user.getUuid())).contains(user.getName()));
  }

  @Test
  public void newMyNewIssuesNotification_DetailsSupplier_getUserNameByUuid_returns_empty_if_user_has_null_name() {
    UserDto user = UserTesting.newUserDto().setLogin("user_noname").setName(null);

    MyNewIssuesNotification underTest = this.underTest.newMyNewIssuesNotification(ImmutableMap.of(user.getUuid(), user));

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);
    assertThat(detailsSupplier.getUserNameByUuid(user.getUuid())).isEmpty();
  }

  @Test
  public void newNewIssuesNotification_DetailsSupplier_getUserNameByUuid_fails_with_NPE_if_uuid_is_null() {
    NewIssuesNotification underTest = this.underTest.newNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("uuid can't be null");

    detailsSupplier.getUserNameByUuid(null);
  }

  @Test
  public void newNewIssuesNotification_DetailsSupplier_getUserNameByUuid_always_returns_empty_if_map_argument_is_empty() {
    NewIssuesNotification underTest = this.underTest.newNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);
    assertThat(detailsSupplier.getUserNameByUuid("foo")).isEmpty();
  }

  @Test
  public void newNewIssuesNotification_DetailsSupplier_getUserNameByUuid_returns_name_of_user_from_map_argument() {
    Set<UserDto> users = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> UserTesting.newUserDto().setLogin("user" + i))
      .collect(Collectors.toSet());

    NewIssuesNotification underTest = this.underTest.newNewIssuesNotification(
      users.stream().collect(uniqueIndex(UserDto::getUuid)));

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);
    assertThat(detailsSupplier.getUserNameByUuid("foo")).isEmpty();
    users
      .forEach(user -> assertThat(detailsSupplier.getUserNameByUuid(user.getUuid())).contains(user.getName()));
  }

  @Test
  public void newNewIssuesNotification_DetailsSupplier_getUserNameByUuid_returns_empty_if_user_has_null_name() {
    UserDto user = UserTesting.newUserDto().setLogin("user_noname").setName(null);

    NewIssuesNotification underTest = this.underTest.newNewIssuesNotification(ImmutableMap.of(user.getUuid(), user));

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);
    assertThat(detailsSupplier.getUserNameByUuid(user.getUuid())).isEmpty();
  }

  @Test
  public void newMyNewIssuesNotification_DetailsSupplier_getComponentNameByUuid_fails_with_ISE_if_TreeRootHolder_is_not_initialized() {
    MyNewIssuesNotification underTest = this.underTest.newMyNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Holder has not been initialized yet");

    detailsSupplier.getComponentNameByUuid("foo");
  }

  @Test
  public void newMyNewIssuesNotification_DetailsSupplier_getComponentNameByUuid_fails_with_NPE_if_uuid_is_null() {
    treeRootHolder.setRoot(ReportComponent.builder(PROJECT, 1).setUuid("rootUuid").setName("root").build());

    MyNewIssuesNotification underTest = this.underTest.newMyNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("uuid can't be null");

    detailsSupplier.getComponentNameByUuid(null);
  }

  @Test
  public void newMyNewIssuesNotification_DetailsSupplier_getComponentNameByUuid_returns_name_of_project_in_TreeRootHolder() {
    treeRootHolder.setRoot(ReportComponent.builder(PROJECT, 1).setUuid("rootUuid").setName("root").build());

    MyNewIssuesNotification underTest = this.underTest.newMyNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    assertThat(detailsSupplier.getComponentNameByUuid("rootUuid")).contains("root");
    assertThat(detailsSupplier.getComponentNameByUuid("foo")).isEmpty();
  }

  @Test
  public void newMyNewIssuesNotification_DetailsSupplier_getComponentNameByUuid_returns_shortName_of_dir_and_file_in_TreeRootHolder() {
    treeRootHolder.setRoot(ReportComponent.builder(PROJECT, 1).setUuid("rootUuid").setName("root")
      .addChildren(ReportComponent.builder(DIRECTORY, 2).setUuid("dir1Uuid").setName("dir1").setShortName("dir1_short")
        .addChildren(ReportComponent.builder(FILE, 21).setUuid("file21Uuid").setName("file21").setShortName("file21_short").build())
        .build())
      .addChildren(ReportComponent.builder(DIRECTORY, 3).setUuid("dir2Uuid").setName("dir2").setShortName("dir2_short")
        .addChildren(ReportComponent.builder(FILE, 31).setUuid("file31Uuid").setName("file31").setShortName("file31_short").build())
        .addChildren(ReportComponent.builder(FILE, 32).setUuid("file32Uuid").setName("file32").setShortName("file32_short").build())
        .build())
      .addChildren(ReportComponent.builder(FILE, 11).setUuid("file11Uuid").setName("file11").setShortName("file11_short").build())
      .build());
    MyNewIssuesNotification underTest = this.underTest.newMyNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    Stream.of("dir1", "dir2", "file11", "file21", "file31", "file32")
      .forEach(name -> {
        assertThat(detailsSupplier.getComponentNameByUuid(name + "Uuid")).contains(name + "_short");
        assertThat(detailsSupplier.getComponentNameByUuid(name)).isEmpty();
      });
  }

  @Test
  public void newNewIssuesNotification_DetailsSupplier_getComponentNameByUuid_fails_with_ISE_if_TreeRootHolder_is_not_initialized() {
    NewIssuesNotification underTest = this.underTest.newNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Holder has not been initialized yet");

    detailsSupplier.getComponentNameByUuid("foo");
  }

  @Test
  public void newNewIssuesNotification_DetailsSupplier_getComponentNameByUuid_fails_with_NPE_if_uuid_is_null() {
    treeRootHolder.setRoot(ReportComponent.builder(PROJECT, 1).setUuid("rootUuid").setName("root").build());
    NewIssuesNotification underTest = this.underTest.newNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("uuid can't be null");

    detailsSupplier.getComponentNameByUuid(null);
  }

  @Test
  public void newNewIssuesNotification_DetailsSupplier_getComponentNameByUuid_returns_name_of_project_in_TreeRootHolder() {
    treeRootHolder.setRoot(ReportComponent.builder(PROJECT, 1).setUuid("rootUuid").setName("root").build());

    NewIssuesNotification underTest = this.underTest.newNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    assertThat(detailsSupplier.getComponentNameByUuid("rootUuid")).contains("root");
    assertThat(detailsSupplier.getComponentNameByUuid("foo")).isEmpty();
  }

  @Test
  public void newNewIssuesNotification_DetailsSupplier_getComponentNameByUuid_returns_shortName_of_dir_and_file_in_TreeRootHolder() {
    treeRootHolder.setRoot(ReportComponent.builder(PROJECT, 1).setUuid("rootUuid").setName("root")
      .addChildren(ReportComponent.builder(DIRECTORY, 2).setUuid("dir1Uuid").setName("dir1").setShortName("dir1_short")
        .addChildren(ReportComponent.builder(FILE, 21).setUuid("file21Uuid").setName("file21").setShortName("file21_short").build())
        .build())
      .addChildren(ReportComponent.builder(DIRECTORY, 3).setUuid("dir2Uuid").setName("dir2").setShortName("dir2_short")
        .addChildren(ReportComponent.builder(FILE, 31).setUuid("file31Uuid").setName("file31").setShortName("file31_short").build())
        .addChildren(ReportComponent.builder(FILE, 32).setUuid("file32Uuid").setName("file32").setShortName("file32_short").build())
        .build())
      .addChildren(ReportComponent.builder(FILE, 11).setUuid("file11Uuid").setName("file11").setShortName("file11_short").build())
      .build());

    NewIssuesNotification underTest = this.underTest.newNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    Stream.of("dir1", "dir2", "file11", "file21", "file31", "file32")
      .forEach(name -> {
        assertThat(detailsSupplier.getComponentNameByUuid(name + "Uuid")).contains(name + "_short");
        assertThat(detailsSupplier.getComponentNameByUuid(name)).isEmpty();
      });
  }

  @Test
  public void newMyNewIssuesNotification_DetailsSupplier_getRuleDefinitionByRuleKey_fails_with_NPE_if_ruleKey_is_null() {
    MyNewIssuesNotification underTest = this.underTest.newMyNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("ruleKey can't be null");

    detailsSupplier.getRuleDefinitionByRuleKey(null);
  }

  @Test
  public void newMyNewIssuesNotification_DetailsSupplier_getRuleDefinitionByRuleKey_always_returns_empty_if_RuleRepository_is_empty() {
    MyNewIssuesNotification underTest = this.underTest.newMyNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    assertThat(detailsSupplier.getRuleDefinitionByRuleKey(RuleKey.of("foo", "bar"))).isEmpty();
    assertThat(detailsSupplier.getRuleDefinitionByRuleKey(RuleKey.of("bar", "foo"))).isEmpty();
  }

  @Test
  public void newMyNewIssuesNotification_DetailsSupplier_getRuleDefinitionByRuleKey_returns_name_and_language_from_RuleRepository() {
    RuleKey rulekey1 = RuleKey.of("foo", "bar");
    RuleKey rulekey2 = RuleKey.of("foo", "donut");
    RuleKey rulekey3 = RuleKey.of("no", "language");
    DumbRule rule1 = ruleRepository.add(rulekey1).setName("rule1").setLanguage("lang1");
    DumbRule rule2 = ruleRepository.add(rulekey2).setName("rule2").setLanguage("lang2");
    DumbRule rule3 = ruleRepository.add(rulekey3).setName("rule3");

    MyNewIssuesNotification underTest = this.underTest.newMyNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    assertThat(detailsSupplier.getRuleDefinitionByRuleKey(rulekey1))
      .contains(new RuleDefinition(rule1.getName(), rule1.getLanguage()));
    assertThat(detailsSupplier.getRuleDefinitionByRuleKey(rulekey2))
      .contains(new RuleDefinition(rule2.getName(), rule2.getLanguage()));
    assertThat(detailsSupplier.getRuleDefinitionByRuleKey(rulekey3))
      .contains(new RuleDefinition(rule3.getName(), null));
    assertThat(detailsSupplier.getRuleDefinitionByRuleKey(RuleKey.of("donut", "foo")))
      .isEmpty();
  }

  @Test
  public void newNewIssuesNotification_DetailsSupplier_getRuleDefinitionByRuleKey_fails_with_NPE_if_ruleKey_is_null() {
    NewIssuesNotification underTest = this.underTest.newNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("ruleKey can't be null");

    detailsSupplier.getRuleDefinitionByRuleKey(null);
  }

  @Test
  public void newNewIssuesNotification_DetailsSupplier_getRuleDefinitionByRuleKey_always_returns_empty_if_RuleRepository_is_empty() {
    NewIssuesNotification underTest = this.underTest.newNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    assertThat(detailsSupplier.getRuleDefinitionByRuleKey(RuleKey.of("foo", "bar"))).isEmpty();
    assertThat(detailsSupplier.getRuleDefinitionByRuleKey(RuleKey.of("bar", "foo"))).isEmpty();
  }

  @Test
  public void newNewIssuesNotification_DetailsSupplier_getRuleDefinitionByRuleKey_returns_name_and_language_from_RuleRepository() {
    RuleKey rulekey1 = RuleKey.of("foo", "bar");
    RuleKey rulekey2 = RuleKey.of("foo", "donut");
    RuleKey rulekey3 = RuleKey.of("no", "language");
    DumbRule rule1 = ruleRepository.add(rulekey1).setName("rule1").setLanguage("lang1");
    DumbRule rule2 = ruleRepository.add(rulekey2).setName("rule2").setLanguage("lang2");
    DumbRule rule3 = ruleRepository.add(rulekey3).setName("rule3");

    NewIssuesNotification underTest = this.underTest.newNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    assertThat(detailsSupplier.getRuleDefinitionByRuleKey(rulekey1))
      .contains(new RuleDefinition(rule1.getName(), rule1.getLanguage()));
    assertThat(detailsSupplier.getRuleDefinitionByRuleKey(rulekey2))
      .contains(new RuleDefinition(rule2.getName(), rule2.getLanguage()));
    assertThat(detailsSupplier.getRuleDefinitionByRuleKey(rulekey3))
      .contains(new RuleDefinition(rule3.getName(), null));
    assertThat(detailsSupplier.getRuleDefinitionByRuleKey(RuleKey.of("donut", "foo")))
      .isEmpty();
  }

  @Test
  public void newIssuesChangesNotification_fails_with_ISE_if_analysis_date_has_not_been_set() {
    Set<DefaultIssue> issues = IntStream.range(0, 1 + new Random().nextInt(2))
      .mapToObj(i -> new DefaultIssue())
      .collect(Collectors.toSet());
    Map<String, UserDto> assigneesByUuid = nonEmptyAssigneesByUuid();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Analysis date has not been set");

    underTest.newIssuesChangesNotification(issues, assigneesByUuid);
  }

  @Test
  public void newIssuesChangesNotification_fails_with_IAE_if_issues_is_empty() {
    analysisMetadata.setAnalysisDate(new Random().nextLong());
    Map<String, UserDto> assigneesByUuid = nonEmptyAssigneesByUuid();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("issues can't be empty");

    underTest.newIssuesChangesNotification(Collections.emptySet(), assigneesByUuid);
  }

  @Test
  public void newIssuesChangesNotification_fails_with_NPE_if_issue_has_no_rule() {
    DefaultIssue issue = new DefaultIssue();
    Map<String, UserDto> assigneesByUuid = nonEmptyAssigneesByUuid();
    analysisMetadata.setAnalysisDate(new Random().nextLong());

    expectedException.expect(NullPointerException.class);

    underTest.newIssuesChangesNotification(ImmutableSet.of(issue), assigneesByUuid);
  }

  @Test
  public void newIssuesChangesNotification_fails_with_ISE_if_rule_of_issue_does_not_exist_in_repository() {
    RuleKey ruleKey = RuleKey.of("foo", "bar");
    DefaultIssue issue = new DefaultIssue()
      .setRuleKey(ruleKey);
    Map<String, UserDto> assigneesByUuid = nonEmptyAssigneesByUuid();
    analysisMetadata.setAnalysisDate(new Random().nextLong());

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can not find rule " + ruleKey + " in RuleRepository");

    underTest.newIssuesChangesNotification(ImmutableSet.of(issue), assigneesByUuid);
  }

  @Test
  public void newIssuesChangesNotification_fails_with_ISE_if_treeRootHolder_is_empty() {
    RuleKey ruleKey = RuleKey.of("foo", "bar");
    DefaultIssue issue = new DefaultIssue()
      .setRuleKey(ruleKey);
    Map<String, UserDto> assigneesByUuid = nonEmptyAssigneesByUuid();
    ruleRepository.add(ruleKey);
    analysisMetadata.setAnalysisDate(new Random().nextLong());

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Holder has not been initialized yet");

    underTest.newIssuesChangesNotification(ImmutableSet.of(issue), assigneesByUuid);
  }

  @Test
  public void newIssuesChangesNotification_fails_with_ISE_if_branch_has_not_been_set() {
    RuleKey ruleKey = RuleKey.of("foo", "bar");
    DefaultIssue issue = new DefaultIssue()
      .setRuleKey(ruleKey);
    Map<String, UserDto> assigneesByUuid = nonEmptyAssigneesByUuid();
    ruleRepository.add(ruleKey);
    analysisMetadata.setAnalysisDate(new Random().nextLong());
    treeRootHolder.setRoot(ReportComponent.builder(PROJECT, 1).build());

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Branch has not been set");

    underTest.newIssuesChangesNotification(ImmutableSet.of(issue), assigneesByUuid);
  }

  @Test
  public void newIssuesChangesNotification_fails_with_NPE_if_issue_has_no_key() {
    RuleKey ruleKey = RuleKey.of("foo", "bar");
    DefaultIssue issue = new DefaultIssue()
      .setRuleKey(ruleKey);
    Map<String, UserDto> assigneesByUuid = nonEmptyAssigneesByUuid();
    ruleRepository.add(ruleKey);
    treeRootHolder.setRoot(ReportComponent.builder(PROJECT, 1).build());
    analysisMetadata.setAnalysisDate(new Random().nextLong());
    analysisMetadata.setBranch(mock(Branch.class));

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("key can't be null");

    underTest.newIssuesChangesNotification(ImmutableSet.of(issue), assigneesByUuid);
  }

  @Test
  public void newIssuesChangesNotification_fails_with_NPE_if_issue_has_no_status() {
    RuleKey ruleKey = RuleKey.of("foo", "bar");
    DefaultIssue issue = new DefaultIssue()
      .setRuleKey(ruleKey)
      .setKey("issueKey");
    Map<String, UserDto> assigneesByUuid = nonEmptyAssigneesByUuid();
    ruleRepository.add(ruleKey);
    treeRootHolder.setRoot(ReportComponent.builder(PROJECT, 1).build());
    analysisMetadata.setAnalysisDate(new Random().nextLong());
    analysisMetadata.setBranch(mock(Branch.class));

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("newStatus can't be null");

    underTest.newIssuesChangesNotification(ImmutableSet.of(issue), assigneesByUuid);
  }

  @Test
  @UseDataProvider("noBranchNameBranches")
  public void newIssuesChangesNotification_creates_project_from_TreeRootHolder_and_branch_name_only_on_long_non_main_branches(Branch branch) {
    RuleKey ruleKey = RuleKey.of("foo", "bar");
    DefaultIssue issue = new DefaultIssue()
      .setRuleKey(ruleKey)
      .setKey("issueKey")
      .setStatus(STATUS_OPEN);
    Map<String, UserDto> assigneesByUuid = nonEmptyAssigneesByUuid();
    ReportComponent project = ReportComponent.builder(PROJECT, 1).build();
    ruleRepository.add(ruleKey);
    treeRootHolder.setRoot(project);
    analysisMetadata.setAnalysisDate(new Random().nextLong());
    analysisMetadata.setBranch(branch);
    IssuesChangesNotification expected = mock(IssuesChangesNotification.class);
    when(issuesChangesSerializer.serialize(any(IssuesChangesNotificationBuilder.class))).thenReturn(expected);

    IssuesChangesNotification notification = underTest.newIssuesChangesNotification(ImmutableSet.of(issue), assigneesByUuid);

    assertThat(notification).isSameAs(expected);

    IssuesChangesNotificationBuilder builder = verifyAndCaptureIssueChangeNotificationBuilder();
    assertThat(builder.getIssues()).hasSize(1);
    ChangedIssue changeIssue = builder.getIssues().iterator().next();
    assertThat(changeIssue.getProject().getUuid()).isEqualTo(project.getUuid());
    assertThat(changeIssue.getProject().getKey()).isEqualTo(project.getKey());
    assertThat(changeIssue.getProject().getProjectName()).isEqualTo(project.getName());
    assertThat(changeIssue.getProject().getBranchName()).isEmpty();
  }

  @DataProvider
  public static Object[][] noBranchNameBranches() {
    Branch mainBranch = mock(Branch.class);
    when(mainBranch.isMain()).thenReturn(true);
    when(mainBranch.getType()).thenReturn(BranchType.LONG);
    Branch shortBranch = mock(Branch.class);
    when(shortBranch.isMain()).thenReturn(false);
    when(shortBranch.getType()).thenReturn(BranchType.SHORT);
    Branch pr = mock(Branch.class);
    when(pr.isMain()).thenReturn(false);
    when(pr.getType()).thenReturn(BranchType.PULL_REQUEST);
    return new Object[][] {
      {mainBranch},
      {shortBranch},
      {pr}
    };
  }

  @Test
  public void newIssuesChangesNotification_creates_project_from_TreeRootHolder_and_branch_name_from_long_branch() {
    RuleKey ruleKey = RuleKey.of("foo", "bar");
    DefaultIssue issue = new DefaultIssue()
      .setRuleKey(ruleKey)
      .setKey("issueKey")
      .setStatus(STATUS_OPEN);
    Map<String, UserDto> assigneesByUuid = nonEmptyAssigneesByUuid();
    ReportComponent project = ReportComponent.builder(PROJECT, 1).build();
    String branchName = randomAlphabetic(12);
    ruleRepository.add(ruleKey);
    treeRootHolder.setRoot(project);
    analysisMetadata.setAnalysisDate(new Random().nextLong());
    analysisMetadata.setBranch(newBranch(BranchType.LONG, branchName));
    IssuesChangesNotification expected = mock(IssuesChangesNotification.class);
    when(issuesChangesSerializer.serialize(any(IssuesChangesNotificationBuilder.class))).thenReturn(expected);

    IssuesChangesNotification notification = underTest.newIssuesChangesNotification(ImmutableSet.of(issue), assigneesByUuid);

    assertThat(notification).isSameAs(expected);

    IssuesChangesNotificationBuilder builder = verifyAndCaptureIssueChangeNotificationBuilder();
    assertThat(builder.getIssues()).hasSize(1);
    ChangedIssue changeIssue = builder.getIssues().iterator().next();
    assertThat(changeIssue.getProject().getUuid()).isEqualTo(project.getUuid());
    assertThat(changeIssue.getProject().getKey()).isEqualTo(project.getKey());
    assertThat(changeIssue.getProject().getProjectName()).isEqualTo(project.getName());
    assertThat(changeIssue.getProject().getBranchName()).contains(branchName);
  }

  @Test
  public void newIssuesChangesNotification_creates_rule_from_RuleRepository() {
    RuleKey ruleKey = RuleKey.of("foo", "bar");
    DefaultIssue issue = new DefaultIssue()
      .setRuleKey(ruleKey)
      .setKey("issueKey")
      .setStatus(STATUS_OPEN);
    Map<String, UserDto> assigneesByUuid = nonEmptyAssigneesByUuid();
    ReportComponent project = ReportComponent.builder(PROJECT, 1).build();
    String branchName = randomAlphabetic(12);
    ruleRepository.add(ruleKey);
    treeRootHolder.setRoot(project);
    analysisMetadata.setAnalysisDate(new Random().nextLong());
    analysisMetadata.setBranch(newBranch(BranchType.LONG, branchName));
    IssuesChangesNotification expected = mock(IssuesChangesNotification.class);
    when(issuesChangesSerializer.serialize(any(IssuesChangesNotificationBuilder.class))).thenReturn(expected);

    IssuesChangesNotification notification = underTest.newIssuesChangesNotification(ImmutableSet.of(issue), assigneesByUuid);

    assertThat(notification).isSameAs(expected);
    IssuesChangesNotificationBuilder builder = verifyAndCaptureIssueChangeNotificationBuilder();
    assertThat(builder.getIssues()).hasSize(1);
    ChangedIssue changeIssue = builder.getIssues().iterator().next();
    assertThat(changeIssue.getRule().getKey()).isEqualTo(ruleKey);
    assertThat(changeIssue.getRule().getName()).isEqualTo(ruleRepository.getByKey(ruleKey).getName());
  }

  @Test
  public void newIssuesChangesNotification_fails_with_ISE_if_issue_has_assignee_not_in_assigneesByUuid() {
    RuleKey ruleKey = RuleKey.of("foo", "bar");
    String assigneeUuid = randomAlphabetic(40);
    DefaultIssue issue = new DefaultIssue()
      .setRuleKey(ruleKey)
      .setKey("issueKey")
      .setStatus(STATUS_OPEN)
      .setAssigneeUuid(assigneeUuid);
    Map<String, UserDto> assigneesByUuid = Collections.emptyMap();
    ReportComponent project = ReportComponent.builder(PROJECT, 1).build();
    ruleRepository.add(ruleKey);
    treeRootHolder.setRoot(project);
    analysisMetadata.setAnalysisDate(new Random().nextLong());
    analysisMetadata.setBranch(newBranch(BranchType.LONG, randomAlphabetic(12)));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can not find DTO for assignee uuid " + assigneeUuid);

    underTest.newIssuesChangesNotification(ImmutableSet.of(issue), assigneesByUuid);
  }

  @Test
  public void newIssuesChangesNotification_creates_assignee_from_UserDto() {
    RuleKey ruleKey = RuleKey.of("foo", "bar");
    String assigneeUuid = randomAlphabetic(40);
    DefaultIssue issue = new DefaultIssue()
      .setRuleKey(ruleKey)
      .setKey("issueKey")
      .setStatus(STATUS_OPEN)
      .setAssigneeUuid(assigneeUuid);
    UserDto userDto = UserTesting.newUserDto();
    Map<String, UserDto> assigneesByUuid = ImmutableMap.of(assigneeUuid, userDto);
    ReportComponent project = ReportComponent.builder(PROJECT, 1).build();
    ruleRepository.add(ruleKey);
    treeRootHolder.setRoot(project);
    analysisMetadata.setAnalysisDate(new Random().nextLong());
    analysisMetadata.setBranch(newBranch(BranchType.LONG, randomAlphabetic(12)));
    IssuesChangesNotification expected = mock(IssuesChangesNotification.class);
    when(issuesChangesSerializer.serialize(any(IssuesChangesNotificationBuilder.class))).thenReturn(expected);

    IssuesChangesNotification notification = underTest.newIssuesChangesNotification(ImmutableSet.of(issue), assigneesByUuid);

    assertThat(notification).isSameAs(expected);
    IssuesChangesNotificationBuilder builder = verifyAndCaptureIssueChangeNotificationBuilder();
    assertThat(builder.getIssues()).hasSize(1);
    ChangedIssue changeIssue = builder.getIssues().iterator().next();
    assertThat(changeIssue.getAssignee()).isPresent();
    IssuesChangesNotificationBuilder.User assignee = changeIssue.getAssignee().get();
    assertThat(assignee.getUuid()).isEqualTo(userDto.getUuid());
    assertThat(assignee.getName()).contains(userDto.getName());
    assertThat(assignee.getLogin()).isEqualTo(userDto.getLogin());
  }

  @Test
  public void newIssuesChangesNotification_creates_AnalysisChange_with_analysis_date() {
    RuleKey ruleKey = RuleKey.of("foo", "bar");
    DefaultIssue issue = new DefaultIssue()
      .setRuleKey(ruleKey)
      .setKey("issueKey")
      .setStatus(STATUS_OPEN);
    Map<String, UserDto> assigneesByUuid = nonEmptyAssigneesByUuid();
    ReportComponent project = ReportComponent.builder(PROJECT, 1).build();
    long analysisDate = new Random().nextLong();
    ruleRepository.add(ruleKey);
    treeRootHolder.setRoot(project);
    analysisMetadata.setAnalysisDate(analysisDate);
    analysisMetadata.setBranch(newBranch(BranchType.LONG, randomAlphabetic(12)));
    IssuesChangesNotification expected = mock(IssuesChangesNotification.class);
    when(issuesChangesSerializer.serialize(any(IssuesChangesNotificationBuilder.class))).thenReturn(expected);

    IssuesChangesNotification notification = underTest.newIssuesChangesNotification(ImmutableSet.of(issue), assigneesByUuid);

    assertThat(notification).isSameAs(expected);
    IssuesChangesNotificationBuilder builder = verifyAndCaptureIssueChangeNotificationBuilder();
    assertThat(builder.getIssues()).hasSize(1);
    assertThat(builder.getChange())
      .isInstanceOf(AnalysisChange.class)
      .extracting(IssuesChangesNotificationBuilder.Change::getDate)
      .isEqualTo(analysisDate);
  }

  @Test
  public void newIssuesChangesNotification_maps_all_issues() {
    Set<DefaultIssue> issues = IntStream.range(0, 3 + new Random().nextInt(5))
      .mapToObj(i -> new DefaultIssue()
        .setRuleKey(RuleKey.of("repo_" + i, "rule_" + i))
        .setKey("issue_key_" + i)
        .setStatus("status_" + i))
      .collect(Collectors.toSet());
    ReportComponent project = ReportComponent.builder(PROJECT, 1).build();
    long analysisDate = new Random().nextLong();
    issues.stream()
      .map(DefaultIssue::ruleKey)
      .forEach(ruleKey -> ruleRepository.add(ruleKey));
    treeRootHolder.setRoot(project);
    analysisMetadata.setAnalysisDate(analysisDate);
    analysisMetadata.setBranch(newBranch(BranchType.LONG, randomAlphabetic(12)));
    IssuesChangesNotification expected = mock(IssuesChangesNotification.class);
    when(issuesChangesSerializer.serialize(any(IssuesChangesNotificationBuilder.class))).thenReturn(expected);

    IssuesChangesNotification notification = underTest.newIssuesChangesNotification(issues, emptyMap());

    assertThat(notification).isSameAs(expected);
    IssuesChangesNotificationBuilder builder = verifyAndCaptureIssueChangeNotificationBuilder();
    assertThat(builder.getIssues()).hasSize(issues.size());
    Map<String, ChangedIssue> changedIssuesByKey = builder.getIssues().stream()
      .collect(uniqueIndex(ChangedIssue::getKey));
    issues.forEach(
      issue -> {
        ChangedIssue changedIssue = changedIssuesByKey.get(issue.key());
        assertThat(changedIssue.getNewStatus()).isEqualTo(issue.status());
        assertThat(changedIssue.getNewResolution()).isEmpty();
        assertThat(changedIssue.getAssignee()).isEmpty();
        assertThat(changedIssue.getRule().getKey()).isEqualTo(issue.ruleKey());
        assertThat(changedIssue.getRule().getName()).isEqualTo(ruleRepository.getByKey(issue.ruleKey()).getName());
      });
  }

  private static Map<String, UserDto> nonEmptyAssigneesByUuid() {
    return IntStream.range(0, 1 + new Random().nextInt(3))
      .boxed()
      .collect(uniqueIndex(i -> "uuid_" + i, i -> new UserDto()));
  }

  private IssuesChangesNotificationBuilder verifyAndCaptureIssueChangeNotificationBuilder() {
    ArgumentCaptor<IssuesChangesNotificationBuilder> builderCaptor = ArgumentCaptor.forClass(IssuesChangesNotificationBuilder.class);
    verify(issuesChangesSerializer).serialize(builderCaptor.capture());
    verifyNoMoreInteractions(issuesChangesSerializer);

    return builderCaptor.getValue();
  }

  private static Branch newBranch(BranchType branchType, String branchName) {
    Branch longBranch = mock(Branch.class);
    when(longBranch.isMain()).thenReturn(false);
    when(longBranch.getType()).thenReturn(branchType);
    when(longBranch.getName()).thenReturn(branchName);
    return longBranch;
  }

  private static Durations readDurationsField(NewIssuesNotification notification) {
    return readField(notification, "durations");
  }

  private static Durations readField(NewIssuesNotification notification, String fieldName) {
    try {
      Field durationsField = NewIssuesNotification.class.getDeclaredField(fieldName);
      durationsField.setAccessible(true);
      Object o = durationsField.get(notification);
      return (Durations) o;
    } catch (IllegalAccessException | NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  private static DetailsSupplier readDetailsSupplier(NewIssuesNotification notification) {
    try {
      Field durationsField = NewIssuesNotification.class.getDeclaredField("detailsSupplier");
      durationsField.setAccessible(true);
      return (DetailsSupplier) durationsField.get(notification);
    } catch (IllegalAccessException | NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }
}
