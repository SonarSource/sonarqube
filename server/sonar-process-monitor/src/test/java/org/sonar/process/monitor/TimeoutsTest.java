/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.process.monitor;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class TimeoutsTest {

  @Test
  public void test_default_values() throws Exception {
    Timeouts timeouts = new Timeouts();
    assertThat(timeouts.getMonitorPingInterval()).isGreaterThan(1000L);
    assertThat(timeouts.getAutokillPingInterval()).isGreaterThan(1000L);
    assertThat(timeouts.getAutokillPingTimeout()).isGreaterThan(1000L);
    assertThat(timeouts.getTerminationTimeout()).isGreaterThan(1000L);
    assertThat(timeouts.getJmxConnectionTimeout()).isGreaterThan(1000L);
  }

  @Test
  public void test_values() throws Exception {
    Timeouts timeouts = new Timeouts();
    timeouts.setAutokillPingInterval(1L);
    timeouts.setAutokillPingTimeout(2L);
    timeouts.setTerminationTimeout(3L);
    timeouts.setJmxConnectionTimeout(4L);
    timeouts.setMonitorPingInterval(5L);

    assertThat(timeouts.getAutokillPingInterval()).isEqualTo(1L);
    assertThat(timeouts.getAutokillPingTimeout()).isEqualTo(2L);
    assertThat(timeouts.getTerminationTimeout()).isEqualTo(3L);
    assertThat(timeouts.getJmxConnectionTimeout()).isEqualTo(4L);
    assertThat(timeouts.getMonitorPingInterval()).isEqualTo(5L);
  }
}
