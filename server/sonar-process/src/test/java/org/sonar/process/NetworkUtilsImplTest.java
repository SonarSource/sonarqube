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
package org.sonar.process;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeThat;

class NetworkUtilsImplTest {

  public static final String PREFER_IPV_6_ADDRESSES = "java.net.preferIPv6Addresses";

  private final NetworkUtilsImpl underTest = new NetworkUtilsImpl();

  @Test
  void getNextAvailablePort_returns_a_port() throws Exception {
    String localhost = InetAddress.getLocalHost().getHostName();
    int port = underTest.getNextAvailablePort(localhost).getAsInt();
    assertThat(port)
      .isGreaterThan(1_023)
      .isLessThanOrEqualTo(65_535);
  }

  @Test
  void getNextAvailablePort_does_not_return_twice_the_same_port() throws Exception {
    String localhost = InetAddress.getLocalHost().getHostName();
    Set<Integer> ports = new HashSet<>(Arrays.asList(
      underTest.getNextAvailablePort(localhost).getAsInt(),
      underTest.getNextAvailablePort(localhost).getAsInt(),
      underTest.getNextAvailablePort(localhost).getAsInt()));
    assertThat(ports).hasSize(3);
  }

  @Test
  void getLocalNonLoopbackIpv4Address_returns_a_valid_local_and_non_loopback_ipv4() {
    Optional<InetAddress> address = underTest.getLocalNonLoopbackIpv4Address();

    // address is empty on offline builds
    assumeThat(address.isPresent(), CoreMatchers.is(true));

    assertThat(address.get()).isInstanceOf(Inet4Address.class);
    assertThat(address.get().isLoopbackAddress()).isFalse();
  }

  @Test
  void getHostname_returns_hostname_of_localhost_otherwise_a_constant() {
    try {
      InetAddress localHost = InetAddress.getLocalHost();
      assertThat(underTest.getHostname()).isEqualTo(localHost.getHostName());
    } catch (UnknownHostException e) {
      // no localhost on host running the UT
      assertThat(underTest.getHostname()).isEqualTo("unresolved hostname");
    }
  }

  @Test
  void getLocalInetAddress_filters_local_addresses() {
    InetAddress address = underTest.getLocalInetAddress(InetAddress::isLoopbackAddress).get();
    assertThat(address.isLoopbackAddress()).isTrue();
  }

  @Test
  void getLocalInetAddress_returns_empty_if_no_local_addresses_match() {
    Optional<InetAddress> address = underTest.getLocalInetAddress(a -> false);
    assertThat(address).isEmpty();
  }

  @Test
  void toInetAddress_supports_host_names() {
    assertThat(underTest.toInetAddress("localhost")).isNotEmpty();
    // do not test values that require DNS calls. Build must support offline mode.
  }

  @Test
  void toInetAddress_supports_ipv4() {
    assertThat(underTest.toInetAddress("1.2.3.4")).isNotEmpty();
  }

  @Test
  void toInetAddress_supports_ipv6() {
    assertThat(underTest.toInetAddress("2a01:e34:ef1f:dbb0:c2f6:a978:c5c0:9ccb")).isNotEmpty();
    assertThat(underTest.toInetAddress("[2a01:e34:ef1f:dbb0:c2f6:a978:c5c0:9ccb]")).isNotEmpty();
  }

  @Test
  void toInetAddress_returns_empty_on_unvalid_IP_and_hostname() {
    assertThat(underTest.toInetAddress(secure().nextAlphabetic(32))).isEmpty();
  }

  @Test
  void isLoopback_returns_true_on_loopback_address_or_host() {
    InetAddress loopback = InetAddress.getLoopbackAddress();

    assertThat(underTest.isLoopback(loopback.getHostAddress())).isTrue();
    assertThat(underTest.isLoopback(loopback.getHostName())).isTrue();
  }

  @Test
  void isLoopback_returns_true_on_localhost_address_or_host_if_loopback() {
    try {
      InetAddress localHost = InetAddress.getLocalHost();
      boolean isLoopback = localHost.isLoopbackAddress();
      assertThat(underTest.isLoopback(localHost.getHostAddress())).isEqualTo(isLoopback);
      assertThat(underTest.isLoopback(localHost.getHostName())).isEqualTo(isLoopback);
    } catch (UnknownHostException e) {
      // ignore, host running the test has no localhost
    }
  }

  @Test
  void isLocal_returns_true_on_loopback_address_or_host() {
    InetAddress loopback = InetAddress.getLoopbackAddress();

    assertThat(underTest.isLocal(loopback.getHostAddress())).isTrue();
    assertThat(underTest.isLocal(loopback.getHostName())).isTrue();
  }

  @Test
  void isLocal_returns_true_on_localhost_address_or_host() {
    try {
      InetAddress localHost = InetAddress.getLocalHost();

      assertThat(underTest.isLocal(localHost.getHostAddress())).isTrue();
      assertThat(underTest.isLocal(localHost.getHostName())).isTrue();
    } catch (UnknownHostException e) {
      // ignore, host running the test has no localhost
    }
  }

  @Test
  void isIpv6Address_returnsAccordingToAddressAndIPv6Preference() {
    System.setProperty(PREFER_IPV_6_ADDRESSES, "false");
    assertThat(underTest.isIpv6Address("2001:db8:3::91")).isTrue();
    assertThat(underTest.isIpv6Address("192.168.1.1")).isFalse();
    assertThat(underTest.isIpv6Address("")).isFalse();

    System.setProperty(PREFER_IPV_6_ADDRESSES, "true");
    assertThat(underTest.isIpv6Address("2001:db8:3::91")).isTrue();
    assertThat(underTest.isIpv6Address("192.168.1.1")).isFalse();
    assertThat(underTest.isIpv6Address("")).isTrue();
  }

}
