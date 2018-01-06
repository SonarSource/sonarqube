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
package org.sonar.server.platform.ws;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.picocontainer.ComponentAdapter;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.health.DbConnectionNodeCheck;
import org.sonar.server.health.EsStatusNodeCheck;
import org.sonar.server.health.HealthCheckerImpl;
import org.sonar.server.health.NodeHealthCheck;
import org.sonar.server.health.WebServerSafemodeNodeCheck;

import static org.assertj.core.api.Assertions.assertThat;

public class SafeModeHealthActionModuleTest {
  private SafeModeHealthActionModule underTest = new SafeModeHealthActionModule();

  @Test
  public void verify_action_and_HealthChecker() {
    ComponentContainer container = new ComponentContainer();

    underTest.configure(container);

    assertThat(classesAddedToContainer(container))
      .contains(HealthCheckerImpl.class)
      .contains(HealthActionSupport.class)
      .contains(SafeModeHealthAction.class)
      .doesNotContain(HealthAction.class);
  }

  @Test
  public void verify_installed_HealthChecks_implementations() {
    ComponentContainer container = new ComponentContainer();

    underTest.configure(container);

    List<Class<?>> checks = classesAddedToContainer(container).stream().filter(NodeHealthCheck.class::isAssignableFrom).collect(Collectors.toList());
    assertThat(checks)
      .hasSize(3)
      .contains(WebServerSafemodeNodeCheck.class)
      .contains(DbConnectionNodeCheck.class)
      .contains(EsStatusNodeCheck.class);
  }

  private List<Class<?>> classesAddedToContainer(ComponentContainer container) {
    Collection<ComponentAdapter<?>> componentAdapters = container.getPicoContainer().getComponentAdapters();
    return componentAdapters.stream().map(ComponentAdapter::getComponentImplementation).collect(Collectors.toList());
  }

}
