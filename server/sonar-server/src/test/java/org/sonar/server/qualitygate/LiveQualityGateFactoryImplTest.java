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
package org.sonar.server.qualitygate;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.issue.Issue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.permission.index.AuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.api.measures.CoreMetrics.BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.CODE_SMELLS_KEY;
import static org.sonar.api.measures.CoreMetrics.VULNERABILITIES_KEY;
import static org.sonar.db.component.ComponentTesting.newBranchDto;

public class LiveQualityGateFactoryImplTest {
  private static final List<String> OPEN_STATUSES = ImmutableList.of(Issue.STATUS_OPEN, Issue.STATUS_CONFIRMED);
  private static final List<String> NON_OPEN_STATUSES = Issue.STATUSES.stream().filter(OPEN_STATUSES::contains).collect(MoreCollectors.toList());

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public EsTester esTester = new EsTester(new IssueIndexDefinition(new MapSettings().asConfig()));
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private Random random = new Random();
  private String randomOpenStatus = OPEN_STATUSES.get(random.nextInt(OPEN_STATUSES.size()));
  private String randomNonOpenStatus = NON_OPEN_STATUSES.get(random.nextInt(NON_OPEN_STATUSES.size()));
  private String randomResolution = Issue.RESOLUTIONS.get(random.nextInt(Issue.RESOLUTIONS.size()));
  private IssueIndexer issueIndexer = new IssueIndexer(esTester.client(), dbTester.getDbClient(), new IssueIteratorFactory(dbTester.getDbClient()));
  private IssueIndex issueIndex = new IssueIndex(esTester.client(), System2.INSTANCE, userSessionRule, new AuthorizationTypeSupport(userSessionRule));

  private LiveQualityGateFactoryImpl underTest = new LiveQualityGateFactoryImpl(issueIndex, System2.INSTANCE);

  @Test
  public void compute_QG_ok_if_there_is_no_issue_in_index_ignoring_permissions() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = insertPrivateBranch(organization, BranchType.SHORT);

    EvaluatedQualityGate qualityGate = underTest.buildForShortLivedBranch(project);

    assertThat(qualityGate.getStatus()).isEqualTo(EvaluatedQualityGate.Status.OK);
    assertThat(qualityGate.getEvaluatedConditions())
      .extracting(EvaluatedCondition::getStatus, EvaluatedCondition::getValue)
      .containsOnly(tuple(EvaluatedCondition.EvaluationStatus.OK, Optional.of("0")));
  }

  @Test
  public void computes_QG_error_if_there_is_one_unresolved_bug_issue_in_index_ignoring_permissions() {
    int unresolvedIssues = 1 + random.nextInt(10);

    computesQGErrorIfThereIsAtLeastOneUnresolvedIssueInIndexOfTypeIgnoringPermissions(
      unresolvedIssues,
      RuleType.BUG,
      tuple(BUGS_KEY, EvaluatedCondition.EvaluationStatus.ERROR, Optional.of(valueOf(unresolvedIssues))),
      tuple(VULNERABILITIES_KEY, EvaluatedCondition.EvaluationStatus.OK, Optional.of("0")),
      tuple(CODE_SMELLS_KEY, EvaluatedCondition.EvaluationStatus.OK, Optional.of("0")));
  }

  @Test
  public void computes_QG_error_if_there_is_one_unresolved_vulnerability_issue_in_index_ignoring_permissions() {
    int unresolvedIssues = 1 + random.nextInt(10);

    computesQGErrorIfThereIsAtLeastOneUnresolvedIssueInIndexOfTypeIgnoringPermissions(
      unresolvedIssues,
      RuleType.VULNERABILITY,
      tuple(BUGS_KEY, EvaluatedCondition.EvaluationStatus.OK, Optional.of("0")),
      tuple(VULNERABILITIES_KEY, EvaluatedCondition.EvaluationStatus.ERROR, Optional.of(valueOf(unresolvedIssues))),
      tuple(CODE_SMELLS_KEY, EvaluatedCondition.EvaluationStatus.OK, Optional.of("0")));
  }

  @Test
  public void computes_QG_error_if_there_is_one_unresolved_codeSmell_issue_in_index_ignoring_permissions() {
    int unresolvedIssues = 1 + random.nextInt(10);

    computesQGErrorIfThereIsAtLeastOneUnresolvedIssueInIndexOfTypeIgnoringPermissions(
      unresolvedIssues,
      RuleType.CODE_SMELL,
      tuple(BUGS_KEY, EvaluatedCondition.EvaluationStatus.OK, Optional.of("0")),
      tuple(VULNERABILITIES_KEY, EvaluatedCondition.EvaluationStatus.OK, Optional.of("0")),
      tuple(CODE_SMELLS_KEY, EvaluatedCondition.EvaluationStatus.ERROR, Optional.of(valueOf(unresolvedIssues))));
  }

  private void computesQGErrorIfThereIsAtLeastOneUnresolvedIssueInIndexOfTypeIgnoringPermissions(
    int unresolvedIssues, RuleType ruleType, Tuple... expectedQGConditions) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = insertPrivateBranch(organization, BranchType.SHORT);
    IntStream.range(0, unresolvedIssues).forEach(i -> insertIssue(project, ruleType, randomOpenStatus, null));
    IntStream.range(0, random.nextInt(10)).forEach(i -> insertIssue(project, ruleType, randomNonOpenStatus, randomResolution));
    indexIssues(project);

    EvaluatedQualityGate qualityGate = underTest.buildForShortLivedBranch(project);

    assertThat(qualityGate.getStatus()).isEqualTo(EvaluatedQualityGate.Status.ERROR);
    assertThat(qualityGate.getEvaluatedConditions())
      .extracting(s -> s.getCondition().getMetricKey(), EvaluatedCondition::getStatus, EvaluatedCondition::getValue)
      .containsOnly(expectedQGConditions);
  }

  @Test
  public void computes_QG_error_with_all_failing_conditions() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = insertPrivateBranch(organization, BranchType.SHORT);
    int unresolvedBugs = 1 + random.nextInt(10);
    int unresolvedVulnerabilities = 1 + random.nextInt(10);
    int unresolvedCodeSmells = 1 + random.nextInt(10);
    IntStream.range(0, unresolvedBugs).forEach(i -> insertIssue(project, RuleType.BUG, randomOpenStatus, null));
    IntStream.range(0, unresolvedVulnerabilities).forEach(i -> insertIssue(project, RuleType.VULNERABILITY, randomOpenStatus, null));
    IntStream.range(0, unresolvedCodeSmells).forEach(i -> insertIssue(project, RuleType.CODE_SMELL, randomOpenStatus, null));
    indexIssues(project);

    EvaluatedQualityGate qualityGate = underTest.buildForShortLivedBranch(project);

    assertThat(qualityGate.getStatus()).isEqualTo(EvaluatedQualityGate.Status.ERROR);
    assertThat(qualityGate.getEvaluatedConditions())
      .extracting(s -> s.getCondition().getMetricKey(), EvaluatedCondition::getStatus, EvaluatedCondition::getValue)
      .containsOnly(
        Tuple.tuple(BUGS_KEY, EvaluatedCondition.EvaluationStatus.ERROR, Optional.of(valueOf(unresolvedBugs))),
        Tuple.tuple(VULNERABILITIES_KEY, EvaluatedCondition.EvaluationStatus.ERROR, Optional.of(valueOf(unresolvedVulnerabilities))),
        Tuple.tuple(CODE_SMELLS_KEY, EvaluatedCondition.EvaluationStatus.ERROR, Optional.of(valueOf(unresolvedCodeSmells))));
  }

  private void indexIssues(ComponentDto project) {
    issueIndexer.indexOnAnalysis(project.uuid());
  }

  private void insertIssue(ComponentDto component, RuleType ruleType, String status, @Nullable String resolution) {
    RuleDefinitionDto rule = RuleTesting.newRule();
    dbTester.rules().insert(rule);
    dbTester.commit();
    dbTester.issues().insert(rule, component, component, i -> i.setType(ruleType).setStatus(status).setResolution(resolution));
    dbTester.commit();
  }

  private ComponentDto insertPrivateBranch(OrganizationDto organization, BranchType branchType) {
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    BranchDto branchDto = newBranchDto(project.projectUuid(), branchType)
      .setKey("foo");
    return dbTester.components().insertProjectBranch(project, branchDto);
  }

}
