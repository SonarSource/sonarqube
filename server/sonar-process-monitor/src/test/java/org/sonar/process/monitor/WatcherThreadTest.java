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
package org.sonar.process.monitor;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WatcherThreadTest {

  @Test(timeout = 10000L)
  public void kill_process_if_watcher_is_interrupted() throws Exception {
    ProcessRef ref = mock(ProcessRef.class, Mockito.RETURNS_DEEP_STUBS);
    final AtomicBoolean waiting = new AtomicBoolean(false);
    when(ref.getProcess().waitFor()).thenAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        waiting.set(true);
        Thread.sleep(Long.MAX_VALUE);
        return 0;
      }
    });
    Monitor monitor = mock(Monitor.class);

    WatcherThread watcher = new WatcherThread(ref, monitor);
    watcher.start();

    while (!waiting.get()) {
      Thread.sleep(50L);
    }
    watcher.interrupt();
    verify(ref).hardKill();
  }
}
