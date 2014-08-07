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
package org.sonar.process;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class MonitoredProcess implements ProcessMXBean {

  public static final String NAME_PROPERTY = "pName";
  private static final long AUTOKILL_TIMEOUT_MS = 15000L;
  private static final long AUTOKILL_CHECK_DELAY_MS = 5000L;
  public static final String MISSING_NAME_ARGUMENT = "Missing Name argument";

  private Long lastPing;
  private final String name;
  protected final Props props;
  private ScheduledFuture<?> pingTask = null;
  private ScheduledExecutorService monitor;

  protected MonitoredProcess(Props props) throws Exception {
    this.props = props;
    this.name = props.of(NAME_PROPERTY);

    // Testing required properties
    if (StringUtils.isEmpty(name)) {
      throw new IllegalStateException(MISSING_NAME_ARGUMENT);
    }

    JmxUtils.registerMBean(this, name);
    ProcessUtils.addSelfShutdownHook(this);
  }

  public final void start() {
    if (monitor != null) {
      throw new IllegalStateException("Already started");
    }

    Logger logger = LoggerFactory.getLogger(getClass());
    logger.debug("Process[{}] starting", name);
    scheduleAutokill();
    doStart();
    logger.debug("Process[{}] started", name);
  }

  /**
   * If the process does not receive pings during the max allowed period, then
   * process auto-kills
   */
  private void scheduleAutokill() {
    final Runnable breakOnMissingPing = new Runnable() {
      @Override
      public void run() {
        long time = System.currentTimeMillis();
        if (time - lastPing > AUTOKILL_TIMEOUT_MS) {
          LoggerFactory.getLogger(getClass()).info(String.format(
            "Did not receive any ping during %d seconds. Shutting down.", AUTOKILL_TIMEOUT_MS / 1000));
          terminate();
        }
      }
    };
    lastPing = System.currentTimeMillis();
    monitor = Executors.newScheduledThreadPool(1);
    pingTask = monitor.scheduleWithFixedDelay(breakOnMissingPing, AUTOKILL_CHECK_DELAY_MS, AUTOKILL_CHECK_DELAY_MS, TimeUnit.MILLISECONDS);
  }

  @Override
  public final long ping() {
    this.lastPing = System.currentTimeMillis();
    return lastPing;
  }

  @Override
  public final void terminate() {
    if (monitor != null) {
      Logger logger = LoggerFactory.getLogger(getClass());
      logger.debug("Process[{}] terminating", name);
      monitor.shutdownNow();
      monitor = null;
      if (pingTask != null) {
        pingTask.cancel(true);
        pingTask = null;
      }
      try {
        doTerminate();
      } catch (Exception e) {
        LoggerFactory.getLogger(getClass()).error("Fail to terminate " + name, e);
        // do not propagate exception
      }
      logger.debug("Process[{}] terminated", name);
    }
  }

  @Override
  public final boolean isReady() {
    try {
      return doIsReady();
    } catch (Exception ignored) {
      return false;
    }
  }

  protected abstract void doStart();

  protected abstract void doTerminate();

  protected abstract boolean doIsReady();
}
