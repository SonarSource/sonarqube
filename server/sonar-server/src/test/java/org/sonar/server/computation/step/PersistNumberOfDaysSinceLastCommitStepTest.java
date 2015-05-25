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

import java.util.Date;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.metric.db.MetricDto;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.component.ComponentTreeBuilders;
import org.sonar.server.computation.component.DbComponentsRefCache;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.language.LanguageRepository;
import org.sonar.server.computation.measure.MetricCache;
import org.sonar.server.db.DbClient;
import org.sonar.server.measure.persistence.MeasureDao;
import org.sonar.server.source.index.SourceLineIndex;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PersistNumberOfDaysSinceLastCommitStepTest extends BaseStepTest {

  @ClassRule
  public static DbTester db = new DbTester();
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  PersistNumberOfDaysSinceLastCommitStep sut;

  DbClient dbClient;
  SourceLineIndex sourceLineIndex;
  MetricCache metricCache;
  Settings projectSettings;
  LanguageRepository languageRepository;

  DbComponentsRefCache dbComponentsRefCache;

  @Before
  public void setUp() throws Exception {
    db.truncateTables();
    dbClient = new DbClient(db.database(), db.myBatis(), new MeasureDao());
    sourceLineIndex = mock(SourceLineIndex.class);
    metricCache = mock(MetricCache.class);
    projectSettings = new Settings();
    languageRepository = mock(LanguageRepository.class);
    when(metricCache.get(anyString())).thenReturn(new MetricDto().setId(10));
    dbComponentsRefCache = new DbComponentsRefCache();

    sut = new PersistNumberOfDaysSinceLastCommitStep(System2.INSTANCE, dbClient, sourceLineIndex, metricCache, dbComponentsRefCache);
  }

  @Override
  protected ComputationStep step() {
    return sut;
  }

  @Test
  public void persist_number_of_days_since_last_commit_from_report() {
    long threeDaysAgo = DateUtils.addDays(new Date(), -3).getTime();
    initReportWithProjectAndFile();
    reportReader.putChangesets(
      BatchReport.Changesets.newBuilder()
        .setComponentRef(2)
        .addChangeset(
          BatchReport.Changesets.Changeset.newBuilder()
            .setDate(threeDaysAgo)
        )
        .build()
      );
    ComputationContext context = new ComputationContext(reportReader, "PROJECT_KEY", projectSettings, dbClient, ComponentTreeBuilders.from(DumbComponent.DUMB_PROJECT),
      languageRepository);

    sut.execute(context);

    db.assertDbUnit(getClass(), "insert-from-report-result.xml", new String[] {"id"}, "project_measures");
  }

  @Test
  public void persist_number_of_days_since_last_commit_from_index() {
    Date sixDaysAgo = DateUtils.addDays(new Date(), -6);
    when(sourceLineIndex.lastCommitDateOnProject("project-uuid")).thenReturn(sixDaysAgo);
    initReportWithProjectAndFile();
    ComputationContext context = new ComputationContext(reportReader, "PROJECT_KEY", projectSettings, dbClient, ComponentTreeBuilders.from(DumbComponent.DUMB_PROJECT),
      languageRepository);

    sut.execute(context);

    db.assertDbUnit(getClass(), "insert-from-index-result.xml", new String[] {"id"}, "project_measures");
  }

  @Test
  public void no_scm_information_in_report_and_index() {
    initReportWithProjectAndFile();
    ComputationContext context = new ComputationContext(reportReader, "PROJECT_KEY", projectSettings, dbClient, ComponentTreeBuilders.from(DumbComponent.DUMB_PROJECT),
      languageRepository);

    sut.execute(context);

    db.assertDbUnit(getClass(), "empty.xml");
  }

  private void initReportWithProjectAndFile() {
    dbComponentsRefCache.addComponent(1, new DbComponentsRefCache.DbComponent(1L, "PROJECT_KEY", "project-uuid"));
    dbComponentsRefCache.addComponent(2, new DbComponentsRefCache.DbComponent(2L, "PROJECT_KEY:file", "file-uuid"));

    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setSnapshotId(1000)
      .build());

    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey("PROJECT_KEY")
      .setSnapshotId(10L)
      .addChildRef(2)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.FILE)
      .setSnapshotId(11L)
      .build());
  }
}
