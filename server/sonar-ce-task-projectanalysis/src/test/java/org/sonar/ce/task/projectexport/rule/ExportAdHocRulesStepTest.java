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
package org.sonar.ce.task.projectexport.rule;

import com.google.common.collect.ImmutableList;
import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.ce.task.projectexport.steps.DumpElement;
import org.sonar.ce.task.projectexport.steps.FakeDumpWriter;
import org.sonar.ce.task.projectexport.steps.ProjectHolder;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleMetadataDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentDto.UUID_PATH_OF_ROOT;

public class ExportAdHocRulesStepTest {
  private static final String PROJECT_UUID = "some-uuid";

  private static final ComponentDto PROJECT = new ComponentDto()
    // no id yet
    .setScope(Scopes.PROJECT)
    .setQualifier(Qualifiers.PROJECT)
    .setDbKey("the_project")
    .setName("The Project")
    .setDescription("The project description")
    .setEnabled(true)
    .setUuid(PROJECT_UUID)
    .setUuidPath(UUID_PATH_OF_ROOT)
    .setRootUuid(PROJECT_UUID)
    .setModuleUuid(null)
    .setModuleUuidPath("." + PROJECT_UUID + ".")
    .setProjectUuid(PROJECT_UUID);

  private static final List<BranchDto> BRANCHES = ImmutableList.of(
    new BranchDto().setBranchType(BranchType.PULL_REQUEST).setProjectUuid(PROJECT_UUID).setKey("pr-1").setUuid("pr-1-uuid").setMergeBranchUuid("master"),
    new BranchDto().setBranchType(BranchType.BRANCH).setProjectUuid(PROJECT_UUID).setKey("branch-2").setUuid("branch-2-uuid").setMergeBranchUuid("master")
      .setExcludeFromPurge(true),
    new BranchDto().setBranchType(BranchType.BRANCH).setProjectUuid(PROJECT_UUID).setKey("branch-3").setUuid("branch-3-uuid").setMergeBranchUuid("master")
      .setExcludeFromPurge(false));


  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private int issueUuidGenerator = 1;
  private FakeDumpWriter dumpWriter = new FakeDumpWriter();
  private ProjectHolder projectHolder = mock(ProjectHolder.class);
  private ExportAdHocRulesStep underTest = new ExportAdHocRulesStep(dbTester.getDbClient(), projectHolder, dumpWriter);

  @Before
  public void setup() {
    ProjectDto project = createProject();
    when(projectHolder.projectDto()).thenReturn(project);
  }

  @Test
  public void export_zero_ad_hoc_rules() {
    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.AdHocRule> exportedRules = dumpWriter.getWrittenMessagesOf(DumpElement.AD_HOC_RULES);
    assertThat(exportedRules).isEmpty();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("0 ad-hoc rules exported");
  }

  @Test
  public void execute_only_exports_ad_hoc_rules_that_reference_project_issue() {
    String differentProject = "diff-proj-uuid";
    RuleDto rule1 = insertRule(RuleKey.of("plugin-1", "rule-1"), true, true);
    RuleDto rule2 = insertRule(RuleKey.of("plugin-1", "rule-2"), true, true);
    insertRule(RuleKey.of("plugin-1", "rule-3"), true, true);
    insertIssue(rule1, differentProject, differentProject);
    insertIssue(rule2, PROJECT_UUID, PROJECT_UUID);

    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.AdHocRule> exportedRules = dumpWriter.getWrittenMessagesOf(DumpElement.AD_HOC_RULES);
    assertThat(exportedRules).hasSize(1);
    assertThat(exportedRules).extracting(ProjectDump.AdHocRule::getPluginName, ProjectDump.AdHocRule::getPluginRuleKey)
      .containsOnly(tuple("plugin-1", "rule-2"));
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("1 ad-hoc rules exported");
  }

  @Test
  public void execute_only_exports_rules_that_are_ad_hoc() {
    RuleDto rule1 = insertRule(RuleKey.of("plugin-1", "rule-1"), false, false);
    RuleDto rule2 = insertRule(RuleKey.of("plugin-1", "rule-2"), true, false);
    RuleDto rule3 = insertRule(RuleKey.of("plugin-1", "rule-3"), true, true);
    insertIssue(rule1, PROJECT_UUID, PROJECT_UUID);
    insertIssue(rule2, PROJECT_UUID, PROJECT_UUID);
    insertIssue(rule3, PROJECT_UUID, PROJECT_UUID);

    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.AdHocRule> exportedRules = dumpWriter.getWrittenMessagesOf(DumpElement.AD_HOC_RULES);
    assertThat(exportedRules).hasSize(1);
    assertThat(exportedRules).extracting(ProjectDump.AdHocRule::getPluginName, ProjectDump.AdHocRule::getPluginRuleKey)
      .containsOnly(tuple("plugin-1", "rule-3"));
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("1 ad-hoc rules exported");
  }

  @Test
  public void execute_exports_ad_hoc_rules_that_are_referenced_by_issues_on_branches_excluded_from_purge() {
    when(projectHolder.branches()).thenReturn(BRANCHES);
    RuleDto rule1 = insertRule(RuleKey.of("plugin-1", "rule-1"), true, true);
    RuleDto rule2 = insertRule(RuleKey.of("plugin-1", "rule-2"), true, true);
    RuleDto rule3 = insertRule(RuleKey.of("plugin-1", "rule-3"), true, true);
    insertIssue(rule1, "branch-1-uuid", "branch-1-uuid");
    insertIssue(rule2, "branch-2-uuid", "branch-2-uuid");
    insertIssue(rule3, "branch-3-uuid", "branch-3-uuid");

    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.AdHocRule> exportedRules = dumpWriter.getWrittenMessagesOf(DumpElement.AD_HOC_RULES);
    assertThat(exportedRules).hasSize(1);
    assertThat(exportedRules).extracting(ProjectDump.AdHocRule::getPluginName, ProjectDump.AdHocRule::getPluginRuleKey)
      .containsOnly(tuple("plugin-1", "rule-2"));
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("1 ad-hoc rules exported");
  }

  @Test
  public void execute_throws_ISE_with_number_of_successful_exports_before_failure() {
    RuleDto rule1 = insertRule(RuleKey.of("plugin-1", "rule-1"), true, true);
    RuleDto rule2 = insertRule(RuleKey.of("plugin-1", "rule-2"), true, true);
    RuleDto rule3 = insertRule(RuleKey.of("plugin-1", "rule-3"), true, true);
    insertIssue(rule1, PROJECT_UUID, PROJECT_UUID);
    insertIssue(rule2, PROJECT_UUID, PROJECT_UUID);
    insertIssue(rule3, PROJECT_UUID, PROJECT_UUID);
    dumpWriter.failIfMoreThan(2, DumpElement.AD_HOC_RULES);

    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Ad-hoc rules export failed after processing 2 rules successfully");
  }

  @Test
  public void getDescription() {
    assertThat(underTest.getDescription()).isEqualTo("Export ad-hoc rules");
  }

  private ProjectDto createProject() {
    Date createdAt = new Date();
    ComponentDto projectDto = dbTester.components().insertPublicProject(PROJECT);
    BRANCHES.forEach(branch -> dbTester.components().insertProjectBranch(projectDto, branch).setCreatedAt(createdAt));
    dbTester.commit();
    return dbTester.components().getProjectDto(projectDto);
  }

  private void insertIssue(RuleDto ruleDto, String projectUuid, String componentUuid) {
    IssueDto dto = createBaseIssueDto(ruleDto, projectUuid, componentUuid);
    insertIssue(dto);
  }

  private void insertIssue(IssueDto dto) {
    dbTester.getDbClient().issueDao().insert(dbTester.getSession(), dto);
    dbTester.commit();
  }

  private IssueDto createBaseIssueDto(RuleDto ruleDto, String projectUuid, String componentUuid) {
    return new IssueDto()
      .setKee("issue_uuid_" + issueUuidGenerator++)
      .setComponentUuid(componentUuid)
      .setProjectUuid(projectUuid)
      .setRuleUuid(ruleDto.getUuid())
      .setStatus("OPEN");
  }

  private RuleDto insertRule(RuleKey ruleKey, boolean isExternal, boolean isAdHoc) {
    dbTester.rules().insert(
      new RuleDefinitionDto()
        .setRuleKey(ruleKey)
        .setIsExternal(isExternal)
        .setIsAdHoc(isAdHoc)
        .setStatus(RuleStatus.READY)
        .setScope(RuleDto.Scope.ALL));
    dbTester.commit();
    return dbTester.getDbClient().ruleDao().selectByKey(dbTester.getSession(), ruleKey)
      .orElseThrow(() -> new RuntimeException("insertAdHocRule failed"));
  }

  private void insertRuleMetadata(String ruleUuid, String adHocName) {
    dbTester.rules().insertOrUpdateMetadata(new RuleMetadataDto()
      .setRuleUuid(ruleUuid)
      .setAdHocName(adHocName));
    dbTester.commit();
  }
}
