/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.application;

import java.util.EnumMap;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.application.command.AbstractCommand;
import org.sonar.application.command.CommandFactory;
import org.sonar.application.config.AppSettings;
import org.sonar.application.config.ClusterSettings;
import org.sonar.application.process.Lifecycle;
import org.sonar.application.process.ProcessEventListener;
import org.sonar.application.process.ProcessLauncher;
import org.sonar.application.process.ProcessLifecycleListener;
import org.sonar.application.process.ProcessMonitor;
import org.sonar.application.process.SQProcess;
import org.sonar.process.ProcessId;

public class SchedulerImpl implements Scheduler, ProcessEventListener, ProcessLifecycleListener, AppStateListener {

  private static final Logger LOG = LoggerFactory.getLogger(SchedulerImpl.class);

  private final AppSettings settings;
  private final AppReloader appReloader;
  private final CommandFactory commandFactory;
  private final ProcessLauncher processLauncher;
  private final AppState appState;
  private final NodeLifecycle nodeLifecycle = new NodeLifecycle();

  private final CountDownLatch keepAlive = new CountDownLatch(1);
  private final AtomicBoolean firstWaitingEsLog = new AtomicBoolean(true);
  private final AtomicBoolean restartRequested = new AtomicBoolean(false);
  private final AtomicBoolean restartDisabled = new AtomicBoolean(false);
  private final EnumMap<ProcessId, SQProcess> processesById = new EnumMap<>(ProcessId.class);
  private final AtomicInteger operationalCountDown = new AtomicInteger();
  private final AtomicInteger stopCountDown = new AtomicInteger(0);
  private StopperThread stopperThread;
  private RestarterThread restarterThread;
  private long processWatcherDelayMs = SQProcess.DEFAULT_WATCHER_DELAY_MS;

  public SchedulerImpl(AppSettings settings, AppReloader appReloader, CommandFactory commandFactory,
    ProcessLauncher processLauncher,
    AppState appState) {
    this.settings = settings;
    this.appReloader = appReloader;
    this.commandFactory = commandFactory;
    this.processLauncher = processLauncher;
    this.appState = appState;
    this.appState.addListener(this);
  }

  SchedulerImpl setProcessWatcherDelayMs(long l) {
    this.processWatcherDelayMs = l;
    return this;
  }

  @Override
  public void schedule() {
    if (!nodeLifecycle.tryToMoveTo(NodeLifecycle.State.STARTING)) {
      return;
    }
    processesById.clear();

    for (ProcessId processId : ClusterSettings.getEnabledProcesses(settings)) {
      SQProcess process = SQProcess.builder(processId)
        .addProcessLifecycleListener(this)
        .addEventListener(this)
        .setWatcherDelayMs(processWatcherDelayMs)
        .build();
      processesById.put(process.getProcessId(), process);
    }
    operationalCountDown.set(processesById.size());

    tryToStartAll();
  }

  private void tryToStartAll() {
    tryToStartEs();
    tryToStartWeb();
    tryToStartCe();
  }

  private void tryToStartEs() {
    SQProcess process = processesById.get(ProcessId.ELASTICSEARCH);
    if (process != null) {
      tryToStartProcess(process, commandFactory::createEsCommand);
    }
  }

  private void tryToStartWeb() {
    SQProcess process = processesById.get(ProcessId.WEB_SERVER);
    if (process == null) {
      return;
    }
    if (!isEsClientStartable()) {
      if (firstWaitingEsLog.getAndSet(false)) {
        LOG.info("Waiting for Elasticsearch to be up and running");
      }
      return;
    }
    if (appState.isOperational(ProcessId.WEB_SERVER, false)) {
      tryToStartProcess(process, () -> commandFactory.createWebCommand(false));
    } else if (appState.tryToLockWebLeader()) {
      tryToStartProcess(process, () -> commandFactory.createWebCommand(true));
    } else {
      Optional<String> leader = appState.getLeaderHostName();
      if (leader.isPresent()) {
        LOG.info("Waiting for initialization from {}", leader.get());
      } else {
        LOG.error("Initialization failed. All nodes must be restarted");
      }
    }
  }

  private void tryToStartCe() {
    SQProcess process = processesById.get(ProcessId.COMPUTE_ENGINE);
    if (process != null && appState.isOperational(ProcessId.WEB_SERVER, true) && isEsClientStartable()) {
      tryToStartProcess(process, commandFactory::createCeCommand);
    }
  }

  private boolean isEsClientStartable() {
    boolean requireLocalEs = ClusterSettings.isLocalElasticsearchEnabled(settings);
    return appState.isOperational(ProcessId.ELASTICSEARCH, requireLocalEs);
  }

  private void tryToStartProcess(SQProcess process, Supplier<AbstractCommand> commandSupplier) {
    tryToStart(process, () -> {
      AbstractCommand command = commandSupplier.get();
      return processLauncher.launch(command);
    });
  }

  private void tryToStart(SQProcess process, Supplier<ProcessMonitor> processMonitorSupplier) {
    try {
      process.start(processMonitorSupplier);
    } catch (RuntimeException e) {
      // failed to start command -> stop everything
      terminate();
      throw e;
    }
  }

  private void stopAll() {
    // order is important for non-cluster mode
    stopProcess(ProcessId.COMPUTE_ENGINE);
    stopProcess(ProcessId.WEB_SERVER);
    stopProcess(ProcessId.ELASTICSEARCH);
  }

  /**
   * Request for graceful stop then blocks until process is stopped.
   * Returns immediately if the process is disabled in configuration.
   */
  private void stopProcess(ProcessId processId) {
    SQProcess process = processesById.get(processId);
    if (process != null) {
      process.stop(1, TimeUnit.MINUTES);
    }
  }

  /**
   * Blocks until all processes are stopped. Pending restart, if
   * any, is disabled.
   */
  @Override
  public void terminate() {
    // disable ability to request for restart
    restartRequested.set(false);
    restartDisabled.set(true);

    if (nodeLifecycle.tryToMoveTo(NodeLifecycle.State.STOPPING)) {
      LOG.info("Stopping SonarQube");
    }
    stopAll();
    if (stopperThread != null) {
      stopperThread.interrupt();
    }
    if (restarterThread != null) {
      restarterThread.interrupt();
    }
    keepAlive.countDown();
  }

  @Override
  public void awaitTermination() {
    try {
      keepAlive.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void onProcessEvent(ProcessId processId, Type type) {
    if (type == Type.OPERATIONAL) {
      onProcessOperational(processId);
    } else if (type == Type.ASK_FOR_RESTART && restartRequested.compareAndSet(false, true)) {
      stopAsync();
    }
  }

  private void onProcessOperational(ProcessId processId) {
    LOG.info("Process[{}] is up", processId.getKey());
    appState.setOperational(processId);
    if (operationalCountDown.decrementAndGet() == 0 && nodeLifecycle.tryToMoveTo(NodeLifecycle.State.OPERATIONAL)) {
      LOG.info("SonarQube is up");
    }
  }

  @Override
  public void onAppStateOperational(ProcessId processId) {
    if (nodeLifecycle.getState() == NodeLifecycle.State.STARTING) {
      tryToStartAll();
    }
  }

  @Override
  public void onProcessState(ProcessId processId, Lifecycle.State to) {
    switch (to) {
      case STOPPED:
        onProcessStop(processId);
        break;
      case STARTING:
        stopCountDown.incrementAndGet();
        break;
      default:
        // Nothing to do
        break;
    }
  }

  private void onProcessStop(ProcessId processId) {
    LOG.info("Process [{}] is stopped", processId.getKey());
    if (stopCountDown.decrementAndGet() == 0 && nodeLifecycle.tryToMoveTo(NodeLifecycle.State.STOPPED)) {
      if (!restartDisabled.get() &&
        restartRequested.compareAndSet(true, false)) {
        LOG.info("SonarQube is restarting");
        restartAsync();
      } else {
        LOG.info("SonarQube is stopped");
        // all processes are stopped, no restart requested
        // Let's clean-up resources
        terminate();
      }

    } else if (nodeLifecycle.tryToMoveTo(NodeLifecycle.State.STOPPING)) {
      // this is the first process stopping
      stopAsync();
    }
  }

  private void stopAsync() {
    stopperThread = new StopperThread();
    stopperThread.start();
  }

  private void restartAsync() {
    restarterThread = new RestarterThread();
    restarterThread.start();
  }

  private class RestarterThread extends Thread {
    public RestarterThread() {
      super("Restarter");
    }

    @Override
    public void run() {
      try {
        appReloader.reload(settings);
        schedule();
      } catch (Exception e) {
        LOG.error("Fail to restart", e);
        terminate();
      }
    }
  }

  private class StopperThread extends Thread {
    public StopperThread() {
      super("Stopper");
    }

    @Override
    public void run() {
      stopAll();
    }
  }
}
