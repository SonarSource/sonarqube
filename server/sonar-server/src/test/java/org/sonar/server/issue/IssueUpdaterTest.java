/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.issue;

import java.util.Date;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDbTester;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueTesting;
import org.sonar.db.rule.RuleDbTester;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.issue.notification.IssueChangeNotification;
import org.sonar.server.issue.ws.SearchResponseData;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.rule.DefaultRuleFinder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.issue.IssueTesting.newDto;
import static org.sonar.db.rule.RuleTesting.newRuleDto;

public class IssueUpdaterTest {

  private System2 system2 = mock(System2.class);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  @Rule
  public EsTester esTester = new EsTester(new IssueIndexDefinition(new MapSettings().asConfig()));

  private DbClient dbClient = dbTester.getDbClient();
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);

  private RuleDbTester ruleDbTester = new RuleDbTester(dbTester);
  private IssueDbTester issueDbTester = new IssueDbTester(dbTester);
  private ComponentDbTester componentDbTester = new ComponentDbTester(dbTester);
  private IssueFieldsSetter issueFieldsSetter = new IssueFieldsSetter();
  private NotificationManager notificationManager = mock(NotificationManager.class);
  private ArgumentCaptor<IssueChangeNotification> notificationArgumentCaptor = ArgumentCaptor.forClass(IssueChangeNotification.class);

  private IssueIndexer issueIndexer = new IssueIndexer(esTester.client(), dbClient, new IssueIteratorFactory(dbClient));
  private TestIssueChangePostProcessor issueChangePostProcessor = new TestIssueChangePostProcessor();
  private IssueUpdater underTest = new IssueUpdater(dbClient,
    new ServerIssueStorage(system2, new DefaultRuleFinder(dbClient, defaultOrganizationProvider), dbClient, issueIndexer), notificationManager, issueChangePostProcessor);

  @Test
  public void update_issue() {
    DefaultIssue issue = issueDbTester.insertIssue(newIssue().setSeverity(MAJOR)).toDefaultIssue();
    IssueChangeContext context = IssueChangeContext.createUser(new Date(), "john");
    issueFieldsSetter.setSeverity(issue, BLOCKER, context);

    underTest.saveIssue(dbTester.getSession(), issue, context, null);

    IssueDto issueReloaded = dbClient.issueDao().selectByKey(dbTester.getSession(), issue.key()).get();
    assertThat(issueReloaded.getSeverity()).isEqualTo(BLOCKER);
  }

  @Test
  public void verify_notification() {
    RuleDto rule = ruleDbTester.insertRule(newRuleDto());
    ComponentDto project = componentDbTester.insertPrivateProject();
    ComponentDto file = componentDbTester.insertComponent(newFileDto(project));
    DefaultIssue issue = issueDbTester.insertIssue(IssueTesting.newIssue(rule.getDefinition(), project, file)).setSeverity(MAJOR).toDefaultIssue();
    IssueChangeContext context = IssueChangeContext.createUser(new Date(), "john");
    issueFieldsSetter.setSeverity(issue, BLOCKER, context);

    underTest.saveIssue(dbTester.getSession(), issue, context, "increase severity");

    verify(notificationManager).scheduleForSending(notificationArgumentCaptor.capture());
    IssueChangeNotification issueChangeNotification = notificationArgumentCaptor.getValue();
    assertThat(issueChangeNotification.getFieldValue("key")).isEqualTo(issue.key());
    assertThat(issueChangeNotification.getFieldValue("old.severity")).isEqualTo(MAJOR);
    assertThat(issueChangeNotification.getFieldValue("new.severity")).isEqualTo(BLOCKER);
    assertThat(issueChangeNotification.getFieldValue("componentKey")).isEqualTo(file.getDbKey());
    assertThat(issueChangeNotification.getFieldValue("componentName")).isEqualTo(file.longName());
    assertThat(issueChangeNotification.getFieldValue("projectKey")).isEqualTo(project.getDbKey());
    assertThat(issueChangeNotification.getFieldValue("projectName")).isEqualTo(project.name());
    assertThat(issueChangeNotification.getFieldValue("ruleName")).isEqualTo(rule.getName());
    assertThat(issueChangeNotification.getFieldValue("changeAuthor")).isEqualTo("john");
    assertThat(issueChangeNotification.getFieldValue("comment")).isEqualTo("increase severity");
  }

  @Test
  public void verify_notification_on_branch() {
    RuleDto rule = ruleDbTester.insertRule(newRuleDto());
    ComponentDto project = componentDbTester.insertMainBranch();
    ComponentDto branch = componentDbTester.insertProjectBranch(project);
    ComponentDto file = componentDbTester.insertComponent(newFileDto(branch));
    DefaultIssue issue = issueDbTester.insertIssue(IssueTesting.newIssue(rule.getDefinition(), branch, file)).setSeverity(MAJOR).toDefaultIssue();
    IssueChangeContext context = IssueChangeContext.createUser(new Date(), "john");
    issueFieldsSetter.setSeverity(issue, BLOCKER, context);

    underTest.saveIssue(dbTester.getSession(), issue, context, "increase severity");

    verify(notificationManager).scheduleForSending(notificationArgumentCaptor.capture());
    IssueChangeNotification issueChangeNotification = notificationArgumentCaptor.getValue();
    assertThat(issueChangeNotification.getFieldValue("key")).isEqualTo(issue.key());
    assertThat(issueChangeNotification.getFieldValue("projectKey")).isEqualTo(project.getDbKey());
    assertThat(issueChangeNotification.getFieldValue("projectName")).isEqualTo(project.name());
    assertThat(issueChangeNotification.getFieldValue("branch")).isEqualTo(branch.getBranch());
  }

  @Test
  public void verify_notification_when_issue_is_linked_on_removed_rule() {
    RuleDto rule = ruleDbTester.insertRule(newRuleDto().setStatus(RuleStatus.REMOVED));
    ComponentDto project = componentDbTester.insertPrivateProject();
    ComponentDto file = componentDbTester.insertComponent(newFileDto(project));
    DefaultIssue issue = issueDbTester.insertIssue(IssueTesting.newIssue(rule.getDefinition(), project, file)).setSeverity(MAJOR).toDefaultIssue();
    IssueChangeContext context = IssueChangeContext.createUser(new Date(), "john");
    issueFieldsSetter.setSeverity(issue, BLOCKER, context);

    underTest.saveIssue(dbTester.getSession(), issue, context, null);

    verify(notificationManager).scheduleForSending(notificationArgumentCaptor.capture());
    assertThat(notificationArgumentCaptor.getValue().getFieldValue("ruleName")).isNull();
  }

  private IssueDto newIssue() {
    RuleDto rule = ruleDbTester.insertRule(newRuleDto());
    ComponentDto project = componentDbTester.insertPrivateProject();
    ComponentDto file = componentDbTester.insertComponent(newFileDto(project));
    return newDto(rule, file, project);
  }

  @Test
  public void saveIssue_populates_specified_SearchResponseData_with_rule_project_and_component_retrieved_from_DB() {
    RuleDto rule = ruleDbTester.insertRule(newRuleDto());
    ComponentDto project = componentDbTester.insertPrivateProject();
    ComponentDto file = componentDbTester.insertComponent(newFileDto(project));
    IssueDto issueDto = IssueTesting.newIssue(rule.getDefinition(), project, file);
    DefaultIssue issue = issueDbTester.insertIssue(issueDto).setSeverity(MAJOR).toDefaultIssue();
    IssueChangeContext context = IssueChangeContext.createUser(new Date(), "john");
    issueFieldsSetter.setSeverity(issue, BLOCKER, context);

    SearchResponseData preloadedSearchResponseData = underTest.saveIssueAndPreloadSearchResponseData(dbTester.getSession(), issue, context, null, true);

    assertThat(preloadedSearchResponseData.getIssues())
      .hasSize(1);
    assertThat(preloadedSearchResponseData.getIssues().iterator().next())
      .isNotSameAs(issueDto);
    assertThat(preloadedSearchResponseData.getRules())
      .extracting(RuleDefinitionDto::getKey)
      .containsOnly(rule.getKey());
    assertThat(preloadedSearchResponseData.getComponents())
      .extracting(ComponentDto::uuid)
      .containsOnly(project.uuid(), file.uuid());
    assertThat(issueChangePostProcessor.calledComponents()).containsExactlyInAnyOrder(file);
  }

  @Test
  public void saveIssue_populates_specified_SearchResponseData_with_no_rule_but_with_project_and_component_if_rule_is_removed() {
    RuleDto rule = ruleDbTester.insertRule(newRuleDto().setStatus(RuleStatus.REMOVED));
    ComponentDto project = componentDbTester.insertPrivateProject();
    ComponentDto file = componentDbTester.insertComponent(newFileDto(project));
    IssueDto issueDto = IssueTesting.newIssue(rule.getDefinition(), project, file);
    DefaultIssue issue = issueDbTester.insertIssue(issueDto).setSeverity(MAJOR).toDefaultIssue();
    IssueChangeContext context = IssueChangeContext.createUser(new Date(), "john");
    issueFieldsSetter.setSeverity(issue, BLOCKER, context);

    SearchResponseData preloadedSearchResponseData = underTest.saveIssueAndPreloadSearchResponseData(dbTester.getSession(), issue, context, null, false);

    assertThat(preloadedSearchResponseData.getIssues())
      .hasSize(1);
    assertThat(preloadedSearchResponseData.getIssues().iterator().next())
      .isNotSameAs(issueDto);
    assertThat(preloadedSearchResponseData.getRules()).isNullOrEmpty();
    assertThat(preloadedSearchResponseData.getComponents())
      .extracting(ComponentDto::uuid)
      .containsOnly(project.uuid(), file.uuid());
  }
}
