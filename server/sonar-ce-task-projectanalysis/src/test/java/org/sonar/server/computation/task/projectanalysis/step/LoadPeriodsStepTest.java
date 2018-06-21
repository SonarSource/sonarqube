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
package org.sonar.server.computation.task.projectanalysis.step;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.text.SimpleDateFormat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.server.computation.task.projectanalysis.component.ReportComponent;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodHolderImpl;
import org.sonar.server.computation.task.step.ComputationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_DATE;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_DAYS;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_PREVIOUS_VERSION;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_VERSION;
import static org.sonar.db.component.SnapshotDto.STATUS_UNPROCESSED;
import static org.sonar.db.event.EventDto.CATEGORY_VERSION;
import static org.sonar.db.event.EventTesting.newEvent;

@RunWith(DataProviderRunner.class)
public class LoadPeriodsStepTest extends BaseStepTest {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();
  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

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

  @Test
  public void no_period_on_first_analysis() {
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setKey("ROOT_KEY").setVersion("1.1").build());

    // No project, no snapshot
    underTest.execute();

    assertThat(periodsHolder.getPeriod()).isNull();
  }

  @Test
  public void feed_one_period() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    SnapshotDto analysis = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227934800000L)); // 2008-11-29
    setupRoot(project);
    String textDate = "2008-11-22";

    settings.setProperty("sonar.leak.period", textDate);
    underTest.execute();

    Period period = periodsHolder.getPeriod();
    assertThat(period).isNotNull();
    assertThat(period.getMode()).isEqualTo(LEAK_PERIOD_MODE_DATE);
    assertThat(period.getModeParameter()).isEqualTo(textDate);
    assertThat(period.getSnapshotDate()).isEqualTo(analysis.getCreatedAt());
    assertThat(period.getAnalysisUuid()).isEqualTo(analysis.getUuid());
  }

  @Test
  public void no_period_when_settings_match_no_analysis() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L)); // 2008-11-29
    setupRoot(project);

    settings.setProperty("sonar.leak.period", "UNKNWOWN VERSION");
    underTest.execute();

    assertThat(periodsHolder.getPeriod()).isNull();
  }

  @Test
  public void no_period_when_settings_is_empty() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L)); // 2008-11-29
    setupRoot(project);

    settings.setProperty("sonar.leak.period", "");
    underTest.execute();

    assertThat(periodsHolder.getPeriod()).isNull();
  }

  @Test
  public void ignore_unprocessed_snapshots() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    dbTester.components().insertSnapshot(project, snapshot -> snapshot.setStatus(STATUS_UNPROCESSED).setCreatedAt(1226379600000L)); // 2008-11-29
    setupRoot(project);

    settings.setProperty("sonar.leak.period", "100");
    underTest.execute();

    assertThat(periodsHolder.getPeriod()).isNull();
  }

  @Test
  public void feed_period_by_date() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    SnapshotDto analysis1 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L).setLast(false)); // 2008-11-11
    SnapshotDto analysis2 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226494680000L).setLast(false)); // 2008-11-12
    SnapshotDto analysis3 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227157200000L).setLast(false)); // 2008-11-20
    SnapshotDto analysis4 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227358680000L).setLast(false)); // 2008-11-22
    SnapshotDto analysis5 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227934800000L).setLast(true)); // 2008-11-29
    setupRoot(project);

    String textDate = "2008-11-22";
    settings.setProperty("sonar.leak.period", textDate);
    underTest.execute();

    // Return analysis from given date 2008-11-22
    Period period = periodsHolder.getPeriod();
    assertThat(period).isNotNull();
    assertThat(period.getMode()).isEqualTo(LEAK_PERIOD_MODE_DATE);
    assertThat(period.getModeParameter()).isEqualTo(textDate);
    assertThat(period.getSnapshotDate()).isEqualTo(analysis4.getCreatedAt());
    assertThat(period.getAnalysisUuid()).isEqualTo(analysis4.getUuid());

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs().get(0)).startsWith("Compare to date 2008-11-22 (analysis of ");
  }

  @Test
  public void search_by_date_return_nearest_later_analysis() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    SnapshotDto analysis1 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L).setLast(false)); // 2008-11-11
    SnapshotDto analysis2 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226494680000L).setLast(false)); // 2008-11-12
    SnapshotDto analysis3 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227157200000L).setLast(false)); // 2008-11-20
    SnapshotDto analysis4 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227358680000L).setLast(false)); // 2008-11-22
    SnapshotDto analysis5 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227934800000L).setLast(true)); // 2008-11-29
    setupRoot(project);

    String date = "2008-11-13";
    settings.setProperty("sonar.leak.period", date);
    underTest.execute();

    // Analysis form 2008-11-20
    Period period = periodsHolder.getPeriod();
    assertThat(period).isNotNull();
    assertThat(period.getMode()).isEqualTo(LEAK_PERIOD_MODE_DATE);
    assertThat(period.getModeParameter()).isEqualTo(date);
    assertThat(period.getSnapshotDate()).isEqualTo(analysis3.getCreatedAt());
    assertThat(period.getAnalysisUuid()).isEqualTo(analysis3.getUuid());
  }

  @Test
  public void no_period_by_date() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227934800000L)); // 2008-11-29
    setupRoot(project);

    // No analysis at and after this date
    settings.setProperty("sonar.leak.period", "2008-11-30");
    underTest.execute();

    assertThat(periodsHolder.getPeriod()).isNull();
  }

  @Test
  public void feed_period_by_days() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    SnapshotDto analysis1 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L).setLast(false)); // 2008-11-11
    SnapshotDto analysis2 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226494680000L).setLast(false)); // 2008-11-12
    SnapshotDto analysis3 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227157200000L).setLast(false)); // 2008-11-20
    SnapshotDto analysis4 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227358680000L).setLast(false)); // 2008-11-22
    SnapshotDto analysis5 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227934800000L).setLast(true)); // 2008-11-29
    setupRoot(project);

    settings.setProperty("sonar.leak.period", "10");
    underTest.execute();

    // return analysis from 2008-11-20
    Period period = periodsHolder.getPeriod();
    assertThat(period).isNotNull();
    assertThat(period.getMode()).isEqualTo(LEAK_PERIOD_MODE_DAYS);
    assertThat(period.getModeParameter()).isEqualTo("10");
    assertThat(period.getSnapshotDate()).isEqualTo(analysis3.getCreatedAt());
    assertThat(period.getAnalysisUuid()).isEqualTo(analysis3.getUuid());

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs().get(0)).startsWith("Compare over 10 days (2008-11-20, analysis of ");
  }

  @Test
  public void no_period_by_days() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    setupRoot(project);

    settings.setProperty("sonar.leak.period", "0");
    underTest.execute();

    assertThat(periodsHolder.getPeriod()).isNull();
  }

  @Test
  public void feed_period_by_previous_version() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    SnapshotDto analysis1 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L).setVersion("0.9").setLast(false)); // 2008-11-11
    SnapshotDto analysis2 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226494680000L).setVersion("1.0").setLast(false)); // 2008-11-12
    SnapshotDto analysis3 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227157200000L).setVersion("1.1").setLast(false)); // 2008-11-20
    SnapshotDto analysis4 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227358680000L).setVersion("1.1").setLast(false)); // 2008-11-22
    SnapshotDto analysis5 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227934800000L).setVersion("1.1").setLast(true)); // 2008-11-29
    dbTester.events().insertEvent(newEvent(analysis1).setName("0.9").setCategory(CATEGORY_VERSION).setDate(analysis1.getCreatedAt()));
    dbTester.events().insertEvent(newEvent(analysis2).setName("1.0").setCategory(CATEGORY_VERSION).setDate(analysis2.getCreatedAt()));
    dbTester.events().insertEvent(newEvent(analysis5).setName("1.1").setCategory(CATEGORY_VERSION).setDate(analysis4.getCreatedAt()));
    setupRoot(project, "1.1");

    settings.setProperty("sonar.leak.period", "previous_version");
    underTest.execute();

    // Analysis form 2008-11-12
    Period period = periodsHolder.getPeriod();
    assertThat(period).isNotNull();
    assertThat(period.getMode()).isEqualTo(LEAK_PERIOD_MODE_PREVIOUS_VERSION);
    assertThat(period.getModeParameter()).isEqualTo("1.0");
    assertThat(period.getSnapshotDate()).isEqualTo(analysis2.getCreatedAt());
    assertThat(period.getAnalysisUuid()).isEqualTo(analysis2.getUuid());

    assertThat(logTester.logs(LoggerLevel.DEBUG)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.DEBUG).get(0)).startsWith("Compare to previous version (");
  }

  @Test
  public void feed_period_by_previous_version_with_previous_version_deleted() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    SnapshotDto analysis1 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L).setVersion("0.9").setLast(false)); // 2008-11-11
    SnapshotDto analysis2 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226494680000L).setVersion("1.0").setLast(false)); // 2008-11-12
    SnapshotDto analysis3 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227157200000L).setVersion("1.1").setLast(false)); // 2008-11-20
    dbTester.events().insertEvent(newEvent(analysis1).setName("0.9").setCategory(CATEGORY_VERSION));
    // The "1.0" version was deleted from the history
    dbTester.events().insertEvent(newEvent(analysis3).setName("1.1").setCategory(CATEGORY_VERSION));
    setupRoot(project, "1.1");

    settings.setProperty("sonar.leak.period", "previous_version");
    underTest.execute();

    // Analysis form 2008-11-11
    Period period = periodsHolder.getPeriod();
    assertThat(period).isNotNull();
    assertThat(period.getMode()).isEqualTo(LEAK_PERIOD_MODE_PREVIOUS_VERSION);
    assertThat(period.getModeParameter()).isEqualTo("0.9");
    assertThat(period.getSnapshotDate()).isEqualTo(analysis1.getCreatedAt());
    assertThat(period.getAnalysisUuid()).isEqualTo(analysis1.getUuid());
  }

  @Test
  public void no_period_by_previous_version() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    setupRoot(project);

    settings.setProperty("sonar.leak.period", "previous_version");
    underTest.execute();

    assertThat(periodsHolder.getPeriod()).isNull();
  }

  @Test
  public void feed_period_by_previous_version_with_first_analysis_when_no_previous_version_found() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    SnapshotDto analysis1 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L).setVersion("0.9").setLast(false)); // 2008-11-11
    SnapshotDto analysis5 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227934800000L).setVersion("1.1").setLast(true)); // 2008-11-29
    dbTester.events().insertEvent(newEvent(analysis5).setName("1.1").setCategory(CATEGORY_VERSION));
    setupRoot(project, "1.1");

    settings.setProperty("sonar.leak.period", "previous_version");
    underTest.execute();

    Period period = periodsHolder.getPeriod();
    assertThat(period).isNotNull();
    assertThat(period.getMode()).isEqualTo(LEAK_PERIOD_MODE_PREVIOUS_VERSION);
    assertThat(period.getModeParameter()).isNull();
    assertThat(period.getSnapshotDate()).isEqualTo(analysis1.getCreatedAt());
    assertThat(period.getAnalysisUuid()).isEqualTo(analysis1.getUuid());
  }

  @Test
  public void feed_period_by_previous_version_with_first_analysis_when_previous_snapshot_is_the_last_one() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    SnapshotDto analysis = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L).setVersion("0.9").setLast(true)); // 2008-11-11
    setupRoot(project, "1.1");

    settings.setProperty("sonar.leak.period", "previous_version");
    underTest.execute();

    Period period = periodsHolder.getPeriod();
    assertThat(period).isNotNull();
    assertThat(period.getMode()).isEqualTo(LEAK_PERIOD_MODE_PREVIOUS_VERSION);
    assertThat(period.getModeParameter()).isNull();
    assertThat(period.getSnapshotDate()).isEqualTo(analysis.getCreatedAt());
    assertThat(period.getAnalysisUuid()).isEqualTo(analysis.getUuid());
  }

  @Test
  public void feed_period_by_version() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    SnapshotDto analysis1 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L).setVersion("0.9").setLast(false)); // 2008-11-11
    SnapshotDto analysis2 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226494680000L).setVersion("1.0").setLast(false)); // 2008-11-12
    SnapshotDto analysis3 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227157200000L).setVersion("1.1").setLast(false)); // 2008-11-20
    SnapshotDto analysis4 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227358680000L).setVersion("1.1").setLast(false)); // 2008-11-22
    SnapshotDto analysis5 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227934800000L).setVersion("1.1").setLast(true)); // 2008-11-29
    dbTester.events().insertEvent(newEvent(analysis1).setName("0.9").setCategory(CATEGORY_VERSION));
    dbTester.events().insertEvent(newEvent(analysis2).setName("1.0").setCategory(CATEGORY_VERSION));
    dbTester.events().insertEvent(newEvent(analysis5).setName("1.1").setCategory(CATEGORY_VERSION));
    setupRoot(project, "1.1");

    settings.setProperty("sonar.leak.period", "1.0");
    underTest.execute();

    // Analysis form 2008-11-11
    Period period = periodsHolder.getPeriod();
    assertThat(period).isNotNull();
    assertThat(period.getMode()).isEqualTo(LEAK_PERIOD_MODE_VERSION);
    assertThat(period.getModeParameter()).isEqualTo("1.0");
    assertThat(period.getSnapshotDate()).isEqualTo(analysis2.getCreatedAt());
    assertThat(period.getAnalysisUuid()).isEqualTo(analysis2.getUuid());

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs().get(0)).startsWith("Compare to version (1.0) (");
  }

  @Test
  public void no_period_by_version() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    setupRoot(project);

    settings.setProperty("sonar.leak.period", "0.8");
    underTest.execute();

    assertThat(periodsHolder.getPeriod()).isNull();
  }

  private void setupRoot(ComponentDto project) {
    setupRoot(project, "1.1");
  }

  private void setupRoot(ComponentDto project, String version) {
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid(project.uuid()).setKey(project.getKey()).setVersion(version).build());
    when(settingsRepository.getConfiguration()).thenReturn(settings.asConfig());
  }

}
