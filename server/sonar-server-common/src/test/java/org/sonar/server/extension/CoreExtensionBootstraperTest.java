/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.extension;

import org.junit.Test;
import org.sonar.api.platform.Server;
import org.sonar.core.platform.SpringComponentContainer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class CoreExtensionBootstraperTest {
  private final SpringComponentContainer componentContainer = new SpringComponentContainer();
  private final CoreExtensionBridge bridge = mock(CoreExtensionBridge.class);
  private final CoreExtensionBootstraper underTest = new CoreExtensionBootstraper(componentContainer);

  @Test
  public void onServerStart_calls_startPlugin_if_Bridge_exists_in_container() {
    componentContainer.add(bridge);
    componentContainer.startComponents();

    underTest.onServerStart(mock(Server.class));

    verify(bridge).getPluginName();
    verify(bridge).startPlugin(componentContainer);
    verifyNoMoreInteractions(bridge);
  }

  @Test
  public void onServerStart_does_not_call_startPlugin_if_Bridge_does_not_exist_in_container() {
    componentContainer.startComponents();
    underTest.onServerStart(mock(Server.class));

    verifyNoMoreInteractions(bridge);
  }
}
