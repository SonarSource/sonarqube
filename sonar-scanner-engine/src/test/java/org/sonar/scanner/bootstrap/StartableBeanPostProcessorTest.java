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
package org.sonar.scanner.bootstrap;

import org.junit.Test;
import org.picocontainer.Startable;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class StartableBeanPostProcessorTest {
  private final StartableBeanPostProcessor underTest = new StartableBeanPostProcessor();

  @Test
  public void starts_pico_startable() {
    Startable startable = mock(Startable.class);
    underTest.postProcessBeforeInitialization(startable, "startable");
    verify(startable).start();
    verifyNoMoreInteractions(startable);
  }

  @Test
  public void starts_api_startable() {
    org.sonar.api.Startable startable = mock(org.sonar.api.Startable.class);
    underTest.postProcessBeforeInitialization(startable, "startable");
    verify(startable).start();
    verifyNoMoreInteractions(startable);
  }

  @Test
  public void stops_pico_startable() {
    Startable startable = mock(Startable.class);
    underTest.postProcessBeforeDestruction(startable, "startable");
    verify(startable).stop();
    verifyNoMoreInteractions(startable);
  }

  @Test
  public void stops_api_startable() {
    org.sonar.api.Startable startable = mock(org.sonar.api.Startable.class);
    underTest.postProcessBeforeDestruction(startable, "startable");
    verify(startable).stop();
    verifyNoMoreInteractions(startable);
  }

}
