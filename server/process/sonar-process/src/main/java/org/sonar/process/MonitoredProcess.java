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

  private final static Logger LOGGER = LoggerFactory.getLogger(MonitoredProcess.class);

  public static final String DEBUG_AGENT = "-agentlib:jdwp";
  private static final long AUTOKILL_TIMEOUT_MS = 30000L;
  private static final long AUTOKILL_CHECK_DELAY_MS = 2000L;
  public static final String NAME_PROPERTY = "pName";
  public static final String MISSING_NAME_ARGUMENT = "Missing Name argument";

  private Long lastPing;
  private final String name;
  private boolean terminated = false;
  private long timeout = AUTOKILL_TIMEOUT_MS;
  private long checkDelay = AUTOKILL_CHECK_DELAY_MS;

  protected final Props props;
  private ScheduledFuture<?> pingTask = null;

  private ScheduledExecutorService monitor;
  private final boolean isMonitored;

  protected MonitoredProcess(Props props) {
    this(props, !props.containsValue(DEBUG_AGENT));
  }

  protected MonitoredProcess(Props props, boolean monitor) {
    this.isMonitored = monitor;
    this.props = props;
    this.name = props.of(NAME_PROPERTY);

    // Testing required properties
    if (StringUtils.isEmpty(name)) {
      throw new IllegalStateException(MISSING_NAME_ARGUMENT);
    }

    JmxUtils.registerMBean(this, name);
    ProcessUtils.addSelfShutdownHook(this);
  }

  public MonitoredProcess setTimeout(long timeout) {
    this.timeout = timeout;
    return this;
  }

  private long getTimeout() {
    return timeout;
  }

  public MonitoredProcess setCheckDelay(long checkDelay) {
    this.checkDelay = checkDelay;
    return this;
  }

  private long getCheckDelay() {
    return checkDelay;
  }

  public final void start() {
    if (monitor != null) {
      throw new IllegalStateException("Already started");
    }
    LOGGER.debug("Process[{}] starting", name);
    scheduleAutokill(this.isMonitored);
    try {
      doStart();
    } catch (Exception e) {
      LOGGER.error("Could not start process: {}", e);
      this.terminate();
    }
    LOGGER.debug("Process[{}] started", name);
  }

  /**
   * If the process does not receive pings during the max allowed period, then
   * process auto-kills
   */
  private void scheduleAutokill(final Boolean isMonitored) {
    final Runnable breakOnMissingPing = new Runnable() {
      @Override
      public void run() {
        long time = System.currentTimeMillis();
        if (time - lastPing > getTimeout()) {
          LoggerFactory.getLogger(getClass()).info(String.format(
            "Did not receive any ping during %d seconds. Shutting down.", getTimeout() / 1000));
          if (isMonitored) {
            terminate();
          }
        }
      }
    };
    lastPing = System.currentTimeMillis();
    monitor = Executors.newScheduledThreadPool(1);
    pingTask = monitor.scheduleAtFixedRate(breakOnMissingPing, getCheckDelay(), getCheckDelay(), TimeUnit.MILLISECONDS);
  }

  @Override
  public final long ping() {
    this.lastPing = System.currentTimeMillis();
    return lastPing;
  }

  @Override
  public final void terminate() {
    if (monitor != null) {
      LOGGER.debug("Process[{}] terminating", name);
      monitor.shutdownNow();
      monitor = null;
      if (pingTask != null) {
        pingTask.cancel(true);
        pingTask = null;
      }
      try {
        doTerminate();
      } catch (Exception e) {
        LOGGER.error("Fail to terminate " + name, e);
        // do not propagate exception
      }
      LOGGER.debug("Process[{}] terminated", name);
      terminated = true;
    }
  }

  public boolean isTerminated() {
    return terminated && monitor == null;
  }

  public boolean isMonitored() {
    return this.isMonitored;
  }

  @Override
  public final boolean isReady() {
    try {
      return doIsReady();
    } catch (Exception ignored) {
      LOGGER.trace("Exception while checking if ready", ignored);
      return false;
    }
  }

  protected abstract void doStart();

  protected abstract void doTerminate();

  protected abstract boolean doIsReady();
}
