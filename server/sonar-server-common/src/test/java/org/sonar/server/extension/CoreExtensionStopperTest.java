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
package org.sonar.server.extension;

import org.junit.Test;
import org.sonar.core.platform.ComponentContainer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class CoreExtensionStopperTest {
  private ComponentContainer componentContainer = new ComponentContainer();
  private CoreExtensionBridge bridge = mock(CoreExtensionBridge.class);

  private CoreExtensionStopper underTest = new CoreExtensionStopper(componentContainer);

  @Test
  public void stop_calls_stopPlugin_if_Bridge_exists_in_container() {
    componentContainer.add(bridge);
    componentContainer.startComponents();

    underTest.stop();

    verify(bridge).getPluginName();
    verify(bridge).stopPlugin();
    verifyNoMoreInteractions(bridge);
  }

  @Test
  public void stop_does_not_call_stopPlugin_if_Bridge_does_not_exist_in_container() {
    underTest.stop();

    verifyNoMoreInteractions(bridge);
  }
}
