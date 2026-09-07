/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.webhook.ws;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import okhttp3.HttpUrl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.config.Configuration;
import org.sonar.server.network.NetworkInterfaceProvider;
import org.sonar.server.user.UserSession;
import org.sonar.server.webhook.WebhookAddressValidator;

import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class WebhookSupportTest {
  private final Configuration configuration = mock(Configuration.class);
  private final NetworkInterfaceProvider networkInterfaceProvider = mock(NetworkInterfaceProvider.class);
  private final WebhookSupport underTest = new WebhookSupport(mock(UserSession.class), configuration, networkInterfaceProvider);

  @DataProvider
  public static Object[][] validUrls() {
    return new Object[][] {
      {"https://some.valid.address.com/random_webhook"},
      {"https://248.235.76.254/some_webhook"},
      {"https://248.235.76.254:8454/some_webhook"},

      // leading-zero octets are parsed as decimal (not octal) by the JVM, so this is just a regular public address, not a loopback bypass
      {"https://0177.0.0.1/some_webhook"},
    };
  }

  @DataProvider
  public static Object[][] loopbackUrls() {
    return new Object[][] {
      {"https://0.0.0.0/some_webhook"},
      {"https://0.0.0.0:8888/some_webhook"},
      {"https://127.0.0.1/some_webhook"},
      {"https://127.0.0.1:7777/some_webhook"},
      {"https://localhost/some_webhook"},
      {"https://localhost:9999/some_webhook"},
      {"https://192.168.1.21/"},

      // decimal-integer notation for 127.0.0.1
      {"https://2130706433/some_webhook"},
    };
  }

  @DataProvider
  public static Object[][] linkLocalAndMulticastUrls() {
    return new Object[][] {
      // link-local range including the cloud provider metadata endpoint (AWS/Azure/GCP)
      {"https://169.254.169.254/latest/meta-data/"},
      {"https://169.254.0.1/some_webhook"},
      {"https://[fe80::1]/some_webhook"},

      // multicast
      {"https://224.0.0.1/some_webhook"},
    };
  }

  @DataProvider
  public static Object[][] cloudMetadataUrls() {
    return new Object[][] {
      // AWS IMDS over IPv6 - unique-local address, not caught by isLinkLocalAddress()
      {"https://[fd00:ec2::254]/latest/meta-data/"},
      // Alibaba Cloud metadata - RFC 6598 shared address space, not caught by any InetAddress predicate
      {"https://100.100.100.200/latest/meta-data/"},
    };
  }

  @DataProvider
  public static Object[][] siteLocalUrls() {
    return new Object[][] {
      // RFC 1918 private-network ranges
      {"https://10.15.15.15/some_webhook"},
      {"https://10.15.15.15:7777/some_webhook"},
      {"https://172.16.16.16/some_webhook"},
      {"https://172.16.16.16:9999/some_webhook"},
      {"https://192.168.0.1/some_webhook"},
      {"https://192.168.0.1:8888/some_webhook"},
    };
  }

  @DataProvider
  public static Object[][] blockedUrls() {
    return Stream.of(loopbackUrls(), linkLocalAndMulticastUrls(), cloudMetadataUrls(), siteLocalUrls())
      .flatMap(Arrays::stream)
      .toArray(Object[][]::new);
  }

  @Before
  public void prepare() throws IOException {
    InetAddress inetAddress = InetAddress.getByName(HttpUrl.parse("https://192.168.1.21/").host());

    when(networkInterfaceProvider.getNetworkInterfaceAddresses())
      .thenReturn(List.of(inetAddress));
  }


  @Test
  @UseDataProvider("validUrls")
  public void checkUrlPatternSuccessfulForValidAddress(String url) {
    assertThatCode(() -> underTest.checkUrlPattern(url, "msg")).doesNotThrowAnyException();
  }

  @Test
  @UseDataProvider("blockedUrls")
  public void checkUrlPatternFailsForBlockedAddress(String url) {
    assertThatThrownBy(() -> underTest.checkUrlPattern(url, "msg"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(WebhookAddressValidator.INVALID_ADDRESS_MESSAGE);
  }

  @Test
  @UseDataProvider("blockedUrls")
  public void checkUrlPatternSuccessfulForBlockedAddressWhenSonarValidateWebhooksPropertyDisabled(String url) {
    when(configuration.getBoolean("sonar.validateWebhooks")).thenReturn(of(false));

    assertThatCode(() -> underTest.checkUrlPattern(url, "msg")).doesNotThrowAnyException();
  }

  @Test
  public void itThrowsIllegalExceptionIfGettingNetworkInterfaceAddressesFails() throws SocketException {
    when(networkInterfaceProvider.getNetworkInterfaceAddresses()).thenThrow(new SocketException());

    assertThatThrownBy(() -> underTest.checkUrlPattern("https://sonarsource.com", "msg"))
      .hasMessageContaining("Can not retrieve a network interfaces")
      .isInstanceOf(IllegalStateException.class);

    verify(networkInterfaceProvider).getNetworkInterfaceAddresses();
  }
}
