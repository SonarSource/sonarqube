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

package org.sonar.core.computation.dbcleaner;

import org.sonar.api.CoreProperties;
import org.sonar.api.ServerSide;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.TimeUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.computation.dbcleaner.period.DefaultPeriodCleaner;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.purge.IdUuidPair;
import org.sonar.core.purge.PurgeConfiguration;
import org.sonar.core.purge.PurgeDao;
import org.sonar.core.purge.PurgeListener;
import org.sonar.core.purge.PurgeProfiler;
import org.sonar.server.issue.index.IssueIndex;

import javax.annotation.Nullable;

import java.util.Date;

import static org.sonar.core.purge.PurgeConfiguration.newDefaultPurgeConfiguration;

@ServerSide
public class ProjectCleaner {
  private static final Logger LOG = Loggers.get(ProjectCleaner.class);

  private final PurgeProfiler profiler;
  private final PurgeListener purgeListener;
  private final PurgeDao purgeDao;
  private final DefaultPeriodCleaner periodCleaner;
  private final IssueIndex issueIndex;

  public ProjectCleaner(PurgeDao purgeDao, DefaultPeriodCleaner periodCleaner, PurgeProfiler profiler, PurgeListener purgeListener,
    IssueIndex issueIndex) {
    this.purgeDao = purgeDao;
    this.periodCleaner = periodCleaner;
    this.profiler = profiler;
    this.purgeListener = purgeListener;
    this.issueIndex = issueIndex;
  }

  public ProjectCleaner purge(DbSession session, IdUuidPair idUuidPair, Settings projectSettings) {
    long start = System.currentTimeMillis();
    profiler.reset();

    PurgeConfiguration configuration = newDefaultPurgeConfiguration(projectSettings, idUuidPair);

    cleanHistoricalData(session, configuration.rootProjectIdUuid().getId(), projectSettings);
    doPurge(session, configuration);

    deleteIndexedIssuesBefore(idUuidPair.getUuid(), configuration.maxLiveDateOfClosedIssues());

    session.commit();
    logProfiling(start, projectSettings);
    return this;
  }

  private void deleteIndexedIssuesBefore(String uuid, @Nullable Date lastDateWithClosedIssues) {
    if (lastDateWithClosedIssues != null) {
      issueIndex.deleteClosedIssuesOfProjectBefore(uuid, lastDateWithClosedIssues);
    }
  }

  private void logProfiling(long start, Settings settings) {
    if (settings.getBoolean(CoreProperties.PROFILING_LOG_PROPERTY)) {
      long duration = System.currentTimeMillis() - start;
      LOG.info("\n -------- Profiling for purge: " + TimeUtils.formatDuration(duration) + " --------\n");
      profiler.dump(duration, LOG);
      LOG.info("\n -------- End of profiling for purge --------\n");
    }
  }

  private void cleanHistoricalData(DbSession session, long resourceId, Settings settings) {
    try {
      periodCleaner.clean(session, resourceId, settings);
    } catch (Exception e) {
      // purge errors must no fail the batch
      LOG.error("Fail to clean historical data [id=" + resourceId + "]", e);
    }
  }

  private void doPurge(DbSession session, PurgeConfiguration configuration) {
    try {
      purgeDao.purge(session, configuration, purgeListener);
    } catch (Exception e) {
      // purge errors must no fail the report analysis
      LOG.error("Fail to purge data [id=" + configuration.rootProjectIdUuid().getId() + "]", e);
    }
  }
}
