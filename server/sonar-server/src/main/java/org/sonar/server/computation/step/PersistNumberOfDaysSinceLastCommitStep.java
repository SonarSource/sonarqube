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

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.measure.MetricCache;
import org.sonar.server.db.DbClient;
import org.sonar.server.source.index.SourceLineIndex;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Date;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Preconditions.checkState;

public class PersistNumberOfDaysSinceLastCommitStep implements ComputationStep {

  private static final long MILLISECONDS_PER_DAY = 1000 * 60 * 60 * 24L;

  private final DbClient dbClient;
  private final SourceLineIndex sourceLineIndex;
  private final MetricCache metricCache;
  private final System2 system;

  private long lastCommitTimestamp = 0L;

  public PersistNumberOfDaysSinceLastCommitStep(System2 system, DbClient dbClient, SourceLineIndex sourceLineIndex, MetricCache metricCache) {
    this.dbClient = dbClient;
    this.sourceLineIndex = sourceLineIndex;
    this.metricCache = metricCache;
    this.system = system;
  }

  @Override
  public String[] supportedProjectQualifiers() {
    return new String[] {Qualifiers.PROJECT};
  }

  @Override
  public String getDescription() {
    return "Compute and persist the number of days since last commit";
  }

  @Override
  public void execute(ComputationContext context) {
    int rootComponentRef = context.getReportMetadata().getRootComponentRef();
    recursivelyProcessComponent(context, rootComponentRef);

    if (!commitFound()) {
      Long lastCommitFromIndex = lastCommitFromIndex(context.getProject().uuid());
      lastCommitTimestamp = firstNonNull(lastCommitFromIndex, lastCommitTimestamp);
    }

    if (commitFound()) {
      persistNumberOfDaysSinceLastCommit(context);
    }
  }

  private void recursivelyProcessComponent(ComputationContext context, int componentRef) {
    BatchReportReader reportReader = context.getReportReader();
    BatchReport.Component component = reportReader.readComponent(componentRef);
    BatchReport.Changesets scm = reportReader.readChangesets(componentRef);
    processScm(scm);

    for (Integer childRef : component.getChildRefList()) {
      recursivelyProcessComponent(context, childRef);
    }
  }

  private void processScm(@Nullable BatchReport.Changesets scm) {
    if (scm == null) {
      return;
    }

    for (BatchReport.Changesets.Changeset changeset : scm.getChangesetList()) {
      if (changeset.hasDate() && changeset.getDate() > lastCommitTimestamp) {
        lastCommitTimestamp = changeset.getDate();
      }
    }
  }

  @CheckForNull
  private Long lastCommitFromIndex(String projectUuid) {
    Date lastCommitDate = sourceLineIndex.lastCommitDateOnProject(projectUuid);
    return lastCommitDate == null ? null : lastCommitDate.getTime();
  }

  private void persistNumberOfDaysSinceLastCommit(ComputationContext context) {
    checkState(commitFound(), "The last commit time should exist");

    long numberOfDaysSinceLastCommit = (system.now() - lastCommitTimestamp) / MILLISECONDS_PER_DAY;
    DbSession dbSession = dbClient.openSession(true);
    try {
      dbClient.measureDao().insert(dbSession, new MeasureDto()
        .setValue((double) numberOfDaysSinceLastCommit)
        .setMetricId(metricCache.get(CoreMetrics.DAYS_SINCE_LAST_COMMIT_KEY).getId())
        .setSnapshotId(context.getReportMetadata().getSnapshotId()));
      dbSession.commit();
    } finally {
      MyBatis.closeQuietly(dbSession);
    }
  }

  private boolean commitFound() {
    return lastCommitTimestamp != 0L;
  }
}
