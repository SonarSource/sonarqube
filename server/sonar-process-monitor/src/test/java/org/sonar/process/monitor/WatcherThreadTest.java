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
package org.sonar.process.monitor;

import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

public class WatcherThreadTest {

  @Test(timeout = 10000L)
  public void continue_even_if_interrupted() throws Exception {
    Monitor monitor = mock(Monitor.class);
    ProcessRef ref = mock(ProcessRef.class, Mockito.RETURNS_DEEP_STUBS);
    when(ref.getProcess().waitFor()).thenThrow(new InterruptedException()).thenReturn(0);
    WatcherThread watcher = new WatcherThread(ref, monitor);
    watcher.start();
    watcher.join();
    verify(monitor).stopAsync();
  }
}
