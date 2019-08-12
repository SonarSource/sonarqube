/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.metric.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricKeyValidatorTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void isMetricKeyValid() {
    assertThat(MetricKeyValidator.isMetricKeyValid("")).isFalse();
    assertThat(MetricKeyValidator.isMetricKeyValid("1_2_3-ABC-1_2_3")).isTrue();
    assertThat(MetricKeyValidator.isMetricKeyValid("123_321")).isTrue();
    assertThat(MetricKeyValidator.isMetricKeyValid("123456")).isFalse();
    assertThat(MetricKeyValidator.isMetricKeyValid("1.2.3_A_3:2:1")).isFalse();
  }

  @Test
  public void checkMetricKeyFormat() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Malformed metric key '123456'. Allowed characters are alphanumeric, '-', '_', with at least one non-digit.");

    MetricKeyValidator.checkMetricKeyFormat("123456");
  }
}
