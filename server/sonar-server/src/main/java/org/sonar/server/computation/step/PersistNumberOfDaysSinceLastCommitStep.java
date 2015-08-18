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
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.measure.MeasureDto;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.metric.MetricRepository;
import org.sonar.server.source.index.SourceLineIndex;

import static com.google.common.base.Objects.firstNonNull;
import static org.sonar.server.computation.component.ComponentVisitor.Order.PRE_ORDER;

public class PersistNumberOfDaysSinceLastCommitStep implements ComputationStep {

  private static final long MILLISECONDS_PER_DAY = 1000 * 60 * 60 * 24L;

  private final DbClient dbClient;
  private final SourceLineIndex sourceLineIndex;
  private final MetricRepository metricRepository;
  private final System2 system;
  private final TreeRootHolder treeRootHolder;
  private final BatchReportReader reportReader;
  private final DbIdsRepository dbIdsRepository;

  public PersistNumberOfDaysSinceLastCommitStep(System2 system, DbClient dbClient, SourceLineIndex sourceLineIndex, MetricRepository metricRepository,
    TreeRootHolder treeRootHolder, BatchReportReader reportReader, DbIdsRepository dbIdsRepository) {
    this.dbClient = dbClient;
    this.sourceLineIndex = sourceLineIndex;
    this.metricRepository = metricRepository;
    this.system = system;
    this.treeRootHolder = treeRootHolder;
    this.reportReader = reportReader;
    this.dbIdsRepository = dbIdsRepository;
  }

  @Override
  public String getDescription() {
    return "Compute and persist the number of days since last commit";
  }

  @Override
  public void execute() {
    NumberOfDaysSinceLastCommitVisitor visitor = new NumberOfDaysSinceLastCommitVisitor();
    Component project = treeRootHolder.getRoot();
    new DepthTraversalTypeAwareCrawler(visitor).visit(project);

    long lastCommitTimestamp = visitor.lastCommitTimestampFromReport;
    if (lastCommitTimestamp == 0L) {
      Long lastCommitFromIndex = lastCommitFromIndex(treeRootHolder.getRoot().getUuid());
      lastCommitTimestamp = firstNonNull(lastCommitFromIndex, lastCommitTimestamp);
    }

    if (lastCommitTimestamp != 0L) {
      persistNumberOfDaysSinceLastCommit(lastCommitTimestamp, dbIdsRepository.getSnapshotId(project));
    }
  }

  @CheckForNull
  private Long lastCommitFromIndex(String projectUuid) {
    Date lastCommitDate = sourceLineIndex.lastCommitDateOnProject(projectUuid);
    return lastCommitDate == null ? null : lastCommitDate.getTime();
  }

  private void persistNumberOfDaysSinceLastCommit(long lastCommitTimestamp, long projectSnapshotId) {
    long numberOfDaysSinceLastCommit = (system.now() - lastCommitTimestamp) / MILLISECONDS_PER_DAY;
    DbSession dbSession = dbClient.openSession(true);
    try {
      dbClient.measureDao().insert(dbSession, new MeasureDto()
        .setValue((double) numberOfDaysSinceLastCommit)
        .setMetricId(metricRepository.getByKey(CoreMetrics.DAYS_SINCE_LAST_COMMIT_KEY).getId())
        .setSnapshotId(projectSnapshotId));
      dbSession.commit();
    } finally {
      MyBatis.closeQuietly(dbSession);
    }
  }

  private class NumberOfDaysSinceLastCommitVisitor extends TypeAwareVisitorAdapter {

    private long lastCommitTimestampFromReport = 0L;

    private NumberOfDaysSinceLastCommitVisitor() {
      super(CrawlerDepthLimit.FILE, PRE_ORDER);
    }

    @Override
    public void visitFile(Component component) {
      BatchReport.Changesets scm = reportReader.readChangesets(component.getReportAttributes().getRef());
      processScm(scm);
    }

    private void processScm(@Nullable BatchReport.Changesets scm) {
      if (scm == null) {
        return;
      }

      for (BatchReport.Changesets.Changeset changeset : scm.getChangesetList()) {
        if (changeset.hasDate() && changeset.getDate() > lastCommitTimestampFromReport) {
          lastCommitTimestampFromReport = changeset.getDate();
        }
      }
    }
  }
}
