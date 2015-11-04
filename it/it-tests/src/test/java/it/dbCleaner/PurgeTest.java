/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package it.dbCleaner;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import it.Category4Suite;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.sonar.wsclient.services.PropertyDeleteQuery;
import org.sonar.wsclient.services.PropertyUpdateQuery;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class PurgeTest {

  static final String PROJECT_KEY = "com.sonarsource.it.samples:multi-modules-sample";
  static final String PROJECT_SAMPLE_PATH = "dbCleaner/xoo-multi-modules-sample";

  @ClassRule
  public static final Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Rule
  public ErrorCollector collector = new ErrorCollector();

  @Before
  public void deleteProjectData() {
    orchestrator.resetData();

    orchestrator.getServer().provisionProject(PROJECT_KEY, PROJECT_KEY);

    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/dbCleaner/one-issue-per-line-profile.xml"));
    orchestrator.getServer().getAdminWsClient().delete(new PropertyDeleteQuery("sonar.dbcleaner.hoursBeforeKeepingOnlyOneSnapshotByDay"));
    orchestrator.getServer().getAdminWsClient().delete(new PropertyDeleteQuery("sonar.dbcleaner.cleanDirectory"));
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

    int measuresOnTrk = 47;
    int measuresOnBrc = 234;
    int measuresOnDir = 117;
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

    int newMeasuresOnTrk = 53;
    int newMeasuresOnBrc = 274;
    int newMeasuresOnDir = 32;
    int newMeasuresOnFil = 0;

    assertMeasuresCountForQualifier("TRK", measuresOnTrk + newMeasuresOnTrk);
    assertMeasuresCountForQualifier("BRC", measuresOnBrc + newMeasuresOnBrc);
    assertMeasuresCountForQualifier("DIR", measuresOnDir + newMeasuresOnDir);
    assertMeasuresCountForQualifier("FIL", measuresOnFil + newMeasuresOnFil);

    // Measures on new_* metrics should be recorded
    collector.checkThat(
      "Wrong number of measure of new_ metrics",
      count("project_measures, metrics where metrics.id = project_measures.metric_id and metrics.name like 'new_%'"),
      equalTo(88));

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
    assertThat(count("snapshots s where s.project_id=(select p.id from projects p where p.kee='com.sonarsource.it.samples:multi-modules-sample')")).isEqualTo(2);
  }

  /**
   * SONAR-2807 & SONAR-3378 & SONAR-4710
   */
  @Test
  public void should_keep_only_one_snapshot_per_day() {
    scan(PROJECT_SAMPLE_PATH);

    int snapshotsCount = count("snapshots where qualifier<>'LIB'");
    int measuresCount = count("project_measures");
    // Using the "sonar.dbcleaner.hoursBeforeKeepingOnlyOneSnapshotByDay" property set to '0' is the way
    // to keep only 1 snapshot per day
    orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.dbcleaner.hoursBeforeKeepingOnlyOneSnapshotByDay", "0"));
    scan(PROJECT_SAMPLE_PATH);
    assertThat(count("snapshots where qualifier<>'LIB'")).as("Different number of snapshots").isEqualTo(snapshotsCount);

    int measureOnNewMetrics = count("project_measures, metrics where metrics.id = project_measures.metric_id and metrics.name like 'new_%'");
    // Number of measures should be the same as previous, with the measures on new metrics
    assertThat(count("project_measures")).as("Different number of measures").isEqualTo(measuresCount + measureOnNewMetrics);
  }

  /**
   * SONAR-3120
   */
  @Test
  public void should_delete_removed_modules() {
    scan("dbCleaner/modules/before");
    assertSingleSnapshot("com.sonarsource.it.samples:multi-modules-sample:module_b");
    assertSingleSnapshot("com.sonarsource.it.samples:multi-modules-sample:module_b:module_b1");

    // we want the previous snapshot to be purged
    orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.dbcleaner.hoursBeforeKeepingOnlyOneSnapshotByDay", "0"));
    scan("dbCleaner/modules/after");
    assertDeleted("com.sonarsource.it.samples:multi-modules-sample:module_b");
    assertDeleted("com.sonarsource.it.samples:multi-modules-sample:module_b:module_b1");
    assertSingleSnapshot("com.sonarsource.it.samples:multi-modules-sample:module_c:module_c1");
  }

  /**
   * SONAR-3120
   */
  @Test
  public void should_delete_removed_files() {
    scan("dbCleaner/files/before");
    assertSingleSnapshot("com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:src/main/xoo/com/sonar/it/samples/modules/a1/HelloA1.xoo");

    scan("dbCleaner/files/after");
    assertDeleted("src/main/xoo/com/sonar/it/samples/modules/a1/HelloA1.xoo");
    assertSingleSnapshot("com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:src/main/xoo/com/sonar/it/samples/modules/a1/NewHelloA1.xoo");
  }

  /**
   * SONAR-2754
   */
  @Test
  public void should_delete_historical_data_of_directories_by_default() {
    scan(PROJECT_SAMPLE_PATH, "2012-01-01");
    String select = "snapshots where scope='DIR'";
    int directorySnapshots = count(select);

    scan(PROJECT_SAMPLE_PATH, "2012-02-02");
    assertThat(count(select)).isEqualTo(directorySnapshots);
  }

  /**
   * SONAR-2754
   */
  @Test
  public void should_not_delete_historical_data_of_directories() {
    scan(PROJECT_SAMPLE_PATH, "2012-01-01");

    String select = "snapshots where scope='DIR'";
    int directorySnapshots = count(select);

    orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.dbcleaner.cleanDirectory", "false"));
    scan(PROJECT_SAMPLE_PATH, "2012-02-02");

    assertThat(count(select)).isEqualTo(2 * directorySnapshots);
  }

  /**
   * SONAR-2061
   */
  @Test
  public void should_delete_historical_data_of_flagged_metrics() {
    scan(PROJECT_SAMPLE_PATH, "2012-01-01");

    // historical data of complexity_in_classes is supposed to be deleted (see CoreMetrics)
    String selectNcloc = "project_measures where metric_id in (select id from metrics where name='ncloc')";
    String selectComplexityInClasses = "project_measures where metric_id in (select id from metrics where name='complexity_in_classes')";
    int nclocCount = count(selectNcloc);
    int complexitInClassesCount = count(selectComplexityInClasses);

    scan(PROJECT_SAMPLE_PATH, "2012-02-02");
    assertThat(count(selectNcloc)).isGreaterThan(nclocCount);
    assertThat(count(selectComplexityInClasses)).isEqualTo(complexitInClassesCount);
  }

  private void assertDeleted(String key) {
    assertThat(count("snapshots s where s.project_id=(select p.id from projects p where p.kee='" + key + "')")).isZero();
    assertThat(count("resource_index ri where ri.resource_id=(select p.id from projects p where p.kee='" + key + "')")).isZero();
  }

  private void assertSingleSnapshot(String key) {
    assertThat(count("snapshots s where s.project_id=(select p.id from projects p where p.kee='" + key + "')")).isEqualTo(1);
    assertThat(count("resource_index ri where ri.resource_id=(select p.id from projects p where p.kee='" + key + "')")).isGreaterThan(1);
  }

  private BuildResult scan(String path, String date) {
    return scan(path, "sonar.projectDate", date);
  }

  private BuildResult scan(String path, String... extraProperties) {
    SonarRunner runner = configureRunner(path, extraProperties);
    return orchestrator.executeBuild(runner);
  }

  private SonarRunner configureRunner(String projectPath, String... props) {
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT_KEY, "xoo", "one-issue-per-line-profile");
    return SonarRunner.create(ItUtils.projectDir(projectPath)).setProperties(props);
  }

  private int count(String condition) {
    return orchestrator.getDatabase().countSql("select count(*) from " + condition);
  }

  private void assertMeasuresCountForQualifier(String qualifier, int count) {
    int result = countMeasures(qualifier);
    if (result != count) {
      logMeasures("GOT", qualifier);
    }
    collector.checkThat("Wrong number of measures for qualifier " + qualifier, result, equalTo(count));
  }

  private int countMeasures(String qualifier) {
    String sql = "SELECT count(pm.id) FROM project_measures pm, snapshots s, metrics m where pm.snapshot_id=s.id and pm.metric_id=m.id and s.qualifier='" + qualifier + "'";
    return orchestrator.getDatabase().countSql(sql);
  }

  private void logMeasures(String title, String qualifier) {
    String sql = "SELECT m.name as metricName, pm.value as value, pm.text_value as textValue, pm.variation_value_1, pm.variation_value_2, pm.variation_value_3, pm.rule_id, pm.characteristic_id "
      +
      "FROM project_measures pm, snapshots s, metrics m " +
      "WHERE pm.snapshot_id=s.id and pm.metric_id=m.id and s.qualifier='"
      + qualifier + "'";
    List<Map<String, String>> rows = orchestrator.getDatabase().executeSql(sql);

    System.out.println("---- " + title + " - measures on qualifier " + qualifier);
    for (Map<String, String> row : rows) {
      System.out.println("  " + row);
    }
  }
}
