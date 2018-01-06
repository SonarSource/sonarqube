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
package org.sonar.db.metric;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricDtoFunctionsTest {

  private MetricDto metric;

  @Test
  public void isOptimizedForBestValue_at_true() {
    metric = new MetricDto()
      .setBestValue(42.0d)
      .setOptimizedBestValue(true);

    boolean result = MetricDtoFunctions.isOptimizedForBestValue().test(metric);

    assertThat(result).isTrue();
  }

  @Test
  public void isOptimizedForBestValue_is_false_when_no_best_value() {
    metric = new MetricDto()
      .setBestValue(null)
      .setOptimizedBestValue(true);

    boolean result = MetricDtoFunctions.isOptimizedForBestValue().test(metric);

    assertThat(result).isFalse();
  }

  @Test
  public void isOptimizedForBestValue_is_false_when_is_not_optimized() {
    metric = new MetricDto()
      .setBestValue(42.0d)
      .setOptimizedBestValue(false);

    boolean result = MetricDtoFunctions.isOptimizedForBestValue().test(metric);

    assertThat(result).isFalse();
  }
}
