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
package org.sonar.server.measure.live;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;

public class MeasureMatrixTest {

  private static final OrganizationDto ORGANIZATION = newOrganizationDto();
  private static final ComponentDto PROJECT = ComponentTesting.newPublicProjectDto(ORGANIZATION);
  private static final ComponentDto FILE = ComponentTesting.newFileDto(PROJECT);
  private static final MetricDto METRIC_1 = newMetricDto().setId(100);
  private static final MetricDto METRIC_2 = newMetricDto().setId(200);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void getMetric() {
    Collection<MetricDto> metrics = asList(METRIC_1, METRIC_2);

    MeasureMatrix underTest = new MeasureMatrix(asList(PROJECT, FILE), metrics, new ArrayList<>());

    assertThat(underTest.getMetric(METRIC_2.getId())).isSameAs(METRIC_2);
  }

  @Test
  public void getMetric_fails_if_metric_is_not_registered() {
    Collection<MetricDto> metrics = asList(METRIC_1);
    MeasureMatrix underTest = new MeasureMatrix(asList(PROJECT, FILE), metrics, new ArrayList<>());

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Metric with id " + METRIC_2.getId() + " not found");

    underTest.getMetric(METRIC_2.getId());
  }

  @Test
  public void getValue_returns_empty_if_measure_is_absent() {
    MetricDto metric = newMetricDto();
    LiveMeasureDto measure = newMeasure(metric, PROJECT).setValue(null);
    MeasureMatrix underTest = new MeasureMatrix(asList(PROJECT), asList(metric), asList(measure));

    assertThat(underTest.getMeasure(FILE, metric.getKey())).isEmpty();
  }

  @Test
  public void getMeasure_throws_IAE_if_metric_is_not_registered() {
    MeasureMatrix underTest = new MeasureMatrix(asList(PROJECT), asList(METRIC_1), emptyList());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Metric with key _missing_ is not registered");

    underTest.getMeasure(PROJECT, "_missing_");
  }

  @Test
  public void setValue_double_rounds_up_and_updates_value() {
    MetricDto metric = newMetricDto().setDecimalScale(2);
    LiveMeasureDto measure = newMeasure(metric, PROJECT).setValue(1.23);
    MeasureMatrix underTest = new MeasureMatrix(asList(PROJECT), asList(metric), asList(measure));

    underTest.setValue(PROJECT, metric.getKey(), 3.14159);

    assertThat(underTest.getMeasure(PROJECT, metric.getKey()).get().getValue()).isEqualTo(3.14);
    assertThat(underTest.getChanged()).hasSize(1);

    underTest.setValue(PROJECT, metric.getKey(), 3.148);
    verifyValue(underTest, PROJECT, metric, 3.15);
    verifyVariation(underTest, PROJECT, metric, null);
  }

  private void verifyValue(MeasureMatrix underTest, ComponentDto component, MetricDto metric, @Nullable Double expectedValue) {
    Optional<LiveMeasureDto> measure = underTest.getMeasure(component, metric.getKey());
    assertThat(measure).isPresent();
    assertThat(measure.get().getValue()).isEqualTo(expectedValue);
  }

  private void verifyVariation(MeasureMatrix underTest, ComponentDto component, MetricDto metric, @Nullable Double expectedVariation) {
    assertThat(underTest.getMeasure(component, metric.getKey()).get().getVariation()).isEqualTo(expectedVariation);
  }

  @Test
  public void setValue_double_does_nothing_if_value_is_unchanged() {
    MetricDto metric = newMetricDto().setDecimalScale(2);
    LiveMeasureDto measure = newMeasure(metric, PROJECT).setValue(3.14);
    MeasureMatrix underTest = new MeasureMatrix(asList(PROJECT), asList(metric), asList(measure));

    underTest.setValue(PROJECT, metric.getKey(), 3.14159);

    assertThat(underTest.getChanged()).isEmpty();
    verifyValue(underTest, PROJECT, metric, 3.14);
  }

  @Test
  public void setValue_double_updates_variation() {
    MetricDto metric = newMetricDto().setDecimalScale(2);
    LiveMeasureDto measure = newMeasure(metric, PROJECT).setValue(3.14).setVariation(1.14);
    MeasureMatrix underTest = new MeasureMatrix(asList(PROJECT), asList(metric), asList(measure));

    underTest.setValue(PROJECT, metric.getKey(), 3.56);

    assertThat(underTest.getChanged()).hasSize(1);
    verifyValue(underTest, PROJECT, metric, 3.56);
    verifyVariation(underTest, PROJECT, metric, 3.56 - (3.14 - 1.14));
  }

  @Test
  public void setValue_double_rounds_up_variation() {
    MetricDto metric = newMetricDto().setDecimalScale(2);
    LiveMeasureDto measure = newMeasure(metric, PROJECT).setValue(3.14).setVariation(1.14);
    MeasureMatrix underTest = new MeasureMatrix(asList(PROJECT), asList(metric), asList(measure));

    underTest.setValue(PROJECT, metric.getKey(), 3.569);

    assertThat(underTest.getChanged()).hasSize(1);
    verifyValue(underTest, PROJECT, metric, 3.57);
    verifyVariation(underTest, PROJECT, metric, 1.57);
  }

  @Test
  public void setValue_String_does_nothing_if_value_is_not_changed() {
    LiveMeasureDto measure = newMeasure(METRIC_1, PROJECT).setData("foo");
    MeasureMatrix underTest = new MeasureMatrix(asList(PROJECT, FILE), asList(METRIC_1), asList(measure));

    underTest.setValue(PROJECT, METRIC_1.getKey(), "foo");

    assertThat(underTest.getMeasure(PROJECT, METRIC_1.getKey()).get().getDataAsString()).isEqualTo("foo");
    assertThat(underTest.getChanged()).isEmpty();
  }

  @Test
  public void setValue_String_updates_value() {
    LiveMeasureDto measure = newMeasure(METRIC_1, PROJECT).setData("foo");
    MeasureMatrix underTest = new MeasureMatrix(asList(PROJECT, FILE), asList(METRIC_1), asList(measure));

    underTest.setValue(PROJECT, METRIC_1.getKey(), "bar");

    assertThat(underTest.getMeasure(PROJECT, METRIC_1.getKey()).get().getDataAsString()).isEqualTo("bar");
    assertThat(underTest.getChanged()).extracting(LiveMeasureDto::getDataAsString).containsExactly("bar");
  }

  @Test
  public void setLeakValue_rounds_up_and_updates_value() {
    MetricDto metric = newMetricDto().setDecimalScale(2);
    LiveMeasureDto measure = newMeasure(metric, PROJECT).setValue(null);
    MeasureMatrix underTest = new MeasureMatrix(asList(PROJECT), asList(metric), asList(measure));

    underTest.setLeakValue(PROJECT, metric.getKey(), 3.14159);
    verifyVariation(underTest, PROJECT, metric, 3.14);
    // do not update value
    verifyValue(underTest, PROJECT, metric, null);

    underTest.setLeakValue(PROJECT, metric.getKey(), 3.148);
    verifyVariation(underTest, PROJECT, metric, 3.15);
    // do not update value
    verifyValue(underTest, PROJECT, metric, null);
  }

  @Test
  public void setLeakValue_double_does_nothing_if_value_is_unchanged() {
    MetricDto metric = newMetricDto().setDecimalScale(2);
    LiveMeasureDto measure = newMeasure(metric, PROJECT).setValue(null).setVariation(3.14);
    MeasureMatrix underTest = new MeasureMatrix(asList(PROJECT), asList(metric), asList(measure));

    underTest.setLeakValue(PROJECT, metric.getKey(), 3.14159);

    assertThat(underTest.getChanged()).isEmpty();
    verifyVariation(underTest, PROJECT, metric, 3.14);
  }

  private LiveMeasureDto newMeasure(MetricDto metric, ComponentDto component) {
    return new LiveMeasureDto().setMetricId(metric.getId()).setData("foo").setComponentUuid(component.uuid());
  }
}
