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
package org.sonar.server.computation.task.projectanalysis.step;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.exceptions.verification.junit.ArgumentsAreDifferent;
import org.sonar.api.notifications.Notification;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.analysis.Branch;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.Component.Type;
import org.sonar.server.computation.task.projectanalysis.component.DefaultBranchImpl;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.issue.IssueCache;
import org.sonar.server.computation.task.projectanalysis.issue.RuleRepositoryRule;
import org.sonar.server.computation.task.step.ComputationStep;
import org.sonar.server.issue.notification.DistributedMetricStatsInt;
import org.sonar.server.issue.notification.IssueChangeNotification;
import org.sonar.server.issue.notification.MyNewIssuesNotification;
import org.sonar.server.issue.notification.NewIssuesNotification;
import org.sonar.server.issue.notification.NewIssuesNotificationFactory;
import org.sonar.server.issue.notification.NewIssuesStatistics;
import org.sonar.server.notification.NotificationService;
import org.sonar.server.util.cache.DiskCache;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newBranchDto;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newProjectBranch;
import static org.sonar.db.issue.IssueTesting.newIssue;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;
import static org.sonar.db.rule.RuleTesting.newRule;
import static org.sonar.server.computation.task.projectanalysis.component.ReportComponent.builder;

public class SendIssueNotificationsStepTest extends BaseStepTest {

  private static final String BRANCH_NAME = "feature";

  private static final long ANALYSE_DATE = 123L;
  private static final int FIVE_MINUTES_IN_MS = 1000 * 60 * 5;

  private static final Duration ISSUE_DURATION = Duration.create(100L);
  private static final String ISSUE_ASSIGNEE = "John";

  private static final Component FILE = builder(Component.Type.FILE, 11).build();
  private static final Component PROJECT = builder(Type.PROJECT, 1)
    .setVersion(RandomStringUtils.randomAlphanumeric(10))
    .addChildren(FILE).build();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule()
    .setRoot(PROJECT);
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule()
    .setBranch(new DefaultBranchImpl())
    .setAnalysisDate(new Date(ANALYSE_DATE));
  @Rule
  public RuleRepositoryRule ruleRepository = new RuleRepositoryRule();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private final Random random = new Random();
  private final RuleType randomRuleType = RuleType.values()[random.nextInt(RuleType.values().length)];
  private NotificationService notificationService = mock(NotificationService.class);
  private NewIssuesNotificationFactory newIssuesNotificationFactory = mock(NewIssuesNotificationFactory.class);
  private NewIssuesNotification newIssuesNotificationMock = createNewIssuesNotificationMock();
  private MyNewIssuesNotification myNewIssuesNotificationMock = createMyNewIssuesNotificationMock();

  private IssueCache issueCache;
  private SendIssueNotificationsStep underTest;

  @Before
  public void setUp() throws Exception {
    issueCache = new IssueCache(temp.newFile(), System2.INSTANCE);
    underTest = new SendIssueNotificationsStep(issueCache, ruleRepository, treeRootHolder, notificationService, analysisMetadataHolder,
      newIssuesNotificationFactory);

    when(newIssuesNotificationFactory.newNewIssuesNotication()).thenReturn(newIssuesNotificationMock);
    when(newIssuesNotificationFactory.newMyNewIssuesNotification()).thenReturn(myNewIssuesNotificationMock);
  }

  @Test
  public void do_not_send_notifications_if_no_subscribers() {
    when(notificationService.hasProjectSubscribersForTypes(PROJECT.getUuid(), SendIssueNotificationsStep.NOTIF_TYPES)).thenReturn(false);

    underTest.execute();

    verify(notificationService, never()).deliver(any(Notification.class));
  }

  @Test
  public void send_global_new_issues_notification() {
    issueCache.newAppender().append(
      new DefaultIssue().setType(randomRuleType).setEffort(ISSUE_DURATION)
        .setCreationDate(new Date(ANALYSE_DATE)))
      .close();
    when(notificationService.hasProjectSubscribersForTypes(eq(PROJECT.getUuid()), any())).thenReturn(true);

    underTest.execute();

    verify(notificationService).deliver(newIssuesNotificationMock);
    verify(newIssuesNotificationMock).setProject(PROJECT.getPublicKey(), PROJECT.getName(), null);
    verify(newIssuesNotificationMock).setAnalysisDate(new Date(ANALYSE_DATE));
    verify(newIssuesNotificationMock).setStatistics(eq(PROJECT.getName()), any());
    verify(newIssuesNotificationMock).setDebt(ISSUE_DURATION);
  }

  @Test
  public void send_global_new_issues_notification_only_for_non_backdated_issues() {
    Random random = new Random();
    Integer[] efforts = IntStream.range(0, 1 + random.nextInt(10)).mapToObj(i -> 10_000 * i).toArray(Integer[]::new);
    Integer[] backDatedEfforts = IntStream.range(0, 1 + random.nextInt(10)).mapToObj(i -> 10 + random.nextInt(100)).toArray(Integer[]::new);
    Duration expectedEffort = Duration.create(Arrays.stream(efforts).mapToInt(i -> i).sum());
    List<DefaultIssue> issues = Stream.concat(Arrays.stream(efforts)
      .map(effort -> new DefaultIssue().setType(randomRuleType).setEffort(Duration.create(effort))
        .setCreationDate(new Date(ANALYSE_DATE))),
      Arrays.stream(backDatedEfforts)
        .map(effort -> new DefaultIssue().setType(randomRuleType).setEffort(Duration.create(effort))
          .setCreationDate(new Date(ANALYSE_DATE - FIVE_MINUTES_IN_MS))))
      .collect(Collectors.toList());
    Collections.shuffle(issues);
    DiskCache<DefaultIssue>.DiskAppender issueCache = this.issueCache.newAppender();
    issues.forEach(issueCache::append);
    when(notificationService.hasProjectSubscribersForTypes(PROJECT.getUuid(), SendIssueNotificationsStep.NOTIF_TYPES)).thenReturn(true);

    underTest.execute();

    verify(notificationService).deliver(newIssuesNotificationMock);
    ArgumentCaptor<NewIssuesStatistics.Stats> statsCaptor = ArgumentCaptor.forClass(NewIssuesStatistics.Stats.class);
    verify(newIssuesNotificationMock).setStatistics(eq(PROJECT.getName()), statsCaptor.capture());
    verify(newIssuesNotificationMock).setDebt(expectedEffort);
    NewIssuesStatistics.Stats stats = statsCaptor.getValue();
    assertThat(stats.hasIssues()).isTrue();
    // just checking all issues have been added to the stats
    DistributedMetricStatsInt severity = stats.getDistributedMetricStats(NewIssuesStatistics.Metric.RULE_TYPE);
    assertThat(severity.getOnLeak()).isEqualTo(efforts.length);
    assertThat(severity.getOffLeak()).isEqualTo(backDatedEfforts.length);
    assertThat(severity.getTotal()).isEqualTo(backDatedEfforts.length + efforts.length);
  }

  @Test
  public void do_not_send_global_new_issues_notification_if_issue_has_been_backdated() {
    issueCache.newAppender().append(
      new DefaultIssue().setType(randomRuleType).setEffort(ISSUE_DURATION)
        .setCreationDate(new Date(ANALYSE_DATE - FIVE_MINUTES_IN_MS)))
      .close();
    when(notificationService.hasProjectSubscribersForTypes(PROJECT.getUuid(), SendIssueNotificationsStep.NOTIF_TYPES)).thenReturn(true);

    underTest.execute();

    verify(notificationService, never()).deliver(any(Notification.class));
  }

  @Test
  public void send_global_new_issues_notification_on_branch() {
    ComponentDto branch = setUpProjectWithBranch();
    issueCache.newAppender().append(
      new DefaultIssue().setType(randomRuleType).setEffort(ISSUE_DURATION).setCreationDate(new Date(ANALYSE_DATE))).close();
    when(notificationService.hasProjectSubscribersForTypes(branch.uuid(), SendIssueNotificationsStep.NOTIF_TYPES)).thenReturn(true);
    analysisMetadataHolder.setBranch(newBranch());

    underTest.execute();

    verify(notificationService).deliver(newIssuesNotificationMock);
    verify(newIssuesNotificationMock).setProject(branch.getKey(), branch.longName(), BRANCH_NAME);
    verify(newIssuesNotificationMock).setAnalysisDate(new Date(ANALYSE_DATE));
    verify(newIssuesNotificationMock).setStatistics(eq(branch.longName()), any(NewIssuesStatistics.Stats.class));
    verify(newIssuesNotificationMock).setDebt(ISSUE_DURATION);
  }

  @Test
  public void do_not_send_global_new_issues_notification_on_branch_if_issue_has_been_backdated() {
    ComponentDto branch = setUpProjectWithBranch();
    issueCache.newAppender().append(
      new DefaultIssue().setType(randomRuleType).setEffort(ISSUE_DURATION).setCreationDate(new Date(ANALYSE_DATE - FIVE_MINUTES_IN_MS))).close();
    when(notificationService.hasProjectSubscribersForTypes(branch.uuid(), SendIssueNotificationsStep.NOTIF_TYPES)).thenReturn(true);
    analysisMetadataHolder.setBranch(newBranch());

    underTest.execute();

    verify(notificationService, never()).deliver(any(Notification.class));
  }

  @Test
  public void send_new_issues_notification_to_user() {
    issueCache.newAppender().append(
      new DefaultIssue().setType(randomRuleType).setEffort(ISSUE_DURATION).setAssignee(ISSUE_ASSIGNEE)
        .setCreationDate(new Date(ANALYSE_DATE)))
      .close();
    when(notificationService.hasProjectSubscribersForTypes(eq(PROJECT.getUuid()), any())).thenReturn(true);

    underTest.execute();

    verify(notificationService).deliver(newIssuesNotificationMock);
    verify(notificationService).deliver(myNewIssuesNotificationMock);
    verify(myNewIssuesNotificationMock).setAssignee(ISSUE_ASSIGNEE);
    verify(myNewIssuesNotificationMock).setProject(PROJECT.getPublicKey(), PROJECT.getName(), null);
    verify(myNewIssuesNotificationMock).setAnalysisDate(new Date(ANALYSE_DATE));
    verify(myNewIssuesNotificationMock).setStatistics(eq(PROJECT.getName()), any(NewIssuesStatistics.Stats.class));
    verify(myNewIssuesNotificationMock).setDebt(ISSUE_DURATION);
  }

  @Test
  public void send_new_issues_notification_to_user_only_for_those_assigned_to_her() {
    Random random = new Random();
    Integer[] assigned = IntStream.range(0, 1 + random.nextInt(10)).mapToObj(i -> 10_000 * i).toArray(Integer[]::new);
    Integer[] assignedToOther = IntStream.range(0, 1 + random.nextInt(10)).mapToObj(i -> 10 + random.nextInt(100)).toArray(Integer[]::new);
    Duration expectedEffort = Duration.create(Arrays.stream(assigned).mapToInt(i -> i).sum());
    String assignee = randomAlphanumeric(5);
    String otherAssignee = randomAlphanumeric(5);
    List<DefaultIssue> issues = Stream.concat(Arrays.stream(assigned)
      .map(effort -> new DefaultIssue().setType(randomRuleType).setEffort(Duration.create(effort))
        .setAssignee(assignee)
        .setCreationDate(new Date(ANALYSE_DATE))),
      Arrays.stream(assignedToOther)
        .map(effort -> new DefaultIssue().setType(randomRuleType).setEffort(Duration.create(effort))
          .setAssignee(otherAssignee)
          .setCreationDate(new Date(ANALYSE_DATE))))
      .collect(Collectors.toList());
    Collections.shuffle(issues);
    DiskCache<DefaultIssue>.DiskAppender issueCache = this.issueCache.newAppender();
    issues.forEach(issueCache::append);
    when(notificationService.hasProjectSubscribersForTypes(PROJECT.getUuid(), SendIssueNotificationsStep.NOTIF_TYPES)).thenReturn(true);
    MyNewIssuesNotification myNewIssuesNotificationMock2 = createMyNewIssuesNotificationMock();
    when(newIssuesNotificationFactory.newMyNewIssuesNotification())
      .thenReturn(myNewIssuesNotificationMock)
      .thenReturn(myNewIssuesNotificationMock2);

    underTest.execute();

    verify(notificationService).deliver(newIssuesNotificationMock);
    verify(notificationService).deliver(myNewIssuesNotificationMock);
    verify(notificationService).deliver(myNewIssuesNotificationMock2);

    MyNewIssuesNotification effectiveMyNewIssuesNotificationMock = this.myNewIssuesNotificationMock;
    try {
      verify(effectiveMyNewIssuesNotificationMock).setAssignee(assignee);
    } catch (ArgumentsAreDifferent e) {
      assertThat(e.getMessage())
        .contains("Wanted:\nmyNewIssuesNotification.setAssignee(\"" + assignee + "\")")
        .contains("Actual invocation has different arguments:\n" +
          "myNewIssuesNotification.setAssignee(\"" + otherAssignee + "\")");
      effectiveMyNewIssuesNotificationMock = myNewIssuesNotificationMock2;
    }
    ArgumentCaptor<NewIssuesStatistics.Stats> statsCaptor = ArgumentCaptor.forClass(NewIssuesStatistics.Stats.class);
    verify(effectiveMyNewIssuesNotificationMock).setStatistics(eq(PROJECT.getName()), statsCaptor.capture());
    verify(effectiveMyNewIssuesNotificationMock).setDebt(expectedEffort);
    NewIssuesStatistics.Stats stats = statsCaptor.getValue();
    assertThat(stats.hasIssues()).isTrue();
    // just checking all issues have been added to the stats
    DistributedMetricStatsInt severity = stats.getDistributedMetricStats(NewIssuesStatistics.Metric.RULE_TYPE);
    assertThat(severity.getOnLeak()).isEqualTo(assigned.length);
    assertThat(severity.getOffLeak()).isEqualTo(0);
    assertThat(severity.getTotal()).isEqualTo(assigned.length);
  }

  @Test
  public void send_new_issues_notification_to_user_only_for_non_backdated_issues() {
    Random random = new Random();
    Integer[] efforts = IntStream.range(0, 1 + random.nextInt(10)).mapToObj(i -> 10_000 * i).toArray(Integer[]::new);
    Integer[] backDatedEfforts = IntStream.range(0, 1 + random.nextInt(10)).mapToObj(i -> 10 + random.nextInt(100)).toArray(Integer[]::new);
    Duration expectedEffort = Duration.create(Arrays.stream(efforts).mapToInt(i -> i).sum());
    List<DefaultIssue> issues = Stream.concat(Arrays.stream(efforts)
      .map(effort -> new DefaultIssue().setType(randomRuleType).setEffort(Duration.create(effort))
        .setAssignee(ISSUE_ASSIGNEE)
        .setCreationDate(new Date(ANALYSE_DATE))),
      Arrays.stream(backDatedEfforts)
        .map(effort -> new DefaultIssue().setType(randomRuleType).setEffort(Duration.create(effort))
          .setAssignee(ISSUE_ASSIGNEE)
          .setCreationDate(new Date(ANALYSE_DATE - FIVE_MINUTES_IN_MS))))
      .collect(Collectors.toList());
    Collections.shuffle(issues);
    DiskCache<DefaultIssue>.DiskAppender issueCache = this.issueCache.newAppender();
    issues.forEach(issueCache::append);
    when(notificationService.hasProjectSubscribersForTypes(PROJECT.getUuid(), SendIssueNotificationsStep.NOTIF_TYPES)).thenReturn(true);

    underTest.execute();

    verify(notificationService).deliver(newIssuesNotificationMock);
    verify(notificationService).deliver(myNewIssuesNotificationMock);
    verify(myNewIssuesNotificationMock).setAssignee(ISSUE_ASSIGNEE);
    ArgumentCaptor<NewIssuesStatistics.Stats> statsCaptor = ArgumentCaptor.forClass(NewIssuesStatistics.Stats.class);
    verify(myNewIssuesNotificationMock).setStatistics(eq(PROJECT.getName()), statsCaptor.capture());
    verify(myNewIssuesNotificationMock).setDebt(expectedEffort);
    NewIssuesStatistics.Stats stats = statsCaptor.getValue();
    assertThat(stats.hasIssues()).isTrue();
    // just checking all issues have been added to the stats
    DistributedMetricStatsInt severity = stats.getDistributedMetricStats(NewIssuesStatistics.Metric.RULE_TYPE);
    assertThat(severity.getOnLeak()).isEqualTo(efforts.length);
    assertThat(severity.getOffLeak()).isEqualTo(backDatedEfforts.length);
    assertThat(severity.getTotal()).isEqualTo(backDatedEfforts.length + efforts.length);
  }

  @Test
  public void do_not_send_new_issues_notification_to_user_if_issue_is_backdated() {
    issueCache.newAppender().append(
      new DefaultIssue().setType(randomRuleType).setEffort(ISSUE_DURATION).setAssignee(ISSUE_ASSIGNEE)
        .setCreationDate(new Date(ANALYSE_DATE - FIVE_MINUTES_IN_MS)))
      .close();
    when(notificationService.hasProjectSubscribersForTypes(PROJECT.getUuid(), SendIssueNotificationsStep.NOTIF_TYPES)).thenReturn(true);

    underTest.execute();

    verify(notificationService, never()).deliver(any(Notification.class));
  }

  @Test
  public void send_issues_change_notification() {
    sendIssueChangeNotification(ANALYSE_DATE);
  }

  @Test
  public void send_issues_change_notification_even_if_issue_is_backdated() {
    sendIssueChangeNotification(ANALYSE_DATE - FIVE_MINUTES_IN_MS);
  }

  private void sendIssueChangeNotification(long issueCreatedAt) {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto()).setDbKey(PROJECT.getKey()).setLongName(PROJECT.getName());
    ComponentDto file = newFileDto(project).setDbKey(FILE.getKey()).setLongName(FILE.getName());
    RuleDefinitionDto ruleDefinitionDto = newRule();
    DefaultIssue issue = newIssue(ruleDefinitionDto, project, file).toDefaultIssue()
      .setNew(false).setChanged(true).setSendNotifications(true).setCreationDate(new Date(issueCreatedAt));
    ruleRepository.add(ruleDefinitionDto.getKey()).setName(ruleDefinitionDto.getName());
    issueCache.newAppender().append(issue).close();
    when(notificationService.hasProjectSubscribersForTypes(PROJECT.getUuid(), SendIssueNotificationsStep.NOTIF_TYPES)).thenReturn(true);

    underTest.execute();

    ArgumentCaptor<IssueChangeNotification> issueChangeNotificationCaptor = ArgumentCaptor.forClass(IssueChangeNotification.class);
    verify(notificationService).deliver(issueChangeNotificationCaptor.capture());
    IssueChangeNotification issueChangeNotification = issueChangeNotificationCaptor.getValue();
    assertThat(issueChangeNotification.getFieldValue("key")).isEqualTo(issue.key());
    assertThat(issueChangeNotification.getFieldValue("assignee")).isEqualTo(issue.assignee());
    assertThat(issueChangeNotification.getFieldValue("message")).isEqualTo(issue.message());
    assertThat(issueChangeNotification.getFieldValue("ruleName")).isEqualTo(ruleDefinitionDto.getName());
    assertThat(issueChangeNotification.getFieldValue("projectName")).isEqualTo(project.longName());
    assertThat(issueChangeNotification.getFieldValue("projectKey")).isEqualTo(project.getKey());
    assertThat(issueChangeNotification.getFieldValue("componentKey")).isEqualTo(file.getKey());
    assertThat(issueChangeNotification.getFieldValue("componentName")).isEqualTo(file.longName());
  }

  @Test
  public void send_issues_change_notification_on_branch() {
    sendIssueChangeNotificationOnBranch(ANALYSE_DATE);
  }

  @Test
  public void send_issues_change_notification_on_branch_even_if_issue_is_backdated() {
    sendIssueChangeNotificationOnBranch(ANALYSE_DATE - FIVE_MINUTES_IN_MS);
  }

  private void sendIssueChangeNotificationOnBranch(long issueCreatedAt) {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto branch = newProjectBranch(project, newBranchDto(project).setKey(BRANCH_NAME));
    ComponentDto file = newFileDto(branch);
    treeRootHolder.setRoot(builder(Type.PROJECT, 2).setKey(branch.getDbKey()).setPublicKey(branch.getKey()).setName(branch.longName()).setUuid(branch.uuid()).addChildren(
      builder(Type.FILE, 11).setKey(file.getDbKey()).setPublicKey(file.getKey()).setName(file.longName()).build()).build());
    RuleDefinitionDto ruleDefinitionDto = newRule();
    DefaultIssue issue = newIssue(ruleDefinitionDto, branch, file).toDefaultIssue()
      .setNew(false)
      .setChanged(true)
      .setSendNotifications(true)
      .setCreationDate(new Date(issueCreatedAt));
    ruleRepository.add(ruleDefinitionDto.getKey()).setName(ruleDefinitionDto.getName());
    issueCache.newAppender().append(issue).close();
    when(notificationService.hasProjectSubscribersForTypes(branch.uuid(), SendIssueNotificationsStep.NOTIF_TYPES)).thenReturn(true);
    analysisMetadataHolder.setBranch(newBranch());

    underTest.execute();

    ArgumentCaptor<IssueChangeNotification> issueChangeNotificationCaptor = ArgumentCaptor.forClass(IssueChangeNotification.class);
    verify(notificationService).deliver(issueChangeNotificationCaptor.capture());
    IssueChangeNotification issueChangeNotification = issueChangeNotificationCaptor.getValue();
    assertThat(issueChangeNotification.getFieldValue("projectName")).isEqualTo(branch.longName());
    assertThat(issueChangeNotification.getFieldValue("projectKey")).isEqualTo(branch.getKey());
    assertThat(issueChangeNotification.getFieldValue("branch")).isEqualTo(BRANCH_NAME);
    assertThat(issueChangeNotification.getFieldValue("componentKey")).isEqualTo(file.getKey());
    assertThat(issueChangeNotification.getFieldValue("componentName")).isEqualTo(file.longName());
  }

  private NewIssuesNotification createNewIssuesNotificationMock() {
    NewIssuesNotification notification = mock(NewIssuesNotification.class);
    when(notification.setProject(any(), any(), any())).thenReturn(notification);
    when(notification.setProjectVersion(any())).thenReturn(notification);
    when(notification.setAnalysisDate(any())).thenReturn(notification);
    when(notification.setStatistics(any(), any())).thenReturn(notification);
    when(notification.setDebt(any())).thenReturn(notification);
    return notification;
  }

  private MyNewIssuesNotification createMyNewIssuesNotificationMock() {
    MyNewIssuesNotification notification = mock(MyNewIssuesNotification.class);
    when(notification.setAssignee(any())).thenReturn(notification);
    when(notification.setProject(any(), any(), any())).thenReturn(notification);
    when(notification.setProjectVersion(any())).thenReturn(notification);
    when(notification.setAnalysisDate(any())).thenReturn(notification);
    when(notification.setStatistics(any(), any())).thenReturn(notification);
    when(notification.setDebt(any())).thenReturn(notification);
    return notification;
  }

  private static Branch newBranch() {
    Branch branch = mock(Branch.class);
    when(branch.isMain()).thenReturn(false);
    when(branch.getName()).thenReturn(BRANCH_NAME);
    return branch;
  }

  private ComponentDto setUpProjectWithBranch() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto branch = newProjectBranch(project, newBranchDto(project).setKey(BRANCH_NAME));
    ComponentDto file = newFileDto(branch);
    treeRootHolder.setRoot(builder(Type.PROJECT, 2).setKey(branch.getDbKey()).setPublicKey(branch.getKey()).setName(branch.longName()).setUuid(branch.uuid()).addChildren(
      builder(Type.FILE, 11).setKey(file.getDbKey()).setPublicKey(file.getKey()).setName(file.longName()).build()).build());
    return branch;
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }
}
