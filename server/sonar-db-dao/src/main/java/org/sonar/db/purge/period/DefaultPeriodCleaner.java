/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.DbSession;
import org.sonar.db.purge.PurgeDao;
import org.sonar.db.purge.PurgeProfiler;
import org.sonar.db.purge.PurgeableAnalysisDto;

public class DefaultPeriodCleaner {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultPeriodCleaner.class);
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
      List<PurgeableAnalysisDto> toDelete = filter.filter(history);
      List<PurgeableAnalysisDto> deleted = delete(rootUuid, toDelete, session);
      history.removeAll(deleted);
    }
  }

  private List<PurgeableAnalysisDto> delete(String rootUuid, List<PurgeableAnalysisDto> snapshots, DbSession session) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("<- Delete analyses of component {}: {}",
        rootUuid,
        snapshots.stream().map(snapshot -> snapshot.getAnalysisUuid() + "@" + DateUtils.formatDateTime(snapshot.getDate()))
          .collect(Collectors.joining(", ")));
    }
    purgeDao.deleteAnalyses(
      session, profiler,
      snapshots.stream().map(PurgeableAnalysisDto::getAnalysisUuid).toList());
    return snapshots;
  }

  private List<PurgeableAnalysisDto> selectAnalysesOfComponent(String componentUuid, DbSession session) {
    return purgeDao.selectProcessedAnalysisByComponentUuid(componentUuid, session);
  }
}
