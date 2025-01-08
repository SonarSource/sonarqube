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
package org.sonar.server.platform;

import org.junit.Test;
import org.sonar.core.platform.ListContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SystemInfoWriterModuleTest {
  private final NodeInformation nodeInformation = mock(NodeInformation.class);
  private final SystemInfoWriterModule underTest = new SystemInfoWriterModule(nodeInformation);

  @Test
  public void verify_system_info_configuration_in_cluster_mode() {
    when(nodeInformation.isStandalone()).thenReturn(false);
    ListContainer container = new ListContainer();
    underTest.configure(container);
    assertThat(container.getAddedObjects()).hasSize(22);
  }

  @Test
  public void verify_system_info_configuration_in_standalone_mode() {
    when(nodeInformation.isStandalone()).thenReturn(true);

    ListContainer container = new ListContainer();
    underTest.configure(container);
    assertThat(container.getAddedObjects()).hasSize(16);
  }
}
