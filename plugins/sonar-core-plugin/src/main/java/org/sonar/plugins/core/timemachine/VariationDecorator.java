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
package org.sonar.plugins.core.timemachine;

import com.google.common.collect.Maps;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.ObjectUtils;
import org.sonar.api.batch.*;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.*;
import org.sonar.api.qualitymodel.Characteristic;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rules.RulePriority;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@DependedUpon(DecoratorBarriers.END_OF_TIME_MACHINE)
public class VariationDecorator implements Decorator {

  private Snapshot[] projectTargetSnapshots;
  private Map<Integer, Metric> metricByIds;
  private DatabaseSession session;

  public VariationDecorator(DatabaseSession session, PeriodLocator periodLocator, Configuration configuration, MetricFinder metricFinder) {
    this.session = session;
    Snapshot snapshot = periodLocator.locate(5);
    projectTargetSnapshots = new Snapshot[]{snapshot};
    initMetrics(metricFinder.findAll());
  }

  /**
   * only for unit tests
   */
  VariationDecorator(DatabaseSession session, Snapshot[] projectTargetSnapshots, Collection<Metric> metrics) {
    this.session = session;
    this.projectTargetSnapshots = projectTargetSnapshots;
    initMetrics(metrics);
  }

  private void initMetrics(Collection<Metric> metrics) {
    this.metricByIds = Maps.newHashMap();
    for (Metric metric : metrics) {
      if (metric.isNumericType()) {
        metricByIds.put(metric.getId(), metric);
      }
    }
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @DependsUpon
  public Collection<Metric> dependsUponMetrics() {
    return metricByIds.values();
  }

  static boolean shouldCalculateDiffValues(Resource resource) {
    // measures on files are currently purged, so past measures are not available
    return !ResourceUtils.isEntity(resource);
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (shouldCalculateDiffValues(resource)) {
      for (int index = 0; index < projectTargetSnapshots.length; index++) {
        Snapshot projectTargetSnapshot = projectTargetSnapshots[index];
        if (projectTargetSnapshot != null) {
          calculateDiffValues(resource, context, index, projectTargetSnapshot);
        }
      }
    }
  }

  private void calculateDiffValues(Resource resource, DecoratorContext context, int index, Snapshot projectTargetSnapshot) {
    List<MeasureModel> pastMeasures = selectPastMeasures(resource.getId(), projectTargetSnapshot);
    compareWithPastMeasures(context, index, pastMeasures);
  }

  void compareWithPastMeasures(DecoratorContext context, int index, List<MeasureModel> pastMeasures) {
    Map<MeasureKey, MeasureModel> pastMeasuresByKey = Maps.newHashMap();
    for (MeasureModel pastMeasure : pastMeasures) {
      pastMeasuresByKey.put(new MeasureKey(pastMeasure), pastMeasure);
    }

    // for each measure, search equivalent past measure
    for (Measure measure : context.getMeasures(MeasuresFilters.all())) {
      // compare with past measure
      MeasureModel pastMeasure = pastMeasuresByKey.get(new MeasureKey(measure));
      if (updateDiffValue(measure, pastMeasure, index)) {
        context.saveMeasure(measure);
      }
    }
  }

  boolean updateDiffValue(Measure measure, MeasureModel pastMeasure, int index) {
    boolean updated = false;
    if (pastMeasure != null && pastMeasure.getValue() != null && measure.getValue() != null) {
      double diff = (measure.getValue().doubleValue() - pastMeasure.getValue().doubleValue());
      updated = true;
      switch (index) {
        case 0:
          measure.setDiffValue1(diff);
          break;
        case 1:
          measure.setDiffValue2(diff);
          break;
        case 2:
          measure.setDiffValue3(diff);
          break;
        default:
          updated = false;
      }
    }
    return updated;
  }

  List<MeasureModel> selectPastMeasures(int resourceId, Snapshot projectTargetSnapshot) {
    // improvements : keep query in cache ? select only some columns ?
    // TODO support measure on rules and characteristics
    String hql = "select m from " + MeasureModel.class.getSimpleName() + " m, " + Snapshot.class.getSimpleName() + " s " +
        "where m.snapshotId=s.id and m.metricId in (:metricIds) and m.ruleId=null and m.rulePriority=null and m.rulesCategoryId=null and m.characteristic=null "
        + "and (s.rootId=:rootSnapshotId or s.id=:rootSnapshotId) and s.resourceId=:resourceId and s.status=:status";
    return session.createQuery(hql)
        .setParameter("metricIds", metricByIds.keySet())
        .setParameter("rootSnapshotId", ObjectUtils.defaultIfNull(projectTargetSnapshot.getRootId(), projectTargetSnapshot.getId()))
        .setParameter("resourceId", resourceId)
        .setParameter("status", Snapshot.STATUS_PROCESSED)
        .getResultList();
  }

  @Override
  public String toString() {
    return getClass().toString();
  }

  static class MeasureKey {
    Integer metricId;
    Integer ruleId;
    Integer categoryId;
    RulePriority priority;
    Characteristic characteristic;

    MeasureKey(MeasureModel model) {
      metricId = model.getMetricId();
      ruleId = model.getRuleId();
      categoryId = model.getRulesCategoryId();
      priority = model.getRulePriority();
      characteristic = model.getCharacteristic();
    }

    MeasureKey(Measure measure) {
      metricId = measure.getMetric().getId();
      characteristic = measure.getCharacteristic();
      // TODO merge RuleMeasure into Measure
      if (measure instanceof RuleMeasure) {
        RuleMeasure rm = (RuleMeasure) measure;
        categoryId = rm.getRuleCategory();
        ruleId = (rm.getRule()==null ? null : rm.getRule().getId());
        priority = rm.getRulePriority();
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      MeasureKey that = (MeasureKey) o;

      if (categoryId != null ? !categoryId.equals(that.categoryId) : that.categoryId != null) return false;
      if (characteristic != null ? !characteristic.equals(that.characteristic) : that.characteristic != null)
        return false;
      if (!metricId.equals(that.metricId)) return false;
      if (priority != that.priority) return false;
      if (ruleId != null ? !ruleId.equals(that.ruleId) : that.ruleId != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = metricId.hashCode();
      result = 31 * result + (ruleId != null ? ruleId.hashCode() : 0);
      result = 31 * result + (categoryId != null ? categoryId.hashCode() : 0);
      result = 31 * result + (priority != null ? priority.hashCode() : 0);
      result = 31 * result + (characteristic != null ? characteristic.hashCode() : 0);
      return result;
    }
  }
}
