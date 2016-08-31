/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.task.projectanalysis.step;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.text.SimpleDateFormat;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.config.Settings;
import org.sonar.api.config.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ReportComponent;
import org.sonar.server.computation.task.projectanalysis.component.SettingsRepository;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.component.ViewsComponent;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodsHolderImpl;
import org.sonar.server.computation.task.step.ComputationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.config.CorePropertyDefinitions.TIMEMACHINE_MODE_DATE;
import static org.sonar.core.config.CorePropertyDefinitions.TIMEMACHINE_MODE_DAYS;
import static org.sonar.core.config.CorePropertyDefinitions.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS;
import static org.sonar.core.config.CorePropertyDefinitions.TIMEMACHINE_MODE_PREVIOUS_VERSION;
import static org.sonar.core.config.CorePropertyDefinitions.TIMEMACHINE_MODE_VERSION;


@RunWith(DataProviderRunner.class)
public class LoadPeriodsStepTest extends BaseStepTest {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
  private static final String ROOT_KEY = "ROOT_KEY";
  private static final ReportComponent PROJECT_ROOT = ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(ROOT_KEY).setVersion("1.1").build();
  private static final ViewsComponent VIEW_ROOT = ViewsComponent.builder(Component.Type.VIEW, ROOT_KEY).setUuid("ABCD").build();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();
  @Rule
  public LogTester logTester = new LogTester();

  PeriodsHolderImpl periodsHolder = new PeriodsHolderImpl();
  DbClient dbClient = dbTester.getDbClient();
  Settings settings = new MapSettings();
  SettingsRepository settingsRepository = mock(SettingsRepository.class);

  LoadPeriodsStep underTest;

  @Override
  protected ComputationStep step() {
    return underTest;
  }

  @Before
  public void setUp() throws Exception {
    analysisMetadataHolder.setAnalysisDate(DATE_FORMAT.parse("2008-11-30").getTime());

    underTest = new LoadPeriodsStep(dbClient, settingsRepository, treeRootHolder, analysisMetadataHolder, periodsHolder);
  }

  private void setupRoot(Component root) {
    treeRootHolder.setRoot(root);
    when(settingsRepository.getSettings(root)).thenReturn(settings);
  }

  @DataProvider
  public static Object[][] projectAndViewRoots() {
    return new Object[][] {
      {PROJECT_ROOT},
      {VIEW_ROOT}
    };
  }

  @Test
  @UseDataProvider("projectAndViewRoots")
  public void no_period_on_first_analysis(Component root) {
    setupRoot(root);

    // No project, no snapshot

    underTest.execute();
    assertThat(periodsHolder.getPeriods()).isEmpty();
  }

  @Test
  @UseDataProvider("projectAndViewRoots")
  public void feed_one_period(Component root) {
    setupRoot(root);

    dbTester.prepareDbUnit(getClass(), "shared.xml");

    String textDate = "2008-11-22";
    settings.setProperty("sonar.timemachine.period1", textDate);

    underTest.execute();
    List<Period> periods = periodsHolder.getPeriods();
    assertThat(periods).hasSize(1);

    Period period = periods.get(0);
    assertThat(period.getMode()).isEqualTo(TIMEMACHINE_MODE_DATE);
    assertThat(period.getModeParameter()).isEqualTo(textDate);
    assertThat(period.getSnapshotDate()).isEqualTo(1227358680000L);
    assertThat(period.getAnalysisUuid()).isEqualTo("u1003");
  }

  @Test
  @UseDataProvider("projectAndViewRoots")
  public void no_period_when_settings_match_no_analysis(Component root) {
    setupRoot(root);

    dbTester.prepareDbUnit(getClass(), "shared.xml");

    settings.setProperty("sonar.timemachine.period1", "UNKNWOWN VERSION");

    underTest.execute();
    assertThat(periodsHolder.getPeriods()).isEmpty();
  }

  @Test
  @UseDataProvider("projectAndViewRoots")
  public void no_period_when_settings_is_empty(Component root) {
    setupRoot(root);

    dbTester.prepareDbUnit(getClass(), "shared.xml");

    settings.setProperty("sonar.timemachine.period1", "");

    underTest.execute();
    assertThat(periodsHolder.getPeriods()).isEmpty();
  }

  @Test
  @UseDataProvider("projectAndViewRoots")
  public void ignore_unprocessed_snapshots(Component root) {
    setupRoot(root);

    dbTester.prepareDbUnit(getClass(), "unprocessed_snapshots.xml");

    settings.setProperty("sonar.timemachine.period1", "100");

    underTest.execute();
    assertThat(periodsHolder.getPeriods()).isEmpty();
  }

  @Test
  @UseDataProvider("projectAndViewRoots")
  public void feed_period_by_date(Component root) {
    setupRoot(root);

    dbTester.prepareDbUnit(getClass(), "shared.xml");

    String textDate = "2008-11-22";
    settings.setProperty("sonar.timemachine.period1", textDate);

    underTest.execute();
    List<Period> periods = periodsHolder.getPeriods();
    assertThat(periods).hasSize(1);

    Period period = periods.get(0);
    // Return analysis from given date 2008-11-22
    assertThat(period.getMode()).isEqualTo(TIMEMACHINE_MODE_DATE);
    assertThat(period.getModeParameter()).isEqualTo(textDate);
    assertThat(period.getSnapshotDate()).isEqualTo(1227358680000L);
    assertThat(period.getAnalysisUuid()).isEqualTo("u1003");

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs().get(0)).startsWith("Compare to date 2008-11-22 (analysis of ");
  }

  @Test
  @UseDataProvider("projectAndViewRoots")
  public void search_by_date_return_nearest_later_analysis(Component root) {
    setupRoot(root);

    dbTester.prepareDbUnit(getClass(), "shared.xml");
    String date = "2008-11-24";

    settings.setProperty("sonar.timemachine.period1", date);

    underTest.execute();
    List<Period> periods = periodsHolder.getPeriods();
    assertThat(periods).hasSize(1);

    // Analysis form 2008-11-29
    Period period = periods.get(0);
    assertThat(period.getMode()).isEqualTo(TIMEMACHINE_MODE_DATE);
    assertThat(period.getModeParameter()).isEqualTo(date);
    assertThat(period.getSnapshotDate()).isEqualTo(1227934800000L);
    assertThat(period.getAnalysisUuid()).isEqualTo("u1004");
  }

  @Test
  @UseDataProvider("projectAndViewRoots")
  public void no_period_by_date(Component root) {
    setupRoot(root);

    dbTester.prepareDbUnit(getClass(), "shared.xml");

    // No analysis at and after this date
    settings.setProperty("sonar.timemachine.period1", "2008-11-30");

    underTest.execute();
    assertThat(periodsHolder.getPeriods()).isEmpty();
  }

  @Test
  @UseDataProvider("projectAndViewRoots")
  public void feed_period_by_days(Component root) {
    setupRoot(root);

    dbTester.prepareDbUnit(getClass(), "shared.xml");

    settings.setProperty("sonar.timemachine.period1", "10");

    underTest.execute();
    List<Period> periods = periodsHolder.getPeriods();
    assertThat(periods).hasSize(1);

    // return analysis from 2008-11-20
    Period period = periods.get(0);
    assertThat(period.getMode()).isEqualTo(TIMEMACHINE_MODE_DAYS);
    assertThat(period.getModeParameter()).isEqualTo("10");
    assertThat(period.getSnapshotDate()).isEqualTo(1227157200000L);
    assertThat(period.getAnalysisUuid()).isEqualTo("u1002");

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs().get(0)).startsWith("Compare over 10 days (2008-11-20, analysis of ");
  }

  @Test
  @UseDataProvider("projectAndViewRoots")
  public void no_period_by_days(Component root) {
    setupRoot(root);

    dbTester.prepareDbUnit(getClass(), "empty.xml");

    settings.setProperty("sonar.timemachine.period1", "0");

    underTest.execute();
    assertThat(periodsHolder.getPeriods()).isEmpty();
  }

  @Test
  @UseDataProvider("projectAndViewRoots")
  public void feed_period_by_previous_analysis(Component root) {
    setupRoot(root);

    dbTester.prepareDbUnit(getClass(), "shared.xml");

    settings.setProperty("sonar.timemachine.period1", "previous_analysis");

    underTest.execute();
    List<Period> periods = periodsHolder.getPeriods();
    assertThat(periods).hasSize(1);

    // return analysis from 2008-11-29
    Period period = periods.get(0);
    assertThat(period.getMode()).isEqualTo(TIMEMACHINE_MODE_PREVIOUS_ANALYSIS);
    assertThat(period.getModeParameter()).isNotNull();
    assertThat(period.getSnapshotDate()).isEqualTo(1227934800000L);
    assertThat(period.getAnalysisUuid()).isEqualTo("u1004");

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs().get(0)).startsWith("Compare to previous analysis (");
  }

  @Test
  @UseDataProvider("projectAndViewRoots")
  public void no_period_by_previous_analysis(Component root) {
    setupRoot(root);

    dbTester.prepareDbUnit(getClass(), "empty.xml");

    settings.setProperty("sonar.timemachine.period1", "previous_analysis");

    underTest.execute();
    assertThat(periodsHolder.getPeriods()).isEmpty();
  }

  @Test
  public void feed_period_by_previous_version() {
    setupRoot(PROJECT_ROOT);

    dbTester.prepareDbUnit(getClass(), "shared.xml");

    settings.setProperty("sonar.timemachine.period1", "previous_version");

    underTest.execute();
    List<Period> periods = periodsHolder.getPeriods();
    assertThat(periods).hasSize(1);

    // Analysis form 2008-11-12
    Period period = periods.get(0);
    assertThat(period.getMode()).isEqualTo(TIMEMACHINE_MODE_PREVIOUS_VERSION);
    assertThat(period.getModeParameter()).isEqualTo("1.0");
    assertThat(period.getSnapshotDate()).isEqualTo(1226494680000L);
    assertThat(period.getAnalysisUuid()).isEqualTo("u1001");

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs().get(0)).startsWith("Compare to previous version (");
  }

  @Test
  public void feed_period_by_previous_version_is_not_supported_for_views() {
    setupRoot(VIEW_ROOT);

    dbTester.prepareDbUnit(getClass(), "shared.xml");

    settings.setProperty("sonar.timemachine.period1", "previous_version");

    underTest.execute();
    List<Period> periods = periodsHolder.getPeriods();
    assertThat(periods).hasSize(0);
  }

  @Test
  public void feed_period_by_previous_version_with_previous_version_deleted() {
    setupRoot(PROJECT_ROOT);

    dbTester.prepareDbUnit(getClass(), "previous_version_deleted.xml");

    settings.setProperty("sonar.timemachine.period1", "previous_version");

    underTest.execute();
    List<Period> periods = periodsHolder.getPeriods();
    assertThat(periods).hasSize(1);

    // Analysis form 2008-11-11
    Period period = periods.get(0);
    assertThat(period.getMode()).isEqualTo(TIMEMACHINE_MODE_PREVIOUS_VERSION);
    assertThat(period.getModeParameter()).isEqualTo("0.9");
    assertThat(period.getSnapshotDate()).isEqualTo(1226379600000L);
    assertThat(period.getAnalysisUuid()).isEqualTo("u1000");
  }

  @Test
  @UseDataProvider("projectAndViewRoots")
  public void no_period_by_previous_version(Component root) {
    setupRoot(root);

    dbTester.prepareDbUnit(getClass(), "empty.xml");

    settings.setProperty("sonar.timemachine.period1", "previous_version");

    underTest.execute();
    assertThat(periodsHolder.getPeriods()).isEmpty();
  }

  @Test
  public void feed_period_by_previous_version_with_first_analysis_when_no_previous_version_found() {
    setupRoot(PROJECT_ROOT);

    dbTester.prepareDbUnit(getClass(), "no_previous_version.xml");

    settings.setProperty("sonar.timemachine.period1", "previous_version");

    underTest.execute();
    List<Period> periods = periodsHolder.getPeriods();
    assertThat(periods).hasSize(1);

    Period period = periods.get(0);
    assertThat(period.getMode()).isEqualTo(TIMEMACHINE_MODE_PREVIOUS_VERSION);
    assertThat(period.getModeParameter()).isNull();
    assertThat(period.getSnapshotDate()).isEqualTo(1226379600000L);
    assertThat(period.getAnalysisUuid()).isEqualTo("u1000");
  }

  @Test
  public void feed_period_by_previous_version_with_first_analysis_when_previous_snapshot_is_the_last_one() {
    setupRoot(PROJECT_ROOT);

    dbTester.prepareDbUnit(getClass(), "previous_version_is_last_one.xml");

    settings.setProperty("sonar.timemachine.period1", "previous_version");

    underTest.execute();
    List<Period> periods = periodsHolder.getPeriods();
    assertThat(periods).hasSize(1);

    Period period = periods.get(0);
    assertThat(period.getMode()).isEqualTo(TIMEMACHINE_MODE_PREVIOUS_VERSION);
    assertThat(period.getModeParameter()).isNull();
    assertThat(period.getSnapshotDate()).isEqualTo(1226379600000L);
    assertThat(period.getAnalysisUuid()).isEqualTo("u1000");
  }

  @Test
  public void no_period_by_previous_version_when_no_event_version_for_views() {
    setupRoot(VIEW_ROOT);

    dbTester.prepareDbUnit(getClass(), "no_previous_version.xml");

    settings.setProperty("sonar.timemachine.period1", "previous_version");

    underTest.execute();
    assertThat(periodsHolder.getPeriods()).isEmpty();
  }

  @Test
  @UseDataProvider("projectAndViewRoots")
  public void feed_period_by_version(Component root) {
    setupRoot(root);

    dbTester.prepareDbUnit(getClass(), "shared.xml");

    settings.setProperty("sonar.timemachine.period1", "0.9");

    underTest.execute();
    List<Period> periods = periodsHolder.getPeriods();
    assertThat(periods).hasSize(1);

    // Analysis form 2008-11-11
    Period period = periods.get(0);
    assertThat(period.getMode()).isEqualTo(TIMEMACHINE_MODE_VERSION);
    assertThat(period.getModeParameter()).isEqualTo("0.9");
    assertThat(period.getSnapshotDate()).isEqualTo(1226379600000L);
    assertThat(period.getAnalysisUuid()).isEqualTo("u1000");

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs().get(0)).startsWith("Compare to version (0.9) (");
  }

  @Test
  @UseDataProvider("projectAndViewRoots")
  public void no_period_by_version(Component root) {
    setupRoot(root);

    dbTester.prepareDbUnit(getClass(), "empty.xml");

    settings.setProperty("sonar.timemachine.period1", "0.8");

    underTest.execute();
    assertThat(periodsHolder.getPeriods()).isEmpty();
  }

  @Test
  public void all_five_types_of_periods_are_supported_for_PROJECT_component_tree() {
    setupRoot(PROJECT_ROOT);

    dbTester.prepareDbUnit(getClass(), "shared.xml");

    settings.setProperty("sonar.timemachine.period1", "2008-11-22"); // Analysis from 2008-11-22 should be returned
    settings.setProperty("sonar.timemachine.period2", "10"); // Analysis from 2008-11-20 should be returned
    settings.setProperty("sonar.timemachine.period3", "previous_analysis"); // Analysis from 2008-11-29 should be returned
    settings.setProperty("sonar.timemachine.period4", "previous_version"); // Analysis from 2008-11-12 should be returned
    settings.setProperty("sonar.timemachine.period5", "0.9"); // Analysis from 2008-11-11

    underTest.execute();
    List<Period> periods = periodsHolder.getPeriods();

    assertThat(periods).extracting("mode").containsExactly(TIMEMACHINE_MODE_DATE, TIMEMACHINE_MODE_DAYS,
      TIMEMACHINE_MODE_PREVIOUS_ANALYSIS, TIMEMACHINE_MODE_PREVIOUS_VERSION,
      TIMEMACHINE_MODE_VERSION);

    assertThat(periods.get(0).getMode()).isEqualTo(TIMEMACHINE_MODE_DATE);
    assertThat(periods.get(0).getIndex()).isEqualTo(1);
    assertThat(periods.get(0).getSnapshotDate()).isEqualTo(1227358680000L);

    assertThat(periods.get(1).getMode()).isEqualTo(TIMEMACHINE_MODE_DAYS);
    assertThat(periods.get(1).getIndex()).isEqualTo(2);
    assertThat(periods.get(1).getSnapshotDate()).isEqualTo(1227157200000L);

    assertThat(periods.get(2).getMode()).isEqualTo(TIMEMACHINE_MODE_PREVIOUS_ANALYSIS);
    assertThat(periods.get(2).getIndex()).isEqualTo(3);
    assertThat(periods.get(2).getSnapshotDate()).isEqualTo(1227934800000L);

    assertThat(periods.get(3).getMode()).isEqualTo(TIMEMACHINE_MODE_PREVIOUS_VERSION);
    assertThat(periods.get(3).getIndex()).isEqualTo(4);
    assertThat(periods.get(3).getSnapshotDate()).isEqualTo(1226494680000L);

    assertThat(periods.get(4).getMode()).isEqualTo(TIMEMACHINE_MODE_VERSION);
    assertThat(periods.get(4).getIndex()).isEqualTo(5);
    assertThat(periods.get(4).getSnapshotDate()).isEqualTo(1226379600000L);
  }

  @Test
  public void feed_four_different_periods() {
    setupRoot(VIEW_ROOT);

    dbTester.prepareDbUnit(getClass(), "shared.xml");

    settings.setProperty("sonar.timemachine.period1", "2008-11-22"); // Analysis from 2008-11-22 should be returned
    settings.setProperty("sonar.timemachine.period2", "10"); // Analysis from 2008-11-20 should be returned
    settings.setProperty("sonar.timemachine.period3", "previous_analysis"); // Analysis from 2008-11-29 should be returned
    settings.setProperty("sonar.timemachine.period4", "previous_version"); // Analysis from 2008-11-12 should be returned
    settings.setProperty("sonar.timemachine.period5", "0.9"); // Analysis from 2008-11-11

    underTest.execute();
    List<Period> periods = periodsHolder.getPeriods();

    assertThat(periods).extracting("mode").containsExactly(
      TIMEMACHINE_MODE_DATE, TIMEMACHINE_MODE_DAYS,
      TIMEMACHINE_MODE_PREVIOUS_ANALYSIS, TIMEMACHINE_MODE_VERSION);

    assertThat(periods.get(0).getMode()).isEqualTo(TIMEMACHINE_MODE_DATE);
    assertThat(periods.get(0).getIndex()).isEqualTo(1);
    assertThat(periods.get(0).getSnapshotDate()).isEqualTo(1227358680000L);

    assertThat(periods.get(1).getMode()).isEqualTo(TIMEMACHINE_MODE_DAYS);
    assertThat(periods.get(1).getIndex()).isEqualTo(2);
    assertThat(periods.get(1).getSnapshotDate()).isEqualTo(1227157200000L);

    assertThat(periods.get(2).getMode()).isEqualTo(TIMEMACHINE_MODE_PREVIOUS_ANALYSIS);
    assertThat(periods.get(2).getIndex()).isEqualTo(3);
    assertThat(periods.get(2).getSnapshotDate()).isEqualTo(1227934800000L);

    assertThat(periods.get(3).getMode()).isEqualTo(TIMEMACHINE_MODE_VERSION);
    assertThat(periods.get(3).getIndex()).isEqualTo(5);
    assertThat(periods.get(3).getSnapshotDate()).isEqualTo(1226379600000L);
  }

  @Test
  public void can_use_project_qualifier_in_settings() {
    setupRoot(PROJECT_ROOT);

    dbTester.prepareDbUnit(getClass(), "shared.xml");

    settings.setProperty("sonar.timemachine.period4.TRK", "2008-11-22");
    settings.setProperty("sonar.timemachine.period5.TRK", "previous_analysis");

    underTest.execute();
    assertThat(periodsHolder.getPeriods()).hasSize(2);
  }

  @Test
  public void can_use_views_qualifier_in_settings() {
    setupRoot(VIEW_ROOT);

    dbTester.prepareDbUnit(getClass(), "shared.xml");

    settings.setProperty("sonar.timemachine.period4.VW", "2008-11-22");
    settings.setProperty("sonar.timemachine.period5.VW", "previous_analysis");

    underTest.execute();
    assertThat(periodsHolder.getPeriods()).hasSize(2);
  }

}
