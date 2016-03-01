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
package org.sonar.server.computation.taskprocessor;

import java.util.HashSet;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.sonar.server.computation.configuration.CeConfigurationRule;

import static org.assertj.core.api.Assertions.assertThat;

public class CeProcessingSchedulerImplMultiThreadTest {

  @Rule
  // due to risks of infinite chaining of tasks/futures, a timeout is required for safety
  public Timeout timeout = Timeout.seconds(60);
  @Rule
  public CeConfigurationRule ceConfiguration = new CeConfigurationRule().setWorkerCount(4);

  private ThreadNameRecordingCeWorkerCallable ceWorkerRunnable = new ThreadNameRecordingCeWorkerCallable();

  @Test
  public void when_workerCount_is_more_than_1_CeWorkerCallable_runs_on_as_many_different_threads() throws InterruptedException {
    int workerCount = 4;

    ceConfiguration
        .setWorkerCount(workerCount)
        // reduce queue polling delay to not wait for processing to start
        .setQueuePollingDelay(10);

    CeProcessingSchedulerExecutorService processingExecutorService = null;
    CeProcessingSchedulerImpl underTest = null;
    try {
      processingExecutorService = new CeProcessingSchedulerExecutorServiceImpl(ceConfiguration);
      underTest = new CeProcessingSchedulerImpl(ceConfiguration, processingExecutorService, ceWorkerRunnable);

      underTest.startScheduling();

      // scheduling starts only after 10ms, leave 100ms for the tasks to run
      Thread.sleep(100);

      assertThat(ceWorkerRunnable.getThreadNames()).hasSize(workerCount);
    }
    finally {
      if (underTest != null) {
        underTest.stop();
      }
      if (processingExecutorService != null) {
        processingExecutorService.stop();
      }
    }
  }

  private static class ThreadNameRecordingCeWorkerCallable implements CeWorkerCallable {
    private final Set<String> threadNames = new HashSet<>();
    private final int maxCallCount = 10;
    private int callCount = 0;

    @Override
    public Boolean call() throws Exception {
      String name = Thread.currentThread().getName();
      threadNames.add(name);
      callCount++;
      // after maxCallCount calls, wait between each call
      return callCount < maxCallCount;
    }

    public Set<String> getThreadNames() {
      return threadNames;
    }
  }
}
