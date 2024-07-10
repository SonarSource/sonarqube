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
package org.sonar.telemetry.metrics.schema;

import org.junit.jupiter.api.Test;
import org.sonar.telemetry.Granularity;
import org.sonar.telemetry.TelemetryDataType;

import static org.assertj.core.api.Assertions.assertThat;

class UserMetricTest {

  @Test
  void gettersAndSetters() {
    UserMetric metric = new UserMetric(
      "user-key-1",
      true,
      "user-uuid",
      TelemetryDataType.BOOLEAN,
      Granularity.DAILY
    );

    assertThat(metric.getValue()).isEqualTo(true);
    assertThat(metric.getKey()).isEqualTo("user-key-1");
    assertThat(metric.getGranularity()).isEqualTo(Granularity.DAILY);
    assertThat(metric.getType()).isEqualTo(TelemetryDataType.BOOLEAN);
    assertThat(metric.getUserUuid()).isEqualTo("user-uuid");
  }

}
