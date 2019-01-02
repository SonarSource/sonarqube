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
package org.sonar.application.cluster.health;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.cluster.health.HealthStateRefresher;
import org.sonar.process.cluster.health.HealthStateRefresherExecutorService;
import org.sonar.process.cluster.health.NodeHealthProvider;
import org.sonar.process.cluster.health.SharedHealthStateImpl;
import org.sonar.process.cluster.hz.HazelcastMember;

public class HealthStateSharingImpl implements HealthStateSharing {
  private static final Logger LOG = LoggerFactory.getLogger(HealthStateSharingImpl.class);

  private final HazelcastMember hzMember;
  private final NodeHealthProvider nodeHealthProvider;
  private HealthStateRefresherExecutorService executorService;
  private HealthStateRefresher healthStateRefresher;

  public HealthStateSharingImpl(HazelcastMember hzMember, NodeHealthProvider nodeHealthProvider) {
    this.hzMember = hzMember;
    this.nodeHealthProvider = nodeHealthProvider;
  }

  @Override
  public void start() {
    executorService = new DelegateHealthStateRefresherExecutorService(
      Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder()
          .setDaemon(false)
          .setNameFormat("health_state_refresh-%d")
          .build()));
    healthStateRefresher = new HealthStateRefresher(executorService, nodeHealthProvider, new SharedHealthStateImpl(hzMember));
    healthStateRefresher.start();
  }

  @Override
  public void stop() {
    healthStateRefresher.stop();
    stopExecutorService(executorService);
  }

  private static void stopExecutorService(ScheduledExecutorService executorService) {
    // Disable new tasks from being submitted
    executorService.shutdown();
    try {
      // Wait a while for existing tasks to terminate
      if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
        // Cancel currently executing tasks
        executorService.shutdownNow();
        // Wait a while for tasks to respond to being canceled
        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
          LOG.warn("Pool {} did not terminate", HealthStateSharingImpl.class.getSimpleName());
        }
      }
    } catch (InterruptedException ie) {
      LOG.warn("Termination of pool {} failed", HealthStateSharingImpl.class.getSimpleName(), ie);
      // (Re-)Cancel if current thread also interrupted
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

}
