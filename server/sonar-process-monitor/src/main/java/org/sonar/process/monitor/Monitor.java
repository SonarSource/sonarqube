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

import org.slf4j.LoggerFactory;
import org.sonar.process.Lifecycle;
import org.sonar.process.MessageException;
import org.sonar.process.State;
import org.sonar.process.SystemExit;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Monitor {

  private final List<ProcessRef> processes = new CopyOnWriteArrayList<ProcessRef>();
  private final TerminatorThread terminator;
  private final JavaProcessLauncher launcher;
  private final JmxConnector jmxConnector;
  private final Lifecycle lifecycle = new Lifecycle();
  private final Timeouts timeouts;

  private final SystemExit systemExit;
  private Thread shutdownHook = new Thread(new MonitorShutdownHook(), "Monitor Shutdown Hook");

  // used by awaitTermination() to block until all processes are shutdown
  private final List<WatcherThread> watcherThreads = new CopyOnWriteArrayList<WatcherThread>();

  Monitor(JavaProcessLauncher launcher, JmxConnector jmxConnector, Timeouts timeouts, SystemExit exit) {
    this.launcher = launcher;
    this.jmxConnector = jmxConnector;
    this.timeouts = timeouts;
    this.terminator = new TerminatorThread(processes, jmxConnector, timeouts);
    this.systemExit = exit;
  }

  public static Monitor create() {
    Timeouts timeouts = new Timeouts();
    return new Monitor(new JavaProcessLauncher(timeouts), new RmiJmxConnector(timeouts),
      timeouts, new SystemExit());
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
        monitor(command, processRef);
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

  private void monitor(JavaCommand command, ProcessRef processRef) {
    // physically watch if process is alive
    WatcherThread watcherThread = new WatcherThread(processRef, this);
    watcherThread.start();
    watcherThreads.add(watcherThread);

    // add to list of monitored processes only when successfully connected to it
    jmxConnector.connect(command, processRef);
    processes.add(processRef);

    // ping process on a regular basis
    processRef.setPingEnabled(!command.isDebugMode());
    if (processRef.isPingEnabled()) {
      PingerThread.startPinging(processRef, jmxConnector, timeouts);
    }

    // wait for process to be ready (accept requests or so on)
    waitForReady(processRef);

    LoggerFactory.getLogger(getClass()).info(String.format("%s is up", processRef));
  }

  private void waitForReady(ProcessRef processRef) {
    boolean ready = false;
    while (!ready) {
      if (processRef.isTerminated()) {
        throw new MessageException(String.format("%s failed to start", processRef));
      }
      try {
        ready = jmxConnector.isReady(processRef);
      } catch (Exception ignored) {
        // failed to send request, probably because RMI server is still not alive
        // trying again, as long as process is alive
        // TODO could be improved to have a STARTING timeout (to be implemented in monitor or
        // in child process ?)
      }
      try {
        Thread.sleep(300L);
      } catch (InterruptedException e) {
        throw new IllegalStateException("Interrupted while waiting for " + processRef + " to be ready", e);
      }
    }
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
    terminateAsync();
    try {
      terminator.join();
    } catch (InterruptedException ignored) {
      // ignore, stop blocking
    }
    // safeguard if TerminatorThread is buggy
    hardKillAll();
    lifecycle.tryToMoveTo(State.STOPPED);
    systemExit.exit(0);
  }

  /**
   * Asks for processes termination and returns without blocking until termination.
   * @return true if termination was requested, false if it was already being terminated
   */
  boolean terminateAsync() {
    boolean requested = false;
    if (lifecycle.tryToMoveTo(State.STOPPING)) {
      requested = true;
      terminator.start();
    }
    return requested;
  }

  private void hardKillAll() {
    // no specific order, kill'em all!!!
    for (ProcessRef process : processes) {
      process.hardKill();
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
}
