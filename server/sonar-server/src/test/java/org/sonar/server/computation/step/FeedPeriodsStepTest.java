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
import java.util.Date;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.component.db.SnapshotDao;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodFinder;
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

    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .setVersion("1.1")
      .build());

    Component project = new DumbComponent(Component.Type.PROJECT, 1, "ABCD", PROJECT_KEY);
    treeRootHolder.setRoot(project);

    sut = new FeedPeriodsStep(dbClient, settings, treeRootHolder, new PeriodFinder(dbClient), reportReader, periodsHolder);
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
  public void get_one_period() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    String textDate = "2008-11-22";
    Date date = DATE_FORMAT.parse(textDate);
    settings.setProperty("sonar.timemachine.period1", textDate);

    sut.execute();
    List<Period> periods = periodsHolder.getPeriods();
    assertThat(periods).hasSize(1);

    Period period =  periods.get(0);
    assertThat(period.getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_DATE);
    assertThat(period.getModeParameter()).isEqualTo(textDate);
    assertThat(period.getSnapshotDate()).isEqualTo(1227358680000L);
    assertThat(period.getTargetDate()).isEqualTo(date.getTime());
    assertThat(period.getProjectSnapshot().getId()).isEqualTo(1003L);
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
  public void get_five_different_periods() throws Exception {
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
    assertThat(periods.get(0).getProjectSnapshot().getId()).isEqualTo(1003L);

    assertThat(periods.get(1).getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_DAYS);
    assertThat(periods.get(1).getIndex()).isEqualTo(2);
    assertThat(periods.get(1).getProjectSnapshot().getId()).isEqualTo(1002L);

    assertThat(periods.get(2).getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS);
    assertThat(periods.get(2).getIndex()).isEqualTo(3);
    assertThat(periods.get(2).getProjectSnapshot().getId()).isEqualTo(1004L);

    assertThat(periods.get(3).getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION);
    assertThat(periods.get(3).getIndex()).isEqualTo(4);
    assertThat(periods.get(3).getProjectSnapshot().getId()).isEqualTo(1001L);

    assertThat(periods.get(4).getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_VERSION);
    assertThat(periods.get(4).getIndex()).isEqualTo(5);
    assertThat(periods.get(4).getProjectSnapshot().getId()).isEqualTo(1000L);
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
