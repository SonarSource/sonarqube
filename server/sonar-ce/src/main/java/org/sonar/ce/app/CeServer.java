/*
 * SonarQube
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
package org.sonar.ce.app;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.CheckForNull;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.ComputeEngine;
import org.sonar.ce.ComputeEngineImpl;
import org.sonar.ce.container.ComputeEngineContainerImpl;
import org.sonar.process.MinimumViableSystem;
import org.sonar.process.Monitored;
import org.sonar.process.ProcessEntryPoint;
import org.sonar.process.Props;
import org.sonar.ce.log.CeProcessLogging;

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

  private final StartupBarrier startupBarrier;
  private final ComputeEngine computeEngine;
  @CheckForNull
  private CeMainThread ceMainThread = null;

  @VisibleForTesting
  protected CeServer(StartupBarrier startupBarrier, ComputeEngine computeEngine, MinimumViableSystem mvs) {
    this.startupBarrier = startupBarrier;
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
  public boolean isUp() {
    checkState(ceMainThread != null, "isUp() can not be called before start()");

    return ceMainThread.isStarted();
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
      new StartupBarrierFactory().create(entryPoint),
      new ComputeEngineImpl(props, new ComputeEngineContainerImpl()),
      new MinimumViableSystem());
    entryPoint.launch(server);
  }

  private class CeMainThread extends Thread {
    private static final int CHECK_FOR_STOP_DELAY = 50;
    private volatile boolean stop = false;
    private volatile boolean started = false;

    public CeMainThread() {
      super(CE_MAIN_THREAD_NAME);
    }

    @Override
    public void run() {
      boolean webServerOperational = startupBarrier.waitForOperational();
      if (!webServerOperational) {
        LOG.debug("Interrupted while waiting for WebServer to be operational. Assuming it will never be. Stopping.");
        // signal CE is done booting (obviously, since we are about to stop)
        this.started = true;
        // release thread (if any) in CeServer#awaitStop()
        stopAwait();
        return;
      }

      boolean startupSuccessful = attemptStartup();
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
      } catch (Throwable e) {
        LOG.error("Compute Engine startup failed", e);
        return false;
      }
    }

    private void startup() {
      LOG.info("Compute Engine starting up...");
      computeEngine.startup();
      LOG.info("Compute Engine is up");
    }

    private void waitForStopSignal() {
      while (!stop) {
        try {
          Thread.sleep(CHECK_FOR_STOP_DELAY);
        } catch (InterruptedException e) {
          // ignore the interruption itself, check the flag
        }
      }
      attemptShutdown();
    }

    private void attemptShutdown() {
      try {
        shutdown();
      } catch (Throwable e) {
        LOG.error("Compute Engine shutdown failed", e);
      } finally {
        // release thread waiting for CeServer
        stopAwait();
      }
    }

    private void shutdown() {
      LOG.info("Compute Engine shutting down...");
      computeEngine.shutdown();
    }

    public boolean isStarted() {
      return started;
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
          t.join(1000);
        } catch (InterruptedException e) {
          // Ignored
        }
      }
    }
  }

}
