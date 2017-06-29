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
package org.sonar.server.computation.task.projectanalysis.step;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.text.SimpleDateFormat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.server.computation.task.projectanalysis.component.ReportComponent;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.component.ViewsComponent;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodHolderImpl;
import org.sonar.server.computation.task.step.ComputationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_DATE;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_DAYS;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_PREVIOUS_ANALYSIS;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_PREVIOUS_VERSION;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_VERSION;

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

  private PeriodHolderImpl periodsHolder = new PeriodHolderImpl();
  private DbClient dbClient = dbTester.getDbClient();
  private MapSettings settings = new MapSettings();
  private ConfigurationRepository settingsRepository = mock(ConfigurationRepository.class);

  private LoadPeriodsStep underTest = new LoadPeriodsStep(dbClient, settingsRepository, treeRootHolder, analysisMetadataHolder, periodsHolder);

  @Override
  protected ComputationStep step() {
    return underTest;
  }

  @Before
  public void setUp() throws Exception {
    analysisMetadataHolder.setAnalysisDate(DATE_FORMAT.parse("2008-11-30").getTime());
  }

  private void setupRoot(Component root) {
    treeRootHolder.setRoot(root);
    when(settingsRepository.getConfiguration(root)).thenReturn(settings.asConfig());
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

    assertThat(periodsHolder.getPeriod()).isNull();
  }

  @Test
  @UseDataProvider("projectAndViewRoots")
  public void feed_one_period(Component root) {
    setupRoot(root);
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    String textDate = "2008-11-22";
    settings.setProperty("sonar.leak.period", textDate);

    underTest.execute();

    Period period = periodsHolder.getPeriod();
    assertThat(period).isNotNull();
    assertThat(period.getMode()).isEqualTo(LEAK_PERIOD_MODE_DATE);
    assertThat(period.getModeParameter()).isEqualTo(textDate);
    assertThat(period.getSnapshotDate()).isEqualTo(1227358680000L);
    assertThat(period.getAnalysisUuid()).isEqualTo("u1003");
  }

  @Test
  @UseDataProvider("projectAndViewRoots")
  public void no_period_when_settings_match_no_analysis(Component root) {
    setupRoot(root);
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    settings.setProperty("sonar.leak.period", "UNKNWOWN VERSION");

    underTest.execute();

    assertThat(periodsHolder.getPeriod()).isNull();
  }

  @Test
  @UseDataProvider("projectAndViewRoots")
  public void no_period_when_settings_is_empty(Component root) {
    setupRoot(root);
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    settings.setProperty("sonar.leak.period", "");

    underTest.execute();

    assertThat(periodsHolder.getPeriod()).isNull();
  }

  @Test
  @UseDataProvider("projectAndViewRoots")
  public void ignore_unprocessed_snapshots(Component root) {
    setupRoot(root);
    dbTester.prepareDbUnit(getClass(), "unprocessed_snapshots.xml");
    settings.setProperty("sonar.leak.period", "100");

    underTest.execute();

    assertThat(periodsHolder.getPeriod()).isNull();
  }

  @Test
  @UseDataProvider("projectAndViewRoots")
  public void feed_period_by_date(Component root) {
    setupRoot(root);
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    String textDate = "2008-11-22";
    settings.setProperty("sonar.leak.period", textDate);

    underTest.execute();

    // Return analysis from given date 2008-11-22
    Period period = periodsHolder.getPeriod();
    assertThat(period).isNotNull();
    assertThat(period.getMode()).isEqualTo(LEAK_PERIOD_MODE_DATE);
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
    settings.setProperty("sonar.leak.period", date);

    underTest.execute();

    // Analysis form 2008-11-29
    Period period = periodsHolder.getPeriod();
    assertThat(period).isNotNull();
    assertThat(period.getMode()).isEqualTo(LEAK_PERIOD_MODE_DATE);
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
    settings.setProperty("sonar.leak.period", "2008-11-30");

    underTest.execute();

    assertThat(periodsHolder.getPeriod()).isNull();
  }

  @Test
  @UseDataProvider("projectAndViewRoots")
  public void feed_period_by_days(Component root) {
    setupRoot(root);
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    settings.setProperty("sonar.leak.period", "10");

    underTest.execute();

    // return analysis from 2008-11-20
    Period period = periodsHolder.getPeriod();
    assertThat(period).isNotNull();
    assertThat(period.getMode()).isEqualTo(LEAK_PERIOD_MODE_DAYS);
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
    settings.setProperty("sonar.leak.period", "0");

    underTest.execute();

    assertThat(periodsHolder.getPeriod()).isNull();
  }

  @Test
  @UseDataProvider("projectAndViewRoots")
  public void feed_period_by_previous_analysis(Component root) {
    setupRoot(root);
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    settings.setProperty("sonar.leak.period", "previous_analysis");

    underTest.execute();

    // return analysis from 2008-11-29
    Period period = periodsHolder.getPeriod();
    assertThat(period).isNotNull();
    assertThat(period.getMode()).isEqualTo(LEAK_PERIOD_MODE_PREVIOUS_ANALYSIS);
    assertThat(period.getModeParameter()).isNotNull();
    assertThat(period.getSnapshotDate()).isEqualTo(1227934800000L);
    assertThat(period.getAnalysisUuid()).isEqualTo("u1004");

    assertThat(logTester.logs(LoggerLevel.DEBUG)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.DEBUG).get(0)).startsWith("Compare to previous analysis (");
  }

  @Test
  @UseDataProvider("projectAndViewRoots")
  public void no_period_by_previous_analysis(Component root) {
    setupRoot(root);
    dbTester.prepareDbUnit(getClass(), "empty.xml");
    settings.setProperty("sonar.leak.period", "previous_analysis");

    underTest.execute();

    assertThat(periodsHolder.getPeriod()).isNull();
  }

  @Test
  public void display_warning_log_when_using_previous_analysis() {
    setupRoot(PROJECT_ROOT);
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    settings.setProperty("sonar.leak.period", "previous_analysis");

    underTest.execute();

    assertThat(logTester.logs(LoggerLevel.WARN))
      .containsOnly("Leak period is set to deprecated value 'previous_analysis'. This value will be removed in next SonarQube LTS, please use another one instead.");
  }

  @Test
  public void feed_period_by_previous_version() {
    setupRoot(PROJECT_ROOT);
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    settings.setProperty("sonar.leak.period", "previous_version");

    underTest.execute();

    // Analysis form 2008-11-12
    Period period = periodsHolder.getPeriod();
    assertThat(period).isNotNull();
    assertThat(period.getMode()).isEqualTo(LEAK_PERIOD_MODE_PREVIOUS_VERSION);
    assertThat(period.getModeParameter()).isEqualTo("1.0");
    assertThat(period.getSnapshotDate()).isEqualTo(1226494680000L);
    assertThat(period.getAnalysisUuid()).isEqualTo("u1001");

    assertThat(logTester.logs(LoggerLevel.DEBUG)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.DEBUG).get(0)).startsWith("Compare to previous version (");
  }

  @Test
  public void feed_period_by_previous_version_is_not_supported_for_views() {
    setupRoot(VIEW_ROOT);
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    settings.setProperty("sonar.leak.period", "previous_version");

    underTest.execute();

    assertThat(periodsHolder.getPeriod()).isNull();
  }

  @Test
  public void feed_period_by_previous_version_with_previous_version_deleted() {
    setupRoot(PROJECT_ROOT);
    dbTester.prepareDbUnit(getClass(), "previous_version_deleted.xml");
    settings.setProperty("sonar.leak.period", "previous_version");

    underTest.execute();

    // Analysis form 2008-11-11
    Period period = periodsHolder.getPeriod();
    assertThat(period).isNotNull();
    assertThat(period.getMode()).isEqualTo(LEAK_PERIOD_MODE_PREVIOUS_VERSION);
    assertThat(period.getModeParameter()).isEqualTo("0.9");
    assertThat(period.getSnapshotDate()).isEqualTo(1226379600000L);
    assertThat(period.getAnalysisUuid()).isEqualTo("u1000");
  }

  @Test
  @UseDataProvider("projectAndViewRoots")
  public void no_period_by_previous_version(Component root) {
    setupRoot(root);
    dbTester.prepareDbUnit(getClass(), "empty.xml");
    settings.setProperty("sonar.leak.period", "previous_version");

    underTest.execute();

    assertThat(periodsHolder.getPeriod()).isNull();
  }

  @Test
  public void feed_period_by_previous_version_with_first_analysis_when_no_previous_version_found() {
    setupRoot(PROJECT_ROOT);
    dbTester.prepareDbUnit(getClass(), "no_previous_version.xml");
    settings.setProperty("sonar.leak.period", "previous_version");

    underTest.execute();

    Period period = periodsHolder.getPeriod();
    assertThat(period).isNotNull();
    assertThat(period.getMode()).isEqualTo(LEAK_PERIOD_MODE_PREVIOUS_VERSION);
    assertThat(period.getModeParameter()).isNull();
    assertThat(period.getSnapshotDate()).isEqualTo(1226379600000L);
    assertThat(period.getAnalysisUuid()).isEqualTo("u1000");
  }

  @Test
  public void feed_period_by_previous_version_with_first_analysis_when_previous_snapshot_is_the_last_one() {
    setupRoot(PROJECT_ROOT);
    dbTester.prepareDbUnit(getClass(), "previous_version_is_last_one.xml");
    settings.setProperty("sonar.leak.period", "previous_version");

    underTest.execute();

    Period period = periodsHolder.getPeriod();
    assertThat(period).isNotNull();
    assertThat(period.getMode()).isEqualTo(LEAK_PERIOD_MODE_PREVIOUS_VERSION);
    assertThat(period.getModeParameter()).isNull();
    assertThat(period.getSnapshotDate()).isEqualTo(1226379600000L);
    assertThat(period.getAnalysisUuid()).isEqualTo("u1000");
  }

  @Test
  public void no_period_by_previous_version_when_no_event_version_for_views() {
    setupRoot(VIEW_ROOT);
    dbTester.prepareDbUnit(getClass(), "no_previous_version.xml");
    settings.setProperty("sonar.leak.period", "previous_version");

    underTest.execute();

    assertThat(periodsHolder.getPeriod()).isNull();
  }

  @Test
  @UseDataProvider("projectAndViewRoots")
  public void feed_period_by_version(Component root) {
    setupRoot(root);
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    settings.setProperty("sonar.leak.period", "0.9");

    underTest.execute();

    // Analysis form 2008-11-11
    Period period = periodsHolder.getPeriod();
    assertThat(period).isNotNull();
    assertThat(period.getMode()).isEqualTo(LEAK_PERIOD_MODE_VERSION);
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
    settings.setProperty("sonar.leak.period", "0.8");

    underTest.execute();

    assertThat(periodsHolder.getPeriod()).isNull();
  }

}
