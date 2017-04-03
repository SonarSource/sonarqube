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
package org.sonar.process;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.net.InetAddress.getLoopbackAddress;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.process.NetworkUtils.getNextAvailablePort;

public class NetworkUtilsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void getNextAvailablePort_never_returns_the_same_port_in_current_jvm() {
    Set<Integer> ports = new HashSet<>();
    for (int i = 0; i < 100; i++) {
      int port = getNextAvailablePort(getLoopbackAddress());
      assertThat(port).isGreaterThan(1_023);
      ports.add(port);
    }
    assertThat(ports).hasSize(100);
  }

  @Test
  public void getNextAvailablePort_retries_to_get_available_port_when_port_has_already_been_allocated() {
    NetworkUtils.PortAllocator portAllocator = mock(NetworkUtils.PortAllocator.class);
    when(portAllocator.getAvailable(any(InetAddress.class))).thenReturn(9_000, 9_000, 9_000, 9_100);

    InetAddress address = getLoopbackAddress();
    assertThat(getNextAvailablePort(address, portAllocator)).isEqualTo(9_000);
    assertThat(getNextAvailablePort(address, portAllocator)).isEqualTo(9_100);
  }

  @Test
  public void getNextAvailablePort_does_not_return_special_ports() {
    NetworkUtils.PortAllocator portAllocator = mock(NetworkUtils.PortAllocator.class);
    when(portAllocator.getAvailable(any(InetAddress.class))).thenReturn(900, 903, 1_059);

    // the two first ports are banned because < 1023, so 1_059 is returned
    assertThat(getNextAvailablePort(getLoopbackAddress(), portAllocator)).isEqualTo(1_059);
  }

  @Test
  public void getNextAvailablePort_throws_ISE_if_too_many_attempts() {
    NetworkUtils.PortAllocator portAllocator = mock(NetworkUtils.PortAllocator.class);
    when(portAllocator.getAvailable(any(InetAddress.class))).thenReturn(900);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to find an available port on ");

    getNextAvailablePort(getLoopbackAddress(), portAllocator);
  }

  @Test
  public void getHostName_must_return_a_value() {
    assertThat(NetworkUtils.getHostName()).containsPattern(".* \\(.*\\)");
  }
}
