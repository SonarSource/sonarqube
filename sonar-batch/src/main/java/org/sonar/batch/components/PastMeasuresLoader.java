/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.batch.components;

import com.google.common.collect.Maps;
import org.apache.commons.lang.ObjectUtils;
import org.sonar.api.BatchExtension;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.resources.Resource;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PastMeasuresLoader implements BatchExtension {

  private Map<Integer, Metric> metricByIds;
  private DatabaseSession session;

  public PastMeasuresLoader(DatabaseSession session, MetricFinder metricFinder) {
    this(session, metricFinder.findAll());
  }

  PastMeasuresLoader(DatabaseSession session, Collection<Metric> metrics) {
    this.session = session;
    this.metricByIds = Maps.newHashMap();
    for (Metric metric : metrics) {
      if (metric.isNumericType()) {
        metricByIds.put(metric.getId(), metric);
      }
    }
  }

  public Collection<Metric> getMetrics() {
    return metricByIds.values();
  }

  public List<MeasureModel> getPastMeasures(Resource resource, PastSnapshot projectPastSnapshot) {
    return getPastMeasures(resource, projectPastSnapshot.getProjectSnapshot());
  }

  public List<MeasureModel> getPastMeasures(Resource resource, Snapshot projectSnapshot) {
    if (isPersisted(resource)) {
      return getPastMeasures(resource.getId(), projectSnapshot);
    }
    return Collections.emptyList();
  }

  public List<MeasureModel> getPastMeasures(int resourceId, Snapshot projectSnapshot) {
    // TODO improvement : select only some columns
    // TODO support measure on characteristics
    String hql = "select m from " + MeasureModel.class.getSimpleName() + " m, " + Snapshot.class.getSimpleName() + " s " +
        "where m.snapshotId=s.id and m.metricId in (:metricIds) and m.ruleId=null and m.rulePriority=null and m.characteristic=null "
        + "and (s.rootId=:rootSnapshotId or s.id=:rootSnapshotId) and s.resourceId=:resourceId and s.status=:status";
    return session.createQuery(hql)
        .setParameter("metricIds", metricByIds.keySet())
        .setParameter("rootSnapshotId", ObjectUtils.defaultIfNull(projectSnapshot.getRootId(), projectSnapshot.getId()))
        .setParameter("resourceId", resourceId)
        .setParameter("status", Snapshot.STATUS_PROCESSED)
        .getResultList();
  }

  private boolean isPersisted(Resource resource) {
    return resource.getId()!=null;
  }
}
