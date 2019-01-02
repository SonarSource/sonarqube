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
package org.sonar.server.platform.monitoring;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Collection;
import javax.annotation.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.picocontainer.ComponentAdapter;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.platform.WebServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.platform.ComponentContainer.COMPONENTS_IN_EMPTY_COMPONENT_CONTAINER;

@RunWith(DataProviderRunner.class)
public class WebSystemInfoModuleTest {
  private WebServer webServer = mock(WebServer.class);
  private MapSettings settings = new MapSettings();
  private WebSystemInfoModule underTest = new WebSystemInfoModule(settings.asConfig(), webServer);

  @Test
  @UseDataProvider("notOnSonarCloud")
  public void verify_system_info_configuration_in_cluster_mode(@Nullable Boolean notOnSonarCloud) {
    when(webServer.isStandalone()).thenReturn(false);
    if (notOnSonarCloud != null) {
      settings.setProperty("sonar.sonarcloud.enabled", notOnSonarCloud);
    }
    ComponentContainer container = new ComponentContainer();

    underTest.configure(container);

    Collection<ComponentAdapter<?>> adapters = container.getPicoContainer().getComponentAdapters();
    assertThat(adapters)
      .hasSize(COMPONENTS_IN_EMPTY_COMPONENT_CONTAINER + 18);
  }

  @Test
  @UseDataProvider("notOnSonarCloud")
  public void verify_system_info_configuration_in_standalone_mode(@Nullable Boolean notOnSonarCloud) {
    when(webServer.isStandalone()).thenReturn(true);
    if (notOnSonarCloud != null) {
      settings.setProperty("sonar.sonarcloud.enabled", notOnSonarCloud);
    }
    ComponentContainer container = new ComponentContainer();

    underTest.configure(container);

    verifyConfigurationStandaloneSQ(container);
  }

  @Test
  public void verify_system_info_configuration_on_SonarCloud() {
    when(webServer.isStandalone()).thenReturn(false);
    settings.setProperty("sonar.sonarcloud.enabled", true);
    ComponentContainer container = new ComponentContainer();

    underTest.configure(container);

    verifyConfigurationStandaloneSQ(container);
  }

  public void verifyConfigurationStandaloneSQ(ComponentContainer container) {
    Collection<ComponentAdapter<?>> adapters = container.getPicoContainer().getComponentAdapters();
    assertThat(adapters)
      .hasSize(COMPONENTS_IN_EMPTY_COMPONENT_CONTAINER + 12);
  }

  @DataProvider
  public static Object[][] notOnSonarCloud() {
    return new Object[][] {
      {null},
      {false}
    };
  }
}
