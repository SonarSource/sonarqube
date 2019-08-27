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
package org.sonar.ce.task.projectanalysis.step;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.notifications.Notification;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.DefaultBranchImpl;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.issue.IssueCache;
import org.sonar.ce.task.projectanalysis.notification.NotificationFactory;
import org.sonar.ce.task.projectanalysis.util.cache.DiskCache;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.issue.notification.DistributedMetricStatsInt;
import org.sonar.server.issue.notification.IssuesChangesNotification;
import org.sonar.server.issue.notification.MyNewIssuesNotification;
import org.sonar.server.issue.notification.NewIssuesNotification;
import org.sonar.server.issue.notification.NewIssuesStatistics;
import org.sonar.server.notification.NotificationService;
import org.sonar.server.project.Project;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.shuffle;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.Component.Type;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.ce.task.projectanalysis.step.SendIssueNotificationsStep.NOTIF_TYPES;
import static org.sonar.db.component.BranchType.LONG;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.db.component.BranchType.SHORT;
import static org.sonar.db.component.ComponentTesting.newBranchDto;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newProjectBranch;
import static org.sonar.db.issue.IssueTesting.newIssue;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;
import static org.sonar.db.rule.RuleTesting.newRule;

public class SendIssueNotificationsStepTest extends BaseStepTest {

  private static final String BRANCH_NAME = "feature";
  private static final String PULL_REQUEST_ID = "pr-123";

  private static final long ANALYSE_DATE = 123L;
  private static final int FIVE_MINUTES_IN_MS = 1000 * 60 * 5;

  private static final Duration ISSUE_DURATION = Duration.create(100L);

  private static final Component FILE = builder(Type.FILE, 11).build();
  private static final Component PROJECT = builder(Type.PROJECT, 1)
    .setProjectVersion(randomAlphanumeric(10))
    .addChildren(FILE).build();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule()
    .setRoot(PROJECT);
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule()
    .setBranch(new DefaultBranchImpl())
    .setAnalysisDate(new Date(ANALYSE_DATE));
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final Random random = new Random();
  private final RuleType[] RULE_TYPES_EXCEPT_HOTSPOTS = Stream.of(RuleType.values()).filter(r -> r != RuleType.SECURITY_HOTSPOT).toArray(RuleType[]::new);
  private final RuleType randomRuleType = RULE_TYPES_EXCEPT_HOTSPOTS[random.nextInt(RULE_TYPES_EXCEPT_HOTSPOTS.length)];
  @SuppressWarnings("unchecked")
  private Class<Map<String, UserDto>> assigneeCacheType = (Class<Map<String, UserDto>>) (Object) Map.class;
  @SuppressWarnings("unchecked")
  private Class<Set<DefaultIssue>> setType = (Class<Set<DefaultIssue>>) (Class<?>) Set.class;
  @SuppressWarnings("unchecked")
  private Class<Map<String, UserDto>> mapType = (Class<Map<String, UserDto>>) (Class<?>) Map.class;
  private ArgumentCaptor<Map<String, UserDto>> assigneeCacheCaptor = ArgumentCaptor.forClass(assigneeCacheType);
  private ArgumentCaptor<Set<DefaultIssue>> issuesSetCaptor = forClass(setType);
  private ArgumentCaptor<Map<String, UserDto>> assigneeByUuidCaptor = forClass(mapType);
  private NotificationService notificationService = mock(NotificationService.class);
  private NotificationFactory notificationFactory = mock(NotificationFactory.class);
  private NewIssuesNotification newIssuesNotificationMock = createNewIssuesNotificationMock();
  private MyNewIssuesNotification myNewIssuesNotificationMock = createMyNewIssuesNotificationMock();

  private IssueCache issueCache;
  private SendIssueNotificationsStep underTest;

  @Before
  public void setUp() throws Exception {
    issueCache = new IssueCache(temp.newFile(), System2.INSTANCE);
    underTest = new SendIssueNotificationsStep(issueCache, treeRootHolder, notificationService, analysisMetadataHolder,
      notificationFactory, db.getDbClient());
    when(notificationFactory.newNewIssuesNotification(any(assigneeCacheType))).thenReturn(newIssuesNotificationMock);
    when(notificationFactory.newMyNewIssuesNotification(any(assigneeCacheType))).thenReturn(myNewIssuesNotificationMock);
  }

  @Test
  public void do_not_send_notifications_if_no_subscribers() {
    analysisMetadataHolder.setProject(new Project(PROJECT.getUuid(), PROJECT.getKey(), PROJECT.getName(), null, emptyList()));
    when(notificationService.hasProjectSubscribersForTypes(PROJECT.getUuid(), NOTIF_TYPES)).thenReturn(false);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    verify(notificationService, never()).deliver(any(Notification.class));
    verify(notificationService, never()).deliverEmails(anyCollection());
    verifyStatistics(context, 0, 0, 0);
  }

  @Test
  public void send_global_new_issues_notification() {
    analysisMetadataHolder.setProject(new Project(PROJECT.getUuid(), PROJECT.getKey(), PROJECT.getName(), null, emptyList()));
    issueCache.newAppender().append(
      new DefaultIssue().setType(randomRuleType).setEffort(ISSUE_DURATION)
        .setCreationDate(new Date(ANALYSE_DATE)))
      .close();
    when(notificationService.hasProjectSubscribersForTypes(eq(PROJECT.getUuid()), any())).thenReturn(true);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    verify(notificationService).deliver(newIssuesNotificationMock);
    verify(newIssuesNotificationMock).setProject(PROJECT.getKey(), PROJECT.getName(), null, null);
    verify(newIssuesNotificationMock).setAnalysisDate(new Date(ANALYSE_DATE));
    verify(newIssuesNotificationMock).setStatistics(eq(PROJECT.getName()), any());
    verify(newIssuesNotificationMock).setDebt(ISSUE_DURATION);
    verifyStatistics(context, 1, 0, 0);
  }

  @Test
  public void send_global_new_issues_notification_only_for_non_backdated_issues() {
    Random random = new Random();
    Integer[] efforts = IntStream.range(0, 1 + random.nextInt(10)).mapToObj(i -> 10_000 * i).toArray(Integer[]::new);
    Integer[] backDatedEfforts = IntStream.range(0, 1 + random.nextInt(10)).mapToObj(i -> 10 + random.nextInt(100)).toArray(Integer[]::new);
    Duration expectedEffort = Duration.create(stream(efforts).mapToInt(i -> i).sum());
    List<DefaultIssue> issues = concat(stream(efforts)
      .map(effort -> new DefaultIssue().setType(randomRuleType).setEffort(Duration.create(effort))
        .setCreationDate(new Date(ANALYSE_DATE))),
      stream(backDatedEfforts)
        .map(effort -> new DefaultIssue().setType(randomRuleType).setEffort(Duration.create(effort))
          .setCreationDate(new Date(ANALYSE_DATE - FIVE_MINUTES_IN_MS))))
            .collect(toList());
    shuffle(issues);
    DiskCache<DefaultIssue>.DiskAppender issueCache = this.issueCache.newAppender();
    issues.forEach(issueCache::append);
    analysisMetadataHolder.setProject(new Project(PROJECT.getUuid(), PROJECT.getKey(), PROJECT.getName(), null, emptyList()));
    when(notificationService.hasProjectSubscribersForTypes(PROJECT.getUuid(), NOTIF_TYPES)).thenReturn(true);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    verify(notificationService).deliver(newIssuesNotificationMock);
    ArgumentCaptor<NewIssuesStatistics.Stats> statsCaptor = forClass(NewIssuesStatistics.Stats.class);
    verify(newIssuesNotificationMock).setStatistics(eq(PROJECT.getName()), statsCaptor.capture());
    verify(newIssuesNotificationMock).setDebt(expectedEffort);
    NewIssuesStatistics.Stats stats = statsCaptor.getValue();
    assertThat(stats.hasIssues()).isTrue();
    // just checking all issues have been added to the stats
    DistributedMetricStatsInt severity = stats.getDistributedMetricStats(NewIssuesStatistics.Metric.RULE_TYPE);
    assertThat(severity.getOnCurrentAnalysis()).isEqualTo(efforts.length);
    assertThat(severity.getTotal()).isEqualTo(backDatedEfforts.length + efforts.length);
    verifyStatistics(context, 1, 0, 0);
  }

  @Test
  public void do_not_send_global_new_issues_notification_if_issue_has_been_backdated() {
    analysisMetadataHolder.setProject(new Project(PROJECT.getUuid(), PROJECT.getKey(), PROJECT.getName(), null, emptyList()));
    issueCache.newAppender().append(
      new DefaultIssue().setType(randomRuleType).setEffort(ISSUE_DURATION)
        .setCreationDate(new Date(ANALYSE_DATE - FIVE_MINUTES_IN_MS)))
      .close();
    when(notificationService.hasProjectSubscribersForTypes(PROJECT.getUuid(), NOTIF_TYPES)).thenReturn(true);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    verify(notificationService, never()).deliver(any(Notification.class));
    verify(notificationService, never()).deliverEmails(anyCollection());
    verifyStatistics(context, 0, 0, 0);
  }

  @Test
  public void send_global_new_issues_notification_on_long_branch() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto branch = setUpBranch(project, LONG);
    issueCache.newAppender().append(
      new DefaultIssue().setType(randomRuleType).setEffort(ISSUE_DURATION).setCreationDate(new Date(ANALYSE_DATE))).close();
    when(notificationService.hasProjectSubscribersForTypes(branch.uuid(), NOTIF_TYPES)).thenReturn(true);
    analysisMetadataHolder.setProject(Project.from(project));
    analysisMetadataHolder.setBranch(newBranch(BranchType.LONG));

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    verify(notificationService).deliver(newIssuesNotificationMock);
    verify(newIssuesNotificationMock).setProject(branch.getKey(), branch.longName(), BRANCH_NAME, null);
    verify(newIssuesNotificationMock).setAnalysisDate(new Date(ANALYSE_DATE));
    verify(newIssuesNotificationMock).setStatistics(eq(branch.longName()), any(NewIssuesStatistics.Stats.class));
    verify(newIssuesNotificationMock).setDebt(ISSUE_DURATION);
    verifyStatistics(context, 1, 0, 0);
  }

  @Test
  public void do_not_send_global_new_issues_notification_on_short_branch() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto branch = setUpBranch(project, SHORT);
    issueCache.newAppender().append(
      new DefaultIssue().setType(randomRuleType).setEffort(ISSUE_DURATION).setCreationDate(new Date(ANALYSE_DATE))).close();
    when(notificationService.hasProjectSubscribersForTypes(project.uuid(), NOTIF_TYPES)).thenReturn(true);
    analysisMetadataHolder.setProject(Project.from(project));
    analysisMetadataHolder.setBranch(newBranch(SHORT));

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    verifyZeroInteractions(notificationService, newIssuesNotificationMock);
  }

  @Test
  public void do_not_send_global_new_issues_notification_on_pull_request() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto branch = setUpBranch(project, PULL_REQUEST);
    issueCache.newAppender().append(
      new DefaultIssue().setType(randomRuleType).setEffort(ISSUE_DURATION).setCreationDate(new Date(ANALYSE_DATE))).close();
    when(notificationService.hasProjectSubscribersForTypes(project.uuid(), NOTIF_TYPES)).thenReturn(true);
    analysisMetadataHolder.setProject(Project.from(project));
    analysisMetadataHolder.setBranch(newPullRequest());
    analysisMetadataHolder.setPullRequestKey(PULL_REQUEST_ID);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    verifyZeroInteractions(notificationService, newIssuesNotificationMock);
  }

  @Test
  public void do_not_send_global_new_issues_notification_on_long_branch_if_issue_has_been_backdated() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto branch = setUpBranch(project, LONG);
    issueCache.newAppender().append(
      new DefaultIssue().setType(randomRuleType).setEffort(ISSUE_DURATION).setCreationDate(new Date(ANALYSE_DATE - FIVE_MINUTES_IN_MS))).close();
    when(notificationService.hasProjectSubscribersForTypes(branch.uuid(), NOTIF_TYPES)).thenReturn(true);
    analysisMetadataHolder.setProject(Project.from(project));
    analysisMetadataHolder.setBranch(newBranch(BranchType.LONG));

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    verify(notificationService, never()).deliver(any(Notification.class));
    verify(notificationService, never()).deliverEmails(anyCollection());
    verifyStatistics(context, 0, 0, 0);
  }

  @Test
  public void send_new_issues_notification_to_user() {
    UserDto user = db.users().insertUser();
    analysisMetadataHolder.setProject(new Project(PROJECT.getUuid(), PROJECT.getKey(), PROJECT.getName(), null, emptyList()));

    issueCache.newAppender().append(
      new DefaultIssue().setType(randomRuleType).setEffort(ISSUE_DURATION).setAssigneeUuid(user.getUuid())
        .setCreationDate(new Date(ANALYSE_DATE)))
      .close();
    when(notificationService.hasProjectSubscribersForTypes(eq(PROJECT.getUuid()), any())).thenReturn(true);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    verify(notificationService).deliverEmails(ImmutableSet.of(newIssuesNotificationMock));
    verify(notificationService).deliverEmails(ImmutableSet.of(myNewIssuesNotificationMock));
    // old API compatibility call
    verify(notificationService).deliver(newIssuesNotificationMock);
    verify(notificationService).deliver(myNewIssuesNotificationMock);
    verify(myNewIssuesNotificationMock).setAssignee(any(UserDto.class));
    verify(myNewIssuesNotificationMock).setProject(PROJECT.getKey(), PROJECT.getName(), null, null);
    verify(myNewIssuesNotificationMock).setAnalysisDate(new Date(ANALYSE_DATE));
    verify(myNewIssuesNotificationMock).setStatistics(eq(PROJECT.getName()), any(NewIssuesStatistics.Stats.class));
    verify(myNewIssuesNotificationMock).setDebt(ISSUE_DURATION);
    verifyStatistics(context, 1, 1, 0);
  }

  @Test
  public void send_new_issues_notification_to_user_only_for_those_assigned_to_her() throws IOException {
    UserDto perceval = db.users().insertUser(u -> u.setLogin("perceval"));
    Integer[] assigned = IntStream.range(0, 5).mapToObj(i -> 10_000 * i).toArray(Integer[]::new);
    Duration expectedEffort = Duration.create(stream(assigned).mapToInt(i -> i).sum());

    UserDto arthur = db.users().insertUser(u -> u.setLogin("arthur"));
    Integer[] assignedToOther = IntStream.range(0, 3).mapToObj(i -> 10).toArray(Integer[]::new);

    List<DefaultIssue> issues = concat(stream(assigned)
      .map(effort -> new DefaultIssue().setType(randomRuleType).setEffort(Duration.create(effort))
        .setAssigneeUuid(perceval.getUuid())
        .setCreationDate(new Date(ANALYSE_DATE))),
      stream(assignedToOther)
        .map(effort -> new DefaultIssue().setType(randomRuleType).setEffort(Duration.create(effort))
          .setAssigneeUuid(arthur.getUuid())
          .setCreationDate(new Date(ANALYSE_DATE))))
            .collect(toList());
    shuffle(issues);
    IssueCache issueCache = new IssueCache(temp.newFile(), System2.INSTANCE);
    DiskCache<DefaultIssue>.DiskAppender newIssueCache = issueCache.newAppender();
    issues.forEach(newIssueCache::append);

    analysisMetadataHolder.setProject(new Project(PROJECT.getUuid(), PROJECT.getKey(), PROJECT.getName(), null, emptyList()));
    when(notificationService.hasProjectSubscribersForTypes(PROJECT.getUuid(), NOTIF_TYPES)).thenReturn(true);

    NotificationFactory notificationFactory = mock(NotificationFactory.class);
    NewIssuesNotification newIssuesNotificationMock = createNewIssuesNotificationMock();
    when(notificationFactory.newNewIssuesNotification(assigneeCacheCaptor.capture()))
      .thenReturn(newIssuesNotificationMock);

    MyNewIssuesNotification myNewIssuesNotificationMock1 = createMyNewIssuesNotificationMock();
    MyNewIssuesNotification myNewIssuesNotificationMock2 = createMyNewIssuesNotificationMock();
    when(notificationFactory.newMyNewIssuesNotification(any(assigneeCacheType)))
      .thenReturn(myNewIssuesNotificationMock1)
      .thenReturn(myNewIssuesNotificationMock2);

    TestComputationStepContext context = new TestComputationStepContext();
    new SendIssueNotificationsStep(issueCache, treeRootHolder, notificationService, analysisMetadataHolder, notificationFactory, db.getDbClient())
      .execute(context);

    verify(notificationService).deliverEmails(ImmutableSet.of(myNewIssuesNotificationMock1, myNewIssuesNotificationMock2));
    // old API compatibility
    verify(notificationService).deliver(myNewIssuesNotificationMock1);
    verify(notificationService).deliver(myNewIssuesNotificationMock2);

    verify(notificationFactory).newNewIssuesNotification(assigneeCacheCaptor.capture());
    verify(notificationFactory, times(2)).newMyNewIssuesNotification(assigneeCacheCaptor.capture());
    verifyNoMoreInteractions(notificationFactory);
    verifyAssigneeCache(assigneeCacheCaptor, perceval, arthur);

    Map<String, MyNewIssuesNotification> myNewIssuesNotificationMocksByUsersName = new HashMap<>();
    ArgumentCaptor<UserDto> userCaptor1 = forClass(UserDto.class);
    verify(myNewIssuesNotificationMock1).setAssignee(userCaptor1.capture());
    myNewIssuesNotificationMocksByUsersName.put(userCaptor1.getValue().getLogin(), myNewIssuesNotificationMock1);

    ArgumentCaptor<UserDto> userCaptor2 = forClass(UserDto.class);
    verify(myNewIssuesNotificationMock2).setAssignee(userCaptor2.capture());
    myNewIssuesNotificationMocksByUsersName.put(userCaptor2.getValue().getLogin(), myNewIssuesNotificationMock2);

    MyNewIssuesNotification myNewIssuesNotificationMock = myNewIssuesNotificationMocksByUsersName.get("perceval");
    ArgumentCaptor<NewIssuesStatistics.Stats> statsCaptor = forClass(NewIssuesStatistics.Stats.class);
    verify(myNewIssuesNotificationMock).setStatistics(eq(PROJECT.getName()), statsCaptor.capture());
    verify(myNewIssuesNotificationMock).setDebt(expectedEffort);

    NewIssuesStatistics.Stats stats = statsCaptor.getValue();
    assertThat(stats.hasIssues()).isTrue();
    // just checking all issues have been added to the stats
    DistributedMetricStatsInt severity = stats.getDistributedMetricStats(NewIssuesStatistics.Metric.RULE_TYPE);
    assertThat(severity.getOnCurrentAnalysis()).isEqualTo(assigned.length);
    assertThat(severity.getTotal()).isEqualTo(assigned.length);

    verifyStatistics(context, 1, 2, 0);
  }

  @Test
  public void send_new_issues_notification_to_user_only_for_non_backdated_issues() {
    UserDto user = db.users().insertUser();
    Random random = new Random();
    Integer[] efforts = IntStream.range(0, 1 + random.nextInt(10)).mapToObj(i -> 10_000 * i).toArray(Integer[]::new);
    Integer[] backDatedEfforts = IntStream.range(0, 1 + random.nextInt(10)).mapToObj(i -> 10 + random.nextInt(100)).toArray(Integer[]::new);
    Duration expectedEffort = Duration.create(stream(efforts).mapToInt(i -> i).sum());
    List<DefaultIssue> issues = concat(stream(efforts)
      .map(effort -> new DefaultIssue().setType(randomRuleType).setEffort(Duration.create(effort))
        .setAssigneeUuid(user.getUuid())
        .setCreationDate(new Date(ANALYSE_DATE))),
      stream(backDatedEfforts)
        .map(effort -> new DefaultIssue().setType(randomRuleType).setEffort(Duration.create(effort))
          .setAssigneeUuid(user.getUuid())
          .setCreationDate(new Date(ANALYSE_DATE - FIVE_MINUTES_IN_MS))))
            .collect(toList());
    shuffle(issues);
    DiskCache<DefaultIssue>.DiskAppender issueCache = this.issueCache.newAppender();
    issues.forEach(issueCache::append);
    analysisMetadataHolder.setProject(new Project(PROJECT.getUuid(), PROJECT.getKey(), PROJECT.getName(), null, emptyList()));
    when(notificationService.hasProjectSubscribersForTypes(PROJECT.getUuid(), NOTIF_TYPES)).thenReturn(true);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    verify(notificationService).deliver(newIssuesNotificationMock);
    verify(notificationService).deliverEmails(ImmutableSet.of(myNewIssuesNotificationMock));
    // old API compatibility
    verify(notificationService).deliver(myNewIssuesNotificationMock);

    verify(notificationFactory).newNewIssuesNotification(assigneeCacheCaptor.capture());
    verify(notificationFactory).newMyNewIssuesNotification(assigneeCacheCaptor.capture());
    verifyNoMoreInteractions(notificationFactory);
    verifyAssigneeCache(assigneeCacheCaptor, user);

    verify(myNewIssuesNotificationMock).setAssignee(any(UserDto.class));
    ArgumentCaptor<NewIssuesStatistics.Stats> statsCaptor = forClass(NewIssuesStatistics.Stats.class);
    verify(myNewIssuesNotificationMock).setStatistics(eq(PROJECT.getName()), statsCaptor.capture());
    verify(myNewIssuesNotificationMock).setDebt(expectedEffort);
    NewIssuesStatistics.Stats stats = statsCaptor.getValue();
    assertThat(stats.hasIssues()).isTrue();
    // just checking all issues have been added to the stats
    DistributedMetricStatsInt severity = stats.getDistributedMetricStats(NewIssuesStatistics.Metric.RULE_TYPE);
    assertThat(severity.getOnCurrentAnalysis()).isEqualTo(efforts.length);
    assertThat(severity.getTotal()).isEqualTo(backDatedEfforts.length + efforts.length);

    verifyStatistics(context, 1, 1, 0);
  }

  private static void verifyAssigneeCache(ArgumentCaptor<Map<String, UserDto>> assigneeCacheCaptor, UserDto... users) {
    Map<String, UserDto> cache = assigneeCacheCaptor.getAllValues().iterator().next();
    assertThat(assigneeCacheCaptor.getAllValues())
      .filteredOn(t -> t != cache)
      .isEmpty();
    Tuple[] expected = stream(users).map(user -> tuple(user.getUuid(), user.getUuid(), user.getId(), user.getLogin())).toArray(Tuple[]::new);
    assertThat(cache.entrySet())
      .extracting(t -> t.getKey(), t -> t.getValue().getUuid(), t -> t.getValue().getId(), t -> t.getValue().getLogin())
      .containsOnly(expected);
  }

  @Test
  public void do_not_send_new_issues_notification_to_user_if_issue_is_backdated() {
    analysisMetadataHolder.setProject(new Project(PROJECT.getUuid(), PROJECT.getKey(), PROJECT.getName(), null, emptyList()));
    UserDto user = db.users().insertUser();
    issueCache.newAppender().append(
      new DefaultIssue().setType(randomRuleType).setEffort(ISSUE_DURATION).setAssigneeUuid(user.getUuid())
        .setCreationDate(new Date(ANALYSE_DATE - FIVE_MINUTES_IN_MS)))
      .close();
    when(notificationService.hasProjectSubscribersForTypes(PROJECT.getUuid(), NOTIF_TYPES)).thenReturn(true);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    verify(notificationService, never()).deliver(any(Notification.class));
    verify(notificationService, never()).deliverEmails(anyCollection());
    verifyStatistics(context, 0, 0, 0);
  }

  @Test
  public void send_issues_change_notification() {
    sendIssueChangeNotification(ANALYSE_DATE);
  }

  @Test
  public void dont_send_issues_change_notification_for_hotspot() {
    UserDto user = db.users().insertUser();
    ComponentDto project = newPrivateProjectDto(newOrganizationDto()).setDbKey(PROJECT.getDbKey()).setLongName(PROJECT.getName());
    ComponentDto file = newFileDto(project).setDbKey(FILE.getDbKey()).setLongName(FILE.getName());
    RuleDefinitionDto ruleDefinitionDto = newRule();
    prepareIssue(ANALYSE_DATE, user, project, file, ruleDefinitionDto, RuleType.SECURITY_HOTSPOT);
    analysisMetadataHolder.setProject(new Project(PROJECT.getUuid(), PROJECT.getKey(), PROJECT.getName(), null, emptyList()));
    when(notificationService.hasProjectSubscribersForTypes(PROJECT.getUuid(), NOTIF_TYPES)).thenReturn(true);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    verify(notificationService, never()).deliver(any(Notification.class));
    verify(notificationService, never()).deliverEmails(anyCollection());
    verifyStatistics(context, 0, 0, 0);
  }

  @Test
  public void send_issues_change_notification_even_if_issue_is_backdated() {
    sendIssueChangeNotification(ANALYSE_DATE - FIVE_MINUTES_IN_MS);
  }

  private void sendIssueChangeNotification(long issueCreatedAt) {
    UserDto user = db.users().insertUser();
    ComponentDto project = newPrivateProjectDto(newOrganizationDto()).setDbKey(PROJECT.getDbKey()).setLongName(PROJECT.getName());
    analysisMetadataHolder.setProject(Project.from(project));
    ComponentDto file = newFileDto(project).setDbKey(FILE.getDbKey()).setLongName(FILE.getName());
    treeRootHolder.setRoot(builder(Type.PROJECT, 2).setKey(project.getDbKey()).setPublicKey(project.getKey()).setName(project.longName()).setUuid(project.uuid())
      .addChildren(
        builder(Type.FILE, 11).setKey(file.getDbKey()).setPublicKey(file.getKey()).setName(file.longName()).build())
      .build());
    RuleDefinitionDto ruleDefinitionDto = newRule();
    RuleType randomTypeExceptHotspot = RuleType.values()[nextInt(RuleType.values().length - 1)];
    DefaultIssue issue = prepareIssue(issueCreatedAt, user, project, file, ruleDefinitionDto, randomTypeExceptHotspot);
    IssuesChangesNotification issuesChangesNotification = mock(IssuesChangesNotification.class);
    when(notificationService.hasProjectSubscribersForTypes(project.uuid(), NOTIF_TYPES)).thenReturn(true);
    when(notificationFactory.newIssuesChangesNotification(anySet(), anyMap())).thenReturn(issuesChangesNotification);

    underTest.execute(new TestComputationStepContext());

    verify(notificationFactory).newIssuesChangesNotification(issuesSetCaptor.capture(), assigneeByUuidCaptor.capture());
    assertThat(issuesSetCaptor.getValue()).hasSize(1);
    assertThat(issuesSetCaptor.getValue().iterator().next()).isEqualTo(issue);
    assertThat(assigneeByUuidCaptor.getValue()).hasSize(1);
    assertThat(assigneeByUuidCaptor.getValue().get(user.getUuid())).isNotNull();
    verify(notificationService).hasProjectSubscribersForTypes(project.uuid(), NOTIF_TYPES);
    verify(notificationService).deliverEmails(singleton(issuesChangesNotification));
    verify(notificationService).deliver(issuesChangesNotification);
    verifyNoMoreInteractions(notificationService);
  }

  private DefaultIssue prepareIssue(long issueCreatedAt, UserDto user, ComponentDto project, ComponentDto file, RuleDefinitionDto ruleDefinitionDto, RuleType type) {
    DefaultIssue issue = newIssue(ruleDefinitionDto, project, file).setType(type).toDefaultIssue()
      .setNew(false).setChanged(true).setSendNotifications(true).setCreationDate(new Date(issueCreatedAt)).setAssigneeUuid(user.getUuid());
    issueCache.newAppender().append(issue).close();
    when(notificationService.hasProjectSubscribersForTypes(project.projectUuid(), NOTIF_TYPES)).thenReturn(true);
    return issue;
  }

  @Test
  public void send_issues_change_notification_on_long_branch() {
    sendIssueChangeNotificationOnLongBranch(ANALYSE_DATE);
  }

  @Test
  public void send_issues_change_notification_on_long_branch_even_if_issue_is_backdated() {
    sendIssueChangeNotificationOnLongBranch(ANALYSE_DATE - FIVE_MINUTES_IN_MS);
  }

  private void sendIssueChangeNotificationOnLongBranch(long issueCreatedAt) {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto branch = newProjectBranch(project, newBranchDto(project).setKey(BRANCH_NAME));
    ComponentDto file = newFileDto(branch);
    treeRootHolder.setRoot(builder(Type.PROJECT, 2).setKey(branch.getDbKey()).setPublicKey(branch.getKey()).setName(branch.longName()).setUuid(branch.uuid()).addChildren(
      builder(Type.FILE, 11).setKey(file.getDbKey()).setPublicKey(file.getKey()).setName(file.longName()).build()).build());
    analysisMetadataHolder.setProject(Project.from(project));
    RuleDefinitionDto ruleDefinitionDto = newRule();
    RuleType randomTypeExceptHotspot = RuleType.values()[nextInt(RuleType.values().length - 1)];
    DefaultIssue issue = newIssue(ruleDefinitionDto, branch, file).setType(randomTypeExceptHotspot).toDefaultIssue()
      .setNew(false)
      .setChanged(true)
      .setSendNotifications(true)
      .setCreationDate(new Date(issueCreatedAt));
    issueCache.newAppender().append(issue).close();
    when(notificationService.hasProjectSubscribersForTypes(project.uuid(), NOTIF_TYPES)).thenReturn(true);
    IssuesChangesNotification issuesChangesNotification = mock(IssuesChangesNotification.class);
    when(notificationFactory.newIssuesChangesNotification(anySet(), anyMap())).thenReturn(issuesChangesNotification);
    analysisMetadataHolder.setBranch(newBranch(BranchType.LONG));

    underTest.execute(new TestComputationStepContext());

    verify(notificationFactory).newIssuesChangesNotification(issuesSetCaptor.capture(), assigneeByUuidCaptor.capture());
    assertThat(issuesSetCaptor.getValue()).hasSize(1);
    assertThat(issuesSetCaptor.getValue().iterator().next()).isEqualTo(issue);
    assertThat(assigneeByUuidCaptor.getValue()).isEmpty();
    verify(notificationService).hasProjectSubscribersForTypes(project.uuid(), NOTIF_TYPES);
    verify(notificationService).deliverEmails(singleton(issuesChangesNotification));
    verify(notificationService).deliver(issuesChangesNotification);
    verifyNoMoreInteractions(notificationService);
  }

  @Test
  public void sends_one_issue_change_notification_every_1000_issues() {
    UserDto user = db.users().insertUser();
    ComponentDto project = newPrivateProjectDto(newOrganizationDto()).setDbKey(PROJECT.getDbKey()).setLongName(PROJECT.getName());
    ComponentDto file = newFileDto(project).setDbKey(FILE.getDbKey()).setLongName(FILE.getName());
    RuleDefinitionDto ruleDefinitionDto = newRule();
    RuleType randomTypeExceptHotspot = RuleType.values()[nextInt(RuleType.values().length - 1)];
    List<DefaultIssue> issues = IntStream.range(0, 2001 + new Random().nextInt(10))
      .mapToObj(i -> newIssue(ruleDefinitionDto, project, file).setKee("uuid_" + i).setType(randomTypeExceptHotspot).toDefaultIssue()
        .setNew(false).setChanged(true).setSendNotifications(true).setAssigneeUuid(user.getUuid()))
      .collect(toList());
    DiskCache<DefaultIssue>.DiskAppender diskAppender = issueCache.newAppender();
    issues.forEach(diskAppender::append);
    diskAppender.close();
    analysisMetadataHolder.setProject(Project.from(project));
    NewIssuesFactoryCaptor newIssuesFactoryCaptor = new NewIssuesFactoryCaptor(() -> mock(IssuesChangesNotification.class));
    when(notificationFactory.newIssuesChangesNotification(anySet(), anyMap())).thenAnswer(newIssuesFactoryCaptor);
    when(notificationService.hasProjectSubscribersForTypes(PROJECT.getUuid(), NOTIF_TYPES)).thenReturn(true);
    when(notificationService.hasProjectSubscribersForTypes(project.uuid(), NOTIF_TYPES)).thenReturn(true);

    underTest.execute(new TestComputationStepContext());

    verify(notificationFactory, times(3)).newIssuesChangesNotification(anySet(), anyMap());
    assertThat(newIssuesFactoryCaptor.issuesSetCaptor).hasSize(3);
    assertThat(newIssuesFactoryCaptor.issuesSetCaptor.get(0)).hasSize(1000);
    assertThat(newIssuesFactoryCaptor.issuesSetCaptor.get(1)).hasSize(1000);
    assertThat(newIssuesFactoryCaptor.issuesSetCaptor.get(2)).hasSize(issues.size() - 2000);
    assertThat(newIssuesFactoryCaptor.assigneeCacheCaptor).hasSize(3);
    assertThat(newIssuesFactoryCaptor.assigneeCacheCaptor).containsOnly(newIssuesFactoryCaptor.assigneeCacheCaptor.iterator().next());
    ArgumentCaptor<Collection> collectionCaptor = forClass(Collection.class);
    verify(notificationService, times(3)).deliverEmails(collectionCaptor.capture());
    assertThat(collectionCaptor.getAllValues()).hasSize(3);
    assertThat(collectionCaptor.getAllValues().get(0)).hasSize(1);
    assertThat(collectionCaptor.getAllValues().get(1)).hasSize(1);
    assertThat(collectionCaptor.getAllValues().get(2)).hasSize(1);
    verify(notificationService, times(3)).deliver(any(IssuesChangesNotification.class));
  }

  /**
   * Since the very same Set object is passed to {@link NotificationFactory#newIssuesChangesNotification(Set, Map)} and
   * reset between each call. We must make a copy of each argument to capture what's been passed to the factory.
   * This is of course not supported by Mockito's {@link ArgumentCaptor} and we implement this ourselves with a
   * {@link Answer}.
   */
  private static class NewIssuesFactoryCaptor implements Answer<Object> {
    private final Supplier<IssuesChangesNotification> delegate;
    private final List<Set<DefaultIssue>> issuesSetCaptor = new ArrayList<>();
    private final List<Map<String, UserDto>> assigneeCacheCaptor = new ArrayList<>();

    private NewIssuesFactoryCaptor(Supplier<IssuesChangesNotification> delegate) {
      this.delegate = delegate;
    }

    @Override
    public Object answer(InvocationOnMock t) {
      Set<DefaultIssue> issuesSet = t.getArgument(0);
      Map<String, UserDto> assigneeCatch = t.getArgument(1);
      issuesSetCaptor.add(ImmutableSet.copyOf(issuesSet));
      assigneeCacheCaptor.add(ImmutableMap.copyOf(assigneeCatch));
      return delegate.get();
    }
  }

  private NewIssuesNotification createNewIssuesNotificationMock() {
    NewIssuesNotification notification = mock(NewIssuesNotification.class);
    when(notification.setProject(any(), any(), any(), any())).thenReturn(notification);
    when(notification.setProjectVersion(any())).thenReturn(notification);
    when(notification.setAnalysisDate(any())).thenReturn(notification);
    when(notification.setStatistics(any(), any())).thenReturn(notification);
    when(notification.setDebt(any())).thenReturn(notification);
    return notification;
  }

  private MyNewIssuesNotification createMyNewIssuesNotificationMock() {
    MyNewIssuesNotification notification = mock(MyNewIssuesNotification.class);
    when(notification.setAssignee(any(UserDto.class))).thenReturn(notification);
    when(notification.setProject(any(), any(), any(), any())).thenReturn(notification);
    when(notification.setProjectVersion(any())).thenReturn(notification);
    when(notification.setAnalysisDate(any())).thenReturn(notification);
    when(notification.setStatistics(any(), any())).thenReturn(notification);
    when(notification.setDebt(any())).thenReturn(notification);
    return notification;
  }

  private static Branch newBranch(BranchType type) {
    Branch branch = mock(Branch.class);
    when(branch.isMain()).thenReturn(false);
    when(branch.getName()).thenReturn(BRANCH_NAME);
    when(branch.getType()).thenReturn(type);
    return branch;
  }

  private static Branch newPullRequest() {
    Branch branch = mock(Branch.class);
    when(branch.isMain()).thenReturn(false);
    when(branch.getType()).thenReturn(PULL_REQUEST);
    when(branch.getName()).thenReturn(BRANCH_NAME);
    when(branch.getPullRequestKey()).thenReturn(PULL_REQUEST_ID);
    return branch;
  }

  private ComponentDto setUpBranch(ComponentDto project, BranchType branchType) {
    ComponentDto branch = newProjectBranch(project, newBranchDto(project, branchType).setKey(BRANCH_NAME));
    ComponentDto file = newFileDto(branch);
    treeRootHolder.setRoot(builder(Type.PROJECT, 2).setKey(branch.getDbKey()).setPublicKey(branch.getKey()).setName(branch.longName()).setUuid(branch.uuid()).addChildren(
      builder(Type.FILE, 11).setKey(file.getDbKey()).setPublicKey(file.getKey()).setName(file.longName()).build()).build());
    return branch;
  }

  private static void verifyStatistics(TestComputationStepContext context, int expectedNewIssuesNotifications, int expectedMyNewIssuesNotifications,
    int expectedIssueChangesNotifications) {
    context.getStatistics().assertValue("newIssuesNotifs", expectedNewIssuesNotifications);
    context.getStatistics().assertValue("myNewIssuesNotifs", expectedMyNewIssuesNotifications);
    context.getStatistics().assertValue("changesNotifs", expectedIssueChangesNotifications);
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }
}
