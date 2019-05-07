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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.MessageException;

import static java.lang.String.format;
import static org.sonar.process.ProcessProperties.Property.CE_GRACEFUL_STOP_TIMEOUT;

/**
 * Immutable implementation of {@link CeConfiguration} initialized at startup from {@link Configuration}.
 */
public class CeConfigurationImpl implements CeConfiguration {
  private static final int DEFAULT_WORKER_THREAD_COUNT = 1;
  private static final int MAX_WORKER_THREAD_COUNT = 10;
  private static final int DEFAULT_WORKER_COUNT = 1;
  // 2 seconds
  private static final long DEFAULT_QUEUE_POLLING_DELAY = 2 * 1000L;
  // 0 minute
  private static final long CANCEL_WORN_OUTS_INITIAL_DELAY = 0;
  // 2 minutes
  private static final long CANCEL_WORN_OUTS_DELAY = 2;

  @CheckForNull
  private final WorkerCountProvider workerCountProvider;
  private final int workerThreadCount;
  private final long gracefulStopTimeoutInMs;
  private int workerCount;

  public CeConfigurationImpl(Configuration configuration) {
    this(configuration, null);
  }

  public CeConfigurationImpl(Configuration configuration, @Nullable WorkerCountProvider workerCountProvider) {
    this.workerCountProvider = workerCountProvider;
    this.gracefulStopTimeoutInMs = configuration.getLong(CE_GRACEFUL_STOP_TIMEOUT.getKey())
      .orElse(Long.parseLong(CE_GRACEFUL_STOP_TIMEOUT.getDefaultValue()));
    if (workerCountProvider == null) {
      this.workerCount = DEFAULT_WORKER_COUNT;
      this.workerThreadCount = DEFAULT_WORKER_THREAD_COUNT;
    } else {
      this.workerCount = readWorkerCount(workerCountProvider);
      this.workerThreadCount = MAX_WORKER_THREAD_COUNT;
    }
  }

  private static synchronized int readWorkerCount(WorkerCountProvider workerCountProvider) {
    int value = workerCountProvider.get();
    if (value < DEFAULT_WORKER_COUNT || value > MAX_WORKER_THREAD_COUNT) {
      throw parsingError(value);
    }
    return value;
  }

  private static MessageException parsingError(int value) {
    return MessageException.of(format(
      "Worker count '%s' is invalid. It must be an integer strictly greater than 0 and less or equal to 10",
      value));
  }

  @Override
  public int getWorkerMaxCount() {
    return workerThreadCount;
  }

  @Override
  public int getWorkerCount() {
    if (workerCountProvider != null) {
      workerCount = readWorkerCount(workerCountProvider);
    }
    return workerCount;
  }

  @Override
  public long getQueuePollingDelay() {
    return DEFAULT_QUEUE_POLLING_DELAY;
  }

  @Override
  public long getCleanTasksInitialDelay() {
    return CANCEL_WORN_OUTS_INITIAL_DELAY;
  }

  @Override
  public long getCleanTasksDelay() {
    return CANCEL_WORN_OUTS_DELAY;
  }

  @Override
  public long getGracefulStopTimeoutInMs() {
    return gracefulStopTimeoutInMs;
  }

}
