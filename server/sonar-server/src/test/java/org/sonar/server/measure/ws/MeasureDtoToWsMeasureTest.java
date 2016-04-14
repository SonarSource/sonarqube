/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.metric.MetricDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.sonar.db.measure.MeasureTesting.newMeasureDto;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.test.TestUtils.hasOnlyPrivateConstructors;

public class MeasureDtoToWsMeasureTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void fail_when_mapping_fails() {
    MetricDto metric = spy(newMetricDto().setKey("metric-key"));
    when(metric.getValueType()).thenThrow(NullPointerException.class);
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Error while mapping a measure of metric key 'metric-key' and parameters ");

    MeasureDtoToWsMeasure.measureDtoToWsMeasure(metric, newMeasureDto(metric, 1L).setValue(5.5d).setData("data"));
  }

  @Test
  public void utility_class() {
    assertThat(hasOnlyPrivateConstructors(MeasureDtoToWsMeasure.class)).isTrue();
  }
}
