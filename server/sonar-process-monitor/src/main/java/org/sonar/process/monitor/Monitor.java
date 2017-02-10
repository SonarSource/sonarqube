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
package org.sonar.process.monitor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.CheckForNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.DefaultProcessCommands;
import org.sonar.process.Lifecycle;
import org.sonar.process.Lifecycle.State;
import org.sonar.process.ProcessId;
import org.sonar.process.ProcessUtils;
import org.sonar.process.SystemExit;

import static org.sonar.process.DefaultProcessCommands.reset;

public class Monitor {

  private static final Logger LOG = LoggerFactory.getLogger(Monitor.class);
  private static final Timeouts TIMEOUTS = new Timeouts();
  private static final long WATCH_DELAY_MS = 500L;

  private static int restartorInstanceCounter = 0;

  private final int processNumber;
  private final FileSystem fileSystem;
  private final SystemExit systemExit;
  private final boolean watchForHardStop;
  private final Thread shutdownHook = new Thread(new MonitorShutdownHook(), "Monitor Shutdown Hook");

  private final List<WatcherThread> watcherThreads = new CopyOnWriteArrayList<>();
  private final Lifecycle lifecycle;

  private final TerminatorThread terminator = new TerminatorThread();
  private final RestartRequestWatcherThread restartWatcher = new RestartRequestWatcherThread();
  @CheckForNull
  private List<JavaCommand> javaCommands;
  @CheckForNull
  private JavaProcessLauncher launcher;
  @CheckForNull
  private RestartorThread restartor;
  @CheckForNull
  HardStopWatcherThread hardStopWatcher;

  Monitor(int processNumber, FileSystem fileSystem, SystemExit exit, boolean watchForHardStop, Lifecycle.LifecycleListener... listeners) {
    this.processNumber = processNumber;
    this.fileSystem = fileSystem;
    this.systemExit = exit;
    this.watchForHardStop = watchForHardStop;
    this.lifecycle = new Lifecycle(listeners);
  }

  public static Monitor create(int processNumber, FileSystem fileSystem, boolean watchForHardStop) {
    return new Monitor(processNumber, fileSystem, new SystemExit(), watchForHardStop);
  }

  public static Monitor create(int processNumber, FileSystem fileSystem, boolean watchForHardStop, Lifecycle.LifecycleListener listener) {
    return new Monitor(processNumber, fileSystem, new SystemExit(), watchForHardStop, Objects.requireNonNull(listener));
  }

  /**
   * Starts commands and blocks current thread until all processes are in state {@link State#STARTED}.
   * @throws java.lang.IllegalArgumentException if commands list is empty
   * @throws java.lang.IllegalStateException if already started or if at least one process failed to start. In this case
   *   all processes are terminated. No need to execute {@link #stop()}
   */
  public void start(List<JavaCommand> commands) throws InterruptedException {
    if (commands.isEmpty()) {
      throw new IllegalArgumentException("At least one command is required");
    }

    if (lifecycle.getState() != State.INIT) {
      throw new IllegalStateException("Can not start multiple times");
    }

    // intercepts CTRL-C
    Runtime.getRuntime().addShutdownHook(shutdownHook);

    // start watching for restart requested by child process
    restartWatcher.start();

    javaCommands = commands;
    startProcesses();
  }

  private void startProcesses() throws InterruptedException {
    // do no start any child process if not in state INIT or RESTARTING (a stop could be in progress too)
    if (lifecycle.tryToMoveTo(State.STARTING)) {
      resetFileSystem();

      // start watching for stop requested by other process (eg. orchestrator) if enabled and not started yet
      if (watchForHardStop && hardStopWatcher == null) {
        hardStopWatcher = new HardStopWatcherThread();
        hardStopWatcher.start();
      }

      startAndMonitorProcesses();
      stopIfAnyProcessDidNotStart();
    }
  }

  private void resetFileSystem() {
    // since JavaLauncher depends on temp directory, which is reset below, we need to close it first
    closeJavaLauncher();
    try {
      fileSystem.reset();
    } catch (IOException e) {
      // failed to reset FileSystem
      throw new RuntimeException("Failed to reset file system", e);
    }
    // reset sharedmemory of App
    reset(fileSystem.getTempDir(), ProcessId.APP.getIpcIndex());
  }

  private void closeJavaLauncher() {
    if (this.launcher != null) {
      this.launcher.close();
      this.launcher = null;
    }
  }

  private void startAndMonitorProcesses() throws InterruptedException{
    File tempDir = fileSystem.getTempDir();
    this.launcher = new JavaProcessLauncher(TIMEOUTS, tempDir);
    for (JavaCommand command : javaCommands) {
      ProcessRef processRef = null;
      try {
        processRef = launcher.launch(command);
        monitor(processRef);
      } catch (InterruptedException | RuntimeException e) {
        if (processRef != null) {
          LOG.error("{} failed to start", processRef);
        }
        // fail to start or to monitor
        stop();
        throw e;
      }
    }
  }

  private void monitor(ProcessRef processRef) throws InterruptedException {
    // physically watch if process is alive
    WatcherThread watcherThread = new WatcherThread(processRef, this);
    watcherThread.start();
    watcherThreads.add(watcherThread);

    // wait for process to be ready (accept requests or so on)
    processRef.waitForUp();

    LOG.info("{} is up", processRef);
  }

  private void stopIfAnyProcessDidNotStart() {
    if (!lifecycle.tryToMoveTo(State.STARTED)) {
      // stopping or stopped during startup, for instance :
      // 1. A is started
      // 2. B starts
      // 3. A crashes while B is starting
      // 4. if B was not monitored during Terminator execution, then it's an alive orphan
      stop();
      throw new IllegalStateException("Stopped during startup");
    }
  }

  /**
   * Blocks until all processes are terminated
   */
  public void awaitTermination() {
    while (awaitChildProcessesTermination()) {
      trace("await termination of restartor...");
      ProcessUtils.awaitTermination(restartor);
    }
    cleanAfterTermination();
  }

  boolean waitForOneRestart() {
    boolean restartRequested = awaitChildProcessesTermination();
    trace("finished waiting, restartRequested={}", restartRequested);
    if (restartRequested) {
      trace("awaitTermination restartor={}", restartor);
      ProcessUtils.awaitTermination(restartor);
    }
    return restartRequested;
  }

  private boolean awaitChildProcessesTermination() {
    trace("await termination of child processes...");
    List<WatcherThread> watcherThreadsCopy = new ArrayList<>(this.watcherThreads);
    for (WatcherThread watcherThread : watcherThreadsCopy) {
      ProcessUtils.awaitTermination(watcherThread);
    }
    trace("all child processes done");
    return hasRestartBeenRequested(watcherThreadsCopy);
  }

  private static boolean hasRestartBeenRequested(List<WatcherThread> watcherThreads) {
    for (WatcherThread watcherThread : watcherThreads) {
      if (watcherThread.isAskedForRestart()) {
        trace("one child process requested restart");
        return true;
      }
    }
    trace("no child process requested restart");
    return false;
  }

  /**
   * Blocks until all processes are terminated.
   */
  public void stop() {
    trace("start hard stop async...");
    stopAsync(State.HARD_STOPPING);
    trace("await termination of terminator...");
    ProcessUtils.awaitTermination(terminator);
    cleanAfterTermination();
    trace("exit...");
    systemExit.exit(0);
  }

  private void cleanAfterTermination() {
    trace("go to STOPPED...");
    if (lifecycle.tryToMoveTo(State.STOPPED)) {
      trace("await termination of restartWatcher and hardStopWatcher...");
      // wait for restartWatcher and hardStopWatcher to cleanly stop
      ProcessUtils.awaitTermination(restartWatcher, hardStopWatcher);
      trace("restartWatcher done");
      // removing shutdown hook to avoid called stop() unnecessarily unless already in shutdownHook
      if (!systemExit.isInShutdownHook()) {
        trace("removing shutdown hook...");
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
      }
      // cleanly close JavaLauncher
      closeJavaLauncher();
    }
  }

  /**
   * Asks for processes termination and returns without blocking until termination.
   * However, if a termination request is already under way (it's not supposed to happen, but, technically, it can occur),
   * this call will be blocking until the previous request finishes.
   */
  public void stopAsync() {
    stopAsync(State.STOPPING);
  }

  private void stopAsync(State stoppingState) {
    assert stoppingState == State.STOPPING || stoppingState == State.HARD_STOPPING;
    if (lifecycle.tryToMoveTo(stoppingState)) {
      terminator.start();
    }
  }

  public void restartAsync() {
    if (lifecycle.tryToMoveTo(State.RESTARTING)) {
      restartor = new RestartorThread();
      restartor.start();
    }
  }

  /**
   * Runs every time a restart request is detected.
   */
  private class RestartorThread extends Thread {

    private RestartorThread() {
      super("Restartor " + (restartorInstanceCounter++));
    }

    @Override
    public void run() {
      stopProcesses();
      try {
        startProcesses();
      } catch (InterruptedException e) {
        // Startup was interrupted. Processes are being stopped asynchronously.
        // Restoring the interruption state.
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Runs only once
   */
  private class TerminatorThread extends Thread {

    private TerminatorThread() {
      super("Terminator");
    }

    @Override
    public void run() {
      stopProcesses();
    }
  }

  /**
   * Watches for any child process requesting a restart of all children processes.
   * It runs once and as long as {@link #lifecycle} hasn't reached {@link Lifecycle.State#STOPPED} and holds its checks
   * when {@link #lifecycle} is not in state {@link Lifecycle.State#STARTED} to avoid taking the same request into account
   * twice.
   */
  public class RestartRequestWatcherThread extends Thread {
    public RestartRequestWatcherThread() {
      super("Restart watcher");
    }

    @Override
    public void run() {
      while (lifecycle.getState() != Lifecycle.State.STOPPED) {
        if (lifecycle.getState() == Lifecycle.State.STARTED && didAnyProcessRequestRestart()) {
          restartAsync();
        }
        try {
          Thread.sleep(WATCH_DELAY_MS);
        } catch (InterruptedException ignored) {
          // keep watching
        }
      }
    }

    private boolean didAnyProcessRequestRestart() {
      for (WatcherThread watcherThread : watcherThreads) {
        ProcessRef processRef = watcherThread.getProcessRef();
        if (processRef.getCommands().askedForRestart()) {
          LOG.info("Process [{}] requested restart", processRef.getKey());
          return true;
        }
      }
      return false;
    }

  }

  public class HardStopWatcherThread extends Thread {

    public HardStopWatcherThread() {
      super("Hard stop watcher");
    }

    @Override
    public void run() {
      while (lifecycle.getState() != Lifecycle.State.STOPPED) {
        if (askedForStop()) {
          trace("Stopping process");
          Monitor.this.stop();
        } else {
          delay();
        }
      }
    }

    private boolean askedForStop() {
      File tempDir = fileSystem.getTempDir();
      try (DefaultProcessCommands processCommands = DefaultProcessCommands.secondary(tempDir, processNumber)) {
        if (processCommands.askedForStop()) {
          return true;
        }
      }
      return false;
    }

    private void delay() {
      try {
        Thread.sleep(WATCH_DELAY_MS);
      } catch (InterruptedException ignored) {
        // keep watching
      }
    }

  }

  private void stopProcesses() {
    List<WatcherThread> watcherThreadsCopy = new ArrayList<>(this.watcherThreads);
    // create a copy and reverse it to terminate in reverse order of startup (dependency order)
    Collections.reverse(watcherThreadsCopy);

    for (WatcherThread watcherThread : watcherThreadsCopy) {
      ProcessRef ref = watcherThread.getProcessRef();
      if (!ref.isStopped()) {
        LOG.info("{} is stopping", ref);
        ref.askForGracefulAsyncStop();

        long killAt = System.currentTimeMillis() + TIMEOUTS.getTerminationTimeout();
        while (!ref.isStopped() && System.currentTimeMillis() < killAt) {
          try {
            Thread.sleep(10L);
          } catch (InterruptedException e) {
            // stop asking for graceful stops, Monitor will hardly kill all processes
            break;
          }
        }
        if (!ref.isStopped()) {
          LOG.info("{} failed to stop in a timely fashion. Killing it.", ref);
        }
        ref.stop();
        LOG.info("{} is stopped", ref);
      }
    }

    // all processes are stopped, no need to keep references to these WatcherThread anymore
    trace("all processes stopped, clean list of watcherThreads...");
    this.watcherThreads.clear();
  }

  public State getState() {
    return lifecycle.getState();
  }

  Thread getShutdownHook() {
    return shutdownHook;
  }

  private class MonitorShutdownHook implements Runnable {
    @Override
    public void run() {
      systemExit.setInShutdownHook();
      trace("calling stop from MonitorShutdownHook...");
      // blocks until everything is corrected terminated
      stop();
    }
  }

  private static void trace(String s) {
    LOG.trace(s);
  }

  private static void trace(String s, Object args) {
    LOG.trace(s, args);
  }

}
