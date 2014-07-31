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
import org.apache.commons.lang.StringEscapeUtils;
import org.sonar.api.database.model.MeasureMapper;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.RuleMeasure;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.technicaldebt.batch.Characteristic;
import org.sonar.batch.duplication.DuplicationCache;
import org.sonar.batch.duplication.DuplicationGroup;
import org.sonar.batch.index.Cache.Entry;
import org.sonar.batch.scan.measure.MeasureCache;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.Nullable;

import java.util.ArrayList;

public final class MeasurePersister implements ScanPersister {
  private final MyBatis mybatis;
  private final RuleFinder ruleFinder;
  private final MeasureCache measureCache;
  private final SnapshotCache snapshotCache;
  private final ResourceCache resourceCache;
  private final DuplicationCache duplicationCache;
  private final org.sonar.api.measures.MetricFinder metricFinder;

  public MeasurePersister(MyBatis mybatis, RuleFinder ruleFinder,
    MeasureCache measureCache, SnapshotCache snapshotCache, ResourceCache resourceCache,
    DuplicationCache duplicationCache, org.sonar.api.measures.MetricFinder metricFinder) {
    this.mybatis = mybatis;
    this.ruleFinder = ruleFinder;
    this.measureCache = measureCache;
    this.snapshotCache = snapshotCache;
    this.resourceCache = resourceCache;
    this.duplicationCache = duplicationCache;
    this.metricFinder = metricFinder;
  }

  @Override
  public void persist() {
    DbSession session = mybatis.openSession(true);
    try {
      MeasureMapper mapper = session.getMapper(MeasureMapper.class);

      for (Entry<Measure> entry : measureCache.entries()) {
        String effectiveKey = entry.key()[0].toString();
        Measure measure = entry.value();
        Resource resource = resourceCache.get(effectiveKey);

        if (shouldPersistMeasure(resource, measure)) {
          Snapshot snapshot = snapshotCache.get(effectiveKey);
          MeasureModel measureModel = model(measure).setSnapshotId(snapshot.getId());
          mapper.insert(measureModel);
        }
      }

      org.sonar.api.measures.Metric duplicationMetricWithId = metricFinder.findByKey(CoreMetrics.DUPLICATIONS_DATA_KEY);
      for (Entry<ArrayList<DuplicationGroup>> entry : duplicationCache.entries()) {
        String effectiveKey = entry.key()[0].toString();
        Measure measure = new Measure(duplicationMetricWithId, toXml(entry.value())).setPersistenceMode(PersistenceMode.DATABASE);
        Resource resource = resourceCache.get(effectiveKey);

        if (shouldPersistMeasure(resource, measure)) {
          Snapshot snapshot = snapshotCache.get(effectiveKey);
          MeasureModel measureModel = model(measure).setSnapshotId(snapshot.getId());
          mapper.insert(measureModel);
        }
      }

      session.commit();
    } catch (Exception e) {
      throw new IllegalStateException("Unable to save some measures", e);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private static String toXml(Iterable<DuplicationGroup> duplications) {
    StringBuilder xml = new StringBuilder();
    xml.append("<duplications>");
    for (DuplicationGroup duplication : duplications) {
      xml.append("<g>");
      toXml(xml, duplication.originBlock());
      for (DuplicationGroup.Block part : duplication.duplicates()) {
        toXml(xml, part);
      }
      xml.append("</g>");
    }
    xml.append("</duplications>");
    return xml.toString();
  }

  private static void toXml(StringBuilder xml, DuplicationGroup.Block part) {
    xml.append("<b s=\"").append(part.startLine())
      .append("\" l=\"").append(part.length())
      .append("\" r=\"").append(StringEscapeUtils.escapeXml(part.resourceKey()))
      .append("\"/>");
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
