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
package org.sonar.server.webhook;

import com.google.common.collect.ImmutableList;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Optional;
import okhttp3.HttpUrl;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.config.Configuration;
import org.sonar.server.network.NetworkInterfaceProvider;

import static org.mockito.Mockito.when;
import static org.sonar.api.CoreProperties.SONAR_VALIDATE_WEBHOOKS_PROPERTY;

public class WebhookCustomDnsTest {
  private static final String INVALID_URL = "Invalid URL: loopback and wildcard addresses are not allowed for webhooks.";

  private final Configuration configuration = Mockito.mock(Configuration.class);
  private final NetworkInterfaceProvider networkInterfaceProvider = Mockito.mock(NetworkInterfaceProvider.class);

  private final WebhookCustomDns underTest = new WebhookCustomDns(configuration, networkInterfaceProvider);

  @Test
  public void lookup_fail_on_localhost() {
    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS_PROPERTY))
      .thenReturn(Optional.of(true));

    Assertions.assertThatThrownBy(() -> underTest.lookup("localhost"))
      .hasMessageContaining(INVALID_URL)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void lookup_fail_on_127_0_0_1() {
    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS_PROPERTY))
      .thenReturn(Optional.of(true));

    Assertions.assertThatThrownBy(() -> underTest.lookup("127.0.0.1"))
      .hasMessageContaining(INVALID_URL)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void lookup_fail_on_192_168_1_21() throws UnknownHostException, SocketException {
    InetAddress inetAddress = InetAddress.getByName(HttpUrl.parse("https://192.168.1.21/").host());

    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS_PROPERTY))
      .thenReturn(Optional.of(true));

    when(networkInterfaceProvider.getNetworkInterfaceAddresses())
      .thenReturn(ImmutableList.of(inetAddress));

    Assertions.assertThatThrownBy(() -> underTest.lookup("192.168.1.21"))
      .hasMessageContaining(INVALID_URL)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void lookup_fail_on_ipv6_local_case_insensitive() throws UnknownHostException, SocketException {
    Optional<InetAddress> inet6Address = Collections.list(NetworkInterface.getNetworkInterfaces())
      .stream()
      .flatMap(ni -> Collections.list(ni.getInetAddresses()).stream())
      .filter(Inet6Address.class::isInstance).findAny();

    if (!inet6Address.isPresent()) {
      return;
    }

    String differentCaseAddress = getDifferentCaseInetAddress(inet6Address.get());

    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS_PROPERTY))
      .thenReturn(Optional.of(true));

    when(networkInterfaceProvider.getNetworkInterfaceAddresses())
      .thenReturn(ImmutableList.of(inet6Address.get()));

    Assertions.assertThatThrownBy(() -> underTest.lookup(differentCaseAddress))
      .hasMessageContaining(INVALID_URL)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void lookup_fail_on_network_interface_throwing_socket_exception() throws SocketException {
    when(networkInterfaceProvider.getNetworkInterfaceAddresses())
      .thenThrow(new SocketException());

    Assertions.assertThatThrownBy(() -> underTest.lookup("sonarsource.com"))
      .hasMessageContaining("Network interfaces could not be fetched.")
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void lookup_dont_fail_on_localhost_if_validation_disabled() throws UnknownHostException {
    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS_PROPERTY))
      .thenReturn(Optional.of(false));

    Assertions.assertThat(underTest.lookup("localhost"))
      .extracting(InetAddress::toString)
      .containsExactlyInAnyOrder("localhost/127.0.0.1");
  }

  @Test
  public void lookup_dont_fail_on_classic_host_with_validation_enabled() throws UnknownHostException {
    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS_PROPERTY))
      .thenReturn(Optional.of(true));

    Assertions.assertThat(underTest.lookup("sonarsource.com").toString()).contains("sonarsource.com/");
  }

  private String getDifferentCaseInetAddress(InetAddress inetAddress) {
    StringBuilder differentCaseAddress = new StringBuilder();
    String address = inetAddress.getHostAddress();
    int i;
    for (i = 0; i < address.length(); i++) {
      char c = address.charAt(i);
      if (Character.isAlphabetic(c)) {
        differentCaseAddress.append(Character.isUpperCase(c) ? Character.toLowerCase(c) : Character.toUpperCase(c));
        break;
      } else {
        differentCaseAddress.append(c);
      }
    }

    if (i < address.length() - 1) {
      differentCaseAddress.append(address.substring(i + 1));
    }

    return differentCaseAddress.toString();
  }
}
