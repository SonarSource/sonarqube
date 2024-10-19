/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.purge;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.TimeUtils;
import org.sonar.db.DbSession;
import org.sonar.db.purge.PurgeConfiguration;
import org.sonar.db.purge.PurgeDao;
import org.sonar.db.purge.PurgeListener;
import org.sonar.db.purge.PurgeProfiler;
import org.sonar.db.purge.period.DefaultPeriodCleaner;

import static org.sonar.db.purge.PurgeConfiguration.newDefaultPurgeConfiguration;

@ServerSide
@ComputeEngineSide
public class ProjectCleaner {
  private static final Logger LOG = LoggerFactory.getLogger(ProjectCleaner.class);

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

  public ProjectCleaner purge(DbSession session, String rootUuid, String projectUuid, Configuration projectConfig, Set<String> disabledComponentUuids) {
    long start = System.currentTimeMillis();
    profiler.reset();

    periodCleaner.clean(session, rootUuid, projectConfig);

    PurgeConfiguration configuration = newDefaultPurgeConfiguration(projectConfig, rootUuid, projectUuid, disabledComponentUuids);
    purgeDao.purge(session, configuration, purgeListener, profiler);

    session.commit();
    logProfiling(start, projectConfig);
    return this;
  }

  private void logProfiling(long start, Configuration config) {
    if (config.getBoolean(CoreProperties.PROFILING_LOG_PROPERTY).orElse(false)) {
      long duration = System.currentTimeMillis() - start;
      LOG.info("");
      LOG.atInfo().setMessage(" -------- Profiling for purge: {} --------").addArgument(() -> TimeUtils.formatDuration(duration)).log();
      LOG.info("");
      for (String line : profiler.getProfilingResult(duration)) {
        LOG.info(line);
      }
      LOG.info("");
      LOG.info(" -------- End of profiling for purge --------");
      LOG.info("");
    }
  }
}
