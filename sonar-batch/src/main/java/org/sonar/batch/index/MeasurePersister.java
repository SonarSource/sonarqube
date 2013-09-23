/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.LoggerFactory;
import org.sonar.api.database.model.MeasureMapper;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.RuleMeasure;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.SonarException;
import org.sonar.core.persistence.MyBatis;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class MeasurePersister {
  private final MyBatis mybatis;
  private final ResourcePersister resourcePersister;
  private final RuleFinder ruleFinder;
  private final MemoryOptimizer memoryOptimizer;
  private final SetMultimap<Resource, Measure> unsavedMeasuresByResource = LinkedHashMultimap.create();
  private boolean delayedMode = false;

  public MeasurePersister(MyBatis mybatis, ResourcePersister resourcePersister, RuleFinder ruleFinder, MemoryOptimizer memoryOptimizer) {
    this.mybatis = mybatis;
    this.resourcePersister = resourcePersister;
    this.ruleFinder = ruleFinder;
    this.memoryOptimizer = memoryOptimizer;
  }

  public void setDelayedMode(boolean delayedMode) {
    this.delayedMode = delayedMode;
  }

  public Measure reloadMeasure(Measure measure) {
    return memoryOptimizer.reloadMeasure(measure);
  }

  public void dump() {
    LoggerFactory.getLogger(getClass()).debug("{} measures to dump", unsavedMeasuresByResource.size());

    insert(getMeasuresToSave());
  }

  public void saveMeasure(Resource resource, Measure measure) {
    if (shouldSaveLater(measure)) {
      unsavedMeasuresByResource.put(resource, measure);
      return;
    }
    MeasureModel model;
    try {
      model = insertOrUpdate(resource, measure);
    } catch (Exception e) {
      // SONAR-4066
      throw new SonarException(String.format("Unable to save measure for metric [%s] on resource [%s]", measure.getMetricKey(), resource.getKey()), e);
    }
    if (model != null) {
      memoryOptimizer.evictDataMeasure(measure, model);
    }
  }

  private MeasureModel insertOrUpdate(Resource resource, Measure measure) {
    Snapshot snapshot = resourcePersister.getSnapshotOrFail(resource);
    if (measure.getId() != null) {
      return update(measure, snapshot);
    }
    if (shouldPersistMeasure(resource, measure)) {
      MeasureModel insert = insert(measure, snapshot);
      measure.setId(insert.getId());
      return insert;
    }
    return null;
  }

  private boolean shouldSaveLater(Measure measure) {
    return delayedMode && measure.getPersistenceMode().useMemory();
  }

  @VisibleForTesting
  static boolean shouldPersistMeasure(Resource resource, Measure measure) {
    return measure.getPersistenceMode().useDatabase() &&
      !(ResourceUtils.isEntity(resource) && measure.isBestValue()) && isMeasureNotEmpty(measure);
  }

  private static boolean isMeasureNotEmpty(Measure measure){
    return measure.getValue() != null || measure.getData() != null
      || measure.getVariation1() != null
      || measure.getVariation2() != null
      || measure.getVariation3() != null
      || measure.getVariation4() != null
      || measure.getVariation5() != null;
  }

  private List<MeasureModelAndDetails> getMeasuresToSave() {
    List<MeasureModelAndDetails> measures = Lists.newArrayList();

    Map<Resource, Collection<Measure>> map = unsavedMeasuresByResource.asMap();
    for (Map.Entry<Resource, Collection<Measure>> entry : map.entrySet()) {
      Resource resource = entry.getKey();
      Snapshot snapshot = resourcePersister.getSnapshot(entry.getKey());
      for (Measure measure : entry.getValue()) {
        if (shouldPersistMeasure(resource, measure)) {
          measures.add(new MeasureModelAndDetails(model(measure).setSnapshotId(snapshot.getId()), resource.getKey(), measure.getMetricKey()));
        }
      }
    }

    unsavedMeasuresByResource.clear();
    return measures;
  }

  private MeasureModel model(Measure measure) {
    MeasureModel model = new MeasureModel();
    // we assume that the index has updated the metric
    model.setMetricId(measure.getMetric().getId());
    model.setDescription(measure.getDescription());
    model.setData(measure.getData());
    model.setAlertStatus(measure.getAlertStatus());
    model.setAlertText(measure.getAlertText());
    model.setTendency(measure.getTendency());
    model.setVariationValue1(measure.getVariation1());
    model.setVariationValue2(measure.getVariation2());
    model.setVariationValue3(measure.getVariation3());
    model.setVariationValue4(measure.getVariation4());
    model.setVariationValue5(measure.getVariation5());
    model.setUrl(measure.getUrl());
    model.setCharacteristic(measure.getCharacteristic());
    model.setPersonId(measure.getPersonId());
    if (measure.getValue() != null) {
      model.setValue(measure.getValue().doubleValue());
    } else {
      model.setValue(null);
    }
    if (measure instanceof RuleMeasure) {
      RuleMeasure ruleMeasure = (RuleMeasure) measure;
      model.setRulePriority(ruleMeasure.getSeverity());
      if (ruleMeasure.getRule() != null) {
        Rule ruleWithId = ruleFinder.findByKey(ruleMeasure.getRule().getRepositoryKey(), ruleMeasure.getRule().getKey());
        if (ruleWithId == null) {
          throw new SonarException("Can not save a measure with unknown rule " + ruleMeasure);
        }
        model.setRuleId(ruleWithId.getId());
      }
    }
    return model;
  }

  private void insert(Iterable<MeasureModelAndDetails> values) {
    SqlSession session = mybatis.openSession();
    try {
      MeasureMapper mapper = session.getMapper(MeasureMapper.class);

      for (MeasureModelAndDetails value : values) {
        try {
          mapper.insert(value.getMeasureModel());
          if (value.getMeasureModel().getMeasureData() != null) {
            mapper.insertData(value.getMeasureModel().getMeasureData());
          }
        } catch (Exception e) {
          // SONAR-4066
          throw new SonarException(String.format("Unable to save measure for metric [%s] on resource [%s]", value.getMetricKey(), value.getResourceKey()), e);
        }
      }

      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private MeasureModel insert(Measure measure, Snapshot snapshot) {
    MeasureModel value = model(measure);
    value.setSnapshotId(snapshot.getId());

    SqlSession session = mybatis.openSession();
    try {
      MeasureMapper mapper = session.getMapper(MeasureMapper.class);

      mapper.insert(value);
      if (value.getMeasureData() != null) {
        mapper.insertData(value.getMeasureData());
      }

      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }

    return value;
  }

  private MeasureModel update(Measure measure, Snapshot snapshot) {
    MeasureModel value = model(measure);
    value.setId(measure.getId());
    value.setSnapshotId(snapshot.getId());

    SqlSession session = mybatis.openSession();
    try {
      MeasureMapper mapper = session.getMapper(MeasureMapper.class);

      mapper.update(value);
      mapper.deleteData(value);
      if (value.getMeasureData() != null) {
        mapper.insertData(value.getMeasureData());
      }

      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }

    return value;
  }

  // SONAR-4066
  private static class MeasureModelAndDetails {
    private final MeasureModel measureModel;
    private final String resourceKey;
    private final String metricKey;

    public MeasureModelAndDetails(MeasureModel measureModel, String resourceKey, String metricKey) {
      this.measureModel = measureModel;
      this.resourceKey = resourceKey;
      this.metricKey = metricKey;
    }

    public MeasureModel getMeasureModel() {
      return measureModel;
    }

    public String getResourceKey() {
      return resourceKey;
    }

    public String getMetricKey() {
      return metricKey;
    }
  }
}
