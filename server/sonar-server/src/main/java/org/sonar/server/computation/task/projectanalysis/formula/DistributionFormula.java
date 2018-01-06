/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.formula;

import com.google.common.base.Optional;
import org.sonar.api.ce.measure.RangeDistributionBuilder;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;

import static java.util.Objects.requireNonNull;

public class DistributionFormula implements Formula<DistributionFormula.DistributionCounter> {

  private final String metricKey;

  public DistributionFormula(String metricKey) {
    this.metricKey = requireNonNull(metricKey, "Metric key cannot be null");
  }

  @Override
  public DistributionCounter createNewCounter() {
    return new DistributionCounter();
  }

  @Override
  public Optional<Measure> createMeasure(DistributionCounter counter, CreateMeasureContext context) {
    Component.Type componentType = context.getComponent().getType();
    Optional<String> value = counter.getValue();
    if (value.isPresent() && CrawlerDepthLimit.LEAVES.isDeeperThan(componentType)) {
      return Optional.of(Measure.newMeasureBuilder().create(value.get()));
    }
    return Optional.absent();
  }

  @Override
  public String[] getOutputMetricKeys() {
    return new String[] {metricKey};
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
    public void initialize(CounterInitializationContext context) {
      Optional<Measure> measureOptional = context.getMeasure(metricKey);
      String data = measureOptional.isPresent() ? measureOptional.get().getData() : null;
      if (data != null) {
        initialized = true;
        distribution.add(data);
      }
    }

    public Optional<String> getValue() {
      if (initialized) {
        return Optional.fromNullable(distribution.build());
      }
      return Optional.absent();
    }
  }
}
