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

package org.sonar.db.purge.period;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbSession;
import org.sonar.db.purge.PurgeDao;
import org.sonar.db.purge.PurgeProfiler;
import org.sonar.db.purge.PurgeSnapshotQuery;
import org.sonar.db.purge.PurgeableSnapshotDto;

@ServerSide
public class DefaultPeriodCleaner {

  private static final Logger LOG = Loggers.get(DefaultPeriodCleaner.class);
  private final PurgeDao purgeDao;
  private final PurgeProfiler profiler;

  public DefaultPeriodCleaner(PurgeDao purgeDao, PurgeProfiler profiler) {
    this.purgeDao = purgeDao;
    this.profiler = profiler;
  }

  public void clean(DbSession session, long projectId, Settings settings) {
    doClean(projectId, new Filters(settings).all(), session);
  }

  @VisibleForTesting
  void doClean(long projectId, List<Filter> filters, DbSession session) {
    List<PurgeableSnapshotDto> history = selectProjectSnapshots(projectId, session);
    for (Filter filter : filters) {
      filter.log();
      delete(filter.filter(history), session);
    }
  }

  private void delete(List<PurgeableSnapshotDto> snapshots, DbSession session) {
    for (PurgeableSnapshotDto snapshot : snapshots) {
      LOG.debug("<- Delete snapshot: {} [{}]", DateUtils.formatDateTime(snapshot.getDate()), snapshot.getSnapshotId());
      purgeDao.deleteSnapshots(PurgeSnapshotQuery.create().setRootSnapshotId(snapshot.getSnapshotId()), session, profiler);
      purgeDao.deleteSnapshots(PurgeSnapshotQuery.create().setId(snapshot.getSnapshotId()), session, profiler);
    }
  }

  private List<PurgeableSnapshotDto> selectProjectSnapshots(long resourceId, DbSession session) {
    return purgeDao.selectPurgeableSnapshots(resourceId, session);
  }
}
