/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectexport.rule;

import com.google.common.collect.ImmutableList;
import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectexport.steps.DumpElement;
import org.sonar.ce.task.projectexport.steps.FakeDumpWriter;
import org.sonar.ce.task.projectexport.steps.ProjectHolder;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.rule.RuleDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExportAdHocRulesStepIT {
  private static final String PROJECT_UUID = "some-uuid";
  private static final List<BranchDto> BRANCHES = ImmutableList.of(
    new BranchDto().setBranchType(BranchType.PULL_REQUEST).setProjectUuid(PROJECT_UUID).setKey("pr-1").setUuid("pr-1-uuid").setMergeBranchUuid("master").setIsMain(false),
    new BranchDto().setBranchType(BranchType.BRANCH).setProjectUuid(PROJECT_UUID).setKey("branch-2").setUuid("branch-2-uuid").setMergeBranchUuid("master")
      .setExcludeFromPurge(true).setIsMain(false),
    new BranchDto().setBranchType(BranchType.BRANCH).setProjectUuid(PROJECT_UUID).setKey("branch-3").setUuid("branch-3-uuid").setMergeBranchUuid("master")
      .setExcludeFromPurge(false).setIsMain(false));

  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private int issueUuidGenerator = 1;
  private ComponentDto mainBranch;
  private final FakeDumpWriter dumpWriter = new FakeDumpWriter();
  private final ProjectHolder projectHolder = mock(ProjectHolder.class);
  private final ExportAdHocRulesStep underTest = new ExportAdHocRulesStep(dbTester.getDbClient(), projectHolder, dumpWriter);

  @Before
  public void setup() {
    logTester.setLevel(Level.DEBUG);
    ProjectDto project = createProject();
    when(projectHolder.projectDto()).thenReturn(project);
  }

  @Test
  public void export_zero_ad_hoc_rules() {
    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.AdHocRule> exportedRules = dumpWriter.getWrittenMessagesOf(DumpElement.AD_HOC_RULES);
    assertThat(exportedRules).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).contains("0 ad-hoc rules exported");
  }

  @Test
  public void execute_only_exports_ad_hoc_rules_that_reference_project_issue() {
    String differentProject = "diff-proj-uuid";
    RuleDto rule1 = insertAddHocRule("rule-1");
    RuleDto rule2 = insertAddHocRule("rule-2");
    insertAddHocRule("rule-3");
    insertIssue(rule1, differentProject, differentProject);
    insertIssue(rule2, mainBranch.uuid(), mainBranch.uuid());

    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.AdHocRule> exportedRules = dumpWriter.getWrittenMessagesOf(DumpElement.AD_HOC_RULES);
    assertThat(exportedRules).hasSize(1);
    assertProtobufAdHocRuleIsCorrectlyBuilt(exportedRules.iterator().next(), rule2);
    assertThat(logTester.logs(Level.DEBUG)).contains("1 ad-hoc rules exported");
  }

  @Test
  public void execute_only_exports_rules_that_are_ad_hoc() {
    RuleDto rule1 = insertStandardRule("rule-1");
    RuleDto rule2 = insertExternalRule("rule-2");
    RuleDto rule3 = insertAddHocRule("rule-3");
    insertIssue(rule1, mainBranch.uuid(), mainBranch.uuid());
    insertIssue(rule2, mainBranch.uuid(), mainBranch.uuid());
    insertIssue(rule3, mainBranch.uuid(), mainBranch.uuid());

    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.AdHocRule> exportedRules = dumpWriter.getWrittenMessagesOf(DumpElement.AD_HOC_RULES);
    assertThat(exportedRules).hasSize(1);
    assertProtobufAdHocRuleIsCorrectlyBuilt(exportedRules.iterator().next(), rule3);
    assertThat(logTester.logs(Level.DEBUG)).contains("1 ad-hoc rules exported");
  }

  @Test
  public void execute_exports_ad_hoc_rules_that_are_referenced_by_issues_on_branches_excluded_from_purge() {
    when(projectHolder.branches()).thenReturn(BRANCHES);
    RuleDto rule1 = insertAddHocRule("rule-1");
    RuleDto rule2 = insertAddHocRule("rule-2");
    RuleDto rule3 = insertAddHocRule("rule-3");
    insertIssue(rule1, "branch-1-uuid", "branch-1-uuid");
    insertIssue(rule2, "branch-2-uuid", "branch-2-uuid");
    insertIssue(rule3, "branch-3-uuid", "branch-3-uuid");

    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.AdHocRule> exportedRules = dumpWriter.getWrittenMessagesOf(DumpElement.AD_HOC_RULES);
    assertThat(exportedRules).hasSize(1);
    assertProtobufAdHocRuleIsCorrectlyBuilt(exportedRules.iterator().next(), rule2);
    assertThat(logTester.logs(Level.DEBUG)).contains("1 ad-hoc rules exported");
  }

  @Test
  public void execute_throws_ISE_with_number_of_successful_exports_before_failure() {
    RuleDto rule1 = insertAddHocRule("rule-1");
    RuleDto rule2 = insertAddHocRule("rule-2");
    RuleDto rule3 = insertAddHocRule("rule-3");
    insertIssue(rule1, mainBranch.uuid(), mainBranch.uuid());
    insertIssue(rule2, mainBranch.uuid(), mainBranch.uuid());
    insertIssue(rule3, mainBranch.uuid(), mainBranch.uuid());
    dumpWriter.failIfMoreThan(2, DumpElement.AD_HOC_RULES);

    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Ad-hoc rules export failed after processing 2 rules successfully");
  }

  @Test
  public void execute_shouldReturnCorrectAdhocRules_whenMultipleIssuesForSameRule() {
    RuleDto rule1 = insertAddHocRule("rule-1");
    insertIssue(rule1, mainBranch.uuid(), mainBranch.uuid());
    insertIssue(rule1, mainBranch.uuid(), mainBranch.uuid());
    insertIssue(rule1, mainBranch.uuid(), mainBranch.uuid());

    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.AdHocRule> exportedRules = dumpWriter.getWrittenMessagesOf(DumpElement.AD_HOC_RULES);
    assertThat(exportedRules).hasSize(1);
    assertProtobufAdHocRuleIsCorrectlyBuilt(exportedRules.iterator().next(), rule1);
  }

  @Test
  public void getDescription() {
    assertThat(underTest.getDescription()).isEqualTo("Export ad-hoc rules");
  }

  @Test
  public void execute_shouldMapFieldWithEmptyString_whenNameOrPluginKeyAreNull() {
    RuleKey ruleKey = RuleKey.of("plugin1", "partiallyInit");
    RuleDto partiallyInitRuleDto = insertAdHocRuleWithoutNameAndPluginKeyAndAdHocInformations(ruleKey);
    insertIssue(partiallyInitRuleDto, mainBranch.uuid(), mainBranch.uuid());

    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.AdHocRule> exportedRules = dumpWriter.getWrittenMessagesOf(DumpElement.AD_HOC_RULES);
    assertThat(exportedRules).hasSize(1);
    ProjectDump.AdHocRule adHocRule = exportedRules.iterator().next();
    assertThat(adHocRule.getName()).isEmpty();
    assertThat(adHocRule.getPluginKey()).isEmpty();
    ProjectDump.AdHocRule.RuleMetadata adHocRuleMetadata = adHocRule.getMetadata();
    assertThat(adHocRuleMetadata.getAdHocDescription()).isEmpty();
    assertThat(adHocRuleMetadata.getAdHocName()).isEmpty();
    assertThat(adHocRuleMetadata.getAdHocSeverity()).isEmpty();
    assertThat(adHocRuleMetadata.getAdHocType()).isZero();

  }

  private RuleDto insertAdHocRuleWithoutNameAndPluginKeyAndAdHocInformations(RuleKey ruleKey) {
    RuleDto partiallyInitRuleDto = new RuleDto()
      .setIsExternal(false)
      .setIsAdHoc(true)
      .setCleanCodeAttribute(CleanCodeAttribute.CONVENTIONAL)
      .addDefaultImpact(new ImpactDto().setSoftwareQuality(SoftwareQuality.MAINTAINABILITY).setSeverity(org.sonar.api.issue.impact.Severity.MEDIUM))
      .addDefaultImpact(new ImpactDto().setSoftwareQuality(SoftwareQuality.RELIABILITY).setSeverity(org.sonar.api.issue.impact.Severity.BLOCKER))
      .setRuleKey(ruleKey)
      .setScope(RuleDto.Scope.ALL)
      .setStatus(RuleStatus.READY);

    dbTester.rules().insert(partiallyInitRuleDto);
    dbTester.commit();

    return dbTester.getDbClient().ruleDao().selectByKey(dbTester.getSession(), ruleKey)
      .orElseThrow(() -> new RuntimeException("insertAdHocRule failed"));
  }

  private ProjectDto createProject() {
    Date createdAt = new Date();
    ProjectData projectData = dbTester.components().insertPublicProject(PROJECT_UUID);
    mainBranch = projectData.getMainBranchComponent();
    BRANCHES.forEach(branch -> dbTester.components().insertProjectBranch(projectData.getProjectDto(), branch).setCreatedAt(createdAt));
    dbTester.commit();
    return projectData.getProjectDto();
  }

  private void insertIssue(RuleDto ruleDto, String branchUuid, String componentUuid) {
    IssueDto dto = createBaseIssueDto(ruleDto, branchUuid, componentUuid);
    insertIssue(dto);
  }

  private void insertIssue(IssueDto dto) {
    dbTester.getDbClient().issueDao().insert(dbTester.getSession(), dto);
    dbTester.commit();
  }

  private IssueDto createBaseIssueDto(RuleDto ruleDto, String branchUuid, String componentUuid) {
    return new IssueDto()
      .setKee("issue_uuid_" + issueUuidGenerator++)
      .setComponentUuid(componentUuid)
      .setProjectUuid(branchUuid)
      .setRuleUuid(ruleDto.getUuid())
      .setStatus("OPEN");
  }

  private RuleDto insertExternalRule(String ruleName) {
    RuleDto ruleDto = new RuleDto()
      .setIsExternal(true)
      .setIsAdHoc(false);
    return insertRule(ruleName, ruleDto);
  }

  private RuleDto insertAddHocRule(String ruleName) {
    RuleDto ruleDto = new RuleDto()
      .setIsExternal(false)
      .setIsAdHoc(true)
      .setCleanCodeAttribute(CleanCodeAttribute.CONVENTIONAL)
      .addDefaultImpact(new ImpactDto().setSoftwareQuality(SoftwareQuality.MAINTAINABILITY).setSeverity(org.sonar.api.issue.impact.Severity.MEDIUM))
      .addDefaultImpact(new ImpactDto().setSoftwareQuality(SoftwareQuality.RELIABILITY).setSeverity(org.sonar.api.issue.impact.Severity.HIGH))
      .setAdHocName("ad_hoc_rule" + RandomStringUtils.secure().nextAlphabetic(10))
      .setAdHocType(RuleType.VULNERABILITY)
      .setAdHocSeverity(Severity.CRITICAL)
      .setAdHocDescription("ad hoc description: " + RandomStringUtils.secure().nextAlphanumeric(100));
    return insertRule(ruleName, ruleDto);
  }

  private RuleDto insertStandardRule(String ruleName) {
    RuleDto ruleDto = new RuleDto()
      .setIsExternal(false)
      .setIsAdHoc(false);
    return insertRule(ruleName, ruleDto);
  }

  private RuleDto insertRule(String ruleName, RuleDto partiallyInitRuleDto) {
    RuleKey ruleKey = RuleKey.of("plugin1", ruleName);
    partiallyInitRuleDto
      .setName("ruleName" + RandomStringUtils.secure().nextAlphanumeric(10))
      .setRuleKey(ruleKey)
      .setPluginKey("pluginKey" + RandomStringUtils.secure().nextAlphanumeric(10))
      .setStatus(RuleStatus.READY)
      .setScope(RuleDto.Scope.ALL);

    dbTester.rules().insert(partiallyInitRuleDto);
    dbTester.commit();
    return dbTester.getDbClient().ruleDao().selectByKey(dbTester.getSession(), ruleKey)
      .orElseThrow(() -> new RuntimeException("insertAdHocRule failed"));
  }

  private static void assertProtobufAdHocRuleIsCorrectlyBuilt(ProjectDump.AdHocRule protobufAdHocRule, RuleDto source) {
    assertThat(protobufAdHocRule.getName()).isEqualTo(source.getName());
    assertThat(protobufAdHocRule.getRef()).isEqualTo(source.getUuid());
    assertThat(protobufAdHocRule.getPluginKey()).isEqualTo(source.getPluginKey());
    assertThat(protobufAdHocRule.getPluginRuleKey()).isEqualTo(source.getRuleKey());
    assertThat(protobufAdHocRule.getPluginName()).isEqualTo(source.getRepositoryKey());
    assertThat(protobufAdHocRule.getName()).isEqualTo(source.getName());
    assertThat(protobufAdHocRule.getStatus()).isEqualTo(source.getStatus().name());
    assertThat(protobufAdHocRule.getType()).isEqualTo(source.getType());
    assertThat(protobufAdHocRule.getScope()).isEqualTo(source.getScope().name());
    assertThat(protobufAdHocRule.getCleanCodeAttribute()).isEqualTo(source.getCleanCodeAttribute().name());
    assertThat(toImpactMap(protobufAdHocRule.getImpactsList())).isEqualTo(toImpactMap(source.getDefaultImpacts()));
    assertProtobufAdHocRuleIsCorrectlyBuilt(protobufAdHocRule.getMetadata(), source);
  }

  private static Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> toImpactMap(Set<ImpactDto> defaultImpacts) {
    return defaultImpacts
      .stream().collect(Collectors.toMap(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity));
  }

  private static Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> toImpactMap(List<ProjectDump.Impact> impactsList) {
    return impactsList.stream()
      .collect(Collectors.toMap(i -> SoftwareQuality.valueOf(i.getSoftwareQuality().name()),
        i -> org.sonar.api.issue.impact.Severity.valueOf(i.getSeverity().name())));
  }

  private static void assertProtobufAdHocRuleIsCorrectlyBuilt(ProjectDump.AdHocRule.RuleMetadata metadata, RuleDto expected) {
    assertThat(metadata.getAdHocName()).isEqualTo(expected.getAdHocName());
    assertThat(metadata.getAdHocDescription()).isEqualTo(expected.getAdHocDescription());
    assertThat(metadata.getAdHocSeverity()).isEqualTo(expected.getAdHocSeverity());
    assertThat(metadata.getAdHocType()).isEqualTo(expected.getAdHocType());
  }

}
