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
package org.sonar.process.monitor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.LoggerFactory;
import org.sonar.process.Lifecycle;
import org.sonar.process.Lifecycle.State;
import org.sonar.process.ProcessCommands;
import org.sonar.process.SystemExit;

public class Monitor {

  private final List<ProcessRef> processes = new CopyOnWriteArrayList<>();
  private final TerminatorThread terminator;
  private final JavaProcessLauncher launcher;
  private final Lifecycle lifecycle = new Lifecycle();

  private final SystemExit systemExit;
  private Thread shutdownHook = new Thread(new MonitorShutdownHook(), "Monitor Shutdown Hook");

  // used by awaitStop() to block until all processes are shutdown
  private final List<WatcherThread> watcherThreads = new CopyOnWriteArrayList<>();
  static int nextProcessId = 0;

  Monitor(JavaProcessLauncher launcher, SystemExit exit, TerminatorThread terminator) {
    this.launcher = launcher;
    this.terminator = terminator;
    this.systemExit = exit;
  }

  public static Monitor create() {
    Timeouts timeouts = new Timeouts();
    return new Monitor(new JavaProcessLauncher(timeouts), new SystemExit(), new TerminatorThread(timeouts));
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

    if (!lifecycle.tryToMoveTo(State.STARTING)) {
      throw new IllegalStateException("Can not start multiple times");
    }

    // intercepts CTRL-C
    Runtime.getRuntime().addShutdownHook(shutdownHook);

    for (JavaCommand command : commands) {
      try {
        ProcessRef processRef = launcher.launch(command);
        monitor(processRef);
      } catch (RuntimeException e) {
        // fail to start or to monitor
        stop();
        throw e;
      }
    }

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

  /**
   * Blocks until all processes are terminated
   */
  public void awaitTermination() {
    for (WatcherThread watcherThread : watcherThreads) {
      while (watcherThread.isAlive()) {
        try {
          watcherThread.join();
        } catch (InterruptedException ignored) {
          // ignore, stop blocking
        }
      }
    }
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
    systemExit.exit(0);
  }

  /**
   * Asks for processes termination and returns without blocking until termination.
   */
  public void stopAsync() {
    if (lifecycle.tryToMoveTo(State.STOPPING)) {
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
