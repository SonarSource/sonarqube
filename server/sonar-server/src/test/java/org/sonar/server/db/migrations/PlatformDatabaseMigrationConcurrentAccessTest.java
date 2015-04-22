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
package org.sonar.server.db.migrations;

import com.google.common.base.Throwables;
import org.junit.After;
import org.junit.Test;
import org.sonar.server.ruby.RubyBridge;
import org.sonar.server.ruby.RubyDatabaseMigration;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlatformDatabaseMigrationConcurrentAccessTest {

  private ExecutorService pool = Executors.newFixedThreadPool(2);
  /**
   * Latch is used to make sure both testing threads try and call {@link PlatformDatabaseMigration#startIt()} at the
   * same time
   */
  private CountDownLatch latch = new CountDownLatch(2);

  /**
   * Implementation of execute runs Runnable synchronously
   */
  private PlatformDatabaseMigrationExecutorService executorService = new PlatformDatabaseMigrationExecutorServiceAdaptor() {
    @Override
    public void execute(Runnable command) {
      command.run();
    }
  };
  /**
   * thread-safe counter of calls to the trigger method of {@link #rubyDatabaseMigration}
   */
  private AtomicInteger triggerCount = new AtomicInteger();
  /**
   * Implementation of RubyDatabaseMigration which trigger method increments a thread-safe counter and add a delay of 200ms
   */
  private RubyDatabaseMigration rubyDatabaseMigration = new RubyDatabaseMigration() {
    @Override
    public void trigger() {
      triggerCount.incrementAndGet();
      try {
        Thread.currentThread().sleep(200);
      } catch (InterruptedException e) {
        Throwables.propagate(e);
      }
    }
  };
  private RubyBridge rubyBridge = mock(RubyBridge.class);
  private PlatformDatabaseMigration underTest = new PlatformDatabaseMigration(rubyBridge, executorService);

  @After
  public void tearDown() throws Exception {
    pool.shutdownNow();
  }

  @Test
  public void two_concurrent_calls_to_startit_call_trigger_only_once() throws Exception {
    when(rubyBridge.databaseMigration()).thenReturn(rubyDatabaseMigration);

    pool.submit(new CallStartit());
    pool.submit(new CallStartit());

    pool.awaitTermination(3, TimeUnit.SECONDS);

    assertThat(triggerCount.get()).isEqualTo(1);
  }

  private class CallStartit implements Runnable {
    @Override
    public void run() {
      latch.countDown();
      try {
        latch.await();
      } catch (InterruptedException e) {
        // propagate interruption
        Thread.currentThread().interrupt();
      }
      underTest.startIt();
    }
  }
}
