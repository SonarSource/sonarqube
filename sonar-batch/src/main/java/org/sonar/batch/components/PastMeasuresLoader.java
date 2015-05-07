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
package org.sonar.batch.components;

import com.google.common.collect.Maps;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchSide;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;

import javax.annotation.Nullable;
import javax.persistence.Query;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Can't be moved. Used by devcockpit.
 */
@BatchSide
public class PastMeasuresLoader {

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

  public List<Object[]> getPastMeasures(Resource resource, PastSnapshot projectPastSnapshot) {
    if (projectPastSnapshot != null && projectPastSnapshot.getProjectSnapshot() != null) {
      return getPastMeasures(resource.getEffectiveKey(), resource.getPath(), projectPastSnapshot.getProjectSnapshot());
    }
    return Collections.emptyList();
  }

  public List<Object[]> getPastMeasures(String resourceKey, Snapshot projectPastSnapshot) {
    return getPastMeasures(resourceKey, null, projectPastSnapshot);
  }

  public List<Object[]> getPastMeasures(String resourceKey, @Nullable String path, Snapshot projectPastSnapshot) {
    String sql = "select m.metric_id, m.characteristic_id, m.person_id, m.rule_id, m.value from project_measures m, snapshots s" +
      " where m.snapshot_id=s.id and m.metric_id in (:metricIds) " +
      "       and (s.root_snapshot_id=:rootSnapshotId or s.id=:rootSnapshotId) " +
      "       and s.status=:status and s.project_id=(select p.id from projects p where p.kee=:resourceKey and p.qualifier<>:lib"
      + (StringUtils.isNotBlank(path) ? " and p.path=:path" : "")
      + ")";
    Query q = session.createNativeQuery(sql)
      .setParameter("metricIds", metricByIds.keySet())
      .setParameter("rootSnapshotId", ObjectUtils.defaultIfNull(projectPastSnapshot.getRootId(), projectPastSnapshot.getId()))
      .setParameter("resourceKey", resourceKey)
      .setParameter("lib", Qualifiers.LIBRARY)
      .setParameter("status", Snapshot.STATUS_PROCESSED);
    if (StringUtils.isNotBlank(path)) {
      q.setParameter("path", path);
    }
    return q.getResultList();
  }

  public static int getMetricId(Object[] row) {
    // can be BigDecimal on Oracle
    return ((Number) row[0]).intValue();
  }

  public static Integer getCharacteristicId(Object[] row) {
    // can be BigDecimal on Oracle
    Number number = (Number) row[1];
    return number != null ? number.intValue() : null;
  }

  public static Integer getPersonId(Object[] row) {
    // can be BigDecimal on Oracle
    Number number = (Number) row[2];
    return number != null ? number.intValue() : null;
  }

  public static Integer getRuleId(Object[] row) {
    // can be BigDecimal on Oracle
    Number number = (Number) row[3];
    return number != null ? number.intValue() : null;
  }

  public static boolean hasValue(Object[] row) {
    return row[4] != null;
  }

  public static double getValue(Object[] row) {
    return ((Number) row[4]).doubleValue();
  }

}
