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

package org.sonar.server.computation.step;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import java.text.SimpleDateFormat;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.LogTester;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.db.component.SnapshotDao;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolderImpl;
import org.sonar.server.db.DbClient;
import org.sonar.test.DbTests;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class FeedPeriodsStepTest extends BaseStepTest {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
  private static final String PROJECT_KEY = "PROJECT_KEY";

  @ClassRule
  public static final DbTester dbTester = new DbTester();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Rule
  public LogTester logTester = new LogTester();

  PeriodsHolderImpl periodsHolder = new PeriodsHolderImpl();

  DbClient dbClient;

  DbSession dbSession;

  Settings settings = new Settings();

  FeedPeriodsStep sut;

  @Override
  protected ComputationStep step() {
    return sut;
  }

  @Before
  public void setUp() throws Exception {
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new ComponentDao(), new SnapshotDao());
    dbSession = dbClient.openSession(false);

    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setAnalysisDate(DATE_FORMAT.parse("2008-11-30").getTime())
      .build());

    treeRootHolder.setRoot(DumbComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).setVersion("1.1").build());

    sut = new FeedPeriodsStep(dbClient, settings, treeRootHolder, reportReader, periodsHolder);
  }

  @After
  public void tearDown() throws Exception {
    dbSession.close();
  }

  @Test
  public void no_period_on_first_analysis() throws Exception {
    // No project, no snapshot

    sut.execute();
    assertThat(periodsHolder.getPeriods()).isEmpty();
  }

  @Test
  public void feed_one_period() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    String textDate = "2008-11-22";
    settings.setProperty("sonar.timemachine.period1", textDate);

    sut.execute();
    List<Period> periods = periodsHolder.getPeriods();
    assertThat(periods).hasSize(1);

    Period period = periods.get(0);
    assertThat(period.getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_DATE);
    assertThat(period.getModeParameter()).isEqualTo(textDate);
    assertThat(period.getSnapshotDate()).isEqualTo(1227358680000L);
    assertThat(period.getSnapshotId()).isEqualTo(1003L);
  }

  @Test
  public void no_period_when_settings_match_no_analysis() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    settings.setProperty("sonar.timemachine.period1", "UNKNWOWN VERSION");

    sut.execute();
    assertThat(periodsHolder.getPeriods()).isEmpty();
  }

  @Test
  public void no_period_when_settings_is_empty() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    settings.setProperty("sonar.timemachine.period1", "");

    sut.execute();
    assertThat(periodsHolder.getPeriods()).isEmpty();
  }

  @Test
  public void ignore_unprocessed_snapshots() throws Exception {
    dbTester.prepareDbUnit(getClass(), "unprocessed_snapshots.xml");

    settings.setProperty("sonar.timemachine.period1", "100");

    sut.execute();
    assertThat(periodsHolder.getPeriods()).isEmpty();
  }

  @Test
  public void feed_period_by_date() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    String textDate = "2008-11-22";
    settings.setProperty("sonar.timemachine.period1", textDate);

    sut.execute();
    List<Period> periods = periodsHolder.getPeriods();
    assertThat(periods).hasSize(1);

    Period period = periods.get(0);
    // Return analysis from given date 2008-11-22
    assertThat(period.getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_DATE);
    assertThat(period.getModeParameter()).isEqualTo(textDate);
    assertThat(period.getSnapshotDate()).isEqualTo(1227358680000L);
    assertThat(period.getSnapshotId()).isEqualTo(1003L);

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs().get(0)).startsWith("Compare to date 2008-11-22 (analysis of ");
  }

  @Test
  public void search_by_date_return_nearest_later_analysis() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    String date = "2008-11-24";

    settings.setProperty("sonar.timemachine.period1", date);

    sut.execute();
    List<Period> periods = periodsHolder.getPeriods();
    assertThat(periods).hasSize(1);

    // Analysis form 2008-11-29
    Period period = periods.get(0);
    assertThat(period.getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_DATE);
    assertThat(period.getModeParameter()).isEqualTo(date);
    assertThat(period.getSnapshotDate()).isEqualTo(1227934800000L);
    assertThat(period.getSnapshotId()).isEqualTo(1004L);
  }

  @Test
  public void no_period_by_date() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    // No analysis at and after this date
    settings.setProperty("sonar.timemachine.period1", "2008-11-30");

    sut.execute();
    assertThat(periodsHolder.getPeriods()).isEmpty();
  }

  @Test
  public void feed_period_by_days() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    settings.setProperty("sonar.timemachine.period1", "10");

    sut.execute();
    List<Period> periods = periodsHolder.getPeriods();
    assertThat(periods).hasSize(1);

    // return analysis from 2008-11-20
    Period period = periods.get(0);
    assertThat(period.getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_DAYS);
    assertThat(period.getModeParameter()).isEqualTo("10");
    assertThat(period.getSnapshotDate()).isEqualTo(1227157200000L);
    assertThat(period.getSnapshotId()).isEqualTo(1002L);

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs().get(0)).startsWith("Compare over 10 days (2008-11-20, analysis of ");
  }

  @Test
  public void no_period_by_days() throws Exception {
    dbTester.prepareDbUnit(getClass(), "empty.xml");

    settings.setProperty("sonar.timemachine.period1", "0");

    sut.execute();
    assertThat(periodsHolder.getPeriods()).isEmpty();
  }

  @Test
  public void feed_period_by_previous_analysis() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    settings.setProperty("sonar.timemachine.period1", "previous_analysis");

    sut.execute();
    List<Period> periods = periodsHolder.getPeriods();
    assertThat(periods).hasSize(1);

    // return analysis from 2008-11-29
    Period period = periods.get(0);
    assertThat(period.getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS);
    assertThat(period.getModeParameter()).isNotNull();
    assertThat(period.getSnapshotDate()).isEqualTo(1227934800000L);
    assertThat(period.getSnapshotId()).isEqualTo(1004L);

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs().get(0)).startsWith("Compare to previous analysis (");
  }

  @Test
  public void no_period_by_previous_analysis() throws Exception {
    dbTester.prepareDbUnit(getClass(), "empty.xml");

    settings.setProperty("sonar.timemachine.period1", "previous_analysis");

    sut.execute();
    assertThat(periodsHolder.getPeriods()).isEmpty();
  }

  @Test
  public void feed_period_by_previous_version() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    settings.setProperty("sonar.timemachine.period1", "previous_version");

    sut.execute();
    List<Period> periods = periodsHolder.getPeriods();
    assertThat(periods).hasSize(1);

    // Analysis form  2008-11-12
    Period period = periods.get(0);
    assertThat(period.getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION);
    assertThat(period.getModeParameter()).isEqualTo("1.0");
    assertThat(period.getSnapshotDate()).isEqualTo(1226494680000L);
    assertThat(period.getSnapshotId()).isEqualTo(1001L);

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs().get(0)).startsWith("Compare to previous version (");
  }

  @Test
  public void feed_period_by_previous_version_wit_previous_version_deleted() throws Exception {
    dbTester.prepareDbUnit(getClass(), "previous_version_deleted.xml");

    settings.setProperty("sonar.timemachine.period1", "previous_version");

    sut.execute();
    List<Period> periods = periodsHolder.getPeriods();
    assertThat(periods).hasSize(1);

    // Analysis form 2008-11-11
    Period period = periods.get(0);
    assertThat(period.getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION);
    assertThat(period.getModeParameter()).isEqualTo("0.9");
    assertThat(period.getSnapshotDate()).isEqualTo(1226379600000L);
    assertThat(period.getSnapshotId()).isEqualTo(1000L);
  }

  @Test
  public void no_period_by_previous_version() throws Exception {
    dbTester.prepareDbUnit(getClass(), "empty.xml");

    settings.setProperty("sonar.timemachine.period1", "previous_version");

    sut.execute();
    assertThat(periodsHolder.getPeriods()).isEmpty();
  }

  @Test
  public void no_period_by_previous_version_when_no_event_version() throws Exception {
    dbTester.prepareDbUnit(getClass(), "no_previous_version.xml");

    settings.setProperty("sonar.timemachine.period1", "previous_version");

    sut.execute();
    assertThat(periodsHolder.getPeriods()).isEmpty();
  }

  @Test
  public void feed_period_by_version() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    settings.setProperty("sonar.timemachine.period1", "0.9");

    sut.execute();
    List<Period> periods = periodsHolder.getPeriods();
    assertThat(periods).hasSize(1);

    // Analysis form 2008-11-11
    Period period = periods.get(0);
    assertThat(period.getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_VERSION);
    assertThat(period.getModeParameter()).isEqualTo("0.9");
    assertThat(period.getSnapshotDate()).isEqualTo(1226379600000L);
    assertThat(period.getSnapshotId()).isEqualTo(1000L);

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs().get(0)).startsWith("Compare to version (0.9) (");
  }

  @Test
  public void no_period_by_version() throws Exception {
    dbTester.prepareDbUnit(getClass(), "empty.xml");

    settings.setProperty("sonar.timemachine.period1", "0.8");

    sut.execute();
    assertThat(periodsHolder.getPeriods()).isEmpty();
  }

  @Test
  public void feed_five_different_periods() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    settings.setProperty("sonar.timemachine.period1", "2008-11-22"); // Analysis from 2008-11-22 should be returned
    settings.setProperty("sonar.timemachine.period2", "10"); // Analysis from 2008-11-20 should be returned
    settings.setProperty("sonar.timemachine.period3", "previous_analysis"); // Analysis from 2008-11-29 should be returned
    settings.setProperty("sonar.timemachine.period4", "previous_version"); // Analysis from 2008-11-12 should be returned
    settings.setProperty("sonar.timemachine.period5", "0.9"); // Anaylsis from 2008-11-11

    sut.execute();
    List<Period> periods = periodsHolder.getPeriods();

    List<String> periodModes = newArrayList(Iterables.transform(periods, new Function<Period, String>() {
      @Override
      public String apply(Period input) {
        return input.getMode();
      }
    }));
    assertThat(periodModes).containsOnly(CoreProperties.TIMEMACHINE_MODE_DATE, CoreProperties.TIMEMACHINE_MODE_DAYS, CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS,
      CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION, CoreProperties.TIMEMACHINE_MODE_VERSION);

    assertThat(periods.get(0).getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_DATE);
    assertThat(periods.get(0).getIndex()).isEqualTo(1);
    assertThat(periods.get(0).getSnapshotDate()).isEqualTo(1227358680000L);

    assertThat(periods.get(1).getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_DAYS);
    assertThat(periods.get(1).getIndex()).isEqualTo(2);
    assertThat(periods.get(1).getSnapshotDate()).isEqualTo(1227157200000L);

    assertThat(periods.get(2).getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS);
    assertThat(periods.get(2).getIndex()).isEqualTo(3);
    assertThat(periods.get(2).getSnapshotDate()).isEqualTo(1227934800000L);

    assertThat(periods.get(3).getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION);
    assertThat(periods.get(3).getIndex()).isEqualTo(4);
    assertThat(periods.get(3).getSnapshotDate()).isEqualTo(1226494680000L);

    assertThat(periods.get(4).getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_VERSION);
    assertThat(periods.get(4).getIndex()).isEqualTo(5);
    assertThat(periods.get(4).getSnapshotDate()).isEqualTo(1226379600000L);
  }

  @Test
  public void can_use_qualifier_in_settings() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    settings.setProperty("sonar.timemachine.period4.TRK", "2008-11-22");
    settings.setProperty("sonar.timemachine.period5.TRK", "previous_analysis");

    sut.execute();
    assertThat(periodsHolder.getPeriods()).hasSize(2);
  }

}
