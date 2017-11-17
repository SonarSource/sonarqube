/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.qualitygate.changeevent;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.DefaultTransitions;
import org.sonar.api.issue.Issue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.AnalysisPropertyDto;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDao;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.qualitygate.LiveQualityGateFactory;
import org.sonar.server.qualitygate.changeevent.IssueChangeTrigger.IssueChange;
import org.sonar.server.settings.ProjectConfigurationLoader;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.webhook.WebhookPayloadFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.sonar.core.util.stream.MoreCollectors.toArrayList;
import static org.sonar.db.component.ComponentTesting.newBranchDto;

@RunWith(DataProviderRunner.class)
public class IssueChangeTriggerImplTest {
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private DbClient dbClient = dbTester.getDbClient();

  private Random random = new Random();
  private RuleType randomRuleType = RuleType.values()[random.nextInt(RuleType.values().length)];

  private IssueChangeContext scanChangeContext = IssueChangeContext.createScan(new Date());
  private IssueChangeContext userChangeContext = IssueChangeContext.createUser(new Date(), "userLogin");
  private WebhookPayloadFactory webhookPayloadFactory = mock(WebhookPayloadFactory.class);
  private DbClient spiedOnDbClient = Mockito.spy(dbClient);
  private ProjectConfigurationLoader projectConfigurationLoader = mock(ProjectConfigurationLoader.class);
  private QGChangeEventListeners qgChangeEventListeners = mock(QGChangeEventListeners.class);
  private LiveQualityGateFactory liveQualityGateFactory = mock(LiveQualityGateFactory.class);
  private IssueChangeTriggerImpl underTest = new IssueChangeTriggerImpl(spiedOnDbClient, projectConfigurationLoader, qgChangeEventListeners, liveQualityGateFactory);
  private DbClient mockedDbClient = mock(DbClient.class);
  private IssueChangeTriggerImpl mockedUnderTest = new IssueChangeTriggerImpl(mockedDbClient, projectConfigurationLoader, qgChangeEventListeners, liveQualityGateFactory);

  @Test
  public void on_type_change_has_no_effect_if_SearchResponseData_has_no_issue() {
    mockedUnderTest.onChange(issueChangeData(), new IssueChange(randomRuleType), userChangeContext);

    Mockito.verifyZeroInteractions(mockedDbClient, projectConfigurationLoader, webhookPayloadFactory);
  }

  @Test
  public void on_type_change_has_no_effect_if_scan_IssueChangeContext() {
    mockedUnderTest.onChange(issueChangeData(), new IssueChange(randomRuleType), scanChangeContext);

    Mockito.verifyZeroInteractions(mockedDbClient, projectConfigurationLoader, webhookPayloadFactory);
  }

  @Test
  public void on_transition_change_has_no_effect_if_SearchResponseData_has_no_issue() {
    mockedUnderTest.onChange(issueChangeData(), new IssueChange(randomAlphanumeric(12)), userChangeContext);

    Mockito.verifyZeroInteractions(mockedDbClient, projectConfigurationLoader, webhookPayloadFactory);
  }

  @Test
  public void onTransition_has_no_effect_if_transition_key_is_empty() {
    on_transition_changeHasNoEffectForTransitionKey("");
  }

  @Test
  public void onTransition_has_no_effect_if_transition_key_is_random() {
    on_transition_changeHasNoEffectForTransitionKey(randomAlphanumeric(99));
  }

  @Test
  public void on_transition_change_has_no_effect_if_transition_key_is_ignored_default_transition_key() {
    Set<String> supportedDefaultTransitionKeys = ImmutableSet.of(
      DefaultTransitions.RESOLVE, DefaultTransitions.FALSE_POSITIVE, DefaultTransitions.WONT_FIX, DefaultTransitions.REOPEN);
    DefaultTransitions.ALL.stream()
      .filter(s -> !supportedDefaultTransitionKeys.contains(s))
      .forEach(this::on_transition_changeHasNoEffectForTransitionKey);
  }

  private void on_transition_changeHasNoEffectForTransitionKey(@Nullable String transitionKey) {
    Mockito.reset(mockedDbClient, projectConfigurationLoader, webhookPayloadFactory);

    mockedUnderTest.onChange(issueChangeData(newIssueDto()), new IssueChange(transitionKey), userChangeContext);

    Mockito.verifyZeroInteractions(mockedDbClient, projectConfigurationLoader, webhookPayloadFactory);
  }

  @Test
  public void on_transition_change_has_no_effect_if_scan_IssueChangeContext() {
    mockedUnderTest.onChange(issueChangeData(newIssueDto()), new IssueChange(randomAlphanumeric(12)), scanChangeContext);

    Mockito.verifyZeroInteractions(mockedDbClient, projectConfigurationLoader, webhookPayloadFactory);
  }

  @Test
  public void on_type_and_transition_change_has_no_effect_if_SearchResponseData_has_no_issue() {
    mockedUnderTest.onChange(issueChangeData(), new IssueChange(randomRuleType, randomAlphanumeric(3)), userChangeContext);

    Mockito.verifyZeroInteractions(mockedDbClient, projectConfigurationLoader, webhookPayloadFactory);
  }

  @Test
  public void on_type_and_transition_change_has_no_effect_if_scan_IssueChangeContext() {
    mockedUnderTest.onChange(issueChangeData(), new IssueChange(randomRuleType, randomAlphanumeric(3)), scanChangeContext);

    Mockito.verifyZeroInteractions(mockedDbClient, projectConfigurationLoader, webhookPayloadFactory);
  }

  @Test
  public void on_type_and_transition_has_no_effect_if_transition_key_is_empty() {
    on_type_and_transition_changeHasNoEffectForTransitionKey("");
  }

  @Test
  public void on_type_and_transition_has_no_effect_if_transition_key_is_random() {
    on_type_and_transition_changeHasNoEffectForTransitionKey(randomAlphanumeric(66));
  }

  @Test
  public void on_type_and_transition_has_no_effect_if_transition_key_is_ignored_default_transition_key() {
    Set<String> supportedDefaultTransitionKeys = ImmutableSet.of(
      DefaultTransitions.RESOLVE, DefaultTransitions.FALSE_POSITIVE, DefaultTransitions.WONT_FIX, DefaultTransitions.REOPEN);
    DefaultTransitions.ALL.stream()
      .filter(s -> !supportedDefaultTransitionKeys.contains(s))
      .forEach(this::on_type_and_transition_changeHasNoEffectForTransitionKey);
  }

  private void on_type_and_transition_changeHasNoEffectForTransitionKey(@Nullable String transitionKey) {
    Mockito.reset(mockedDbClient, projectConfigurationLoader, webhookPayloadFactory);

    mockedUnderTest.onChange(issueChangeData(newIssueDto()), new IssueChange(randomRuleType, transitionKey), userChangeContext);

    Mockito.verifyZeroInteractions(mockedDbClient, projectConfigurationLoader, webhookPayloadFactory);
  }

  @Test
  @UseDataProvider("validIssueChanges")
  public void broadcast_to_QGEventListeners_for_short_living_branch_of_issue(IssueChange issueChange) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPublicProject(organization);
    ComponentAndBranch branch = insertProjectBranch(project, BranchType.SHORT, "foo");
    SnapshotDto analysis = insertAnalysisTask(branch);
    Configuration configuration = mockLoadProjectConfiguration(branch);

    Map<String, String> properties = new HashMap<>();
    properties.put("sonar.analysis.test1", randomAlphanumeric(50));
    properties.put("sonar.analysis.test2", randomAlphanumeric(5000));
    insertPropertiesFor(analysis.getUuid(), properties);

    underTest.onChange(issueChangeData(newIssueDto(branch)), issueChange, userChangeContext);

    Collection<QGChangeEvent> events = verifyListenersBroadcastedTo();
    assertThat(events).hasSize(1);
    QGChangeEvent event = events.iterator().next();
    assertThat(event.getProject()).isEqualTo(branch.component);
    assertThat(event.getBranch()).isEqualTo(branch.branch);
    assertThat(event.getAnalysis()).isEqualTo(analysis);
    assertThat(event.getProjectConfiguration()).isSameAs(configuration);
  }

  @Test
  public void do_not_load_project_configuration_nor_analysis_nor_call_webhook_if_there_are_no_short_branch() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPublicProject(organization);
    ComponentAndBranch longBranch1 = insertProjectBranch(project, BranchType.LONG, "foo");
    ComponentAndBranch longBranch2 = insertProjectBranch(project, BranchType.LONG, "bar");
    ImmutableList<IssueDto> issueDtos = ImmutableList.of(newIssueDto(project), newIssueDto(longBranch1), newIssueDto(longBranch2));

    SnapshotDao snapshotDaoSpy = Mockito.spy(dbClient.snapshotDao());
    Mockito.when(spiedOnDbClient.snapshotDao()).thenReturn(snapshotDaoSpy);
    underTest.onChange(issueChangeData(issueDtos), new IssueChange(randomRuleType), userChangeContext);

    Mockito.verifyZeroInteractions(projectConfigurationLoader);
    Mockito.verify(snapshotDaoSpy, Mockito.times(0)).selectLastAnalysesByRootComponentUuids(Matchers.any(DbSession.class), Matchers.anyCollectionOf(String.class));
  }

  @Test
  public void creates_single_QGChangeEvent_per_short_branch_with_at_least_one_issue_in_SearchResponseData() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentAndBranch branch1 = insertPrivateBranch(organization, BranchType.SHORT);
    ComponentAndBranch branch2 = insertPrivateBranch(organization, BranchType.SHORT);
    ComponentAndBranch branch3 = insertPrivateBranch(organization, BranchType.SHORT);
    SnapshotDto analysis1 = insertAnalysisTask(branch1);
    SnapshotDto analysis2 = insertAnalysisTask(branch2);
    SnapshotDto analysis3 = insertAnalysisTask(branch3);
    int issuesBranch1 = 2 + random.nextInt(10);
    int issuesBranch2 = 2 + random.nextInt(10);
    int issuesBranch3 = 2 + random.nextInt(10);
    List<IssueDto> issueDtos = Stream.of(
      IntStream.range(0, issuesBranch1).mapToObj(i -> newIssueDto(branch1.component)),
      IntStream.range(0, issuesBranch2).mapToObj(i -> newIssueDto(branch2.component)),
      IntStream.range(0, issuesBranch3).mapToObj(i -> newIssueDto(branch3.component)))
      .flatMap(s -> s)
      .collect(MoreCollectors.toList());
    Configuration configuration1 = mock(Configuration.class);
    Configuration configuration2 = mock(Configuration.class);
    Configuration configuration3 = mock(Configuration.class);
    mockLoadProjectConfigurations(
      branch1.component, configuration1,
      branch2.component, configuration2,
      branch3.component, configuration3);

    ComponentDao componentDaoSpy = Mockito.spy(dbClient.componentDao());
    BranchDao branchDaoSpy = Mockito.spy(dbClient.branchDao());
    SnapshotDao snapshotDaoSpy = Mockito.spy(dbClient.snapshotDao());
    Mockito.when(spiedOnDbClient.componentDao()).thenReturn(componentDaoSpy);
    Mockito.when(spiedOnDbClient.branchDao()).thenReturn(branchDaoSpy);
    Mockito.when(spiedOnDbClient.snapshotDao()).thenReturn(snapshotDaoSpy);
    underTest.onChange(issueChangeData(issueDtos), new IssueChange(randomRuleType), userChangeContext);

    Collection<QGChangeEvent> qgChangeEvents = verifyListenersBroadcastedTo();
    assertThat(qgChangeEvents)
      .hasSize(3)
      .extracting(QGChangeEvent::getBranch, QGChangeEvent::getProjectConfiguration, QGChangeEvent::getAnalysis)
      .containsOnly(
        tuple(branch1.branch, configuration1, analysis1),
        tuple(branch2.branch, configuration2, analysis2),
        tuple(branch3.branch, configuration3, analysis3));

    // verifyWebhookCalled(branch1, configuration1, analysis1);
    // verifyWebhookCalled(branch2, configuration2, analysis2);
    // verifyWebhookCalled(branch3, configuration3, analysis3);
    // extractPayloadFactoryArguments(3);

    Set<String> uuids = ImmutableSet.of(branch1.uuid(), branch2.uuid(), branch3.uuid());
    Mockito.verify(componentDaoSpy).selectByUuids(Matchers.any(DbSession.class), Matchers.eq(uuids));
    Mockito.verify(branchDaoSpy).selectByUuids(Matchers.any(DbSession.class), Matchers.eq(uuids));
    Mockito.verify(snapshotDaoSpy).selectLastAnalysesByRootComponentUuids(Matchers.any(DbSession.class), Matchers.eq(uuids));
    Mockito.verifyNoMoreInteractions(componentDaoSpy, branchDaoSpy, snapshotDaoSpy);
  }

  @Test
  public void create_QGChangeEvent_only_for_short_branches() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentAndBranch shortBranch = insertPrivateBranch(organization, BranchType.SHORT);
    ComponentAndBranch longBranch = insertPrivateBranch(organization, BranchType.LONG);
    SnapshotDto analysis1 = insertAnalysisTask(shortBranch);
    SnapshotDto analysis2 = insertAnalysisTask(longBranch);
    Configuration configuration = mockLoadProjectConfiguration(shortBranch);

    ComponentDao componentDaoSpy = Mockito.spy(dbClient.componentDao());
    BranchDao branchDaoSpy = Mockito.spy(dbClient.branchDao());
    SnapshotDao snapshotDaoSpy = Mockito.spy(dbClient.snapshotDao());
    Mockito.when(spiedOnDbClient.componentDao()).thenReturn(componentDaoSpy);
    Mockito.when(spiedOnDbClient.branchDao()).thenReturn(branchDaoSpy);
    Mockito.when(spiedOnDbClient.snapshotDao()).thenReturn(snapshotDaoSpy);
    underTest.onChange(
      issueChangeData(asList(newIssueDto(shortBranch), newIssueDto(longBranch))),
      new IssueChange(randomRuleType),
      userChangeContext);

    Collection<QGChangeEvent> qgChangeEvents = verifyListenersBroadcastedTo();
    assertThat(qgChangeEvents)
      .hasSize(1)
      .extracting(QGChangeEvent::getBranch, QGChangeEvent::getProjectConfiguration, QGChangeEvent::getAnalysis)
      .containsOnly(tuple(shortBranch.branch, configuration, analysis1));

    Set<String> uuids = ImmutableSet.of(shortBranch.uuid(), longBranch.uuid());
    Mockito.verify(componentDaoSpy).selectByUuids(Matchers.any(DbSession.class), Matchers.eq(uuids));
    Mockito.verify(branchDaoSpy).selectByUuids(Matchers.any(DbSession.class), Matchers.eq(uuids));
    Mockito.verify(snapshotDaoSpy).selectLastAnalysesByRootComponentUuids(Matchers.any(DbSession.class), Matchers.eq(ImmutableSet.of(shortBranch.uuid())));
    Mockito.verifyNoMoreInteractions(componentDaoSpy, branchDaoSpy, snapshotDaoSpy);
  }

  @Test
  public void do_not_load_componentDto_from_DB_if_all_are_in_inputData() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentAndBranch branch1 = insertPrivateBranch(organization, BranchType.SHORT);
    ComponentAndBranch branch2 = insertPrivateBranch(organization, BranchType.SHORT);
    ComponentAndBranch branch3 = insertPrivateBranch(organization, BranchType.SHORT);
    SnapshotDto analysis1 = insertAnalysisTask(branch1);
    SnapshotDto analysis2 = insertAnalysisTask(branch2);
    SnapshotDto analysis3 = insertAnalysisTask(branch3);
    List<IssueDto> issueDtos = asList(newIssueDto(branch1), newIssueDto(branch2), newIssueDto(branch3));
    Configuration configuration1 = mock(Configuration.class);
    Configuration configuration2 = mock(Configuration.class);
    Configuration configuration3 = mock(Configuration.class);
    mockLoadProjectConfigurations(
      branch1.component, configuration1,
      branch2.component, configuration2,
      branch3.component, configuration3);

    ComponentDao componentDaoSpy = Mockito.spy(dbClient.componentDao());
    Mockito.when(spiedOnDbClient.componentDao()).thenReturn(componentDaoSpy);
    underTest.onChange(
      issueChangeData(issueDtos, branch1, branch2, branch3),
      new IssueChange(randomRuleType),
      userChangeContext);

    Collection<QGChangeEvent> qgChangeEvents = verifyListenersBroadcastedTo();
    assertThat(qgChangeEvents)
      .hasSize(3)
      .extracting(QGChangeEvent::getBranch, QGChangeEvent::getProjectConfiguration, QGChangeEvent::getAnalysis)
      .containsOnly(
        tuple(branch1.branch, configuration1, analysis1),
        tuple(branch2.branch, configuration2, analysis2),
        tuple(branch3.branch, configuration3, analysis3));

    Mockito.verify(componentDaoSpy, Mockito.times(0)).selectByUuids(Matchers.any(DbSession.class), Matchers.anyCollectionOf(String.class));
    Mockito.verifyNoMoreInteractions(componentDaoSpy);
  }

  @Test
  public void call_db_only_for_componentDto_not_in_inputData() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentAndBranch branch1 = insertPrivateBranch(organization, BranchType.SHORT);
    ComponentAndBranch branch2 = insertPrivateBranch(organization, BranchType.SHORT);
    ComponentAndBranch branch3 = insertPrivateBranch(organization, BranchType.SHORT);
    SnapshotDto analysis1 = insertAnalysisTask(branch1);
    SnapshotDto analysis2 = insertAnalysisTask(branch2);
    SnapshotDto analysis3 = insertAnalysisTask(branch3);
    List<IssueDto> issueDtos = asList(newIssueDto(branch1), newIssueDto(branch2), newIssueDto(branch3));
    Configuration configuration1 = mock(Configuration.class);
    Configuration configuration2 = mock(Configuration.class);
    Configuration configuration3 = mock(Configuration.class);
    mockLoadProjectConfigurations(
      branch1.component, configuration1,
      branch2.component, configuration2,
      branch3.component, configuration3);

    ComponentDao componentDaoSpy = Mockito.spy(dbClient.componentDao());
    BranchDao branchDaoSpy = Mockito.spy(dbClient.branchDao());
    SnapshotDao snapshotDaoSpy = Mockito.spy(dbClient.snapshotDao());
    Mockito.when(spiedOnDbClient.componentDao()).thenReturn(componentDaoSpy);
    Mockito.when(spiedOnDbClient.branchDao()).thenReturn(branchDaoSpy);
    Mockito.when(spiedOnDbClient.snapshotDao()).thenReturn(snapshotDaoSpy);
    underTest.onChange(
      issueChangeData(issueDtos, branch1, branch3),
      new IssueChange(randomRuleType),
      userChangeContext);

    assertThat(verifyListenersBroadcastedTo()).hasSize(3);

    Set<String> uuids = ImmutableSet.of(branch1.uuid(), branch2.uuid(), branch3.uuid());
    Mockito.verify(componentDaoSpy).selectByUuids(Matchers.any(DbSession.class), Matchers.eq(ImmutableSet.of(branch2.uuid())));
    Mockito.verify(branchDaoSpy).selectByUuids(Matchers.any(DbSession.class), Matchers.eq(uuids));
    Mockito.verify(snapshotDaoSpy).selectLastAnalysesByRootComponentUuids(Matchers.any(DbSession.class), Matchers.eq(uuids));
    Mockito.verifyNoMoreInteractions(componentDaoSpy, branchDaoSpy, snapshotDaoSpy);
  }

  @Test
  public void supports_issues_on_files_and_filter_on_short_branches_asap_when_calling_db() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    ComponentDto file = dbTester.components().insertComponent(ComponentTesting.newFileDto(project));
    ComponentAndBranch shortBranch = insertProjectBranch(project, BranchType.SHORT, "foo");
    ComponentAndBranch longBranch = insertProjectBranch(project, BranchType.LONG, "bar");
    ComponentDto shortBranchFile = dbTester.components().insertComponent(ComponentTesting.newFileDto(shortBranch.component));
    ComponentDto longBranchFile = dbTester.components().insertComponent(ComponentTesting.newFileDto(longBranch.component));
    SnapshotDto analysis1 = insertAnalysisTask(project);
    SnapshotDto analysis2 = insertAnalysisTask(shortBranch);
    SnapshotDto analysis3 = insertAnalysisTask(longBranch);
    List<IssueDto> issueDtos = asList(
      newIssueDto(file, project),
      newIssueDto(shortBranchFile, shortBranch),
      newIssueDto(longBranchFile, longBranch));
    Configuration configuration = mockLoadProjectConfiguration(shortBranch);

    ComponentDao componentDaoSpy = Mockito.spy(dbClient.componentDao());
    BranchDao branchDaoSpy = Mockito.spy(dbClient.branchDao());
    SnapshotDao snapshotDaoSpy = Mockito.spy(dbClient.snapshotDao());
    Mockito.when(spiedOnDbClient.componentDao()).thenReturn(componentDaoSpy);
    Mockito.when(spiedOnDbClient.branchDao()).thenReturn(branchDaoSpy);
    Mockito.when(spiedOnDbClient.snapshotDao()).thenReturn(snapshotDaoSpy);
    underTest.onChange(issueChangeData(issueDtos), new IssueChange(randomRuleType), userChangeContext);

    Collection<QGChangeEvent> qgChangeEvents = verifyListenersBroadcastedTo();
    assertThat(qgChangeEvents)
      .hasSize(1)
      .extracting(QGChangeEvent::getBranch, QGChangeEvent::getProjectConfiguration, QGChangeEvent::getAnalysis)
      .containsOnly(tuple(shortBranch.branch, configuration, analysis2));

    Set<String> uuids = ImmutableSet.of(project.uuid(), shortBranch.uuid(), longBranch.uuid());
    Mockito.verify(componentDaoSpy).selectByUuids(Matchers.any(DbSession.class), Matchers.eq(uuids));
    Mockito.verify(branchDaoSpy).selectByUuids(Matchers.any(DbSession.class), Matchers.eq(ImmutableSet.of(shortBranch.uuid(), longBranch.uuid())));
    Mockito.verify(snapshotDaoSpy).selectLastAnalysesByRootComponentUuids(Matchers.any(DbSession.class), Matchers.eq(ImmutableSet.of(shortBranch.uuid())));
    Mockito.verifyNoMoreInteractions(componentDaoSpy, branchDaoSpy, snapshotDaoSpy);
  }

  private Configuration mockLoadProjectConfiguration(ComponentAndBranch componentAndBranch) {
    Configuration configuration = mock(Configuration.class);
    Mockito.when(projectConfigurationLoader.loadProjectConfigurations(Matchers.any(DbSession.class), Matchers.eq(singleton(componentAndBranch.component))))
      .thenReturn(ImmutableMap.of(componentAndBranch.uuid(), configuration));
    return configuration;
  }

  private void mockLoadProjectConfigurations(Object... branchesAndConfiguration) {
    checkArgument(branchesAndConfiguration.length % 2 == 0);
    Set<ComponentDto> components = new HashSet<>();
    Map<String, Configuration> result = new HashMap<>();
    for (int i = 0; i < branchesAndConfiguration.length; i++) {
      ComponentDto component = (ComponentDto) branchesAndConfiguration[i++];
      Configuration configuration = (Configuration) branchesAndConfiguration[i];
      components.add(component);
      result.put(component.uuid(), configuration);
    }
    Mockito.when(projectConfigurationLoader.loadProjectConfigurations(Matchers.any(DbSession.class), Matchers.eq(components)))
      .thenReturn(result);
  }

  private ComponentAndBranch insertPrivateBranch(OrganizationDto organization, BranchType branchType) {
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    BranchDto branchDto = newBranchDto(project.projectUuid(), branchType)
      .setKey("foo");
    ComponentDto newComponent = dbTester.components().insertProjectBranch(project, branchDto);
    return new ComponentAndBranch(newComponent, branchDto);
  }

  public ComponentAndBranch insertProjectBranch(ComponentDto project, BranchType type, String branchKey) {
    BranchDto branchDto = newBranchDto(project.projectUuid(), type).setKey(branchKey);
    ComponentDto newComponent = dbTester.components().insertProjectBranch(project, branchDto);
    return new ComponentAndBranch(newComponent, branchDto);
  }

  private static class ComponentAndBranch {
    private final ComponentDto component;

    private final BranchDto branch;

    private ComponentAndBranch(ComponentDto component, BranchDto branch) {
      this.component = component;
      this.branch = branch;
    }

    public ComponentDto getComponent() {
      return component;
    }

    public BranchDto getBranch() {
      return branch;
    }

    public String uuid() {
      return component.uuid();
    }

  }

  private void insertPropertiesFor(String snapshotUuid, Map<String, String> properties) {
    List<AnalysisPropertyDto> analysisProperties = properties.entrySet().stream()
      .map(entry -> new AnalysisPropertyDto()
        .setUuid(UuidFactoryFast.getInstance().create())
        .setSnapshotUuid(snapshotUuid)
        .setKey(entry.getKey())
        .setValue(entry.getValue()))
      .collect(toArrayList(properties.size()));
    dbTester.getDbClient().analysisPropertiesDao().insert(dbTester.getSession(), analysisProperties);
    dbTester.getSession().commit();
  }

  private SnapshotDto insertAnalysisTask(ComponentAndBranch componentAndBranch) {
    return insertAnalysisTask(componentAndBranch.component);
  }

  private SnapshotDto insertAnalysisTask(ComponentDto component) {
    return dbTester.components().insertSnapshot(component);
  }

  private Collection<QGChangeEvent> verifyListenersBroadcastedTo() {
    Class<Collection<QGChangeEvent>> clazz = (Class<Collection<QGChangeEvent>>) (Class) Collection.class;
    ArgumentCaptor<Collection<QGChangeEvent>> supplierCaptor = ArgumentCaptor.forClass(clazz);
    Mockito.verify(qgChangeEventListeners).broadcast(
      Matchers.same(Trigger.ISSUE_CHANGE),
      supplierCaptor.capture());
    return supplierCaptor.getValue();
  }

  @DataProvider
  public static Object[][] validIssueChanges() {
    return new Object[][] {
      {new IssueChange(RuleType.BUG)},
      {new IssueChange(RuleType.VULNERABILITY)},
      {new IssueChange(RuleType.CODE_SMELL)},
      {new IssueChange(DefaultTransitions.RESOLVE)},
      {new IssueChange(RuleType.BUG, DefaultTransitions.RESOLVE)},
      {new IssueChange(RuleType.VULNERABILITY, DefaultTransitions.RESOLVE)},
      {new IssueChange(RuleType.CODE_SMELL, DefaultTransitions.RESOLVE)},
      {new IssueChange(DefaultTransitions.FALSE_POSITIVE)},
      {new IssueChange(RuleType.BUG, DefaultTransitions.FALSE_POSITIVE)},
      {new IssueChange(RuleType.VULNERABILITY, DefaultTransitions.FALSE_POSITIVE)},
      {new IssueChange(RuleType.CODE_SMELL, DefaultTransitions.FALSE_POSITIVE)},
      {new IssueChange(DefaultTransitions.WONT_FIX)},
      {new IssueChange(RuleType.BUG, DefaultTransitions.WONT_FIX)},
      {new IssueChange(RuleType.VULNERABILITY, DefaultTransitions.WONT_FIX)},
      {new IssueChange(RuleType.CODE_SMELL, DefaultTransitions.WONT_FIX)},
      {new IssueChange(DefaultTransitions.REOPEN)},
      {new IssueChange(RuleType.BUG, DefaultTransitions.REOPEN)},
      {new IssueChange(RuleType.VULNERABILITY, DefaultTransitions.REOPEN)},
      {new IssueChange(RuleType.CODE_SMELL, DefaultTransitions.REOPEN)}
    };
  }

  private IssueChangeTrigger.IssueChangeData issueChangeData() {
    return new IssueChangeTrigger.IssueChangeData(emptyList(), emptyList());
  }

  private IssueChangeTrigger.IssueChangeData issueChangeData(IssueDto issueDto) {
    return new IssueChangeTrigger.IssueChangeData(singletonList(issueDto.toDefaultIssue()), emptyList());
  }

  private IssueChangeTrigger.IssueChangeData issueChangeData(Collection<IssueDto> issueDtos, ComponentAndBranch... components) {
    return new IssueChangeTrigger.IssueChangeData(
      issueDtos.stream().map(IssueDto::toDefaultIssue).collect(Collectors.toList()),
      Arrays.stream(components).map(ComponentAndBranch::getComponent).collect(Collectors.toList()));
  }

  private IssueDto newIssueDto(@Nullable ComponentAndBranch projectAndBranch) {
    return projectAndBranch == null ? newIssueDto() : newIssueDto(projectAndBranch.component, projectAndBranch.component);
  }

  private IssueDto newIssueDto(ComponentDto componentDto) {
    return newIssueDto(componentDto, componentDto);
  }

  private IssueDto newIssueDto() {
    return newIssueDto((ComponentDto) null, (ComponentDto) null);
  }

  private IssueDto newIssueDto(@Nullable ComponentDto component, @Nullable ComponentAndBranch componentAndBranch) {
    return newIssueDto(component, componentAndBranch == null ? null : componentAndBranch.component);
  }

  private IssueDto newIssueDto(@Nullable ComponentDto component, @Nullable ComponentDto project) {
    RuleType randomRuleType = RuleType.values()[random.nextInt(RuleType.values().length)];
    String randomStatus = Issue.STATUSES.get(random.nextInt(Issue.STATUSES.size()));
    IssueDto res = new IssueDto()
      .setType(randomRuleType)
      .setStatus(randomStatus)
      .setRuleKey(randomAlphanumeric(3), randomAlphanumeric(4));
    if (component != null) {
      res.setComponent(component);
    }
    if (project != null) {
      res.setProject(project);
    }
    return res;
  }
}
