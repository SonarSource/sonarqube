/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.devcockpit.bridge;

import org.junit.Test;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.devcockpit.DevCockpitBridge;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class DevCockpitStopperTest {
  private ComponentContainer componentContainer = new ComponentContainer();
  private DevCockpitBridge devCockpitBridge = mock(DevCockpitBridge.class);

  private DevCockpitStopper underTest = new DevCockpitStopper(componentContainer);

  @Test
  public void stop_calls_stopDevCockpit_if_DevCockpitBridge_exists_in_container() {
    componentContainer.add(devCockpitBridge);
    componentContainer.startComponents();

    underTest.stop();


    verify(devCockpitBridge).stopDevCockpit();
    verifyNoMoreInteractions(devCockpitBridge);
  }

  @Test
  public void stop_does_not_call_stopDevCockpit_if_DevCockpitBridge_does_not_exist_in_container() {
    underTest.stop();

    verifyNoMoreInteractions(devCockpitBridge);
  }
}
