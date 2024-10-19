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
package org.sonar.server.measure.ws;

import com.google.common.collect.ImmutableSortedSet;
import java.util.Set;
import java.util.function.Predicate;
import org.sonar.api.resources.Qualifiers;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;

public class MetricDtoWithBestValue {
  private static final Set<String> QUALIFIERS_ELIGIBLE_FOR_BEST_VALUE = ImmutableSortedSet.of(Qualifiers.FILE, Qualifiers.UNIT_TEST_FILE);

  private final MetricDto metric;
  private final LiveMeasureDto bestValue;

  MetricDtoWithBestValue(MetricDto metric) {
    this.metric = metric;
    LiveMeasureDto measure = new LiveMeasureDto().setMetricUuid(metric.getUuid());
    measure.setValue(metric.getBestValue());
    this.bestValue = measure;
  }

  MetricDto getMetric() {
    return metric;
  }

  LiveMeasureDto getBestValue() {
    return bestValue;
  }

  static Predicate<ComponentDto> isEligibleForBestValue() {
    return component -> QUALIFIERS_ELIGIBLE_FOR_BEST_VALUE.contains(component.qualifier());
  }
}
