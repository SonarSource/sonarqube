/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import org.picocontainer.Startable;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static java.lang.String.format;

/**
 * Immutable implementation of {@link CeConfiguration} which takes value returned by
 * {@link CeConfiguration#getWorkerCount()} from property {@link CeConfigurationImpl#CE_WORKERS_COUNT_PROPERTY} and
 * always returns {@link #DEFAULT_QUEUE_POLLING_DELAY} when {@link CeConfiguration#getQueuePollingDelay()} is called.
 */
public class CeConfigurationImpl implements CeConfiguration, Startable {
  private static final String CE_WORKERS_COUNT_PROPERTY = "sonar.ce.workerCount";

  private static final Logger LOG = Loggers.get(CeConfigurationImpl.class);

  private static final int DEFAULT_WORKER_COUNT = 1;
  // 2 seconds
  private static final long DEFAULT_QUEUE_POLLING_DELAY = 2 * 1000L;
  // 1 minute
  private static final long CANCEL_WORN_OUTS_INITIAL_DELAY = 1;
  // 10 minutes
  private static final long CANCEL_WORN_OUTS_DELAY = 10;

  private final int workerCount;

  public CeConfigurationImpl(Settings settings) {
    String workerCountAsStr = settings.getString(CE_WORKERS_COUNT_PROPERTY);
    if (workerCountAsStr == null || workerCountAsStr.isEmpty()) {
      this.workerCount = DEFAULT_WORKER_COUNT;
    } else {
      this.workerCount = parseStringValue(workerCountAsStr);
    }
  }

  private static int parseStringValue(String workerCountAsStr) {
    try {
      int value = Integer.parseInt(workerCountAsStr);
      if (value < 1) {
        throw parsingError(workerCountAsStr);
      }
      return value;
    } catch (NumberFormatException e) {
      throw parsingError(workerCountAsStr);
    }
  }

  private static MessageException parsingError(String workerCountAsStr) {
    return MessageException.of(format(
      "value '%s' of property %s is invalid. It must an integer strictly greater than 0.",
      workerCountAsStr,
      CE_WORKERS_COUNT_PROPERTY)
      );
  }

  @Override
  public void start() {
    if (this.workerCount > 1) {
      LOG.info("Compute Engine will use {} concurrent workers to process tasks", this.workerCount);
    }
  }

  @Override
  public void stop() {
    // nothing to do
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

}
