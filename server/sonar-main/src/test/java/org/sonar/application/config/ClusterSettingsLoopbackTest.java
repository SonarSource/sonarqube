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

import static org.sonar.process.ProcessProperties.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.CLUSTER_HOSTS;
import static org.sonar.process.ProcessProperties.CLUSTER_NETWORK_INTERFACES;
import static org.sonar.process.ProcessProperties.CLUSTER_SEARCH_HOSTS;
import static org.sonar.process.ProcessProperties.JDBC_URL;
import static org.sonar.process.ProcessProperties.SEARCH_HOST;

@RunWith(Theories.class)
public class ClusterSettingsLoopbackTest {

  private TestAppSettings settings;
  private static final String LOOPBACK_FORBIDDEN = " must not be a loopback address";
  private static final String NOT_LOCAL_ADDRESS = " is not a local address";
  private static final String NOT_RESOLVABLE = " cannot be resolved";

  @DataPoints("parameter")
  public static final ValueAndResult[] VALID_SINGLE_IP = {
      // Valid IPs
      new ValueAndResult("1.2.3.4", NOT_LOCAL_ADDRESS),
      new ValueAndResult("1.2.3.4:9001", NOT_LOCAL_ADDRESS),
      new ValueAndResult("2a01:e34:ef1f:dbb0:c2f6:a978:c5c0:9ccb", NOT_LOCAL_ADDRESS),
      new ValueAndResult("[2a01:e34:ef1f:dbb0:c2f6:a978:c5c0:9ccb]:9001", NOT_LOCAL_ADDRESS),

      // Valid Name
      new ValueAndResult("www.sonarqube.org", NOT_LOCAL_ADDRESS),
      new ValueAndResult("www.google.fr", NOT_LOCAL_ADDRESS),
      new ValueAndResult("www.google.com, www.sonarsource.com, wwww.sonarqube.org", NOT_LOCAL_ADDRESS),

      new ValueAndResult("...", NOT_RESOLVABLE),
      new ValueAndResult("භඦආ\uD801\uDC8C\uD801\uDC8B", NOT_RESOLVABLE),

      // Valide IPs List
      new ValueAndResult("1.2.3.4,2a01:e34:ef1f:dbb0:c2f6:a978:c5c0:9ccb", NOT_LOCAL_ADDRESS),
      new ValueAndResult("1.2.3.4:9001,[2a01:e34:ef1f:dbb0:c2f6:a978:c5c0:9ccb]:9001", NOT_LOCAL_ADDRESS),
      new ValueAndResult("2a01:e34:ef1f:dbb0:c2f6:a978:c5c0:9ccb,1.2.3.4:9001", NOT_LOCAL_ADDRESS),
      new ValueAndResult("[2a01:e34:ef1f:dbb0:c2f6:a978:c5c0:9ccb]:9001,2a01:e34:ef1f:dbb0:c2f6:a978:c5c0:9ccc", NOT_LOCAL_ADDRESS),

      // Loopback IPs
      new ValueAndResult("localhost", LOOPBACK_FORBIDDEN),
      new ValueAndResult("127.0.0.1", LOOPBACK_FORBIDDEN),
      new ValueAndResult("127.1.1.1", LOOPBACK_FORBIDDEN),
      new ValueAndResult("127.243.136.241", LOOPBACK_FORBIDDEN),
      new ValueAndResult("::1", LOOPBACK_FORBIDDEN),
      new ValueAndResult("0:0:0:0:0:0:0:1", LOOPBACK_FORBIDDEN),
      new ValueAndResult("localhost:9001", LOOPBACK_FORBIDDEN),
      new ValueAndResult("127.0.0.1:9001", LOOPBACK_FORBIDDEN),
      new ValueAndResult("127.1.1.1:9001", LOOPBACK_FORBIDDEN),
      new ValueAndResult("127.243.136.241:9001", LOOPBACK_FORBIDDEN),
      new ValueAndResult("[::1]:9001", LOOPBACK_FORBIDDEN),
      new ValueAndResult("[0:0:0:0:0:0:0:1]:9001", LOOPBACK_FORBIDDEN),

      // Loopback IPs list
      new ValueAndResult("127.0.0.1,192.168.11.25", LOOPBACK_FORBIDDEN),
      new ValueAndResult("192.168.11.25,127.1.1.1", LOOPBACK_FORBIDDEN),
      new ValueAndResult("2a01:e34:ef1f:dbb0:c2f6:a978:c5c0:9ccb,0:0:0:0:0:0:0:1", LOOPBACK_FORBIDDEN),
      new ValueAndResult("0:0:0:0:0:0:0:1,2a01:e34:ef1f:dbb0:c2f6:a978:c5c0:9ccb", LOOPBACK_FORBIDDEN),
      new ValueAndResult("2a01:e34:ef1f:dbb0:c3f6:a978:c5c0:9ccb,::1", LOOPBACK_FORBIDDEN),
      new ValueAndResult("::1,2a01:e34:ef1f:dbb0:c3f6:a978:c5c0:9ccb", LOOPBACK_FORBIDDEN),
      new ValueAndResult("::1,2a01:e34:ef1f:dbb0:c3f6:a978:c5c0:9ccb,2a01:e34:ef1f:dbb0:b3f6:a978:c5c0:9ccb", LOOPBACK_FORBIDDEN),
      new ValueAndResult("localhost:9001", LOOPBACK_FORBIDDEN),
      new ValueAndResult("127.0.0.1:9001", LOOPBACK_FORBIDDEN),
      new ValueAndResult("127.1.1.1:9001", LOOPBACK_FORBIDDEN),
      new ValueAndResult("127.243.136.241:9001", LOOPBACK_FORBIDDEN),
      new ValueAndResult("[::1]:9001", LOOPBACK_FORBIDDEN),
      new ValueAndResult("[0:0:0:0:0:0:0:1]:9001", LOOPBACK_FORBIDDEN),
      new ValueAndResult("127.0.0.1,192.168.11.25:9001", LOOPBACK_FORBIDDEN),
      new ValueAndResult("192.168.11.25:9001,127.1.1.1:9001", LOOPBACK_FORBIDDEN),
      new ValueAndResult("2a01:e34:ef1f:dbb0:c2f6:a978:c5c0:9ccb,[0:0:0:0:0:0:0:1]:9001", LOOPBACK_FORBIDDEN),
      new ValueAndResult("[0:0:0:0:0:0:0:1]:9001,[2a01:e34:ef1f:dbb0:c2f6:a978:c5c0:9ccb]:9001", LOOPBACK_FORBIDDEN),
      new ValueAndResult("[2a01:e34:ef1f:dbb0:c3f6:a978:c5c0:9ccb]:9001,[::1]:9001", LOOPBACK_FORBIDDEN),
      new ValueAndResult("[::1]:9001,[2a01:e34:ef1f:dbb0:c3f6:a978:c5c0:9ccb]:9001", LOOPBACK_FORBIDDEN),
      new ValueAndResult("[::1]:9001,[2a01:e34:ef1f:dbb0:c3f6:a978:c5c0:9ccb]:9001,[2a01:e34:ef1f:dbb0:b3f6:a978:c5c0:9ccb]:9001", LOOPBACK_FORBIDDEN)
  };

  @DataPoints("key")
  public static final Key[] KEYS = {
    new Key(SEARCH_HOST, false, false),
    new Key(CLUSTER_NETWORK_INTERFACES, true, false),
    new Key(CLUSTER_SEARCH_HOSTS, true, true),
    new Key(CLUSTER_HOSTS, true, true)
  };


  @DataPoints("unresolvable_hosts")
  public static final String[] UNRESOLVABLE_HOSTS = {
  };

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void resetSettings() {
    settings = getClusterSettings();
  }

  @Theory
  public void accept_throws_MessageException(
    @FromDataPoints("key") Key propertyKey,
    @FromDataPoints("parameter") ValueAndResult valueAndResult) {
    // Skip the test if the value is a list and if the key is not accepting a list
    if (settings == null) {
      System.out.println("No network found, skipping the test");
      return;
    }
    if ((valueAndResult.isList() && propertyKey.acceptList) || !valueAndResult.isList()) {
      settings.set(propertyKey.getKey(), valueAndResult.getValue());

      // If the key accept non local IPs there won't be any exception
      if (!propertyKey.acceptNonLocal || valueAndResult.getMessage() != NOT_LOCAL_ADDRESS) {
        expectedException.expect(MessageException.class);
        expectedException.expectMessage(valueAndResult.getMessage());
      }

      new ClusterSettings().accept(settings.getProps());
    }
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
        return null;
      }

    } catch (SocketException e) {
      return null;
    }

    TestAppSettings testAppSettings = new TestAppSettings()
      .set(CLUSTER_ENABLED, "true")
      .set(CLUSTER_SEARCH_HOSTS, "192.168.233.1, 192.168.233.2,192.168.233.3")
      .set(CLUSTER_HOSTS, "192.168.233.1, 192.168.233.2,192.168.233.3")
      .set(SEARCH_HOST, localAddress)
      .set(JDBC_URL, "jdbc:mysql://localhost:3306/sonar?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true&useConfigs=maxPerformance");
    return testAppSettings;
  }

  private static class Key {
    private final String key;
    private final boolean acceptList;
    private final boolean acceptNonLocal;

    private Key(String key, boolean acceptList, boolean acceptNonLocal) {
      this.key = key;
      this.acceptList = acceptList;
      this.acceptNonLocal = acceptNonLocal;
    }

    public String getKey() {
      return key;
    }

    public boolean acceptList() {
      return acceptList;
    }

    public boolean acceptNonLocal() {
      return acceptNonLocal;
    }
  }

  private static class ValueAndResult {
    private final String value;
    private final String message;

    private ValueAndResult(String value, String message) {
      this.value = value;
      this.message = message;
    }

    public String getValue() {
      return value;
    }

    public String getMessage() {
      return message;
    }

    public boolean isList() {
      return value != null && value.contains(",");
    }
  }
}
