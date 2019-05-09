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
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.sharedmemoryfile.DefaultProcessCommands;
import org.sonar.process.sharedmemoryfile.ProcessCommands;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
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
  private volatile Monitored monitored;
  private volatile StopperThread stopperThread;
  private volatile HardStopperThread hardStopperThread;

  private ProcessEntryPoint(Props props, SystemExit exit, ProcessCommands commands, Runtime runtime) {
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
    } finally {
      logger.trace("Hard stopping to clean any resource...");
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
    waitForStop();


    stopAsync()
      .ifPresent(stoppingThread -> {
        try {
          // join() does nothing if thread already finished
          stoppingThread.join();
          commands.endWatch();
          exit.exit(0);
        } catch (InterruptedException e) {
          // stop can be aborted by a hard stop
          Thread.currentThread().interrupt();
        }
      });
  }

  private void waitForStop() {
    try {
      stopLatch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private CountDownLatch stopLatch = new CountDownLatch(1);

  private Optional<StopperThread> stopAsync() {
    if (lifecycle.tryToMoveTo(Lifecycle.State.STOPPING)) {
      long terminationTimeoutMs = Long.parseLong(props.nonNullValue(PROPERTY_GRACEFUL_STOP_TIMEOUT_MS));
      stopperThread = new StopperThread(monitored, lifecycle, terminationTimeoutMs);
      stopperThread.start();
      stopWatcher.stopWatching();
      return of(stopperThread);
    }
    // stopperThread could already exist
    return ofNullable(stopperThread);
  }

  /**
   * Blocks until stopped in a timely fashion (see {@link HardStopperThread})
   */
  void hardStop() {
    hardStopAsync()
      .ifPresent(stoppingThread -> {
        try {
          // join() does nothing if thread already finished
          stoppingThread.join();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        commands.endWatch();
        exit.exit(0);
      });
  }

  private Optional<HardStopperThread> hardStopAsync() {
    if (lifecycle.tryToMoveTo(Lifecycle.State.HARD_STOPPING)) {
      hardStopperThread = new HardStopperThread(monitored, lifecycle, HARD_STOP_TIMEOUT_MS, stopperThread);
      hardStopperThread.start();
      hardStopWatcher.stopWatching();
      stopWatcher.stopWatching();
      return of(hardStopperThread);
    }
    // hardStopperThread could already exist
    return ofNullable(hardStopperThread);
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

    private StopperThread(Monitored monitored, Lifecycle lifecycle, long terminationTimeoutMs) {
      super("Stopper", () -> {
        if (!lifecycle.isCurrentState(STOPPED)) {
          monitored.stop();
          lifecycle.tryToMoveTo(STOPPED);
        }
      }, terminationTimeoutMs);
    }

  }

  /**
   * Stops process in a short time fashion
   */
  private static class HardStopperThread extends AbstractStopperThread {

    private HardStopperThread(Monitored monitored, Lifecycle lifecycle, long terminationTimeoutMs, @Nullable StopperThread stopperThread) {
      super(
        "HardStopper",
        () -> {
          if (stopperThread != null) {
            stopperThread.stopIt();
          }
          monitored.hardStop();
          lifecycle.tryToMoveTo(STOPPED);
        },
        terminationTimeoutMs);
    }

  }
}
