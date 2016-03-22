/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.governance.bridge;

import org.junit.Test;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.governance.GovernanceBridge;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class GovernanceStopperTest {
  private ComponentContainer componentContainer = spy(new ComponentContainer());
  private GovernanceStopper underTest = new GovernanceStopper(componentContainer);

  @Test
  public void start_has_no_effect() {
    underTest.start();
    
    verifyNoMoreInteractions(componentContainer);
  }

  @Test
  public void stop_has_no_effect_when_no_GovernanceBridge() {
    underTest.stop();

    verify(componentContainer).getComponentByType(GovernanceBridge.class);
    verifyNoMoreInteractions(componentContainer);
  }

  @Test
  public void stop_calls_GovernanceBridge_stopGovernance_when_in_ComponentContainer() {
    GovernanceBridge governanceBridge = mock(GovernanceBridge.class);

    componentContainer.add(governanceBridge);

    underTest.stop();

    verify(governanceBridge).stopGovernance();
  }
}
