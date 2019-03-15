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

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.DefaultBranchImpl;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.issue.IssueCache;
import org.sonar.ce.task.projectanalysis.issue.RuleRepositoryRule;
import org.sonar.ce.task.projectanalysis.util.cache.DiskCache;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.issue.notification.DistributedMetricStatsInt;
import org.sonar.server.issue.notification.IssueChangeNotification;
import org.sonar.server.issue.notification.MyNewIssuesNotification;
import org.sonar.server.issue.notification.NewIssuesNotification;
import org.sonar.server.issue.notification.NewIssuesNotificationFactory;
import org.sonar.server.issue.notification.NewIssuesStatistics;
import org.sonar.server.notification.NotificationService;

import static java.util.Arrays.stream;
import static java.util.Collections.shuffle;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.Component.Type;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.ce.task.projectanalysis.step.SendIssueNotificationsStep.NOTIF_TYPES;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
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
  public RuleRepositoryRule ruleRepository = new RuleRepositoryRule();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final Random random = new Random();
  private final RuleType[] RULE_TYPES_EXCEPT_HOTSPOTS = Stream.of(RuleType.values()).filter(r -> r != RuleType.SECURITY_HOTSPOT).toArray(RuleType[]::new);
  private final RuleType randomRuleType = RULE_TYPES_EXCEPT_HOTSPOTS[random.nextInt(RULE_TYPES_EXCEPT_HOTSPOTS.length)];
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
      newIssuesNotificationFactory, db.getDbClient());

    when(newIssuesNotificationFactory.newNewIssuesNotification()).thenReturn(newIssuesNotificationMock);
    when(newIssuesNotificationFactory.newMyNewIssuesNotification()).thenReturn(myNewIssuesNotificationMock);
  }

  @Test
  public void do_not_send_notifications_if_no_subscribers() {
    when(notificationService.hasProjectSubscribersForTypes(PROJECT.getUuid(), NOTIF_TYPES)).thenReturn(false);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    verify(notificationService, never()).deliver(any());
    verifyStatistics(context, 0, 0, 0);
  }

  @Test
  public void send_global_new_issues_notification() {
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
    assertThat(severity.getOnLeak()).isEqualTo(efforts.length);
    assertThat(severity.getTotal()).isEqualTo(backDatedEfforts.length + efforts.length);
    verifyStatistics(context, 1, 0, 0);
  }

  @Test
  public void do_not_send_global_new_issues_notification_if_issue_has_been_backdated() {
    issueCache.newAppender().append(
      new DefaultIssue().setType(randomRuleType).setEffort(ISSUE_DURATION)
        .setCreationDate(new Date(ANALYSE_DATE - FIVE_MINUTES_IN_MS)))
      .close();
    when(notificationService.hasProjectSubscribersForTypes(PROJECT.getUuid(), NOTIF_TYPES)).thenReturn(true);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    verify(notificationService, never()).deliver(any());
    verifyStatistics(context, 0, 0, 0);
  }

  @Test
  public void send_global_new_issues_notification_on_branch() {
    ComponentDto branch = setUpProjectWithBranch();
    issueCache.newAppender().append(
      new DefaultIssue().setType(randomRuleType).setEffort(ISSUE_DURATION).setCreationDate(new Date(ANALYSE_DATE))).close();
    when(notificationService.hasProjectSubscribersForTypes(branch.uuid(), NOTIF_TYPES)).thenReturn(true);
    analysisMetadataHolder.setBranch(newBranch());

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
  public void send_global_new_issues_notification_on_pull_request() {
    ComponentDto branch = setUpProjectWithBranch();
    issueCache.newAppender().append(
      new DefaultIssue().setType(randomRuleType).setEffort(ISSUE_DURATION).setCreationDate(new Date(ANALYSE_DATE))).close();
    when(notificationService.hasProjectSubscribersForTypes(branch.uuid(), NOTIF_TYPES)).thenReturn(true);
    analysisMetadataHolder.setBranch(newPullRequest());
    analysisMetadataHolder.setPullRequestKey(PULL_REQUEST_ID);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    verify(notificationService).deliver(newIssuesNotificationMock);
    verify(newIssuesNotificationMock).setProject(branch.getKey(), branch.longName(), null, PULL_REQUEST_ID);
    verify(newIssuesNotificationMock).setAnalysisDate(new Date(ANALYSE_DATE));
    verify(newIssuesNotificationMock).setStatistics(eq(branch.longName()), any(NewIssuesStatistics.Stats.class));
    verify(newIssuesNotificationMock).setDebt(ISSUE_DURATION);
    verifyStatistics(context, 1, 0, 0);
  }

  @Test
  public void do_not_send_global_new_issues_notification_on_branch_if_issue_has_been_backdated() {
    ComponentDto branch = setUpProjectWithBranch();
    issueCache.newAppender().append(
      new DefaultIssue().setType(randomRuleType).setEffort(ISSUE_DURATION).setCreationDate(new Date(ANALYSE_DATE - FIVE_MINUTES_IN_MS))).close();
    when(notificationService.hasProjectSubscribersForTypes(branch.uuid(), NOTIF_TYPES)).thenReturn(true);
    analysisMetadataHolder.setBranch(newBranch());

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    verify(notificationService, never()).deliver(any());
    verifyStatistics(context, 0, 0, 0);
  }

  @Test
  public void send_new_issues_notification_to_user() {
    UserDto user = db.users().insertUser();

    issueCache.newAppender().append(
      new DefaultIssue().setType(randomRuleType).setEffort(ISSUE_DURATION).setAssigneeUuid(user.getUuid())
        .setCreationDate(new Date(ANALYSE_DATE)))
      .close();
    when(notificationService.hasProjectSubscribersForTypes(eq(PROJECT.getUuid()), any())).thenReturn(true);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

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

    when(notificationService.hasProjectSubscribersForTypes(PROJECT.getUuid(), NOTIF_TYPES)).thenReturn(true);

    NewIssuesNotificationFactory newIssuesNotificationFactory = mock(NewIssuesNotificationFactory.class);
    NewIssuesNotification newIssuesNotificationMock = createNewIssuesNotificationMock();
    when(newIssuesNotificationFactory.newNewIssuesNotification()).thenReturn(newIssuesNotificationMock);

    MyNewIssuesNotification myNewIssuesNotificationMock1 = createMyNewIssuesNotificationMock();
    MyNewIssuesNotification myNewIssuesNotificationMock2 = createMyNewIssuesNotificationMock();
    when(newIssuesNotificationFactory.newMyNewIssuesNotification()).thenReturn(myNewIssuesNotificationMock1).thenReturn(myNewIssuesNotificationMock2);

    TestComputationStepContext context = new TestComputationStepContext();
    new SendIssueNotificationsStep(issueCache, ruleRepository, treeRootHolder, notificationService, analysisMetadataHolder, newIssuesNotificationFactory, db.getDbClient())
      .execute(context);

    verify(notificationService).deliver(myNewIssuesNotificationMock1);
    Map<String, MyNewIssuesNotification> myNewIssuesNotificationMocksByUsersName = new HashMap<>();
    ArgumentCaptor<UserDto> userCaptor1 = forClass(UserDto.class);
    verify(myNewIssuesNotificationMock1).setAssignee(userCaptor1.capture());
    myNewIssuesNotificationMocksByUsersName.put(userCaptor1.getValue().getLogin(), myNewIssuesNotificationMock1);

    verify(notificationService).deliver(myNewIssuesNotificationMock2);
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
    assertThat(severity.getOnLeak()).isEqualTo(assigned.length);
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
    when(notificationService.hasProjectSubscribersForTypes(PROJECT.getUuid(), NOTIF_TYPES)).thenReturn(true);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    verify(notificationService).deliver(newIssuesNotificationMock);
    verify(notificationService).deliver(myNewIssuesNotificationMock);
    verify(myNewIssuesNotificationMock).setAssignee(any(UserDto.class));
    ArgumentCaptor<NewIssuesStatistics.Stats> statsCaptor = forClass(NewIssuesStatistics.Stats.class);
    verify(myNewIssuesNotificationMock).setStatistics(eq(PROJECT.getName()), statsCaptor.capture());
    verify(myNewIssuesNotificationMock).setDebt(expectedEffort);
    NewIssuesStatistics.Stats stats = statsCaptor.getValue();
    assertThat(stats.hasIssues()).isTrue();
    // just checking all issues have been added to the stats
    DistributedMetricStatsInt severity = stats.getDistributedMetricStats(NewIssuesStatistics.Metric.RULE_TYPE);
    assertThat(severity.getOnLeak()).isEqualTo(efforts.length);
    assertThat(severity.getTotal()).isEqualTo(backDatedEfforts.length + efforts.length);

    verifyStatistics(context, 1, 1, 0);
  }

  @Test
  public void do_not_send_new_issues_notification_to_user_if_issue_is_backdated() {
    UserDto user = db.users().insertUser();
    issueCache.newAppender().append(
      new DefaultIssue().setType(randomRuleType).setEffort(ISSUE_DURATION).setAssigneeUuid(user.getUuid())
        .setCreationDate(new Date(ANALYSE_DATE - FIVE_MINUTES_IN_MS)))
      .close();
    when(notificationService.hasProjectSubscribersForTypes(PROJECT.getUuid(), NOTIF_TYPES)).thenReturn(true);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    verify(notificationService, never()).deliver(any());
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
    DefaultIssue issue = prepareIssue(ANALYSE_DATE, user, project, file, ruleDefinitionDto, RuleType.SECURITY_HOTSPOT);

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    verify(notificationService, never()).deliver(any());
    verifyStatistics(context, 0, 0, 0);
  }

  @Test
  public void send_issues_change_notification_even_if_issue_is_backdated() {
    sendIssueChangeNotification(ANALYSE_DATE - FIVE_MINUTES_IN_MS);
  }

  private void sendIssueChangeNotification(long issueCreatedAt) {
    UserDto user = db.users().insertUser();
    ComponentDto project = newPrivateProjectDto(newOrganizationDto()).setDbKey(PROJECT.getDbKey()).setLongName(PROJECT.getName());
    ComponentDto file = newFileDto(project).setDbKey(FILE.getDbKey()).setLongName(FILE.getName());
    RuleDefinitionDto ruleDefinitionDto = newRule();
    RuleType randomTypeExceptHotspot = RuleType.values()[nextInt(RuleType.values().length - 1)];
    DefaultIssue issue = prepareIssue(issueCreatedAt, user, project, file, ruleDefinitionDto, randomTypeExceptHotspot);

    underTest.execute(new TestComputationStepContext());

    ArgumentCaptor<IssueChangeNotification> issueChangeNotificationCaptor = forClass(IssueChangeNotification.class);
    verify(notificationService).deliver(issueChangeNotificationCaptor.capture());
    IssueChangeNotification issueChangeNotification = issueChangeNotificationCaptor.getValue();
    assertThat(issueChangeNotification.getFieldValue("key")).isEqualTo(issue.key());
    assertThat(issueChangeNotification.getFieldValue("message")).isEqualTo(issue.message());
    assertThat(issueChangeNotification.getFieldValue("ruleName")).isEqualTo(ruleDefinitionDto.getName());
    assertThat(issueChangeNotification.getFieldValue("projectName")).isEqualTo(project.longName());
    assertThat(issueChangeNotification.getFieldValue("projectKey")).isEqualTo(project.getKey());
    assertThat(issueChangeNotification.getFieldValue("componentKey")).isEqualTo(file.getKey());
    assertThat(issueChangeNotification.getFieldValue("componentName")).isEqualTo(file.longName());
    assertThat(issueChangeNotification.getFieldValue("assignee")).isEqualTo(user.getLogin());
  }

  private DefaultIssue prepareIssue(long issueCreatedAt, UserDto user, ComponentDto project, ComponentDto file, RuleDefinitionDto ruleDefinitionDto, RuleType type) {
    DefaultIssue issue = newIssue(ruleDefinitionDto, project, file).setType(type).toDefaultIssue()
      .setNew(false).setChanged(true).setSendNotifications(true).setCreationDate(new Date(issueCreatedAt)).setAssigneeUuid(user.getUuid());
    ruleRepository.add(ruleDefinitionDto.getKey()).setName(ruleDefinitionDto.getName());
    issueCache.newAppender().append(issue).close();
    when(notificationService.hasProjectSubscribersForTypes(PROJECT.getUuid(), NOTIF_TYPES)).thenReturn(true);
    return issue;
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
    RuleType randomTypeExceptHotspot = RuleType.values()[nextInt(RuleType.values().length - 1)];
    DefaultIssue issue = newIssue(ruleDefinitionDto, branch, file).setType(randomTypeExceptHotspot).toDefaultIssue()
      .setNew(false)
      .setChanged(true)
      .setSendNotifications(true)
      .setCreationDate(new Date(issueCreatedAt));
    ruleRepository.add(ruleDefinitionDto.getKey()).setName(ruleDefinitionDto.getName());
    issueCache.newAppender().append(issue).close();
    when(notificationService.hasProjectSubscribersForTypes(branch.uuid(), NOTIF_TYPES)).thenReturn(true);
    analysisMetadataHolder.setBranch(newBranch());

    underTest.execute(new TestComputationStepContext());

    ArgumentCaptor<IssueChangeNotification> issueChangeNotificationCaptor = forClass(IssueChangeNotification.class);
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

  private static Branch newBranch() {
    Branch branch = mock(Branch.class);
    when(branch.isMain()).thenReturn(false);
    when(branch.getName()).thenReturn(BRANCH_NAME);
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

  private ComponentDto setUpProjectWithBranch() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto branch = newProjectBranch(project, newBranchDto(project).setKey(BRANCH_NAME));
    ComponentDto file = newFileDto(branch);
    treeRootHolder.setRoot(builder(Type.PROJECT, 2).setKey(branch.getDbKey()).setPublicKey(branch.getKey()).setName(branch.longName()).setUuid(branch.uuid()).addChildren(
      builder(Type.FILE, 11).setKey(file.getDbKey()).setPublicKey(file.getKey()).setName(file.longName()).build()).build());
    return branch;
  }

  private static void verifyStatistics(TestComputationStepContext context, int expectedNewIssuesNotifications, int expectedMyNewIssuesNotifications, int expectedIssueChangesNotifications) {
    context.getStatistics().assertValue("newIssuesNotifs", expectedNewIssuesNotifications);
    context.getStatistics().assertValue("myNewIssuesNotifs", expectedMyNewIssuesNotifications);
    context.getStatistics().assertValue("changesNotifs", expectedIssueChangesNotifications);
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }
}
