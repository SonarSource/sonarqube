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

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.measure.db.MetricDto;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.measure.MetricCache;
import org.sonar.server.db.DbClient;
import org.sonar.server.measure.persistence.MeasureDao;
import org.sonar.server.source.index.SourceLineIndex;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PersistNumberOfDaysSinceLastCommitStepTest extends BaseStepTest {

  @ClassRule
  public static DbTester db = new DbTester();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  File dir;

  PersistNumberOfDaysSinceLastCommitStep sut;

  DbClient dbClient;
  SourceLineIndex sourceLineIndex;
  MetricCache metricCache;

  @Before
  public void setUp() throws Exception {
    dbClient = new DbClient(db.database(), db.myBatis(), new MeasureDao());
    sourceLineIndex = mock(SourceLineIndex.class);
    metricCache = mock(MetricCache.class);
    when(metricCache.get(anyString())).thenReturn(new MetricDto().setId(10));
    dir = temp.newFolder();
    db.truncateTables();

    sut = new PersistNumberOfDaysSinceLastCommitStep(System2.INSTANCE, dbClient, sourceLineIndex, metricCache);
  }

  @Override
  protected ComputationStep step() throws IOException {
    return sut;
  }

  @Test
  public void persist_number_of_days_since_last_commit_from_report() throws Exception {
    long threeDaysAgo = DateUtils.addDays(new Date(), -3).getTime();
    BatchReportWriter reportWriter = initReportWithProjectAndFile();
    reportWriter.writeComponentChangesets(
      BatchReport.Changesets.newBuilder()
        .setComponentRef(2)
        .addChangeset(
          BatchReport.Changesets.Changeset.newBuilder()
            .setDate(threeDaysAgo)
        )
        .build()
      );
    ComputationContext context = new ComputationContext(new BatchReportReader(dir), ComponentTesting.newProjectDto("project-uuid"));

    sut.execute(context);

    db.assertDbUnit(getClass(), "insert-from-report-result.xml", new String[] {"id"}, "project_measures");
  }

  @Test
  public void persist_number_of_days_since_last_commit_from_index() throws Exception {
    Date sixDaysAgo = DateUtils.addDays(new Date(), -6);
    when(sourceLineIndex.lastCommitDateOnProject("project-uuid")).thenReturn(sixDaysAgo);
    initReportWithProjectAndFile();
    ComputationContext context = new ComputationContext(new BatchReportReader(dir), ComponentTesting.newProjectDto("project-uuid"));

    sut.execute(context);

    db.assertDbUnit(getClass(), "insert-from-index-result.xml", new String[] {"id"}, "project_measures");
  }

  @Test
  public void no_scm_information_in_report_and_index() throws Exception {
    initReportWithProjectAndFile();
    ComputationContext context = new ComputationContext(new BatchReportReader(dir), ComponentTesting.newProjectDto("project-uuid"));

    sut.execute(context);

    db.assertDbUnit(getClass(), "empty.xml");
  }

  private BatchReportWriter initReportWithProjectAndFile() throws IOException {
    BatchReportWriter writer = new BatchReportWriter(dir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setSnapshotId(1000)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey("project-key")
      .setUuid("project-uuid")
      .setSnapshotId(10L)
      .addChildRef(2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.FILE)
      .setSnapshotId(11L)
      .build());

    return writer;
  }
}
