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

import org.apache.commons.lang.StringEscapeUtils;
import org.sonar.api.batch.sensor.duplication.DuplicationGroup;
import org.sonar.api.database.model.MeasureMapper;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.RuleFinder;
import org.sonar.batch.duplication.DuplicationCache;
import org.sonar.batch.index.Cache.Entry;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;

import java.util.List;

public final class DuplicationPersister implements ScanPersister {
  private final MyBatis mybatis;
  private final RuleFinder ruleFinder;
  private final SnapshotCache snapshotCache;
  private final ResourceCache resourceCache;
  private final DuplicationCache duplicationCache;
  private final org.sonar.api.measures.MetricFinder metricFinder;

  public DuplicationPersister(MyBatis mybatis, RuleFinder ruleFinder,
    SnapshotCache snapshotCache, ResourceCache resourceCache,
    DuplicationCache duplicationCache, org.sonar.api.measures.MetricFinder metricFinder) {
    this.mybatis = mybatis;
    this.ruleFinder = ruleFinder;
    this.snapshotCache = snapshotCache;
    this.resourceCache = resourceCache;
    this.duplicationCache = duplicationCache;
    this.metricFinder = metricFinder;
  }

  @Override
  public void persist() {
    // Don't use batch insert for duplications since keeping all data in memory can produce OOM
    DbSession session = mybatis.openSession(false);
    try {
      MeasureMapper mapper = session.getMapper(MeasureMapper.class);
      org.sonar.api.measures.Metric duplicationMetricWithId = metricFinder.findByKey(CoreMetrics.DUPLICATIONS_DATA_KEY);
      for (Entry<List<DuplicationGroup>> entry : duplicationCache.entries()) {
        String effectiveKey = entry.key()[0].toString();
        Measure measure = new Measure(duplicationMetricWithId, toXml(entry.value())).setPersistenceMode(PersistenceMode.DATABASE);
        Resource resource = resourceCache.get(effectiveKey);

        if (MeasurePersister.shouldPersistMeasure(resource, measure)) {
          Snapshot snapshot = snapshotCache.get(effectiveKey);
          MeasureModel measureModel = MeasurePersister.model(measure, ruleFinder).setSnapshotId(snapshot.getId());
          mapper.insert(measureModel);
        }
      }

      session.commit();
    } catch (Exception e) {
      throw new IllegalStateException("Unable to save some measures", e);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private static String toXml(Iterable<DuplicationGroup> duplications) {
    StringBuilder xml = new StringBuilder();
    xml.append("<duplications>");
    for (DuplicationGroup duplication : duplications) {
      xml.append("<g>");
      toXml(xml, duplication.originBlock());
      for (DuplicationGroup.Block part : duplication.duplicates()) {
        toXml(xml, part);
      }
      xml.append("</g>");
    }
    xml.append("</duplications>");
    return xml.toString();
  }

  private static void toXml(StringBuilder xml, DuplicationGroup.Block part) {
    xml.append("<b s=\"").append(part.startLine())
      .append("\" l=\"").append(part.length())
      .append("\" r=\"").append(StringEscapeUtils.escapeXml(part.resourceKey()))
      .append("\"/>");
  }

}
