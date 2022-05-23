/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.db.report;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.SeverityUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.within;
import static org.sonar.api.issue.Issue.RESOLUTION_WONT_FIX;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.report.QualityGateFindingDto.NEW_CODE_METRIC_PREFIX;

public class RegulatoryReportDaoTest {
  private static final String PROJECT_UUID = "prj_uuid";
  private static final String PROJECT_KEY = "prj_key";
  private static final String FILE_UUID = "file_uuid";
  private static final String FILE_KEY = "file_key";
  private static final String BRANCH_UUID = "branch_uuid";
  private static final String BRANCH_NAME = "branch";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final RegulatoryReportDao underTest = db.getDbClient().regulatoryReportDao();
  private ComponentDto project;
  private RuleDto rule;
  private ComponentDto file;

  @Before
  public void prepare() {
    rule = db.rules().insertRule();
    project = db.components().insertPrivateProject(t -> t.setProjectUuid(PROJECT_UUID).setUuid(PROJECT_UUID).setDbKey(PROJECT_KEY));
    file = db.components().insertComponent(newFileDto(project).setUuid(FILE_UUID).setDbKey(FILE_KEY));
  }

  @Test
  public void getQualityGateFindings_returns_all_quality_gate_details_for_project() {
    ProjectDto project = db.components().insertPublicProjectDto();
    BranchDto branch = db.components().insertProjectBranch(project).setBranchType(BranchType.BRANCH);
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    db.qualityGates().setDefaultQualityGate(gate);

    MetricDto metric1 = db.measures().insertMetric(m -> m.setDescription("metric 1 description").setDecimalScale(0));
    QualityGateConditionDto condition1 = db.qualityGates().addCondition(gate, metric1);

    MetricDto metric2 = db.measures().insertMetric(m -> m.setDescription("metric 2 description").setDecimalScale(1));
    QualityGateConditionDto condition2 = db.qualityGates().addCondition(gate, metric2);

    MetricDto metric3 = db.measures().insertMetric(m -> m.setDescription("metric 3 description").setDecimalScale(0));
    QualityGateConditionDto condition3 = db.qualityGates().addCondition(gate, metric3);

    db.qualityGates().associateProjectToQualityGate(project, gate);
    db.commit();

    List<QualityGateFindingDto> findings = new ArrayList<>();
    underTest.getQualityGateFindings(db.getSession(), gate.getUuid(), result -> findings.add(result.getResultObject()));

    QualityGateFindingDto finding = findings.stream().filter(f -> f.getDescription().equals("metric 1 description")).findFirst().get();

    // check fields
    assertThat(findings).hasSize(3);
    assertThat(findings.stream().map(f -> f.getDescription()).collect(Collectors.toSet())).containsExactlyInAnyOrder("metric 1 description", "metric 2 description", "metric 3 description");
    assertThat(finding.getDescription()).isEqualTo(metric1.getDescription());
    assertThat(finding.getOperatorDescription()).isEqualTo(QualityGateFindingDto.OperatorDescription.valueOf(condition1.getOperator()).getDescription());
    assertThat(finding.getErrorThreshold()).isEqualTo(condition1.getErrorThreshold());
    assertThat(finding.getValueType()).isEqualTo(metric1.getValueType());
    assertThat(finding.isNewCodeMetric()).isEqualTo(metric1.getKey().startsWith(NEW_CODE_METRIC_PREFIX));
    assertThat(finding.isEnabled()).isEqualTo(metric1.isEnabled());
    assertThat(finding.getBestValue()).isEqualTo(metric1.getBestValue(), within(0.00001));
    assertThat(finding.getWorstValue()).isEqualTo(metric1.getWorstValue(), within(0.00001));
    assertThat(finding.isOptimizedBestValue()).isEqualTo(metric1.isOptimizedBestValue());
    assertThat(finding.getDecimalScale()).isEqualTo(metric1.getDecimalScale());
  }

  @Test
  public void getQualityProfileFindings_returns_all_quality_profile_details_for_project() {
    String projectUuid = "project_uuid";
    String projectKey = "project_key";
    String branchUuid = "branch_uuid";
    String branchName = "branch";

    BranchDto branch = new BranchDto()
      .setBranchType(BranchType.BRANCH)
      .setKey(branchName)
      .setUuid(branchUuid)
      .setProjectUuid(projectUuid);

    db.getDbClient().branchDao().insert(db.getSession(), branch);

    ProjectDto project = db.components().insertPublicProjectDto(t -> t.setProjectUuid(projectUuid)
      .setUuid(projectUuid)
      .setDbKey(projectKey)
      .setMainBranchProjectUuid(branchUuid));

    QProfileDto cppQPWithoutActiveRules = db.qualityProfiles().insert(qp -> qp.setIsBuiltIn(true).setLanguage("cpp"));
    db.qualityProfiles().setAsDefault(cppQPWithoutActiveRules);

    QProfileDto javaBuiltInQPWithActiveRules = db.qualityProfiles().insert(qp -> qp.setIsBuiltIn(true).setLanguage("java"));
    RuleDto rule1 = db.rules().insert(r -> r.setName("rule 1 title"));
    ActiveRuleDto activeRule1 = db.qualityProfiles().activateRule(javaBuiltInQPWithActiveRules, rule1);
    RuleDto rule2 = db.rules().insert(r -> r.setName("rule 2 title"));
    ActiveRuleDto activeRule2 = db.qualityProfiles().activateRule(javaBuiltInQPWithActiveRules, rule2);
    RuleDto rule3 = db.rules().insert(r -> r.setName("rule 3 title"));
    ActiveRuleDto activeRule3 = db.qualityProfiles().activateRule(javaBuiltInQPWithActiveRules, rule3);

    db.qualityProfiles().associateWithProject(project, cppQPWithoutActiveRules, javaBuiltInQPWithActiveRules);
    db.getSession().commit();

    List<QualityProfileFindingDto> findings = new ArrayList<>();
    underTest.getQualityProfileFindings(db.getSession(), javaBuiltInQPWithActiveRules.getKee(), result -> findings.add(result.getResultObject()));

    QualityProfileFindingDto finding = findings.stream().filter(f -> f.getTitle().equals("rule 1 title")).findFirst().get();

    assertThat(findings).hasSize(3);
    assertThat(findings.stream().map(f -> f.getTitle()).collect(Collectors.toSet())).containsExactlyInAnyOrder("rule 1 title", "rule 2 title", "rule 3 title");
    assertThat(finding.getLanguage()).isEqualTo(rule1.getLanguage());
    assertThat(finding.getTitle()).isEqualTo(rule1.getName());
    assertThat(finding.getReferenceKey()).isEqualTo(RuleKey.of(rule1.getRepositoryKey(), rule1.getRuleKey()));
    assertThat(finding.getStatus()).isEqualTo(rule1.getStatus());
    assertThat(finding.getType()).isEqualTo(RuleType.valueOf(rule1.getType()));
    assertThat(finding.getSeverity()).isEqualTo(SeverityUtil.getSeverityFromOrdinal(activeRule1.getSeverity()));
  }

  @Test
  public void scrollIssues_returns_all_non_closed_issues_for_project() {
    IssueDto issue1 = db.issues().insertIssue(rule, project, file, i -> i.setStatus("OPEN").setResolution(null));
    IssueDto issue2 = db.issues().insertIssue(rule, project, file, i -> i.setStatus("CONFIRMED").setResolution(null));
    IssueDto issue3 = db.issues().insertIssue(rule, project, file, i -> i.setStatus("RESOLVED").setResolution(RESOLUTION_WONT_FIX));

    // not returned
    IssueDto issue4 = db.issues().insertIssue(rule, project, file, i -> i.setStatus("CLOSED").setResolution(null));
    ComponentDto otherProject = db.components().insertPrivateProject();
    ComponentDto otherFile = db.components().insertComponent(newFileDto(otherProject));
    IssueDto issue5 = db.issues().insertIssue(rule, otherProject, otherFile);

    List<IssueFindingDto> issues = new ArrayList<>();
    underTest.scrollIssues(db.getSession(), PROJECT_UUID, result -> issues.add(result.getResultObject()));
    assertThat(issues).extracting(IssueFindingDto::getKey).containsOnly(issue1.getKey(), issue2.getKey(), issue3.getKey());

    // check fields
    IssueFindingDto issue = issues.stream().filter(i -> i.getKey().equals(issue1.getKey())).findFirst().get();
    assertThat(issue.getFileName()).isEqualTo(file.path());
    assertThat(issue.getRuleName()).isEqualTo(rule.getName());
    assertThat(issue.getRuleKey()).isEqualTo(rule.getRuleKey());
    assertThat(issue.getRuleRepository()).isEqualTo(rule.getRepositoryKey());
    assertThat(issue.getMessage()).isEqualTo(issue1.getMessage());
    assertThat(issue.getLine()).isEqualTo(issue1.getLine());
    assertThat(issue.getSeverity()).isEqualTo(issue1.getSeverity());
    assertThat(issue.getType().getDbConstant()).isEqualTo(issue1.getType());
    assertThat(issue.getSecurityStandards()).isEqualTo(rule.getSecurityStandards());
    assertThat(issue.isManualSeverity()).isEqualTo(issue1.isManualSeverity());
  }
}
