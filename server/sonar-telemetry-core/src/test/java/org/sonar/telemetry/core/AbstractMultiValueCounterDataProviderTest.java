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
package org.sonar.telemetry.core;

import org.junit.jupiter.api.Test;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

class AbstractMultiValueCounterDataProviderTest {

  @Test
  void testGetters() {
    AbstractMultiValueCounterDataProvider underTest = new AbstractMultiValueCounterDataProvider("metricKey") {
    };
    assertThat(underTest.getMetricKey()).isEqualTo("metricKey");
    assertThat(underTest.getDimension()).isEqualTo(Dimension.INSTALLATION);
    assertThat(underTest.getGranularity()).isEqualTo(Granularity.ADHOC);
    assertThat(underTest.getType()).isEqualTo(TelemetryDataType.INTEGER);
    underTest.incrementCount("key1");
    underTest.incrementCount("key1");
    underTest.incrementCount("key1");
    underTest.incrementCount("key2");
    underTest.incrementCount("key2");
    assertThat(underTest.getValues()).contains(entry("key1", 3), entry("key2", 2));
    underTest.after();
    assertThat(underTest.getValues()).isEmpty();
  }

}
