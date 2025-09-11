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
package org.sonar.ce.task.projectexport.issue;

import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Random;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.event.Level;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.core.rule.RuleType;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectexport.component.ComponentRepositoryImpl;
import org.sonar.ce.task.projectexport.component.MutableComponentRepository;
import org.sonar.ce.task.projectexport.rule.RuleRepository;
import org.sonar.ce.task.projectexport.rule.RuleRepositoryImpl;
import org.sonar.ce.task.projectexport.steps.DumpElement;
import org.sonar.ce.task.projectexport.steps.FakeDumpWriter;
import org.sonar.ce.task.projectexport.steps.ProjectHolder;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.protobuf.DbIssues.Locations;
import org.sonar.db.protobuf.DbIssues.MessageFormattingType;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleDto.Scope;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_REOPENED;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;

@RunWith(DataProviderRunner.class)
public class ExportIssuesStepIT {
  private static final String SOME_PROJECT_UUID = "project uuid";
  private static final String PROJECT_KEY = "projectkey";
  private static final String SOME_REPO = "rule repo";
  private static final String READY_RULE_KEY = "rule key 1";
  public static final DbIssues.MessageFormatting MESSAGE_FORMATTING = DbIssues.MessageFormatting.newBuilder().setStart(0).setEnd(4).setType(MessageFormattingType.CODE).build();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public LogTester logTester = new LogTester();

  private final DbClient dbClient = dbTester.getDbClient();
  private final DbSession dbSession = dbClient.openSession(false);
  private final ProjectHolder projectHolder = mock(ProjectHolder.class);
  private final FakeDumpWriter dumpWriter = new FakeDumpWriter();
  private final RuleRepository ruleRepository = new RuleRepositoryImpl();
  private final MutableComponentRepository componentRepository = new ComponentRepositoryImpl();

  private final ExportIssuesStep underTest = new ExportIssuesStep(dbClient, projectHolder, dumpWriter, ruleRepository, componentRepository);

  private RuleDto readyRuleDto;

  @Before
  public void setUp() {
    logTester.setLevel(Level.DEBUG);
    ProjectDto project = createProject();
    when(projectHolder.projectDto()).thenReturn(project);
    when(projectHolder.branches()).thenReturn(newArrayList(
      new BranchDto().setBranchType(BranchType.BRANCH).setKey("master").setProjectUuid(SOME_PROJECT_UUID).setUuid(SOME_PROJECT_UUID)));

    // adds a random number of Rules to db and repository so that READY_RULE_KEY does always get id=ref=1
    for (int i = 0; i < new Random().nextInt(150); i++) {
      RuleKey ruleKey = RuleKey.of("repo_" + i, "key_" + i);
      RuleDto ruleDto = insertRule(ruleKey.toString());
      ruleRepository.register(ruleDto.getUuid(), ruleKey);
    }
    this.readyRuleDto = insertRule(READY_RULE_KEY);
    componentRepository.register(12, SOME_PROJECT_UUID, false);
  }

  @After
  public void tearDown() {
    dbSession.close();
  }

  @Test
  public void getDescription_is_set() {
    assertThat(underTest.getDescription()).isEqualTo("Export issues");
  }

  @Test
  public void execute_written_writes_no_issues_when_project_has_no_issues() {
    underTest.execute(new TestComputationStepContext());

    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.ISSUES)).isEmpty();
  }

  @Test
  public void execute_written_writes_no_issues_when_project_has_only_CLOSED_issues() {
    insertIssue(readyRuleDto, SOME_PROJECT_UUID, Issue.STATUS_CLOSED);

    underTest.execute(new TestComputationStepContext());

    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.ISSUES)).isEmpty();
  }

  @Test
  public void execute_fails_with_ISE_if_componentUuid_is_not_set() {
    insertIssue(createBaseIssueDto(readyRuleDto, SOME_PROJECT_UUID).setComponentUuid(null));

    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Issue export failed after processing 0 issues successfully")
      .hasRootCauseInstanceOf(NullPointerException.class)
      .hasRootCauseMessage("uuid can not be null");
  }

  @DataProvider
  public static Object[][] allStatusesButCLOSED() {
    return new Object[][] {
      {STATUS_OPEN},
      {STATUS_CONFIRMED},
      {STATUS_REOPENED},
      {STATUS_RESOLVED}
    };
  }

  @Test
  @UseDataProvider("allStatusesButCLOSED")
  public void execute_writes_issues_with_any_status_but_CLOSED(String status) {
    String uuid = insertIssue(readyRuleDto, SOME_PROJECT_UUID, status).getKey();

    underTest.execute(new TestComputationStepContext());

    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.ISSUES))
      .extracting(ProjectDump.Issue::getUuid)
      .containsOnly(uuid);
  }

  @Test
  public void execute_writes_issues_from_any_component_in_project_are_written() {
    componentRepository.register(13, "module uuid", false);
    componentRepository.register(14, "dir uuid", false);
    componentRepository.register(15, "file uuid", false);
    String projectIssueUuid = insertIssue(createBaseIssueDto(readyRuleDto, SOME_PROJECT_UUID)).getKey();
    String moduleIssueUuid = insertIssue(createBaseIssueDto(readyRuleDto, "module uuid")).getKey();
    String dirIssueUuid = insertIssue(createBaseIssueDto(readyRuleDto, "dir uuid")).getKey();
    String fileIssueUuid = insertIssue(createBaseIssueDto(readyRuleDto, "file uuid")).getKey();

    underTest.execute(new TestComputationStepContext());

    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.ISSUES))
      .extracting(ProjectDump.Issue::getUuid)
      .containsOnly(projectIssueUuid, moduleIssueUuid, dirIssueUuid, fileIssueUuid);
  }

  @Test
  public void execute_ignores_issues_of_other_projects() {
    insertIssue(createBaseIssueDto(readyRuleDto, SOME_PROJECT_UUID).setProjectUuid("other project"));
    String projectIssueUuid = insertIssue(createBaseIssueDto(readyRuleDto, SOME_PROJECT_UUID)).getKey();

    underTest.execute(new TestComputationStepContext());

    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.ISSUES))
      .extracting(ProjectDump.Issue::getUuid)
      .containsOnly(projectIssueUuid);
  }

  @Test
  public void verify_field_by_field_mapping() {
    String componentUuid = "component uuid";
    long componentRef = 5454;
    componentRepository.register(componentRef, componentUuid, false);
    DbIssues.MessageFormattings messageFormattings = DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build();
    IssueDto issueDto = new IssueDto()
      .setKee("issue uuid")
      .setComponentUuid(componentUuid)
      .setType(988)
      .setMessage("msg")
      .setMessageFormattings(messageFormattings)
      .setLine(10)
      .setChecksum("checksum")
      .setResolution("resolution")
      .setSeverity("severity")
      .setManualSeverity(true)
      .setGap(13.13d)
      .setEffort(99L)
      .setAssigneeUuid("assignee-uuid")
      .setAuthorLogin("author")
      .setTagsString("tags")
      .setRuleDescriptionContextKey("test_rule_description_context_key")
      .setIssueCreationTime(963L)
      .setIssueUpdateTime(852L)
      .addImpact(new ImpactDto().setSoftwareQuality(SoftwareQuality.MAINTAINABILITY).setSeverity(Severity.HIGH).setManualSeverity(true))
      .addImpact(new ImpactDto().setSoftwareQuality(SoftwareQuality.SECURITY).setSeverity(Severity.BLOCKER).setManualSeverity(false))
      .setIssueCloseTime(741L)
      .setCodeVariants(List.of("v1", "v2"))
      .setPrioritizedRule(true)
      .setInternalTagsString("internal-tag-1,internal-tag-2");

    // fields tested separately and/or required to match SQL request
    issueDto
      .setType(RuleType.CODE_SMELL)
      .setLocations(Locations.newBuilder().addFlow(DbIssues.Flow.newBuilder()).build())
      .setRuleUuid(readyRuleDto.getUuid())
      .setStatus(STATUS_OPEN).setProjectUuid(SOME_PROJECT_UUID);

    insertIssue(issueDto);

    underTest.execute(new TestComputationStepContext());

    ProjectDump.Issue issue = getWrittenIssue();

    assertThat(issue.getUuid()).isEqualTo(issueDto.getKey());
    assertThat(issue.getComponentRef()).isEqualTo(componentRef);
    assertThat(issue.getType()).isEqualTo(issueDto.getType());
    assertThat(issue.getMessage()).isEqualTo(issueDto.getMessage());
    assertThat(issue.getLine()).isEqualTo(issueDto.getLine());
    assertThat(issue.getChecksum()).isEqualTo(issueDto.getChecksum());
    assertThat(issue.getStatus()).isEqualTo(issueDto.getStatus());
    assertThat(issue.getResolution()).isEqualTo(issueDto.getResolution());
    assertThat(issue.getSeverity()).isEqualTo(issueDto.getSeverity());
    assertThat(issue.getManualSeverity()).isEqualTo(issueDto.isManualSeverity());
    assertThat(issue.getGap()).isEqualTo(issueDto.getGap());
    assertThat(issue.getEffort()).isEqualTo(issueDto.getEffort());
    assertThat(issue.getAssignee()).isEqualTo(issueDto.getAssigneeUuid());
    assertThat(issue.getAuthor()).isEqualTo(issueDto.getAuthorLogin());
    assertThat(issue.getTags()).isEqualTo(issueDto.getTagsString());
    assertThat(issue.getRuleDescriptionContextKey()).isEqualTo(issue.getRuleDescriptionContextKey());
    assertThat(issue.getIssueCreatedAt()).isEqualTo(issueDto.getIssueCreationTime());
    assertThat(issue.getIssueUpdatedAt()).isEqualTo(issueDto.getIssueUpdateTime());
    assertThat(issue.getIssueClosedAt()).isEqualTo(issueDto.getIssueCloseTime());
    assertThat(issue.getLocations()).isNotEmpty();
    assertThat(issue.getImpactsList()).extracting(ProjectDump.Impact::getSoftwareQuality, ProjectDump.Impact::getSeverity, ProjectDump.Impact::getManualSeverity)
      .containsOnly(tuple(ProjectDump.SoftwareQuality.MAINTAINABILITY, ProjectDump.Severity.HIGH, true),
        tuple(ProjectDump.SoftwareQuality.SECURITY, ProjectDump.Severity.BLOCKER, false));
    assertThat(issue.getMessageFormattingsList())
      .isEqualTo(ExportIssuesStep.dbToDumpMessageFormatting(messageFormattings.getMessageFormattingList()));
    assertThat(issue.getCodeVariants()).isEqualTo(issueDto.getCodeVariantsString());
    assertThat(issue.getPrioritizedRule()).isEqualTo(issueDto.isPrioritizedRule());
    assertThat(issue.getInternalTags()).isEqualTo(issueDto.getInternalTagsString());
  }

  @Test
  public void verify_two_issues_are_exported_including_one_without_software_quality() {
    IssueDto issueDto = createBaseIssueDto(readyRuleDto, SOME_PROJECT_UUID);
    IssueDto issueDto2 = createBaseIssueDto(readyRuleDto, SOME_PROJECT_UUID);
    issueDto2
      .addImpact(new ImpactDto().setSoftwareQuality(SoftwareQuality.MAINTAINABILITY).setSeverity(Severity.HIGH))
      .addImpact(new ImpactDto().setSoftwareQuality(SoftwareQuality.RELIABILITY).setSeverity(Severity.LOW));

    insertIssue(issueDto);
    insertIssue(issueDto2);

    underTest.execute(new TestComputationStepContext());
    List<ProjectDump.Issue> issuesInReport = dumpWriter.getWrittenMessagesOf(DumpElement.ISSUES);

    assertThat(issuesInReport).hasSize(2);
    assertThat(issuesInReport).filteredOn(i -> i.getImpactsList().size() == 2).hasSize(1);
    assertThat(issuesInReport).filteredOn(i -> i.getImpactsList().isEmpty()).hasSize(1);
  }

  @Test
  public void verify_mapping_of_nullable_numerical_fields_to_defaultValue() {
    insertIssue(readyRuleDto, SOME_PROJECT_UUID, STATUS_OPEN);

    underTest.execute(new TestComputationStepContext());

    ProjectDump.Issue issue = getWrittenIssue();

    assertThat(issue.getLine()).isEqualTo(DumpElement.ISSUES.NO_LINE);
    assertThat(issue.getGap()).isEqualTo(DumpElement.ISSUES.NO_GAP);
    assertThat(issue.getEffort()).isEqualTo(DumpElement.ISSUES.NO_EFFORT);
    assertThat(issue.getIssueCreatedAt()).isEqualTo(DumpElement.NO_DATETIME);
    assertThat(issue.getIssueUpdatedAt()).isEqualTo(DumpElement.NO_DATETIME);
    assertThat(issue.getIssueClosedAt()).isEqualTo(DumpElement.NO_DATETIME);
    assertThat(issue.hasRuleDescriptionContextKey()).isFalse();
  }

  @Test
  public void ruleRef_is_ref_provided_by_RuleRepository() {

    IssueDto issueDto = insertIssue(readyRuleDto, SOME_PROJECT_UUID, STATUS_OPEN);

    underTest.execute(new TestComputationStepContext());

    ProjectDump.Issue issue = getWrittenIssue();
    assertThat(issue.getRuleRef())
      .isEqualTo(ruleRepository.register(issueDto.getRuleUuid(), readyRuleDto.getKey()).ref());
  }

  @Test
  public void locations_is_not_set_in_protobuf_if_null_in_DB() {
    insertIssue(readyRuleDto, SOME_PROJECT_UUID, STATUS_OPEN);

    underTest.execute(new TestComputationStepContext());

    assertThat(getWrittenIssue().getLocations()).isEmpty();
  }

  @Test
  public void message_formattings_is_empty_in_protobuf_if_null_in_DB() {
    insertIssue(readyRuleDto, SOME_PROJECT_UUID, STATUS_OPEN);

    underTest.execute(new TestComputationStepContext());

    assertThat(getWrittenIssue().getMessageFormattingsList()).isEmpty();
  }

  @Test
  public void execute_fails_with_ISE_if_locations_cannot_be_parsed_to_protobuf() throws URISyntaxException, IOException {
    byte[] rubbishBytes = getRubbishBytes();
    String uuid = insertIssue(createBaseIssueDto(readyRuleDto, SOME_PROJECT_UUID).setLocations(rubbishBytes)).getKey();

    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Issue export failed after processing 0 issues successfully");
  }

  @Test
  public void execute_logs_number_total_exported_issue_count_when_successful() {
    insertIssue(createBaseIssueDto(readyRuleDto, SOME_PROJECT_UUID));
    insertIssue(createBaseIssueDto(readyRuleDto, SOME_PROJECT_UUID));
    insertIssue(createBaseIssueDto(readyRuleDto, SOME_PROJECT_UUID));

    underTest.execute(new TestComputationStepContext());

    assertThat(logTester.logs(Level.DEBUG)).contains("3 issues exported");
  }

  @Test
  public void execute_throws_ISE_with_number_of_successful_exports_before_failure() throws URISyntaxException, IOException {
    insertIssue(createBaseIssueDto(readyRuleDto, SOME_PROJECT_UUID));
    insertIssue(createBaseIssueDto(readyRuleDto, SOME_PROJECT_UUID));
    insertIssue(createBaseIssueDto(readyRuleDto, SOME_PROJECT_UUID).setLocations(getRubbishBytes())).getKey();

    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Issue export failed after processing 2 issues successfully");
  }

  private byte[] getRubbishBytes() throws IOException, URISyntaxException {
    return FileUtils.readFileToByteArray(new File(getClass().getResource("rubbish_data.txt").toURI()));
  }

  private ProjectDump.Issue getWrittenIssue() {
    return dumpWriter.getWrittenMessagesOf(DumpElement.ISSUES).get(0);
  }

  // private void expectExportFailure() {
  // expectExportFailure(0);
  // }

  // private void expectExportFailure(int i) {
  // expectedException.expect(IllegalStateException.class);
  // expectedException.expectMessage("Issue export failed after processing " + i + " issues successfully");
  // }

  private int issueUuidGenerator = 1;

  private IssueDto insertIssue(RuleDto ruleDto, String componentUuid, String status) {
    IssueDto dto = createBaseIssueDto(ruleDto, componentUuid, status);
    return insertIssue(dto);
  }

  private IssueDto insertIssue(IssueDto dto) {
    dbClient.issueDao().insert(dbSession, dto);
    dbSession.commit();
    return dto;
  }

  private ProjectDto createProject() {
    ComponentDto projectDto = dbTester.components().insertPrivateProject(c -> c.setKey(PROJECT_KEY).setUuid(SOME_PROJECT_UUID)).getMainBranchComponent();
    dbTester.commit();
    return dbTester.components().getProjectDtoByMainBranch(projectDto);
  }

  private IssueDto createBaseIssueDto(RuleDto ruleDto, String componentUuid) {
    return createBaseIssueDto(ruleDto, componentUuid, STATUS_OPEN);
  }

  private IssueDto createBaseIssueDto(RuleDto ruleDto, String componentUuid, String status) {
    return new IssueDto()
      .setKee("issue_uuid_" + issueUuidGenerator++)
      .setComponentUuid(componentUuid)
      .setProjectUuid(SOME_PROJECT_UUID)
      .setRuleUuid(ruleDto.getUuid())
      .setCreatedAt(System2.INSTANCE.now())
      .setStatus(status);
  }

  private RuleDto insertRule(String ruleKey1) {
    RuleDto dto = new RuleDto().setRepositoryKey(SOME_REPO).setScope(Scope.MAIN).setRuleKey(ruleKey1).setStatus(RuleStatus.READY);
    dbTester.rules().insert(dto);
    dbSession.commit();
    return dto;
  }
}
