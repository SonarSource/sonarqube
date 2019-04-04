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
package org.sonar.ce.task.projectanalysis.issue;

import com.google.common.collect.ImmutableList;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.issue.Issue;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.utils.DateUtils.addDays;
import static org.sonar.api.utils.DateUtils.parseDateTime;

@RunWith(DataProviderRunner.class)
public class ComponentIssuesLoaderTest {
  private static final Date NOW = parseDateTime("2018-08-17T13:44:53+0000");
  private static final Date DATE_LIMIT_30_DAYS_BACK_MIDNIGHT = parseDateTime("2018-07-18T00:00:00+0000");

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = db.getDbClient();
  private System2 system2 = mock(System2.class);

  @Test
  public void loadClosedIssues_returns_single_DefaultIssue_by_issue_based_on_first_row() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));
    RuleDefinitionDto rule = db.rules().insert(t -> t.setType(CODE_SMELL));
    Date issueDate = addDays(NOW, -10);
    IssueDto issue = db.issues().insert(rule, project, file, t -> t.setStatus(STATUS_CLOSED).setIssueCloseDate(issueDate).setType(CODE_SMELL).setIsFromHotspot(false));
    db.issues().insertFieldDiffs(issue, newToClosedDiffsWithLine(issueDate, 10));
    db.issues().insertFieldDiffs(issue, newToClosedDiffsWithLine(addDays(issueDate, 3), 20));
    db.issues().insertFieldDiffs(issue, newToClosedDiffsWithLine(addDays(issueDate, 1), 30));
    when(system2.now()).thenReturn(NOW.getTime());

    ComponentIssuesLoader underTest = newComponentIssuesLoader(newEmptySettings());
    List<DefaultIssue> defaultIssues = underTest.loadClosedIssues(file.uuid());

    assertThat(defaultIssues).hasSize(1);
    assertThat(defaultIssues.iterator().next().getLine()).isEqualTo(20);
  }

  @Test
  public void loadClosedIssues_returns_single_DefaultIssue_with_null_line_if_first_row_has_no_line_diff() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));
    RuleDefinitionDto rule = db.rules().insert(t -> t.setType(CODE_SMELL));
    Date issueDate = addDays(NOW, -10);
    IssueDto issue = db.issues().insert(rule, project, file, t -> t.setStatus(STATUS_CLOSED).setIssueCloseDate(issueDate).setType(CODE_SMELL).setIsFromHotspot(false));
    db.issues().insertFieldDiffs(issue, newToClosedDiffsWithLine(issueDate, 10));
    db.issues().insertFieldDiffs(issue, newToClosedDiffsWithLine(addDays(issueDate, 2), null));
    db.issues().insertFieldDiffs(issue, newToClosedDiffsWithLine(addDays(issueDate, 1), 30));
    when(system2.now()).thenReturn(NOW.getTime());

    ComponentIssuesLoader underTest = newComponentIssuesLoader(newEmptySettings());
    List<DefaultIssue> defaultIssues = underTest.loadClosedIssues(file.uuid());

    assertThat(defaultIssues).hasSize(1);
    assertThat(defaultIssues.iterator().next().getLine()).isNull();
  }

  @Test
  public void loadClosedIssues_returns_only_closed_issues_with_close_date() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));
    RuleDefinitionDto rule = db.rules().insert(t -> t.setType(CODE_SMELL));
    Date issueDate = addDays(NOW, -10);
    IssueDto closedIssue = db.issues().insert(rule, project, file, t -> t.setStatus(STATUS_CLOSED).setIssueCloseDate(issueDate).setType(CODE_SMELL).setIsFromHotspot(false));
    db.issues().insertFieldDiffs(closedIssue, newToClosedDiffsWithLine(issueDate, 10));
    IssueDto issueNoCloseDate = db.issues().insert(rule, project, file, t -> t.setStatus(STATUS_CLOSED).setIsFromHotspot(false));
    db.issues().insertFieldDiffs(issueNoCloseDate, newToClosedDiffsWithLine(issueDate, 10));
    when(system2.now()).thenReturn(NOW.getTime());

    ComponentIssuesLoader underTest = newComponentIssuesLoader(newEmptySettings());
    List<DefaultIssue> defaultIssues = underTest.loadClosedIssues(file.uuid());

    assertThat(defaultIssues)
      .extracting(DefaultIssue::key)
      .containsOnly(closedIssue.getKey());
  }

  @Test
  public void loadClosedIssues_returns_only_closed_issues_which_close_date_is_from_day_30_days_ago() {
    ComponentIssuesLoader underTest = newComponentIssuesLoader(newEmptySettings());
    loadClosedIssues_returns_only_closed_issues_with_close_date_is_from_30_days_ago(underTest);
  }

  @Test
  public void loadClosedIssues_returns_only_closed_issues_with_close_date_is_from_30_days_ago_if_property_is_empty() {
    Configuration configuration = newConfiguration(null);
    ComponentIssuesLoader underTest = newComponentIssuesLoader(configuration);

    loadClosedIssues_returns_only_closed_issues_with_close_date_is_from_30_days_ago(underTest);
  }

  @Test
  public void loadClosedIssues_returns_only_closed_with_close_date_is_from_30_days_ago_if_property_is_less_than_0() {
    Configuration configuration = newConfiguration(String.valueOf(-(1 + new Random().nextInt(10))));
    ComponentIssuesLoader underTest = newComponentIssuesLoader(configuration);

    loadClosedIssues_returns_only_closed_issues_with_close_date_is_from_30_days_ago(underTest);
  }

  @Test
  public void loadClosedIssues_returns_only_closed_with_close_date_is_from_30_days_ago_if_property_is_30() {
    Configuration configuration = newConfiguration("30");
    ComponentIssuesLoader underTest = newComponentIssuesLoader(configuration);

    loadClosedIssues_returns_only_closed_issues_with_close_date_is_from_30_days_ago(underTest);
  }

  @Test
  @UseDataProvider("notAnIntegerPropertyValues")
  public void loadClosedIssues_returns_only_closed_with_close_date_is_from_30_days_ago_if_property_is_not_an_integer(String notAnInteger) {
    Configuration configuration = newConfiguration(notAnInteger);
    ComponentIssuesLoader underTest = newComponentIssuesLoader(configuration);

    loadClosedIssues_returns_only_closed_issues_with_close_date_is_from_30_days_ago(underTest);
  }

  @DataProvider
  public static Object[][] notAnIntegerPropertyValues() {
    return new Object[][] {
      {"foo"},
      {"1,3"},
      {"1.3"},
      {"-3.14"}
    };
  }

  private void loadClosedIssues_returns_only_closed_issues_with_close_date_is_from_30_days_ago(ComponentIssuesLoader underTest) {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));
    RuleDefinitionDto rule = db.rules().insert(t -> t.setType(CODE_SMELL));
    Date[] issueDates = new Date[] {
      addDays(NOW, -10),
      addDays(NOW, -31),
      addDays(NOW, -30),
      DATE_LIMIT_30_DAYS_BACK_MIDNIGHT,
      addDays(NOW, -29),
      addDays(NOW, -60),
    };
    IssueDto[] issues = Arrays.stream(issueDates)
      .map(issueDate -> {
        IssueDto closedIssue = db.issues().insert(rule, project, file, t -> t.setStatus(STATUS_CLOSED).setIssueCloseDate(issueDate).setType(CODE_SMELL).setIsFromHotspot(false));
        db.issues().insertFieldDiffs(closedIssue, newToClosedDiffsWithLine(issueDate, 10));
        return closedIssue;
      })
      .toArray(IssueDto[]::new);
    when(system2.now()).thenReturn(NOW.getTime());

    List<DefaultIssue> defaultIssues = underTest.loadClosedIssues(file.uuid());

    assertThat(defaultIssues)
      .extracting(DefaultIssue::key)
      .containsOnly(issues[0].getKey(), issues[2].getKey(), issues[3].getKey(), issues[4].getKey());
  }

  @Test
  public void loadClosedIssues_returns_empty_without_querying_DB_if_property_is_0() {
    System2 system2 = mock(System2.class);
    DbClient dbClient = mock(DbClient.class);
    Configuration configuration = newConfiguration("0");
    String componentUuid = randomAlphabetic(15);
    ComponentIssuesLoader underTest = new ComponentIssuesLoader(dbClient,
      null /* not used in loadClosedIssues */, null /* not used in loadClosedIssues */, configuration, system2);

    assertThat(underTest.loadClosedIssues(componentUuid)).isEmpty();

    verifyZeroInteractions(dbClient, system2);
  }

  @Test
  public void loadLatestDiffChangesForReopeningOfClosedIssues_does_not_query_DB_if_issue_list_is_empty() {
    DbClient dbClient = mock(DbClient.class);
    ComponentIssuesLoader underTest = new ComponentIssuesLoader(dbClient,
      null /* not used in method */, null /* not used in method */, newConfiguration("0"), null /* not used by method */);

    underTest.loadLatestDiffChangesForReopeningOfClosedIssues(emptyList());

    verifyZeroInteractions(dbClient, system2);
  }

  @Test
  @UseDataProvider("statusOrResolutionFieldName")
  public void loadLatestDiffChangesForReopeningOfClosedIssues_add_diff_change_with_most_recent_status_or_resolution(String statusOrResolutionFieldName) {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));
    RuleDefinitionDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, file);
    db.issues().insertChange(issue, t -> t.setChangeData(randomDiffWith(statusOrResolutionFieldName, "val1")).setIssueChangeCreationDate(5));
    db.issues().insertChange(issue, t -> t.setChangeData(randomDiffWith(statusOrResolutionFieldName, "val2")).setIssueChangeCreationDate(20));
    db.issues().insertChange(issue, t -> t.setChangeData(randomDiffWith(statusOrResolutionFieldName, "val3")).setIssueChangeCreationDate(13));
    ComponentIssuesLoader underTest = new ComponentIssuesLoader(dbClient,
      null /* not used in method */, null /* not used in method */, newConfiguration("0"), null /* not used by method */);
    DefaultIssue defaultIssue = new DefaultIssue().setKey(issue.getKey());

    underTest.loadLatestDiffChangesForReopeningOfClosedIssues(ImmutableList.of(defaultIssue));

    assertThat(defaultIssue.changes())
      .hasSize(1);
    assertThat(defaultIssue.changes())
      .extracting(t -> t.get(statusOrResolutionFieldName))
      .filteredOn(t -> hasValue(t, "val2"))
      .hasSize(1);
  }

  @Test
  public void loadLatestDiffChangesForReopeningOfClosedIssues_add_single_diff_change_when_most_recent_status_and_resolution_is_the_same_diff() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));
    RuleDefinitionDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, file);
    db.issues().insertChange(issue, t -> t.setChangeData(randomDiffWith("status", "valStatus1")).setIssueChangeCreationDate(5));
    db.issues().insertChange(issue, t -> t.setChangeData(randomDiffWith("status", "valStatus2")).setIssueChangeCreationDate(19));
    db.issues().insertChange(issue, t -> t.setChangeData(randomDiffWith("status", "valStatus3", "resolution", "valRes3")).setIssueChangeCreationDate(20));
    db.issues().insertChange(issue, t -> t.setChangeData(randomDiffWith("resolution", "valRes4")).setIssueChangeCreationDate(13));
    ComponentIssuesLoader underTest = new ComponentIssuesLoader(dbClient,
      null /* not used in method */, null /* not used in method */, newConfiguration("0"), null /* not used by method */);
    DefaultIssue defaultIssue = new DefaultIssue().setKey(issue.getKey());

    underTest.loadLatestDiffChangesForReopeningOfClosedIssues(ImmutableList.of(defaultIssue));

    assertThat(defaultIssue.changes())
      .hasSize(1);
    assertThat(defaultIssue.changes())
      .extracting(t -> t.get("status"))
      .filteredOn(t -> hasValue(t, "valStatus3"))
      .hasSize(1);
    assertThat(defaultIssue.changes())
      .extracting(t -> t.get("resolution"))
      .filteredOn(t -> hasValue(t, "valRes3"))
      .hasSize(1);
  }

  @Test
  public void loadLatestDiffChangesForReopeningOfClosedIssues_adds_2_diff_changes_if_most_recent_status_and_resolution_are_not_the_same_diff() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));
    RuleDefinitionDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, file);
    db.issues().insertChange(issue, t -> t.setChangeData(randomDiffWith("status", "valStatus1")).setIssueChangeCreationDate(5));
    db.issues().insertChange(issue, t -> t.setChangeData(randomDiffWith("status", "valStatus2", "resolution", "valRes2")).setIssueChangeCreationDate(19));
    db.issues().insertChange(issue, t -> t.setChangeData(randomDiffWith("status", "valStatus3")).setIssueChangeCreationDate(20));
    db.issues().insertChange(issue, t -> t.setChangeData(randomDiffWith("resolution", "valRes4")).setIssueChangeCreationDate(13));
    ComponentIssuesLoader underTest = new ComponentIssuesLoader(dbClient,
      null /* not used in method */, null /* not used in method */, newConfiguration("0"), null /* not used by method */);
    DefaultIssue defaultIssue = new DefaultIssue().setKey(issue.getKey());

    underTest.loadLatestDiffChangesForReopeningOfClosedIssues(ImmutableList.of(defaultIssue));

    assertThat(defaultIssue.changes())
      .hasSize(2);
    assertThat(defaultIssue.changes())
      .extracting(t -> t.get("status"))
      .filteredOn(t -> hasValue(t, "valStatus3"))
      .hasSize(1);
    assertThat(defaultIssue.changes())
      .extracting(t -> t.get("resolution"))
      .filteredOn(t -> hasValue(t, "valRes2"))
      .hasSize(1);
  }

  private static boolean hasValue(@Nullable FieldDiffs.Diff t, String value) {
    if (t == null) {
      return false;
    }
    return (t.oldValue() == null || value.equals(t.oldValue())) && (t.newValue() == null || value.equals(t.newValue()));
  }

  @DataProvider
  public static Object[][] statusOrResolutionFieldName() {
    return new Object[][] {
      {"status"},
      {"resolution"},
    };
  }

  private static String randomDiffWith(String... fieldsAndValues) {
    Random random = new Random();
    List<Diff> diffs = new ArrayList<>();
    for (int i = 0; i < fieldsAndValues.length; i++) {
      int oldOrNew = random.nextInt(3);
      String value = fieldsAndValues[i + 1];
      diffs.add(new Diff(fieldsAndValues[i], oldOrNew <= 2 ? value : null, oldOrNew >= 2 ? value : null));
      i++;
    }
    IntStream.range(0, random.nextInt(5))
      .forEach(i -> diffs.add(new Diff(randomAlphabetic(10), random.nextBoolean() ? null : randomAlphabetic(11), random.nextBoolean() ? null : randomAlphabetic(12))));
    Collections.shuffle(diffs);

    FieldDiffs res = new FieldDiffs();
    diffs.forEach(diff -> res.setDiff(diff.field, diff.oldValue, diff.newValue));
    return res.toEncodedString();
  }

  private static final class Diff {
    private final String field;
    private final String oldValue;
    private final String newValue;

    private Diff(String field, @Nullable String oldValue, @Nullable String newValue) {
      this.field = field;
      this.oldValue = oldValue;
      this.newValue = newValue;
    }
  }

  private static FieldDiffs newToClosedDiffsWithLine(Date creationDate, @Nullable Integer oldLineValue) {
    FieldDiffs fieldDiffs = new FieldDiffs().setCreationDate(addDays(creationDate, -5))
      .setDiff("status", randomNonCloseStatus(), STATUS_CLOSED);
    if (oldLineValue != null) {
      fieldDiffs.setDiff("line", oldLineValue, "");
    }
    return fieldDiffs;
  }

  private static String randomNonCloseStatus() {
    String[] nonCloseStatuses = Issue.STATUSES.stream()
      .filter(t -> !STATUS_CLOSED.equals(t))
      .toArray(String[]::new);
    return nonCloseStatuses[new Random().nextInt(nonCloseStatuses.length)];
  }

  private ComponentIssuesLoader newComponentIssuesLoader(Configuration configuration) {
    return new ComponentIssuesLoader(dbClient,
      null /* not used in loadClosedIssues */, null /* not used in loadClosedIssues */, configuration, system2);
  }

  private static Configuration newEmptySettings() {
    return new MapSettings().asConfig();
  }

  private static Configuration newConfiguration(@Nullable String maxAge) {
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.issuetracking.closedissues.maxage", maxAge);
    return settings.asConfig();
  }
}
