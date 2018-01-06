/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration;

import com.google.common.base.Throwables;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Test;
import org.sonar.server.platform.Platform;
import org.sonar.server.platform.db.migration.engine.MigrationEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DatabaseMigrationImplConcurrentAccessTest {

  private ExecutorService pool = Executors.newFixedThreadPool(2);
  /**
   * Latch is used to make sure both testing threads try and call {@link DatabaseMigrationImpl#startIt()} at the
   * same time
   */
  private CountDownLatch latch = new CountDownLatch(2);

  /**
   * Implementation of execute runs Runnable synchronously
   */
  private DatabaseMigrationExecutorService executorService = new DatabaseMigrationExecutorServiceAdaptor() {
    @Override
    public void execute(Runnable command) {
      command.run();
    }
  };
  private AtomicInteger triggerCount = new AtomicInteger();
  private MigrationEngine incrementingMigrationEngine = new MigrationEngine() {
    @Override
    public void execute() {
      // need execute to consume some time to avoid UT to fail because it ran too fast and threads never executed concurrently
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        Throwables.propagate(e);
      }
      triggerCount.incrementAndGet();
    }
  };
  private MutableDatabaseMigrationState migrationState = mock(MutableDatabaseMigrationState.class);
  private Platform platform = mock(Platform.class);
  private DatabaseMigrationImpl underTest = new DatabaseMigrationImpl(executorService, migrationState, incrementingMigrationEngine, platform);

  @After
  public void tearDown() {
    pool.shutdownNow();
  }

  @Test
  public void two_concurrent_calls_to_startit_call_migration_engine_only_once() throws Exception {
    pool.submit(new CallStartit());
    pool.submit(new CallStartit());

    pool.awaitTermination(2, TimeUnit.SECONDS);

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
