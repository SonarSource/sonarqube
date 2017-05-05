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

package org.sonar.application.config;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.process.MessageException;

import static java.lang.String.format;
import static org.sonar.process.ProcessProperties.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.CLUSTER_HOSTS;
import static org.sonar.process.ProcessProperties.CLUSTER_NETWORK_INTERFACES;
import static org.sonar.process.ProcessProperties.CLUSTER_SEARCH_HOSTS;
import static org.sonar.process.ProcessProperties.JDBC_URL;
import static org.sonar.process.ProcessProperties.SEARCH_HOST;

@RunWith(Theories.class)
public class ClusterSettingsLoopbackTest {

  private TestAppSettings settings;

  @DataPoints("loopback_with_single_ip")
  public static final String[] LOOPBACK_SINGLE_IP = {
    "localhost",
    "127.0.0.1",
    "127.1.1.1",
    "127.243.136.241",
    "::1",
    "0:0:0:0:0:0:0:1"
  };

  @DataPoints("loopback_with_multiple_ips")
  public static final String[] LOOPBACK_IPS = {
    "localhost",
    "127.0.0.1",
    "127.1.1.1",
    "127.243.136.241",
    "::1",
    "0:0:0:0:0:0:0:1",
    "127.0.0.1,192.168.11.25",
    "192.168.11.25,127.1.1.1",
    "2a01:e34:ef1f:dbb0:c2f6:a978:c5c0:9ccb,0:0:0:0:0:0:0:1",
    "0:0:0:0:0:0:0:1,2a01:e34:ef1f:dbb0:c2f6:a978:c5c0:9ccb",
    "2a01:e34:ef1f:dbb0:c3f6:a978:c5c0:9ccb,::1",
    "::1,2a01:e34:ef1f:dbb0:c3f6:a978:c5c0:9ccb",
    "::1,2a01:e34:ef1f:dbb0:c3f6:a978:c5c0:9ccb,2a01:e34:ef1f:dbb0:b3f6:a978:c5c0:9ccb"
  };

  @DataPoints("key_for_single_ip")
  public static final String[] PROPERTY_KEYS_WITH_SINGLE_IP = {
    SEARCH_HOST
  };

  @DataPoints("key_for_multiple_ips")
  public static final String[] PROPERTY_KEYS_WITH_MULTIPLE_IPS = {
    CLUSTER_NETWORK_INTERFACES,
    CLUSTER_SEARCH_HOSTS,
    CLUSTER_HOSTS
  };

  @DataPoints("key_with_local_ip")
  public static final String[] PROPERTY_KEYS_ALL = {
    CLUSTER_NETWORK_INTERFACES,
    SEARCH_HOST
  };

  @DataPoints("not_local_address")
  public static final String[] NOT_LOCAL_ADDRESS = {
    "www.sonarqube.org",
    "www.google.fr",
    "www.google.com, www.sonarsource.com, wwww.sonarqube.org"
  };

  @DataPoints("unresolvable_hosts")
  public static final String[] UNRESOLVABLE_HOSTS = {
    "...",
    "භඦආ\uD801\uDC8C\uD801\uDC8B"
  };

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void resetSettings() {
    settings = getClusterSettings();
  }

  @Theory
  public void accept_throws_MessageException_if_not_local_address(
    @FromDataPoints("key_with_local_ip") String propertyKey,
    @FromDataPoints("not_local_address") String inet) {
    settings.set(propertyKey, inet);

    expectedException.expect(MessageException.class);
    expectedException.expectMessage(" is not a local address");

    new ClusterSettings().accept(settings.getProps());
  }

  @Theory
  public void accept_throws_MessageException_if_unresolvable_host(
    @FromDataPoints("key_with_local_ip") String propertyKey,
    @FromDataPoints("unresolvable_hosts") String inet) {
    settings.set(propertyKey, inet);

    expectedException.expect(MessageException.class);
    expectedException.expectMessage(" cannot be resolved");

    new ClusterSettings().accept(settings.getProps());
  }

  @Theory
  public void accept_throws_MessageException_if_loopback(
    @FromDataPoints("key_for_single_ip") String propertyKey,
    @FromDataPoints("loopback_with_single_ip") String inet) {
    settings.set(propertyKey, inet);
    checkLoopback(propertyKey);
  }

  @Theory
  public void accept_throws_MessageException_if_loopback_for_multiple_ips(
    @FromDataPoints("key_for_multiple_ips") String propertyKey,
    @FromDataPoints("loopback_with_multiple_ips") String inet) {
    settings.set(propertyKey, inet);
    checkLoopback(propertyKey);
  }

  private void checkLoopback(String key) {
    expectedException.expect(MessageException.class);
    expectedException.expectMessage(format(" of [%s] must not be a loopback address",  key));

    new ClusterSettings().accept(settings.getProps());
  }

  private static TestAppSettings getClusterSettings()  {
    String localAddress = null;
    try {
      Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
      for (NetworkInterface networkInterface : Collections.list(nets)) {
        if (!networkInterface.isLoopback() && networkInterface.isUp()) {
          localAddress = networkInterface.getInetAddresses().nextElement().getHostAddress();
        }
      }
      if (localAddress == null) {
        throw new RuntimeException("Cannot find a non loopback card required for tests");
      }

    } catch (SocketException e) {
      throw new RuntimeException("Cannot find a non loopback card required for tests");
    }

    TestAppSettings testAppSettings = new TestAppSettings()
      .set(CLUSTER_ENABLED, "true")
      .set(CLUSTER_SEARCH_HOSTS, "192.168.233.1, 192.168.233.2,192.168.233.3")
      .set(CLUSTER_HOSTS, "192.168.233.1, 192.168.233.2,192.168.233.3")
      .set(SEARCH_HOST, localAddress)
      .set(JDBC_URL, "jdbc:mysql://localhost:3306/sonar?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true&useConfigs=maxPerformance");
    return testAppSettings;
  }
}
