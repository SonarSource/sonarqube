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
package org.sonar.batch.deprecated.components;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.sonar.api.batch.TimeMachine;
import org.sonar.api.batch.TimeMachineQuery;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.technicaldebt.batch.Characteristic;
import org.sonar.api.technicaldebt.batch.TechnicalDebtModel;
import org.sonar.batch.index.DefaultIndex;

import javax.annotation.Nullable;
import javax.persistence.Query;
import java.util.*;

import static org.sonar.api.utils.DateUtils.dateToLong;

public class DefaultTimeMachine implements TimeMachine {

  private DatabaseSession session;
  private DefaultIndex index;
  private MetricFinder metricFinder;
  private TechnicalDebtModel techDebtModel;

  public DefaultTimeMachine(DatabaseSession session, DefaultIndex index, MetricFinder metricFinder, TechnicalDebtModel techDebtModel) {
    this.session = session;
    this.index = index;
    this.metricFinder = metricFinder;
    this.techDebtModel = techDebtModel;
  }

  @Override
  public List<Measure> getMeasures(TimeMachineQuery query) {
    Map<Integer, Metric> metricById = getMetricsById(query);

    List<Object[]> objects = execute(query, metricById.keySet());
    List<Measure> result = Lists.newArrayList();

    for (Object[] object : objects) {
      MeasureModel model = (MeasureModel) object[0];
      Integer characteristicId = model.getCharacteristicId();
      Characteristic characteristic = techDebtModel.characteristicById(characteristicId);
      Measure measure = toMeasure(model, metricById.get(model.getMetricId()), characteristic);
      measure.setDate(new Date((Long) object[1]));
      result.add(measure);
    }
    return result;
  }

  @Override
  public List<Object[]> getMeasuresFields(TimeMachineQuery query) {
    return Collections.emptyList();
  }

  protected List<Object[]> execute(TimeMachineQuery query, Set<Integer> metricIds) {
    Resource resource = query.getResource();
    if (resource != null && resource.getId() == null) {
      resource = index.getResource(query.getResource());
    }
    if (resource == null) {
      return Collections.emptyList();
    }

    StringBuilder sb = new StringBuilder();
    Map<String, Object> params = Maps.newHashMap();

    sb.append("SELECT m, s.createdAt ");
    sb.append(" FROM ")
      .append(MeasureModel.class.getSimpleName())
      .append(" m, ")
      .append(Snapshot.class.getSimpleName())
      .append(" s WHERE m.snapshotId=s.id AND s.resourceId=:resourceId AND s.status=:status AND s.qualifier<>:lib");
    params.put("resourceId", resource.getId());
    params.put("status", Snapshot.STATUS_PROCESSED);
    params.put("lib", Qualifiers.LIBRARY);

    sb.append(" AND m.characteristicId IS NULL");
    sb.append(" AND m.personId IS NULL");
    sb.append(" AND m.ruleId IS NULL AND m.rulePriority IS NULL");
    if (!metricIds.isEmpty()) {
      sb.append(" AND m.metricId IN (:metricIds) ");
      params.put("metricIds", metricIds);
    }
    if (query.isFromCurrentAnalysis()) {
      sb.append(" AND s.createdAt>=:from ");
      params.put("from", index.getProject().getAnalysisDate());

    } else if (query.getFrom() != null) {
      sb.append(" AND s.createdAt>=:from ");
      params.put("from", dateToLong(query.getFrom()));
    }
    if (query.isToCurrentAnalysis()) {
      sb.append(" AND s.createdAt<=:to ");
      params.put("to", dateToLong(index.getProject().getAnalysisDate()));

    } else if (query.getTo() != null) {
      sb.append(" AND s.createdAt<=:to ");
      params.put("to", dateToLong(query.getTo()));
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

  public Map<Integer, Metric> getMetricsById(TimeMachineQuery query) {
    Collection<Metric> metrics = metricFinder.findAll(query.getMetricKeys());
    Map<Integer, Metric> result = Maps.newHashMap();
    for (Metric metric : metrics) {
      result.put(metric.getId(), metric);
    }
    return result;
  }

  static Measure toMeasure(MeasureModel model, Metric metric, @Nullable Characteristic characteristic) {
    // NOTE: measures on rule are not supported
    Measure measure = new Measure(metric);
    measure.setDescription(model.getDescription());
    measure.setValue(model.getValue());
    measure.setData(model.getData(metric));
    measure.setAlertStatus(model.getAlertStatus());
    measure.setAlertText(model.getAlertText());
    measure.setVariation1(model.getVariationValue1());
    measure.setVariation2(model.getVariationValue2());
    measure.setVariation3(model.getVariationValue3());
    measure.setVariation4(model.getVariationValue4());
    measure.setVariation5(model.getVariationValue5());
    measure.setCharacteristic(characteristic);
    measure.setPersonId(model.getPersonId());
    return measure;
  }
}
