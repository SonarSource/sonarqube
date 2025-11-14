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
package org.sonar.server.monitoring;

import io.prometheus.client.CollectorRegistry;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.app.ProcessCommandWrapper;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ComputeEngineMetricStatusTaskTest {

  private final ServerMonitoringMetrics serverMonitoringMetrics = mock(ServerMonitoringMetrics.class);
  private final ProcessCommandWrapper processCommandWrapper = mock(ProcessCommandWrapper.class);
  private final Configuration configuration = new MapSettings().asConfig();

  private final ComputeEngineMetricStatusTask underTest = new ComputeEngineMetricStatusTask(serverMonitoringMetrics, processCommandWrapper, configuration);

  @Before
  public void before() {
    CollectorRegistry.defaultRegistry.clear();
  }

  @Test
  public void when_compute_engine_up_status_is_updated_to_green() {
    when(processCommandWrapper.isCeOperational()).thenReturn(true);

    underTest.run();

    verify(serverMonitoringMetrics, times(1)).setComputeEngineStatusToGreen();
    verifyNoMoreInteractions(serverMonitoringMetrics);
  }

  @Test
  public void when_compute_engine_down_status_is_updated_to_red() {
    when(processCommandWrapper.isCeOperational()).thenReturn(false);

    underTest.run();

    verify(serverMonitoringMetrics, times(1)).setComputeEngineStatusToRed();
    verifyNoMoreInteractions(serverMonitoringMetrics);
  }

  @Test
  public void task_has_default_delay(){
    Assertions.assertThat(underTest.getDelay()).isPositive();
    Assertions.assertThat(underTest.getPeriod()).isPositive();
  }

}
