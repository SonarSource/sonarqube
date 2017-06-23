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
package org.sonarqube.tests.dbCleaner;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category4Suite;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.sonarqube.ws.WsMeasures;
import org.sonarqube.ws.WsMeasures.SearchHistoryResponse.HistoryValue;
import org.sonarqube.ws.client.measure.SearchHistoryRequest;
import util.ItUtils;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang.time.DateUtils.addDays;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static util.ItUtils.formatDate;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.runProjectAnalysis;
import static util.ItUtils.setServerProperty;

@Ignore
public class PurgeTest {

  private static final String COUNT_FILE_MEASURES = "project_measures pm, projects p where p.uuid = pm.component_uuid and p.scope='FIL'";
  private static final String COUNT_DIR_MEASURES = "project_measures pm, projects p where p.uuid = pm.component_uuid and p.scope='DIR'";
  private static final String PROJECT_KEY = "com.sonarsource.it.samples:multi-modules-sample";
  private static final String PROJECT_SAMPLE_PATH = "dbCleaner/xoo-multi-modules-sample";

  private static final String ONE_DAY_AGO = DateFormatUtils.ISO_DATE_FORMAT.format(addDays(new Date(), -1));
  private static final String TWO_DAYS_AGO = DateFormatUtils.ISO_DATE_FORMAT.format(addDays(new Date(), -2));

  @ClassRule
  public static final Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Rule
  public ErrorCollector collector = new ErrorCollector();

  @Before
  public void deleteProjectData() {
    orchestrator.resetData();

    orchestrator.getServer().provisionProject(PROJECT_KEY, PROJECT_KEY);

    ItUtils.restoreProfile(orchestrator, getClass().getResource("/dbCleaner/one-issue-per-line-profile.xml"));

    setServerProperty(orchestrator, "sonar.dbcleaner.cleanDirectory", null);
    setServerProperty(orchestrator, "sonar.dbcleaner.hoursBeforeKeepingOnlyOneSnapshotByDay", null);
    setServerProperty(orchestrator, "sonar.dbcleaner.weeksBeforeKeepingOnlyOneSnapshotByWeek", null);
  }

  @Test
  public void test_evolution_of_number_of_rows_when_scanning_two_times_the_same_project() {
    Date today = new Date();
    Date yesterday = DateUtils.addDays(today, -1);

    scan(PROJECT_SAMPLE_PATH, DateFormatUtils.ISO_DATE_FORMAT.format(yesterday));

    // count components
    collector.checkThat("Wrong number of projects", count("projects where qualifier in ('TRK','BRC')"), equalTo(7));
    collector.checkThat("Wrong number of directories", count("projects where qualifier in ('DIR')"), equalTo(4));
    collector.checkThat("Wrong number of files", count("projects where qualifier in ('FIL')"), equalTo(4));
    collector.checkThat("Wrong number of unit test files", count("projects where qualifier in ('UTS')"), equalTo(0));

    int measuresOnTrk = 45;
    int measuresOnBrc = 222;
    int measuresOnDir = 141;
    int measuresOnFil = 69;

    // count measures 
    assertMeasuresCountForQualifier("TRK", measuresOnTrk);
    assertMeasuresCountForQualifier("BRC", measuresOnBrc);
    assertMeasuresCountForQualifier("DIR", measuresOnDir);
    assertMeasuresCountForQualifier("FIL", measuresOnFil);

    // No new_* metrics measure should be recorded the first time
    collector.checkThat(
      "Wrong number of measure of new_ metrics",
      count("project_measures, metrics where metrics.id = project_measures.metric_id and metrics.name like 'new_%'"),
      equalTo(0));

    int expectedMeasures = measuresOnTrk + measuresOnBrc + measuresOnDir + measuresOnFil;
    collector.checkThat("Wrong number of measures", count("project_measures"), equalTo(expectedMeasures));
    collector.checkThat("Wrong number of measure data", count("project_measures where measure_data is not null"), equalTo(0));

    // count other tables that are constant between 2 scans
    int expectedIssues = 52;

    collector.checkThat("Wrong number of issues", count("issues"), equalTo(expectedIssues));

    // must be a different date, else a single snapshot is kept per day
    scan(PROJECT_SAMPLE_PATH, DateFormatUtils.ISO_DATE_FORMAT.format(today));

    int newMeasuresOnTrk = 58;
    int newMeasuresOnBrc = 304;
    int newMeasuresOnDir = 56;
    int newMeasuresOnFil = 0;

    assertMeasuresCountForQualifier("TRK", measuresOnTrk + newMeasuresOnTrk);
    assertMeasuresCountForQualifier("BRC", measuresOnBrc + newMeasuresOnBrc);
    assertMeasuresCountForQualifier("DIR", measuresOnDir + newMeasuresOnDir);
    assertMeasuresCountForQualifier("FIL", measuresOnFil + newMeasuresOnFil);

    // Measures on new_* metrics should be recorded
    collector.checkThat(
      "Wrong number of measure of new_ metrics",
      count("project_measures, metrics where metrics.id = project_measures.metric_id and metrics.name like 'new_%'"),
      equalTo(154));

    // added measures relate to project and new_* metrics
    expectedMeasures += newMeasuresOnTrk + newMeasuresOnBrc + newMeasuresOnDir + newMeasuresOnFil;
    collector.checkThat("Wrong number of measures after second analysis", count("project_measures"), equalTo(expectedMeasures));
    collector.checkThat("Wrong number of measure data", count("project_measures where measure_data is not null"), equalTo(0));
    collector.checkThat("Wrong number of issues", count("issues"), equalTo(expectedIssues));
  }

  /**
   * SONAR-3378
   */
  @Test
  public void should_keep_all_snapshots_the_first_day() {
    // analyse once
    scan(PROJECT_SAMPLE_PATH);
    // analyse twice
    scan(PROJECT_SAMPLE_PATH);
    // and check we have 2 snapshots
    assertThat(count("snapshots s where s.component_uuid=(select p.uuid from projects p where p.kee='com.sonarsource.it.samples:multi-modules-sample')")).isEqualTo(2);
  }

  /**
   * SONAR-2807 & SONAR-3378 & SONAR-4710
   */
  @Test
  public void should_keep_only_one_snapshot_per_day() {
    scan(PROJECT_SAMPLE_PATH);

    int snapshotsCount = count("snapshots");
    int measuresCount = count("project_measures");
    // Using the "sonar.dbcleaner.hoursBeforeKeepingOnlyOneSnapshotByDay" property set to '0' is the way
    // to keep only 1 snapshot per day
    setServerProperty(orchestrator, "sonar.dbcleaner.hoursBeforeKeepingOnlyOneSnapshotByDay", "0");
    scan(PROJECT_SAMPLE_PATH);
    assertThat(count("snapshots")).as("Different number of snapshots").isEqualTo(snapshotsCount);

    int measureOnNewMetrics = count("project_measures, metrics where metrics.id = project_measures.metric_id and metrics.name like 'new_%'");
    // Number of measures should be the same as previous, with the measures on new metrics
    assertThat(count("project_measures")).as("Different number of measures").isEqualTo(measuresCount + measureOnNewMetrics);
  }

  /**
   * SONAR-7175
   */
  @Test
  public void keep_latest_snapshot() {
    // Keep all snapshots from last 4 weeks
    setServerProperty(orchestrator, "sonar.dbcleaner.weeksBeforeKeepingOnlyOneSnapshotByWeek", "4");

    Date oneWeekAgo = addDays(new Date(), -7);

    // Execute an analysis wednesday last week
    Calendar lastWednesday = Calendar.getInstance();
    lastWednesday.setTime(oneWeekAgo);
    lastWednesday.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
    String lastWednesdayFormatted = formatDate(lastWednesday.getTime());
    runProjectAnalysis(orchestrator, PROJECT_SAMPLE_PATH, "sonar.projectDate", lastWednesdayFormatted);

    // Execute an analysis thursday last week
    Calendar lastThursday = Calendar.getInstance();
    lastThursday.setTime(oneWeekAgo);
    lastThursday.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
    String lastThursdayFormatted = formatDate(lastThursday.getTime());
    runProjectAnalysis(orchestrator, PROJECT_SAMPLE_PATH, "sonar.projectDate", lastThursdayFormatted);

    // Now only keep 1 snapshot per week
    setServerProperty(orchestrator, "sonar.dbcleaner.weeksBeforeKeepingOnlyOneSnapshotByWeek", "0");

    // Execute an analysis today to execute the purge of previous weeks snapshots
    runProjectAnalysis(orchestrator, PROJECT_SAMPLE_PATH);

    // Check that only analysis from last thursday is kept (as it's the last one from previous week)
    WsMeasures.SearchHistoryResponse response = newAdminWsClient(orchestrator).measures().searchHistory(SearchHistoryRequest.builder()
      .setComponent(PROJECT_KEY)
      .setMetrics(singletonList("ncloc"))
      .build());
    assertThat(response.getMeasuresCount()).isEqualTo(1);
    assertThat(response.getMeasuresList().get(0).getHistoryList()).extracting(HistoryValue::getDate).doesNotContain(lastWednesdayFormatted, lastThursdayFormatted);
  }

  /**
   * SONAR-3120
   */
  @Test
  public void should_delete_removed_modules() {
    scan("dbCleaner/modules/before");
    assertExists("com.sonarsource.it.samples:multi-modules-sample:module_b");
    assertExists("com.sonarsource.it.samples:multi-modules-sample:module_b:module_b1");

    // we want the previous snapshot to be purged
    setServerProperty(orchestrator, "sonar.dbcleaner.hoursBeforeKeepingOnlyOneSnapshotByDay", "0");

    scan("dbCleaner/modules/after");
    assertDisabled("com.sonarsource.it.samples:multi-modules-sample:module_b");
    assertDisabled("com.sonarsource.it.samples:multi-modules-sample:module_b:module_b1");
    assertExists("com.sonarsource.it.samples:multi-modules-sample:module_c:module_c1");
  }

  /**
   * SONAR-3120
   */
  @Test
  public void should_delete_removed_files() {
    String fileKey = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:src/main/xoo/com/sonar/it/samples/modules/a1/HelloA1.xoo";
    scan("dbCleaner/files/before");
    assertExists(fileKey);

    scan("dbCleaner/files/after");
    assertDisabled(fileKey);
    assertExists("com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:src/main/xoo/com/sonar/it/samples/modules/a1/NewHelloA1.xoo");
  }

  /**
   * SONAR-2754
   */
  @Test
  public void should_delete_historical_data_of_directories_by_default() {
    scan(PROJECT_SAMPLE_PATH, TWO_DAYS_AGO);

    int fileMeasures = count(COUNT_FILE_MEASURES);
    int dirMeasures = count(COUNT_DIR_MEASURES);

    scan(PROJECT_SAMPLE_PATH, ONE_DAY_AGO);

    // second analysis with new_* metrics
    assertThat(count(COUNT_FILE_MEASURES)).isLessThan(2 * fileMeasures);
    assertThat(count(COUNT_DIR_MEASURES)).isLessThan(2 * dirMeasures);
  }

  /**
   * SONAR-2754
   */
  @Test
  public void should_not_delete_historical_data_of_directories() {
    scan(PROJECT_SAMPLE_PATH, TWO_DAYS_AGO);

    int fileMeasures = count(COUNT_FILE_MEASURES);
    int dirMeasures = count(COUNT_DIR_MEASURES);

    setServerProperty(orchestrator, "sonar.dbcleaner.cleanDirectory", "false");

    scan(PROJECT_SAMPLE_PATH, ONE_DAY_AGO);

    // second analysis as NEW_* metrics
    assertThat(count(COUNT_FILE_MEASURES)).isLessThan(2 * fileMeasures);
    assertThat(count(COUNT_DIR_MEASURES)).isGreaterThan(2 * dirMeasures);
  }

  /**
   * SONAR-2061
   */
  @Test
  public void should_delete_historical_data_of_flagged_metrics() {
    scan(PROJECT_SAMPLE_PATH, TWO_DAYS_AGO);

    // historical data of complexity_in_classes is supposed to be deleted (see CoreMetrics)
    String selectNcloc = "project_measures where metric_id in (select id from metrics where name='ncloc')";
    String selectComplexityInClasses = "project_measures where metric_id in (select id from metrics where name='complexity_in_classes')";
    int nclocCount = count(selectNcloc);
    int complexitInClassesCount = count(selectComplexityInClasses);

    scan(PROJECT_SAMPLE_PATH, ONE_DAY_AGO);
    assertThat(count(selectNcloc)).isGreaterThan(nclocCount);
    assertThat(count(selectComplexityInClasses)).isEqualTo(complexitInClassesCount);
  }

  private void assertDisabled(String key) {
    assertThat(enabledStatusOfComponent(key)).isFalse();
  }

  private void assertExists(String key) {
    assertThat(enabledStatusOfComponent(key)).isTrue();
  }

  private Boolean enabledStatusOfComponent(String key) {
    return orchestrator.getDatabase().executeSql("select enabled from projects p where p.kee='" + key + "'")
      .stream()
      .findFirst()
      .map(PurgeTest::toBoolean)
      .orElse(null);
  }

  private static Boolean toBoolean(Map<String, String> s) {
    String value = s.get("ENABLED");
    if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("t") || value.equals("1")) {
      return true;
    }
    if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("f") || value.equals("0")) {
      return false;
    }
    throw new IllegalArgumentException("Unsupported value can not be converted to boolean " + value);
  }

  private BuildResult scan(String path, String date) {
    return scan(path, "sonar.projectDate", date);
  }

  private BuildResult scan(String path, String... extraProperties) {
    SonarScanner runner = configureRunner(path, extraProperties);
    return orchestrator.executeBuild(runner);
  }

  private SonarScanner configureRunner(String projectPath, String... props) {
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT_KEY, "xoo", "one-issue-per-line-profile");
    return SonarScanner.create(ItUtils.projectDir(projectPath)).setProperties(props);
  }

  private int count(String condition) {
    return orchestrator.getDatabase().countSql("select count(1) from " + condition);
  }

  private void assertMeasuresCountForQualifier(String qualifier, int count) {
    int result = countMeasures(qualifier);
    if (result != count) {
      logMeasures("GOT", qualifier);
    }
    collector.checkThat("Wrong number of measures for qualifier " + qualifier, result, equalTo(count));
  }

  private int countMeasures(String qualifier) {
    String sql = "SELECT count(1) FROM project_measures pm, projects p, metrics m where p.uuid=pm.component_uuid and pm.metric_id=m.id and p.qualifier='" + qualifier + "'";
    return orchestrator.getDatabase().countSql(sql);
  }

  private void logMeasures(String title, String qualifier) {
    String sql = "SELECT m.name as metricName, pm.value as value, pm.text_value as textValue, pm.variation_value_1, pm.variation_value_2, pm.variation_value_3 "
      +
      "FROM project_measures pm, projects p, metrics m " +
      "WHERE pm.component_uuid=p.uuid and pm.metric_id=m.id and p.qualifier='"
      + qualifier + "'";
    List<Map<String, String>> rows = orchestrator.getDatabase().executeSql(sql);

    System.out.println("---- " + title + " - measures on qualifier " + qualifier);
    for (Map<String, String> row : rows) {
      System.out.println("  " + row);
    }
  }

}
