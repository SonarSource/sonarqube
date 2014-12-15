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

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.TimeUtils;
import org.sonar.core.purge.*;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.plugins.dbcleaner.api.PurgeTask;
import org.sonar.core.computation.dbcleaner.period.DefaultPeriodCleaner;

import static org.sonar.core.purge.PurgeConfiguration.newDefaultPurgeConfiguration;

/**
 * @since 2.14
 */
public class DefaultPurgeTask implements PurgeTask {
  private static final Logger LOG = LoggerFactory.getLogger(DefaultPurgeTask.class);
  private final PurgeProfiler profiler;
  private final PurgeDao purgeDao;
  private final ResourceDao resourceDao;
  private final Settings settings;
  private final DefaultPeriodCleaner periodCleaner;

  public DefaultPurgeTask(PurgeDao purgeDao, ResourceDao resourceDao, Settings settings, DefaultPeriodCleaner periodCleaner, PurgeProfiler profiler) {
    this.purgeDao = purgeDao;
    this.resourceDao = resourceDao;
    this.settings = settings;
    this.periodCleaner = periodCleaner;
    this.profiler = profiler;
  }

  @Override
  public DefaultPurgeTask delete(long resourceId) {
    ResourceDto resource = resourceDao.getResource(resourceId);
    if (resource != null) {
      purgeDao.deleteResourceTree(new IdUuidPair(resource.getId(), resource.getUuid()));
    }

    return this;
  }

  @VisibleForTesting
  boolean isNotViewNorSubview(String resourceQualifier) {
    return !(Qualifiers.VIEW.equals(resourceQualifier) || Qualifiers.SUBVIEW.equals(resourceQualifier));
  }

  @Override
  public DefaultPurgeTask purge(long resourceId) {
    long start = System.currentTimeMillis();
    String resourceQualifier = resourceDao.getResource(resourceId).getQualifier();
    if (isNotViewNorSubview(resourceQualifier)) {
      profiler.reset();
      cleanHistoricalData(resourceId);
      doPurge(resourceId);
      if (settings.getBoolean(CoreProperties.PROFILING_LOG_PROPERTY)) {
        long duration = System.currentTimeMillis() - start;
        LOG.info("\n -------- Profiling for purge: " + TimeUtils.formatDuration(duration) + " --------\n");
        profiler.dump(duration, LOG);
        LOG.info("\n -------- End of profiling for purge --------\n");
      }
    }
    return this;
  }

  private void cleanHistoricalData(long resourceId) {
    try {
      periodCleaner.clean(resourceId);
    } catch (Exception e) {
      // purge errors must no fail the batch
      LOG.error("Fail to clean historical data [id=" + resourceId + "]", e);
    }
  }

  private void doPurge(long resourceId) {
    try {
      purgeDao.purge(newPurgeConfigurationOnResource(resourceId), PurgeListener.EMPTY);
    } catch (Exception e) {
      // purge errors must no fail the report analysis
      LOG.error("Fail to purge data [id=" + resourceId + "]", e);
    }
  }

  public PurgeConfiguration newPurgeConfigurationOnResource(long resourceId) {
    return newDefaultPurgeConfiguration(settings, resourceId);
  }
}
