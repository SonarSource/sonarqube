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
package org.sonar.server.issue.webhook;

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
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.issue.DefaultTransitions;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
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
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDao;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.issue.webhook.IssueChangeWebhook.IssueChange;
import org.sonar.server.permission.index.AuthorizationTypeSupport;
import org.sonar.server.qualitygate.ShortLivingBranchQualityGate;
import org.sonar.server.settings.ProjectConfigurationLoader;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.webhook.Analysis;
import org.sonar.server.webhook.Branch;
import org.sonar.server.webhook.Project;
import org.sonar.server.webhook.ProjectAnalysis;
import org.sonar.server.webhook.QualityGate;
import org.sonar.server.webhook.QualityGate.EvaluationStatus;
import org.sonar.server.webhook.WebHooks;
import org.sonar.server.webhook.WebhookPayload;
import org.sonar.server.webhook.WebhookPayloadFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.CODE_SMELLS_KEY;
import static org.sonar.api.measures.CoreMetrics.VULNERABILITIES_KEY;
import static org.sonar.core.util.stream.MoreCollectors.toArrayList;
import static org.sonar.server.webhook.QualityGate.Operator.GREATER_THAN;

@RunWith(DataProviderRunner.class)
public class IssueChangeWebhookImplTest {
  private static final List<String> OPEN_STATUSES = ImmutableList.of(Issue.STATUS_OPEN, Issue.STATUS_CONFIRMED);
  private static final List<String> NON_OPEN_STATUSES = Issue.STATUSES.stream().filter(OPEN_STATUSES::contains).collect(MoreCollectors.toList());

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public EsTester esTester = new EsTester(new IssueIndexDefinition(new MapSettings().asConfig()));
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private DbClient dbClient = dbTester.getDbClient();

  private Random random = new Random();
  private String randomResolution = Issue.RESOLUTIONS.get(random.nextInt(Issue.RESOLUTIONS.size()));
  private String randomOpenStatus = OPEN_STATUSES.get(random.nextInt(OPEN_STATUSES.size()));
  private String randomNonOpenStatus = NON_OPEN_STATUSES.get(random.nextInt(NON_OPEN_STATUSES.size()));
  private RuleType randomRuleType = RuleType.values()[random.nextInt(RuleType.values().length)];

  private IssueChangeContext scanChangeContext = IssueChangeContext.createScan(new Date());
  private IssueChangeContext userChangeContext = IssueChangeContext.createUser(new Date(), "userLogin");
  private IssueIndexer issueIndexer = new IssueIndexer(esTester.client(), dbTester.getDbClient(), new IssueIteratorFactory(dbTester.getDbClient()));
  private WebHooks webHooks = mock(WebHooks.class);
  private WebhookPayloadFactory webhookPayloadFactory = mock(WebhookPayloadFactory.class);
  private IssueIndex issueIndex = new IssueIndex(esTester.client(), System2.INSTANCE, userSessionRule, new AuthorizationTypeSupport(userSessionRule));
  private DbClient spiedOnDbClient = spy(dbClient);
  private ProjectConfigurationLoader projectConfigurationLoader = mock(ProjectConfigurationLoader.class);
  private IssueChangeWebhookImpl underTest = new IssueChangeWebhookImpl(spiedOnDbClient, webHooks, projectConfigurationLoader, webhookPayloadFactory, issueIndex, System2.INSTANCE);
  private DbClient mockedDbClient = mock(DbClient.class);
  private IssueIndex spiedOnIssueIndex = spy(issueIndex);
  private IssueChangeWebhookImpl mockedUnderTest = new IssueChangeWebhookImpl(mockedDbClient, webHooks, projectConfigurationLoader, webhookPayloadFactory, spiedOnIssueIndex, System2.INSTANCE);

  @Test
  public void on_type_change_has_no_effect_if_SearchResponseData_has_no_issue() {
    mockedUnderTest.onChange(issueChangeData(), new IssueChange(randomRuleType), userChangeContext);

    verifyZeroInteractions(mockedDbClient, webHooks, projectConfigurationLoader, webhookPayloadFactory, spiedOnIssueIndex);
  }

  @Test
  public void on_type_change_has_no_effect_if_scan_IssueChangeContext() {
    mockedUnderTest.onChange(issueChangeData(), new IssueChange(randomRuleType), scanChangeContext);

    verifyZeroInteractions(mockedDbClient, webHooks, projectConfigurationLoader, webhookPayloadFactory, spiedOnIssueIndex);
  }

  @Test
  public void on_transition_change_has_no_effect_if_SearchResponseData_has_no_issue() {
    mockedUnderTest.onChange(issueChangeData(), new IssueChange(randomAlphanumeric(12)), userChangeContext);

    verifyZeroInteractions(mockedDbClient, webHooks, projectConfigurationLoader, webhookPayloadFactory, spiedOnIssueIndex);
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
    reset(mockedDbClient, webHooks, projectConfigurationLoader, webhookPayloadFactory);

    mockedUnderTest.onChange(issueChangeData(newIssueDto(null)), new IssueChange(transitionKey), userChangeContext);

    verifyZeroInteractions(mockedDbClient, webHooks, projectConfigurationLoader, webhookPayloadFactory, spiedOnIssueIndex);
  }

  @Test
  public void on_transition_change_has_no_effect_if_scan_IssueChangeContext() {
    mockedUnderTest.onChange(issueChangeData(newIssueDto(null)), new IssueChange(randomAlphanumeric(12)), scanChangeContext);

    verifyZeroInteractions(mockedDbClient, webHooks, projectConfigurationLoader, webhookPayloadFactory, spiedOnIssueIndex);
  }

  @Test
  public void on_type_and_transition_change_has_no_effect_if_SearchResponseData_has_no_issue() {
    mockedUnderTest.onChange(issueChangeData(), new IssueChange(randomRuleType, randomAlphanumeric(3)), userChangeContext);

    verifyZeroInteractions(mockedDbClient, webHooks, projectConfigurationLoader, webhookPayloadFactory, spiedOnIssueIndex);
  }

  @Test
  public void on_type_and_transition_change_has_no_effect_if_scan_IssueChangeContext() {
    mockedUnderTest.onChange(issueChangeData(), new IssueChange(randomRuleType, randomAlphanumeric(3)), scanChangeContext);

    verifyZeroInteractions(mockedDbClient, webHooks, projectConfigurationLoader, webhookPayloadFactory, spiedOnIssueIndex);
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
    reset(mockedDbClient, webHooks, projectConfigurationLoader, webhookPayloadFactory);

    mockedUnderTest.onChange(issueChangeData(newIssueDto(null)), new IssueChange(randomRuleType, transitionKey), userChangeContext);

    verifyZeroInteractions(mockedDbClient, webHooks, projectConfigurationLoader, webhookPayloadFactory, spiedOnIssueIndex);
  }

  @Test
  @UseDataProvider("validIssueChanges")
  public void call_webhook_for_short_living_branch_of_issue(IssueChange issueChange) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPublicProject(organization);
    ComponentDto branch = dbTester.components().insertProjectBranch(project, branchDto -> branchDto
      .setBranchType(BranchType.SHORT)
      .setKey("foo"));
    SnapshotDto analysis = insertAnalysisTask(branch);
    Configuration configuration = mockLoadProjectConfiguration(branch);
    mockWebhookEnabled(configuration);
    mockPayloadSupplierConsumedByWebhooks();

    Map<String, String> properties = new HashMap<>();
    properties.put("sonar.analysis.test1", randomAlphanumeric(50));
    properties.put("sonar.analysis.test2", randomAlphanumeric(5000));
    insertPropertiesFor(analysis.getUuid(), properties);

    underTest.onChange(issueChangeData(newIssueDto(branch)), issueChange, userChangeContext);

    ProjectAnalysis projectAnalysis = verifyWebhookCalledAndExtractPayloadFactoryArgument(branch, configuration, analysis);
    assertThat(projectAnalysis).isEqualTo(
      new ProjectAnalysis(
        new Project(project.uuid(), project.getKey(), project.name()),
        null,
        new Analysis(analysis.getUuid(), analysis.getCreatedAt()),
        new Branch(false, "foo", Branch.Type.SHORT),
        new QualityGate(
          valueOf(ShortLivingBranchQualityGate.ID),
          ShortLivingBranchQualityGate.NAME,
          QualityGate.Status.OK,
          ImmutableSet.of(
            new QualityGate.Condition(EvaluationStatus.OK, BUGS_KEY, GREATER_THAN, "0", null, false, "0"),
            new QualityGate.Condition(EvaluationStatus.OK, CoreMetrics.VULNERABILITIES_KEY, GREATER_THAN, "0", null, false, "0"),
            new QualityGate.Condition(EvaluationStatus.OK, CODE_SMELLS_KEY, GREATER_THAN, "0", null, false, "0"))),
        null,
        properties));
  }

  @Test
  @UseDataProvider("validIssueChanges")
  public void do_not_retrieve_analysis_nor_call_webhook_if_webhook_are_disabled_for_short_branch(IssueChange issueChange) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPublicProject(organization);
    ComponentDto branch1 = dbTester.components().insertProjectBranch(project, branchDto -> branchDto
      .setBranchType(BranchType.SHORT)
      .setKey("foo"));
    ComponentDto branch2 = dbTester.components().insertProjectBranch(project, branchDto -> branchDto
      .setBranchType(BranchType.SHORT)
      .setKey("bar"));
    SnapshotDto analysis2 = insertAnalysisTask(branch2);
    Configuration configuration1 = mock(Configuration.class);
    Configuration configuration2 = mock(Configuration.class);
    mockLoadProjectConfigurations(
      branch1, configuration1,
      branch2, configuration2);
    mockWebhookDisabled(configuration1);
    mockWebhookEnabled(configuration2);
    mockPayloadSupplierConsumedByWebhooks();
    ImmutableList<IssueDto> issueDtos = ImmutableList.of(newIssueDto(branch1), newIssueDto(branch2));

    SnapshotDao snapshotDaoSpy = spy(dbClient.snapshotDao());
    when(spiedOnDbClient.snapshotDao()).thenReturn(snapshotDaoSpy);
    underTest.onChange(issueChangeData(issueDtos), issueChange, userChangeContext);

    verifyWebhookCalled(branch2, configuration2, analysis2);
    verify(snapshotDaoSpy).selectLastAnalysesByRootComponentUuids(any(DbSession.class), eq(singleton(branch2.uuid())));
  }

  @Test
  @UseDataProvider("validIssueChanges")
  public void do_not_load_project_configuration_nor_analysis_nor_call_webhook_if_there_are_no_short_branch(IssueChange issueChange) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPublicProject(organization);
    ComponentDto longBranch1 = dbTester.components().insertProjectBranch(project, branchDto -> branchDto
      .setBranchType(BranchType.LONG)
      .setKey("foo"));
    ComponentDto longBranch2 = dbTester.components().insertProjectBranch(project, branchDto -> branchDto
      .setBranchType(BranchType.LONG)
      .setKey("bar"));
    ImmutableList<IssueDto> issueDtos = ImmutableList.of(newIssueDto(project), newIssueDto(longBranch1), newIssueDto(longBranch2));

    SnapshotDao snapshotDaoSpy = spy(dbClient.snapshotDao());
    when(spiedOnDbClient.snapshotDao()).thenReturn(snapshotDaoSpy);
    underTest.onChange(issueChangeData(issueDtos), new IssueChange(randomRuleType), userChangeContext);

    verifyZeroInteractions(projectConfigurationLoader, webHooks);
    verify(snapshotDaoSpy, times(0)).selectLastAnalysesByRootComponentUuids(any(DbSession.class), anyCollectionOf(String.class));
  }

  @Test
  @UseDataProvider("validIssueChanges")
  public void do_not_load_analysis_nor_call_webhook_if_there_no_short_branch_with_enabled_webhook(IssueChange issueChange) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPublicProject(organization);
    ComponentDto branch1 = dbTester.components().insertProjectBranch(project, branchDto -> branchDto
      .setBranchType(BranchType.SHORT)
      .setKey("foo"));
    ComponentDto branch2 = dbTester.components().insertProjectBranch(project, branchDto -> branchDto
      .setBranchType(BranchType.SHORT)
      .setKey("bar"));
    Configuration configuration1 = mock(Configuration.class);
    Configuration configuration2 = mock(Configuration.class);
    mockLoadProjectConfigurations(
      branch1, configuration1,
      branch2, configuration2);
    mockWebhookDisabled(configuration1, configuration2);
    mockPayloadSupplierConsumedByWebhooks();
    ImmutableList<IssueDto> issueDtos = ImmutableList.of(newIssueDto(branch1), newIssueDto(branch2));

    SnapshotDao snapshotDaoSpy = spy(dbClient.snapshotDao());
    when(spiedOnDbClient.snapshotDao()).thenReturn(snapshotDaoSpy);
    underTest.onChange(issueChangeData(issueDtos), issueChange, userChangeContext);

    verify(webHooks).isEnabled(configuration1);
    verify(webHooks).isEnabled(configuration2);
    verify(webHooks, times(0)).sendProjectAnalysisUpdate(any(), any(), any());
    verify(snapshotDaoSpy, times(0)).selectLastAnalysesByRootComponentUuids(any(DbSession.class), anyCollectionOf(String.class));
  }

  @Test
  public void compute_QG_ok_if_there_is_no_issue_in_index_ignoring_permissions() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto branch = insertPrivateBranch(organization, BranchType.SHORT);
    SnapshotDto analysis = insertAnalysisTask(branch);
    Configuration configuration = mockLoadProjectConfiguration(branch);
    mockWebhookEnabled(configuration);
    mockPayloadSupplierConsumedByWebhooks();

    underTest.onChange(issueChangeData(newIssueDto(branch)), new IssueChange(randomRuleType), userChangeContext);

    ProjectAnalysis projectAnalysis = verifyWebhookCalledAndExtractPayloadFactoryArgument(branch, configuration, analysis);
    QualityGate qualityGate = projectAnalysis.getQualityGate().get();
    assertThat(qualityGate.getStatus()).isEqualTo(QualityGate.Status.OK);
    assertThat(qualityGate.getConditions())
      .extracting(QualityGate.Condition::getStatus, QualityGate.Condition::getValue)
      .containsOnly(Tuple.tuple(EvaluationStatus.OK, Optional.of("0")));
  }

  @Test
  public void computes_QG_error_if_there_is_one_unresolved_bug_issue_in_index_ignoring_permissions() {
    int unresolvedIssues = 1 + random.nextInt(10);

    computesQGErrorIfThereIsAtLeastOneUnresolvedIssueInIndexOfTypeIgnoringPermissions(
      unresolvedIssues,
      RuleType.BUG,
      Tuple.tuple(BUGS_KEY, EvaluationStatus.ERROR, Optional.of(valueOf(unresolvedIssues))),
      Tuple.tuple(VULNERABILITIES_KEY, EvaluationStatus.OK, Optional.of("0")),
      Tuple.tuple(CODE_SMELLS_KEY, EvaluationStatus.OK, Optional.of("0")));
  }

  @Test
  public void computes_QG_error_if_there_is_one_unresolved_vulnerability_issue_in_index_ignoring_permissions() {
    int unresolvedIssues = 1 + random.nextInt(10);

    computesQGErrorIfThereIsAtLeastOneUnresolvedIssueInIndexOfTypeIgnoringPermissions(
      unresolvedIssues,
      RuleType.VULNERABILITY,
      Tuple.tuple(BUGS_KEY, EvaluationStatus.OK, Optional.of("0")),
      Tuple.tuple(VULNERABILITIES_KEY, EvaluationStatus.ERROR, Optional.of(valueOf(unresolvedIssues))),
      Tuple.tuple(CODE_SMELLS_KEY, EvaluationStatus.OK, Optional.of("0")));
  }

  @Test
  public void computes_QG_error_if_there_is_one_unresolved_codeSmell_issue_in_index_ignoring_permissions() {
    int unresolvedIssues = 1 + random.nextInt(10);

    computesQGErrorIfThereIsAtLeastOneUnresolvedIssueInIndexOfTypeIgnoringPermissions(
      unresolvedIssues,
      RuleType.CODE_SMELL,
      Tuple.tuple(BUGS_KEY, EvaluationStatus.OK, Optional.of("0")),
      Tuple.tuple(VULNERABILITIES_KEY, EvaluationStatus.OK, Optional.of("0")),
      Tuple.tuple(CODE_SMELLS_KEY, EvaluationStatus.ERROR, Optional.of(valueOf(unresolvedIssues))));
  }

  private void computesQGErrorIfThereIsAtLeastOneUnresolvedIssueInIndexOfTypeIgnoringPermissions(
    int unresolvedIssues, RuleType ruleType, Tuple... expectedQGConditions) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto branch = insertPrivateBranch(organization, BranchType.SHORT);
    SnapshotDto analysis = insertAnalysisTask(branch);
    IntStream.range(0, unresolvedIssues).forEach(i -> insertIssue(branch, ruleType, randomOpenStatus, null));
    IntStream.range(0, random.nextInt(10)).forEach(i -> insertIssue(branch, ruleType, randomNonOpenStatus, randomResolution));
    indexIssues(branch);
    Configuration configuration = mockLoadProjectConfiguration(branch);
    mockWebhookEnabled(configuration);
    mockPayloadSupplierConsumedByWebhooks();

    underTest.onChange(issueChangeData(newIssueDto(branch)), new IssueChange(randomRuleType), userChangeContext);

    ProjectAnalysis projectAnalysis = verifyWebhookCalledAndExtractPayloadFactoryArgument(branch, configuration, analysis);
    QualityGate qualityGate = projectAnalysis.getQualityGate().get();
    assertThat(qualityGate.getStatus()).isEqualTo(QualityGate.Status.ERROR);
    assertThat(qualityGate.getConditions())
      .extracting(QualityGate.Condition::getMetricKey, QualityGate.Condition::getStatus, QualityGate.Condition::getValue)
      .containsOnly(expectedQGConditions);
  }

  @Test
  public void computes_QG_error_with_all_failing_conditions() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto branch = insertPrivateBranch(organization, BranchType.SHORT);
    SnapshotDto analysis = insertAnalysisTask(branch);
    int unresolvedBugs = 1 + random.nextInt(10);
    int unresolvedVulnerabilities = 1 + random.nextInt(10);
    int unresolvedCodeSmells = 1 + random.nextInt(10);
    IntStream.range(0, unresolvedBugs).forEach(i -> insertIssue(branch, RuleType.BUG, randomOpenStatus, null));
    IntStream.range(0, unresolvedVulnerabilities).forEach(i -> insertIssue(branch, RuleType.VULNERABILITY, randomOpenStatus, null));
    IntStream.range(0, unresolvedCodeSmells).forEach(i -> insertIssue(branch, RuleType.CODE_SMELL, randomOpenStatus, null));
    indexIssues(branch);
    Configuration configuration = mockLoadProjectConfiguration(branch);
    mockWebhookEnabled(configuration);
    mockPayloadSupplierConsumedByWebhooks();

    underTest.onChange(issueChangeData(newIssueDto(branch)), new IssueChange(randomRuleType), userChangeContext);

    ProjectAnalysis projectAnalysis = verifyWebhookCalledAndExtractPayloadFactoryArgument(branch, configuration, analysis);
    QualityGate qualityGate = projectAnalysis.getQualityGate().get();
    assertThat(qualityGate.getStatus()).isEqualTo(QualityGate.Status.ERROR);
    assertThat(qualityGate.getConditions())
      .extracting(QualityGate.Condition::getMetricKey, QualityGate.Condition::getStatus, QualityGate.Condition::getValue)
      .containsOnly(
        Tuple.tuple(BUGS_KEY, EvaluationStatus.ERROR, Optional.of(valueOf(unresolvedBugs))),
        Tuple.tuple(VULNERABILITIES_KEY, EvaluationStatus.ERROR, Optional.of(valueOf(unresolvedVulnerabilities))),
        Tuple.tuple(CODE_SMELLS_KEY, EvaluationStatus.ERROR, Optional.of(valueOf(unresolvedCodeSmells))));
  }

  @Test
  public void call_webhook_only_once_per_short_branch_with_at_least_one_issue_in_SearchResponseData() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto branch1 = insertPrivateBranch(organization, BranchType.SHORT);
    ComponentDto branch2 = insertPrivateBranch(organization, BranchType.SHORT);
    ComponentDto branch3 = insertPrivateBranch(organization, BranchType.SHORT);
    SnapshotDto analysis1 = insertAnalysisTask(branch1);
    SnapshotDto analysis2 = insertAnalysisTask(branch2);
    SnapshotDto analysis3 = insertAnalysisTask(branch3);
    int issuesBranch1 = 2 + random.nextInt(10);
    int issuesBranch2 = 2 + random.nextInt(10);
    int issuesBranch3 = 2 + random.nextInt(10);
    List<IssueDto> issueDtos = Stream.of(
      IntStream.range(0, issuesBranch1).mapToObj(i -> newIssueDto(branch1)),
      IntStream.range(0, issuesBranch2).mapToObj(i -> newIssueDto(branch2)),
      IntStream.range(0, issuesBranch3).mapToObj(i -> newIssueDto(branch3)))
      .flatMap(s -> s)
      .collect(MoreCollectors.toList());
    Configuration configuration1 = mock(Configuration.class);
    Configuration configuration2 = mock(Configuration.class);
    Configuration configuration3 = mock(Configuration.class);
    mockLoadProjectConfigurations(branch1, configuration1,
      branch2, configuration2,
      branch3, configuration3);
    mockWebhookEnabled(configuration1, configuration2, configuration3);
    mockPayloadSupplierConsumedByWebhooks();

    ComponentDao componentDaoSpy = spy(dbClient.componentDao());
    BranchDao branchDaoSpy = spy(dbClient.branchDao());
    SnapshotDao snapshotDaoSpy = spy(dbClient.snapshotDao());
    when(spiedOnDbClient.componentDao()).thenReturn(componentDaoSpy);
    when(spiedOnDbClient.branchDao()).thenReturn(branchDaoSpy);
    when(spiedOnDbClient.snapshotDao()).thenReturn(snapshotDaoSpy);
    underTest.onChange(issueChangeData(issueDtos), new IssueChange(randomRuleType), userChangeContext);

    verifyWebhookCalled(branch1, configuration1, analysis1);
    verifyWebhookCalled(branch2, configuration2, analysis2);
    verifyWebhookCalled(branch3, configuration3, analysis3);
    extractPayloadFactoryArguments(3);

    Set<String> uuids = ImmutableSet.of(branch1.uuid(), branch2.uuid(), branch3.uuid());
    verify(componentDaoSpy).selectByUuids(any(DbSession.class), eq(uuids));
    verify(branchDaoSpy).selectByUuids(any(DbSession.class), eq(uuids));
    verify(snapshotDaoSpy).selectLastAnalysesByRootComponentUuids(any(DbSession.class), eq(uuids));
    verifyNoMoreInteractions(componentDaoSpy, branchDaoSpy, snapshotDaoSpy);
  }

  @Test
  public void call_webhood_only_for_short_branches() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto shortBranch = insertPrivateBranch(organization, BranchType.SHORT);
    ComponentDto longBranch = insertPrivateBranch(organization, BranchType.LONG);
    SnapshotDto analysis1 = insertAnalysisTask(shortBranch);
    SnapshotDto analysis2 = insertAnalysisTask(longBranch);
    Configuration configuration = mockLoadProjectConfiguration(shortBranch);
    mockWebhookEnabled(configuration);
    mockPayloadSupplierConsumedByWebhooks();

    ComponentDao componentDaoSpy = spy(dbClient.componentDao());
    BranchDao branchDaoSpy = spy(dbClient.branchDao());
    SnapshotDao snapshotDaoSpy = spy(dbClient.snapshotDao());
    when(spiedOnDbClient.componentDao()).thenReturn(componentDaoSpy);
    when(spiedOnDbClient.branchDao()).thenReturn(branchDaoSpy);
    when(spiedOnDbClient.snapshotDao()).thenReturn(snapshotDaoSpy);
    underTest.onChange(
      issueChangeData(asList(newIssueDto(shortBranch), newIssueDto(longBranch))),
      new IssueChange(randomRuleType),
      userChangeContext);

    verifyWebhookCalledAndExtractPayloadFactoryArgument(shortBranch, configuration, analysis1);

    Set<String> uuids = ImmutableSet.of(shortBranch.uuid(), longBranch.uuid());
    verify(componentDaoSpy).selectByUuids(any(DbSession.class), eq(uuids));
    verify(branchDaoSpy).selectByUuids(any(DbSession.class), eq(uuids));
    verify(snapshotDaoSpy).selectLastAnalysesByRootComponentUuids(any(DbSession.class), eq(ImmutableSet.of(shortBranch.uuid())));
    verifyNoMoreInteractions(componentDaoSpy, branchDaoSpy, snapshotDaoSpy);
  }

  @Test
  public void do_not_load_componentDto_from_DB_if_all_are_in_inputData() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto branch1 = insertPrivateBranch(organization, BranchType.SHORT);
    ComponentDto branch2 = insertPrivateBranch(organization, BranchType.SHORT);
    ComponentDto branch3 = insertPrivateBranch(organization, BranchType.SHORT);
    SnapshotDto analysis1 = insertAnalysisTask(branch1);
    SnapshotDto analysis2 = insertAnalysisTask(branch2);
    SnapshotDto analysis3 = insertAnalysisTask(branch3);
    List<IssueDto> issueDtos = asList(newIssueDto(branch1), newIssueDto(branch2), newIssueDto(branch3));
    Configuration configuration1 = mock(Configuration.class);
    Configuration configuration2 = mock(Configuration.class);
    Configuration configuration3 = mock(Configuration.class);
    mockLoadProjectConfigurations(
        branch1, configuration1,
        branch2, configuration2,
        branch3, configuration3);
    mockWebhookEnabled(configuration1, configuration2, configuration3);
    mockPayloadSupplierConsumedByWebhooks();

    ComponentDao componentDaoSpy = spy(dbClient.componentDao());
    when(spiedOnDbClient.componentDao()).thenReturn(componentDaoSpy);
    underTest.onChange(
        issueChangeData(issueDtos, branch1, branch2, branch3),
        new IssueChange(randomRuleType),
        userChangeContext);

    verifyWebhookCalled(branch1, configuration1, analysis1);
    verifyWebhookCalled(branch2, configuration2, analysis2);
    verifyWebhookCalled(branch3, configuration3, analysis3);

    verify(componentDaoSpy, times(0)).selectByUuids(any(DbSession.class), anyCollectionOf(String.class));
    verifyNoMoreInteractions(componentDaoSpy);
  }

  @Test
  public void call_db_only_for_componentDto_not_in_inputData() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto branch1 = insertPrivateBranch(organization, BranchType.SHORT);
    ComponentDto branch2 = insertPrivateBranch(organization, BranchType.SHORT);
    ComponentDto branch3 = insertPrivateBranch(organization, BranchType.SHORT);
    SnapshotDto analysis1 = insertAnalysisTask(branch1);
    SnapshotDto analysis2 = insertAnalysisTask(branch2);
    SnapshotDto analysis3 = insertAnalysisTask(branch3);
    List<IssueDto> issueDtos = asList(newIssueDto(branch1), newIssueDto(branch2), newIssueDto(branch3));
    Configuration configuration1 = mock(Configuration.class);
    Configuration configuration2 = mock(Configuration.class);
    Configuration configuration3 = mock(Configuration.class);
    mockLoadProjectConfigurations(
      branch1, configuration1,
      branch2, configuration2,
      branch3, configuration3);
    mockWebhookEnabled(configuration1, configuration2, configuration3);
    mockPayloadSupplierConsumedByWebhooks();

    ComponentDao componentDaoSpy = spy(dbClient.componentDao());
    BranchDao branchDaoSpy = spy(dbClient.branchDao());
    SnapshotDao snapshotDaoSpy = spy(dbClient.snapshotDao());
    when(spiedOnDbClient.componentDao()).thenReturn(componentDaoSpy);
    when(spiedOnDbClient.branchDao()).thenReturn(branchDaoSpy);
    when(spiedOnDbClient.snapshotDao()).thenReturn(snapshotDaoSpy);
    underTest.onChange(
      issueChangeData(issueDtos, branch1, branch3),
      new IssueChange(randomRuleType),
      userChangeContext);

    verifyWebhookCalled(branch1, configuration1, analysis1);
    verifyWebhookCalled(branch2, configuration2, analysis2);
    verifyWebhookCalled(branch3, configuration3, analysis3);
    extractPayloadFactoryArguments(3);

    Set<String> uuids = ImmutableSet.of(branch1.uuid(), branch2.uuid(), branch3.uuid());
    verify(componentDaoSpy).selectByUuids(any(DbSession.class), eq(ImmutableSet.of(branch2.uuid())));
    verify(branchDaoSpy).selectByUuids(any(DbSession.class), eq(uuids));
    verify(snapshotDaoSpy).selectLastAnalysesByRootComponentUuids(any(DbSession.class), eq(uuids));
    verifyNoMoreInteractions(componentDaoSpy, branchDaoSpy, snapshotDaoSpy);
  }

  @Test
  public void supports_issues_on_files_and_filter_on_short_branches_asap_when_calling_db() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    ComponentDto projectFile = dbTester.components().insertComponent(ComponentTesting.newFileDto(project));
    ComponentDto shortBranch = dbTester.components().insertProjectBranch(project, branchDto -> branchDto
      .setBranchType(BranchType.SHORT)
      .setKey("foo"));
    ComponentDto longBranch = dbTester.components().insertProjectBranch(project, branchDto -> branchDto
      .setBranchType(BranchType.LONG)
      .setKey("bar"));
    ComponentDto shortBranchFile = dbTester.components().insertComponent(ComponentTesting.newFileDto(shortBranch));
    ComponentDto longBranchFile = dbTester.components().insertComponent(ComponentTesting.newFileDto(longBranch));
    SnapshotDto analysis1 = insertAnalysisTask(project);
    SnapshotDto analysis2 = insertAnalysisTask(shortBranch);
    SnapshotDto analysis3 = insertAnalysisTask(longBranch);
    List<IssueDto> issueDtos = asList(
      newIssueDto(projectFile, project),
      newIssueDto(shortBranchFile, shortBranch),
      newIssueDto(longBranchFile, longBranch));
    Configuration configuration = mockLoadProjectConfiguration(shortBranch);
    mockWebhookEnabled(configuration);
    mockPayloadSupplierConsumedByWebhooks();

    ComponentDao componentDaoSpy = spy(dbClient.componentDao());
    BranchDao branchDaoSpy = spy(dbClient.branchDao());
    SnapshotDao snapshotDaoSpy = spy(dbClient.snapshotDao());
    when(spiedOnDbClient.componentDao()).thenReturn(componentDaoSpy);
    when(spiedOnDbClient.branchDao()).thenReturn(branchDaoSpy);
    when(spiedOnDbClient.snapshotDao()).thenReturn(snapshotDaoSpy);
    underTest.onChange(issueChangeData(issueDtos), new IssueChange(randomRuleType), userChangeContext);

    verifyWebhookCalledAndExtractPayloadFactoryArgument(shortBranch, configuration, analysis2);

    Set<String> uuids = ImmutableSet.of(project.uuid(), shortBranch.uuid(), longBranch.uuid());
    verify(componentDaoSpy).selectByUuids(any(DbSession.class), eq(uuids));
    verify(branchDaoSpy).selectByUuids(any(DbSession.class), eq(ImmutableSet.of(shortBranch.uuid(), longBranch.uuid())));
    verify(snapshotDaoSpy).selectLastAnalysesByRootComponentUuids(any(DbSession.class), eq(ImmutableSet.of(shortBranch.uuid())));
    verifyNoMoreInteractions(componentDaoSpy, branchDaoSpy, snapshotDaoSpy);
  }

  private void mockWebhookEnabled(Configuration... configurations) {
    for (Configuration configuration : configurations) {
      when(webHooks.isEnabled(configuration)).thenReturn(true);
    }
  }

  private void mockWebhookDisabled(Configuration... configurations) {
    for (Configuration configuration : configurations) {
      when(webHooks.isEnabled(configuration)).thenReturn(false);
    }
  }

  private Configuration mockLoadProjectConfiguration(ComponentDto shortBranch) {
    Configuration configuration = mock(Configuration.class);
    when(projectConfigurationLoader.loadProjectConfigurations(any(DbSession.class), eq(singleton(shortBranch))))
      .thenReturn(ImmutableMap.of(shortBranch.uuid(), configuration));
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
    when(projectConfigurationLoader.loadProjectConfigurations(any(DbSession.class), eq(components)))
      .thenReturn(result);
  }

  private void mockPayloadSupplierConsumedByWebhooks() {
    doAnswer(invocationOnMock -> {
      Supplier<WebhookPayload> supplier = (Supplier<WebhookPayload>) invocationOnMock.getArguments()[2];
      supplier.get();
      return null;
    }).when(webHooks).sendProjectAnalysisUpdate(any(Configuration.class), any(), any());
  }

  private void insertIssue(ComponentDto branch, RuleType ruleType, String status, @Nullable String resolution) {
    RuleDefinitionDto rule = RuleTesting.newRule();
    dbTester.rules().insert(rule);
    dbTester.commit();
    dbTester.issues().insert(rule, branch, branch, i -> i.setType(ruleType).setStatus(status).setResolution(resolution));
    dbTester.commit();
  }

  private ComponentDto insertPrivateBranch(OrganizationDto organization, BranchType branchType) {
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    return dbTester.components().insertProjectBranch(project, branchDto -> branchDto
      .setBranchType(branchType)
      .setKey("foo"));
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

  private SnapshotDto insertAnalysisTask(ComponentDto branch) {
    return dbTester.components().insertSnapshot(branch);
  }

  private ProjectAnalysis verifyWebhookCalledAndExtractPayloadFactoryArgument(ComponentDto branch, Configuration configuration, SnapshotDto analysis) {
    verifyWebhookCalled(branch, configuration, analysis);

    return extractPayloadFactoryArguments(1).iterator().next();
  }

  private void verifyWebhookCalled(ComponentDto branch, Configuration branchConfiguration, SnapshotDto analysis) {
    verify(webHooks).isEnabled(branchConfiguration);
    ArgumentCaptor<Supplier> supplierCaptor = ArgumentCaptor.forClass(Supplier.class);
    verify(webHooks).sendProjectAnalysisUpdate(
      same(branchConfiguration),
      eq(new WebHooks.Analysis(branch.uuid(), analysis.getUuid(), null)),
      supplierCaptor.capture());
  }

  private List<ProjectAnalysis> extractPayloadFactoryArguments(int time) {
    ArgumentCaptor<ProjectAnalysis> projectAnalysisCaptor = ArgumentCaptor.forClass(ProjectAnalysis.class);
    verify(webhookPayloadFactory, times(time)).create(projectAnalysisCaptor.capture());
    return projectAnalysisCaptor.getAllValues();
  }

  private void indexIssues(ComponentDto branch) {
    issueIndexer.indexOnAnalysis(branch.uuid());
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

  private IssueChangeWebhook.IssueChangeData issueChangeData() {
    return new IssueChangeWebhook.IssueChangeData(emptyList(), emptyList());
  }

  private IssueChangeWebhook.IssueChangeData issueChangeData(IssueDto issueDto) {
    return new IssueChangeWebhook.IssueChangeData(singletonList(issueDto.toDefaultIssue()), emptyList());
  }

  private IssueChangeWebhook.IssueChangeData issueChangeData(Collection<IssueDto> issueDtos, ComponentDto... components) {
    return new IssueChangeWebhook.IssueChangeData(
      issueDtos.stream().map(IssueDto::toDefaultIssue).collect(Collectors.toList()),
      Arrays.stream(components).collect(Collectors.toList()));
  }

  private IssueDto newIssueDto(@Nullable ComponentDto project) {
    return newIssueDto(project, project);
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
