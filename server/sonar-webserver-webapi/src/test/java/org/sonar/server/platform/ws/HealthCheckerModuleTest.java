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
package org.sonar.server.platform.ws;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.junit.Test;
import org.picocontainer.ComponentAdapter;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.health.AppNodeClusterCheck;
import org.sonar.server.health.CeStatusNodeCheck;
import org.sonar.server.health.ClusterHealthCheck;
import org.sonar.server.health.DbConnectionNodeCheck;
import org.sonar.server.health.EsStatusClusterCheck;
import org.sonar.server.health.EsStatusNodeCheck;
import org.sonar.server.health.HealthCheckerImpl;
import org.sonar.server.health.NodeHealthCheck;
import org.sonar.server.health.WebServerStatusNodeCheck;
import org.sonar.server.platform.WebServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HealthCheckerModuleTest {
  private WebServer webServer = mock(WebServer.class);
  private HealthCheckerModule underTest = new HealthCheckerModule(webServer);

  @Test
  public void verify_HealthChecker() {
    boolean standalone = new Random().nextBoolean();
    when(webServer.isStandalone()).thenReturn(standalone);
    ComponentContainer container = new ComponentContainer();

    underTest.configure(container);

    assertThat(classesAddedToContainer(container))
      .describedAs("Verifying action and HealthChecker with standalone=%s", standalone)
      .contains(HealthCheckerImpl.class)
      .doesNotContain(HealthActionSupport.class)
      .doesNotContain(HealthAction.class)
      .doesNotContain(SafeModeHealthAction.class);
  }

  @Test
  public void verify_installed_NodeHealthChecks_implementations_when_standalone() {
    when(webServer.isStandalone()).thenReturn(true);
    ComponentContainer container = new ComponentContainer();

    underTest.configure(container);

    List<Class<?>> checks = classesAddedToContainer(container).stream().filter(NodeHealthCheck.class::isAssignableFrom).collect(Collectors.toList());
    assertThat(checks)
      .hasSize(4)
      .contains(WebServerStatusNodeCheck.class)
      .contains(DbConnectionNodeCheck.class)
      .contains(EsStatusNodeCheck.class)
      .contains(CeStatusNodeCheck.class);
  }

  @Test
  public void verify_installed_NodeHealthChecks_implementations_when_clustered() {
    when(webServer.isStandalone()).thenReturn(false);
    ComponentContainer container = new ComponentContainer();

    underTest.configure(container);

    List<Class<?>> checks = classesAddedToContainer(container).stream().filter(NodeHealthCheck.class::isAssignableFrom).collect(Collectors.toList());
    assertThat(checks)
      .hasSize(3)
      .contains(WebServerStatusNodeCheck.class)
      .contains(DbConnectionNodeCheck.class)
      .contains(CeStatusNodeCheck.class)
      .doesNotContain(EsStatusNodeCheck.class);
  }

  @Test
  public void verify_installed_ClusterHealthChecks_implementations_in_standalone() {
    when(webServer.isStandalone()).thenReturn(true);
    ComponentContainer container = new ComponentContainer();

    underTest.configure(container);

    List<Class<?>> checks = classesAddedToContainer(container).stream().filter(ClusterHealthCheck.class::isAssignableFrom).collect(Collectors.toList());
    assertThat(checks).isEmpty();
  }

  @Test
  public void verify_installed_ClusterHealthChecks_implementations_in_clustering() {
    when(webServer.isStandalone()).thenReturn(false);
    ComponentContainer container = new ComponentContainer();

    underTest.configure(container);

    List<Class<?>> checks = classesAddedToContainer(container).stream().filter(ClusterHealthCheck.class::isAssignableFrom).collect(Collectors.toList());
    assertThat(checks)
      .hasSize(2)
      .contains(EsStatusClusterCheck.class)
      .contains(AppNodeClusterCheck.class);
  }

  private List<Class<?>> classesAddedToContainer(ComponentContainer container) {
    Collection<ComponentAdapter<?>> componentAdapters = container.getPicoContainer().getComponentAdapters();
    return componentAdapters.stream().map(ComponentAdapter::getComponentImplementation).collect(Collectors.toList());
  }
}
