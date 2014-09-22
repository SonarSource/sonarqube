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

public class ProcessEntryPoint {

  public static final String PROPERTY_PROCESS_KEY = "process.key";
  public static final String PROPERTY_TERMINATION_TIMEOUT = "process.terminationTimeout";
  public static final String PROPERTY_STATUS_PATH = "process.statusPath";

  private final Props props;
  private final Lifecycle lifecycle = new Lifecycle();
  private final SharedStatus sharedStatus;
  private volatile Monitored monitored;
  private volatile StopperThread stopperThread;
  private final SystemExit exit;

  private Thread shutdownHook = new Thread(new Runnable() {
    @Override
    public void run() {
      exit.setInShutdownHook();
      terminate();
    }
  });

  ProcessEntryPoint(Props props, SystemExit exit, SharedStatus sharedStatus) {
    this.props = props;
    this.exit = exit;
    this.sharedStatus = sharedStatus;
  }

  public Props getProps() {
    return props;
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
      Runtime.getRuntime().addShutdownHook(shutdownHook);
      monitored.start();
      boolean ready = false;
      while (!ready) {
        ready = monitored.isReady();
        Thread.sleep(200L);
      }

      sharedStatus.setReady();

      if (lifecycle.tryToMoveTo(Lifecycle.State.STARTED)) {
        monitored.awaitStop();
      }
    } catch (Exception e) {
      LoggerFactory.getLogger(getClass()).warn("Fail to start", e);

    } finally {
      terminate();
    }
  }

  boolean isStarted() {
    return lifecycle.getState() == Lifecycle.State.STARTED;
  }

  /**
   * Blocks until stopped in a timely fashion (see {@link org.sonar.process.StopperThread})
   */
  void terminate() {
    if (lifecycle.tryToMoveTo(Lifecycle.State.STOPPING)) {
      stopperThread = new StopperThread(monitored, sharedStatus, Long.parseLong(props.nonNullValue(PROPERTY_TERMINATION_TIMEOUT)));
      stopperThread.start();
    }
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

  Lifecycle.State getState() {
    return lifecycle.getState();
  }

  Thread getShutdownHook() {
    return shutdownHook;
  }

  public static ProcessEntryPoint createForArguments(String[] args) {
    Props props = ConfigurationUtils.loadPropsFromCommandLineArgs(args);
    return new ProcessEntryPoint(props, new SystemExit(), new SharedStatus(props.nonNullValueAsFile(PROPERTY_STATUS_PATH)));
  }
}
