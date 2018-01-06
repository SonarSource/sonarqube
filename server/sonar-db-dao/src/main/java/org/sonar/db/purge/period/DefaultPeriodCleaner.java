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
package org.sonar.db.purge.period;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.List;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbSession;
import org.sonar.db.purge.IdUuidPair;
import org.sonar.db.purge.PurgeDao;
import org.sonar.db.purge.PurgeProfiler;
import org.sonar.db.purge.PurgeableAnalysisDto;

public class DefaultPeriodCleaner {

  private static final Logger LOG = Loggers.get(DefaultPeriodCleaner.class);
  private final PurgeDao purgeDao;
  private final PurgeProfiler profiler;

  public DefaultPeriodCleaner(PurgeDao purgeDao, PurgeProfiler profiler) {
    this.purgeDao = purgeDao;
    this.profiler = profiler;
  }

  public void clean(DbSession session, String rootUuid, Configuration config) {
    doClean(rootUuid, new Filters(config).all(), session);
  }

  @VisibleForTesting
  void doClean(String rootUuid, List<Filter> filters, DbSession session) {
    List<PurgeableAnalysisDto> history = new ArrayList<>(selectAnalysesOfComponent(rootUuid, session));
    for (Filter filter : filters) {
      filter.log();
      history.removeAll(delete(rootUuid, filter.filter(history), session));
    }
  }

  private List<PurgeableAnalysisDto> delete(String rootUuid, List<PurgeableAnalysisDto> snapshots, DbSession session) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("<- Delete analyses of component {}: {}",
        rootUuid,
        Joiner.on(", ").join(
          snapshots.stream()
            .map(snapshot -> snapshot.getAnalysisUuid() + "@" + DateUtils.formatDateTime(snapshot.getDate()))
            .collect(MoreCollectors.toArrayList(snapshots.size()))));
    }
    purgeDao.deleteAnalyses(
      session, profiler,
      snapshots.stream().map(DefaultPeriodCleaner::toIdUuidPair).collect(MoreCollectors.toList(snapshots.size())));
    return snapshots;
  }

  private static IdUuidPair toIdUuidPair(PurgeableAnalysisDto snapshot) {
    return new IdUuidPair(snapshot.getAnalysisId(), snapshot.getAnalysisUuid());
  }

  private List<PurgeableAnalysisDto> selectAnalysesOfComponent(String componentUuid, DbSession session) {
    return purgeDao.selectPurgeableAnalyses(componentUuid, session);
  }
}
