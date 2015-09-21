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
package org.sonar.batch.cache;

import org.sonarqube.ws.QualityProfiles.WsSearchResponse.QualityProfile;
import org.sonar.batch.rule.ActiveRulesLoader;
import org.sonar.batch.repository.QualityProfileLoader;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class NonAssociatedCacheSynchronizer {
  private static final Logger LOG = LoggerFactory.getLogger(NonAssociatedCacheSynchronizer.class);

  private ProjectCacheStatus cacheStatus;
  private QualityProfileLoader qualityProfileLoader;
  private ActiveRulesLoader activeRulesLoader;

  public NonAssociatedCacheSynchronizer(QualityProfileLoader qualityProfileLoader, ActiveRulesLoader activeRulesLoader, ProjectCacheStatus cacheStatus) {
    this.qualityProfileLoader = qualityProfileLoader;
    this.activeRulesLoader = activeRulesLoader;
    this.cacheStatus = cacheStatus;
  }

  public void execute(boolean force) {
    Date lastSync = cacheStatus.getSyncStatus();

    if (lastSync != null) {
      if (!force) {
        LOG.info("Found cache [{}]", lastSync);
        return;
      } else {
        LOG.info("-- Found cache [{}], synchronizing data..", lastSync);
      }
      cacheStatus.delete();
    } else {
      LOG.info("-- Cache not found, synchronizing data..");
    }

    loadData();

    cacheStatus.save();
    LOG.info("-- Succesfully synchronized cache");
  }

  private static Collection<String> getKeys(Collection<QualityProfile> qProfiles) {
    List<String> list = new ArrayList<>(qProfiles.size());
    for (QualityProfile qp : qProfiles) {
      list.add(qp.getKey());
    }

    return list;
  }

  private void loadData() {
    Profiler profiler = Profiler.create(Loggers.get(ProjectCacheSynchronizer.class));

    profiler.startInfo("Load default quality profiles");
    Collection<QualityProfile> qProfiles = qualityProfileLoader.loadDefault(null);
    profiler.stopInfo();

    profiler.startInfo("Load default active rules");
    Collection<String> keys = getKeys(qProfiles);
    for (String k : keys) {
      activeRulesLoader.load(k, null);
    }
    profiler.stopInfo();
  }
}
