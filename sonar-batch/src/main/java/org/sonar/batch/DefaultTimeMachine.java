/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.batch;

import org.sonar.api.batch.TimeMachine;
import org.sonar.api.batch.TimeMachineQuery;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Resource;
import org.sonar.batch.indexer.DefaultSonarIndex;
import org.sonar.jpa.dao.MeasuresDao;

import java.util.*;
import javax.persistence.Query;

public class DefaultTimeMachine implements TimeMachine {

  private DatabaseSession session;
  private DefaultSonarIndex index;
  private MeasuresDao measuresDao;

  public DefaultTimeMachine(DatabaseSession session, DefaultSonarIndex index, MeasuresDao measuresDao) {
    this.session = session;
    this.index = index;
    this.measuresDao = measuresDao;
  }

  public List<Measure> getMeasures(TimeMachineQuery query) {
    List<Object[]> objects = execute(query, true);
    List<Measure> result = new ArrayList<Measure>();

    for (Object[] object : objects) {
      MeasureModel model = (MeasureModel) object[0];
      Measure measure = model.toMeasure();
      measure.setDate((Date) object[1]);
      result.add(measure);
    }
    return result;
  }

  public List<Object[]> getMeasuresFields(TimeMachineQuery query) {
    return execute(query, false);
  }

  protected List execute(TimeMachineQuery query, boolean selectAllFields) {
    Resource resource = index.getResource(query.getResource());
    if (resource == null) {
      return Collections.emptyList();
    }

    StringBuilder sb = new StringBuilder();
    Map<String, Object> params = new HashMap<String, Object>();

    if (selectAllFields) {
      sb.append("SELECT m, s.createdAt ");
    } else {
      sb.append("SELECT s.createdAt, m.metric, m.value ");
    }
    sb.append(" FROM " + MeasureModel.class.getSimpleName() + " m, " + Snapshot.class.getSimpleName() + " s WHERE m.snapshotId=s.id AND s.resourceId=:resourceId AND s.status=:status AND m.characteristic IS NULL ");
    params.put("resourceId", resource.getId());
    params.put("status", Snapshot.STATUS_PROCESSED);

    sb.append(" AND m.rule IS NULL AND m.rulePriority IS NULL AND m.rulesCategoryId IS NULL ");
    if (query.getMetrics() != null) {
      sb.append(" AND m.metric IN (:metrics) ");
      params.put("metrics", measuresDao.getMetrics(query.getMetrics()));
    }
    if (query.isFromCurrentAnalysis()) {
      sb.append(" AND s.createdAt>=:from ");
      params.put("from", index.getProject().getAnalysisDate());

    } else if (query.getFrom() != null) {
      sb.append(" AND s.createdAt>=:from ");
      params.put("from", query.getFrom());
    }
    if (query.isToCurrentAnalysis()) {
      sb.append(" AND s.createdAt<=:to ");
      params.put("to", index.getProject().getAnalysisDate());

    } else if (query.getTo() != null) {
      sb.append(" AND s.createdAt<=:to ");
      params.put("to", query.getTo());
    }
    if (query.isOnlyLastAnalysis()) {
      sb.append(" AND s.last=:last ");
      params.put("last", Boolean.TRUE);
    }
    sb.append(" ORDER BY s.createdAt ");

    Query jpaQuery = session.createQuery(sb.toString());

    for (Map.Entry<String, Object> entry : params.entrySet()) {
      jpaQuery.setParameter(entry.getKey(), entry.getValue());
    }
    return jpaQuery.getResultList();
  }
}
