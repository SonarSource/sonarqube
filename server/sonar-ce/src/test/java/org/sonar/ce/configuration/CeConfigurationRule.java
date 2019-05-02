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
package org.sonar.ce.configuration;

import org.junit.rules.ExternalResource;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Mutable implementation of {@link CeConfiguration} as {@link org.junit.Rule}.
 */
public class CeConfigurationRule extends ExternalResource implements CeConfiguration {
  private int workerThreadCount = 1;
  private int workerCount = 1;
  private long queuePollingDelay = 2 * 1000L;
  private long cleanTasksInitialDelay = 1L;
  private long cleanTasksDelay = 10L;

  @Override
  public int getWorkerMaxCount() {
    return workerThreadCount;
  }

  public void setWorkerThreadCount(int workerThreadCount) {
    checkArgument(workerThreadCount >= 1, "worker thread count must be >= 1");
    this.workerThreadCount = workerThreadCount;
  }

  @Override
  public int getWorkerCount() {
    return workerCount;
  }

  public CeConfigurationRule setWorkerCount(int workerCount) {
    checkArgument(workerCount >= 1, "worker count must be >= 1");
    this.workerCount = workerCount;
    return this;
  }

  @Override
  public long getQueuePollingDelay() {
    return queuePollingDelay;
  }

  public void setQueuePollingDelay(int queuePollingDelay) {
    checkArgument(queuePollingDelay > 0, "Queue polling delay must be >= 0");
    this.queuePollingDelay = queuePollingDelay;
  }

  @Override
  public long getCleanTasksInitialDelay() {
    return cleanTasksInitialDelay;
  }

  public void setCleanTasksInitialDelay(long cleanTasksInitialDelay) {
    checkArgument(cleanTasksInitialDelay > 0, "cancel worn-outs polling initial delay must be >= 1");
    this.cleanTasksInitialDelay = cleanTasksInitialDelay;
  }

  @Override
  public long getCleanTasksDelay() {
    return cleanTasksDelay;
  }

  public void setCleanTasksDelay(long cleanTasksDelay) {
    checkArgument(cleanTasksDelay > 0, "cancel worn-outs polling delay must be >= 1");
    this.cleanTasksDelay = cleanTasksDelay;
  }

  @Override
  public long getGracefulStopTimeoutInMs() {
    return 6 * 60 * 60 * 1_000L;
  }
}
