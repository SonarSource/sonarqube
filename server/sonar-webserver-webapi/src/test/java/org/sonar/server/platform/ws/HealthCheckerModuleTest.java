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
package org.sonar.server.platform.ws;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.junit.Test;
import org.sonar.core.platform.ListContainer;
import org.sonar.server.common.health.AppNodeClusterCheck;
import org.sonar.server.common.health.CeStatusNodeCheck;
import org.sonar.server.common.health.ClusterHealthCheck;
import org.sonar.server.common.health.DbConnectionNodeCheck;
import org.sonar.server.common.health.EsStatusClusterCheck;
import org.sonar.server.common.health.EsStatusNodeCheck;
import org.sonar.server.common.health.NodeHealthCheck;
import org.sonar.server.common.health.WebServerStatusNodeCheck;
import org.sonar.server.health.HealthCheckerImpl;
import org.sonar.server.platform.NodeInformation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HealthCheckerModuleTest {
  private final NodeInformation nodeInformation = mock(NodeInformation.class);
  private final HealthCheckerModule underTest = new HealthCheckerModule(nodeInformation);

  @Test
  public void verify_HealthChecker() {
    boolean standalone = new Random().nextBoolean();
    when(nodeInformation.isStandalone()).thenReturn(standalone);
    ListContainer container = new ListContainer();

    underTest.configure(container);

    assertThat(container.getAddedObjects())
      .describedAs("Verifying action and HealthChecker with standalone=%s", standalone)
      .contains(HealthCheckerImpl.class)
      .doesNotContain(HealthActionSupport.class)
      .doesNotContain(HealthAction.class)
      .doesNotContain(SafeModeHealthAction.class);
  }

  @Test
  public void verify_installed_NodeHealthChecks_implementations_when_standalone() {
    when(nodeInformation.isStandalone()).thenReturn(true);
    ListContainer container = new ListContainer();

    underTest.configure(container);

    List<Class<?>> checks = container.getAddedObjects().stream()
      .filter(Class.class::isInstance)
      .map(o -> (Class<?>) o)
      .filter(NodeHealthCheck.class::isAssignableFrom).collect(Collectors.toList());
    assertThat(checks).containsOnly(WebServerStatusNodeCheck.class, DbConnectionNodeCheck.class, EsStatusNodeCheck.class, CeStatusNodeCheck.class);
  }

  @Test
  public void verify_installed_NodeHealthChecks_implementations_when_clustered() {
    when(nodeInformation.isStandalone()).thenReturn(false);
    ListContainer container = new ListContainer();

    underTest.configure(container);

    List<Class<?>> checks = container.getAddedObjects().stream()
      .filter(Class.class::isInstance)
      .map(o -> (Class<?>) o)
      .filter(NodeHealthCheck.class::isAssignableFrom).collect(Collectors.toList());
    assertThat(checks).containsOnly(WebServerStatusNodeCheck.class, DbConnectionNodeCheck.class, CeStatusNodeCheck.class);
  }

  @Test
  public void verify_installed_ClusterHealthChecks_implementations_in_standalone() {
    when(nodeInformation.isStandalone()).thenReturn(true);
    ListContainer container = new ListContainer();

    underTest.configure(container);

    List<Class<?>> checks = container.getAddedObjects().stream()
      .filter(o -> o instanceof Class<?>)
      .map(o -> (Class<?>) o)
      .filter(ClusterHealthCheck.class::isAssignableFrom).collect(Collectors.toList());
    assertThat(checks).isEmpty();
  }

  @Test
  public void verify_installed_ClusterHealthChecks_implementations_in_clustering() {
    when(nodeInformation.isStandalone()).thenReturn(false);
    ListContainer container = new ListContainer();

    underTest.configure(container);

    List<Class<?>> checks = container.getAddedObjects().stream()
      .filter(o -> o instanceof Class<?>)
      .map(o -> (Class<?>) o)
      .filter(ClusterHealthCheck.class::isAssignableFrom).collect(Collectors.toList());
    assertThat(checks).containsOnly(EsStatusClusterCheck.class, AppNodeClusterCheck.class);
  }
}
