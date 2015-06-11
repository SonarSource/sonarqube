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
package org.sonar.server.computation.measure;

import com.google.common.base.Optional;
import java.util.Objects;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.computation.issue.RuleCache;
import org.sonar.server.computation.metric.Metric;

public class BatchMeasureToMeasure {
  private final RuleCache ruleCache;

  public BatchMeasureToMeasure(RuleCache ruleCache) {
    this.ruleCache = ruleCache;
  }

  public Optional<Measure> toMeasure(@Nullable BatchReport.Measure batchMeasure, Metric metric) {
    Objects.requireNonNull(metric);
    if (batchMeasure == null) {
      return Optional.absent();
    }

    MeasureImpl.Builder builder = createBuilder(batchMeasure);
    String data = batchMeasure.hasStringValue() ? batchMeasure.getStringValue() : null;
    switch (metric.getType().getValueType()) {
      case INT:
        return toIntegerMeasure(builder, batchMeasure, data);
      case LONG:
        return toLongMeasure(builder, batchMeasure, data);
      case DOUBLE:
        return toDoubleMeasure(builder, batchMeasure, data);
      case BOOLEAN:
        return toBooleanMeasure(builder, batchMeasure, data);
      case STRING:
        return toStringMeasure(builder, batchMeasure);
      case LEVEL:
        return toLevelMeasure(builder, batchMeasure);
      case NO_VALUE:
        return toNoValueMeasure(builder, batchMeasure);
      default:
        throw new IllegalArgumentException("Unsupported Measure.ValueType " + metric.getType().getValueType());
    }
  }

  private MeasureImpl.Builder createBuilder(BatchReport.Measure batchMeasure) {
    if (batchMeasure.hasCharactericId() && batchMeasure.hasRuleKey()) {
      throw new IllegalArgumentException("Measure with both characteristicId and ruleKey are not supported");
    }
    if (batchMeasure.hasCharactericId()) {
      return MeasureImpl.builder().forCharacteristic(batchMeasure.getCharactericId());
    }
    if (batchMeasure.hasRuleKey()) {
      RuleDto ruleDto = ruleCache.get(RuleKey.parse(batchMeasure.getRuleKey()));
      return MeasureImpl.builder().forRule(ruleDto.getId());
    }
    return MeasureImpl.builder();
  }

  private static Optional<Measure> toIntegerMeasure(MeasureImpl.Builder builder, BatchReport.Measure batchMeasure, @Nullable String data) {
    if (!batchMeasure.hasIntValue()) {
      return toMeasure(builder.createNoValue(), batchMeasure);
    }
    return toMeasure(builder.create(batchMeasure.getIntValue(), data), batchMeasure);
  }

  private static Optional<Measure> toLongMeasure(MeasureImpl.Builder builder, BatchReport.Measure batchMeasure, @Nullable String data) {
    if (!batchMeasure.hasLongValue()) {
      return toMeasure(builder.createNoValue(), batchMeasure);
    }
    return toMeasure(builder.create(batchMeasure.getLongValue(), data), batchMeasure);
  }

  private static Optional<Measure> toDoubleMeasure(MeasureImpl.Builder builder, BatchReport.Measure batchMeasure, @Nullable String data) {
    if (!batchMeasure.hasDoubleValue()) {
      return toMeasure(builder.createNoValue(), batchMeasure);
    }
    return toMeasure(builder.create(batchMeasure.getDoubleValue(), data), batchMeasure);
  }

  private static Optional<Measure> toBooleanMeasure(MeasureImpl.Builder builder, BatchReport.Measure batchMeasure, @Nullable String data) {
    if (!batchMeasure.hasBooleanValue()) {
      return toMeasure(builder.createNoValue(), batchMeasure);
    }
    return toMeasure(builder.create(batchMeasure.getBooleanValue(), data), batchMeasure);
  }

  private static Optional<Measure> toStringMeasure(MeasureImpl.Builder builder, BatchReport.Measure batchMeasure) {
    if (!batchMeasure.hasStringValue()) {
      return toMeasure(builder.createNoValue(), batchMeasure);
    }
    return toMeasure(builder.create(batchMeasure.getStringValue()), batchMeasure);
  }
  
  private static Optional<Measure> toLevelMeasure(MeasureImpl.Builder builder, BatchReport.Measure batchMeasure) {
    if (!batchMeasure.hasStringValue()) {
      return toMeasure(builder.createNoValue(), batchMeasure);
    }
    Optional<Measure.Level> level = Measure.Level.toLevel(batchMeasure.getStringValue());
    if (!level.isPresent()) {
      return toMeasure(builder.createNoValue(), batchMeasure);
    }
    return toMeasure(builder.create(level.get()), batchMeasure);
  }

  private static Optional<Measure> toNoValueMeasure(MeasureImpl.Builder builder, BatchReport.Measure batchMeasure) {
    return toMeasure(builder.createNoValue(), batchMeasure);
  }

  private static Optional<Measure> toMeasure(MeasureImpl measure, BatchReport.Measure batchMeasure) {
    if (batchMeasure.hasAlertStatus() && !measure.hasQualityGateStatus()) {
      Optional<Measure.Level> qualityGateStatus = Measure.Level.toLevel(batchMeasure.getAlertStatus());
      if (qualityGateStatus.isPresent()) {
        String text = batchMeasure.hasAlertText() ? batchMeasure.getAlertText() : null;
        measure.setQualityGateStatus(new QualityGateStatus(qualityGateStatus.get(), text));
      }
    }
    if (hasAnyVariation(batchMeasure))  {
      measure.setVariations(createVariations(batchMeasure));
    }
    return Optional.of((Measure) measure);
  }

  private static boolean hasAnyVariation(BatchReport.Measure batchMeasure) {
    return batchMeasure.hasVariationValue1()
        || batchMeasure.hasVariationValue2()
        || batchMeasure.hasVariationValue3()
        || batchMeasure.hasVariationValue4()
        || batchMeasure.hasVariationValue5();
  }

  private static MeasureVariations createVariations(BatchReport.Measure batchMeasure) {
    return new MeasureVariations(
        batchMeasure.hasVariationValue1() ? batchMeasure.getVariationValue1() : null,
        batchMeasure.hasVariationValue2() ? batchMeasure.getVariationValue2() : null,
        batchMeasure.hasVariationValue3() ? batchMeasure.getVariationValue3() : null,
        batchMeasure.hasVariationValue4() ? batchMeasure.getVariationValue4() : null,
        batchMeasure.hasVariationValue5() ? batchMeasure.getVariationValue5() : null
    );
  }

}
