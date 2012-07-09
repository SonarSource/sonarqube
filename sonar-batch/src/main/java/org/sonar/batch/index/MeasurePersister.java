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
package org.sonar.batch.index;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.LoggerFactory;
import org.sonar.api.database.model.MeasureDataDto;
import org.sonar.api.database.model.MeasureDto;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.database.model.MeasureModelMapper;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
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

import static com.google.common.collect.Iterables.filter;

import static com.google.common.base.Predicates.not;

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

    List<MeasureDto> measuresToSave = getMeasuresToSave();
    insert(filter(measuresToSave, HAS_LARGE_DATA));
    batchInsert(filter(measuresToSave, not(HAS_LARGE_DATA)));
  }

  public void saveMeasure(Resource resource, Measure measure) {
    if (shouldSaveLater(measure)) {
      unsavedMeasuresByResource.put(resource, measure);
      return;
    }

    MeasureModel model = insertOrUpdate(resource, measure);
    if (model != null) {
      memoryOptimizer.evictDataMeasure(measure, model);
    }
  }

  private MeasureModel insertOrUpdate(Resource resource, Measure measure) {
    if (measure.getId() != null) {
      return update(measure);
    }
    if (shouldPersistMeasure(resource, measure)) {
      return insert(measure, resourcePersister.getSnapshotOrFail(resource));
    }
    return null;
  }

  private boolean shouldSaveLater(Measure measure) {
    return delayedMode && measure.getPersistenceMode().useMemory();
  }

  @VisibleForTesting
  static boolean shouldPersistMeasure(Resource resource, Measure measure) {
    return measure.getPersistenceMode().useDatabase() &&
      !(ResourceUtils.isEntity(resource) && isBestValueMeasure(measure, measure.getMetric()));
  }

  @VisibleForTesting
  static boolean isBestValueMeasure(Measure measure, Metric metric) {
    return measure.getId() == null
      && metric.isOptimizedBestValue() == Boolean.TRUE
      && metric.getBestValue() != null
      && (measure.getValue() == null || NumberUtils.compare(metric.getBestValue(), measure.getValue()) == 0)
      && measure.getAlertStatus() == null
      && measure.getDescription() == null
      && measure.getTendency() == null
      && measure.getUrl() == null
      && !measure.hasData()
      && (measure.getVariation1() == null || NumberUtils.compare(measure.getVariation1().doubleValue(), 0.0) == 0)
      && (measure.getVariation2() == null || NumberUtils.compare(measure.getVariation2().doubleValue(), 0.0) == 0)
      && (measure.getVariation3() == null || NumberUtils.compare(measure.getVariation3().doubleValue(), 0.0) == 0)
      && (measure.getVariation4() == null || NumberUtils.compare(measure.getVariation4().doubleValue(), 0.0) == 0)
      && (measure.getVariation5() == null || NumberUtils.compare(measure.getVariation5().doubleValue(), 0.0) == 0);
  }

  private List<MeasureDto> getMeasuresToSave() {
    List<MeasureDto> batch = Lists.newArrayList();

    Map<Resource, Collection<Measure>> map = unsavedMeasuresByResource.asMap();
    for (Map.Entry<Resource, Collection<Measure>> entry : map.entrySet()) {
      Resource resource = entry.getKey();
      Snapshot snapshot = resourcePersister.getSnapshot(entry.getKey());
      for (Measure measure : entry.getValue()) {
        if (shouldPersistMeasure(resource, measure)) {
          batch.add(new MeasureDto(model(measure).setSnapshotId(snapshot.getId())));
        }
      }
    }

    unsavedMeasuresByResource.clear();
    return batch;
  }

  private MeasureModel model(Measure measure) {
    MeasureModel model = new MeasureModel();
    model.setMetricId(measure.getMetric().getId()); // we assume that the index has updated the metric
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

  private void batchInsert(Iterable<MeasureDto> values) {
    SqlSession session = mybatis.openBatchSession();
    try {
      MeasureModelMapper mapper = session.getMapper(MeasureModelMapper.class);
      for (MeasureDto value : values) {
        mapper.insert(value);
      }
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void insert(Iterable<MeasureDto> values) {
    SqlSession session = mybatis.openSession();
    try {
      MeasureModelMapper mapper = session.getMapper(MeasureModelMapper.class);
      for (MeasureDto value : values) {
        mapper.insert(value);
        mapper.insertData(new MeasureDataDto(value.getId(), value.getSnapshotId(), value.getMeasureData().getData()));
      }
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private MeasureModel insert(Measure measure, Snapshot snapshot) {
    MeasureModel model = model(measure).setSnapshotId(snapshot.getId());

    SqlSession session = mybatis.openSession();
    try {
      MeasureModelMapper mapper = session.getMapper(MeasureModelMapper.class);
      MeasureDto value = new MeasureDto(model);
      mapper.insert(value);
      if (value.getMeasureData() != null) {
        mapper.insertData(new MeasureDataDto(value.getId(), value.getSnapshotId(), value.getMeasureData().getData()));
      }
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }

    return model;
  }

  private MeasureModel update(Measure measure) {
    MeasureModel model = model(measure);
    model.setId(measure.getId());

    SqlSession session = mybatis.openSession();
    try {
      MeasureModelMapper mapper = session.getMapper(MeasureModelMapper.class);
      mapper.update(new MeasureDto(model));
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }

    return model;
  }

  private static final Predicate<MeasureDto> HAS_LARGE_DATA = new Predicate<MeasureDto>() {
    public boolean apply(@Nullable MeasureDto measure) {
      return (null != measure) && (measure.getMeasureData() != null);
    }
  };
}
