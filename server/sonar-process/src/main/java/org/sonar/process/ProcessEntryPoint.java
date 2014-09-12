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

import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ProcessEntryPoint implements ProcessMXBean {

  public static final String PROPERTY_PROCESS_KEY = "process.key";
  public static final String PROPERTY_AUTOKILL_DISABLED = "process.autokill.disabled";
  public static final String PROPERTY_AUTOKILL_PING_TIMEOUT = "process.autokill.pingTimeout";
  public static final String PROPERTY_AUTOKILL_PING_INTERVAL = "process.autokill.pingInterval";
  public static final String PROPERTY_TERMINATION_TIMEOUT = "process.terminationTimeout";

  private final Props props;
  private final Lifecycle lifecycle = new Lifecycle();
  private volatile MonitoredProcess monitoredProcess;
  private volatile long lastPing = 0L;
  private volatile StopperThread stopperThread;
  private final SystemExit exit;
  private Thread shutdownHook = new Thread(new Runnable() {
    @Override
    public void run() {
      exit.setInShutdownHook();
      terminate();
    }
  });

  ProcessEntryPoint(Props props, SystemExit exit) {
    this.props = props;
    this.exit = exit;
  }

  public Props getProps() {
    return props;
  }

  /**
   * Launch process and waits until it's down
   */
  public void launch(MonitoredProcess mp) {
    if (!lifecycle.tryToMoveTo(State.STARTING)) {
      throw new IllegalStateException("Already started");
    }
    monitoredProcess = mp;

    // TODO check if these properties are available in System Info
    JmxUtils.registerMBean(this, props.nonNullValue(PROPERTY_PROCESS_KEY));
    Runtime.getRuntime().addShutdownHook(shutdownHook);
    if (!props.valueAsBoolean(PROPERTY_AUTOKILL_DISABLED, false)) {
      // mainly for Java Debugger
      scheduleAutokill();
    }

    try {
      monitoredProcess.start();
      if (lifecycle.tryToMoveTo(State.STARTED)) {
        monitoredProcess.awaitTermination();
      }
    } catch (Exception ignored) {
    } finally {
      terminate();
    }
  }

  @Override
  public boolean isReady() {
    return lifecycle.getState() == State.STARTED;
  }

  @Override
  public void ping() {
    lastPing = System.currentTimeMillis();
  }

  /**
   * Blocks until stopped in a timely fashion (see {@link org.sonar.process.StopperThread})
   */
  @Override
  public void terminate() {
    if (lifecycle.tryToMoveTo(State.STOPPING)) {
      stopperThread = new StopperThread(monitoredProcess, Long.parseLong(props.nonNullValue(PROPERTY_TERMINATION_TIMEOUT)));
      stopperThread.start();
    }
    try {
      // stopperThread is not null for sure
      // join() does nothing if thread already finished
      stopperThread.join();
      lifecycle.tryToMoveTo(State.STOPPED);
    } catch (InterruptedException e) {
      // nothing to do, the process is going to be exited
    }
    exit.exit(0);
  }

  private void scheduleAutokill() {
    final long autokillPingTimeoutMs = props.valueAsInt(PROPERTY_AUTOKILL_PING_TIMEOUT);
    long autokillPingIntervalMs = props.valueAsInt(PROPERTY_AUTOKILL_PING_INTERVAL);
    Runnable autokiller = new Runnable() {
      @Override
      public void run() {
        long time = System.currentTimeMillis();
        if (time - lastPing > autokillPingTimeoutMs) {
          LoggerFactory.getLogger(getClass()).info(String.format(
            "Did not receive any ping during %d seconds. Shutting down.", autokillPingTimeoutMs / 1000));
          terminate();
        }
      }
    };
    lastPing = System.currentTimeMillis();
    ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
    monitor.scheduleWithFixedDelay(autokiller, autokillPingIntervalMs, autokillPingIntervalMs, TimeUnit.MILLISECONDS);
  }

  State getState() {
    return lifecycle.getState();
  }

  Thread getShutdownHook() {
    return shutdownHook;
  }

  public static ProcessEntryPoint createForArguments(String[] args) {
    Props props = ConfigurationUtils.loadPropsFromCommandLineArgs(args);
    return new ProcessEntryPoint(props, new SystemExit());
  }
}
