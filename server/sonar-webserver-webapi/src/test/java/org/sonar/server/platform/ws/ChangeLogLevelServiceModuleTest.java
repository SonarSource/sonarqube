/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import org.junit.Test;
import org.sonar.core.platform.ListContainer;
import org.sonar.server.platform.WebServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChangeLogLevelServiceModuleTest {
  private final WebServer webServer = mock(WebServer.class);
  private final ChangeLogLevelServiceModule underTest = new ChangeLogLevelServiceModule(webServer);

  @Test
  public void provide_returns_ChangeLogLevelClusterService_if_cluster_not_on_SonarCloud() {
    when(webServer.isStandalone()).thenReturn(false);
    ListContainer container = new ListContainer();

    underTest.configure(container);

    assertThat(container.getAddedObjects()).containsOnly(ChangeLogLevelClusterService.class);
  }

  @Test
  public void provide_returns_ChangeLogLevelStandaloneService_if_SQ_standalone() {
    when(webServer.isStandalone()).thenReturn(true);
    ListContainer container = new ListContainer();

    underTest.configure(container);

    verifyInStandaloneSQ(container);
  }

  private void verifyInStandaloneSQ(ListContainer container) {
    assertThat(container.getAddedObjects()).containsOnly(ChangeLogLevelStandaloneService.class);
  }
}
