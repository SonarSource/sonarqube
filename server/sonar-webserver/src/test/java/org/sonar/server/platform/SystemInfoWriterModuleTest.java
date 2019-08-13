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
package org.sonar.server.platform;

import java.util.Collection;
import org.junit.Test;
import org.picocontainer.ComponentAdapter;
import org.sonar.core.platform.ComponentContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.platform.ComponentContainer.COMPONENTS_IN_EMPTY_COMPONENT_CONTAINER;

public class SystemInfoWriterModuleTest {
  private WebServer webServer = mock(WebServer.class);
  private SystemInfoWriterModule underTest = new SystemInfoWriterModule(webServer);

  @Test
  public void verify_system_info_configuration_in_cluster_mode() {
    when(webServer.isStandalone()).thenReturn(false);
    ComponentContainer container = new ComponentContainer();

    underTest.configure(container);

    Collection<ComponentAdapter<?>> adapters = container.getPicoContainer().getComponentAdapters();
    assertThat(adapters)
      .hasSize(COMPONENTS_IN_EMPTY_COMPONENT_CONTAINER + 17);
  }

  @Test
  public void verify_system_info_configuration_in_standalone_mode() {
    when(webServer.isStandalone()).thenReturn(true);
    ComponentContainer container = new ComponentContainer();

    underTest.configure(container);

    verifyConfigurationStandaloneSQ(container);
  }

  public void verifyConfigurationStandaloneSQ(ComponentContainer container) {
    Collection<ComponentAdapter<?>> adapters = container.getPicoContainer().getComponentAdapters();
    assertThat(adapters)
      .hasSize(COMPONENTS_IN_EMPTY_COMPONENT_CONTAINER + 11);
  }

}
