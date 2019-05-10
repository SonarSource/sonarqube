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
import java.util.concurrent.CountDownLatch;
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

  private CountDownLatch awaitStop = new CountDownLatch(1);

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
    checkState(ceMainThread != null, "awaitStop() must not be called before start()");
    while (true) {
      try {
        awaitStop.await();
        return;
      } catch (InterruptedException e) {
        // abort waiting
      }
    }
  }

  @Override
  public void stop() {
    if (ceMainThread != null) {
      ceMainThread.stopIt();
      awaitStop();
    }
  }

  @Override
  public void hardStop() {
    if (ceMainThread != null) {
      ceMainThread.stopItNow();
      awaitStop();
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
    private final CountDownLatch stopSignal = new CountDownLatch(1);
    private volatile boolean started = false;
    private volatile boolean operational = false;
    private volatile boolean hardStop = false;
    private volatile boolean dontInterrupt = false;

    public CeMainThread() {
      super(CE_MAIN_THREAD_NAME);
    }

    @Override
    public void run() {
      boolean startupSuccessful = attemptStartup();
      this.operational = startupSuccessful;
      this.started = true;
      try {
        if (startupSuccessful) {
          try {
            stopSignal.await();
          } catch (InterruptedException e) {
            // don't restore interrupt flag since it would be unset in attemptShutdown anyway
          }

          attemptShutdown();
        }
      } finally {
        // release thread(s) waiting for CeServer to stop
        signalAwaitStop();
      }
    }

    private boolean attemptStartup() {
      try {
        LOG.info("Compute Engine starting up...");
        computeEngine.startup();
        LOG.info("Compute Engine is operational");
        return true;
      } catch (org.sonar.api.utils.MessageException | org.sonar.process.MessageException e) {
        LOG.error("Compute Engine startup failed: " + e.getMessage());
        return false;
      } catch (Throwable e) {
        LOG.error("Compute Engine startup failed", e);
        return false;
      }
    }

    private void attemptShutdown() {
      try {
        LOG.info("Compute Engine is stopping...");
        if (!hardStop) {
          computeEngine.stopProcessing();
        }
        dontInterrupt = true;
        // make sure that interrupt flag is unset because we don't want to interrupt shutdown of pico container
        interrupted();
        computeEngine.shutdown();
        LOG.info("Compute Engine is stopped");
      } catch (Throwable e) {
        LOG.error("Compute Engine failed to stop", e);
      }
    }

    public boolean isStarted() {
      return started;
    }

    public boolean isOperational() {
      return operational;
    }

    public void stopIt() {
      stopSignal.countDown();
    }

    public void stopItNow() {
      hardStop = true;
      stopSignal.countDown();
      // interrupt current thread unless it's already performing shutdown
      if (!dontInterrupt) {
        interrupt();
      }
    }

    private void signalAwaitStop() {
      awaitStop.countDown();
    }
  }

}
