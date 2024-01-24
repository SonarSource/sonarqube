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
package org.sonar.ce.task.projectanalysis.formula;

import com.google.gson.Gson;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Test;
import org.sonar.api.issue.impact.Severity;
import org.sonar.ce.task.projectanalysis.measure.Measure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ImpactSumFormulaTest {

  public static final ImpactSumFormula IMPACT_SUM_FORMULA = ImpactSumFormula.createImpactSumFormula("metricKey");
  private final Gson gson = new Gson();

  private final CounterInitializationContext counterInitializationContext = mock(CounterInitializationContext.class);

  private final CreateMeasureContext createMeasureContext = mock(CreateMeasureContext.class);

  @Test
  public void getOutputMetricKeys_shouldReturnCorrectMetrics() {
    assertThat(IMPACT_SUM_FORMULA.getOutputMetricKeys()).containsExactly("metricKey");
  }

  @Test
  public void createMeasure_whenCounterReturnsValue_shouldReturnExpectedMeasure() {
    ImpactSumFormula.ImpactCounter counter = IMPACT_SUM_FORMULA.createNewCounter();
    String value = newImpactJson(4, 2, 1, 1);
    addMeasure("metricKey", value);
    counter.initialize(counterInitializationContext);

    assertThat(IMPACT_SUM_FORMULA.createMeasure(counter, createMeasureContext)).get().extracting(Measure::getData)
      .isEqualTo(value);
  }

  @Test
  public void createMeasure_whenCounterReturnsNoValue_shouldReturnNoMeasure() {
    ImpactSumFormula.ImpactCounter counter = IMPACT_SUM_FORMULA.createNewCounter();
    assertThat(IMPACT_SUM_FORMULA.createMeasure(counter, createMeasureContext)).isEmpty();
  }

  @Test
  public void createNewCounter_shouldReturnExpectedCounter() {
    assertThat(IMPACT_SUM_FORMULA.createNewCounter()).isNotNull()
      .isInstanceOf(ImpactSumFormula.ImpactCounter.class);
  }

  @Test
  public void getValue_whenInitialized_shouldReturnExpectedValue() {
    ImpactSumFormula.ImpactCounter counter = IMPACT_SUM_FORMULA.createNewCounter();
    String value = newImpactJson(4, 2, 1, 1);
    addMeasure("metricKey", value);
    counter.initialize(counterInitializationContext);
    assertThat(counter.getValue()).get().isEqualTo(value);
  }

  @Test
  public void getValue_whenAggregatingExistingValue_shouldReturnAggregationResult() {
    ImpactSumFormula.ImpactCounter counter = IMPACT_SUM_FORMULA.createNewCounter();
    String value = newImpactJson(4, 2, 1, 1);
    addMeasure("metricKey", value);
    counter.initialize(counterInitializationContext);

    ImpactSumFormula.ImpactCounter counter2 = IMPACT_SUM_FORMULA.createNewCounter();
    String value2 = newImpactJson(3, 1, 1, 1);
    addMeasure("metricKey", value2);
    counter2.initialize(counterInitializationContext);

    counter.aggregate(counter2);
    assertThat(counter.getValue()).get().isEqualTo(newImpactJson(7, 3, 2, 2));
  }

  @Test
  public void getValue_whenAggregatingUninitializedValue_shouldReturnEmptyValue() {
    ImpactSumFormula.ImpactCounter counter = IMPACT_SUM_FORMULA.createNewCounter();
    String value = newImpactJson(4, 2, 1, 1);
    addMeasure("metricKey", value);
    counter.initialize(counterInitializationContext);

    ImpactSumFormula.ImpactCounter counter2 = IMPACT_SUM_FORMULA.createNewCounter();

    counter.aggregate(counter2);
    assertThat(counter.getValue()).isEmpty();
  }

  @Test
  public void getValue_whenAggregatingEmptyValue_shouldReturnEmptyValue() {

    ImpactSumFormula.ImpactCounter counter = IMPACT_SUM_FORMULA.createNewCounter();
    String value = newImpactJson(4, 2, 1, 1);
    addMeasure("metricKey", value);
    counter.initialize(counterInitializationContext);

    ImpactSumFormula.ImpactCounter counter2 = IMPACT_SUM_FORMULA.createNewCounter();
    addMeasure("metricKey", null);
    counter2.initialize(counterInitializationContext);

    counter.aggregate(counter2);

    assertThat(counter.getValue()).isEmpty();
  }

  @Test
  public void getValue_whenNotInitialized_shouldReturnEmpty() {
    ImpactSumFormula.ImpactCounter counter = IMPACT_SUM_FORMULA.createNewCounter();
    assertThat(counter.getValue()).isEmpty();
  }


  public String newImpactJson(Integer total, Integer high, Integer medium, Integer low) {
    return gson.toJson(Map.of("total", total, Severity.HIGH.name(), high, Severity.MEDIUM.name(), medium, Severity.LOW.name(), low));
  }

  private void addMeasure(String metricKey, @Nullable String value) {
    when(counterInitializationContext.getMeasure(metricKey)).thenReturn(Optional.ofNullable(value).map(v -> Measure.newMeasureBuilder().create(v)));
  }


}
