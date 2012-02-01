/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.*;
import org.sonar.api.measures.*;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;
import org.sonar.batch.components.PastMeasuresLoader;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.components.TimeMachineConfiguration;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@DependedUpon(DecoratorBarriers.END_OF_TIME_MACHINE)
public class VariationDecorator implements Decorator {

  private List<PastSnapshot> projectPastSnapshots;
  private MetricFinder metricFinder;
  private PastMeasuresLoader pastMeasuresLoader;
  private final boolean enabledFileVariation;


  public VariationDecorator(PastMeasuresLoader pastMeasuresLoader, MetricFinder metricFinder, TimeMachineConfiguration configuration) {
    this(pastMeasuresLoader, metricFinder, configuration.getProjectPastSnapshots(), configuration.isFileVariationEnabled());
  }

  VariationDecorator(PastMeasuresLoader pastMeasuresLoader, MetricFinder metricFinder, List<PastSnapshot> projectPastSnapshots, boolean enabledFileVariation) {
    this.pastMeasuresLoader = pastMeasuresLoader;
    this.projectPastSnapshots = projectPastSnapshots;
    this.metricFinder = metricFinder;
    this.enabledFileVariation = enabledFileVariation;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return project.isLatestAnalysis();
  }

  @DependsUpon
  public Collection<Metric> dependsUponMetrics() {
    return pastMeasuresLoader.getMetrics();
  }

  public void decorate(Resource resource, DecoratorContext context) {
    for (PastSnapshot projectPastSnapshot : projectPastSnapshots) {
      if (shouldComputeVariation(projectPastSnapshot.getMode(), resource)) {
        computeVariation(resource, context, projectPastSnapshot);
      }
    }
  }

  boolean shouldComputeVariation(String variationMode, Resource resource) {
    if (Scopes.FILE.equals(resource.getScope()) && !Qualifiers.UNIT_TEST_FILE.equals(resource.getQualifier())) {
      return enabledFileVariation && StringUtils.equals(variationMode, CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS);
    }

    // measures on files are currently purged, so past measures are not available on files
    return StringUtils.equals(Scopes.PROJECT, resource.getScope()) || StringUtils.equals(Scopes.DIRECTORY, resource.getScope());
  }

  private void computeVariation(Resource resource, DecoratorContext context, PastSnapshot pastSnapshot) {
    List<Object[]> pastMeasures = pastMeasuresLoader.getPastMeasures(resource, pastSnapshot);
    compareWithPastMeasures(context, pastSnapshot.getIndex(), pastMeasures);
  }

  void compareWithPastMeasures(DecoratorContext context, int index, List<Object[]> pastMeasures) {
    Map<MeasureKey, Object[]> pastMeasuresByKey = Maps.newHashMap();
    for (Object[] pastMeasure : pastMeasures) {
      pastMeasuresByKey.put(new MeasureKey(pastMeasure), pastMeasure);
    }

    // for each measure, search equivalent past measure
    for (Measure measure : context.getMeasures(MeasuresFilters.all())) {
      // compare with past measure
      Integer metricId = (measure.getMetric().getId() != null ? measure.getMetric().getId() : metricFinder.findByKey(measure.getMetric().getKey()).getId());
      Integer characteristicId = (measure.getCharacteristic() != null ? measure.getCharacteristic().getId() : null);
      String committer = measure.getCommitter();
      Integer ruleId =  (measure instanceof RuleMeasure ? ((RuleMeasure)measure).getRule().getId() : null);

      Object[] pastMeasure = pastMeasuresByKey.get(new MeasureKey(metricId, characteristicId, committer, ruleId));
      if (updateVariation(measure, pastMeasure, index)) {
        context.saveMeasure(measure);
      }
    }
  }

  boolean updateVariation(Measure measure, Object[] pastMeasure, int index) {
    if (pastMeasure != null && PastMeasuresLoader.hasValue(pastMeasure) && measure.getValue() != null) {
      double variation = (measure.getValue().doubleValue() - PastMeasuresLoader.getValue(pastMeasure));
      measure.setVariation(index, variation);
      return true;
    }
    return false;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  static final class MeasureKey {
    int metricId;
    Integer characteristicId;
    String committer;
    Integer ruleId;

    MeasureKey(Object[] pastFields) {
      metricId = PastMeasuresLoader.getMetricId(pastFields);
      characteristicId = PastMeasuresLoader.getCharacteristicId(pastFields);
      committer = PastMeasuresLoader.getCommitter(pastFields);
      ruleId = PastMeasuresLoader.getRuleId(pastFields);
    }

    MeasureKey(int metricId, Integer characteristicId, String committer, Integer ruleId) {
      this.metricId = metricId;
      this.characteristicId = characteristicId;
      this.committer = committer;
      this.ruleId = ruleId;
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
      if (metricId != that.metricId) {
        return false;
      }
      if (characteristicId != null ? !characteristicId.equals(that.characteristicId) : that.characteristicId != null) {
        return false;
      }
      if (committer != null ? !committer.equals(that.committer) : that.committer != null) {
        return false;
      }
      if (ruleId != null ? !ruleId.equals(that.ruleId) : that.ruleId != null) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      int result = metricId;
      result = 31 * result + (characteristicId != null ? characteristicId.hashCode() : 0);
      result = 31 * result + (committer != null ? committer.hashCode() : 0);
      result = 31 * result + (ruleId != null ? ruleId.hashCode() : 0);
      return result;
    }
  }
}
