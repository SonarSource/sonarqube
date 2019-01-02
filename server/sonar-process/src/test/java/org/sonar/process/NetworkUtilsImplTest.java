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
package org.sonar.process;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeThat;

public class NetworkUtilsImplTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private NetworkUtilsImpl underTest = new NetworkUtilsImpl();

  @Test
  public void getNextAvailablePort_returns_a_port() throws Exception {
    int port = underTest.getNextAvailablePort(InetAddress.getLocalHost());
    assertThat(port)
      .isGreaterThan(1_023)
      .isLessThanOrEqualTo(65_535);
  }

  @Test
  public void getNextAvailablePort_does_not_return_twice_the_same_port() throws Exception {
    Set<Integer> ports = new HashSet<>(Arrays.asList(
      underTest.getNextAvailablePort(InetAddress.getLocalHost()),
      underTest.getNextAvailablePort(InetAddress.getLocalHost()),
      underTest.getNextAvailablePort(InetAddress.getLocalHost())));
    assertThat(ports).hasSize(3);
  }

  @Test
  public void getLocalNonLoopbackIpv4Address_returns_a_valid_local_and_non_loopback_ipv4() {
    Optional<InetAddress> address = underTest.getLocalNonLoopbackIpv4Address();

    // address is empty on offline builds
    assumeThat(address.isPresent(), CoreMatchers.is(true));

    assertThat(address.get()).isInstanceOf(Inet4Address.class);
    assertThat(address.get().isLoopbackAddress()).isFalse();
  }

  @Test
  public void getLocalInetAddress_filters_local_addresses() {
    InetAddress address = underTest.getLocalInetAddress(InetAddress::isLoopbackAddress).get();
    assertThat(address.isLoopbackAddress()).isTrue();
  }

  @Test
  public void getLocalInetAddress_returns_empty_if_no_local_addresses_match() {
    Optional<InetAddress> address = underTest.getLocalInetAddress(a -> false);
    assertThat(address).isEmpty();
  }

  @Test
  public void toInetAddress_supports_host_names() throws Exception {
    assertThat(underTest.toInetAddress("localhost")).isNotNull();
    // do not test values that require DNS calls. Build must support offline mode.
  }

  @Test
  public void toInetAddress_supports_ipv4() throws Exception {
    assertThat(underTest.toInetAddress("1.2.3.4")).isNotNull();
  }

  @Test
  public void toInetAddress_supports_ipv6() throws Exception {
    assertThat(underTest.toInetAddress("2a01:e34:ef1f:dbb0:c2f6:a978:c5c0:9ccb")).isNotNull();
    assertThat(underTest.toInetAddress("[2a01:e34:ef1f:dbb0:c2f6:a978:c5c0:9ccb]")).isNotNull();
  }

}
