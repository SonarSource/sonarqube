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
package org.sonar.batch.index;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.RuleMeasure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;

import java.util.Map;

public final class MeasurePersister {

  private boolean delayedMode = false;
  private SetMultimap<Integer, Measure> unsavedMeasuresBySnapshotId = HashMultimap.create();
  private DatabaseSession session;
  private ResourcePersister resourcePersister;

  public MeasurePersister(DatabaseSession session, ResourcePersister resourcePersister) {
    this.session = session;
    this.resourcePersister = resourcePersister;
  }

  public void setDelayedMode(boolean delayedMode) {
    this.delayedMode = delayedMode;
  }

  public void saveMeasure(Project project, Measure measure) {
    saveMeasure(project, project, measure);
  }

  public void saveMeasure(Project project, Resource resource, Measure measure) {
    Snapshot snapshot = resourcePersister.saveResource(project, resource);
    if (snapshot != null) {
      if (delayedMode && measure.getPersistenceMode().useMemory()) {
        unsavedMeasuresBySnapshotId.put(snapshot.getId(), measure);

      } else if (measure.getId() != null) {
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
    return measure.getPersistenceMode().useDatabase() && !(
        ResourceUtils.isEntity(resource) &&
            metric.isOptimizedBestValue() == Boolean.TRUE &&
            metric.getBestValue() != null &&
            NumberUtils.compare(metric.getBestValue(), measure.getValue()) == 0 &&
            !measure.hasOptionalData());
  }

  public void dump() {
    LoggerFactory.getLogger(getClass()).debug("{} measures to dump", unsavedMeasuresBySnapshotId.size());
    for (Map.Entry<Integer, Measure> entry : unsavedMeasuresBySnapshotId.entries()) {
      MeasureModel model = createModel(entry.getValue());
      model.setSnapshotId(entry.getKey());
      model.save(session);
    }
    session.commit();
    unsavedMeasuresBySnapshotId.clear();
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
    merge.setDiffValue1(measure.getDiffValue1());
    merge.setDiffValue2(measure.getDiffValue2());
    merge.setDiffValue3(measure.getDiffValue3());
    merge.setUrl(measure.getUrl());
    merge.setCharacteristic(measure.getCharacteristic());
    if (measure.getValue() != null) {
      merge.setValue(measure.getValue().doubleValue());
    } else {
      merge.setValue(null);
    }
    if (measure instanceof RuleMeasure) {
      RuleMeasure ruleMeasure = (RuleMeasure) measure;
      merge.setRulesCategoryId(ruleMeasure.getRuleCategory());
      merge.setRulePriority(ruleMeasure.getRulePriority());
      merge.setRule(ruleMeasure.getRule());
    }
    return merge;
  }
}
