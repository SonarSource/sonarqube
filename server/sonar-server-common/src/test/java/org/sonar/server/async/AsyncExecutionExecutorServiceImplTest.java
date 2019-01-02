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
package org.sonar.server.async;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AsyncExecutionExecutorServiceImplTest {
  private AsyncExecutionExecutorServiceImpl underTest = new AsyncExecutionExecutorServiceImpl();

  @Test
  public void submit_executes_runnable_in_another_thread() {
    try (SlowRunnable slowRunnable = new SlowRunnable()) {
      underTest.submit(slowRunnable);
      assertThat(slowRunnable.executed).isFalse();
    }
  }

  private static final class SlowRunnable implements Runnable, AutoCloseable {
    private final CountDownLatch latch = new CountDownLatch(1);
    private volatile boolean executed = false;

    @Override
    public void run() {
      try {
        latch.await(30, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        // ignore
      }
      executed = true;
    }

    @Override
    public void close() {
      latch.countDown();
    }
  }
}
