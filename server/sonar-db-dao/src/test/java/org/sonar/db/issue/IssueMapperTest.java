/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.issue.Issue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@RunWith(DataProviderRunner.class)
public class IssueMapperTest {

  private static final long NO_FILTERING_ON_CLOSE_DATE = 1L;

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbSession dbSession = dbTester.getSession();

  private IssueMapper underTest = dbSession.getMapper(IssueMapper.class);

  private ComponentDto project, file, file2;
  private RuleDto rule;
  private Random random = new Random();
  private System2 system2 = new AlwaysIncreasingSystem2();

  @Before
  public void setUp() throws Exception {
    OrganizationDto organizationDto = dbTester.organizations().insert();
    project = ComponentTesting.newPrivateProjectDto(organizationDto);
    dbTester.getDbClient().componentDao().insert(dbSession, project);
    file = ComponentTesting.newFileDto(project, null);
    dbTester.getDbClient().componentDao().insert(dbSession, file);
    file2 = ComponentTesting.newFileDto(project, null).setUuid("file2 uuid");
    dbTester.getDbClient().componentDao().insert(dbSession, file2);
    rule = RuleTesting.newXooX1();
    dbTester.rules().insertRule(rule);
    dbSession.commit();
  }

  @Test
  public void insert() {
    underTest.insert(newIssue());
    dbTester.getSession().commit();

    IssueDto result = underTest.selectByKey("ABCDE");
    assertThat(result).isNotNull();
    assertThat(result.getId()).isNotNull();
    assertThat(result.getKey()).isEqualTo("ABCDE");
    assertThat(result.getComponentUuid()).isEqualTo(file.uuid());
    assertThat(result.getProjectUuid()).isEqualTo(project.uuid());
    assertThat(result.getRuleId()).isEqualTo(rule.getId());
    assertThat(result.getType()).isEqualTo(2);
    assertThat(result.getLine()).isEqualTo(500);
    assertThat(result.getGap()).isEqualTo(3.14d);
    assertThat(result.getEffort()).isEqualTo(10L);
    assertThat(result.getResolution()).isEqualTo("FIXED");
    assertThat(result.getStatus()).isEqualTo("RESOLVED");
    assertThat(result.getSeverity()).isEqualTo("BLOCKER");
    assertThat(result.getAuthorLogin()).isEqualTo("morgan");
    assertThat(result.getAssigneeUuid()).isEqualTo("karadoc");
    assertThat(result.getIssueAttributes()).isEqualTo("JIRA=FOO-1234");
    assertThat(result.getChecksum()).isEqualTo("123456789");
    assertThat(result.getMessage()).isEqualTo("the message");
    assertThat(result.getIssueCreationTime()).isEqualTo(1_401_000_000_000L);
    assertThat(result.getIssueUpdateTime()).isEqualTo(1_402_000_000_000L);
    assertThat(result.getIssueCloseTime()).isEqualTo(1_403_000_000_000L);
    assertThat(result.getCreatedAt()).isEqualTo(1_400_000_000_000L);
    assertThat(result.getUpdatedAt()).isEqualTo(1_500_000_000_000L);
  }

  @Test
  public void update() {
    underTest.insert(newIssue());

    dbTester.getSession().commit();

    IssueDto update = new IssueDto();
    update.setKee("ABCDE");
    update.setComponentUuid("other component uuid");
    update.setProjectUuid(project.uuid());
    update.setRuleId(rule.getId());
    update.setType(3);
    update.setLine(500);
    update.setGap(3.14);
    update.setEffort(10L);
    update.setResolution("FIXED");
    update.setStatus("RESOLVED");
    update.setSeverity("BLOCKER");
    update.setAuthorLogin("morgan");
    update.setAssigneeUuid("karadoc");
    update.setIssueAttributes("JIRA=FOO-1234");
    update.setChecksum("123456789");
    update.setMessage("the message");

    update.setIssueCreationTime(1_550_000_000_000L);
    update.setIssueUpdateTime(1_550_000_000_000L);
    update.setIssueCloseTime(1_550_000_000_000L);
    // Should not change
    update.setCreatedAt(1_400_123_456_789L);
    update.setUpdatedAt(1_550_000_000_000L);

    underTest.update(update);
    dbTester.getSession().commit();

    IssueDto result = underTest.selectByKey("ABCDE");
    assertThat(result).isNotNull();
    assertThat(result.getId()).isNotNull();
    assertThat(result.getKey()).isEqualTo("ABCDE");
    assertThat(result.getComponentUuid()).isEqualTo(file.uuid());
    assertThat(result.getProjectUuid()).isEqualTo(project.uuid());
    assertThat(result.getRuleId()).isEqualTo(rule.getId());
    assertThat(result.getType()).isEqualTo(3);
    assertThat(result.getLine()).isEqualTo(500);
    assertThat(result.getGap()).isEqualTo(3.14d);
    assertThat(result.getEffort()).isEqualTo(10L);
    assertThat(result.getResolution()).isEqualTo("FIXED");
    assertThat(result.getStatus()).isEqualTo("RESOLVED");
    assertThat(result.getSeverity()).isEqualTo("BLOCKER");
    assertThat(result.getAuthorLogin()).isEqualTo("morgan");
    assertThat(result.getAssigneeUuid()).isEqualTo("karadoc");
    assertThat(result.getIssueAttributes()).isEqualTo("JIRA=FOO-1234");
    assertThat(result.getChecksum()).isEqualTo("123456789");
    assertThat(result.getMessage()).isEqualTo("the message");
    assertThat(result.getIssueCreationTime()).isEqualTo(1_550_000_000_000L);
    assertThat(result.getIssueUpdateTime()).isEqualTo(1_550_000_000_000L);
    assertThat(result.getIssueCloseTime()).isEqualTo(1_550_000_000_000L);
    assertThat(result.getCreatedAt()).isEqualTo(1_400_000_000_000L);
    assertThat(result.getUpdatedAt()).isEqualTo(1_550_000_000_000L);
  }

  @Test
  public void updateBeforeSelectedDate_without_conflict() {
    underTest.insert(newIssue());

    IssueDto dto = newIssue()
      .setComponentUuid(file2.uuid())
      .setType(3)
      .setLine(600)
      .setGap(1.12d)
      .setEffort(50L)
      .setIssueUpdateTime(1_600_000_000_000L)
      .setUpdatedAt(1_600_000_000_000L);

    // selected after last update -> ok
    dto.setSelectedAt(1500000000000L);

    int count = underTest.updateIfBeforeSelectedDate(dto);
    assertThat(count).isEqualTo(1);
    dbTester.getSession().commit();

    IssueDto result = underTest.selectByKey("ABCDE");
    assertThat(result).isNotNull();
    assertThat(result.getComponentUuid()).isEqualTo(file2.uuid());
    assertThat(result.getType()).isEqualTo(3);
    assertThat(result.getLine()).isEqualTo(600);
    assertThat(result.getGap()).isEqualTo(1.12d);
    assertThat(result.getEffort()).isEqualTo(50L);
    assertThat(result.getIssueUpdateTime()).isEqualTo(1_600_000_000_000L);
    assertThat(result.getUpdatedAt()).isEqualTo(1_600_000_000_000L);
  }

  @Test
  public void updateBeforeSelectedDate_with_conflict() {
    underTest.insert(newIssue());

    IssueDto dto = newIssue()
      .setComponentUuid(file2.uuid())
      .setType(3)
      .setLine(600)
      .setGap(1.12d)
      .setEffort(50L)
      .setIssueUpdateTime(1_600_000_000_000L)
      .setUpdatedAt(1_600_000_000_000L);

    // selected before last update -> ko
    dto.setSelectedAt(1400000000000L);

    int count = underTest.updateIfBeforeSelectedDate(dto);
    assertThat(count).isEqualTo(0);
    dbTester.getSession().commit();

    // No change
    IssueDto result = underTest.selectByKey("ABCDE");
    assertThat(result).isNotNull();
    assertThat(result.getComponentUuid()).isEqualTo(file.uuid());
    assertThat(result.getType()).isEqualTo(2);
    assertThat(result.getLine()).isEqualTo(500);
    assertThat(result.getGap()).isEqualTo(3.14d);
    assertThat(result.getEffort()).isEqualTo(10L);
    assertThat(result.getIssueUpdateTime()).isEqualTo(1_402_000_000_000L);
    assertThat(result.getUpdatedAt()).isEqualTo(1_500_000_000_000L);
  }

  @Test
  public void scrollClosedByComponentUuid_returns_empty_when_no_issue_for_component() {
    String componentUuid = randomAlphabetic(10);
    RecorderResultHandler resultHandler = new RecorderResultHandler();

    underTest.scrollClosedByComponentUuid(componentUuid, new Date().getTime(), resultHandler);

    assertThat(resultHandler.issues).isEmpty();
  }

  @Test
  @UseDataProvider("closedIssuesSupportedRuleTypes")
  public void scrollClosedByComponentUuid_returns_closed_issues_with_at_least_one_diff_to_CLOSED(RuleType ruleType) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto component = randomComponent(organization);
    IssueDto expected = insertNewClosedIssue(component, ruleType);
    IssueChangeDto changeDto = insertToClosedDiff(expected);

    RecorderResultHandler resultHandler = new RecorderResultHandler();
    underTest.scrollClosedByComponentUuid(component.uuid(), NO_FILTERING_ON_CLOSE_DATE, resultHandler);

    assertThat(resultHandler.issues).hasSize(1);
    IssueDto issue = resultHandler.issues.iterator().next();
    assertThat(issue.getKey()).isEqualTo(issue.getKey());
    assertThat(issue.getClosedChangeData()).contains(changeDto.getChangeData());
  }

  @Test
  @UseDataProvider("closedIssuesSupportedRuleTypes")
  public void scrollClosedByComponentUuid_does_not_return_closed_issues_of_non_existing_rule(RuleType ruleType) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto component = randomComponent(organization);
    IssueDto issueWithRule = insertNewClosedIssue(component, ruleType);
    IssueChangeDto issueChange = insertToClosedDiff(issueWithRule);
    IssueDto issueWithoutRule = insertNewClosedIssue(component, new RuleDefinitionDto().setType(ruleType).setId(-50));
    insertToClosedDiff(issueWithoutRule);

    RecorderResultHandler resultHandler = new RecorderResultHandler();
    underTest.scrollClosedByComponentUuid(component.uuid(), NO_FILTERING_ON_CLOSE_DATE, resultHandler);

    assertThat(resultHandler.issues)
      .extracting(IssueDto::getKey, t -> t.getClosedChangeData().get())
      .containsOnly(tuple(issueWithRule.getKey(), issueChange.getChangeData()));
  }

  @Test
  @UseDataProvider("closedIssuesSupportedRuleTypes")
  public void scrollClosedByComponentUuid_does_not_return_closed_issues_of_orphan_component(RuleType ruleType) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto component = randomComponent(organization);
    IssueDto issue = insertNewClosedIssue(component, ruleType);
    IssueChangeDto issueChange = insertToClosedDiff(issue);
    IssueDto issueMissingComponent = insertNewClosedIssue(component, ruleType, t -> t.setComponentUuid("does_not_exist"));
    insertToClosedDiff(issueMissingComponent);
    IssueDto issueMissingProject = insertNewClosedIssue(component, ruleType, t -> t.setProjectUuid("does_not_exist"));
    insertToClosedDiff(issueMissingProject);

    RecorderResultHandler resultHandler = new RecorderResultHandler();
    underTest.scrollClosedByComponentUuid(component.uuid(), NO_FILTERING_ON_CLOSE_DATE, resultHandler);

    assertThat(resultHandler.issues)
      .extracting(IssueDto::getKey, t -> t.getClosedChangeData().get())
      .containsOnly(tuple(issue.getKey(), issueChange.getChangeData()));
  }

  @Test
  @UseDataProvider("closedIssuesSupportedRuleTypes")
  public void scrollClosedByComponentUuid_does_not_return_closed_issues_without_any_status_diff_to_CLOSED(RuleType ruleType) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto component = randomComponent(organization);
    IssueDto issueWithLineDiff = insertNewClosedIssue(component, ruleType);
    IssueChangeDto issueChange = insertToClosedDiff(issueWithLineDiff);
    insertNewClosedIssue(component, ruleType);

    RecorderResultHandler resultHandler = new RecorderResultHandler();
    underTest.scrollClosedByComponentUuid(component.uuid(), NO_FILTERING_ON_CLOSE_DATE, resultHandler);

    assertThat(resultHandler.issues)
      .extracting(IssueDto::getKey, t -> t.getClosedChangeData().get())
      .containsOnly(tuple(issueWithLineDiff.getKey(), issueChange.getChangeData()));
  }

  @Test
  public void scrollClosedByComponentUuid_does_not_return_closed_issues_of_type_SECURITY_HOTSPOT() {
    RuleType ruleType = randomSupportedRuleType();
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto component = randomComponent(organization);
    IssueDto securityHotspotIssue = insertNewClosedIssue(component, RuleType.SECURITY_HOTSPOT);
    insertToClosedDiff(securityHotspotIssue);
    IssueDto issue = insertNewClosedIssue(component, ruleType);
    IssueChangeDto issueChange = insertToClosedDiff(issue);

    RecorderResultHandler resultHandler = new RecorderResultHandler();
    underTest.scrollClosedByComponentUuid(component.uuid(), NO_FILTERING_ON_CLOSE_DATE, resultHandler);

    assertThat(resultHandler.issues)
      .extracting(IssueDto::getKey, t -> t.getClosedChangeData().get())
      .containsOnly(tuple(issue.getKey(), issueChange.getChangeData()));
  }

  @Test
  public void scrollClosedByComponentUuid_returns_closed_issues_without_isHotspot_flag() {
    RuleType ruleType = randomSupportedRuleType();
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto component = randomComponent(organization);
    IssueDto noHotspotFlagIssue = insertNewClosedIssue(component, ruleType);
    IssueChangeDto noFlagIssueChange = insertToClosedDiff(noHotspotFlagIssue);
    manuallySetToNullFromHotpotsColumn(noHotspotFlagIssue);
    IssueDto issue = insertNewClosedIssue(component, ruleType);
    IssueChangeDto issueChange = insertToClosedDiff(issue);

    RecorderResultHandler resultHandler = new RecorderResultHandler();
    underTest.scrollClosedByComponentUuid(component.uuid(), NO_FILTERING_ON_CLOSE_DATE, resultHandler);

    assertThat(resultHandler.issues)
      .extracting(IssueDto::getKey, t -> t.getClosedChangeData().get())
      .containsOnly(
        tuple(issue.getKey(), issueChange.getChangeData()),
        tuple(noHotspotFlagIssue.getKey(), noFlagIssueChange.getChangeData()));
  }

  @Test
  public void scrollClosedByComponentUuid_does_not_return_closed_issues_without_close_date() {
    RuleType ruleType = randomSupportedRuleType();
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto component = randomComponent(organization);
    IssueDto issueWithoutCloseDate = insertNewClosedIssue(component, ruleType, t -> t.setIssueCloseDate(null));
    insertToClosedDiff(issueWithoutCloseDate);
    IssueDto issueCloseDate = insertNewClosedIssue(component, ruleType);
    IssueChangeDto changeDto = insertToClosedDiff(issueCloseDate);

    RecorderResultHandler resultHandler = new RecorderResultHandler();
    underTest.scrollClosedByComponentUuid(component.uuid(), NO_FILTERING_ON_CLOSE_DATE, resultHandler);

    assertThat(resultHandler.issues).hasSize(1);
    IssueDto issue = resultHandler.issues.iterator().next();
    assertThat(issue.getKey()).isEqualTo(issue.getKey());
    assertThat(issue.getClosedChangeData()).contains(changeDto.getChangeData());
  }

  @Test
  public void scrollClosedByComponentUuid_returns_closed_issues_which_close_date_is_greater_or_equal_to_requested() {
    RuleType ruleType = randomSupportedRuleType();
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto component = randomComponent(organization);
    RuleDefinitionDto rule1 = dbTester.rules().insert(t -> t.setType(ruleType));
    IssueDto[] issues = new IssueDto[] {
      insertNewClosedIssue(component, rule1, 1_999_999L),
      insertNewClosedIssue(component, rule1, 3_999_999L),
      insertNewClosedIssue(component, rule1, 2_999_999L),
      insertNewClosedIssue(component, rule1, 10_999_999L)
    };
    Arrays.stream(issues).forEach(this::insertToClosedDiff);

    RecorderResultHandler resultHandler = new RecorderResultHandler();
    underTest.scrollClosedByComponentUuid(component.uuid(), 4_000_000L, resultHandler);

    assertThat(resultHandler.issues)
      .extracting(IssueDto::getKey)
      .containsOnly(issues[3].getKey());

    resultHandler = new RecorderResultHandler();
    underTest.scrollClosedByComponentUuid(component.uuid(), 11_999_999L, resultHandler);

    assertThat(resultHandler.issues).isEmpty();

    resultHandler = new RecorderResultHandler();
    underTest.scrollClosedByComponentUuid(component.uuid(), 3_999_999L, resultHandler);

    assertThat(resultHandler.issues)
      .extracting(IssueDto::getKey)
      .containsOnly(issues[3].getKey(), issues[1].getKey());

    resultHandler = new RecorderResultHandler();
    underTest.scrollClosedByComponentUuid(component.uuid(), 2_999_999L, resultHandler);

    assertThat(resultHandler.issues)
      .extracting(IssueDto::getKey)
      .containsOnly(issues[3].getKey(), issues[1].getKey(), issues[2].getKey());

    resultHandler = new RecorderResultHandler();
    underTest.scrollClosedByComponentUuid(component.uuid(), 1L, resultHandler);

    assertThat(resultHandler.issues)
      .extracting(IssueDto::getKey)
      .containsOnly(issues[3].getKey(), issues[1].getKey(), issues[2].getKey(), issues[0].getKey());
  }

  private void manuallySetToNullFromHotpotsColumn(IssueDto fromHostSpotIssue) {
    dbTester.executeUpdateSql("update issues set from_hotspot = null where kee = '" + fromHostSpotIssue.getKey() + "'");
    dbTester.commit();
  }

  @Test
  @UseDataProvider("closedIssuesSupportedRuleTypes")
  public void scrollClosedByComponentUuid_does_not_return_closed_issues_with_isHotspot_flag_true(RuleType ruleType) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto component = randomComponent(organization);
    IssueDto fromHostSpotIssue = insertNewClosedIssue(component, ruleType, t -> t.setIsFromHotspot(true));
    insertToClosedDiff(fromHostSpotIssue);
    IssueDto issue = insertNewClosedIssue(component, ruleType);
    IssueChangeDto issueChange = insertToClosedDiff(issue);

    RecorderResultHandler resultHandler = new RecorderResultHandler();
    underTest.scrollClosedByComponentUuid(component.uuid(), NO_FILTERING_ON_CLOSE_DATE, resultHandler);

    assertThat(resultHandler.issues)
      .extracting(IssueDto::getKey, t -> t.getClosedChangeData().get())
      .containsOnly(tuple(issue.getKey(), issueChange.getChangeData()));
  }

  @Test
  @UseDataProvider("closedIssuesSupportedRuleTypes")
  public void scrollClosedByComponentUuid_return_one_row_per_status_diff_to_CLOSED_sorted_by_most_recent_creation_date_first(RuleType ruleType) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto component = randomComponent(organization);
    IssueDto issue = insertNewClosedIssue(component, ruleType);
    Date date = new Date();
    IssueChangeDto changes[] = new IssueChangeDto[] {
      insertToClosedDiff(issue, DateUtils.addDays(date, -10)),
      insertToClosedDiff(issue, DateUtils.addDays(date, -60)),
      insertToClosedDiff(issue, date),
      insertToClosedDiff(issue, DateUtils.addDays(date, -5))
    };

    RecorderResultHandler resultHandler = new RecorderResultHandler();
    underTest.scrollClosedByComponentUuid(component.uuid(), NO_FILTERING_ON_CLOSE_DATE, resultHandler);

    assertThat(resultHandler.issues)
      .hasSize(4)
      .extracting(IssueDto::getKey, t -> t.getClosedChangeData().get())
      .containsExactly(
        tuple(issue.getKey(), changes[2].getChangeData()),
        tuple(issue.getKey(), changes[3].getChangeData()),
        tuple(issue.getKey(), changes[0].getChangeData()),
        tuple(issue.getKey(), changes[1].getChangeData()));
  }

  @Test
  @UseDataProvider("closedIssuesSupportedRuleTypes")
  public void scrollClosedByComponentUuid_does_not_return_row_for_status_change_from_close(RuleType ruleType) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto component = randomComponent(organization);
    IssueDto issue = insertNewClosedIssue(component, ruleType);
    Date date = new Date();
    IssueChangeDto changes[] = new IssueChangeDto[] {
      insertToClosedDiff(issue, DateUtils.addDays(date, -10), Issue.STATUS_CLOSED, Issue.STATUS_REOPENED),
      insertToClosedDiff(issue, DateUtils.addDays(date, -60)),
      insertToClosedDiff(issue, date),
      insertToClosedDiff(issue, DateUtils.addDays(date, -5))
    };

    RecorderResultHandler resultHandler = new RecorderResultHandler();
    underTest.scrollClosedByComponentUuid(component.uuid(), NO_FILTERING_ON_CLOSE_DATE, resultHandler);

    assertThat(resultHandler.issues)
      .hasSize(3)
      .extracting(IssueDto::getKey, t -> t.getClosedChangeData().get())
      .containsExactly(
        tuple(issue.getKey(), changes[2].getChangeData()),
        tuple(issue.getKey(), changes[3].getChangeData()),
        tuple(issue.getKey(), changes[1].getChangeData()));
  }

  private IssueChangeDto insertToClosedDiff(IssueDto issueDto) {
    return insertToClosedDiff(issueDto, new Date());
  }

  private IssueChangeDto insertToClosedDiff(IssueDto issueDto, Date date) {
    String[] statusesButClosed = Issue.STATUSES.stream()
      .filter(t -> !Issue.STATUS_CLOSED.equals(t))
      .toArray(String[]::new);
    String previousStatus = statusesButClosed[random.nextInt(statusesButClosed.length)];
    return insertToClosedDiff(issueDto, date, previousStatus, Issue.STATUS_CLOSED);
  }

  private IssueChangeDto insertToClosedDiff(IssueDto issue, Date creationDate, String previousStatus, String nextStatus) {
    FieldDiffs diffs = new FieldDiffs()
      .setCreationDate(creationDate);
    IntStream.range(0, random.nextInt(3)).forEach(i -> diffs.setDiff("key_b" + i, "old_" + i, "new_" + i));
    diffs.setDiff("status", previousStatus, nextStatus);
    IntStream.range(0, random.nextInt(3)).forEach(i -> diffs.setDiff("key_a" + i, "old_" + i, "new_" + i));

    IssueChangeDto changeDto = IssueChangeDto.of(issue.getKey(), diffs);
    dbTester.getDbClient().issueChangeDao().insert(dbSession, changeDto);
    return changeDto;
  }

  @SafeVarargs
  private final IssueDto insertNewClosedIssue(ComponentDto component, RuleType ruleType, Consumer<IssueDto>... consumers) {
    RuleDefinitionDto rule = dbTester.rules().insert(t -> t.setType(ruleType));
    return insertNewClosedIssue(component, rule, system2.now(), consumers);
  }

  @SafeVarargs
  private final IssueDto insertNewClosedIssue(ComponentDto component, RuleDefinitionDto rule, Consumer<IssueDto>... consumers) {
    return insertNewClosedIssue(component, rule, system2.now(), consumers);
  }

  @SafeVarargs
  private final IssueDto insertNewClosedIssue(ComponentDto component, RuleDefinitionDto rule, long issueCloseTime, Consumer<IssueDto>... consumers) {
    IssueDto res = new IssueDto()
      .setKee(UuidFactoryFast.getInstance().create())
      .setRuleId(rule.getId())
      .setType(rule.getType())
      .setComponentUuid(component.uuid())
      .setProjectUuid(component.projectUuid())
      .setStatus(Issue.STATUS_CLOSED)
      .setIssueCloseTime(issueCloseTime);
    Arrays.asList(consumers).forEach(c -> c.accept(res));
    underTest.insert(res);
    dbSession.commit();
    return res;
  }

  private static final RuleType[] SUPPORTED_RULE_TYPES = Arrays.stream(RuleType.values())
    .filter(t -> t != RuleType.SECURITY_HOTSPOT)
    .toArray(RuleType[]::new);

  @DataProvider
  public static Object[][] closedIssuesSupportedRuleTypes() {
    return Arrays.stream(SUPPORTED_RULE_TYPES)
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }

  private static RuleType randomSupportedRuleType() {
    return SUPPORTED_RULE_TYPES[new Random().nextInt(SUPPORTED_RULE_TYPES.length)];
  }

  private ComponentDto randomComponent(OrganizationDto organization) {
    ComponentDto project = dbTester.components().insertPublicProject(organization);
    ComponentDto module = dbTester.components().insertComponent(ComponentTesting.newModuleDto(project));
    ComponentDto dir = dbTester.components().insertComponent(ComponentTesting.newDirectory(project, "foo"));
    ComponentDto file = dbTester.components().insertComponent(ComponentTesting.newFileDto(project));
    ComponentDto[] components = new ComponentDto[] {project, module, dir, file};
    return components[random.nextInt(components.length)];
  }

  private IssueDto newIssue() {
    return new IssueDto()
      .setKee("ABCDE")
      .setComponentUuid(file.uuid())
      .setProjectUuid(project.uuid())
      .setRuleId(rule.getId())
      .setType(2)
      .setLine(500)
      .setGap(3.14)
      .setEffort(10L)
      .setResolution("FIXED")
      .setStatus("RESOLVED")
      .setSeverity("BLOCKER")
      .setAuthorLogin("morgan")
      .setAssigneeUuid("karadoc")
      .setIssueAttributes("JIRA=FOO-1234")
      .setChecksum("123456789")
      .setMessage("the message")
      .setIssueCreationTime(1_401_000_000_000L)
      .setIssueUpdateTime(1_402_000_000_000L)
      .setIssueCloseTime(1_403_000_000_000L)
      .setCreatedAt(1_400_000_000_000L)
      .setUpdatedAt(1_500_000_000_000L);
  }

  private static class RecorderResultHandler implements ResultHandler<IssueDto> {
    private final List<IssueDto> issues = new ArrayList<>();

    @Override
    public void handleResult(ResultContext<? extends IssueDto> resultContext) {
      issues.add(resultContext.getResultObject());
    }

    public List<IssueDto> getIssues() {
      return issues;
    }
  }
}
