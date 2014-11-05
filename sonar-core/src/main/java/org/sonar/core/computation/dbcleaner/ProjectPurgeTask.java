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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.ServerComponent;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.TimeUtils;
import org.sonar.core.computation.dbcleaner.period.DefaultPeriodCleaner;
import org.sonar.core.purge.PurgeConfiguration;
import org.sonar.core.purge.PurgeDao;
import org.sonar.core.purge.PurgeProfiler;

public class ProjectPurgeTask implements ServerComponent {
  private static final Logger LOG = LoggerFactory.getLogger(ProjectPurgeTask.class);
  private final PurgeProfiler profiler;
  private final PurgeDao purgeDao;
  private final DefaultPeriodCleaner periodCleaner;

  public ProjectPurgeTask(PurgeDao purgeDao, DefaultPeriodCleaner periodCleaner, PurgeProfiler profiler) {
    this.purgeDao = purgeDao;
    this.periodCleaner = periodCleaner;
    this.profiler = profiler;
  }

  public ProjectPurgeTask purge(PurgeConfiguration configuration, Settings settings) {
    long start = System.currentTimeMillis();
    profiler.reset();
    cleanHistoricalData(configuration.rootProjectId(), settings);
    doPurge(configuration);
    if (settings.getBoolean(CoreProperties.PROFILING_LOG_PROPERTY)) {
      long duration = System.currentTimeMillis() - start;
      LOG.info("\n -------- Profiling for purge: " + TimeUtils.formatDuration(duration) + " --------\n");
      profiler.dump(duration, LOG);
      LOG.info("\n -------- End of profiling for purge --------\n");
    }
    return this;
  }

  private void cleanHistoricalData(long resourceId, Settings settings) {
    try {
      periodCleaner.clean(resourceId, settings);
    } catch (Exception e) {
      // purge errors must no fail the batch
      LOG.error("Fail to clean historical data [id=" + resourceId + "]", e);
    }
  }

  private void doPurge(PurgeConfiguration configuration) {
    try {
      purgeDao.purge(configuration);
    } catch (Exception e) {
      // purge errors must no fail the report analysis
      LOG.error("Fail to purge data [id=" + configuration.rootProjectId() + "]", e);
    }
  }
}
