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
package org.sonar.batch.report;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.sonar.api.measures.*;
import org.sonar.api.measures.Metric.Level;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.technicaldebt.batch.Characteristic;
import org.sonar.batch.index.BatchResource;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.Constants.MeasureValueType;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.batch.scan.measure.MeasureCache;

import javax.annotation.Nullable;

import java.io.Serializable;

public class MeasuresPublisher implements ReportPublisherStep {

  private final ResourceCache resourceCache;
  private final MeasureCache measureCache;
  private final MetricFinder metricFinder;

  public MeasuresPublisher(ResourceCache resourceCache, MeasureCache measureCache, MetricFinder metricFinder) {
    this.resourceCache = resourceCache;
    this.measureCache = measureCache;
    this.metricFinder = metricFinder;
  }

  @Override
  public void publish(BatchReportWriter writer) {
    for (final BatchResource resource : resourceCache.all()) {
      Iterable<Measure> batchMeasures = measureCache.byResource(resource.resource());
      batchMeasures = Iterables.filter(batchMeasures, new Predicate<Measure>() {
        @Override
        public boolean apply(Measure input) {
          // Reload Metric to have all Hibernate fields populated
          input.setMetric(metricFinder.findByKey(input.getMetricKey()));
          return shouldPersistMeasure(resource.resource(), input);
        }

      });
      Iterable<org.sonar.batch.protocol.output.BatchReport.Measure> reportMeasures = Iterables.transform(batchMeasures, new Function<Measure, BatchReport.Measure>() {
        private final BatchReport.Measure.Builder builder = BatchReport.Measure.newBuilder();

        @Override
        public BatchReport.Measure apply(Measure input) {
          return toReportMeasure(builder, input);
        }
      });
      writer.writeComponentMeasures(resource.batchId(), reportMeasures);
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
    return measure.getValue() != null || measure.getData() != null || isNotEmpty;
  }

  private BatchReport.Measure toReportMeasure(BatchReport.Measure.Builder builder, Measure measure) {
    builder.clear();

    builder.setValueType(getMeasureValueType(measure.getMetric().getType()));
    setValueAccordingToType(builder, measure);
    // Because some numeric measures also have a data (like Sqale rating)
    String data = measure.getData();
    if (data != null) {
      builder.setStringValue(data);
    }
    builder.setMetricKey(measure.getMetricKey());

    // temporary fields during development of computation stack
    String description = measure.getDescription();
    if (description != null) {
      builder.setDescription(description);
    }
    if (measure instanceof RuleMeasure) {
      RuleMeasure ruleMeasure = (RuleMeasure) measure;
      RuleKey ruleKey = ruleMeasure.ruleKey();
      if (ruleKey != null) {
        builder.setRuleKey(ruleKey.toString());
      }
      RulePriority severity = ruleMeasure.getSeverity();
      if (severity != null) {
        builder.setSeverity(Constants.Severity.valueOf(severity.toString()));
      }
    }
    Level alertStatus = measure.getAlertStatus();
    if (alertStatus != null) {
      builder.setAlertStatus(alertStatus.toString());
    }
    String alertText = measure.getAlertText();
    if (alertText != null) {
      builder.setAlertText(alertText);
    }
    Double variation1 = measure.getVariation1();
    if (variation1 != null) {
      builder.setVariationValue1(variation1);
    }
    Double variation2 = measure.getVariation2();
    if (variation2 != null) {
      builder.setVariationValue2(variation2);
    }
    Double variation3 = measure.getVariation3();
    if (variation3 != null) {
      builder.setVariationValue3(variation3);
    }
    Double variation4 = measure.getVariation4();
    if (variation4 != null) {
      builder.setVariationValue4(variation4);
    }
    Double variation5 = measure.getVariation5();
    if (variation5 != null) {
      builder.setVariationValue5(variation5);
    }
    Characteristic charac = measure.getCharacteristic();
    if (charac != null) {
      builder.setCharactericId(charac.id());
    }
    Integer personId = measure.getPersonId();
    if (personId != null) {
      builder.setPersonId(personId);
    }
    return builder.build();
  }

  private void setValueAccordingToType(BatchReport.Measure.Builder builder, Measure measure) {
    Serializable value = measure.value();
    // Value is null for new_xxx measures where only variations are populated
    if (value != null) {
      switch (builder.getValueType()) {
        case BOOLEAN:
          builder.setBooleanValue((Boolean) value);
          break;
        case DOUBLE:
          builder.setDoubleValue((Double) value);
          break;
        case INT:
          builder.setIntValue((Integer) value);
          break;
        case LONG:
          builder.setLongValue((Long) value);
          break;
        case STRING:
          builder.setStringValue((String) value);
          break;
        default:
          throw new IllegalStateException("Unknown value type: " + builder.getValueType());
      }
    }
  }

  private MeasureValueType getMeasureValueType(ValueType type) {
    switch (type) {
      case INT:
      case MILLISEC:
      case RATING:
        return MeasureValueType.INT;
      case FLOAT:
      case PERCENT:
        return MeasureValueType.DOUBLE;
      case BOOL:
        return MeasureValueType.BOOLEAN;
      case STRING:
      case DATA:
      case LEVEL:
      case DISTRIB:
        return MeasureValueType.STRING;
      case WORK_DUR:
        return MeasureValueType.LONG;
      default:
        throw new IllegalStateException("Unknown value type: " + type);
    }
  }

}
