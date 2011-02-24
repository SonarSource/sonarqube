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
package org.sonar.batch.index;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.RuleMeasure;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.SonarException;

import java.util.Collection;
import java.util.Map;

public final class MeasurePersister {

  private boolean delayedMode = false;
  private SetMultimap<Resource, Measure> unsavedMeasuresByResource = LinkedHashMultimap.create();
  private DatabaseSession session;
  private ResourcePersister resourcePersister;
  private RuleFinder ruleFinder;

  public MeasurePersister(DatabaseSession session, ResourcePersister resourcePersister, RuleFinder ruleFinder) {
    this.session = session;
    this.resourcePersister = resourcePersister;
    this.ruleFinder = ruleFinder;
  }

  public void setDelayedMode(boolean delayedMode) {
    this.delayedMode = delayedMode;
  }

  public void saveMeasure(Resource resource, Measure measure) {
    boolean saveLater = (measure.getPersistenceMode().useMemory() && delayedMode);
    if (saveLater) {
      unsavedMeasuresByResource.put(resource, measure);

    } else {
      Snapshot snapshot = resourcePersister.getSnapshotOrFail(resource);
      if (measure.getId() != null) {
        // update
        MeasureModel model = session.reattach(MeasureModel.class, measure.getId());
        model = mergeModel(measure, model);
        model.save(session);

      } else if (shouldPersistMeasure(resource, measure)) {
        // insert
        MeasureModel model = createModel(measure);
        model.setSnapshotId(snapshot.getId());
        model.save(session);
        measure.setId(model.getId()); // could be removed
      }
    }
  }

  static boolean shouldPersistMeasure(Resource resource, Measure measure) {
    Metric metric = measure.getMetric();
    return measure.getPersistenceMode().useDatabase() &&
        !(ResourceUtils.isEntity(resource) && isBestValueMeasure(measure, metric));
  }

  static boolean isBestValueMeasure(Measure measure, Metric metric) {
    return measure.getId() == null &&
        metric.isOptimizedBestValue() == Boolean.TRUE &&
        metric.getBestValue() != null &&
        (measure.getValue() == null || NumberUtils.compare(metric.getBestValue(), measure.getValue()) == 0) &&
        measure.getAlertStatus() == null &&
        measure.getDescription() == null &&
        measure.getTendency() == null &&
        measure.getUrl() == null &&
        !measure.hasData() &&
        (measure.getVariation1() == null || NumberUtils.compare(measure.getVariation1().doubleValue(), 0.0) == 0) &&
        (measure.getVariation2() == null || NumberUtils.compare(measure.getVariation2().doubleValue(), 0.0) == 0) &&
        (measure.getVariation3() == null || NumberUtils.compare(measure.getVariation3().doubleValue(), 0.0) == 0) &&
        (measure.getVariation4() == null || NumberUtils.compare(measure.getVariation4().doubleValue(), 0.0) == 0) &&
        (measure.getVariation5() == null || NumberUtils.compare(measure.getVariation5().doubleValue(), 0.0) == 0);
  }

  public void dump() {
    LoggerFactory.getLogger(getClass()).debug("{} measures to dump", unsavedMeasuresByResource.size());
    Map<Resource, Collection<Measure>> map = unsavedMeasuresByResource.asMap();
    for (Map.Entry<Resource, Collection<Measure>> entry : map.entrySet()) {
      Resource resource = entry.getKey();
      Snapshot snapshot = resourcePersister.getSnapshot(entry.getKey());
      for (Measure measure : entry.getValue()) {
        if (shouldPersistMeasure(resource, measure)) {
          MeasureModel model = createModel(measure);
          model.setSnapshotId(snapshot.getId());
          model.save(session);
        }
      }
    }

    session.commit();
    unsavedMeasuresByResource.clear();
  }

  MeasureModel createModel(Measure measure) {
    return mergeModel(measure, new MeasureModel());
  }

  MeasureModel mergeModel(Measure measure, MeasureModel merge) {
    merge.setMetricId(measure.getMetric().getId());// we assume that the index has updated the metric
    merge.setDescription(measure.getDescription());
    merge.setData(measure.getData());
    merge.setAlertStatus(measure.getAlertStatus());
    merge.setAlertText(measure.getAlertText());
    merge.setTendency(measure.getTendency());
    merge.setVariationValue1(measure.getVariation1());
    merge.setVariationValue2(measure.getVariation2());
    merge.setVariationValue3(measure.getVariation3());
    merge.setVariationValue4(measure.getVariation4());
    merge.setVariationValue5(measure.getVariation5());
    merge.setUrl(measure.getUrl());
    merge.setCharacteristic(measure.getCharacteristic());
    if (measure.getValue() != null) {
      merge.setValue(measure.getValue().doubleValue());
    } else {
      merge.setValue(null);
    }
    if (measure instanceof RuleMeasure) {
      RuleMeasure ruleMeasure = (RuleMeasure) measure;
      merge.setRulePriority(ruleMeasure.getRulePriority());
      if (ruleMeasure.getRule() != null) {
        Rule ruleWithId = ruleFinder.findByKey(ruleMeasure.getRule().getRepositoryKey(), ruleMeasure.getRule().getKey());
        if (ruleWithId != null) {
          merge.setRuleId(ruleWithId.getId());
        } else {
          throw new SonarException("Can not save a measure with unknown rule " + ruleMeasure);
        }
      }
    }
    return merge;
  }
}
