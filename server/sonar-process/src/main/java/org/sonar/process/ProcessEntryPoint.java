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
package org.sonar.process;

import java.io.File;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.sharedmemoryfile.DefaultProcessCommands;
import org.sonar.process.sharedmemoryfile.ProcessCommands;

import static org.sonar.process.Lifecycle.State.STOPPED;

public class ProcessEntryPoint {

  public static final String PROPERTY_PROCESS_KEY = "process.key";
  public static final String PROPERTY_PROCESS_INDEX = "process.index";
  public static final String PROPERTY_GRACEFUL_STOP_TIMEOUT_MS = "process.gracefulStopTimeout";
  public static final String PROPERTY_SHARED_PATH = "process.sharedDir";
  // 1 second
  private static final long HARD_STOP_TIMEOUT_MS = 1_000L;

  private final Props props;
  private final String processKey;
  private final Lifecycle lifecycle = new Lifecycle();
  private final ProcessCommands commands;
  private final SystemExit exit;
  private final StopWatcher stopWatcher;
  private final StopWatcher hardStopWatcher;
  // new Runnable() is important to avoid conflict of call to ProcessEntryPoint#stop() with Thread#stop()
  private final Runtime runtime;
  private Monitored monitored;
  private volatile StopperThread stopperThread;

  public ProcessEntryPoint(Props props, SystemExit exit, ProcessCommands commands, Runtime runtime) {
    this.props = props;
    this.processKey = props.nonNullValue(PROPERTY_PROCESS_KEY);
    this.exit = exit;
    this.commands = commands;
    this.stopWatcher = createStopWatcher(commands, this);
    this.hardStopWatcher = createHardStopWatcher(commands, this);
    this.runtime = runtime;
  }

  public ProcessCommands getCommands() {
    return commands;
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

    Logger logger = LoggerFactory.getLogger(getClass());
    try {
      launch(logger);
    } catch (Exception e) {
      logger.warn("Fail to start {}", processKey, e);
      hardStop();
    }
  }

  private void launch(Logger logger) throws InterruptedException {
    logger.info("Starting {}", processKey);
    runtime.addShutdownHook(new Thread(() -> {
      exit.setInShutdownHook();
      stop();
    }));
    stopWatcher.start();
    hardStopWatcher.start();

    monitored.start();
    Monitored.Status status = waitForStatus(s -> s != Monitored.Status.DOWN);
    if (status == Monitored.Status.UP || status == Monitored.Status.OPERATIONAL) {
      // notify monitor that process is ready
      commands.setUp();

      if (lifecycle.tryToMoveTo(Lifecycle.State.STARTED)) {
        Monitored.Status newStatus = waitForStatus(s -> s == Monitored.Status.OPERATIONAL || s == Monitored.Status.FAILED);
        if (newStatus == Monitored.Status.OPERATIONAL && lifecycle.tryToMoveTo(Lifecycle.State.OPERATIONAL)) {
          commands.setOperational();
        }

        monitored.awaitStop();
      }
    } else {
      logger.trace("Fail to start. Hard stopping...");
      hardStop();
    }
  }

  private Monitored.Status waitForStatus(Predicate<Monitored.Status> statusPredicate) throws InterruptedException {
    Monitored.Status status = monitored.getStatus();
    while (!statusPredicate.test(status)) {
      Thread.sleep(20);
      status = monitored.getStatus();
    }
    return status;
  }

  void stop() {
    stopAsync();
    monitored.awaitStop();
  }

  /**
   * Blocks until stopped in a timely fashion (see {@link HardStopperThread})
   */
  void hardStop() {
    hardStopAsync();
    monitored.awaitStop();
  }

  private void stopAsync() {
    if (lifecycle.tryToMoveTo(Lifecycle.State.STOPPING)) {
      LoggerFactory.getLogger(ProcessEntryPoint.class).info("Gracefully stopping process");
      stopWatcher.stopWatching();
      long terminationTimeoutMs = Long.parseLong(props.nonNullValue(PROPERTY_GRACEFUL_STOP_TIMEOUT_MS));
      stopperThread = new StopperThread(monitored, this::terminate, terminationTimeoutMs);
      stopperThread.start();
    }
  }

  private void hardStopAsync() {
    if (lifecycle.tryToMoveTo(Lifecycle.State.HARD_STOPPING)) {
      LoggerFactory.getLogger(ProcessEntryPoint.class).info("Hard stopping process");
      if (stopperThread != null) {
        stopperThread.stopIt();
      }
      hardStopWatcher.stopWatching();
      stopWatcher.stopWatching();
      new HardStopperThread(monitored, this::terminate).start();
    }
  }

  private void terminate() {
    lifecycle.tryToMoveTo(STOPPED);
    hardStopWatcher.stopWatching();
    stopWatcher.stopWatching();
    commands.endWatch();
  }

  public static ProcessEntryPoint createForArguments(String[] args) {
    Props props = ConfigurationUtils.loadPropsFromCommandLineArgs(args);
    File sharedDir = getSharedDir(props);
    int processNumber = getProcessNumber(props);
    ProcessCommands commands = DefaultProcessCommands.main(sharedDir, processNumber);
    return new ProcessEntryPoint(props, new SystemExit(), commands, Runtime.getRuntime());
  }

  private static int getProcessNumber(Props props) {
    return Integer.parseInt(props.nonNullValue(PROPERTY_PROCESS_INDEX));
  }

  private static File getSharedDir(Props props) {
    return props.nonNullValueAsFile(PROPERTY_SHARED_PATH);
  }

  /**
   * This watchdog is looking for hard stop to be requested via {@link ProcessCommands#askedForHardStop()}.
   */
  private static StopWatcher createHardStopWatcher(ProcessCommands commands, ProcessEntryPoint processEntryPoint) {
    return new StopWatcher("HardStop Watcher", processEntryPoint::hardStopAsync, commands::askedForHardStop);
  }

  /**
   * This watchdog is looking for graceful stop to be requested via {@link ProcessCommands#askedForStop()} ()}.
   */
  private static StopWatcher createStopWatcher(ProcessCommands commands, ProcessEntryPoint processEntryPoint) {
    return new StopWatcher("Stop Watcher", processEntryPoint::stopAsync, commands::askedForStop);
  }

  /**
   * Stops process in a graceful fashion
   */
  private static class StopperThread extends AbstractStopperThread {

    private StopperThread(Monitored monitored, Runnable postAction, long terminationTimeoutMs) {
      super("Stopper", () -> {
        monitored.stop();
        postAction.run();
      }, terminationTimeoutMs);
    }
  }

  /**
   * Stops process in a short time fashion
   */
  private static class HardStopperThread extends AbstractStopperThread {

    private HardStopperThread(Monitored monitored, Runnable postAction) {
      super(
        "HardStopper", () -> {
          monitored.hardStop();
          postAction.run();
        }, HARD_STOP_TIMEOUT_MS);
    }
  }
}
