/*
 * SonarQube :: Process Monitor
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.CheckForNull;
import org.slf4j.LoggerFactory;
import org.sonar.process.Lifecycle;
import org.sonar.process.Lifecycle.State;
import org.sonar.process.ProcessCommands;
import org.sonar.process.SystemExit;

public class Monitor {
  private static final Timeouts TIMEOUTS = new Timeouts();

  private final List<ProcessRef> processes = new CopyOnWriteArrayList<>();
  private final JavaProcessLauncher launcher;

  private final SystemExit systemExit;
  private Thread shutdownHook = new Thread(new MonitorShutdownHook(), "Monitor Shutdown Hook");

  // used by awaitStop() to block until all processes are shutdown
  private List<WatcherThread> watcherThreads = new CopyOnWriteArrayList<>();
  @CheckForNull
  private List<JavaCommand> javaCommands;
  @CheckForNull
  private Lifecycle lifecycle;
  @CheckForNull
  private RestartRequestWatcherThread restartWatcher;
  @CheckForNull
  private TerminatorThread terminator;
  static int nextProcessId = 1;

  Monitor(JavaProcessLauncher launcher, SystemExit exit) {
    this.launcher = launcher;
    this.systemExit = exit;
  }

  public static Monitor create() {
    return new Monitor(new JavaProcessLauncher(TIMEOUTS), new SystemExit());
  }

  /**
   * Starts commands and blocks current thread until all processes are in state {@link State#STARTED}.
   * @throws java.lang.IllegalArgumentException if commands list is empty
   * @throws java.lang.IllegalStateException if already started or if at least one process failed to start. In this case
   *   all processes are terminated. No need to execute {@link #stop()}
   */
  public void start(List<JavaCommand> commands) {
    if (commands.isEmpty()) {
      throw new IllegalArgumentException("At least one command is required");
    }

    if (lifecycle != null) {
      throw new IllegalStateException("Can not start multiple times");
    }

    // intercepts CTRL-C
    Runtime.getRuntime().addShutdownHook(shutdownHook);

    this.javaCommands = commands;
    start();
  }

  private void start() {
    resetState();
    List<ProcessRef> processRefs = startAndMonitorProcesses();
    startWatchingForRestartRequests(processRefs);
  }

  private void resetState() {
    this.lifecycle = new Lifecycle();
    lifecycle.tryToMoveTo(State.STARTING);
    this.watcherThreads.clear();
  }

  private List<ProcessRef> startAndMonitorProcesses() {
    List<ProcessRef> processRefs = new ArrayList<>(javaCommands.size());
    for (JavaCommand command : javaCommands) {
      try {
        ProcessRef processRef = launcher.launch(command);
        monitor(processRef);
        processRefs.add(processRef);
      } catch (RuntimeException e) {
        // fail to start or to monitor
        stop();
        throw e;
      }
    }
    return processRefs;
  }

  private void monitor(ProcessRef processRef) {
    // physically watch if process is alive
    WatcherThread watcherThread = new WatcherThread(processRef, this);
    watcherThread.start();
    watcherThreads.add(watcherThread);

    processes.add(processRef);

    // wait for process to be ready (accept requests or so on)
    processRef.waitForReady();

    LoggerFactory.getLogger(getClass()).info(String.format("%s is up", processRef));
  }

  private void startWatchingForRestartRequests(List<ProcessRef> processRefs) {
    if (lifecycle.tryToMoveTo(State.STARTED)) {
      stopRestartWatcher();
      startRestartWatcher(processRefs);
    } else {
      // stopping or stopped during startup, for instance :
      // 1. A is started
      // 2. B starts
      // 3. A crashes while B is starting
      // 4. if B was not monitored during Terminator execution, then it's an alive orphan
      stop();
      throw new IllegalStateException("Stopped during startup");
    }
  }

  private void stopRestartWatcher() {
    if (this.restartWatcher != null) {
      this.restartWatcher.stopWatching();
      try {
        this.restartWatcher.join();
      } catch (InterruptedException e) {
        // failed to cleanly stop (very unlikely), ignore and proceed
      }
    }
  }

  private void startRestartWatcher(List<ProcessRef> processRefs) {
    this.restartWatcher = new RestartRequestWatcherThread(this, processRefs);
    this.restartWatcher.start();
  }

  /**
   * Blocks until all processes are terminated
   */
  public void awaitTermination() {
    while (awaitTerminationImpl()) {
      LoggerFactory.getLogger(RestartRequestWatcherThread.class).info("Restarting SQ...");
      start();
    }
    stopRestartWatcher();
  }

  boolean waitForOneRestart() {
    boolean restartRequested = awaitTerminationImpl();
    if (restartRequested) {
      start();
    }
    return restartRequested;
  }

  private boolean awaitTerminationImpl() {
    for (WatcherThread watcherThread : watcherThreads) {
      while (watcherThread.isAlive()) {
        try {
          watcherThread.join();
        } catch (InterruptedException ignored) {
          // ignore, stop blocking
        }
      }
    }
    return hasRestartBeenRequested();
  }

  private boolean hasRestartBeenRequested() {
    for (WatcherThread watcherThread : watcherThreads) {
      if (watcherThread.isAskedForRestart()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Blocks until all processes are terminated.
   */
  public void stop() {
    stopAsync();
    try {
      terminator.join();
    } catch (InterruptedException ignored) {
      // stop blocking and exiting
    }
    // safeguard if TerminatorThread is buggy
    lifecycle.tryToMoveTo(State.STOPPED);
    // cleanly stop restart watcher
    stopRestartWatcher();
    systemExit.exit(0);
  }

  /**
   * Asks for processes termination and returns without blocking until termination.
   * However, if a termination request is already under way (it's not supposed to happen, but, technically, it can occur),
   * this call will be blocking until the previous request finishes.
   */
  public void stopAsync() {
    if (lifecycle.tryToMoveTo(State.STOPPING)) {
      if (terminator != null) {
        try {
          terminator.join();
        } catch (InterruptedException e) {
          // stop waiting for thread to complete and continue with creating a new one
        }
      }
      terminator = new TerminatorThread(TIMEOUTS);
      terminator.setProcesses(processes);
      terminator.start();
    }
  }

  public State getState() {
    return lifecycle.getState();
  }

  Thread getShutdownHook() {
    return shutdownHook;
  }

  public void restartAsync() {
    stopAsync();
  }

  private class MonitorShutdownHook implements Runnable {
    @Override
    public void run() {
      systemExit.setInShutdownHook();
      // blocks until everything is corrected terminated
      stop();
    }
  }

  public static int getNextProcessId() {
    if (nextProcessId >= ProcessCommands.MAX_PROCESSES) {
      throw new IllegalStateException("The maximum number of processes launched has been reached " + ProcessCommands.MAX_PROCESSES);
    }
    return nextProcessId++;
  }
}
