/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.net.InetAddress;
import java.util.Optional;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.process.MessageException;
import org.sonar.process.NetworkUtils;
import org.sonar.process.NetworkUtilsImpl;

import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.spy;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_HOSTS;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_HOST;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_TYPE;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_SEARCH_HOSTS;
import static org.sonar.process.ProcessProperties.Property.JDBC_URL;
import static org.sonar.process.ProcessProperties.Property.SEARCH_HOST;

public class ClusterSettingsLoopbackTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private InetAddress loopback = InetAddress.getLoopbackAddress();
  private InetAddress nonLoopbackLocal;
  private NetworkUtils network = spy(NetworkUtilsImpl.INSTANCE);

  @Before
  public void setUp() {
    Optional<InetAddress> opt = network.getLocalNonLoopbackIpv4Address();
    assumeThat(opt.isPresent(), CoreMatchers.is(true));

    nonLoopbackLocal = opt.get();
  }

  @Test
  public void ClusterSettings_throws_MessageException_if_host_of_search_node_is_loopback() {
    verifySearchFailureIfLoopback(CLUSTER_NODE_HOST.getKey());
    verifySearchFailureIfLoopback(CLUSTER_SEARCH_HOSTS.getKey());
    verifySearchFailureIfLoopback(CLUSTER_HOSTS.getKey());
    verifySearchFailureIfLoopback(SEARCH_HOST.getKey());
  }

  @Test
  public void ClusterSettings_throws_MessageException_if_host_of_app_node_is_loopback() {
    verifyAppFailureIfLoopback(CLUSTER_NODE_HOST.getKey());
    verifyAppFailureIfLoopback(CLUSTER_SEARCH_HOSTS.getKey());
    verifyAppFailureIfLoopback(CLUSTER_HOSTS.getKey());
  }

  private void verifySearchFailureIfLoopback(String propertyKey) {
    TestAppSettings settings = newSettingsForSearchNode();
    verifyFailure(propertyKey, settings);
  }

  private void verifyAppFailureIfLoopback(String propertyKey) {
    TestAppSettings settings = newSettingsForAppNode();
    verifyFailure(propertyKey, settings);
  }

  private void verifyFailure(String propertyKey, TestAppSettings settings) {
    settings.set(propertyKey, loopback.getHostAddress());

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Property " + propertyKey + " must be a local non-loopback address: " + loopback.getHostAddress());

    new ClusterSettings(network).accept(settings.getProps());
  }

  private TestAppSettings newSettingsForAppNode() {
    return new TestAppSettings()
      .set(CLUSTER_ENABLED.getKey(), "true")
      .set(CLUSTER_NODE_TYPE.getKey(), "application")
      .set(CLUSTER_NODE_HOST.getKey(), nonLoopbackLocal.getHostAddress())
      .set(CLUSTER_HOSTS.getKey(), nonLoopbackLocal.getHostAddress())
      .set(CLUSTER_SEARCH_HOSTS.getKey(), nonLoopbackLocal.getHostAddress())
      .set("sonar.auth.jwtBase64Hs256Secret", "abcde")
      .set(JDBC_URL.getKey(), "jdbc:mysql://localhost:3306/sonar");
  }

  private TestAppSettings newSettingsForSearchNode() {
    return new TestAppSettings()
      .set(CLUSTER_ENABLED.getKey(), "true")
      .set(CLUSTER_NODE_TYPE.getKey(), "search")
      .set(CLUSTER_NODE_HOST.getKey(), nonLoopbackLocal.getHostAddress())
      .set(CLUSTER_HOSTS.getKey(), nonLoopbackLocal.getHostAddress())
      .set(CLUSTER_SEARCH_HOSTS.getKey(), nonLoopbackLocal.getHostAddress())
      .set(SEARCH_HOST.getKey(), nonLoopbackLocal.getHostAddress());
  }
}
