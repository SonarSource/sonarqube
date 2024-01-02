/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.issue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ComponentUpdateDto;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.junit.Assert.assertFalse;
import static org.sonar.api.issue.Issue.RESOLUTION_FALSE_POSITIVE;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_WONT_FIX;
import static org.sonar.api.issue.Issue.STATUSES;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_REOPENED;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.issue.IssueTesting.newCodeReferenceIssue;
import static org.sonar.db.protobuf.DbIssues.MessageFormattingType.CODE;

public class IssueDaoTest {

  private static final String PROJECT_UUID = "prj_uuid";
  private static final String PROJECT_KEY = "prj_key";
  private static final String FILE_UUID = "file_uuid";
  private static final String FILE_KEY = "file_key";
  private static final RuleDto RULE = RuleTesting.newXooX1();
  private static final String ISSUE_KEY1 = "I1";
  private static final String ISSUE_KEY2 = "I2";
  private static final String TEST_CONTEXT_KEY = "test_context_key";

  private static final RuleType[] RULE_TYPES_EXCEPT_HOTSPOT = Stream.of(RuleType.values())
    .filter(r -> r != RuleType.SECURITY_HOTSPOT)
    .toArray(RuleType[]::new);
  private static final DbIssues.MessageFormattings MESSAGE_FORMATTING = DbIssues.MessageFormattings.newBuilder()
    .addMessageFormatting(DbIssues.MessageFormatting.newBuilder()
      .setStart(0)
      .setEnd(4)
      .setType(CODE)
      .build())
    .build();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final IssueDao underTest = db.getDbClient().issueDao();

  @Test
  public void selectByKeyOrFail() {
    prepareTables();

    IssueDto issue = underTest.selectOrFailByKey(db.getSession(), ISSUE_KEY1);
    assertThat(issue.getKee()).isEqualTo(ISSUE_KEY1);
    assertThat(issue.getComponentUuid()).isEqualTo(FILE_UUID);
    assertThat(issue.getProjectUuid()).isEqualTo(PROJECT_UUID);
    assertThat(issue.getRuleUuid()).isEqualTo(RULE.getUuid());
    assertThat(issue.getLanguage()).isEqualTo(RULE.getLanguage());
    assertThat(issue.getSeverity()).isEqualTo("BLOCKER");
    assertThat(issue.getType()).isEqualTo(2);
    assertThat(issue.isManualSeverity()).isFalse();
    assertThat(issue.getMessage()).isEqualTo("the message");
    assertThat(issue.parseMessageFormattings()).isEqualTo(MESSAGE_FORMATTING);
    assertThat(issue.getOptionalRuleDescriptionContextKey()).contains(TEST_CONTEXT_KEY);
    assertThat(issue.getLine()).isEqualTo(500);
    assertThat(issue.getEffort()).isEqualTo(10L);
    assertThat(issue.getGap()).isEqualTo(3.14);
    assertThat(issue.getStatus()).isEqualTo("RESOLVED");
    assertThat(issue.getResolution()).isEqualTo("FIXED");
    assertThat(issue.getChecksum()).isEqualTo("123456789");
    assertThat(issue.getAuthorLogin()).isEqualTo("morgan");
    assertThat(issue.getAssigneeUuid()).isEqualTo("karadoc");
    assertThat(issue.getIssueCreationDate()).isNotNull();
    assertThat(issue.getIssueUpdateDate()).isNotNull();
    assertThat(issue.getIssueCloseDate()).isNotNull();
    assertThat(issue.getCreatedAt()).isEqualTo(1_440_000_000_000L);
    assertThat(issue.getUpdatedAt()).isEqualTo(1_440_000_000_000L);
    assertThat(issue.getRuleRepo()).isEqualTo(RULE.getRepositoryKey());
    assertThat(issue.getRule()).isEqualTo(RULE.getRuleKey());
    assertThat(issue.getComponentKey()).isEqualTo(FILE_KEY);
    assertThat(issue.getProjectKey()).isEqualTo(PROJECT_KEY);
    assertThat(issue.getLocations()).isNull();
    assertThat(issue.parseLocations()).isNull();
    assertThat(issue.isExternal()).isTrue();
    assertFalse(issue.isQuickFixAvailable());
  }

  @Test
  public void selectByKeyOrFail_fails_if_key_not_found() {
    assertThatThrownBy(() -> {
      prepareTables();
      underTest.selectOrFailByKey(db.getSession(), "DOES_NOT_EXIST");
    })
      .isInstanceOf(RowNotFoundException.class)
      .hasMessage("Issue with key 'DOES_NOT_EXIST' does not exist");
  }

  @Test
  public void selectByKeys() {
    // contains I1 and I2
    prepareTables();

    List<IssueDto> issues = underTest.selectByKeys(db.getSession(), asList("I1", "I2", "I3"));
    // results are not ordered, so do not use "containsExactly"
    assertThat(issues).extracting("key").containsOnly("I1", "I2");
  }

  @Test
  public void selectIssueKeysByComponentUuid() {
    // contains I1 and I2
    prepareTables();

    Set<String> issues = underTest.selectIssueKeysByComponentUuid(db.getSession(), PROJECT_UUID);

    // results are not ordered, so do not use "containsExactly"
    assertThat(issues).containsOnly("I1", "I2");
  }

  @Test
  public void selectIssueKeysByComponentUuidFiltersAccordingly() {
    // contains I1 and I2
    prepareTables();

    // adds I3
    underTest.insert(db.getSession(), newIssueDto("I3")
      .setMessage("the message")
      .setRuleUuid(RULE.getUuid())
      .setComponentUuid(FILE_UUID)
      .setStatus("OPEN")
      .setProjectUuid(PROJECT_UUID));

    // Filter by including repositories
    Set<String> issues = underTest.selectIssueKeysByComponentUuid(db.getSession(), PROJECT_UUID, List.of("xoo"),
      emptyList(), emptyList(), 1);
    // results are not ordered, so do not use "containsExactly"
    assertThat(issues).containsOnly("I1", "I3");

    // Filter by excluding repositories
    issues = underTest.selectIssueKeysByComponentUuid(db.getSession(), PROJECT_UUID, emptyList(), List.of("xoo"),
      emptyList(), 1);
    assertThat(issues).isEmpty();

    // Filter by language
    issues = underTest.selectIssueKeysByComponentUuid(db.getSession(), PROJECT_UUID, emptyList(), emptyList(), List.of("xoo"), 1);
    assertThat(issues).containsOnly("I1", "I3");
  }

  @Test
  public void selectIssueKeysByComponentUuidAndChangedSinceFiltersAccordingly() {
    long t1 = 1_340_000_000_000L;
    long t2 = 1_400_000_000_000L;
    // contains I1 and I2
    prepareTables();
    // Insert I3, I4, where t1 < t2
    IntStream.range(3, 5).forEach(i -> underTest.insert(db.getSession(), newIssueDto("I" + i).setUpdatedAt(t1)));

    // Filter by including repositories
    Set<String> issues = underTest.selectIssueKeysByComponentUuidAndChangedSinceDate(db.getSession(), PROJECT_UUID, t2, List.of("xoo"),
      emptyList(), emptyList(), 1);
    // results are not ordered, so do not use "containsExactly"
    assertThat(issues).contains("I1");

    // Filter by excluding repositories
    issues = underTest.selectIssueKeysByComponentUuidAndChangedSinceDate(db.getSession(), PROJECT_UUID, t2,
      emptyList(), List.of("xoo"), emptyList(), 1);
    assertThat(issues).isEmpty();

    // Filter by language
    issues = underTest.selectIssueKeysByComponentUuidAndChangedSinceDate(db.getSession(), PROJECT_UUID, t2, emptyList(),
      emptyList(), List.of("xoo"), 1);
    assertThat(issues).contains("I1");
  }

  @Test
  public void selectByBranch() {
    long updatedAt = 1_340_000_000_000L;
    long changedSince = 1_000_000_000_000L;

    ComponentDto project = db.components().insertPrivateProject();
    RuleDto rule = db.rules().insert(r -> r.setRepositoryKey("java").setLanguage("java"));

    ComponentDto branchA = db.components().insertProjectBranch(project, b -> b.setKey("branchA"));
    ComponentDto fileA = db.components().insertComponent(newFileDto(branchA));

    List<String> statusesA = List.of(STATUS_OPEN, STATUS_REVIEWED, STATUS_CLOSED, STATUS_RESOLVED);
    IntStream.range(0, statusesA.size()).forEach(i -> insertBranchIssue(branchA, fileA, rule, "A" + i, statusesA.get(i), updatedAt));

    ComponentDto branchB = db.components().insertProjectBranch(project, b -> b.setKey("branchB"));
    ComponentDto fileB = db.components().insertComponent(newFileDto(branchB));

    List<String> statusesB = List.of(STATUS_OPEN, STATUS_RESOLVED);
    IntStream.range(0, statusesB.size()).forEach(i -> insertBranchIssue(branchB, fileB, rule, "B" + i, statusesB.get(i), updatedAt));

    List<IssueDto> branchAIssuesA1 = underTest.selectByBranch(db.getSession(), Set.of("issueA0", "issueA1", "issueA3"),
      buildSelectByBranchQuery(branchA, "java", false, changedSince));

    assertThat(branchAIssuesA1)
      .extracting(IssueDto::getKey, IssueDto::getStatus)
      .containsExactlyInAnyOrder(
        tuple("issueA0", STATUS_OPEN),
        tuple("issueA1", STATUS_REVIEWED),
        tuple("issueA3", STATUS_RESOLVED));

    assertThat(branchAIssuesA1.get(0))
      .extracting(IssueDto::getMessage, IssueDto::parseMessageFormattings)
      .containsOnly("message", MESSAGE_FORMATTING);

    List<IssueDto> branchAIssuesA2 = underTest.selectByBranch(db.getSession(), Set.of("issueA0", "issueA1", "issueA3"),
      buildSelectByBranchQuery(branchA, "java", true, changedSince));

    assertThat(branchAIssuesA2)
      .extracting(IssueDto::getKey, IssueDto::getStatus)
      .containsExactlyInAnyOrder(tuple("issueA0", STATUS_OPEN),
        tuple("issueA1", STATUS_REVIEWED),
        tuple("issueA3", STATUS_RESOLVED));

    List<IssueDto> branchBIssuesB1 = underTest.selectByBranch(db.getSession(), Set.of("issueB0", "issueB1"), buildSelectByBranchQuery(branchB, "java", false, changedSince));

    assertThat(branchBIssuesB1)
      .extracting(IssueDto::getKey, IssueDto::getStatus)
      .containsExactlyInAnyOrder(
        tuple("issueB0", STATUS_OPEN),
        tuple("issueB1", STATUS_RESOLVED));

    List<IssueDto> branchBIssuesB2 = underTest.selectByBranch(db.getSession(), Set.of("issueB0", "issueB1"), buildSelectByBranchQuery(branchB, "java", true, changedSince));

    assertThat(branchBIssuesB2)
      .extracting(IssueDto::getKey, IssueDto::getStatus)
      .containsExactlyInAnyOrder(tuple("issueB0", STATUS_OPEN),
        tuple("issueB1", STATUS_RESOLVED));
  }

  @Test
  public void selectByComponentUuidPaginated() {
    // contains I1 and I2
    prepareTables();

    List<IssueDto> issues = underTest.selectByComponentUuidPaginated(db.getSession(), PROJECT_UUID, 1);

    // results are not ordered, so do not use "containsExactly"
    assertThat(issues).extracting("key").containsOnly("I1", "I2");
  }

  @Test
  public void scrollNonClosedByComponentUuid() {
    RuleDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    IssueDto openIssue1OnFile = db.issues().insert(rule, project, file, i -> i.setStatus("OPEN").setResolution(null).setType(randomRuleTypeExceptHotspot()));
    IssueDto openIssue2OnFile = db.issues().insert(rule, project, file, i -> i.setStatus("OPEN").setResolution(null).setType(randomRuleTypeExceptHotspot()));
    IssueDto closedIssueOnFile = db.issues().insert(rule, project, file, i -> i.setStatus("CLOSED").setResolution("FIXED").setType(randomRuleTypeExceptHotspot()));
    IssueDto openIssueOnProject = db.issues().insert(rule, project, project, i -> i.setStatus("OPEN").setResolution(null).setType(randomRuleTypeExceptHotspot()));

    IssueDto securityHotspot = db.issues().insert(rule, project, file, i -> i.setType(RuleType.SECURITY_HOTSPOT));

    RuleDto external = db.rules().insert(ruleDefinitionDto -> ruleDefinitionDto.setIsExternal(true));
    IssueDto issueFromExteralruleOnFile = db.issues().insert(external, project, file, i -> i.setKee("ON_FILE_FROM_EXTERNAL").setType(randomRuleTypeExceptHotspot()));

    assertThat(underTest.selectNonClosedByComponentUuidExcludingExternalsAndSecurityHotspots(db.getSession(), file.uuid()))
      .extracting(IssueDto::getKey)
      .containsExactlyInAnyOrder(Arrays.stream(new IssueDto[] {openIssue1OnFile, openIssue2OnFile}).map(IssueDto::getKey).toArray(String[]::new));

    assertThat(underTest.selectNonClosedByComponentUuidExcludingExternalsAndSecurityHotspots(db.getSession(), project.uuid()))
      .extracting(IssueDto::getKey)
      .containsExactlyInAnyOrder(Arrays.stream(new IssueDto[] {openIssueOnProject}).map(IssueDto::getKey).toArray(String[]::new));

    assertThat(underTest.selectNonClosedByComponentUuidExcludingExternalsAndSecurityHotspots(db.getSession(), "does_not_exist")).isEmpty();
  }

  @Test
  public void scrollNonClosedByModuleOrProject() {
    RuleDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto anotherProject = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    ComponentDto file = db.components().insertComponent(newFileDto(module));
    IssueDto openIssue1OnFile = db.issues().insert(rule, project, file, i -> i.setStatus("OPEN").setResolution(null).setType(randomRuleTypeExceptHotspot()));
    IssueDto openIssue2OnFile = db.issues().insert(rule, project, file, i -> i.setStatus("OPEN").setResolution(null).setType(randomRuleTypeExceptHotspot()));
    IssueDto closedIssueOnFile = db.issues().insert(rule, project, file, i -> i.setStatus("CLOSED").setResolution("FIXED").setType(randomRuleTypeExceptHotspot()));
    IssueDto openIssueOnModule = db.issues().insert(rule, project, module, i -> i.setStatus("OPEN").setResolution(null).setType(randomRuleTypeExceptHotspot()));
    IssueDto openIssueOnProject = db.issues().insert(rule, project, project, i -> i.setStatus("OPEN").setResolution(null).setType(randomRuleTypeExceptHotspot()));
    IssueDto openIssueOnAnotherProject = db.issues().insert(rule, anotherProject, anotherProject,
      i -> i.setStatus("OPEN").setResolution(null).setType(randomRuleTypeExceptHotspot()));

    IssueDto securityHotspot = db.issues().insert(rule, project, file, i -> i.setType(RuleType.SECURITY_HOTSPOT));

    RuleDto external = db.rules().insert(ruleDefinitionDto -> ruleDefinitionDto.setIsExternal(true));
    IssueDto issueFromExteralruleOnFile = db.issues().insert(external, project, file, i -> i.setKee("ON_FILE_FROM_EXTERNAL").setType(randomRuleTypeExceptHotspot()));

    assertThat(underTest.selectNonClosedByModuleOrProjectExcludingExternalsAndSecurityHotspots(db.getSession(), project))
      .extracting(IssueDto::getKey)
      .containsExactlyInAnyOrder(
        Arrays.stream(new IssueDto[] {openIssue1OnFile, openIssue2OnFile, openIssueOnModule, openIssueOnProject}).map(IssueDto::getKey).toArray(String[]::new));

    assertThat(underTest.selectNonClosedByModuleOrProjectExcludingExternalsAndSecurityHotspots(db.getSession(), module))
      .extracting(IssueDto::getKey)
      .containsExactlyInAnyOrder(Arrays.stream(new IssueDto[] {openIssue1OnFile, openIssue2OnFile, openIssueOnModule}).map(IssueDto::getKey).toArray(String[]::new));

    ComponentDto notPersisted = ComponentTesting.newPrivateProjectDto();
    assertThat(underTest.selectNonClosedByModuleOrProjectExcludingExternalsAndSecurityHotspots(db.getSession(), notPersisted)).isEmpty();
  }

  @Test
  public void selectOpenByComponentUuid() {
    RuleDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto projectBranch = db.components().insertProjectBranch(project,
      b -> b.setKey("feature/foo")
        .setBranchType(BranchType.BRANCH));

    ComponentDto file = db.components().insertComponent(newFileDto(projectBranch));

    IssueDto openIssue = db.issues().insert(rule, projectBranch, file, i -> i.setStatus(STATUS_OPEN).setResolution(null));
    IssueDto closedIssue = db.issues().insert(rule, projectBranch, file, i -> i.setStatus(STATUS_CLOSED).setResolution(RESOLUTION_FIXED));
    IssueDto reopenedIssue = db.issues().insert(rule, projectBranch, file, i -> i.setStatus(STATUS_REOPENED).setResolution(null));
    IssueDto confirmedIssue = db.issues().insert(rule, projectBranch, file, i -> i.setStatus(STATUS_CONFIRMED).setResolution(null));
    IssueDto wontfixIssue = db.issues().insert(rule, projectBranch, file, i -> i.setStatus(STATUS_RESOLVED).setResolution(RESOLUTION_WONT_FIX));
    IssueDto fpIssue = db.issues().insert(rule, projectBranch, file, i -> i.setStatus(STATUS_RESOLVED).setResolution(RESOLUTION_FALSE_POSITIVE));

    assertThat(underTest.selectOpenByComponentUuids(db.getSession(), Collections.singletonList(file.uuid())))
      .extracting("kee")
      .containsOnly(openIssue.getKey(), reopenedIssue.getKey(), confirmedIssue.getKey(), wontfixIssue.getKey(), fpIssue.getKey());
  }

  @Test
  public void selectOpenByComponentUuid_should_correctly_map_required_fields() {
    RuleDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto projectBranch = db.components().insertProjectBranch(project,
      b -> b.setKey("feature/foo")
        .setBranchType(BranchType.BRANCH));

    ComponentDto file = db.components().insertComponent(newFileDto(projectBranch));
    IssueDto fpIssue = db.issues().insert(rule, projectBranch, file, i -> i.setStatus("RESOLVED").setResolution("FALSE-POSITIVE"));

    PrIssueDto fp = underTest.selectOpenByComponentUuids(db.getSession(), Collections.singletonList(file.uuid())).get(0);
    assertThat(fp.getLine()).isEqualTo(fpIssue.getLine());
    assertThat(fp.getMessage()).isEqualTo(fpIssue.getMessage());
    assertThat(fp.getChecksum()).isEqualTo(fpIssue.getChecksum());
    assertThat(fp.getRuleKey()).isEqualTo(fpIssue.getRuleKey());
    assertThat(fp.getStatus()).isEqualTo(fpIssue.getStatus());

    assertThat(fp.getLine()).isNotNull();
    assertThat(fp.getLine()).isNotZero();
    assertThat(fp.getMessage()).isNotNull();
    assertThat(fp.getChecksum()).isNotNull();
    assertThat(fp.getChecksum()).isNotEmpty();
    assertThat(fp.getRuleKey()).isNotNull();
    assertThat(fp.getStatus()).isNotNull();
    assertThat(fp.getBranchKey()).isEqualTo("feature/foo");
    assertThat(fp.getIssueUpdateDate()).isNotNull();
  }

  @Test
  public void test_selectIssueGroupsByComponent_on_component_without_issues() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));

    Collection<IssueGroupDto> groups = underTest.selectIssueGroupsByComponent(db.getSession(), file, 1_000L);

    assertThat(groups).isEmpty();
  }

  @Test
  public void selectByKey_givenOneIssueWithQuickFix_selectOneIssueWithQuickFix() {
    prepareIssuesComponent();
    underTest.insert(db.getSession(), newIssueDto(ISSUE_KEY1)
      .setMessage("the message")
      .setRuleUuid(RULE.getUuid())
      .setComponentUuid(FILE_UUID)
      .setProjectUuid(PROJECT_UUID)
      .setQuickFixAvailable(true));

    IssueDto issue = underTest.selectOrFailByKey(db.getSession(), ISSUE_KEY1);

    assertThat(issue.getKee()).isEqualTo(ISSUE_KEY1);
    assertThat(issue.isQuickFixAvailable()).isTrue();
  }

  @Test
  public void selectByKey_givenOneIssueWithoutQuickFix_selectOneIssueWithoutQuickFix() {
    prepareIssuesComponent();
    String issueKey = ISSUE_KEY1;
    underTest.insert(db.getSession(), createIssueWithKey(issueKey));

    IssueDto issue = underTest.selectOrFailByKey(db.getSession(), ISSUE_KEY1);

    assertThat(issue.getKee()).isEqualTo(ISSUE_KEY1);
    assertThat(issue.isQuickFixAvailable()).isFalse();
  }

  @Test
  public void selectIssueGroupsByComponent_on_file() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));
    RuleDto rule = db.rules().insert();
    IssueDto fpBug = db.issues().insert(rule, project, file,
      i -> i.setStatus("RESOLVED").setResolution("FALSE-POSITIVE").setSeverity("MAJOR").setType(RuleType.BUG).setIssueCreationTime(1_500L));
    IssueDto criticalBug1 = db.issues().insert(rule, project, file,
      i -> i.setStatus("OPEN").setResolution(null).setSeverity("CRITICAL").setType(RuleType.BUG).setIssueCreationTime(1_600L));
    IssueDto criticalBug2 = db.issues().insert(rule, project, file,
      i -> i.setStatus("OPEN").setResolution(null).setSeverity("CRITICAL").setType(RuleType.BUG).setIssueCreationTime(1_700L));
    // closed issues are ignored
    IssueDto closed = db.issues().insert(rule, project, file,
      i -> i.setStatus("CLOSED").setResolution("REMOVED").setSeverity("CRITICAL").setType(RuleType.BUG).setIssueCreationTime(1_700L));

    Collection<IssueGroupDto> result = underTest.selectIssueGroupsByComponent(db.getSession(), file, 1_000L);

    assertThat(result.stream().mapToLong(IssueGroupDto::getCount).sum()).isEqualTo(3);

    assertThat(result.stream().filter(g -> g.getRuleType() == RuleType.BUG.getDbConstant()).mapToLong(IssueGroupDto::getCount).sum()).isEqualTo(3);
    assertThat(result.stream().filter(g -> g.getRuleType() == RuleType.CODE_SMELL.getDbConstant()).mapToLong(IssueGroupDto::getCount).sum()).isZero();
    assertThat(result.stream().filter(g -> g.getRuleType() == RuleType.VULNERABILITY.getDbConstant()).mapToLong(IssueGroupDto::getCount).sum()).isZero();

    assertThat(result.stream().filter(g -> g.getSeverity().equals("CRITICAL")).mapToLong(IssueGroupDto::getCount).sum()).isEqualTo(2);
    assertThat(result.stream().filter(g -> g.getSeverity().equals("MAJOR")).mapToLong(IssueGroupDto::getCount).sum()).isOne();
    assertThat(result.stream().filter(g -> g.getSeverity().equals("MINOR")).mapToLong(IssueGroupDto::getCount).sum()).isZero();

    assertThat(result.stream().filter(g -> g.getStatus().equals("OPEN")).mapToLong(IssueGroupDto::getCount).sum()).isEqualTo(2);
    assertThat(result.stream().filter(g -> g.getStatus().equals("RESOLVED")).mapToLong(IssueGroupDto::getCount).sum()).isOne();
    assertThat(result.stream().filter(g -> g.getStatus().equals("CLOSED")).mapToLong(IssueGroupDto::getCount).sum()).isZero();

    assertThat(result.stream().filter(g -> "FALSE-POSITIVE".equals(g.getResolution())).mapToLong(IssueGroupDto::getCount).sum()).isOne();
    assertThat(result.stream().filter(g -> g.getResolution() == null).mapToLong(IssueGroupDto::getCount).sum()).isEqualTo(2);

    assertThat(result.stream().filter(IssueGroupDto::isInLeak).mapToLong(IssueGroupDto::getCount).sum()).isEqualTo(3);
    assertThat(result.stream().filter(g -> !g.isInLeak()).mapToLong(IssueGroupDto::getCount).sum()).isZero();

    // test leak
    result = underTest.selectIssueGroupsByComponent(db.getSession(), file, 999_999_999L);
    assertThat(result.stream().filter(IssueGroupDto::isInLeak).mapToLong(IssueGroupDto::getCount).sum()).isZero();
    assertThat(result.stream().filter(g -> !g.isInLeak()).mapToLong(IssueGroupDto::getCount).sum()).isEqualTo(3);

    // test leak using exact creation time of criticalBug2 issue
    result = underTest.selectIssueGroupsByComponent(db.getSession(), file, criticalBug2.getIssueCreationTime());
    assertThat(result.stream().filter(IssueGroupDto::isInLeak).mapToLong(IssueGroupDto::getCount).sum()).isZero();
    assertThat(result.stream().filter(g -> !g.isInLeak()).mapToLong(IssueGroupDto::getCount).sum()).isEqualTo(3);
  }

  @Test
  public void selectGroupsOfComponentTreeOnLeak_on_file_new_code_reference_branch() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));
    RuleDto rule = db.rules().insert();
    IssueDto fpBug = db.issues().insert(rule, project, file,
      i -> i.setStatus("RESOLVED").setResolution("FALSE-POSITIVE").setSeverity("MAJOR").setType(RuleType.BUG));
    IssueDto criticalBug1 = db.issues().insert(rule, project, file,
      i -> i.setStatus("OPEN").setResolution(null).setSeverity("CRITICAL").setType(RuleType.BUG));
    IssueDto criticalBug2 = db.issues().insert(rule, project, file,
      i -> i.setStatus("OPEN").setResolution(null).setSeverity("CRITICAL").setType(RuleType.BUG));

    db.issues().insert(rule, project, file,
      i -> i.setStatus("OPEN").setResolution(null).setSeverity("CRITICAL").setType(RuleType.BUG));

    // two issues part of new code period on reference branch
    db.issues().insertNewCodeReferenceIssue(fpBug);
    db.issues().insertNewCodeReferenceIssue(criticalBug1);
    db.issues().insertNewCodeReferenceIssue(criticalBug2);

    Collection<IssueGroupDto> result = underTest.selectIssueGroupsByComponent(db.getSession(), file, -1);

    assertThat(result.stream().mapToLong(IssueGroupDto::getCount).sum()).isEqualTo(4);

    assertThat(result.stream().filter(g -> g.getRuleType() == RuleType.BUG.getDbConstant()).mapToLong(IssueGroupDto::getCount).sum()).isEqualTo(4);
    assertThat(result.stream().filter(g -> g.getRuleType() == RuleType.CODE_SMELL.getDbConstant()).mapToLong(IssueGroupDto::getCount).sum()).isZero();
    assertThat(result.stream().filter(g -> g.getRuleType() == RuleType.VULNERABILITY.getDbConstant()).mapToLong(IssueGroupDto::getCount).sum()).isZero();

    assertThat(result.stream().filter(g -> g.getSeverity().equals("CRITICAL")).mapToLong(IssueGroupDto::getCount).sum()).isEqualTo(3);
    assertThat(result.stream().filter(g -> g.getSeverity().equals("MAJOR")).mapToLong(IssueGroupDto::getCount).sum()).isOne();
    assertThat(result.stream().filter(g -> g.getSeverity().equals("MINOR")).mapToLong(IssueGroupDto::getCount).sum()).isZero();

    assertThat(result.stream().filter(g -> g.getStatus().equals("OPEN")).mapToLong(IssueGroupDto::getCount).sum()).isEqualTo(3);
    assertThat(result.stream().filter(g -> g.getStatus().equals("RESOLVED")).mapToLong(IssueGroupDto::getCount).sum()).isOne();
    assertThat(result.stream().filter(g -> g.getStatus().equals("CLOSED")).mapToLong(IssueGroupDto::getCount).sum()).isZero();

    assertThat(result.stream().filter(g -> "FALSE-POSITIVE".equals(g.getResolution())).mapToLong(IssueGroupDto::getCount).sum()).isOne();
    assertThat(result.stream().filter(g -> g.getResolution() == null).mapToLong(IssueGroupDto::getCount).sum()).isEqualTo(3);

    assertThat(result.stream().filter(IssueGroupDto::isInLeak).mapToLong(IssueGroupDto::getCount).sum()).isEqualTo(3);
    assertThat(result.stream().filter(g -> !g.isInLeak()).mapToLong(IssueGroupDto::getCount).sum()).isOne();
  }

  @Test
  public void selectModuleAndDirComponentUuidsOfOpenIssuesForProjectUuid() {
    assertThat(underTest.selectModuleAndDirComponentUuidsOfOpenIssuesForProjectUuid(db.getSession(), randomAlphabetic(12)))
      .isEmpty();

    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto module11 = db.components().insertComponent(newModuleDto(project1));
    ComponentDto dir11 = db.components().insertComponent(newDirectory(module11, randomAlphabetic(10)));
    ComponentDto dir12 = db.components().insertComponent(newDirectory(module11, randomAlphabetic(11)));
    ComponentDto module12 = db.components().insertComponent(newModuleDto(project1));
    ComponentDto dir13 = db.components().insertComponent(newDirectory(module12, randomAlphabetic(12)));
    ComponentDto dir14 = db.components().insertComponent(newDirectory(project1, randomAlphabetic(13)));
    ComponentDto file11 = db.components().insertComponent(newFileDto(project1));
    ComponentDto application = db.components().insertPrivateApplication();
    ComponentDto view = db.components().insertPublicPortfolio();
    ComponentDto subview = db.components().insertSubView(view);
    ComponentDto project2 = db.components().insertPublicProject();
    ComponentDto module21 = db.components().insertComponent(newModuleDto(project2));
    ComponentDto dir21 = db.components().insertComponent(newDirectory(project2, randomAlphabetic(15)));
    ComponentDto file21 = db.components().insertComponent(newFileDto(project2));
    List<ComponentDto> allcomponents = asList(project1, module11, dir11, dir12, module12, dir13, dir14, file11, application, view, subview, project2, module21, dir21, file21);
    List<ComponentDto> allModuleOrDirs = asList(module11, dir11, dir12, module12, dir13, dir14, module21, dir21);

    // no issues => always empty
    allcomponents.stream()
      .map(ComponentDto::uuid)
      .forEach(uuid -> assertThat(underTest.selectModuleAndDirComponentUuidsOfOpenIssuesForProjectUuid(db.getSession(), uuid))
        .isEmpty());

    // return module or dir only if has issue with status different from CLOSED
    allModuleOrDirs
      .forEach(moduleOrDir -> {
        String projectUuid = moduleOrDir.branchUuid();
        // CLOSED issue => not returned
        db.issues().insertIssue(t -> t.setProjectUuid(projectUuid).setComponent(moduleOrDir).setStatus(STATUS_CLOSED));
        assertThat(underTest.selectModuleAndDirComponentUuidsOfOpenIssuesForProjectUuid(db.getSession(), projectUuid))
          .isEmpty();

        // status != CLOSED => returned
        STATUSES.stream()
          .filter(t -> !STATUS_CLOSED.equals(t))
          .forEach(status -> {
            IssueDto issue = db.issues().insertIssue(t -> t.setProjectUuid(projectUuid).setComponent(moduleOrDir).setStatus(status));
            assertThat(underTest.selectModuleAndDirComponentUuidsOfOpenIssuesForProjectUuid(db.getSession(), projectUuid))
              .containsOnly(moduleOrDir.uuid());

            db.executeDdl("delete from issues where kee='" + issue.getKey() + "'");
            db.commit();
            assertThat(underTest.selectModuleAndDirComponentUuidsOfOpenIssuesForProjectUuid(db.getSession(), projectUuid))
              .isEmpty();
          });
      });

    // never return project, view, subview, app or file, whatever the issue status
    Stream.of(project1, file11, application, view, subview, project2, file21)
      .forEach(neitherModuleNorDir -> {
        String projectUuid = neitherModuleNorDir.branchUuid();
        STATUSES
          .forEach(status -> {
            db.issues().insertIssue(t -> t.setProjectUuid(projectUuid).setComponent(neitherModuleNorDir).setStatus(status));
            assertThat(underTest.selectModuleAndDirComponentUuidsOfOpenIssuesForProjectUuid(db.getSession(), projectUuid))
              .isEmpty();
          });
      });

    // never return whatever the component if it is disabled
    allcomponents
      .forEach(component -> {
        String projectUuid = component.branchUuid();

        // issues for each status => returned if component is dir or module
        STATUSES
          .forEach(status -> db.issues().insertIssue(t -> t.setProjectUuid(projectUuid).setComponent(component).setStatus(status)));
        if (allModuleOrDirs.contains(component)) {
          assertThat(underTest.selectModuleAndDirComponentUuidsOfOpenIssuesForProjectUuid(db.getSession(), projectUuid))
            .containsOnly(component.uuid());
        } else {
          assertThat(underTest.selectModuleAndDirComponentUuidsOfOpenIssuesForProjectUuid(db.getSession(), projectUuid))
            .isEmpty();
        }

        // disable component and test again => not returned anymore
        db.getDbClient().componentDao().update(db.getSession(), ComponentUpdateDto.copyFrom(component).setBEnabled(false).setBChanged(true), component.qualifier());
        db.getDbClient().componentDao().applyBChangesForRootComponentUuid(db.getSession(), projectUuid);
        db.commit();
        assertThat(db.getDbClient().componentDao().selectByUuid(db.getSession(), component.uuid()).get().isEnabled())
          .isFalse();
        assertThat(underTest.selectModuleAndDirComponentUuidsOfOpenIssuesForProjectUuid(db.getSession(), projectUuid))
          .isEmpty();
      });
  }

  @Test
  public void selectByKey_givenOneIssueNewOnReferenceBranch_selectOneIssueWithNewOnReferenceBranch() {
    prepareIssuesComponent();
    underTest.insert(db.getSession(), newIssueDto(ISSUE_KEY1)
      .setMessage("the message")
      .setRuleUuid(RULE.getUuid())
      .setComponentUuid(FILE_UUID)
      .setProjectUuid(PROJECT_UUID)
      .setQuickFixAvailable(true));
    underTest.insert(db.getSession(), newIssueDto(ISSUE_KEY2)
      .setMessage("the message")
      .setRuleUuid(RULE.getUuid())
      .setComponentUuid(FILE_UUID)
      .setProjectUuid(PROJECT_UUID)
      .setQuickFixAvailable(true));
    IssueDto issue1 = underTest.selectOrFailByKey(db.getSession(), ISSUE_KEY1);
    IssueDto issue2 = underTest.selectOrFailByKey(db.getSession(), ISSUE_KEY2);

    assertThat(issue1.isNewCodeReferenceIssue()).isFalse();
    assertThat(issue2.isNewCodeReferenceIssue()).isFalse();

    underTest.insertAsNewCodeOnReferenceBranch(db.getSession(), newCodeReferenceIssue(issue1));

    assertThat(underTest.selectOrFailByKey(db.getSession(), ISSUE_KEY1).isNewCodeReferenceIssue()).isTrue();
    assertThat(underTest.selectOrFailByKey(db.getSession(), ISSUE_KEY2).isNewCodeReferenceIssue()).isFalse();

    underTest.deleteAsNewCodeOnReferenceBranch(db.getSession(), ISSUE_KEY1);
    assertThat(underTest.selectOrFailByKey(db.getSession(), ISSUE_KEY1).isNewCodeReferenceIssue()).isFalse();
  }

  @Test
  public void selectByKey_givenOneIssueWithoutRuleDescriptionContextKey_returnsEmptyOptional() {
    prepareIssuesComponent();
    underTest.insert(db.getSession(), createIssueWithKey(ISSUE_KEY1)
      .setRuleDescriptionContextKey(null));
    IssueDto issue1 = underTest.selectOrFailByKey(db.getSession(), ISSUE_KEY1);

    assertThat(issue1.getOptionalRuleDescriptionContextKey()).isEmpty();
  }

  @Test
  public void selectByKey_givenOneIssueWithRuleDescriptionContextKey_returnsContextKey() {
    prepareIssuesComponent();
    underTest.insert(db.getSession(), createIssueWithKey(ISSUE_KEY1)
      .setRuleDescriptionContextKey(TEST_CONTEXT_KEY));

    IssueDto issue1 = underTest.selectOrFailByKey(db.getSession(), ISSUE_KEY1);

    assertThat(issue1.getOptionalRuleDescriptionContextKey()).contains(TEST_CONTEXT_KEY);
  }

  @Test
  public void update_whenUpdatingRuleDescriptionContextKeyToNull_returnsEmptyContextKey() {
    prepareIssuesComponent();
    IssueDto issue = createIssueWithKey(ISSUE_KEY1).setRuleDescriptionContextKey(TEST_CONTEXT_KEY);
    underTest.insert(db.getSession(), issue);

    issue.setRuleDescriptionContextKey(null);
    underTest.update(db.getSession(), issue);

    issue = underTest.selectOrFailByKey(db.getSession(), ISSUE_KEY1);
    assertThat(issue.getOptionalRuleDescriptionContextKey()).isEmpty();
  }

  @Test
  public void update_whenUpdatingRuleDescriptionContextKeyToNotNull_returnsContextKey() {
    prepareIssuesComponent();
    IssueDto issue = createIssueWithKey(ISSUE_KEY1).setRuleDescriptionContextKey(null);
    underTest.insert(db.getSession(), issue);

    issue.setRuleDescriptionContextKey(TEST_CONTEXT_KEY);
    underTest.update(db.getSession(), issue);

    issue = underTest.selectOrFailByKey(db.getSession(), ISSUE_KEY1);
    assertThat(issue.getOptionalRuleDescriptionContextKey()).contains(TEST_CONTEXT_KEY);
  }

  @Test
  public void update_givenOneIssueWithoutRuleDescriptionContextKey_returnsContextKey() {
    prepareIssuesComponent();
    IssueDto issue = createIssueWithKey(ISSUE_KEY1).setRuleDescriptionContextKey(TEST_CONTEXT_KEY);
    underTest.insert(db.getSession(), issue);

    issue.setRuleDescriptionContextKey(null);
    underTest.update(db.getSession(), issue);

    issue = underTest.selectOrFailByKey(db.getSession(), ISSUE_KEY1);
    assertThat(issue.getOptionalRuleDescriptionContextKey()).isEmpty();

    issue.setRuleDescriptionContextKey(TEST_CONTEXT_KEY);
    underTest.update(db.getSession(), issue);

    issue = underTest.selectOrFailByKey(db.getSession(), ISSUE_KEY1);
    assertThat(issue.getOptionalRuleDescriptionContextKey()).contains(TEST_CONTEXT_KEY);
  }

  private static IssueDto createIssueWithKey(String issueKey) {
    return newIssueDto(issueKey)
      .setMessage("the message")
      .setRuleUuid(RULE.getUuid())
      .setComponentUuid(FILE_UUID)
      .setProjectUuid(PROJECT_UUID)
      .setQuickFixAvailable(false);
  }

  private static IssueDto newIssueDto(String key) {
    IssueDto dto = new IssueDto();
    dto.setComponent(new ComponentDto().setKey("struts:Action").setUuid("component-uuid"));
    dto.setProject(new ComponentDto().setKey("struts").setUuid("project-uuid"));
    dto.setRule(RuleTesting.newRule(RuleKey.of("java", "S001")).setUuid("uuid-200"));
    dto.setKee(key);
    dto.setType(2);
    dto.setLine(500);
    dto.setGap(3.14);
    dto.setEffort(10L);
    dto.setResolution("FIXED");
    dto.setStatus("RESOLVED");
    dto.setSeverity("BLOCKER");
    dto.setAuthorLogin("morgan");
    dto.setAssigneeUuid("karadoc");
    dto.setChecksum("123456789");
    dto.setMessage("the message");
    dto.setMessageFormattings(MESSAGE_FORMATTING);
    dto.setRuleDescriptionContextKey(TEST_CONTEXT_KEY);
    dto.setCreatedAt(1_440_000_000_000L);
    dto.setUpdatedAt(1_440_000_000_000L);
    dto.setIssueCreationTime(1_450_000_000_000L);
    dto.setIssueUpdateTime(1_450_000_000_000L);
    dto.setIssueCloseTime(1_450_000_000_000L);
    return dto;
  }

  private void prepareIssuesComponent() {
    db.rules().insert(RULE.setIsExternal(true));
    ComponentDto projectDto = db.components().insertPrivateProject(t -> t.setUuid(PROJECT_UUID).setKey(PROJECT_KEY));
    db.components().insertComponent(newFileDto(projectDto).setUuid(FILE_UUID).setKey(FILE_KEY));
  }

  private void prepareTables() {
    prepareIssuesComponent();
    underTest.insert(db.getSession(), newIssueDto(ISSUE_KEY1)
      .setMessage("the message")
      .setRuleUuid(RULE.getUuid())
      .setComponentUuid(FILE_UUID)
      .setProjectUuid(PROJECT_UUID));
    underTest.insert(db.getSession(), newIssueDto(ISSUE_KEY2)
      .setRuleUuid(RULE.getUuid())
      .setComponentUuid(FILE_UUID)
      .setStatus("CLOSED")
      .setProjectUuid(PROJECT_UUID));
    db.getSession().commit();
  }

  private static RuleType randomRuleTypeExceptHotspot() {
    return RULE_TYPES_EXCEPT_HOTSPOT[nextInt(RULE_TYPES_EXCEPT_HOTSPOT.length)];
  }

  private void insertBranchIssue(ComponentDto branch, ComponentDto file, RuleDto rule, String id, String status, Long updateAt) {
    db.issues().insert(rule, branch, file, i -> i.setKee("issue" + id).setStatus(status).setUpdatedAt(updateAt).setType(randomRuleTypeExceptHotspot())
      .setMessage("message")
      .setMessageFormattings(MESSAGE_FORMATTING));
  }

  private static IssueQueryParams buildSelectByBranchQuery(ComponentDto branch, String language, boolean resolvedOnly, Long changedSince) {
    return new IssueQueryParams(branch.uuid(), List.of(language), List.of(), List.of(), resolvedOnly, changedSince);
  }
}
