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
package org.sonar.server.organization.ws;

import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.core.platform.ComponentContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.platform.ComponentContainer.COMPONENTS_IN_EMPTY_COMPONENT_CONTAINER;

public class OrganizationsWsModuleTest {

  private ComponentContainer container = new ComponentContainer();
  private MapSettings mapSettings = new MapSettings();
  private OrganizationsWsModule underTest = new OrganizationsWsModule(mapSettings.asConfig());

  @Test
  public void verify_component_count_when_on_SonarQube() {
    mapSettings.setProperty("sonar.sonarcloud.enabled", false);

    underTest.configure(container);

    assertThat(container.getPicoContainer().getComponentAdapters())
      .hasSize(COMPONENTS_IN_EMPTY_COMPONENT_CONTAINER + 5);
  }

  @Test
  public void verify_component_count_when_on_SonarCloud() {
    mapSettings.setProperty("sonar.sonarcloud.enabled", true);

    underTest.configure(container);

    assertThat(container.getPicoContainer().getComponentAdapters())
      .hasSize(COMPONENTS_IN_EMPTY_COMPONENT_CONTAINER + 12);
  }

}
