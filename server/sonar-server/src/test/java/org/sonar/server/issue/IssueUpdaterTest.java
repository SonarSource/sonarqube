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
package org.sonar.server.issue;

import java.util.Date;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueTesting;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.issue.notification.IssueChangeNotification;
import org.sonar.server.issue.ws.SearchResponseData;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.rule.DefaultRuleFinder;

import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.db.component.ComponentTesting.newFileDto;

public class IssueUpdaterTest {

  private System2 system2 = mock(System2.class);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(system2);

  @Rule
  public EsTester es = EsTester.create();

  private DbClient dbClient = db.getDbClient();
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);

  private IssueFieldsSetter issueFieldsSetter = new IssueFieldsSetter();
  private NotificationManager notificationManager = mock(NotificationManager.class);
  private ArgumentCaptor<IssueChangeNotification> notificationArgumentCaptor = ArgumentCaptor.forClass(IssueChangeNotification.class);

  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), dbClient, new IssueIteratorFactory(dbClient));
  private TestIssueChangePostProcessor issueChangePostProcessor = new TestIssueChangePostProcessor();
  private IssueUpdater underTest = new IssueUpdater(dbClient,
    new WebIssueStorage(system2, dbClient, new DefaultRuleFinder(dbClient, defaultOrganizationProvider), issueIndexer), notificationManager, issueChangePostProcessor);

  @Test
  public void update_issue() {
    DefaultIssue issue = db.issues().insertIssue(i -> i.setSeverity(MAJOR)).toDefaultIssue();
    IssueChangeContext context = IssueChangeContext.createUser(new Date(), "user_uuid");
    issueFieldsSetter.setSeverity(issue, BLOCKER, context);

    underTest.saveIssue(db.getSession(), issue, context, null);

    IssueDto issueReloaded = dbClient.issueDao().selectByKey(db.getSession(), issue.key()).get();
    assertThat(issueReloaded.getSeverity()).isEqualTo(BLOCKER);
  }

  @Test
  public void verify_notification() {
    UserDto assignee = db.users().insertUser();
    RuleDto rule = db.rules().insertRule();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleType randomTypeExceptHotspot = RuleType.values()[nextInt(RuleType.values().length - 1)];
    DefaultIssue issue = db.issues().insertIssue(IssueTesting.newIssue(rule.getDefinition(), project, file)
      .setType(randomTypeExceptHotspot))
      .setSeverity(MAJOR)
      .setAssigneeUuid(assignee.getUuid())
      .toDefaultIssue();
    UserDto changeAuthor = db.users().insertUser();
    IssueChangeContext context = IssueChangeContext.createUser(new Date(), changeAuthor.getUuid());
    issueFieldsSetter.setSeverity(issue, BLOCKER, context);

    underTest.saveIssue(db.getSession(), issue, context, "increase severity");

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
    assertThat(issueChangeNotification.getFieldValue("changeAuthor")).isEqualTo(changeAuthor.getLogin());
    assertThat(issueChangeNotification.getFieldValue("comment")).isEqualTo("increase severity");
    assertThat(issueChangeNotification.getFieldValue("assignee")).isEqualTo(assignee.getLogin());
  }

  @Test
  public void verify_no_notification_on_hotspot() {
    UserDto assignee = db.users().insertUser();
    RuleDto rule = db.rules().insertRule();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    DefaultIssue issue = db.issues().insertIssue(IssueTesting.newIssue(rule.getDefinition(), project, file)
      .setType(RuleType.SECURITY_HOTSPOT))
      .setSeverity(MAJOR)
      .setAssigneeUuid(assignee.getUuid())
      .toDefaultIssue();
    UserDto changeAuthor = db.users().insertUser();
    IssueChangeContext context = IssueChangeContext.createUser(new Date(), changeAuthor.getUuid());
    issueFieldsSetter.setSeverity(issue, BLOCKER, context);

    underTest.saveIssue(db.getSession(), issue, context, "increase severity");

    verify(notificationManager, never()).scheduleForSending(any());
  }

  @Test
  public void verify_notification_on_branch() {
    RuleDto rule = db.rules().insertRule();
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project);
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    RuleType randomTypeExceptHotspot = RuleType.values()[nextInt(RuleType.values().length - 1)];
    DefaultIssue issue = db.issues().insertIssue(IssueTesting.newIssue(rule.getDefinition(), branch, file)
      .setType(randomTypeExceptHotspot)).setSeverity(MAJOR).toDefaultIssue();
    IssueChangeContext context = IssueChangeContext.createUser(new Date(), "user_uuid");
    issueFieldsSetter.setSeverity(issue, BLOCKER, context);

    underTest.saveIssue(db.getSession(), issue, context, "increase severity");

    verify(notificationManager).scheduleForSending(notificationArgumentCaptor.capture());
    IssueChangeNotification issueChangeNotification = notificationArgumentCaptor.getValue();
    assertThat(issueChangeNotification.getFieldValue("key")).isEqualTo(issue.key());
    assertThat(issueChangeNotification.getFieldValue("projectKey")).isEqualTo(project.getDbKey());
    assertThat(issueChangeNotification.getFieldValue("projectName")).isEqualTo(project.name());
    assertThat(issueChangeNotification.getFieldValue("branch")).isEqualTo(branch.getBranch());
  }

  @Test
  public void verify_notification_when_issue_is_linked_on_removed_rule() {
    RuleDto rule = db.rules().insertRule(r -> r.setStatus(RuleStatus.REMOVED));
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleType randomTypeExceptHotspot = RuleType.values()[nextInt(RuleType.values().length - 1)];
    DefaultIssue issue = db.issues().insertIssue(IssueTesting.newIssue(rule.getDefinition(), project, file)
    .setType(randomTypeExceptHotspot)).setSeverity(MAJOR).toDefaultIssue();
    IssueChangeContext context = IssueChangeContext.createUser(new Date(), "user_uuid");
    issueFieldsSetter.setSeverity(issue, BLOCKER, context);

    underTest.saveIssue(db.getSession(), issue, context, null);

    verify(notificationManager).scheduleForSending(notificationArgumentCaptor.capture());
    assertThat(notificationArgumentCaptor.getValue().getFieldValue("ruleName")).isNull();
  }

  @Test
  public void verify_notification_when_assignee_has_changed() {
    UserDto oldAssignee = db.users().insertUser();
    RuleDto rule = db.rules().insertRule();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleType randomTypeExceptHotspot = RuleType.values()[nextInt(RuleType.values().length - 1)];
    DefaultIssue issue = db.issues().insertIssue(IssueTesting.newIssue(rule.getDefinition(), project, file)
    .setType(randomTypeExceptHotspot))
      .setAssigneeUuid(oldAssignee.getUuid())
      .toDefaultIssue();
    UserDto changeAuthor = db.users().insertUser();
    IssueChangeContext context = IssueChangeContext.createUser(new Date(), changeAuthor.getUuid());
    UserDto newAssignee = db.users().insertUser();
    issueFieldsSetter.assign(issue, newAssignee, context);

    underTest.saveIssue(db.getSession(), issue, context, null);

    verify(notificationManager).scheduleForSending(notificationArgumentCaptor.capture());
    IssueChangeNotification issueChangeNotification = notificationArgumentCaptor.getValue();
    assertThat(issueChangeNotification.getFieldValue("key")).isEqualTo(issue.key());
    assertThat(issueChangeNotification.getFieldValue("new.assignee")).isEqualTo(newAssignee.getName());
    assertThat(issueChangeNotification.getFieldValue("old.assignee")).isNull();
    assertThat(issueChangeNotification.getFieldValue("assignee")).isEqualTo(newAssignee.getLogin());
  }

  @Test
  public void saveIssue_populates_specified_SearchResponseData_with_rule_project_and_component_retrieved_from_DB() {
    RuleDto rule = db.rules().insertRule();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    IssueDto issueDto = IssueTesting.newIssue(rule.getDefinition(), project, file);
    DefaultIssue issue = db.issues().insertIssue(issueDto).setSeverity(MAJOR).toDefaultIssue();
    IssueChangeContext context = IssueChangeContext.createUser(new Date(), "user_uuid");
    issueFieldsSetter.setSeverity(issue, BLOCKER, context);

    SearchResponseData preloadedSearchResponseData = underTest.saveIssueAndPreloadSearchResponseData(db.getSession(), issue, context, null, true);

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
    RuleDto rule = db.rules().insertRule(r -> r.setStatus(RuleStatus.REMOVED));
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    IssueDto issueDto = IssueTesting.newIssue(rule.getDefinition(), project, file);
    DefaultIssue issue = db.issues().insertIssue(issueDto).setSeverity(MAJOR).toDefaultIssue();
    IssueChangeContext context = IssueChangeContext.createUser(new Date(), "user_uuid");
    issueFieldsSetter.setSeverity(issue, BLOCKER, context);

    SearchResponseData preloadedSearchResponseData = underTest.saveIssueAndPreloadSearchResponseData(db.getSession(), issue, context, null, false);

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
