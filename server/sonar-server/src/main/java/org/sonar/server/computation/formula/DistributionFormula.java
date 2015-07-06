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

public class DistributionFormula implements Formula<DistributionFormula.DistributionCounter> {

  private final String metricKey;

  public DistributionFormula(String metricKey) {
    this.metricKey = Objects.requireNonNull(metricKey, "Metric key cannot be null");
  }

  @Override
  public DistributionCounter createNewCounter() {
    return new DistributionCounter();
  }

  @Override
  public Optional<Measure> createMeasure(DistributionCounter counter, CreateMeasureContext context) {
    Component.Type componentType = context.getComponent().getType();
    Optional<String> value = counter.getValue();
    if (value.isPresent() && componentType.isHigherThan(Component.Type.FILE)) {
      return Optional.of(Measure.newMeasureBuilder().create(value.get()));
    }
    return Optional.absent();
  }

  @Override
  public String getOutputMetricKey() {
    return metricKey;
  }

  class DistributionCounter implements Counter<DistributionCounter> {

    private final RangeDistributionBuilder distribution = new RangeDistributionBuilder();
    private boolean initialized = false;

    @Override
    public void aggregate(DistributionCounter counter) {
      Optional<String> value = counter.getValue();
      if (value.isPresent()) {
        initialized = true;
        distribution.add(value.get());
      }
    }

    @Override
    public void aggregate(FileAggregateContext context) {
      Optional<Measure> measureOptional = context.getMeasure(metricKey);
      String data = measureOptional.isPresent() ? measureOptional.get().getData() : null;
      if (data != null) {
        initialized = true;
        distribution.add(data);
      }
    }

    public Optional<String> getValue() {
      if (initialized) {
        return distribution.build();
      }
      return Optional.absent();
    }
  }
}
