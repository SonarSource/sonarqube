/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.ws.client.qualitygate;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class UpdateConditionRequestTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  UpdateConditionRequest.Builder underTest = UpdateConditionRequest.builder();

  @Test
  public void create_condition_request() {
    UpdateConditionRequest result = underTest
      .setConditionId(10)
      .setMetricKey("metric")
      .setOperator("LT")
      .setWarning("warning")
      .setError("error")
      .setPeriod(1)
      .build();

    assertThat(result.getConditionId()).isEqualTo(10);
    assertThat(result.getMetricKey()).isEqualTo("metric");
    assertThat(result.getOperator()).isEqualTo("LT");
    assertThat(result.getWarning()).isEqualTo("warning");
    assertThat(result.getError()).isEqualTo("error");
    assertThat(result.getPeriod()).isEqualTo(1);
  }

  @Test
  public void fail_when_no_quality_gate() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Condition id is mandatory and must not be empty");

    underTest
      .setMetricKey("metric")
      .setOperator("LT")
      .setWarning("warning")
      .build();
  }

  @Test
  public void fail_when_no_metric() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Metric key is mandatory and must not be empty");

    underTest
      .setConditionId(10)
      .setOperator("LT")
      .setWarning("warning")
      .build();
  }

  @Test
  public void fail_when_no_operator() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Operator is mandatory and must not be empty");

    underTest
      .setConditionId(10)
      .setMetricKey("metric")
      .setWarning("warning")
      .build();
  }

}
