/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.ce.task.projectanalysis.measure;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.ce.task.projectanalysis.component.Component;

/**
 * This class represents a metric key and an associated measure.
 * It can be used to easily compare the content of the SetMultimap returned by {@link MeasureRepository#getRawMeasures(Component)}
 * or {@link MeasureRepositoryRule#getAddedRawMeasures(int)}.
 * <p>
 * This class is also highly useful to accurately make sure of the SetMultimap content since this
 * object implements a deep equals of Measure objects (see {@link #deepEquals(Measure, Measure)}), when
 * {@link Measure#equals(Object)} only care about the ruleId and characteristicId.
 * </p>
 * <p>
 * In order to explore the content of the Map, use {@link #toEntries(Map)} to convert it
 * to an Iterable of {@link MeasureRepoEntry} and then take benefit of AssertJ API, eg.:
 * <pre>
 * assertThat(MeasureRepoEntry.toEntries(measureRepository.getAddedRawMeasures(componentRef))).containsOnly(
 *   MeasureRepoEntry.entryOf(DEVELOPMENT_COST_KEY, newMeasureBuilder().create(Long.toString(expectedDevCost))),
 *   MeasureRepoEntry.entryOf(SQALE_DEBT_RATIO_KEY, newMeasureBuilder().create(expectedDebtRatio))
 * );
 * </pre>
 * </p>
 */
public final class MeasureRepoEntry {
  private final String metricKey;
  private final Measure measure;

  public MeasureRepoEntry(String metricKey, Measure measure) {
    this.metricKey = metricKey;
    this.measure = measure;
  }

  public static Function<Map.Entry<String, Measure>, MeasureRepoEntry> toMeasureRepoEntry() {
    return EntryToMeasureRepoEntry.INSTANCE;
  }

  public static Iterable<MeasureRepoEntry> toEntries(Map<String, Measure> data) {
    return data.entrySet().stream().map(toMeasureRepoEntry()).toList();
  }

  public static MeasureRepoEntry entryOf(String metricKey, Measure measure) {
    return new MeasureRepoEntry(metricKey, measure);
  }

  public static boolean deepEquals(Measure measure, Measure measure1) {
    return measure.getValueType() == measure1.getValueType()
      && equalsByValue(measure, measure1)
      && equalsByQualityGateStatus(measure, measure1)
      && Objects.equals(measure.getData(), measure1.getData());
  }

  private static boolean equalsByValue(Measure measure, Measure measure1) {
    switch (measure.getValueType()) {
      case BOOLEAN:
        return measure.getBooleanValue() == measure1.getBooleanValue();
      case INT:
        return measure.getIntValue() == measure1.getIntValue();
      case LONG:
        return measure.getLongValue() == measure1.getLongValue();
      case DOUBLE:
        return Double.compare(measure.getDoubleValue(), measure1.getDoubleValue()) == 0;
      case STRING:
        return measure.getStringValue().equals(measure1.getStringValue());
      case LEVEL:
        return measure.getLevelValue() == measure1.getLevelValue();
      case NO_VALUE:
        return true;
      default:
        throw new IllegalArgumentException("Unsupported ValueType " + measure.getValueType());
    }
  }

  private static boolean equalsByQualityGateStatus(Measure measure, Measure measure1) {
    if (measure.hasQualityGateStatus() != measure1.hasQualityGateStatus()) {
      return false;
    }
    if (!measure.hasQualityGateStatus()) {
      return true;
    }
    return Objects.equals(measure.getQualityGateStatus(), measure1.getQualityGateStatus());
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MeasureRepoEntry that = (MeasureRepoEntry) o;
    return Objects.equals(metricKey, that.metricKey) &&
      deepEquals(measure, that.measure);
  }

  @Override
  public int hashCode() {
    return Objects.hash(metricKey, measure);
  }

  @Override
  public String toString() {
    return "<" + metricKey + ", " + measure + '>';
  }

  private enum EntryToMeasureRepoEntry implements Function<Map.Entry<String, Measure>, MeasureRepoEntry> {
    INSTANCE;

    @Nullable
    @Override
    public MeasureRepoEntry apply(@Nonnull Map.Entry<String, Measure> input) {
      return new MeasureRepoEntry(input.getKey(), input.getValue());
    }
  }
}
