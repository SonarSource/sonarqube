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
package org.sonar.application.process;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.ProcessId;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class SQProcess {

  public static final long DEFAULT_WATCHER_DELAY_MS = 500L;
  private static final Logger LOG = LoggerFactory.getLogger(SQProcess.class);

  private final ProcessId processId;
  private final Lifecycle lifecycle;
  private final List<ProcessEventListener> eventListeners;
  private final long watcherDelayMs;

  private ProcessMonitor process;
  private StreamGobbler stdOutGobbler;
  private StreamGobbler stdErrGobbler;
  private final StopWatcher stopWatcher;
  private final EventWatcher eventWatcher;
  // keep flag so that the operational event is sent only once
  // to listeners
  private final AtomicBoolean operational = new AtomicBoolean(false);

  private SQProcess(Builder builder) {
    this.processId = requireNonNull(builder.processId, "processId can't be null");
    this.lifecycle = new Lifecycle(this.processId, builder.lifecycleListeners);
    this.eventListeners = builder.eventListeners;
    this.watcherDelayMs = builder.watcherDelayMs;
    this.stopWatcher = new StopWatcher();
    this.eventWatcher = new EventWatcher();
  }

  public boolean start(Supplier<ProcessMonitor> commandLauncher) {
    if (!lifecycle.tryToMoveTo(Lifecycle.State.STARTING)) {
      // has already been started
      return false;
    }
    try {
      this.process = commandLauncher.get();
    } catch (RuntimeException e) {
      LOG.error("Fail to launch process [{}]", processId.getKey(), e);
      lifecycle.tryToMoveTo(Lifecycle.State.STOPPED);
      throw e;
    }
    this.stdOutGobbler = new StreamGobbler(process.getInputStream(), processId.getKey());
    this.stdOutGobbler.start();
    this.stdErrGobbler = new StreamGobbler(process.getErrorStream(), processId.getKey());
    this.stdErrGobbler.start();
    this.stopWatcher.start();
    this.eventWatcher.start();
    // Could be improved by checking the status "up" in shared memory.
    // Not a problem so far as this state is not used by listeners.
    lifecycle.tryToMoveTo(Lifecycle.State.STARTED);
    return true;
  }

  public ProcessId getProcessId() {
    return processId;
  }

  Lifecycle.State getState() {
    return lifecycle.getState();
  }

  /**
   * Sends kill signal and awaits termination. No guarantee that process is gracefully terminated (=shutdown hooks
   * executed). It depends on OS.
   */
  public void stop(long timeout, TimeUnit timeoutUnit) {
    if (lifecycle.tryToMoveTo(Lifecycle.State.STOPPING)) {
      stopGracefully(timeout, timeoutUnit);
      if (process != null && process.isAlive()) {
        LOG.info("{} failed to stop in a timely fashion. Killing it.", processId.getKey());
      }
      // enforce stop and clean-up even if process has been gracefully stopped
      stopForcibly();
    } else {
      // already stopping or stopped
      waitForDown();
    }
  }

  private void waitForDown() {
    while (process != null && process.isAlive()) {
      try {
        process.waitFor();
      } catch (InterruptedException ignored) {
        // ignore, waiting for process to stop
        Thread.currentThread().interrupt();
      }
    }
  }

  private void stopGracefully(long timeout, TimeUnit timeoutUnit) {
    if (process == null) {
      return;
    }
    try {
      // request graceful stop
      process.askForStop();
      process.waitFor(timeout, timeoutUnit);
    } catch (InterruptedException e) {
      // can't wait for the termination of process. Let's assume it's down.
      LOG.warn("Interrupted while stopping process {}", processId, e);
      Thread.currentThread().interrupt();
    } catch (Throwable e) {
      LOG.error("Can not ask for graceful stop of process {}", processId, e);
    }
  }

  public void stopForcibly() {
    eventWatcher.interrupt();
    stopWatcher.interrupt();
    if (process != null) {
      process.destroyForcibly();
      waitForDown();
      process.closeStreams();
    }
    if (stdOutGobbler != null) {
      StreamGobbler.waitUntilFinish(stdOutGobbler);
      stdOutGobbler.interrupt();
    }
    if (stdErrGobbler != null) {
      StreamGobbler.waitUntilFinish(stdErrGobbler);
      stdErrGobbler.interrupt();
    }
    lifecycle.tryToMoveTo(Lifecycle.State.STOPPED);
  }

  void refreshState() {
    if (process.isAlive()) {
      if (!operational.get() && process.isOperational()) {
        operational.set(true);
        eventListeners.forEach(l -> l.onProcessEvent(processId, ProcessEventListener.Type.OPERATIONAL));
      }
      if (process.askedForRestart()) {
        process.acknowledgeAskForRestart();
        eventListeners.forEach(l -> l.onProcessEvent(processId, ProcessEventListener.Type.ASK_FOR_RESTART));
      }
    } else {
      stopForcibly();
    }
  }

  @Override
  public String toString() {
    return format("Process[%s]", processId.getKey());
  }

  /**
   * This thread blocks as long as the monitored process is physically alive.
   * It avoids from executing {@link Process#exitValue()} at a fixed rate :
   * <ul>
   *   <li>no usage of exception for flow control. Indeed {@link Process#exitValue()} throws an exception
   *   if process is alive. There's no method <code>Process#isAlive()</code></li>
   *   <li>no delay, instantaneous notification that process is down</li>
   * </ul>
   */
  private class StopWatcher extends Thread {
    StopWatcher() {
      // this name is different than Thread#toString(), which includes name, priority
      // and thread group
      // -> do not override toString()
      super(format("StopWatcher[%s]", processId.getKey()));
    }

    @Override
    public void run() {
      try {
        process.waitFor();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        // stop watching process
      }
      stopForcibly();
    }
  }

  private class EventWatcher extends Thread {
    EventWatcher() {
      // this name is different than Thread#toString(), which includes name, priority
      // and thread group
      // -> do not override toString()
      super(format("EventWatcher[%s]", processId.getKey()));
    }

    @Override
    public void run() {
      try {
        while (process.isAlive()) {
          refreshState();
          Thread.sleep(watcherDelayMs);
        }
      } catch (InterruptedException e) {
        // request to stop watching process. To avoid unexpected behaviors
        // the process is stopped.
        Thread.currentThread().interrupt();
        stopForcibly();
      }
    }
  }

  public static Builder builder(ProcessId processId) {
    return new Builder(processId);
  }

  public static class Builder {
    private final ProcessId processId;
    private final List<ProcessEventListener> eventListeners = new ArrayList<>();
    private final List<ProcessLifecycleListener> lifecycleListeners = new ArrayList<>();
    private long watcherDelayMs = DEFAULT_WATCHER_DELAY_MS;

    private Builder(ProcessId processId) {
      this.processId = processId;
    }

    public Builder addEventListener(ProcessEventListener listener) {
      this.eventListeners.add(listener);
      return this;
    }

    public Builder addProcessLifecycleListener(ProcessLifecycleListener listener) {
      this.lifecycleListeners.add(listener);
      return this;
    }

    /**
     * Default delay is {@link #DEFAULT_WATCHER_DELAY_MS}
     */
    public Builder setWatcherDelayMs(long l) {
      this.watcherDelayMs = l;
      return this;
    }

    public SQProcess build() {
      return new SQProcess(this);
    }
  }
}
