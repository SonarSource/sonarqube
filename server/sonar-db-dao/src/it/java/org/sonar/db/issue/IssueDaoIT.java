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

import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.ibatis.cursor.Cursor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.Pagination;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.dependency.CveDto;
import org.sonar.db.dependency.IssuesDependencyDto;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.db.user.UserDto;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.sonar.api.issue.Issue.RESOLUTION_FALSE_POSITIVE;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_WONT_FIX;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_REOPENED;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.api.issue.impact.Severity.BLOCKER;
import static org.sonar.api.issue.impact.Severity.HIGH;
import static org.sonar.api.issue.impact.Severity.INFO;
import static org.sonar.api.issue.impact.Severity.LOW;
import static org.sonar.api.issue.impact.Severity.MEDIUM;
import static org.sonar.api.issue.impact.SoftwareQuality.MAINTAINABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.RELIABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.SECURITY;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.issue.IssueListQuery.IssueListQueryBuilder.newIssueListQueryBuilder;
import static org.sonar.db.issue.IssueTesting.generateIssues;
import static org.sonar.db.issue.IssueTesting.newCodeReferenceIssue;
import static org.sonar.db.protobuf.DbIssues.MessageFormattingType.CODE;

class IssueDaoIT {

  private static final String PROJECT_UUID = "prj_uuid";
  private static final String PROJECT_KEY = "prj_key";
  private static final String FILE_UUID = "file_uuid";
  private static final String FILE_KEY = "file_key";
  private static final RuleDto RULE = RuleTesting.newXooX1();
  private static final String ISSUE_KEY1 = "I1";
  private static final String ISSUE_KEY2 = "I2";
  private static final String TEST_CONTEXT_KEY = "test_context_key";
  private static final String USER_LOGIN = "user_login";

  private static final Random RANDOM = new SecureRandom();

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

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final IssueDao underTest = db.getDbClient().issueDao();

  private ComponentDto projectDto;
  private UserDto userDto;

  @BeforeEach
  void setup() {
    int i = db.countSql(db.getSession(), "select count(1) from rules_default_impacts");

    db.rules().insert(RULE.setIsExternal(true));
    projectDto = db.components().insertPrivateProject(t -> t.setUuid(PROJECT_UUID).setKey(PROJECT_KEY)).getMainBranchComponent();
    db.components().insertComponent(newFileDto(projectDto).setUuid(FILE_UUID).setKey(FILE_KEY));
    userDto = db.users().insertUser(USER_LOGIN);
  }

  @Test
  void selectByKeyOrFail() {
    prepareTables();
    IssueDto expected = new IssueDto()
      .setKee(ISSUE_KEY1)
      .setComponentUuid(FILE_UUID)
      .setProjectUuid(PROJECT_UUID)
      .setRuleUuid(RULE.getUuid())
      .setLanguage(Optional.ofNullable(RULE.getLanguage()).orElseGet(() -> fail("Rule language should not be null here")))
      .setSeverity("BLOCKER")
      .setType(2)
      .setManualSeverity(false)
      .setMessage("the message")
      .setRuleDescriptionContextKey(TEST_CONTEXT_KEY)
      .setRuleCleanCodeAttribute(RULE.getCleanCodeAttribute())
      .setLine(500)
      .setEffort(10L)
      .setGap(3.14)
      .setStatus("RESOLVED")
      .setResolution("FIXED")
      .setChecksum("123456789")
      .setAuthorLogin("morgan")
      .setAssigneeUuid(userDto.getUuid())
      .setAssigneeLogin(USER_LOGIN)
      .setCreatedAt(1_440_000_000_000L)
      .setUpdatedAt(1_440_000_000_000L)
      .setRule(RULE)
      .setComponentKey(FILE_KEY)
      .setProjectKey(PROJECT_KEY)
      .setExternal(true)
      .setTags(List.of("tag1", "tag2"))
      .setCodeVariants(List.of("variant1", "variant2"))
      .setQuickFixAvailable(false)
      .setMessageFormattings(MESSAGE_FORMATTING);

    IssueDto issue = underTest.selectOrFailByKey(db.getSession(), ISSUE_KEY1);

    assertThat(issue).usingRecursiveComparison()
      .ignoringFields("filePath", "issueCreationDate", "issueUpdateDate", "issueCloseDate", "cleanCodeAttribute", "impacts",
        "ruleDefaultImpacts")
      .isEqualTo(expected);
    assertThat(issue.parseMessageFormattings()).isEqualTo(MESSAGE_FORMATTING);
    assertThat(issue.getIssueCreationDate()).isNotNull();
    assertThat(issue.getIssueUpdateDate()).isNotNull();
    assertThat(issue.getIssueCloseDate()).isNotNull();
    assertThat(issue.getRuleRepo()).isEqualTo(RULE.getRepositoryKey());
    assertThat(issue.getRule()).isEqualTo(RULE.getRuleKey());
    assertThat(issue.getEffectiveCleanCodeAttribute()).isEqualTo(RULE.getCleanCodeAttribute());
    assertThat(issue.parseLocations()).isNull();
    assertThat(issue.getImpacts())
      .extracting(ImpactDto::getSeverity, ImpactDto::getSoftwareQuality, ImpactDto::isManualSeverity)
      .containsExactlyInAnyOrder(
        tuple(MEDIUM, RELIABILITY, false),
        tuple(LOW, SECURITY, false));
    assertThat(issue.getRuleDefaultImpacts())
      .extracting(ImpactDto::getSeverity, ImpactDto::getSoftwareQuality)
      .containsExactlyInAnyOrder(tuple(HIGH, MAINTAINABILITY));
  }

  @Test
  void selectByKeyOrFail_fails_if_key_not_found() {
    prepareTables();
    DbSession session = db.getSession();
    assertThatThrownBy(() -> underTest.selectOrFailByKey(session, "DOES_NOT_EXIST"))
      .isInstanceOf(RowNotFoundException.class)
      .hasMessage("Issue with key 'DOES_NOT_EXIST' does not exist");
  }

  @Test
  void selectByKeys() {
    // contains I1 and I2
    prepareTables();

    List<IssueDto> issues = underTest.selectByKeys(db.getSession(), asList("I1", "I2", "I3"));

    assertThat(issues).extracting(IssueDto::getKey).containsExactlyInAnyOrder("I1", "I2");
    assertThat(issues).filteredOn(issueDto -> issueDto.getKey().equals("I1"))
      .extracting(IssueDto::getAssigneeLogin)
      .containsExactly(USER_LOGIN);
    assertThat(issues).filteredOn(issueDto -> issueDto.getKey().equals("I1"))
      .extracting(IssueDto::getImpacts)
      .flatMap(issueImpactDtos -> issueImpactDtos)
      .extracting(ImpactDto::getSeverity, ImpactDto::getSoftwareQuality)
      .containsExactlyInAnyOrder(
        tuple(MEDIUM, RELIABILITY),
        tuple(LOW, SECURITY));
  }

  @Test
  void selectByKeys_shouldFetchCveIds() {
    prepareTables();
    var cveDto1 = new CveDto("cve_uuid_1", "CVE-123", "Some CVE description", 1.0, 2.0, 3.0, 4L, 5L, 6L, 7L);
    db.getDbClient().cveDao().insert(db.getSession(), cveDto1);
    var cveDto2 = new CveDto("cve_uuid_2", "CVE-456", "Some CVE description", 1.0, 2.0, 3.0, 4L, 5L, 6L, 7L);
    db.getDbClient().cveDao().insert(db.getSession(), cveDto2);
    db.issues().insertIssuesDependency(new IssuesDependencyDto(ISSUE_KEY1, cveDto1.uuid()));
    db.issues().insertIssuesDependency(new IssuesDependencyDto(ISSUE_KEY2, cveDto2.uuid()));

    List<IssueDto> issues = underTest.selectByKeys(db.getSession(), asList("I1", "I2", "I3"));

    assertThat(issues).extracting(IssueDto::getCveId).containsExactlyInAnyOrder(cveDto1.id(), cveDto2.id());
  }

  @Test
  void scrollIndexationIssues_shouldReturnDto() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    RuleDto rule = db.rules().insert(r -> r.setRepositoryKey("java").setLanguage("java")
      .replaceAllDefaultImpacts(List.of(new ImpactDto()
        .setSoftwareQuality(MAINTAINABILITY)
        .setSeverity(MEDIUM),
        new ImpactDto()
          .setSoftwareQuality(RELIABILITY)
          .setSeverity(LOW))));

    ComponentDto branchA = db.components().insertProjectBranch(project, b -> b.setKey("branchA"));
    ComponentDto fileA = db.components().insertComponent(newFileDto(branchA));

    IntStream.range(0, 100).forEach(i -> insertBranchIssueWithManualSeverity(branchA, fileA, rule, "A" + i, STATUS_OPEN, 1_340_000_000_000L));

    Cursor<IndexedIssueDto> issues = underTest.scrollIssuesForIndexation(db.getSession(), null, null);

    Iterator<IndexedIssueDto> iterator = issues.iterator();
    int issueCount = 0;
    while (iterator.hasNext()) {
      IndexedIssueDto next = iterator.next();
      assertThat(next.getRuleDefaultImpacts()).hasSize(2)
        .extracting(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity, ImpactDto::isManualSeverity)
        .containsExactlyInAnyOrder(
          tuple(RELIABILITY, LOW, false),
          tuple(MAINTAINABILITY, MEDIUM, false));
      assertThat(next.getImpacts())
        .extracting(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity, ImpactDto::isManualSeverity)
        .containsExactlyInAnyOrder(
          tuple(MAINTAINABILITY, HIGH, true),
          tuple(RELIABILITY, LOW, false));
      issueCount++;
    }
    assertThat(issueCount).isEqualTo(100);
  }

  @Test
  void selectIssueKeysByComponentUuid() {
    // contains I1 and I2
    prepareTables();

    Set<String> issues = underTest.selectIssueKeysByComponentUuid(db.getSession(), PROJECT_UUID);

    // results are not ordered, so do not use "containsExactly"
    assertThat(issues).containsOnly("I1", "I2");
  }

  @Test
  void selectIssueKeysByComponentUuidFiltersAccordingly() {
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
  void selectIssueKeysByComponentUuidAndChangedSinceFiltersAccordingly() {
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
  void selectByBranch() {
    long updatedAt = 1_340_000_000_000L;
    long changedSince = 1_000_000_000_000L;

    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    RuleDto rule = db.rules().insert(r -> r.setRepositoryKey("java").setLanguage("java"));

    ComponentDto branchA = db.components().insertProjectBranch(project, b -> b.setKey("branchA"));
    ComponentDto fileA = db.components().insertComponent(newFileDto(branchA));

    List<String> statusesA = List.of(STATUS_OPEN, STATUS_REVIEWED, STATUS_CLOSED, STATUS_RESOLVED);
    IntStream.range(0, statusesA.size()).forEach(i -> insertBranchIssue(branchA, fileA, rule, "A" + i, statusesA.get(i), updatedAt));

    insertBranchIssue(branchA, fileA, rule, "WithResolution", STATUS_RESOLVED, RESOLUTION_FIXED, updatedAt);

    ComponentDto branchB = db.components().insertProjectBranch(project, b -> b.setKey("branchB"));
    ComponentDto fileB = db.components().insertComponent(newFileDto(branchB));

    List<String> statusesB = List.of(STATUS_OPEN, STATUS_RESOLVED);
    IntStream.range(0, statusesB.size()).forEach(i -> insertBranchIssue(branchB, fileB, rule, "B" + i, statusesB.get(i), updatedAt));

    List<IssueDto> branchAIssuesA1 = underTest.selectByBranch(db.getSession(), Set.of("issueA0", "issueA1", "issueA3",
      "issueWithResolution"),
      buildSelectByBranchQuery(branchA, false, changedSince));

    assertThat(branchAIssuesA1)
      .extracting(IssueDto::getKey, IssueDto::getStatus, IssueDto::getResolution)
      .containsExactlyInAnyOrder(
        tuple("issueA0", STATUS_OPEN, null),
        tuple("issueA1", STATUS_REVIEWED, null),
        tuple("issueA3", STATUS_RESOLVED, null),
        tuple("issueWithResolution", STATUS_RESOLVED, RESOLUTION_FIXED));

    assertThat(branchAIssuesA1.get(0))
      .extracting(IssueDto::getMessage, IssueDto::parseMessageFormattings)
      .containsOnly("message", MESSAGE_FORMATTING);

    List<IssueDto> branchAIssuesA2 = underTest.selectByBranch(db.getSession(), Set.of("issueA0", "issueA1", "issueA3"),
      buildSelectByBranchQuery(branchA, true, changedSince));

    assertThat(branchAIssuesA2)
      .extracting(IssueDto::getKey, IssueDto::getStatus)
      .containsExactlyInAnyOrder(tuple("issueA0", STATUS_OPEN),
        tuple("issueA1", STATUS_REVIEWED),
        tuple("issueA3", STATUS_RESOLVED));

    List<IssueDto> branchBIssuesB1 = underTest.selectByBranch(db.getSession(), Set.of("issueB0", "issueB1"),
      buildSelectByBranchQuery(branchB, false, changedSince));

    assertThat(branchBIssuesB1)
      .extracting(IssueDto::getKey, IssueDto::getStatus)
      .containsExactlyInAnyOrder(
        tuple("issueB0", STATUS_OPEN),
        tuple("issueB1", STATUS_RESOLVED));

    List<IssueDto> branchBIssuesB2 = underTest.selectByBranch(db.getSession(), Set.of("issueB0", "issueB1"),
      buildSelectByBranchQuery(branchB, true, changedSince));

    assertThat(branchBIssuesB2)
      .extracting(IssueDto::getKey, IssueDto::getStatus)
      .containsExactlyInAnyOrder(tuple("issueB0", STATUS_OPEN),
        tuple("issueB1", STATUS_RESOLVED));
  }

  @Test
  void selectOpenByComponentUuid() {
    RuleDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto projectBranch = db.components().insertProjectBranch(project,
      b -> b.setKey("feature/foo")
        .setBranchType(BranchType.BRANCH));

    ComponentDto file = db.components().insertComponent(newFileDto(projectBranch));

    IssueDto openIssue = db.issues().insert(rule, projectBranch, file, i -> i.setStatus(STATUS_OPEN).setResolution(null));
    db.issues().insert(rule, projectBranch, file, i -> i.setStatus(STATUS_CLOSED).setResolution(RESOLUTION_FIXED));
    IssueDto reopenedIssue = db.issues().insert(rule, projectBranch, file, i -> i.setStatus(STATUS_REOPENED).setResolution(null));
    IssueDto confirmedIssue = db.issues().insert(rule, projectBranch, file, i -> i.setStatus(STATUS_CONFIRMED).setResolution(null));
    IssueDto wontfixIssue = db.issues().insert(rule, projectBranch, file,
      i -> i.setStatus(STATUS_RESOLVED).setResolution(RESOLUTION_WONT_FIX));
    IssueDto fpIssue = db.issues().insert(rule, projectBranch, file,
      i -> i.setStatus(STATUS_RESOLVED).setResolution(RESOLUTION_FALSE_POSITIVE));

    assertThat(underTest.selectOpenByComponentUuids(db.getSession(), Collections.singletonList(file.uuid())))
      .extracting("kee")
      .containsOnly(openIssue.getKey(), reopenedIssue.getKey(), confirmedIssue.getKey(), wontfixIssue.getKey(), fpIssue.getKey());
  }

  @Test
  void selectOpenByComponentUuid_should_correctly_map_required_fields() {
    RuleDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
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
  void test_selectIssueGroupsByComponent_on_component_without_issues() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));

    Collection<IssueGroupDto> groups = underTest.selectIssueGroupsByComponent(db.getSession(), file, 1_000L);

    assertThat(groups).isEmpty();
  }

  @Test
  void selectByKey_givenOneIssueWithQuickFix_selectOneIssueWithQuickFix() {
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
  void selectByKey_givenOneIssueWithoutQuickFix_selectOneIssueWithoutQuickFix() {
    underTest.insert(db.getSession(), createIssueWithKey(ISSUE_KEY1));

    IssueDto issue = underTest.selectOrFailByKey(db.getSession(), ISSUE_KEY1);

    assertThat(issue.getKee()).isEqualTo(ISSUE_KEY1);
    assertThat(issue.isQuickFixAvailable()).isFalse();
  }

  @Test
  void selectIssueGroupsByComponent_on_file() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));
    RuleDto rule = db.rules().insert();
    db.issues().insert(rule, project, file,
      i -> i.setStatus("RESOLVED").setResolution("FALSE-POSITIVE").setSeverity("MAJOR").setType(RuleType.BUG).setIssueCreationTime(1_500L));
    db.issues().insert(rule, project, file,
      i -> i.setStatus("OPEN").setResolution(null).setSeverity("CRITICAL").setType(RuleType.BUG).setIssueCreationTime(1_600L));
    IssueDto criticalBug2 = db.issues().insert(rule, project, file,
      i -> i.setStatus("OPEN").setResolution(null).setSeverity("CRITICAL").setType(RuleType.BUG).setIssueCreationTime(1_700L));
    // closed issues are ignored
    db.issues().insert(rule, project, file,
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
  void selectGroupsOfComponentTreeOnLeak_on_file_new_code_reference_branch() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
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

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void selectIssueImpactGroupsByComponent_shouldReturnImpactGroups(boolean inLeak) {

    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));
    RuleDto rule = db.rules().insert();
    db.issues().insert(rule, project, file,
      i -> i.setStatus(STATUS_OPEN).setEffort(60L).replaceAllImpacts(List.of(createImpact(SECURITY, HIGH), createImpact(MAINTAINABILITY, LOW))));
    db.issues().insert(rule, project, file,
      i -> i.setStatus(STATUS_OPEN).setEffort(60L).replaceAllImpacts(List.of(createImpact(SECURITY, HIGH), createImpact(MAINTAINABILITY, HIGH))));
    db.issues().insert(rule, project, file,
      i -> i.setStatus(STATUS_REOPENED).setEffort(60L).replaceAllImpacts(List.of(createImpact(SECURITY, HIGH))));
    db.issues().insert(rule, project, file,
      i -> i.setStatus(STATUS_RESOLVED).setEffort(60L).setResolution(RESOLUTION_WONT_FIX).replaceAllImpacts(List.of(createImpact(SECURITY, HIGH))));
    // issues in ignored status
    db.issues().insert(rule, project, file,
      i -> i.setStatus(Issue.STATUS_CLOSED).setEffort(60L).replaceAllImpacts(List.of(createImpact(SECURITY, HIGH))));
    db.issues().insert(rule, project, file,
      i -> i.setStatus(STATUS_RESOLVED).setEffort(60L).setResolution(RESOLUTION_FALSE_POSITIVE).replaceAllImpacts(List.of(createImpact(SECURITY, HIGH))));

    Collection<IssueImpactGroupDto> result = underTest.selectIssueImpactGroupsByComponent(db.getSession(), file, inLeak ? 1L : Long.MAX_VALUE);

    assertThat(result).hasSize(5);
    assertThat(result.stream().filter(IssueImpactGroupDto::isInLeak)).hasSize(inLeak ? 5 : 0);
    assertThat(result.stream().mapToLong(IssueImpactGroupDto::getCount).sum()).isEqualTo(6);

    assertThat(result.stream().filter(g -> g.getSoftwareQuality() == SECURITY).mapToLong(IssueImpactGroupDto::getCount).sum()).isEqualTo(4);
    assertThat(result.stream().filter(g -> g.getSoftwareQuality() == MAINTAINABILITY).mapToLong(IssueImpactGroupDto::getCount).sum()).isEqualTo(2);

    assertThat(result.stream().filter(g -> g.getSoftwareQuality() == SECURITY).mapToDouble(IssueImpactGroupDto::getEffort).sum()).isEqualTo(4 * 60);
    assertThat(result.stream().filter(g -> g.getSoftwareQuality() == MAINTAINABILITY).mapToDouble(IssueImpactGroupDto::getEffort).sum()).isEqualTo(2 * 60);

    assertThat(result.stream().filter(g -> g.getSeverity() == HIGH).mapToLong(IssueImpactGroupDto::getCount).sum()).isEqualTo(5);
    assertThat(result.stream().filter(g -> g.getSeverity() == LOW).mapToLong(IssueImpactGroupDto::getCount).sum()).isOne();

    assertThat(result.stream().filter(g -> STATUS_OPEN.equals(g.getStatus())).mapToLong(IssueImpactGroupDto::getCount).sum()).isEqualTo(4);
    assertThat(result.stream().filter(g -> STATUS_REOPENED.equals(g.getStatus())).mapToLong(IssueImpactGroupDto::getCount).sum()).isOne();
    assertThat(result.stream().filter(g -> STATUS_RESOLVED.equals(g.getStatus())).mapToLong(IssueImpactGroupDto::getCount).sum()).isOne();

    assertThat(result.stream().filter(g -> RESOLUTION_WONT_FIX.equals(g.getResolution())).mapToLong(IssueImpactGroupDto::getCount).sum()).isOne();
    assertThat(result.stream().filter(g -> g.getResolution() == null).mapToLong(IssueImpactGroupDto::getCount).sum()).isEqualTo(5);

    assertThat(result.stream().noneMatch(g -> STATUS_CLOSED.equals(g.getResolution()))).isTrue();
    assertThat(result.stream().noneMatch(g -> RESOLUTION_FALSE_POSITIVE.equals(g.getResolution()))).isTrue();
    assertThat(result.stream().noneMatch(g -> RELIABILITY == g.getSoftwareQuality())).isTrue();
    assertThat(result.stream().noneMatch(g -> MEDIUM == g.getSeverity())).isTrue();

  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void selectIssueImpactSeverityGroupsByComponent_shouldReturnImpactSeverityGroups(boolean inLeak) {

    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));
    RuleDto rule = db.rules().insert();
    db.issues().insert(rule, project, file,
      i -> i.setStatus(STATUS_OPEN).setEffort(60L).replaceAllImpacts(List.of(createImpact(RELIABILITY, BLOCKER), createImpact(SECURITY,
        BLOCKER))));
    db.issues().insert(rule, project, file,
      i -> i.setStatus(STATUS_OPEN).setEffort(60L).replaceAllImpacts(List.of(createImpact(RELIABILITY, BLOCKER))));
    db.issues().insert(rule, project, file,
      i -> i.setStatus(STATUS_REOPENED).setEffort(60L).replaceAllImpacts(List.of(createImpact(RELIABILITY, INFO))));
    db.issues().insert(rule, project, file,
      i -> i.setStatus(STATUS_REOPENED).setEffort(60L).replaceAllImpacts(List.of(createImpact(RELIABILITY, INFO), createImpact(SECURITY,
        BLOCKER))));
    db.issues().insert(rule, project, file,
      i -> i.setStatus(STATUS_RESOLVED).setEffort(60L).setResolution(RESOLUTION_WONT_FIX)
        .replaceAllImpacts(List.of(createImpact(MAINTAINABILITY, HIGH), createImpact(RELIABILITY, INFO), createImpact(SECURITY, BLOCKER))));
    db.issues().insert(rule, project, file,
      i -> i.setStatus(STATUS_RESOLVED).setEffort(60L).setResolution(RESOLUTION_WONT_FIX).replaceAllImpacts(List.of(createImpact(SECURITY, HIGH))));
    // issues in ignored status
    db.issues().insert(rule, project, file,
      i -> i.setStatus(Issue.STATUS_CLOSED).setEffort(60L).replaceAllImpacts(List.of(createImpact(SECURITY, HIGH))));
    db.issues().insert(rule, project, file,
      i -> i.setStatus(STATUS_RESOLVED).setEffort(60L).setResolution(RESOLUTION_FALSE_POSITIVE).replaceAllImpacts(List.of(createImpact(SECURITY, HIGH))));

    Collection<IssueImpactSeverityGroupDto> result = underTest.selectIssueImpactSeverityGroupsByComponent(db.getSession(), file, inLeak ? 1L : Long.MAX_VALUE);

    assertThat(result).hasSize(6);
    assertThat(result.stream().filter(IssueImpactSeverityGroupDto::isInLeak)).hasSize(inLeak ? 6 : 0);
    // 6 issues, but 1 has 2 different severity impact, and 1 has 3 different severity impact
    // The total count should then be 6 + 1 (1 additional severity for 1 issue) + 2 (2 additional severities from 1 issue)
    assertThat(result.stream().mapToLong(IssueImpactSeverityGroupDto::getCount).sum()).isEqualTo(6 + 1 + 2);

    assertThat(result.stream().filter(g -> g.getSeverity() == BLOCKER).mapToLong(IssueImpactSeverityGroupDto::getCount).sum()).isEqualTo(4);
    assertThat(result.stream().filter(g -> g.getSeverity() == HIGH).mapToLong(IssueImpactSeverityGroupDto::getCount).sum()).isEqualTo(2);
    assertThat(result.stream().filter(g -> g.getSeverity() == MEDIUM).mapToLong(IssueImpactSeverityGroupDto::getCount).sum()).isZero();
    assertThat(result.stream().filter(g -> g.getSeverity() == LOW).mapToLong(IssueImpactSeverityGroupDto::getCount).sum()).isZero();
    assertThat(result.stream().filter(g -> g.getSeverity() == INFO).mapToLong(IssueImpactSeverityGroupDto::getCount).sum()).isEqualTo(3);

    assertThat(result.stream().filter(g -> RESOLUTION_WONT_FIX.equals(g.getResolution())).mapToLong(IssueImpactSeverityGroupDto::getCount).sum()).isEqualTo(4);
    assertThat(result.stream().filter(g -> g.getResolution() == null).mapToLong(IssueImpactSeverityGroupDto::getCount).sum()).isEqualTo(5);

    assertThat(result.stream().noneMatch(g -> STATUS_CLOSED.equals(g.getResolution()))).isTrue();
    assertThat(result.stream().noneMatch(g -> RESOLUTION_FALSE_POSITIVE.equals(g.getResolution()))).isTrue();
    assertThat(result.stream().noneMatch(g -> MEDIUM == g.getSeverity())).isTrue();

  }

  @Test
  void selectIssueImpactGroupsByComponent_whenNewCodeFromReferenceBranch_shouldReturnImpactGroups() {

    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));
    RuleDto rule = db.rules().insert();
    IssueDto issueInNewCodePeriod = db.issues().insert(rule, project, file,
      i -> i.setStatus(STATUS_OPEN).setEffort(1L).replaceAllImpacts(List.of(createImpact(SECURITY, HIGH), createImpact(MAINTAINABILITY, LOW))));
    db.issues().insert(rule, project, file,
      i -> i.setStatus(STATUS_RESOLVED).setEffort(2L).setResolution(RESOLUTION_WONT_FIX).replaceAllImpacts(List.of(createImpact(SECURITY, HIGH))));

    db.issues().insertNewCodeReferenceIssue(issueInNewCodePeriod);

    Collection<IssueImpactGroupDto> result = underTest.selectIssueImpactGroupsByComponent(db.getSession(), file, -1L);

    assertThat(result).hasSize(3);
    assertThat(result.stream().filter(IssueImpactGroupDto::isInLeak)).hasSize(2);
    assertThat(result.stream().mapToLong(IssueImpactGroupDto::getCount).sum()).isEqualTo(3);

    assertThat(result.stream().filter(g -> g.getSoftwareQuality() == SECURITY).mapToLong(IssueImpactGroupDto::getCount).sum()).isEqualTo(2);
    assertThat(result.stream().filter(g -> g.getSoftwareQuality() == MAINTAINABILITY).mapToLong(IssueImpactGroupDto::getCount).sum()).isEqualTo(1);

    assertThat(result.stream().filter(g -> g.getSoftwareQuality() == SECURITY && g.isInLeak()).mapToDouble(IssueImpactGroupDto::getEffort).sum()).isEqualTo(1d);
    assertThat(result.stream().filter(g -> g.getSoftwareQuality() == MAINTAINABILITY && g.isInLeak()).mapToDouble(IssueImpactGroupDto::getEffort).sum()).isEqualTo(1d);
    assertThat(result.stream().filter(g -> g.getSoftwareQuality() == SECURITY && !g.isInLeak()).mapToDouble(IssueImpactGroupDto::getEffort).sum()).isEqualTo(2d);

    assertThat(result.stream().filter(g -> g.getSeverity() == HIGH).mapToLong(IssueImpactGroupDto::getCount).sum()).isEqualTo(2);
    assertThat(result.stream().filter(g -> g.getSeverity() == LOW).mapToLong(IssueImpactGroupDto::getCount).sum()).isOne();

    assertThat(result.stream().filter(g -> STATUS_OPEN.equals(g.getStatus())).mapToLong(IssueImpactGroupDto::getCount).sum()).isEqualTo(2);
    assertThat(result.stream().filter(g -> STATUS_REOPENED.equals(g.getStatus())).mapToLong(IssueImpactGroupDto::getCount).sum()).isZero();
    assertThat(result.stream().filter(g -> STATUS_RESOLVED.equals(g.getStatus())).mapToLong(IssueImpactGroupDto::getCount).sum()).isOne();

    assertThat(result.stream().filter(g -> RESOLUTION_WONT_FIX.equals(g.getResolution())).mapToLong(IssueImpactGroupDto::getCount).sum()).isOne();

    assertThat(result.stream().filter(IssueImpactGroupDto::isInLeak).mapToLong(IssueImpactGroupDto::getCount).sum()).isEqualTo(2);
    assertThat(result.stream().filter(g -> g.isInLeak() && g.getSoftwareQuality() == SECURITY).mapToLong(IssueImpactGroupDto::getCount).sum()).isEqualTo(1);
    assertThat(result.stream().filter(g -> g.isInLeak() && g.getSoftwareQuality() == MAINTAINABILITY).mapToLong(IssueImpactGroupDto::getCount).sum()).isEqualTo(1);
  }

  @NotNull
  private static ImpactDto createImpact(SoftwareQuality softwareQuality, Severity high) {
    return new ImpactDto().setSoftwareQuality(softwareQuality).setSeverity(high);
  }

  @Test
  void selectIssueImpactGroupsByComponent_whenComponentWithNoIssues_shouldReturnEmpty() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));

    Collection<IssueImpactGroupDto> groups = underTest.selectIssueImpactGroupsByComponent(db.getSession(), file, 1_000L);

    assertThat(groups).isEmpty();
  }

  @Test
  void selectByKey_givenOneIssueNewOnReferenceBranch_selectOneIssueWithNewOnReferenceBranch() {
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
  void selectByKey_givenOneIssueWithoutRuleDescriptionContextKey_returnsEmptyOptional() {
    underTest.insert(db.getSession(), createIssueWithKey(ISSUE_KEY1)
      .setRuleDescriptionContextKey(null));
    IssueDto issue1 = underTest.selectOrFailByKey(db.getSession(), ISSUE_KEY1);

    assertThat(issue1.getOptionalRuleDescriptionContextKey()).isEmpty();
  }

  @Test
  void selectByKey_givenOneIssueWithRuleDescriptionContextKey_returnsContextKey() {
    underTest.insert(db.getSession(), createIssueWithKey(ISSUE_KEY1)
      .setRuleDescriptionContextKey(TEST_CONTEXT_KEY));

    IssueDto issue1 = underTest.selectOrFailByKey(db.getSession(), ISSUE_KEY1);

    assertThat(issue1.getOptionalRuleDescriptionContextKey()).contains(TEST_CONTEXT_KEY);
  }

  @Test
  void insert_shouldInsertBatchIssuesWithImpacts() {
    ImpactDto impact1 = new ImpactDto()
      .setSoftwareQuality(MAINTAINABILITY)
      .setSeverity(HIGH)
      .setManualSeverity(false);
    ImpactDto impact2 = new ImpactDto()
      .setSoftwareQuality(SECURITY)
      .setSeverity(LOW)
      .setManualSeverity(true);
    IssueDto issue1 = createIssueWithKey(ISSUE_KEY1)
      .addImpact(impact1)
      .addImpact(impact2);
    IssueDto issue2 = createIssueWithKey(ISSUE_KEY2);
    underTest.insert(db.getSession(), issue1, issue2);

    List<IssueDto> issueDtos = underTest.selectByKeys(db.getSession(), Set.of(ISSUE_KEY1, ISSUE_KEY2));
    assertThat(issueDtos)
      .extracting(IssueDto::getKey)
      .containsExactlyInAnyOrder(ISSUE_KEY1, ISSUE_KEY2);
    assertThat(issueDtos).filteredOn(issueDto -> issueDto.getKey().equals(ISSUE_KEY1))
      .flatExtracting(IssueDto::getImpacts)
      .containsExactlyInAnyOrder(impact1, impact2)
      .doesNotContainNull();
  }

  @Test
  void deleteIssueImpacts_shouldDeleteOnlyImpactsOfIssue() {
    ImpactDto impact1 = new ImpactDto()
      .setSoftwareQuality(MAINTAINABILITY)
      .setSeverity(HIGH);
    ImpactDto impact2 = new ImpactDto()
      .setSoftwareQuality(SECURITY)
      .setSeverity(LOW);
    IssueDto issue1 = createIssueWithKey(ISSUE_KEY1)
      .addImpact(impact1)
      .addImpact(impact2);
    underTest.insert(db.getSession(), issue1);
    underTest.deleteIssueImpacts(db.getSession(), issue1);

    Optional<IssueDto> issueDto = underTest.selectByKey(db.getSession(), ISSUE_KEY1);
    assertThat(issueDto).isPresent()
      .get()
      .extracting(IssueDto::getImpacts)
      .satisfies(impactDtos -> assertThat(impactDtos).isEmpty());
  }

  @Test
  void updateWithoutIssueImpacts_shouldNotReplaceIssueImpacts() {
    ImpactDto impact1 = new ImpactDto()
      .setSoftwareQuality(MAINTAINABILITY)
      .setSeverity(HIGH);
    ImpactDto impact2 = new ImpactDto()
      .setSoftwareQuality(SECURITY)
      .setSeverity(LOW)
      .setManualSeverity(true);
    IssueDto issue1 = createIssueWithKey(ISSUE_KEY1)
      .addImpact(impact1)
      .addImpact(impact2);
    underTest.insert(db.getSession(), issue1);

    issue1.setResolution(RESOLUTION_FALSE_POSITIVE);

    underTest.updateWithoutIssueImpacts(db.getSession(), issue1);

    Optional<IssueDto> issueDto = underTest.selectByKey(db.getSession(), ISSUE_KEY1);
    assertThat(issueDto).isPresent()
      .get()
      .satisfies(i -> assertThat(i.getResolution()).isEqualTo(RESOLUTION_FALSE_POSITIVE))
      .extracting(IssueDto::getImpacts)
      .satisfies(impactDtos -> assertThat(impactDtos).extracting(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity)
        .containsExactlyInAnyOrder(tuple(MAINTAINABILITY, HIGH), tuple(SECURITY, LOW)));
  }

  @Test
  void update_whenUpdatingRuleDescriptionContextKeyToNull_returnsEmptyContextKey() {
    IssueDto issue = createIssueWithKey(ISSUE_KEY1).setRuleDescriptionContextKey(TEST_CONTEXT_KEY);
    underTest.insert(db.getSession(), issue);

    issue.setRuleDescriptionContextKey(null);
    underTest.update(db.getSession(), issue);

    issue = underTest.selectOrFailByKey(db.getSession(), ISSUE_KEY1);
    assertThat(issue.getOptionalRuleDescriptionContextKey()).isEmpty();
  }

  @Test
  void update_whenUpdatingRuleDescriptionContextKeyToNotNull_returnsContextKey() {
    IssueDto issue = createIssueWithKey(ISSUE_KEY1).setRuleDescriptionContextKey(null);
    underTest.insert(db.getSession(), issue);

    issue.setRuleDescriptionContextKey(TEST_CONTEXT_KEY);
    underTest.update(db.getSession(), issue);

    issue = underTest.selectOrFailByKey(db.getSession(), ISSUE_KEY1);
    assertThat(issue.getOptionalRuleDescriptionContextKey()).contains(TEST_CONTEXT_KEY);
  }

  @Test
  void update_givenOneIssueWithoutRuleDescriptionContextKey_returnsContextKey() {
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

  @Test
  void selectIssueKeysByQuery_shouldBePaginated() {
    List<IssueDto> issues = generateIssues(10, i -> createIssueWithKey("i-" + i));
    issues.forEach(issue -> underTest.insert(db.getSession(), issue));

    List<String> results = underTest.selectIssueKeysByQuery(
      db.getSession(),
      newIssueListQueryBuilder().project(PROJECT_KEY).build(),
      Pagination.forPage(2).andSize(3));

    assertThat(results.stream().toList()).hasSize(3);
  }

  @Test
  void selectIssueKeysByQuery_whenFilteredByBranch_shouldGetOnlyBranchIssues() {
    BranchDto branchDto = ComponentTesting.newBranchDto(PROJECT_UUID, BRANCH);
    ComponentDto branch = db.components().insertProjectBranch(projectDto, branchDto);
    ComponentDto branchFile = db.components().insertComponent(newFileDto(branch));
    List<IssueDto> mainBranchIssues = generateIssues(3, i -> createIssueWithKey("i-" + i));
    List<IssueDto> otherBranchIssues = generateIssues(3, i -> createIssueWithKey("branch-" + i, branch.uuid(), branchFile.uuid()));
    Stream.concat(mainBranchIssues.stream(), otherBranchIssues.stream())
      .forEach(issue -> underTest.insert(db.getSession(), issue));

    List<String> results = underTest.selectIssueKeysByQuery(
      db.getSession(),
      newIssueListQueryBuilder().project(PROJECT_KEY).branch(branchDto.getKey()).build(),
      Pagination.forPage(1).andSize(6));

    List<String> expectedKeys = List.of("branch-0", "branch-1", "branch-2");
    assertThat(results.stream().toList()).containsExactlyInAnyOrderElementsOf(expectedKeys);
  }

  @Test
  void selectIssueKeysByQuery_whenFilteredByPullRequest_shouldGetOnlyPRIssues() {
    BranchDto pullRequestDto = ComponentTesting.newBranchDto(PROJECT_UUID, PULL_REQUEST);
    ComponentDto branch = db.components().insertProjectBranch(projectDto, pullRequestDto);
    ComponentDto branchFile = db.components().insertComponent(newFileDto(branch));
    List<IssueDto> mainBranchIssues = generateIssues(3, i -> createIssueWithKey("i-" + i));
    List<IssueDto> otherBranchIssues = generateIssues(3, i -> createIssueWithKey("pr-" + i, branch.uuid(), branchFile.uuid()));
    Stream.concat(mainBranchIssues.stream(), otherBranchIssues.stream())
      .forEach(issue -> underTest.insert(db.getSession(), issue));

    List<String> results = underTest.selectIssueKeysByQuery(
      db.getSession(),
      newIssueListQueryBuilder().project(PROJECT_KEY).pullRequest(pullRequestDto.getKey()).build(),
      Pagination.forPage(1).andSize(6));

    List<String> expectedKeys = List.of("pr-0", "pr-1", "pr-2");
    assertThat(results.stream().toList()).containsExactlyInAnyOrderElementsOf(expectedKeys);
  }

  @Test
  void selectIssueKeysByQuery_whenFilteredByTypes_shouldGetIssuesWithSpecifiedTypes() {
    List<IssueDto> bugs = generateIssues(3, i -> createIssueWithKey("bug-" + i).setType(RuleType.BUG));
    List<IssueDto> codeSmells = generateIssues(3, i -> createIssueWithKey("codesmell-" + i).setType(RuleType.CODE_SMELL));
    Stream.of(bugs, codeSmells)
      .flatMap(Collection::stream)
      .forEach(issue -> underTest.insert(db.getSession(), issue));

    List<String> results = underTest.selectIssueKeysByQuery(
      db.getSession(),
      newIssueListQueryBuilder().project(PROJECT_KEY).types(List.of(RuleType.BUG.getDbConstant())).build(),
      Pagination.forPage(1).andSize(10));

    List<String> expectedKeys = List.of("bug-0", "bug-1", "bug-2");
    assertThat(results.stream().toList()).containsExactlyInAnyOrderElementsOf(expectedKeys);
  }

  @Test
  void selectIssueKeysByQuery_whenFilteredByFilteredStatuses_shouldGetIssuesWithoutSpecifiedStatuses() {
    List<IssueDto> openIssues = generateIssues(3, i -> createIssueWithKey("open-" + i).setStatus("OPEN"));
    List<IssueDto> closedIssues = generateIssues(3, i -> createIssueWithKey("closed-" + i).setStatus("CLOSED"));
    List<IssueDto> resolvedIssues = generateIssues(3, i -> createIssueWithKey("resolved-" + i).setStatus("RESOLVED"));
    Stream.of(openIssues, closedIssues, resolvedIssues)
      .flatMap(Collection::stream)
      .forEach(issue -> underTest.insert(db.getSession(), issue));

    List<String> results = underTest.selectIssueKeysByQuery(
      db.getSession(),
      newIssueListQueryBuilder().project(PROJECT_KEY).statuses(List.of("OPEN")).build(),
      Pagination.forPage(1).andSize(10));

    List<String> expectedKeys = List.of("open-0", "open-1", "open-2");
    assertThat(results.stream().toList()).containsExactlyInAnyOrderElementsOf(expectedKeys);
  }

  @Test
  void selectIssueKeysByQuery_whenFilteredByFilteredResolutions_shouldGetIssuesWithoutSpecifiedResolution() {
    List<IssueDto> unresolvedIssues = generateIssues(3, i -> createIssueWithKey("open-" + i).setResolution(null));
    List<IssueDto> wontfixIssues = generateIssues(3, i -> createIssueWithKey("wf-" + i).setResolution("WONTFIX"));
    List<IssueDto> falsePositiveIssues = generateIssues(3, i -> createIssueWithKey("fp-" + i).setResolution("FALSE-POSITIVE"));
    Stream.of(unresolvedIssues, wontfixIssues, falsePositiveIssues)
      .flatMap(Collection::stream)
      .forEach(issue -> underTest.insert(db.getSession(), issue));

    List<String> results = underTest.selectIssueKeysByQuery(
      db.getSession(),
      newIssueListQueryBuilder().project(PROJECT_KEY).resolutions(List.of("WONTFIX")).build(),
      Pagination.forPage(1).andSize(10));

    List<String> expectedKeys = List.of("wf-0", "wf-1", "wf-2");
    assertThat(results.stream().toList()).containsExactlyInAnyOrderElementsOf(expectedKeys);
  }

  @Test
  void selectIssueKeysByQuery_whenFilteredByFileComponent_shouldGetIssuesWithinFileOnly() {
    ComponentDto otherFileDto = db.components().insertComponent(newFileDto(projectDto).setUuid("OTHER_UUID").setKey("OTHER_KEY"));
    List<IssueDto> fromFileIssues = generateIssues(3, i -> createIssueWithKey("file-" + i));
    List<IssueDto> fromOtherFileIssues = generateIssues(3, i -> createIssueWithKey("otherfile-" + i, PROJECT_UUID, otherFileDto.uuid()));
    Stream.of(fromFileIssues, fromOtherFileIssues)
      .flatMap(Collection::stream)
      .forEach(issue -> underTest.insert(db.getSession(), issue));

    List<String> results = underTest.selectIssueKeysByQuery(
      db.getSession(),
      newIssueListQueryBuilder().component(otherFileDto.getKey()).build(),
      Pagination.forPage(1).andSize(10));

    List<String> expectedKeys = List.of("otherfile-0", "otherfile-1", "otherfile-2");
    assertThat(results.stream().toList()).containsExactlyInAnyOrderElementsOf(expectedKeys);
  }

  @Test
  void selectIssueKeysByQuery_whenFilteredWithInNewCodeReference_shouldGetNewCodeReferenceIssues() {
    List<IssueDto> issues = generateIssues(3, i -> createIssueWithKey("i-" + i));
    List<IssueDto> newCodeRefIssues = generateIssues(3, i -> createIssueWithKey("newCodeRef-" + i));
    Stream.of(issues, newCodeRefIssues)
      .flatMap(Collection::stream)
      .forEach(issue -> underTest.insert(db.getSession(), issue));
    newCodeRefIssues.forEach(issue -> db.issues().insertNewCodeReferenceIssue(issue));

    List<String> results = underTest.selectIssueKeysByQuery(
      db.getSession(),
      newIssueListQueryBuilder().project(PROJECT_KEY).newCodeOnReference(true).build(),
      Pagination.forPage(1).andSize(10));

    List<String> expectedKeys = List.of("newCodeRef-0", "newCodeRef-1", "newCodeRef-2");
    assertThat(results.stream().toList()).containsExactlyInAnyOrderElementsOf(expectedKeys);
  }

  @Test
  void selectIssueKeysByQuery_whenFilteredWithCreatedAfter_shouldGetIssuesCreatedAfterDate() {
    List<IssueDto> createdBeforeIssues = generateIssues(3,
      i -> createIssueWithKey("createdBefore-" + i).setResolution(null).setIssueCreationDate(new Date(1_400_000_000_000L)));
    List<IssueDto> createdAfterIssues = generateIssues(3,
      i -> createIssueWithKey("createdAfter-" + i).setResolution(null).setIssueCreationDate(new Date(1_420_000_000_000L)));
    Stream.of(createdBeforeIssues, createdAfterIssues)
      .flatMap(Collection::stream)
      .forEach(issue -> underTest.insert(db.getSession(), issue));

    List<String> results = underTest.selectIssueKeysByQuery(
      db.getSession(),
      newIssueListQueryBuilder().project(PROJECT_KEY).createdAfter(1_410_000_000_000L).build(),
      Pagination.forPage(1).andSize(10));

    List<String> expectedKeys = List.of("createdAfter-0", "createdAfter-1", "createdAfter-2");
    assertThat(results.stream().toList()).containsExactlyInAnyOrderElementsOf(expectedKeys);
  }

  @Test
  void selectIssueKeysByQuery_whenFilteredWithSoftwareQualities_shouldGetThoseIssuesOnly() {
    prepareTables(); // One of the issues has software quality impact: SECURITY

    List<String> results = underTest.selectIssueKeysByQuery(
      db.getSession(),
      newIssueListQueryBuilder().project(PROJECT_KEY).softwareQualities(List.of("SECURITY")).build(),
      Pagination.forPage(1).andSize(10));

    List<String> expectedKeys = List.of("I1");
    assertThat(results.stream().toList()).containsExactlyInAnyOrderElementsOf(expectedKeys);
  }

  @Test
  void updateIfBeforeSelectedDate_whenCalledWithBeforeSelectDate_shouldUpdateImpacts() {
    prepareTables();
    IssueDto issueDto = underTest.selectOrFailByKey(db.getSession(), ISSUE_KEY1)
      .setSelectedAt(1_440_000_000_000L);

    boolean isUpdated = underTest.updateIfBeforeSelectedDate(db.getSession(), issueDto);

    assertThat(isUpdated).isTrue();
  }

  @Test
  void updateIfBeforeSelectedDate_whenCalledWithAfterSelectDate_shouldNotUpdateImpacts() {
    prepareTables();
    IssueDto issueDto = underTest.selectOrFailByKey(db.getSession(), ISSUE_KEY1)
      .setSelectedAt(1_400_000_000_000L)
      .replaceAllImpacts(List.of(createImpact(RELIABILITY, LOW)));

    underTest.updateIfBeforeSelectedDate(db.getSession(), issueDto);

    assertThat(underTest.selectOrFailByKey(db.getSession(), ISSUE_KEY1).getImpacts()).extracting(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity)
      .containsExactlyInAnyOrder(tuple(RELIABILITY, MEDIUM), tuple(SECURITY, LOW));
  }

  @Test
  void insertIssueImpacts_should_insert_all_values() {
    IssueDto issueDto = createIssueWithKey(ISSUE_KEY1);
    ImpactDto impactDto1 = new ImpactDto(MAINTAINABILITY, MEDIUM, true);
    ImpactDto impactDto2 = new ImpactDto(RELIABILITY, HIGH, false);
    issueDto.addImpact(impactDto1);
    issueDto.addImpact(impactDto2);
    underTest.insert(db.getSession(), issueDto);

    assertThat(underTest.selectOrFailByKey(db.getSession(), ISSUE_KEY1).getImpacts()).extracting(ImpactDto::getSoftwareQuality,
      ImpactDto::getSeverity, ImpactDto::isManualSeverity)
      .containsExactlyInAnyOrder(tuple(MAINTAINABILITY, MEDIUM, true), tuple(RELIABILITY, HIGH, false));
  }

  private static IssueDto createIssueWithKey(String issueKey) {
    return createIssueWithKey(issueKey, PROJECT_UUID, FILE_UUID);
  }

  private static IssueDto createIssueWithKey(String issueKey, String branchUuid, String fileUuid) {
    return newIssueDto(issueKey)
      .setMessage("the message")
      .setRuleUuid(RULE.getUuid())
      .setComponentUuid(fileUuid)
      .setProjectUuid(branchUuid)
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
    dto.setTags(Set.of("tag1", "tag2"));
    dto.setCodeVariants(Set.of("variant1", "variant2"));
    return dto;
  }

  private void prepareTables() {
    underTest.insert(db.getSession(), newIssueDto(ISSUE_KEY1)
      .setMessage("the message")
      .setRuleUuid(RULE.getUuid())
      .setComponentUuid(FILE_UUID)
      .setProjectUuid(PROJECT_UUID)
      .setAssigneeUuid(userDto.getUuid())
      .addImpact(newIssueImpact(RELIABILITY, MEDIUM))
      .addImpact(newIssueImpact(SECURITY, LOW)));
    underTest.insert(db.getSession(), newIssueDto(ISSUE_KEY2)
      .setRuleUuid(RULE.getUuid())
      .setComponentUuid(FILE_UUID)
      .setStatus("CLOSED")
      .setProjectUuid(PROJECT_UUID));
    db.getSession().commit();
  }

  private static ImpactDto newIssueImpact(SoftwareQuality softwareQuality, Severity severity) {
    return new ImpactDto()
      .setSoftwareQuality(softwareQuality)
      .setSeverity(severity);
  }

  private static RuleType randomRuleTypeExceptHotspot() {
    return RULE_TYPES_EXCEPT_HOTSPOT[RANDOM.nextInt(RULE_TYPES_EXCEPT_HOTSPOT.length)];
  }

  private void insertBranchIssue(ComponentDto branch, ComponentDto file, RuleDto rule, String id, String status,
    @Nullable String resolution, Long updateAt) {
    db.issues().insert(rule, branch, file, i -> i.setKee("issue" + id)
      .setStatus(status)
      .setResolution(resolution)
      .setUpdatedAt(updateAt)
      .setType(randomRuleTypeExceptHotspot())
      .setMessage("message")
      .setMessageFormattings(MESSAGE_FORMATTING));
  }

  private void insertBranchIssueWithManualSeverity(ComponentDto branch, ComponentDto file, RuleDto rule, String id, String status,
    Long updateAt) {
    db.issues().insert(rule, branch, file, i -> i.setKee("issue" + id)
      .setStatus(status)
      .setResolution(null)
      .setUpdatedAt(updateAt)
      .setType(randomRuleTypeExceptHotspot())
      .setMessage("message")
      .setMessageFormattings(MESSAGE_FORMATTING)
      .replaceAllImpacts(List.of(
        new ImpactDto()
          .setSoftwareQuality(MAINTAINABILITY)
          .setSeverity(HIGH)
          .setManualSeverity(true),
        new ImpactDto()
          .setSoftwareQuality(RELIABILITY)
          .setSeverity(LOW)
          .setManualSeverity(false))));
  }

  private void insertBranchIssue(ComponentDto branch, ComponentDto file, RuleDto rule, String id, String status, Long updateAt) {
    insertBranchIssue(branch, file, rule, id, status, null, updateAt);
  }

  private static IssueQueryParams buildSelectByBranchQuery(ComponentDto branch, boolean resolvedOnly, Long changedSince) {
    return new IssueQueryParams(branch.uuid(), List.of("java"), List.of(), List.of(), resolvedOnly, changedSince);
  }
}
