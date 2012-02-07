/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.dbcleaner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Scopes;
import org.sonar.core.purge.PurgeDao;
import org.sonar.core.purge.PurgeSnapshotQuery;
import org.sonar.plugins.dbcleaner.api.DbCleanerConstants;
import org.sonar.plugins.dbcleaner.api.PurgeTask;
import org.sonar.plugins.dbcleaner.period.DefaultPeriodCleaner;

/**
 * @since 2.14
 */
@Properties({
  @Property(
    key = DbCleanerConstants.PROPERTY_CLEAN_DIRECTORY,
    defaultValue = "true",
    name = "Clean history data of directories/packages")
})
public class DefaultPurgeTask implements PurgeTask {
  private static final Logger LOG = LoggerFactory.getLogger(ProjectPurgePostJob.class);

  private PurgeDao purgeDao;
  private Settings settings;
  private DefaultPeriodCleaner periodCleaner;

  public DefaultPurgeTask(PurgeDao purgeDao, Settings settings, DefaultPeriodCleaner periodCleaner) {
    this.purgeDao = purgeDao;
    this.settings = settings;
    this.periodCleaner = periodCleaner;
  }

  public PurgeTask purgeProject(long projectId) {
    cleanHistoricalData(projectId);
    deleteAbortedBuilds(projectId);
    deleteFileHistory(projectId);
    if (settings.getBoolean(DbCleanerConstants.PROPERTY_CLEAN_DIRECTORY)) {
      deleteDirectoryHistory(projectId);
    }
    purgeProjectResources(projectId);
    return this;
  }

  public PurgeTask deleteProject(long projectId) {
    purgeDao.deleteProject(projectId);
    return this;
  }

  private void cleanHistoricalData(long projectId) {
    try {
      LOG.debug("Clean project historical data [id=" + projectId + "]");
      periodCleaner.purge(projectId);
    } catch (Exception e) {
      // purge errors must no fail the batch
      LOG.error("Fail to clean project historical data [id=" + projectId + "]", e);
    }
  }

  private void purgeProjectResources(long projectId) {
    try {
      LOG.debug("Purge project [id=" + projectId + "]");
      purgeDao.purgeProject(projectId);
    } catch (Exception e) {
      // purge errors must no fail the batch
      LOG.error("Fail to purge project [id=" + projectId + "]", e);
    }
  }

  private void deleteDirectoryHistory(long projectId) {
    try {
      LOG.debug("Delete historical data of directories [id=" + projectId + "]");
      PurgeSnapshotQuery query = PurgeSnapshotQuery.create()
        .setRootProjectId(projectId)
        .setIslast(false)
        .setScopes(new String[]{Scopes.DIRECTORY});
      purgeDao.deleteSnapshots(query);
    } catch (Exception e) {
      // purge errors must no fail the batch
      LOG.error("Fail to delete historical data of directories [id=" + projectId + "]", e);
    }
  }

  private void deleteFileHistory(long projectId) {
    try {
      LOG.debug("Delete historical data of files [id=" + projectId + "]");
      PurgeSnapshotQuery query = PurgeSnapshotQuery.create()
        .setRootProjectId(projectId)
        .setIslast(false)
        .setScopes(new String[]{Scopes.FILE});
      purgeDao.deleteSnapshots(query);
    } catch (Exception e) {
      // purge errors must no fail the batch
      LOG.error("Fail to delete historical data of files [id=" + projectId + "]", e);
    }
  }

  private void deleteAbortedBuilds(long projectId) {
    try {
      LOG.debug("Delete aborted builds [id=" + projectId + "]");
      PurgeSnapshotQuery query = PurgeSnapshotQuery.create()
        .setRootProjectId(projectId)
        .setIslast(false)
        .setStatus(new String[]{"U"});
      purgeDao.deleteSnapshots(query);
    } catch (Exception e) {
      // purge errors must no fail the batch
      LOG.error("Fail to delete historical aborted builds [id=" + projectId + "]", e);
    }
  }
}
