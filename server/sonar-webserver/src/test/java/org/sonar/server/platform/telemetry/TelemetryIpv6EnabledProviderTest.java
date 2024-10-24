/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.telemetry;

import java.net.InetAddress;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.sonar.api.config.Configuration;
import org.sonar.process.NetworkUtils;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class TelemetryIpv6EnabledProviderTest {

  private final Configuration configuration = mock(Configuration.class);

  @Test
  void testGetters_whenIpv6Address(){
    assertIpVersion(6);
  }

  @Test
  void testGetters_whenIpv4Address(){
    assertIpVersion(4);
  }

  void assertIpVersion(Integer expectedProtocol) {
    TelemetryIpv6EnabledProvider underTest = new TelemetryIpv6EnabledProvider(new NetworkUtilsTestImpl(6 == expectedProtocol), configuration);
    assertEquals("internet_protocol_version", underTest.getMetricKey());
    assertEquals(Dimension.INSTALLATION, underTest.getDimension());
    assertEquals(Granularity.MONTHLY, underTest.getGranularity());
    assertEquals(TelemetryDataType.INTEGER, underTest.getType());
    assertEquals(Optional.of(expectedProtocol), underTest.getValue());
  }

  static class NetworkUtilsTestImpl implements NetworkUtils {

    private final boolean isIpv6Address;

    public NetworkUtilsTestImpl(boolean isIpv6Address) {
      this.isIpv6Address = isIpv6Address;
    }

    @Override
    public int getNextLoopbackAvailablePort() {
      return 0;
    }

    @Override
    public OptionalInt getNextAvailablePort(String hostOrAddress) {
      return OptionalInt.empty();
    }

    @Override
    public String getHostname() {
      return "";
    }

    @Override
    public Optional<InetAddress> toInetAddress(String hostOrAddress) {
      return Optional.empty();
    }

    @Override
    public boolean isLocal(String hostOrAddress) {
      return false;
    }

    @Override
    public boolean isLoopback(String hostOrAddress) {
      return false;
    }

    @Override
    public boolean isIpv6Address(String hostOrAddress) {
      return isIpv6Address;
    }

    @Override
    public Optional<InetAddress> getLocalInetAddress(Predicate<InetAddress> predicate) {
      return Optional.empty();
    }
  }

}
