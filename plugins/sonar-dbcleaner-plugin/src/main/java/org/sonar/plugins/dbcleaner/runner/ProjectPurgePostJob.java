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
package org.sonar.plugins.dbcleaner.runner;

import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Scopes;
import org.sonar.core.NotDryRun;
import org.sonar.core.purge.PurgeDao;
import org.sonar.core.purge.PurgeSnapshotQuery;
import org.sonar.plugins.dbcleaner.api.DbCleanerConstants;
import org.sonar.plugins.dbcleaner.period.DefaultPeriodCleaner;

@Properties({
  @Property(
    key = DbCleanerConstants.PROPERTY_CLEAN_DIRECTORY,
    defaultValue = "false",
    name = "Clean history data of directories/packages")
})
@NotDryRun
public class ProjectPurgePostJob implements PostJob {

  private PurgeDao purgeDao;
  private Settings settings;
  private DefaultPeriodCleaner periodCleaner;

  public ProjectPurgePostJob(PurgeDao purgeDao, Settings settings, DefaultPeriodCleaner periodCleaner) {
    this.purgeDao = purgeDao;
    this.settings = settings;
    this.periodCleaner = periodCleaner;
  }

  public void executeOn(final Project project, SensorContext context) {
    long projectId = (long) project.getId();
    cleanHistory(projectId);
    deleteAbortedBuilds(projectId);
    deleteFileHistory(projectId);
    if (settings.getBoolean(DbCleanerConstants.PROPERTY_CLEAN_DIRECTORY)) {
      deleteDirectoryHistory(projectId);
    }
    purgeProject(projectId);
  }

  private void cleanHistory(long projectId) {
    periodCleaner.purge(projectId);
  }

  private void purgeProject(long projectId) {
    purgeDao.purgeProject(projectId);
  }

  private void deleteDirectoryHistory(long projectId) {
    PurgeSnapshotQuery query = PurgeSnapshotQuery.create()
      .setRootProjectId(projectId)
      .setIslast(false)
      .setScopes(new String[]{Scopes.DIRECTORY});
    purgeDao.deleteSnapshots(query);
  }

  private void deleteFileHistory(long projectId) {
    PurgeSnapshotQuery query = PurgeSnapshotQuery.create()
      .setRootProjectId(projectId)
      .setIslast(false)
      .setScopes(new String[]{Scopes.FILE});
    purgeDao.deleteSnapshots(query);
  }

  private void deleteAbortedBuilds(long projectId) {
    PurgeSnapshotQuery query = PurgeSnapshotQuery.create()
      .setRootProjectId(projectId)
      .setIslast(false)
      .setStatus(new String[]{"U"});
    purgeDao.deleteSnapshots(query);
  }
}
