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

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.SnapshotDto;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.measure.db.MetricDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.SnapshotTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.component.db.SnapshotDao;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasuresCache;
import org.sonar.server.computation.measure.MetricCache;
import org.sonar.server.db.DbClient;
import org.sonar.server.measure.persistence.MeasureDao;
import org.sonar.server.measure.persistence.MetricDao;
import org.sonar.server.util.cache.DiskCacheById;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class PersistComputeMeasuresStepTest extends BaseStepTest {

  static final String PROJECT_UUID = "PROJECT";
  static final String METRIC_KEY = "Metric";

  @ClassRule
  public static DbTester dbTester = new DbTester();

  DbClient dbClient;

  DbSession session;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  BatchReportWriter writer;

  ComputationContext context;

  MeasuresCache measuresCache;

  PersistComputeMeasuresStep sut;

  @Before
  public void setup() throws Exception {
    dbTester.truncateTables();

    File reportDir = temp.newFolder();
    writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());
    context = new ComputationContext(new BatchReportReader(reportDir), ComponentTesting.newProjectDto(PROJECT_UUID));

    measuresCache = new MeasuresCache(temp.newFolder("measures"), System2.INSTANCE);

    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new MeasureDao(), new MetricDao(), new ComponentDao(), new SnapshotDao(System2.INSTANCE));
    session = dbClient.openSession(false);

    dbClient.metricDao().insert(session, new MetricDto().setKey(METRIC_KEY).setEnabled(true));
    session.commit();

    sut = (PersistComputeMeasuresStep) step();
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Override
  protected ComputationStep step() throws IOException {
    return new PersistComputeMeasuresStep(dbClient, new MetricCache(dbClient), measuresCache);
  }

  @Test
  public void persist_one_measure() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto(PROJECT_UUID).setKey("ProjectKey");
    dbClient.componentDao().insert(session, project);
    SnapshotDto snapshotDto = SnapshotTesting.createForProject(project);
    dbClient.snapshotDao().insert(session, snapshotDto);

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setUuid(PROJECT_UUID)
      .setId(project.getId())
      .setSnapshotId(snapshotDto.getId())
      .build());
    DiskCacheById<Measure>.DiskAppender measureAppender = measuresCache.newAppender(1);
    measureAppender.append(new Measure().setComponentUuid(PROJECT_UUID).setMetricKey(METRIC_KEY).setValue(2d));
    measureAppender.close();
    session.commit();

    sut.execute(context);

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(1);
    MeasureDto measureDto = dbClient.measureDao().findByComponentKeyAndMetricKey(session, "ProjectKey", METRIC_KEY);
    assertThat(measureDto.getMetricKey()).isEqualTo(METRIC_KEY);
    assertThat(measureDto.getComponentId()).isEqualTo(project.getId());
    assertThat(measureDto.getSnapshotId()).isEqualTo(snapshotDto.getId());
    assertThat(measureDto.getValue()).isEqualTo(2d);
  }

}
