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
public class ChangeLogLevelActionModuleTest {
  private WebServer webServer = mock(WebServer.class);
  private MapSettings settings = new MapSettings();
  private ChangeLogLevelActionModule underTest = new ChangeLogLevelActionModule(webServer, settings.asConfig());

  @Test
  @UseDataProvider("notOnSonarCloud")
  public void provide_returns_ChangeLogLevelClusterService_if_cluster_not_on_SonarCloud(@Nullable Boolean sonarcloudOrNot) {
    when(webServer.isStandalone()).thenReturn(false);
    if (sonarcloudOrNot != null) {
      settings.setProperty("sonar.sonarcloud.enabled", sonarcloudOrNot);
    }
    ComponentContainer container = new ComponentContainer();

    underTest.configure(container);

    Collection<ComponentAdapter<?>> adapters = container.getPicoContainer().getComponentAdapters();
    assertThat(adapters)
      .hasSize(COMPONENTS_IN_EMPTY_COMPONENT_CONTAINER + 2)
      .extracting(ComponentAdapter::getComponentKey)
      .contains(ChangeLogLevelClusterService.class, ChangeLogLevelAction.class)
      .doesNotContain(ChangeLogLevelStandaloneService.class);
  }

  @Test
  public void provide_returns_ChangeLogLevelStandaloneService_on_SonarCloud() {
    when(webServer.isStandalone()).thenReturn(false);
    settings.setProperty("sonar.sonarcloud.enabled", true);
    ComponentContainer container = new ComponentContainer();

    underTest.configure(container);

    verifyInStandaloneSQ(container);
  }

  @Test
  @UseDataProvider("notOnSonarCloud")
  public void provide_returns_ChangeLogLevelStandaloneService_if_SQ_standalone(@Nullable Boolean sonarcloudOrNot) {
    when(webServer.isStandalone()).thenReturn(true);
    if (sonarcloudOrNot != null) {
      settings.setProperty("sonar.sonarcloud.enabled", sonarcloudOrNot);
    }
    ComponentContainer container = new ComponentContainer();

    underTest.configure(container);

    verifyInStandaloneSQ(container);
  }

  private void verifyInStandaloneSQ(ComponentContainer container) {
    Collection<ComponentAdapter<?>> adapters = container.getPicoContainer().getComponentAdapters();
    assertThat(adapters)
      .hasSize(COMPONENTS_IN_EMPTY_COMPONENT_CONTAINER + 2)
      .extracting(ComponentAdapter::getComponentKey)
      .contains(ChangeLogLevelStandaloneService.class, ChangeLogLevelAction.class)
      .doesNotContain(ChangeLogLevelClusterService.class);
  }

  @DataProvider
  public static Object[][] notOnSonarCloud() {
    return new Object[][] {
      {null},
      {false}
    };
  }


}
