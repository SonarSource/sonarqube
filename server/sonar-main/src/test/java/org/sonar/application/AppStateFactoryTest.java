/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.application;

import com.google.common.collect.ImmutableMap;
import java.net.InetAddress;
import java.util.Optional;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.sonar.application.cluster.ClusterAppStateImpl;
import org.sonar.application.config.TestAppSettings;
import org.sonar.process.NetworkUtilsImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeThat;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_HZ_HOSTS;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NAME;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_HOST;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_TYPE;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_SEARCH_HOSTS;

public class AppStateFactoryTest {

  @Test
  public void create_cluster_implementation_if_cluster_is_enabled() {
    Optional<InetAddress> ip = NetworkUtilsImpl.INSTANCE.getLocalNonLoopbackIpv4Address();
    assumeThat(ip.isPresent(), CoreMatchers.is(true));
    TestAppSettings settings = new TestAppSettings(ImmutableMap.<String, String>builder()
      .put(CLUSTER_ENABLED.getKey(), "true")
      .put(CLUSTER_NODE_TYPE.getKey(), "application")
      .put(CLUSTER_NODE_HOST.getKey(), ip.get().getHostAddress())
      .put(CLUSTER_HZ_HOSTS.getKey(), ip.get().getHostAddress())
      .put(CLUSTER_NAME.getKey(), "foo")
      .put(CLUSTER_SEARCH_HOSTS.getKey(), "localhost:9002")
      .build());
    AppStateFactory underTest = new AppStateFactory(settings);

    AppState appState = underTest.create();
    assertThat(appState).isInstanceOf(ClusterAppStateImpl.class);
    appState.close();
  }

  @Test
  public void cluster_implementation_is_disabled_by_default() {
    TestAppSettings settings = new TestAppSettings();
    AppStateFactory underTest = new AppStateFactory(settings);

    assertThat(underTest.create()).isInstanceOf(AppStateImpl.class);
  }
}
