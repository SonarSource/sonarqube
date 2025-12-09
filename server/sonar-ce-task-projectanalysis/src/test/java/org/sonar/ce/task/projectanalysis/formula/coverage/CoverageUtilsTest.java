/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.formula.coverage;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.formula.CounterInitializationContext;
import org.sonar.ce.task.projectanalysis.measure.Measure;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.ce.task.projectanalysis.formula.coverage.CoverageUtils.getLongMeasureValue;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;

class CoverageUtilsTest {

  private static final String SOME_METRIC_KEY = "some key";

  @RegisterExtension
  private final CounterInitializationContextRule fileAggregateContext = new CounterInitializationContextRule();

  @Test
  void verify_calculate_coverage() {
    assertThat(CoverageUtils.calculateCoverage(5, 10)).isEqualTo(50d);
  }

  @Test
  void getLongMeasureValue_returns_0_if_measure_does_not_exist() {
    assertThat(getLongMeasureValue(fileAggregateContext, SOME_METRIC_KEY)).isZero();
  }

  @Test
  void getLongMeasureValue_returns_0_if_measure_is_NO_VALUE() {
    fileAggregateContext.put(SOME_METRIC_KEY, newMeasureBuilder().createNoValue());

    assertThat(getLongMeasureValue(fileAggregateContext, SOME_METRIC_KEY)).isZero();
  }

  @Test
  void getLongMeasureValue_returns_value_if_measure_is_INT() {
    fileAggregateContext.put(SOME_METRIC_KEY, newMeasureBuilder().create(152));

    assertThat(getLongMeasureValue(fileAggregateContext, SOME_METRIC_KEY)).isEqualTo(152L);
  }

  @Test
  void getLongMeasureValue_returns_value_if_measure_is_LONG() {
    fileAggregateContext.put(SOME_METRIC_KEY, newMeasureBuilder().create(152L));

    assertThat(getLongMeasureValue(fileAggregateContext, SOME_METRIC_KEY)).isEqualTo(152L);
  }

  @Test
  void getLongMeasureValue_throws_ISE_if_measure_is_DOUBLE() {
    assertThatThrownBy(() -> {
      fileAggregateContext.put(SOME_METRIC_KEY, newMeasureBuilder().create(152d, 1));
      getLongMeasureValue(fileAggregateContext, SOME_METRIC_KEY);
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("value can not be converted to long because current value type is a DOUBLE");
  }

  private static class CounterInitializationContextRule implements CounterInitializationContext, AfterEachCallback {
    private final Map<String, Measure> measures = new HashMap<>();

    public CounterInitializationContextRule put(String metricKey, Measure measure) {
      checkNotNull(metricKey);
      checkNotNull(measure);
      checkState(!measures.containsKey(metricKey));
      measures.put(metricKey, measure);
      return this;
    }

    @Override
    public Component getLeaf() {
      throw new UnsupportedOperationException("getFile is not supported");
    }

    @Override
    public Optional<Measure> getMeasure(String metricKey) {
      return Optional.ofNullable(measures.get(metricKey));
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
      measures.clear();
    }
  }
}
