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
package org.sonar.server.webhook;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.issue.Issue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.AnalysisPropertyDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.permission.index.AuthorizationTypeSupport;
import org.sonar.server.qualitygate.Condition;
import org.sonar.server.qualitygate.EvaluatedCondition;
import org.sonar.server.qualitygate.EvaluatedCondition.EvaluationStatus;
import org.sonar.server.qualitygate.EvaluatedQualityGate;
import org.sonar.server.qualitygate.QualityGate;
import org.sonar.server.qualitygate.ShortLivingBranchQualityGate;
import org.sonar.server.qualitygate.changeevent.QGChangeEvent;
import org.sonar.server.qualitygate.changeevent.Trigger;
import org.sonar.server.tester.UserSessionRule;

import static java.lang.String.valueOf;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.sonar.api.measures.CoreMetrics.BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.CODE_SMELLS_KEY;
import static org.sonar.api.measures.CoreMetrics.VULNERABILITIES_KEY;
import static org.sonar.core.util.stream.MoreCollectors.toArrayList;
import static org.sonar.db.component.BranchType.LONG;
import static org.sonar.db.component.ComponentTesting.newBranchDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.server.qualitygate.Condition.Operator.GREATER_THAN;

public class WebhookQGChangeEventListenerTest {
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

  private IssueIndexer issueIndexer = new IssueIndexer(esTester.client(), dbTester.getDbClient(), new IssueIteratorFactory(dbTester.getDbClient()));
  private WebHooks webHooks = mock(WebHooks.class);
  private WebhookPayloadFactory webhookPayloadFactory = mock(WebhookPayloadFactory.class);
  private IssueIndex issueIndex = new IssueIndex(esTester.client(), System2.INSTANCE, userSessionRule, new AuthorizationTypeSupport(userSessionRule));
  private DbClient spiedOnDbClient = Mockito.spy(dbClient);
  private WebhookQGChangeEventListener underTest = new WebhookQGChangeEventListener(webHooks, webhookPayloadFactory, issueIndex, spiedOnDbClient, System2.INSTANCE);
  private DbClient mockedDbClient = mock(DbClient.class);
  private IssueIndex spiedOnIssueIndex = Mockito.spy(issueIndex);
  private WebhookQGChangeEventListener mockedUnderTest = new WebhookQGChangeEventListener(webHooks, webhookPayloadFactory, spiedOnIssueIndex, mockedDbClient, System2.INSTANCE);

  @Test
  public void onChanges_has_no_effect_if_changeEvents_is_empty() {
    mockedUnderTest.onChanges(Trigger.ISSUE_CHANGE, Collections.emptyList());

    verifyZeroInteractions(webHooks, webhookPayloadFactory, spiedOnIssueIndex, mockedDbClient);
  }

  @Test
  public void onChanges_has_no_effect_if_no_webhook_is_configured() {
    Configuration configuration1 = mock(Configuration.class);
    Configuration configuration2 = mock(Configuration.class);
    mockWebhookDisabled(configuration1, configuration2);

    mockedUnderTest.onChanges(Trigger.ISSUE_CHANGE, ImmutableList.of(
      new QGChangeEvent(new ComponentDto(), new BranchDto(), new SnapshotDto(), configuration1),
      new QGChangeEvent(new ComponentDto(), new BranchDto(), new SnapshotDto(), configuration2)));

    verify(webHooks).isEnabled(configuration1);
    verify(webHooks).isEnabled(configuration2);
    verifyZeroInteractions(webhookPayloadFactory, spiedOnIssueIndex, mockedDbClient);
  }

  @Test
  public void onChanges_calls_webhook_for_changeEvent_with_webhook_enabled() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPublicProject(organization);
    ComponentAndBranch branch = insertProjectBranch(project, BranchType.SHORT, "foo");
    SnapshotDto analysis = insertAnalysisTask(branch);
    Configuration configuration = mock(Configuration.class);
    mockWebhookEnabled(configuration);
    mockPayloadSupplierConsumedByWebhooks();
    Map<String, String> properties = new HashMap<>();
    properties.put("sonar.analysis.test1", randomAlphanumeric(50));
    properties.put("sonar.analysis.test2", randomAlphanumeric(5000));
    insertPropertiesFor(analysis.getUuid(), properties);

    underTest.onChanges(Trigger.ISSUE_CHANGE, singletonList(newQGChangeEvent(branch, analysis, configuration)));

    ProjectAnalysis projectAnalysis = verifyWebhookCalledAndExtractPayloadFactoryArgument(branch, configuration, analysis);
    Condition condition1 = new Condition(BUGS_KEY, GREATER_THAN, "0", null, false);
    Condition condition2 = new Condition(VULNERABILITIES_KEY, GREATER_THAN, "0", null, false);
    Condition condition3 = new Condition(CODE_SMELLS_KEY, GREATER_THAN, "0", null, false);
    assertThat(projectAnalysis).isEqualTo(
      new ProjectAnalysis(
        new Project(project.uuid(), project.getKey(), project.name()),
        null,
        new Analysis(analysis.getUuid(), analysis.getCreatedAt()),
        new Branch(false, "foo", Branch.Type.SHORT),
        EvaluatedQualityGate.newBuilder()
          .setQualityGate(
            new QualityGate(
              valueOf(ShortLivingBranchQualityGate.ID),
              ShortLivingBranchQualityGate.NAME,
              ImmutableSet.of(condition1, condition2, condition3)))
          .setStatus(EvaluatedQualityGate.Status.OK)
          .addCondition(condition1, EvaluationStatus.OK, "0")
          .addCondition(condition2, EvaluationStatus.OK, "0")
          .addCondition(condition3, EvaluationStatus.OK, "0")
          .build(),
        null,
        properties));
  }

  @Test
  public void onChanges_does_not_call_webhook_if_disabled_for_QGChangeEvent() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPublicProject(organization);
    ComponentAndBranch branch1 = insertProjectBranch(project, BranchType.SHORT, "foo");
    ComponentAndBranch branch2 = insertProjectBranch(project, BranchType.SHORT, "bar");
    SnapshotDto analysis1 = insertAnalysisTask(branch1);
    SnapshotDto analysis2 = insertAnalysisTask(branch2);
    Configuration configuration1 = mock(Configuration.class);
    Configuration configuration2 = mock(Configuration.class);
    mockWebhookDisabled(configuration1);
    mockWebhookEnabled(configuration2);
    mockPayloadSupplierConsumedByWebhooks();

    underTest.onChanges(
      Trigger.ISSUE_CHANGE,
      ImmutableList.of(
        newQGChangeEvent(branch1, analysis1, configuration1),
        newQGChangeEvent(branch2, analysis2, configuration2)));

    verifyWebhookNotCalled(branch1, analysis1, configuration1);
    verifyWebhookCalled(branch2, analysis2, configuration2);
  }

  @Test
  public void onChanges_calls_webhook_for_any_type_of_branch() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentAndBranch mainBranch = insertMainBranch(organization);
    ComponentAndBranch longBranch = insertProjectBranch(mainBranch.component, BranchType.LONG, "foo");
    SnapshotDto analysis1 = insertAnalysisTask(mainBranch);
    SnapshotDto analysis2 = insertAnalysisTask(longBranch);
    Configuration configuration1 = mock(Configuration.class);
    Configuration configuration2 = mock(Configuration.class);
    mockWebhookEnabled(configuration1, configuration2);

    underTest.onChanges(Trigger.ISSUE_CHANGE, ImmutableList.of(
      newQGChangeEvent(mainBranch, analysis1, configuration1),
      newQGChangeEvent(longBranch, analysis2, configuration2)));

    verifyWebhookCalled(mainBranch, analysis1, configuration1);
    verifyWebhookCalled(longBranch, analysis2, configuration2);
  }

  @Test
  public void onChanges_calls_webhook_once_per_QGChangeEvent_even_for_same_branch_and_configuration() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentAndBranch branch1 = insertPrivateBranch(organization, BranchType.SHORT);
    SnapshotDto analysis1 = insertAnalysisTask(branch1);
    Configuration configuration1 = mock(Configuration.class);
    mockWebhookEnabled(configuration1);
    mockPayloadSupplierConsumedByWebhooks();

    underTest.onChanges(Trigger.ISSUE_CHANGE, ImmutableList.of(
      newQGChangeEvent(branch1, analysis1, configuration1),
      newQGChangeEvent(branch1, analysis1, configuration1),
      newQGChangeEvent(branch1, analysis1, configuration1)));

    verify(webHooks, times(3)).isEnabled(configuration1);
    verify(webHooks, times(3)).sendProjectAnalysisUpdate(
      Matchers.same(configuration1),
      Matchers.eq(new WebHooks.Analysis(branch1.uuid(), analysis1.getUuid(), null)),
      any(Supplier.class));
    extractPayloadFactoryArguments(3);
  }

  @Test
  public void compute_QG_ok_if_there_is_no_issue_in_index_ignoring_permissions() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentAndBranch branch = insertPrivateBranch(organization, BranchType.SHORT);
    SnapshotDto analysis = insertAnalysisTask(branch);
    Configuration configuration = mock(Configuration.class);
    mockWebhookEnabled(configuration);
    mockPayloadSupplierConsumedByWebhooks();

    underTest.onChanges(Trigger.ISSUE_CHANGE, singletonList(newQGChangeEvent(branch, analysis, configuration)));

    ProjectAnalysis projectAnalysis = verifyWebhookCalledAndExtractPayloadFactoryArgument(branch, configuration, analysis);
    EvaluatedQualityGate qualityGate = projectAnalysis.getQualityGate().get();
    assertThat(qualityGate.getStatus()).isEqualTo(EvaluatedQualityGate.Status.OK);
    assertThat(qualityGate.getEvaluatedConditions())
      .extracting(EvaluatedCondition::getStatus, EvaluatedCondition::getValue)
      .containsOnly(tuple(EvaluationStatus.OK, Optional.of("0")));
  }

  @Test
  public void computes_QG_error_if_there_is_one_unresolved_bug_issue_in_index_ignoring_permissions() {
    int unresolvedIssues = 1 + random.nextInt(10);

    computesQGErrorIfThereIsAtLeastOneUnresolvedIssueInIndexOfTypeIgnoringPermissions(
      unresolvedIssues,
      RuleType.BUG,
      tuple(BUGS_KEY, EvaluationStatus.ERROR, Optional.of(valueOf(unresolvedIssues))),
      tuple(VULNERABILITIES_KEY, EvaluationStatus.OK, Optional.of("0")),
      tuple(CODE_SMELLS_KEY, EvaluationStatus.OK, Optional.of("0")));
  }

  @Test
  public void computes_QG_error_if_there_is_one_unresolved_vulnerability_issue_in_index_ignoring_permissions() {
    int unresolvedIssues = 1 + random.nextInt(10);

    computesQGErrorIfThereIsAtLeastOneUnresolvedIssueInIndexOfTypeIgnoringPermissions(
      unresolvedIssues,
      RuleType.VULNERABILITY,
      tuple(BUGS_KEY, EvaluationStatus.OK, Optional.of("0")),
      tuple(VULNERABILITIES_KEY, EvaluationStatus.ERROR, Optional.of(valueOf(unresolvedIssues))),
      tuple(CODE_SMELLS_KEY, EvaluationStatus.OK, Optional.of("0")));
  }

  @Test
  public void computes_QG_error_if_there_is_one_unresolved_codeSmell_issue_in_index_ignoring_permissions() {
    int unresolvedIssues = 1 + random.nextInt(10);

    computesQGErrorIfThereIsAtLeastOneUnresolvedIssueInIndexOfTypeIgnoringPermissions(
      unresolvedIssues,
      RuleType.CODE_SMELL,
      tuple(BUGS_KEY, EvaluationStatus.OK, Optional.of("0")),
      tuple(VULNERABILITIES_KEY, EvaluationStatus.OK, Optional.of("0")),
      tuple(CODE_SMELLS_KEY, EvaluationStatus.ERROR, Optional.of(valueOf(unresolvedIssues))));
  }

  private void computesQGErrorIfThereIsAtLeastOneUnresolvedIssueInIndexOfTypeIgnoringPermissions(
    int unresolvedIssues, RuleType ruleType, Tuple... expectedQGConditions) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentAndBranch branch = insertPrivateBranch(organization, BranchType.SHORT);
    SnapshotDto analysis = insertAnalysisTask(branch);
    IntStream.range(0, unresolvedIssues).forEach(i -> insertIssue(branch, ruleType, randomOpenStatus, null));
    IntStream.range(0, random.nextInt(10)).forEach(i -> insertIssue(branch, ruleType, randomNonOpenStatus, randomResolution));
    indexIssues(branch);
    Configuration configuration = mock(Configuration.class);
    mockWebhookEnabled(configuration);
    mockPayloadSupplierConsumedByWebhooks();

    underTest.onChanges(Trigger.ISSUE_CHANGE, singletonList(newQGChangeEvent(branch, analysis, configuration)));

    ProjectAnalysis projectAnalysis = verifyWebhookCalledAndExtractPayloadFactoryArgument(branch, configuration, analysis);
    EvaluatedQualityGate qualityGate = projectAnalysis.getQualityGate().get();
    assertThat(qualityGate.getStatus()).isEqualTo(EvaluatedQualityGate.Status.ERROR);
    assertThat(qualityGate.getEvaluatedConditions())
      .extracting(s -> s.getCondition().getMetricKey(), EvaluatedCondition::getStatus, EvaluatedCondition::getValue)
      .containsOnly(expectedQGConditions);
  }

  @Test
  public void computes_QG_error_with_all_failing_conditions() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentAndBranch branch = insertPrivateBranch(organization, BranchType.SHORT);
    SnapshotDto analysis = insertAnalysisTask(branch);
    int unresolvedBugs = 1 + random.nextInt(10);
    int unresolvedVulnerabilities = 1 + random.nextInt(10);
    int unresolvedCodeSmells = 1 + random.nextInt(10);
    IntStream.range(0, unresolvedBugs).forEach(i -> insertIssue(branch, RuleType.BUG, randomOpenStatus, null));
    IntStream.range(0, unresolvedVulnerabilities).forEach(i -> insertIssue(branch, RuleType.VULNERABILITY, randomOpenStatus, null));
    IntStream.range(0, unresolvedCodeSmells).forEach(i -> insertIssue(branch, RuleType.CODE_SMELL, randomOpenStatus, null));
    indexIssues(branch);
    Configuration configuration = mock(Configuration.class);
    mockWebhookEnabled(configuration);
    mockPayloadSupplierConsumedByWebhooks();

    underTest.onChanges(Trigger.ISSUE_CHANGE, singletonList(newQGChangeEvent(branch, analysis, configuration)));

    ProjectAnalysis projectAnalysis = verifyWebhookCalledAndExtractPayloadFactoryArgument(branch, configuration, analysis);
    EvaluatedQualityGate qualityGate = projectAnalysis.getQualityGate().get();
    assertThat(qualityGate.getStatus()).isEqualTo(EvaluatedQualityGate.Status.ERROR);
    assertThat(qualityGate.getEvaluatedConditions())
      .extracting(s -> s.getCondition().getMetricKey(), EvaluatedCondition::getStatus, EvaluatedCondition::getValue)
      .containsOnly(
        Tuple.tuple(BUGS_KEY, EvaluationStatus.ERROR, Optional.of(valueOf(unresolvedBugs))),
        Tuple.tuple(VULNERABILITIES_KEY, EvaluationStatus.ERROR, Optional.of(valueOf(unresolvedVulnerabilities))),
        Tuple.tuple(CODE_SMELLS_KEY, EvaluationStatus.ERROR, Optional.of(valueOf(unresolvedCodeSmells))));
  }

  private void mockWebhookEnabled(Configuration... configurations) {
    for (Configuration configuration : configurations) {
      Mockito.when(webHooks.isEnabled(configuration)).thenReturn(true);
    }
  }

  private void mockWebhookDisabled(Configuration... configurations) {
    for (Configuration configuration : configurations) {
      Mockito.when(webHooks.isEnabled(configuration)).thenReturn(false);
    }
  }

  private void mockPayloadSupplierConsumedByWebhooks() {
    Mockito.doAnswer(invocationOnMock -> {
      Supplier<WebhookPayload> supplier = (Supplier<WebhookPayload>) invocationOnMock.getArguments()[2];
      supplier.get();
      return null;
    }).when(webHooks)
      .sendProjectAnalysisUpdate(Matchers.any(Configuration.class), Matchers.any(), Matchers.any());
  }

  private void insertIssue(ComponentAndBranch componentAndBranch, RuleType ruleType, String status, @Nullable String resolution) {
    ComponentDto component = componentAndBranch.component;
    RuleDefinitionDto rule = RuleTesting.newRule();
    dbTester.rules().insert(rule);
    dbTester.commit();
    dbTester.issues().insert(rule, component, component, i -> i.setType(ruleType).setStatus(status).setResolution(resolution));
    dbTester.commit();
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
    return dbTester.components().insertSnapshot(componentAndBranch.component);
  }

  private ProjectAnalysis verifyWebhookCalledAndExtractPayloadFactoryArgument(ComponentAndBranch componentAndBranch, Configuration configuration, SnapshotDto analysis) {
    verifyWebhookCalled(componentAndBranch, analysis, configuration);

    return extractPayloadFactoryArguments(1).iterator().next();
  }

  private void verifyWebhookCalled(ComponentAndBranch componentAndBranch, SnapshotDto analysis, Configuration branchConfiguration) {
    verify(webHooks).isEnabled(branchConfiguration);
    verify(webHooks).sendProjectAnalysisUpdate(
      Matchers.same(branchConfiguration),
      Matchers.eq(new WebHooks.Analysis(componentAndBranch.uuid(), analysis.getUuid(), null)),
      any(Supplier.class));
  }

  private void verifyWebhookNotCalled(ComponentAndBranch componentAndBranch, SnapshotDto analysis, Configuration branchConfiguration) {
    verify(webHooks).isEnabled(branchConfiguration);
    verify(webHooks, times(0)).sendProjectAnalysisUpdate(
      Matchers.same(branchConfiguration),
      Matchers.eq(new WebHooks.Analysis(componentAndBranch.uuid(), analysis.getUuid(), null)),
      any(Supplier.class));
  }

  private List<ProjectAnalysis> extractPayloadFactoryArguments(int time) {
    ArgumentCaptor<ProjectAnalysis> projectAnalysisCaptor = ArgumentCaptor.forClass(ProjectAnalysis.class);
    verify(webhookPayloadFactory, Mockito.times(time)).create(projectAnalysisCaptor.capture());
    return projectAnalysisCaptor.getAllValues();
  }

  private void indexIssues(ComponentAndBranch componentAndBranch) {
    issueIndexer.indexOnAnalysis(componentAndBranch.uuid());
  }

  private ComponentAndBranch insertPrivateBranch(OrganizationDto organization, BranchType branchType) {
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    BranchDto branchDto = newBranchDto(project.projectUuid(), branchType)
      .setKey("foo");
    ComponentDto newComponent = dbTester.components().insertProjectBranch(project, branchDto);
    return new ComponentAndBranch(newComponent, branchDto);
  }

  public ComponentAndBranch insertMainBranch(OrganizationDto organization) {
    ComponentDto project = newPrivateProjectDto(organization);
    BranchDto branch = newBranchDto(project, LONG).setKey("master");
    dbTester.components().insertComponent(project);
    dbClient.branchDao().insert(dbTester.getSession(), branch);
    dbTester.commit();
    return new ComponentAndBranch(project, branch);
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

  private static QGChangeEvent newQGChangeEvent(ComponentAndBranch branch, SnapshotDto analysis, Configuration configuration) {
    return new QGChangeEvent(branch.component, branch.branch, analysis, configuration);
  }

}
