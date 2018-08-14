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
import javax.annotation.Nullable;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
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

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@RunWith(DataProviderRunner.class)
public class IssueMapperTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbSession dbSession = dbTester.getSession();

  private IssueMapper underTest = dbSession.getMapper(IssueMapper.class);

  private ComponentDto project, file, file2;
  private RuleDto rule;
  private Random random = new Random();

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

    underTest.scrollClosedByComponentUuid(componentUuid, resultHandler);

    assertThat(resultHandler.issues).isEmpty();
  }

  @Test
  @UseDataProvider("closedIssuesSupportedRuleTypes")
  public void scrollClosedByComponentUuid_returns_closed_issues_with_at_least_one_line_diff(RuleType ruleType) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto component = randomComponent(organization);
    IssueDto expected = insertNewClosedIssue(component, ruleType);
    IssueChangeDto changeDto = insertNewLineDiff(expected);

    RecorderResultHandler resultHandler = new RecorderResultHandler();
    underTest.scrollClosedByComponentUuid(component.uuid(), resultHandler);

    assertThat(resultHandler.issues).hasSize(1);
    IssueDto issue = resultHandler.issues.iterator().next();
    assertThat(issue.getKey()).isEqualTo(issue.getKey());
    assertThat(issue.getLineChangeData()).contains(changeDto.getChangeData());
  }

  @Test
  @UseDataProvider("closedIssuesSupportedRuleTypes")
  public void scrollClosedByComponentUuid_does_not_return_closed_issues_of_non_existing_rule(RuleType ruleType) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto component = randomComponent(organization);
    IssueDto issueWithRule = insertNewClosedIssue(component, ruleType);
    IssueChangeDto issueChange = insertNewLineDiff(issueWithRule);
    IssueDto issueWithoutRule = insertNewClosedIssue(component, new RuleDefinitionDto().setType(ruleType).setId(-50));
    insertNewLineDiff(issueWithoutRule);

    RecorderResultHandler resultHandler = new RecorderResultHandler();
    underTest.scrollClosedByComponentUuid(component.uuid(), resultHandler);

    assertThat(resultHandler.issues)
      .extracting(IssueDto::getKey, t -> t.getLineChangeData().get())
      .containsOnly(tuple(issueWithRule.getKey(), issueChange.getChangeData()));
  }

  @Test
  @UseDataProvider("closedIssuesSupportedRuleTypes")
  public void scrollClosedByComponentUuid_does_not_return_closed_issues_of_orphan_component(RuleType ruleType) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto component = randomComponent(organization);
    IssueDto issue = insertNewClosedIssue(component, ruleType);
    IssueChangeDto issueChange = insertNewLineDiff(issue);
    IssueDto issueMissingComponent = insertNewClosedIssue(component, ruleType, t -> t.setComponentUuid("does_not_exist"));
    insertNewLineDiff(issueMissingComponent);
    IssueDto issueMissingProject = insertNewClosedIssue(component, ruleType, t -> t.setProjectUuid("does_not_exist"));
    insertNewLineDiff(issueMissingProject);

    RecorderResultHandler resultHandler = new RecorderResultHandler();
    underTest.scrollClosedByComponentUuid(component.uuid(), resultHandler);

    assertThat(resultHandler.issues)
      .extracting(IssueDto::getKey, t -> t.getLineChangeData().get())
      .containsOnly(tuple(issue.getKey(), issueChange.getChangeData()));
  }

  @Test
  @UseDataProvider("closedIssuesSupportedRuleTypes")
  public void scrollClosedByComponentUuid_does_not_return_closed_issues_without_any_line_diff(RuleType ruleType) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto component = randomComponent(organization);
    IssueDto issueWithLineDiff = insertNewClosedIssue(component, ruleType);
    IssueChangeDto issueChange = insertNewLineDiff(issueWithLineDiff);
    insertNewClosedIssue(component, ruleType);

    RecorderResultHandler resultHandler = new RecorderResultHandler();
    underTest.scrollClosedByComponentUuid(component.uuid(), resultHandler);

    assertThat(resultHandler.issues)
      .extracting(IssueDto::getKey, t -> t.getLineChangeData().get())
      .containsOnly(tuple(issueWithLineDiff.getKey(), issueChange.getChangeData()));
  }

  @Test
  @UseDataProvider("closedIssuesSupportedRuleTypes")
  public void scrollClosedByComponentUuid_does_not_return_closed_issues_of_type_SECURITY_HOTSPOT(RuleType ruleType) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto component = randomComponent(organization);
    IssueDto securityHotspotIssue = insertNewClosedIssue(component, RuleType.SECURITY_HOTSPOT);
    insertNewLineDiff(securityHotspotIssue);
    IssueDto issue = insertNewClosedIssue(component, ruleType);
    IssueChangeDto issueChange = insertNewLineDiff(issue);

    RecorderResultHandler resultHandler = new RecorderResultHandler();
    underTest.scrollClosedByComponentUuid(component.uuid(), resultHandler);

    assertThat(resultHandler.issues)
      .extracting(IssueDto::getKey, t -> t.getLineChangeData().get())
      .containsOnly(tuple(issue.getKey(), issueChange.getChangeData()));
  }

  @Test
  @UseDataProvider("closedIssuesSupportedRuleTypes")
  public void scrollClosedByComponentUuid_return_closed_issues_without_isHotspot_flag(RuleType ruleType) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto component = randomComponent(organization);
    IssueDto noHotspotFlagIssue = insertNewClosedIssue(component, ruleType);
    IssueChangeDto noFlagIssueChange = insertNewLineDiff(noHotspotFlagIssue);
    manuallySetToNullFromHotpotsColumn(noHotspotFlagIssue);
    IssueDto issue = insertNewClosedIssue(component, ruleType);
    IssueChangeDto issueChange = insertNewLineDiff(issue);

    RecorderResultHandler resultHandler = new RecorderResultHandler();
    underTest.scrollClosedByComponentUuid(component.uuid(), resultHandler);

    assertThat(resultHandler.issues)
      .extracting(IssueDto::getKey, t -> t.getLineChangeData().get())
      .containsOnly(
        tuple(issue.getKey(), issueChange.getChangeData()),
        tuple(noHotspotFlagIssue.getKey(), noFlagIssueChange.getChangeData()));
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
    insertNewLineDiff(fromHostSpotIssue);
    IssueDto issue = insertNewClosedIssue(component, ruleType);
    IssueChangeDto issueChange = insertNewLineDiff(issue);

    RecorderResultHandler resultHandler = new RecorderResultHandler();
    underTest.scrollClosedByComponentUuid(component.uuid(), resultHandler);

    assertThat(resultHandler.issues)
      .extracting(IssueDto::getKey, t -> t.getLineChangeData().get())
      .containsOnly(tuple(issue.getKey(), issueChange.getChangeData()));
  }

  @Test
  @UseDataProvider("closedIssuesSupportedRuleTypes")
  public void scrollClosedByComponentUuid_return_one_row_per_line_diff_sorted_by_most_recent_creation_date_first(RuleType ruleType) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto component = randomComponent(organization);
    IssueDto issue = insertNewClosedIssue(component, ruleType);
    Date date = new Date();
    IssueChangeDto changes[] = new IssueChangeDto[] {
      insertNewLineDiff(issue, DateUtils.addDays(date, -10)),
      insertNewLineDiff(issue, DateUtils.addDays(date, -60)),
      insertNewLineDiff(issue, date),
      insertNewLineDiff(issue, DateUtils.addDays(date, -5))
    };

    RecorderResultHandler resultHandler = new RecorderResultHandler();
    underTest.scrollClosedByComponentUuid(component.uuid(), resultHandler);

    assertThat(resultHandler.issues)
      .hasSize(4)
      .extracting(IssueDto::getKey, t -> t.getLineChangeData().get())
      .containsExactly(
        tuple(issue.getKey(), changes[2].getChangeData()),
        tuple(issue.getKey(), changes[3].getChangeData()),
        tuple(issue.getKey(), changes[0].getChangeData()),
        tuple(issue.getKey(), changes[1].getChangeData()));
  }

  private IssueChangeDto insertNewLineDiff(IssueDto issueDto) {
    return insertNewLineDiff(issueDto, new Date());
  }

  private IssueChangeDto insertNewLineDiff(IssueDto issueDto, Date date) {
    Integer oldLine = random.nextInt(10);
    Integer newLine = 10 + random.nextInt(10);
    Integer[][] values = new Integer[][] {
      {oldLine, newLine},
      {oldLine, null},
      {null, newLine},
    };
    Integer[] choice = values[random.nextInt(values.length)];
    return insertNewLineDiff(issueDto, date, choice[0], choice[1]);
  }

  private IssueChangeDto insertNewLineDiff(IssueDto issue, Date creationDate, @Nullable Integer before, @Nullable Integer after) {
    checkArgument(before != null || after != null);

    FieldDiffs diffs = new FieldDiffs()
      .setCreationDate(creationDate);
    IntStream.range(0, random.nextInt(3)).forEach(i -> diffs.setDiff("key_b" + i, "old_" + i, "new_" + i));
    diffs.setDiff("line", toDiffValue(before), toDiffValue(after));
    IntStream.range(0, random.nextInt(3)).forEach(i -> diffs.setDiff("key_a" + i, "old_" + i, "new_" + i));

    IssueChangeDto changeDto = IssueChangeDto.of(issue.getKey(), diffs);
    dbTester.getDbClient().issueChangeDao().insert(dbSession, changeDto);
    return changeDto;
  }

  private static String toDiffValue(@Nullable Integer after) {
    return after == null ? "" : String.valueOf(after);
  }

  @SafeVarargs
  private final IssueDto insertNewClosedIssue(ComponentDto component, RuleType ruleType, Consumer<IssueDto>... consumers) {
    RuleDefinitionDto rule = dbTester.rules().insert(t -> t.setType(ruleType));
    return insertNewClosedIssue(component, rule, consumers);
  }

  @SafeVarargs
  private final IssueDto insertNewClosedIssue(ComponentDto component, RuleDefinitionDto rule, Consumer<IssueDto>... consumers) {
    IssueDto res = new IssueDto()
      .setKee(UuidFactoryFast.getInstance().create())
      .setRuleId(rule.getId())
      .setType(rule.getType())
      .setComponentUuid(component.uuid())
      .setProjectUuid(component.projectUuid())
      .setStatus(Issue.STATUS_CLOSED);
    Arrays.asList(consumers).forEach(c -> c.accept(res));
    underTest.insert(res);
    dbSession.commit();
    return res;
  }

  @DataProvider
  public static Object[][] closedIssuesSupportedRuleTypes() {
    return Arrays.stream(RuleType.values())
      .filter(t -> t != RuleType.SECURITY_HOTSPOT)
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
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
