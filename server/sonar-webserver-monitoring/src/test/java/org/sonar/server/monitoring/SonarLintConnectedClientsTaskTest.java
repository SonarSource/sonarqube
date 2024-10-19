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
package org.sonar.server.monitoring;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.server.pushapi.sonarlint.SonarLintClientsRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SonarLintConnectedClientsTaskTest {

  private final ServerMonitoringMetrics metrics = mock(ServerMonitoringMetrics.class);
  private final SonarLintClientsRegistry sonarLintClientsRegistry = mock(SonarLintClientsRegistry.class);
  private final SonarLintConnectedClientsTaskTest.DumpMapConfiguration config = new SonarLintConnectedClientsTaskTest.DumpMapConfiguration();
  private final SonarLintConnectedClientsTask underTest = new SonarLintConnectedClientsTask(metrics, sonarLintClientsRegistry, config);

  @Test
  public void run_when5ConnectedClients_updateWith5() {
    when(sonarLintClientsRegistry.countConnectedClients()).thenReturn(5L);

    underTest.run();

    verify(metrics).setNumberOfConnectedSonarLintClients(5L);
  }

  @Test
  public void getDelay_returnNumberIfConfigEmpty() {
    long delay = underTest.getDelay();

    assertThat(delay).isPositive();
  }

  @Test
  public void getDelay_returnNumberFromConfig() {
    config.put("sonar.server.monitoring.other.initial.delay", "100000");

    long delay = underTest.getDelay();

    assertThat(delay).isEqualTo(100_000L);
  }

  @Test
  public void getPeriod_returnNumberIfConfigEmpty() {
    long delay = underTest.getPeriod();

    assertThat(delay).isPositive();
  }

  @Test
  public void getPeriod_returnNumberFromConfig() {
    config.put("sonar.server.monitoring.other.period", "100000");

    long delay = underTest.getPeriod();

    assertThat(delay).isEqualTo(100_000L);
  }

  private static class DumpMapConfiguration implements Configuration {
    private final Map<String, String> keyValues = new HashMap<>();

    public Configuration put(String key, String value) {
      keyValues.put(key, value.trim());
      return this;
    }

    @Override
    public Optional<String> get(String key) {
      return Optional.ofNullable(keyValues.get(key));
    }

    @Override
    public boolean hasKey(String key) {
      throw new UnsupportedOperationException("hasKey not implemented");
    }

    @Override
    public String[] getStringArray(String key) {
      throw new UnsupportedOperationException("getStringArray not implemented");
    }
  }

}
