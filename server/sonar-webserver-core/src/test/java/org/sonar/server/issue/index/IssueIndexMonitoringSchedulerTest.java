/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.issue.index;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IssueIndexMonitoringSchedulerTest {

  @Test
  void whenScheduled_ThreadHaveCorrectProperties() throws InterruptedException, ExecutionException, TimeoutException {
    IssueIndexMonitoringScheduler issueIndexMonitoringScheduler = new IssueIndexMonitoringScheduler();
    Future<?> testTask = issueIndexMonitoringScheduler.submit(() -> {
      assertThat(Thread.currentThread().getName())
        .isEqualTo("asyncIssueIndexCreationTelemetryMonitor");
      assertThat(Thread.currentThread().isDaemon()).isTrue();
    });
    issueIndexMonitoringScheduler.start();
    testTask.get(10, TimeUnit.SECONDS);
    issueIndexMonitoringScheduler.stop();
  }
}
