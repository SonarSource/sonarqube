/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.process;

import com.google.common.annotations.VisibleForTesting;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for tasks that run on Java 21 virtual threads.
 * Provides common functionality for managing virtual thread lifecycle.
 */
public abstract class VirtualThreadTask implements Runnable {
  private final AtomicReference<Thread> virtualThread = new AtomicReference<>();

  protected void startVirtualThread(String threadName) {
    Thread newThread = Thread.ofVirtual()
      .name(threadName)
      .start(this);

    if (!virtualThread.compareAndSet(null, newThread)) {
      // Another thread already started, interrupt the thread we just created
      newThread.interrupt();
      throw new IllegalThreadStateException("Virtual thread already started");
    }
  }

  public void interrupt() {
    Thread thread = virtualThread.get();
    if (thread != null) {
      thread.interrupt();
    }
  }

  @VisibleForTesting
  public boolean isAlive() {
    Thread thread = virtualThread.get();
    return thread != null && thread.isAlive();
  }

  @VisibleForTesting
  public String getThreadName() {
    Thread thread = virtualThread.get();
    return thread != null ? thread.getName() : null;
  }
}
