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
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.ProcessId;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class ManagedProcessHandler {

  public static final long DEFAULT_WATCHER_DELAY_MS = 500L;
  private static final Logger LOG = LoggerFactory.getLogger(ManagedProcessHandler.class);

  private final ProcessId processId;
  private final ManagedProcessLifecycle lifecycle;
  private final List<ManagedProcessEventListener> eventListeners;
  private final Timeout stopTimeout;
  private final Timeout hardStopTimeout;
  private final long watcherDelayMs;

  private ManagedProcess process;
  private StreamGobbler stdOutGobbler;
  private StreamGobbler stdErrGobbler;
  private final StopWatcher stopWatcher;
  private final EventWatcher eventWatcher;
  // keep flag so that the operational event is sent only once
  // to listeners
  private boolean operational = false;

  private ManagedProcessHandler(Builder builder) {
    this.processId = requireNonNull(builder.processId, "processId can't be null");
    this.lifecycle = new ManagedProcessLifecycle(this.processId, builder.lifecycleListeners);
    this.eventListeners = builder.eventListeners;
    this.stopTimeout = builder.stopTimeout;
    this.hardStopTimeout = builder.hardStopTimeout;
    this.watcherDelayMs = builder.watcherDelayMs;
    this.stopWatcher = new StopWatcher();
    this.eventWatcher = new EventWatcher();
  }

  public boolean start(Supplier<ManagedProcess> commandLauncher) {
    if (!lifecycle.tryToMoveTo(ManagedProcessLifecycle.State.STARTING)) {
      // has already been started
      return false;
    }
    try {
      this.process = commandLauncher.get();
    } catch (RuntimeException e) {
      LOG.error("Fail to launch process [{}]", processId.getKey(), e);
      lifecycle.tryToMoveTo(ManagedProcessLifecycle.State.STOPPING);
      finalizeStop();
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
    lifecycle.tryToMoveTo(ManagedProcessLifecycle.State.STARTED);
    return true;
  }

  public ProcessId getProcessId() {
    return processId;
  }

  ManagedProcessLifecycle.State getState() {
    return lifecycle.getState();
  }

  public void stop() throws InterruptedException {
    if (lifecycle.tryToMoveTo(ManagedProcessLifecycle.State.STOPPING)) {
      stopImpl();
      if (process != null && process.isAlive()) {
        LOG.info("{} failed to stop in a graceful fashion. Hard stopping it.", processId.getKey());
        hardStop();
      } else {
        // enforce stop and clean-up even if process has been quickly stopped
        finalizeStop();
      }
    } else {
      // already stopping or stopped
      waitForDown();
    }
  }

  /**
   * Sends kill signal and awaits termination. No guarantee that process is gracefully terminated (=shutdown hooks
   * executed). It depends on OS.
   */
  public void hardStop() throws InterruptedException {
    if (lifecycle.tryToMoveTo(ManagedProcessLifecycle.State.HARD_STOPPING)) {
      hardStopImpl();
      if (process != null && process.isAlive()) {
        LOG.info("{} failed to stop in a quick fashion. Killing it.", processId.getKey());
      }
      // enforce stop and clean-up even if process has been quickly stopped
      finalizeStop();
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

  private void stopImpl() throws InterruptedException {
    if (process == null) {
      return;
    }
    try {
      process.askForStop();
      process.waitFor(stopTimeout.getDuration(), stopTimeout.getUnit());
    } catch (InterruptedException e) {
      // can't wait for the termination of process. Let's assume it's down.
      throw rethrowWithWarn(e, format("Interrupted while stopping process %s", processId));
    } catch (Throwable e) {
      LOG.error("Failed asking for graceful stop of process {}", processId, e);
    }
  }

  private void hardStopImpl() throws InterruptedException {
    if (process == null) {
      return;
    }
    try {
      process.askForHardStop();
      process.waitFor(hardStopTimeout.getDuration(), hardStopTimeout.getUnit());
    } catch (InterruptedException e) {
      // can't wait for the termination of process. Let's assume it's down.
      throw rethrowWithWarn(e,
        format("Interrupted while hard stopping process %s (currentThread=%s)", processId, Thread.currentThread().getName()));
    } catch (Throwable e) {
      LOG.error("Failed while asking for hard stop of process {}", processId, e);
    }
  }

  private static InterruptedException rethrowWithWarn(InterruptedException e, String errorMessage) {
    LOG.warn(errorMessage, e);
    Thread.currentThread().interrupt();
    return new InterruptedException(errorMessage);
  }

  private void finalizeStop() {
    if (!lifecycle.tryToMoveTo(ManagedProcessLifecycle.State.FINALIZE_STOPPING)) {
      return;
    }

    interrupt(eventWatcher);
    interrupt(stopWatcher);
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
    // will trigger state listeners
    lifecycle.tryToMoveTo(ManagedProcessLifecycle.State.STOPPED);
  }

  private static void interrupt(@Nullable Thread thread) {
    Thread currentThread = Thread.currentThread();
    // prevent current thread from interrupting itself
    if (thread != null && currentThread != thread) {
      thread.interrupt();
      if (LOG.isTraceEnabled()) {
        Exception e = new Exception("(capturing stack trace for debugging purpose)");
        LOG.trace("{} interrupted {}", currentThread.getName(), thread.getName(), e);
      }
    }
  }

  void refreshState() {
    if (process.isAlive()) {
      if (!operational && process.isOperational()) {
        operational = true;
        eventListeners.forEach(l -> l.onManagedProcessEvent(processId, ManagedProcessEventListener.Type.OPERATIONAL));
      }
      if (process.askedForRestart()) {
        process.acknowledgeAskForRestart();
        eventListeners.forEach(l -> l.onManagedProcessEvent(processId, ManagedProcessEventListener.Type.ASK_FOR_RESTART));
      }
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
   * <li>no usage of exception for flow control. Indeed {@link Process#exitValue()} throws an exception
   * if process is alive. There's no method <code>Process#isAlive()</code></li>
   * <li>no delay, instantaneous notification that process is down</li>
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
      // since process is already stopped, this will only finalize the stop sequence
      // call hardStop() rather than finalizeStop() directly because hardStop() checks lifeCycle state and this
      // avoid running to concurrent stop finalization pieces of code
      try {
        hardStop();
      } catch (InterruptedException e) {
        LOG.debug("Interrupted while stopping [{}] after process ended", processId.getKey(), e);
        Thread.currentThread().interrupt();
      }
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
        Thread.currentThread().interrupt();
      }
    }
  }

  public static Builder builder(ProcessId processId) {
    return new Builder(processId);
  }

  public static class Builder {
    private final ProcessId processId;
    private final List<ManagedProcessEventListener> eventListeners = new ArrayList<>();
    private final List<ProcessLifecycleListener> lifecycleListeners = new ArrayList<>();
    private long watcherDelayMs = DEFAULT_WATCHER_DELAY_MS;
    private Timeout stopTimeout;
    private Timeout hardStopTimeout;

    private Builder(ProcessId processId) {
      this.processId = processId;
    }

    public Builder addEventListener(ManagedProcessEventListener listener) {
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

    public Builder setStopTimeout(Timeout stopTimeout) {
      this.stopTimeout = ensureStopTimeoutNonNull(stopTimeout);
      return this;
    }

    public Builder setHardStopTimeout(Timeout hardStopTimeout) {
      this.hardStopTimeout = ensureHardStopTimeoutNonNull(hardStopTimeout);
      return this;
    }

    private static Timeout ensureStopTimeoutNonNull(Timeout stopTimeout) {
      return requireNonNull(stopTimeout, "stopTimeout can't be null");
    }

    private static Timeout ensureHardStopTimeoutNonNull(Timeout hardStopTimeout) {
      return requireNonNull(hardStopTimeout, "hardStopTimeout can't be null");
    }

    public ManagedProcessHandler build() {
      ensureStopTimeoutNonNull(this.stopTimeout);
      ensureHardStopTimeoutNonNull(this.hardStopTimeout);
      return new ManagedProcessHandler(this);
    }
  }

  public static final class Timeout {
    private final long duration;
    private final TimeUnit timeoutUnit;

    private Timeout(long duration, TimeUnit unit) {
      this.duration = duration;
      this.timeoutUnit = Objects.requireNonNull(unit, "unit can't be null");
    }

    public static Timeout newTimeout(long duration, TimeUnit unit) {
      return new Timeout(duration, unit);
    }

    public long getDuration() {
      return duration;
    }

    public TimeUnit getUnit() {
      return timeoutUnit;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Timeout timeout = (Timeout) o;
      return duration == timeout.duration && timeoutUnit == timeout.timeoutUnit;
    }

    @Override
    public int hashCode() {
      return Objects.hash(duration, timeoutUnit);
    }
  }
}
