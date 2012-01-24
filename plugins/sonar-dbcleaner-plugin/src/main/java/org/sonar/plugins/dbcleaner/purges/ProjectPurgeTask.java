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
package org.sonar.plugins.dbcleaner.purges;

import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.core.purge.PurgeDao;
import org.sonar.core.purge.PurgeSnapshotQuery;

public class ProjectPurgeTask implements BatchExtension {

  private ProjectPurge[] purges;
  private PurgeDao purgeDao;

  public ProjectPurgeTask(ProjectPurge[] purges, PurgeDao purgeDao) {
    this.purges = purges;
    this.purgeDao = purgeDao;
  }

  public ProjectPurgeTask execute(ProjectPurgeContext context) {
    purgeProject(context);
    purgeSnapshots(context);
    return this;
  }

  private void purgeProject(ProjectPurgeContext purgeContext) {
    for (ProjectPurge purge : purges) {
      LoggerFactory.getLogger(getClass()).debug("Executing purge " + purge);
      purge.execute(purgeContext);
    }
  }

  private void purgeSnapshots(ProjectPurgeContext context) {
    LoggerFactory.getLogger(getClass()).debug("Purging snapshots");
    PurgeSnapshotQuery query = PurgeSnapshotQuery.create()
      .setBeforeBuildDate(context.getBeforeBuildDate())
      .setRootProjectId(context.getRootProjectId());
    purgeDao.purgeSnapshots(query);
  }
}
