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
import org.sonar.api.batch.*;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilters;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.RuleMeasure;
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

  private List<PastSnapshot> projectPastSnapshots;
  private PastMeasuresLoader pastMeasuresLoader;

  public VariationDecorator(PastMeasuresLoader pastMeasuresLoader, TimeMachineConfiguration configuration) {
    this(pastMeasuresLoader, configuration.getProjectPastSnapshots());
  }

  VariationDecorator(PastMeasuresLoader pastMeasuresLoader, List<PastSnapshot> projectPastSnapshots) {
    this.pastMeasuresLoader = pastMeasuresLoader;
    this.projectPastSnapshots = projectPastSnapshots;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return project.isLatestAnalysis();
  }

  @DependsUpon
  public Collection<Metric> dependsUponMetrics() {
    return pastMeasuresLoader.getMetrics();
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (shouldCalculateVariations(resource)) {
      for (PastSnapshot projectPastSnapshot : projectPastSnapshots) {
        calculateVariation(resource, context, projectPastSnapshot);
      }
    }
  }

  static boolean shouldCalculateVariations(Resource resource) {
    // measures on files are currently purged, so past measures are not available on files
    return !ResourceUtils.isEntity(resource);
  }

  private void calculateVariation(Resource resource, DecoratorContext context, PastSnapshot pastSnapshot) {
    List<MeasureModel> pastMeasures = pastMeasuresLoader.getPastMeasures(resource, pastSnapshot);
    compareWithPastMeasures(context, pastSnapshot.getIndex(), pastMeasures);
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
      if (updateVariation(measure, pastMeasure, index)) {
        context.saveMeasure(measure);
      }
    }
  }

  boolean updateVariation(Measure measure, MeasureModel pastMeasure, int index) {
    if (pastMeasure != null && pastMeasure.getValue() != null && measure.getValue() != null) {
      double variation = (measure.getValue().doubleValue() - pastMeasure.getValue().doubleValue());
      measure.setVariation(index, variation);
      return true;
    }
    return false;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  static class MeasureKey {
    Integer metricId;
    Integer ruleId;
    RulePriority priority;
    Characteristic characteristic;

    MeasureKey(MeasureModel model) {
      metricId = model.getMetricId();
      ruleId = model.getRuleId();
      priority = model.getRulePriority();
      characteristic = model.getCharacteristic();
    }

    MeasureKey(Measure measure) {
      metricId = measure.getMetric().getId();
      characteristic = measure.getCharacteristic();
      // TODO merge RuleMeasure into Measure
      if (measure instanceof RuleMeasure) {
        RuleMeasure rm = (RuleMeasure) measure;
        ruleId = (rm.getRule() == null ? null : rm.getRule().getId());
        priority = rm.getRulePriority();
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      MeasureKey that = (MeasureKey) o;
      if (characteristic != null ? !characteristic.equals(that.characteristic) : that.characteristic != null) {
        return false;
      }
      if (!metricId.equals(that.metricId)) {
        return false;
      }
      if (priority != that.priority) {
        return false;
      }
      if (ruleId != null ? !ruleId.equals(that.ruleId) : that.ruleId != null) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      int result = metricId.hashCode();
      result = 31 * result + (ruleId != null ? ruleId.hashCode() : 0);
      result = 31 * result + (priority != null ? priority.hashCode() : 0);
      result = 31 * result + (characteristic != null ? characteristic.hashCode() : 0);
      return result;
    }
  }
}