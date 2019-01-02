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
package org.sonar.ce.app;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.ComputeEngine;
import org.sonar.ce.ComputeEngineImpl;
import org.sonar.ce.container.ComputeEngineContainerImpl;
import org.sonar.ce.logging.CeProcessLogging;
import org.sonar.process.MinimumViableSystem;
import org.sonar.process.Monitored;
import org.sonar.process.ProcessEntryPoint;
import org.sonar.process.Props;

import static com.google.common.base.Preconditions.checkState;
import static org.sonar.process.ProcessUtils.awaitTermination;

/**
 * The Compute Engine server which starts a daemon thread to run the {@link ComputeEngineImpl} when it's {@link #start()}
 * method is called.
 * <p>
 * This is the class to call to run a standalone {@link ComputeEngineImpl} (see {@link #main(String[])}).
 * </p>
 */
public class CeServer implements Monitored {
  private static final Logger LOG = Loggers.get(CeServer.class);

  private static final String CE_MAIN_THREAD_NAME = "ce-main";

  /**
   * Thread that currently is inside our await() method.
   */
  private AtomicReference<Thread> awaitThread = new AtomicReference<>();
  private volatile boolean stopAwait = false;

  private final ComputeEngine computeEngine;
  @Nullable
  private CeMainThread ceMainThread = null;

  @VisibleForTesting
  protected CeServer(ComputeEngine computeEngine, MinimumViableSystem mvs) {
    this.computeEngine = computeEngine;
    mvs
      .checkWritableTempDir()
      .checkRequiredJavaOptions(ImmutableMap.of("file.encoding", "UTF-8"));
  }

  @Override
  public void start() {
    checkState(ceMainThread == null, "start() can not be called twice");
    // start main thread
    ceMainThread = new CeMainThread();
    ceMainThread.start();
  }

  @Override
  public Status getStatus() {
    checkState(ceMainThread != null, "getStatus() can not be called before start()");

    if (ceMainThread.isStarted()) {
      return ceMainThread.isOperational() ? Status.OPERATIONAL : Status.FAILED;
    }
    return Status.DOWN;
  }

  @Override
  public void awaitStop() {
    checkState(awaitThread.compareAndSet(null, Thread.currentThread()), "There can't be more than one thread waiting for the Compute Engine to stop");
    checkState(ceMainThread != null, "awaitStop() must not be called before start()");

    try {
      while (!stopAwait) {
        try {
          // wait for a quite long time but we will be interrupted if flag changes anyway
          Thread.sleep(10_000);
        } catch (InterruptedException e) {
          // continue and check the flag
        }
      }
    } finally {
      awaitThread = null;
    }
  }

  @Override
  public void stop() {
    if (ceMainThread != null) {
      // signal main Thread to stop
      ceMainThread.stopIt();
      awaitTermination(ceMainThread);
    }
  }

  /**
   * Can't be started as is. Needs to be bootstrapped by sonar-application
   */
  public static void main(String[] args) {
    ProcessEntryPoint entryPoint = ProcessEntryPoint.createForArguments(args);
    Props props = entryPoint.getProps();
    new CeProcessLogging().configure(props);
    CeServer server = new CeServer(
      new ComputeEngineImpl(props, new ComputeEngineContainerImpl()),
      new MinimumViableSystem());
    entryPoint.launch(server);
  }

  private class CeMainThread extends Thread {
    private static final int CHECK_FOR_STOP_DELAY = 50;
    private volatile boolean stop = false;
    private volatile boolean started = false;
    private volatile boolean operational = false;

    public CeMainThread() {
      super(CE_MAIN_THREAD_NAME);
    }

    @Override
    public void run() {
      boolean startupSuccessful = attemptStartup();
      this.operational = startupSuccessful;
      this.started = true;
      if (startupSuccessful) {
        // call below is blocking
        waitForStopSignal();
      } else {
        stopAwait();
      }
    }

    private boolean attemptStartup() {
      try {
        startup();
        return true;
      } catch (org.sonar.api.utils.MessageException | org.sonar.process.MessageException e) {
        LOG.error("Compute Engine startup failed: " + e.getMessage());
        return false;
      } catch (Throwable e) {
        LOG.error("Compute Engine startup failed", e);
        return false;
      }
    }

    private void startup() {
      LOG.info("Compute Engine starting up...");
      computeEngine.startup();
      LOG.info("Compute Engine is operational");
    }

    private void waitForStopSignal() {
      while (!stop) {
        try {
          Thread.sleep(CHECK_FOR_STOP_DELAY);
        } catch (InterruptedException e) {
          // ignore the interruption itself
          // Do not propagate the isInterrupted flag with Thread.currentThread().interrupt()
          // It will break the shutdown of ComputeEngineContainerImpl#stop()
        }
      }
      attemptShutdown();
    }

    private void attemptShutdown() {
      try {
        LOG.info("Compute Engine is stopping...");
        computeEngine.shutdown();
        LOG.info("Compute Engine is stopped");
      } catch (Throwable e) {
        LOG.error("Compute Engine failed to stop", e);
      } finally {
        // release thread waiting for CeServer
        stopAwait();
      }
    }

    public boolean isStarted() {
      return started;
    }

    public boolean isOperational() {
      return operational;
    }

    public void stopIt() {
      // stop looping indefinitely
      this.stop = true;
      // interrupt current thread in case its waiting for WebServer
      interrupt();
    }

    private void stopAwait() {
      stopAwait = true;
      Thread t = awaitThread.get();
      if (t != null) {
        t.interrupt();
        try {
          t.join(1_000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          // Ignored
        }
      }
    }
  }

}
