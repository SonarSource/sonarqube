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
package org.sonar.batch.index;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.database.model.MeasureMapper;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.measures.RuleMeasure;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.technicaldebt.batch.Characteristic;
import org.sonar.batch.index.Cache.Entry;
import org.sonar.batch.scan.measure.MeasureCache;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.Nullable;

public class MeasurePersister implements ScanPersister {
  private final MyBatis mybatis;
  private final RuleFinder ruleFinder;
  private final MeasureCache measureCache;
  private final ResourceCache resourceCache;
  private final MetricFinder metricFinder;

  public MeasurePersister(MyBatis mybatis, RuleFinder ruleFinder, MetricFinder metricFinder,
    MeasureCache measureCache, ResourceCache resourceCache) {
    this.mybatis = mybatis;
    this.ruleFinder = ruleFinder;
    this.metricFinder = metricFinder;
    this.measureCache = measureCache;
    this.resourceCache = resourceCache;
  }

  @Override
  public void persist() {
    try (DbSession session = mybatis.openSession(true)) {
      MeasureMapper mapper = session.getMapper(MeasureMapper.class);

      for (Entry<Measure> entry : measureCache.entries()) {
        String effectiveKey = entry.key()[0].toString();
        Measure measure = entry.value();
        BatchResource batchResource = resourceCache.get(effectiveKey);

        // Reload Metric to have all Hibernate fields populated
        measure.setMetric(metricFinder.findByKey(measure.getMetricKey()));

        if (shouldPersistMeasure(batchResource.resource(), measure)) {
          MeasureModel measureModel = model(measure, ruleFinder).setSnapshotId(batchResource.snapshotId());
          mapper.insert(measureModel);
        }
      }

      session.commit();
    } catch (Exception e) {
      throw new IllegalStateException("Unable to save some measures", e);
    }
  }

  @VisibleForTesting
  static boolean shouldPersistMeasure(@Nullable Resource resource, @Nullable Measure measure) {
    if (resource == null || measure == null) {
      return false;
    }
    return measure.getPersistenceMode().useDatabase() &&
      !(ResourceUtils.isEntity(resource) && measure.isBestValue()) && isMeasureNotEmpty(measure);
  }

  private static boolean isMeasureNotEmpty(Measure measure) {
    boolean isNotEmpty = false;
    for (int i = 1; i <= 5; i++) {
      isNotEmpty = isNotEmpty || measure.getVariation(i) != null;
    }
    return measure.getValue() != null || measure.getData() != null
      || isNotEmpty;
  }

  static MeasureModel model(Measure measure, RuleFinder ruleFinder) {
    MeasureModel model = new MeasureModel();
    // Assume Metric was reloaded
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
    Characteristic characteristic = measure.getCharacteristic();
    if (characteristic != null) {
      model.setCharacteristicId(characteristic.id());
    }
    model.setPersonId(measure.getPersonId());
    model.setValue(measure.getValue());
    if (measure instanceof RuleMeasure) {
      RuleMeasure ruleMeasure = (RuleMeasure) measure;
      model.setRulePriority(ruleMeasure.getSeverity());
      RuleKey ruleKey = ruleMeasure.ruleKey();
      if (ruleKey != null) {
        Rule ruleWithId = ruleFinder.findByKey(ruleKey);
        if (ruleWithId == null) {
          throw new IllegalStateException("Can not save a measure with unknown rule " + ruleMeasure);
        }
        model.setRuleId(ruleWithId.getId());
      }
    }
    return model;
  }
}
