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
package org.sonar.server.computation.dbcleaner;

import java.util.Collection;
import org.sonar.api.CoreProperties;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.TimeUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbSession;
import org.sonar.db.purge.IdUuidPair;
import org.sonar.db.purge.PurgeConfiguration;
import org.sonar.db.purge.PurgeDao;
import org.sonar.db.purge.PurgeListener;
import org.sonar.db.purge.PurgeProfiler;
import org.sonar.db.purge.period.DefaultPeriodCleaner;

import static org.sonar.db.purge.PurgeConfiguration.newDefaultPurgeConfiguration;

@ServerSide
@ComputeEngineSide
public class ProjectCleaner {
  private static final Logger LOG = Loggers.get(ProjectCleaner.class);

  private final PurgeProfiler profiler;
  private final PurgeListener purgeListener;
  private final PurgeDao purgeDao;
  private final DefaultPeriodCleaner periodCleaner;

  public ProjectCleaner(PurgeDao purgeDao, DefaultPeriodCleaner periodCleaner, PurgeProfiler profiler, PurgeListener purgeListener) {
    this.purgeDao = purgeDao;
    this.periodCleaner = periodCleaner;
    this.profiler = profiler;
    this.purgeListener = purgeListener;
  }

  public ProjectCleaner purge(DbSession session, IdUuidPair rootId, Configuration projectConfig, Collection<String> disabledComponentUuids) {
    long start = System.currentTimeMillis();
    profiler.reset();

    PurgeConfiguration configuration = newDefaultPurgeConfiguration(projectConfig, rootId, disabledComponentUuids);

    periodCleaner.clean(session, configuration.rootProjectIdUuid().getUuid(), projectConfig);
    purgeDao.purge(session, configuration, purgeListener, profiler);

    session.commit();
    logProfiling(start, projectConfig);
    return this;
  }

  private void logProfiling(long start, Configuration config) {
    if (config.getBoolean(CoreProperties.PROFILING_LOG_PROPERTY).orElse(false)) {
      long duration = System.currentTimeMillis() - start;
      LOG.info("\n -------- Profiling for purge: " + TimeUtils.formatDuration(duration) + " --------\n");
      profiler.dump(duration, LOG);
      LOG.info("\n -------- End of profiling for purge --------\n");
    }
  }
}
