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
package org.sonar.batch.index;

import org.sonar.api.batch.sensor.duplication.DuplicationGroup;
import org.sonar.api.database.model.MeasureMapper;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.rules.RuleFinder;
import org.sonar.batch.duplication.DuplicationCache;
import org.sonar.batch.duplication.DuplicationUtils;
import org.sonar.batch.index.Cache.Entry;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;

import java.util.List;

public final class DuplicationPersister implements ScanPersister {
  private final MyBatis mybatis;
  private final RuleFinder ruleFinder;
  private final ResourceCache resourceCache;
  private final DuplicationCache duplicationCache;
  private final MetricFinder metricFinder;

  public DuplicationPersister(MyBatis mybatis, RuleFinder ruleFinder, ResourceCache resourceCache,
    DuplicationCache duplicationCache, MetricFinder metricFinder) {
    this.mybatis = mybatis;
    this.ruleFinder = ruleFinder;
    this.resourceCache = resourceCache;
    this.duplicationCache = duplicationCache;
    this.metricFinder = metricFinder;
  }

  @Override
  public void persist() {
    // Don't use batch insert for duplications since keeping all data in memory can produce OOM
    try (DbSession session = mybatis.openSession(false)) {
      MeasureMapper mapper = session.getMapper(MeasureMapper.class);
      Metric duplicationMetricWithId = metricFinder.findByKey(CoreMetrics.DUPLICATIONS_DATA_KEY);
      for (Entry<List<DuplicationGroup>> entry : duplicationCache.entries()) {
        String effectiveKey = entry.key()[0].toString();
        Measure measure = new Measure(duplicationMetricWithId, DuplicationUtils.toXml(entry.value())).setPersistenceMode(PersistenceMode.DATABASE);
        BatchResource batchResource = resourceCache.get(effectiveKey);

        if (MeasurePersister.shouldPersistMeasure(batchResource.resource(), measure)) {
          MeasureModel measureModel = MeasurePersister.model(measure, ruleFinder, metricFinder).setSnapshotId(batchResource.snapshotId());
          mapper.insert(measureModel);
          session.commit();
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException("Unable to save some measures", e);
    }
  }

}
