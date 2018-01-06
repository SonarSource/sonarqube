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
package org.sonar.ce.configuration;

import javax.annotation.CheckForNull;
import org.picocontainer.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.MessageException;

import static java.lang.String.format;

/**
 * Immutable implementation of {@link CeConfiguration} which takes value returned by an implementation of
 * {@link WorkerCountProvider}, if any is available, or use the {@link #DEFAULT_WORKER_COUNT default worker count}.
 * In addition, it always returns {@link #DEFAULT_QUEUE_POLLING_DELAY} when
 * {@link CeConfiguration#getQueuePollingDelay()} is called.
 */
public class CeConfigurationImpl implements CeConfiguration, Startable {
  private static final int DEFAULT_WORKER_THREAD_COUNT = 1;
  private static final int MAX_WORKER_THREAD_COUNT = 10;
  private static final int DEFAULT_WORKER_COUNT = 1;
  // 2 seconds
  private static final long DEFAULT_QUEUE_POLLING_DELAY = 2 * 1000L;
  // 1 minute
  private static final long CANCEL_WORN_OUTS_INITIAL_DELAY = 1;
  // 10 minutes
  private static final long CANCEL_WORN_OUTS_DELAY = 10;
  // 40 seconds
  private static final int GRACEFUL_STOP_TIMEOUT = 40;
  public static final String SONAR_CE_GRACEFUL_STOP_TIME_OUT_IN_MS = "sonar.ce.gracefulStopTimeOutInMs";

  @CheckForNull
  private final WorkerCountProvider workerCountProvider;
  private final int workerThreadCount;
  private final int gracefultStopTimeoutInMs;
  private int workerCount;

  public CeConfigurationImpl(Configuration configuration) {
    this.workerCountProvider = null;
    this.workerThreadCount = DEFAULT_WORKER_THREAD_COUNT;
    this.workerCount = DEFAULT_WORKER_COUNT;
    this.gracefultStopTimeoutInMs = configuration.getInt(SONAR_CE_GRACEFUL_STOP_TIME_OUT_IN_MS).orElse(GRACEFUL_STOP_TIMEOUT);
  }

  public CeConfigurationImpl(Configuration configuration, WorkerCountProvider workerCountProvider) {
    this.workerCountProvider = workerCountProvider;
    this.workerThreadCount = MAX_WORKER_THREAD_COUNT;
    this.workerCount = readWorkerCount(workerCountProvider);
    this.gracefultStopTimeoutInMs = configuration.getInt(SONAR_CE_GRACEFUL_STOP_TIME_OUT_IN_MS).orElse(GRACEFUL_STOP_TIMEOUT);
  }

  private static int readWorkerCount(WorkerCountProvider workerCountProvider) {
    int value = workerCountProvider.get();
    if (value < DEFAULT_WORKER_COUNT || value > MAX_WORKER_THREAD_COUNT) {
      throw parsingError(value);
    }
    return value;
  }

  private static MessageException parsingError(int value) {
    return MessageException.of(format(
        "Worker count '%s' is invalid. It must an integer strictly greater than 0 and less or equal to 10",
        value));
  }

  @Override
  public void start() {
    //
  }

  @Override
  public void stop() {
    // nothing to do
  }

  @Override
  public void refresh() {
    if (workerCountProvider != null) {
      this.workerCount = readWorkerCount(workerCountProvider);
    }
  }

  @Override
  public int getWorkerMaxCount() {
    return workerThreadCount;
  }

  @Override
  public int getWorkerCount() {
    return workerCount;
  }

  @Override
  public long getQueuePollingDelay() {
    return DEFAULT_QUEUE_POLLING_DELAY;
  }

  @Override
  public long getCleanCeTasksInitialDelay() {
    return CANCEL_WORN_OUTS_INITIAL_DELAY;
  }

  @Override
  public long getCleanCeTasksDelay() {
    return CANCEL_WORN_OUTS_DELAY;
  }

  @Override
  public int getGracefulStopTimeoutInMs() {
    return gracefultStopTimeoutInMs;
  }

}
