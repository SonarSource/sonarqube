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
package org.sonar.server.webhook;

import com.google.common.collect.ImmutableList;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import okhttp3.HttpUrl;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.event.Level;
import org.sonar.api.config.Configuration;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.server.network.NetworkInterfaceProvider;

import static org.mockito.Mockito.when;
import static org.sonar.api.CoreProperties.SONAR_VALIDATE_WEBHOOKS_PROPERTY;
import static org.sonar.core.config.SecurityProperties.WEBHOOKS_ALLOWED_ADDRESSES_PROPERTY;

@RunWith(DataProviderRunner.class)
public class WebhookCustomDnsTest {
  private static final String INVALID_URL = WebhookAddressValidator.INVALID_ADDRESS_MESSAGE;

  @Rule
  public LogTester logTester = new LogTester();

  private final Configuration configuration = Mockito.mock(Configuration.class);
  private final NetworkInterfaceProvider networkInterfaceProvider = Mockito.mock(NetworkInterfaceProvider.class);

  private final WebhookCustomDns underTest = new WebhookCustomDns(configuration, networkInterfaceProvider);

  @Before
  public void setUp() {
    when(configuration.getStringArray(WEBHOOKS_ALLOWED_ADDRESSES_PROPERTY)).thenReturn(new String[0]);
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

  @DataProvider
  public static Object[][] blockedAddresses() {
    return new Object[][] {
      {"localhost"},
      {"127.0.0.1"},
      {"169.254.169.254"},
      {"fe80::1"},
      {"224.0.0.1"},
      {"ff02::1"},
      {"fd00:ec2::254"},
      {"100.100.100.200"},
      {"2130706433"},
      {"10.0.0.1"},
      {"172.16.0.1"},
      {"192.168.0.1"},
    };
  }

  @Test
  @UseDataProvider("blockedAddresses")
  public void lookup_fail_on_blocked_address(String address) {
    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS_PROPERTY))
      .thenReturn(Optional.of(true));

    Assertions.assertThatThrownBy(() -> underTest.lookup(address))
      .hasMessageContaining(INVALID_URL)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @DataProvider
  public static Object[][] blockedAddressesAllowedWhenValidationDisabled() {
    return new Object[][] {
      {"169.254.169.254", "169.254.169.254"},
      {"224.0.0.1", "224.0.0.1"},
      {"fd00:ec2::254", "fd00:ec2:0:0:0:0:0:254"},
      {"100.100.100.200", "100.100.100.200"},
      {"10.0.0.1", "10.0.0.1"},
    };
  }

  @Test
  @UseDataProvider("blockedAddressesAllowedWhenValidationDisabled")
  public void lookup_dont_fail_on_blocked_address_if_validation_disabled(String address, String expectedHostAddress) throws UnknownHostException {
    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS_PROPERTY))
      .thenReturn(Optional.of(false));

    Assertions.assertThat(underTest.lookup(address))
      .extracting(InetAddress::getHostAddress)
      .containsExactly(expectedHostAddress);
  }

  @Test
  public void lookup_dont_fail_on_site_local_address_in_allow_list() throws UnknownHostException {
    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS_PROPERTY)).thenReturn(Optional.of(true));
    when(configuration.getStringArray(WEBHOOKS_ALLOWED_ADDRESSES_PROPERTY)).thenReturn(new String[] {"10.20.30.40"});

    Assertions.assertThat(underTest.lookup("10.20.30.40"))
      .extracting(InetAddress::getHostAddress)
      .containsExactly("10.20.30.40");
  }

  @Test
  public void lookup_dont_fail_on_site_local_address_in_allow_list_with_surrounding_spaces() throws UnknownHostException {
    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS_PROPERTY)).thenReturn(Optional.of(true));
    when(configuration.getStringArray(WEBHOOKS_ALLOWED_ADDRESSES_PROPERTY)).thenReturn(new String[] {"10.0.0.1", " 10.20.30.40 "});

    Assertions.assertThat(underTest.lookup("10.20.30.40"))
      .extracting(InetAddress::getHostAddress)
      .containsExactly("10.20.30.40");
  }

  @Test
  public void lookup_dont_fail_on_ipv6_site_local_address_in_allow_list_written_in_another_form() throws UnknownHostException {
    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS_PROPERTY)).thenReturn(Optional.of(true));
    when(configuration.getStringArray(WEBHOOKS_ALLOWED_ADDRESSES_PROPERTY)).thenReturn(new String[] {"FEC0:0:0:0:0:0:0:1"});

    Assertions.assertThat(underTest.lookup("fec0::1"))
      .extracting(InetAddress::getHostAddress)
      .containsExactly("fec0:0:0:0:0:0:0:1");
  }

  @DataProvider
  public static Object[][] allowListRangesContainingAddress() {
    return new Object[][] {
      {"10.20.30.0/24", "10.20.30.40"},
      {"10.0.0.0/8", "10.20.30.40"},
      {"172.16.0.0/12", "172.31.255.255"},
      {"192.168.1.128/25", "192.168.1.200"},
      {"10.20.30.40/32", "10.20.30.40"},
      {"10.20.30.99/24", "10.20.30.40"},
      {"0.0.0.0/0", "10.20.30.40"},
      {"fec0::/10", "fec0::1"},
      {"fec0::1/128", "fec0::1"},
      {"::/0", "fec0::1"},
      {"::ffff:10.20.30.40", "10.20.30.40"},
      {"10.20.30.0/24", "10.20.30.0"},
      {"10.20.30.0/24", "10.20.30.255"},
    };
  }

  @Test
  @UseDataProvider("allowListRangesContainingAddress")
  public void lookup_dont_fail_on_site_local_address_in_allow_list_range(String range, String address) throws UnknownHostException {
    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS_PROPERTY)).thenReturn(Optional.of(true));
    when(configuration.getStringArray(WEBHOOKS_ALLOWED_ADDRESSES_PROPERTY)).thenReturn(new String[] {range});

    Assertions.assertThat(underTest.lookup(address))
      .extracting(InetAddress::getHostAddress)
      .containsExactly(InetAddress.getByName(address).getHostAddress());
  }

  @DataProvider
  public static Object[][] allowListRangesNotContainingAddress() {
    return new Object[][] {
      {"10.20.31.0/24", "10.20.30.40"},
      {"192.168.1.128/25", "192.168.1.127"},
      {"::/0", "10.20.30.40"},
      {"0.0.0.0/0", "fec0::1"},
      {"fec0::/128", "10.20.30.40"},
      {"fec0::/129", "fec0::1"},
      {"fec0::/-0", "fec0::1"},
      {"fec0::/99999999999999999999", "fec0::1"},
      {"::ffff:10.20.31.0/120", "10.20.30.40"},
      {"::ffff:10.20.30.40/128", "10.20.30.41"},
    };
  }

  @Test
  @UseDataProvider("allowListRangesNotContainingAddress")
  public void lookup_fail_on_site_local_address_outside_allow_list_range(String range, String address) {
    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS_PROPERTY)).thenReturn(Optional.of(true));
    when(configuration.getStringArray(WEBHOOKS_ALLOWED_ADDRESSES_PROPERTY)).thenReturn(new String[] {range});

    Assertions.assertThatThrownBy(() -> underTest.lookup(address))
      .hasMessageContaining(INVALID_URL)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @DataProvider
  public static Object[][] allowListEntriesThatAreNotIpAddresses() {
    return new Object[][] {
      {"jenkins.internal"},
      {"10.20.30"},
      {"10.20.30.40:8080"},
      {" "},
      {"10.20.30.0/"},
      {"10.20.30.0/abc"},
      {"10.20.30.0/33"},
      {"10.20.30.0/-1"},
      {"10.20.30.0/-0"},
      {"10.20.30.0/-00"},
      {"10.20.30.0/+24"},
      {"10.20.30.0/24/8"},
      {"*"},
      {"10.20.*.*"},
      {"10.20.30.0-255"},
      {"10.20.30.0/255.255.255.0"},
      {"010.20.30.40"},
      {"0xa.20.30.40"},
      {"/24"},
      {"fec0::1%eth0"},
      {"fec0::1%eth0/64"},
      {"fec0::1%1"},
      {"fec0::1%1/64"},
      {"::ffff:10.20.30.40/129"},
      {"::ffff:10.20.30.0/120"},
      {"::ffff:10.20.30.40/128"},
      {"::ffff:10.20.30.0/0"},
      {"::ffff:10.20.30.40/32"},
      {"10.20.30.0/99999999999999999999"},
    };
  }

  @Test
  @UseDataProvider("allowListEntriesThatAreNotIpAddresses")
  public void lookup_fail_on_site_local_address_if_allow_list_entry_is_not_an_ip_address(String entry) {
    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS_PROPERTY)).thenReturn(Optional.of(true));
    when(configuration.getStringArray(WEBHOOKS_ALLOWED_ADDRESSES_PROPERTY)).thenReturn(new String[] {entry});

    Assertions.assertThatThrownBy(() -> underTest.lookup("10.20.30.40"))
      .hasMessageContaining(INVALID_URL)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void lookup_logs_a_warning_for_an_allow_list_entry_that_is_not_an_ip_address() {
    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS_PROPERTY)).thenReturn(Optional.of(true));
    when(configuration.getStringArray(WEBHOOKS_ALLOWED_ADDRESSES_PROPERTY)).thenReturn(new String[] {"jenkins.internal", "10.20.30.0/abc"});

    Assertions.assertThatThrownBy(() -> underTest.lookup("10.20.30.40"))
      .isInstanceOf(IllegalArgumentException.class);

    Assertions.assertThat(logTester.logs(Level.WARN)).containsExactly(
      "Webhook allowed address 'jenkins.internal' is ignored: it is not a valid IP address or CIDR range",
      "Webhook allowed address '10.20.30.0/abc' is ignored: it is not a valid IP address or CIDR range");
  }

  @Test
  public void lookup_does_not_log_for_blank_allow_list_entries_and_valid_entries() {
    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS_PROPERTY)).thenReturn(Optional.of(true));
    when(configuration.getStringArray(WEBHOOKS_ALLOWED_ADDRESSES_PROPERTY)).thenReturn(new String[] {"", " ", "10.20.30.0/24"});

    Assertions.assertThatCode(() -> underTest.lookup("10.20.30.40")).doesNotThrowAnyException();

    Assertions.assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  public void lookup_does_not_log_for_valid_allow_list_entries_of_the_other_ip_family() throws UnknownHostException {
    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS_PROPERTY)).thenReturn(Optional.of(true));
    when(configuration.getStringArray(WEBHOOKS_ALLOWED_ADDRESSES_PROPERTY)).thenReturn(new String[] {"10.20.30.0/24", "fec0::/10"});

    Assertions.assertThat(underTest.lookup("10.20.30.40")).hasSize(1);
    Assertions.assertThat(underTest.lookup("fec0::5")).hasSize(1);
    Assertions.assertThatThrownBy(() -> underTest.lookup("10.20.31.1")).isInstanceOf(IllegalArgumentException.class);

    Assertions.assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  public void lookup_replaces_control_characters_and_truncates_long_allow_list_entries_in_logs() {
    String longEntry = "a".repeat(100);
    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS_PROPERTY)).thenReturn(Optional.of(true));
    when(configuration.getStringArray(WEBHOOKS_ALLOWED_ADDRESSES_PROPERTY)).thenReturn(new String[] {"bad\r\nWARN forged", longEntry});

    Assertions.assertThatThrownBy(() -> underTest.lookup("10.20.30.40"))
      .isInstanceOf(IllegalArgumentException.class);

    Assertions.assertThat(logTester.logs(Level.WARN)).containsExactly(
      "Webhook allowed address 'bad__WARN forged' is ignored: it is not a valid IP address or CIDR range",
      "Webhook allowed address '" + "a".repeat(64) + "...' is ignored: it is not a valid IP address or CIDR range");
  }

  @Test
  public void lookup_fail_on_site_local_address_without_reading_network_interfaces_when_allow_list_is_empty() throws SocketException {
    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS_PROPERTY)).thenReturn(Optional.of(true));
    when(networkInterfaceProvider.getNetworkInterfaceAddresses()).thenThrow(new SocketException());

    Assertions.assertThatThrownBy(() -> underTest.lookup("10.20.30.40"))
      .hasMessageContaining(INVALID_URL)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void lookup_fail_on_site_local_address_outside_allow_list() {
    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS_PROPERTY)).thenReturn(Optional.of(true));
    when(configuration.getStringArray(WEBHOOKS_ALLOWED_ADDRESSES_PROPERTY)).thenReturn(new String[] {"10.20.30.40"});

    Assertions.assertThatThrownBy(() -> underTest.lookup("10.21.0.1"))
      .hasMessageContaining(INVALID_URL)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @DataProvider
  public static Object[][] addressesNeverAllowed() {
    return new Object[][] {
      {"127.0.0.1"},
      {"169.254.169.254"},
      {"224.0.0.1"},
      {"fd00:ec2::254"},
      {"100.100.100.200"},
    };
  }

  @Test
  @UseDataProvider("addressesNeverAllowed")
  public void lookup_fail_on_non_site_local_blocked_address_even_if_in_allow_list(String address) {
    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS_PROPERTY)).thenReturn(Optional.of(true));
    when(configuration.getStringArray(WEBHOOKS_ALLOWED_ADDRESSES_PROPERTY)).thenReturn(new String[] {address, "0.0.0.0/0", "::/0"});

    Assertions.assertThatThrownBy(() -> underTest.lookup(address))
      .hasMessageContaining(INVALID_URL)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void lookup_fail_on_sonarqube_server_address_even_if_in_allow_list() throws UnknownHostException, SocketException {
    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS_PROPERTY)).thenReturn(Optional.of(true));
    when(configuration.getStringArray(WEBHOOKS_ALLOWED_ADDRESSES_PROPERTY)).thenReturn(new String[] {"192.168.1.21"});
    when(networkInterfaceProvider.getNetworkInterfaceAddresses()).thenReturn(List.of(InetAddress.getByName("192.168.1.21")));

    Assertions.assertThatThrownBy(() -> underTest.lookup("192.168.1.21"))
      .hasMessageContaining(INVALID_URL)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void lookup_dont_fail_on_leading_zero_octet_address() throws UnknownHostException {
    // the JVM parses leading-zero octets as decimal, not octal, so "0177.0.0.1" resolves to 177.0.0.1 - a regular public
    // address, not a loopback bypass
    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS_PROPERTY))
      .thenReturn(Optional.of(true));

    Assertions.assertThat(underTest.lookup("0177.0.0.1"))
      .extracting(InetAddress::getHostAddress)
      .containsExactly("177.0.0.1");
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
