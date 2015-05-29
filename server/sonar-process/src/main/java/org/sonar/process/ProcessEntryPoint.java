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

public class ProcessEntryPoint implements Stoppable {

  public static final String PROPERTY_PROCESS_KEY = "process.key";
  public static final String PROPERTY_PROCESS_INDEX = "process.index";
  public static final String PROPERTY_TERMINATION_TIMEOUT = "process.terminationTimeout";
  public static final String PROPERTY_SHARED_PATH = "process.sharedDir";

  private final Props props;
  private final Lifecycle lifecycle = new Lifecycle();
  private final ProcessCommands commands;
  private final SystemExit exit;
  private volatile Monitored monitored;
  private volatile StopperThread stopperThread;
  private final StopWatcher stopWatcher;

  // new Runnable() is important to avoid conflict of call to ProcessEntryPoint#stop() with Thread#stop()
  private Thread shutdownHook = new Thread(new Runnable() {
    @Override
    public void run() {
      exit.setInShutdownHook();
      stop();
    }
  });

  ProcessEntryPoint(Props props, SystemExit exit, ProcessCommands commands) {
    this.props = props;
    this.exit = exit;
    this.commands = commands;
    this.stopWatcher = new StopWatcher(commands, this);
  }

  public Props getProps() {
    return props;
  }

  public String getKey() {
    return props.nonNullValue(PROPERTY_PROCESS_KEY);
  }

  /**
   * Launch process and waits until it's down
   */
  public void launch(Monitored mp) {
    if (!lifecycle.tryToMoveTo(Lifecycle.State.STARTING)) {
      throw new IllegalStateException("Already started");
    }
    monitored = mp;

    try {
      LoggerFactory.getLogger(getClass()).info("Starting " + getKey());
      Runtime.getRuntime().addShutdownHook(shutdownHook);
      stopWatcher.start();

      monitored.start();
      boolean ready = false;
      while (!ready) {
        ready = monitored.isReady();
        Thread.sleep(20L);
      }

      // notify monitor that process is ready
      commands.setReady();

      if (lifecycle.tryToMoveTo(Lifecycle.State.STARTED)) {
        monitored.awaitStop();
      }
    } catch (Exception e) {
      LoggerFactory.getLogger(getClass()).warn("Fail to start " + getKey(), e);

    } finally {
      stop();
    }
  }

  boolean isStarted() {
    return lifecycle.getState() == Lifecycle.State.STARTED;
  }

  /**
   * Blocks until stopped in a timely fashion (see {@link org.sonar.process.StopperThread})
   */
  void stop() {
    stopAsync();
    try {
      // stopperThread is not null for sure
      // join() does nothing if thread already finished
      stopperThread.join();
      lifecycle.tryToMoveTo(Lifecycle.State.STOPPED);
    } catch (InterruptedException e) {
      // nothing to do, the process is going to be exited
    }
    exit.exit(0);
  }

  @Override
  public void stopAsync() {
    if (lifecycle.tryToMoveTo(Lifecycle.State.STOPPING)) {
      stopperThread = new StopperThread(monitored, commands, Long.parseLong(props.nonNullValue(PROPERTY_TERMINATION_TIMEOUT)));
      stopperThread.start();
      stopWatcher.stopWatching();
    }
  }

  Lifecycle.State getState() {
    return lifecycle.getState();
  }

  Thread getShutdownHook() {
    return shutdownHook;
  }

  public static ProcessEntryPoint createForArguments(String[] args) {
    Props props = ConfigurationUtils.loadPropsFromCommandLineArgs(args);
    ProcessCommands commands = new DefaultProcessCommands(
      props.nonNullValueAsFile(PROPERTY_SHARED_PATH), Integer.parseInt(props.nonNullValue(PROPERTY_PROCESS_INDEX)));
    return new ProcessEntryPoint(props, new SystemExit(), commands);
  }
}
