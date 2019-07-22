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
package org.sonar.server.project.ws;

import org.junit.Test;
import org.sonar.api.config.internal.ConfigurationBridge;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.process.ProcessProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.platform.ComponentContainer.COMPONENTS_IN_EMPTY_COMPONENT_CONTAINER;

public class ProjectsWsModuleTest {

  private MapSettings settings = new MapSettings();

  @Test
  public void verify_count_of_added_components_on_SonarQube() {
    ComponentContainer container = new ComponentContainer();
    new ProjectsWsModule(new ConfigurationBridge(settings)).configure(container);
    assertThat(container.size()).isEqualTo(COMPONENTS_IN_EMPTY_COMPONENT_CONTAINER + 12);
  }

  @Test
  public void verify_count_of_added_components_on_SonarCloud() {
    ComponentContainer container = new ComponentContainer();
    settings.setProperty(ProcessProperties.Property.SONARCLOUD_ENABLED.getKey(), true);

    new ProjectsWsModule(new ConfigurationBridge(settings)).configure(container);
    assertThat(container.size()).isEqualTo(COMPONENTS_IN_EMPTY_COMPONENT_CONTAINER + 11);
  }

}
