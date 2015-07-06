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

package org.sonar.server.computation.formula;

import com.google.common.base.Optional;
import java.util.Objects;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.measure.Measure;

public class SumFormula implements Formula<SumFormula.SumCounter> {

  private final String metricKey;

  public SumFormula(String metricKey) {
    this.metricKey = Objects.requireNonNull(metricKey, "Metric key cannot be null");
  }

  @Override
  public SumCounter createNewCounter() {
    return new SumCounter();
  }

  @Override
  public Optional<Measure> createMeasure(SumCounter counter, CreateMeasureContext context) {
    Optional<Integer> valueOptional = counter.getValue();
    if (valueOptional.isPresent() && context.getComponent().getType().isHigherThan(Component.Type.FILE)) {
      return Optional.of(Measure.newMeasureBuilder().create(valueOptional.get()));
    }
    return Optional.absent();

  }

  @Override
  public String getOutputMetricKey() {
    return metricKey;
  }

  class SumCounter implements Counter<SumCounter> {

    private int value = 0;
    private boolean initialized = false;

    @Override
    public void aggregate(SumCounter counter) {
      if (counter.getValue().isPresent()) {
        addValue(counter.getValue().get());
      }
    }

    @Override
    public void aggregate(FileAggregateContext context) {
      Optional<Measure> measureOptional = context.getMeasure(metricKey);
      if (measureOptional.isPresent()) {
        addValue(measureOptional.get().getIntValue());
      }
    }

    private void addValue(int newValue) {
      initialized = true;
      value += newValue;
    }

    public Optional<Integer> getValue() {
      if (initialized) {
        return Optional.of(value);
      }
      return Optional.absent();
    }
  }
}
