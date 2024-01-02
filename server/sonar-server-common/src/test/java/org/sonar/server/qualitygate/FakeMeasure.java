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
package org.sonar.server.qualitygate;

import java.util.Optional;
import java.util.OptionalDouble;
import javax.annotation.Nullable;
import org.sonar.api.measures.Metric;

public class FakeMeasure  implements QualityGateEvaluator.Measure {
  private Double value;
  private Metric.ValueType valueType;

  private FakeMeasure() {
    // nothing to do
  }

  public FakeMeasure(Metric.ValueType valueType) {
    this.valueType = valueType;
  }

  public FakeMeasure(@Nullable Double value) {
    this.value = value;
    this.valueType = Metric.ValueType.FLOAT;
  }

  public FakeMeasure(@Nullable Integer value) {
    this.value = value == null ? null : value.doubleValue();
    this.valueType = Metric.ValueType.INT;
  }

  public FakeMeasure(@Nullable Long value) {
    this.value = value == null ? null : value.doubleValue();
    this.valueType = Metric.ValueType.MILLISEC;
  }

  @Override
  public Metric.ValueType getType() {
    return valueType;
  }

  @Override
  public OptionalDouble getValue() {
    return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
  }

  @Override
  public Optional<String> getStringValue() {
    return Optional.empty();
  }
}
