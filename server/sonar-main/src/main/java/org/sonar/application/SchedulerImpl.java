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
package org.sonar.application;

import java.util.EnumMap;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.application.command.AbstractCommand;
import org.sonar.application.command.CommandFactory;
import org.sonar.application.config.AppSettings;
import org.sonar.application.config.ClusterSettings;
import org.sonar.application.process.ManagedProcessEventListener;
import org.sonar.application.process.ManagedProcessHandler;
import org.sonar.application.process.ManagedProcessLifecycle;
import org.sonar.application.process.ProcessLifecycleListener;
import org.sonar.process.ProcessId;
import org.sonar.process.ProcessProperties;

import static org.sonar.application.NodeLifecycle.State.FINALIZE_STOPPING;
import static org.sonar.application.NodeLifecycle.State.HARD_STOPPING;
import static org.sonar.application.NodeLifecycle.State.RESTARTING;
import static org.sonar.application.NodeLifecycle.State.STOPPED;
import static org.sonar.application.NodeLifecycle.State.STOPPING;
import static org.sonar.application.process.ManagedProcessHandler.Timeout.newTimeout;
import static org.sonar.process.ProcessProperties.Property.CE_GRACEFUL_STOP_TIMEOUT;
import static org.sonar.process.ProcessProperties.Property.WEB_GRACEFUL_STOP_TIMEOUT;
import static org.sonar.process.ProcessProperties.parseTimeoutMs;

public class SchedulerImpl implements Scheduler, ManagedProcessEventListener, ProcessLifecycleListener, AppStateListener {

  private static final Logger LOG = LoggerFactory.getLogger(SchedulerImpl.class);
  private static final ManagedProcessHandler.Timeout HARD_STOP_TIMEOUT = newTimeout(1, TimeUnit.MINUTES);
  private static int hardStopperThreadIndex = 0;
  private static int restartStopperThreadIndex = 0;

  private final AppSettings settings;
  private final AppReloader appReloader;
  private final CommandFactory commandFactory;
  private final ProcessLauncher processLauncher;
  private final AppState appState;
  private final NodeLifecycle nodeLifecycle = new NodeLifecycle();

  private final CountDownLatch awaitTermination = new CountDownLatch(1);
  private final AtomicBoolean firstWaitingEsLog = new AtomicBoolean(true);
  private final EnumMap<ProcessId, ManagedProcessHandler> processesById = new EnumMap<>(ProcessId.class);
  private final AtomicInteger operationalCountDown = new AtomicInteger();
  private final AtomicInteger stopCountDown = new AtomicInteger(0);
  private RestartStopperThread restartStopperThread;
  private HardStopperThread hardStopperThread;
  private RestarterThread restarterThread;
  private long processWatcherDelayMs = ManagedProcessHandler.DEFAULT_WATCHER_DELAY_MS;

  public SchedulerImpl(AppSettings settings, AppReloader appReloader, CommandFactory commandFactory,
    ProcessLauncher processLauncher, AppState appState) {
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
  public void schedule() throws InterruptedException {
    if (!nodeLifecycle.tryToMoveTo(NodeLifecycle.State.STARTING)) {
      return;
    }
    processesById.clear();

    for (ProcessId processId : ClusterSettings.getEnabledProcesses(settings)) {
      ManagedProcessHandler process = ManagedProcessHandler.builder(processId)
        .addProcessLifecycleListener(this)
        .addEventListener(this)
        .setWatcherDelayMs(processWatcherDelayMs)
        .setStopTimeout(stopTimeoutFor(processId, settings))
        .setHardStopTimeout(HARD_STOP_TIMEOUT)
        .build();
      processesById.put(process.getProcessId(), process);
    }
    operationalCountDown.set(processesById.size());

    tryToStartAll();
  }

  private static ManagedProcessHandler.Timeout stopTimeoutFor(ProcessId processId, AppSettings settings) {
    switch (processId) {
      case ELASTICSEARCH:
        return HARD_STOP_TIMEOUT;
      case WEB_SERVER:
        return newTimeout(getStopTimeoutMs(settings, WEB_GRACEFUL_STOP_TIMEOUT), TimeUnit.MILLISECONDS);
      case COMPUTE_ENGINE:
        return newTimeout(getStopTimeoutMs(settings, CE_GRACEFUL_STOP_TIMEOUT), TimeUnit.MILLISECONDS);
      case APP:
      default:
        throw new IllegalArgumentException("Unsupported processId " + processId);
    }
  }

  private static long getStopTimeoutMs(AppSettings settings, ProcessProperties.Property property) {
    String timeoutMs = settings.getValue(property.getKey())
      .orElse(property.getDefaultValue());
    // give some time to CE/Web to shutdown itself after "timeoutMs"
    long gracePeriod = HARD_STOP_TIMEOUT.getUnit().toMillis(HARD_STOP_TIMEOUT.getDuration());
    return parseTimeoutMs(property, timeoutMs) + gracePeriod;
  }

  private void tryToStartAll() throws InterruptedException {
    tryToStartEs();
    tryToStartWeb();
    tryToStartCe();
  }

  private void tryToStartEs() throws InterruptedException {
    ManagedProcessHandler process = processesById.get(ProcessId.ELASTICSEARCH);
    if (process != null) {
      tryToStartProcess(process, commandFactory::createEsCommand);
    }
  }

  private void tryToStartWeb() throws InterruptedException {
    ManagedProcessHandler process = processesById.get(ProcessId.WEB_SERVER);
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

  private void tryToStartCe() throws InterruptedException {
    ManagedProcessHandler process = processesById.get(ProcessId.COMPUTE_ENGINE);
    if (process != null && appState.isOperational(ProcessId.WEB_SERVER, true) && isEsClientStartable()) {
      tryToStartProcess(process, commandFactory::createCeCommand);
    }
  }

  private boolean isEsClientStartable() {
    boolean requireLocalEs = ClusterSettings.isLocalElasticsearchEnabled(settings);
    return appState.isOperational(ProcessId.ELASTICSEARCH, requireLocalEs);
  }

  private void tryToStartProcess(ManagedProcessHandler processHandler, Supplier<AbstractCommand> commandSupplier) throws InterruptedException {
    // starter or restarter thread was interrupted, we should not proceed with starting the process
    if (Thread.currentThread().isInterrupted()) {
      throw new InterruptedException();
    }

    try {
      processHandler.start(() -> {
        AbstractCommand command = commandSupplier.get();
        return processLauncher.launch(command);
      });
    } catch (RuntimeException e) {
      // failed to start command -> do nothing
      // the process failing to start will move directly to STOP state
      // this early stop of the process will be picked up by onProcessStop (which calls hardStopAsync)
      // through interface ProcessLifecycleListener#onProcessState implemented by SchedulerImpl
      LOG.trace("Failed to start process [{}] (currentThread={})",
        processHandler.getProcessId().getKey(), Thread.currentThread().getName(), e);
    }
  }

  @Override
  public void stop() {
    if (nodeLifecycle.tryToMoveTo(STOPPING)) {
      LOG.info("Stopping SonarQube");
      stopImpl();
    }
  }

  private void stopImpl() {
    try {
      stopAll();
      finalizeStop();
    } catch (InterruptedException e) {
      LOG.debug("Stop interrupted", e);
      Thread.currentThread().interrupt();
    }
  }

  private void stopAll() throws InterruptedException {
    // order is important for non-cluster mode
    stopProcess(ProcessId.COMPUTE_ENGINE);
    stopProcess(ProcessId.WEB_SERVER);
    stopProcess(ProcessId.ELASTICSEARCH);
  }

  /**
   * Request for graceful stop then blocks until process is stopped.
   * Returns immediately if the process is disabled in configuration.
   *
   * @throws InterruptedException if {@link ManagedProcessHandler#hardStop()} throws a {@link InterruptedException}
   */
  private void stopProcess(ProcessId processId) throws InterruptedException {
    ManagedProcessHandler process = processesById.get(processId);
    if (process != null) {
      LOG.debug("Stopping [{}]...", process.getProcessId().getKey());
      process.stop();
    }
  }

  /**
   * Blocks until all processes are quickly stopped. Pending restart, if any, is disabled.
   */
  @Override
  public void hardStop() {
    if (nodeLifecycle.tryToMoveTo(HARD_STOPPING)) {
      LOG.info("Hard stopping SonarQube");
      hardStopImpl();
    }
  }

  private void hardStopImpl() {
    try {
      hardStopAll();
      finalizeStop();
    } catch (InterruptedException e) {
      // ignore and assume SQ stop is handled by another thread
      LOG.debug("Stopping all processes was interrupted in the middle of a hard stop" +
        " (current thread name is \"{}\")", Thread.currentThread().getName());
      Thread.currentThread().interrupt();
    }
  }

  private void hardStopAll() throws InterruptedException {
    // order is important for non-cluster mode
    hardStopProcess(ProcessId.COMPUTE_ENGINE);
    hardStopProcess(ProcessId.WEB_SERVER);
    hardStopProcess(ProcessId.ELASTICSEARCH);
  }

  /**
   * This might be called twice: once by the state listener and once by the stop/hardStop implementations.
   * The reason is that if all process are already stopped (may occur, eg., when stopping because restart of 1st process failed),
   * the node state won't be updated on process stopped callback.
   */
  private void finalizeStop() {
    if (nodeLifecycle.tryToMoveTo(FINALIZE_STOPPING)) {
      interrupt(restartStopperThread);
      interrupt(hardStopperThread);
      interrupt(restarterThread);
      if (nodeLifecycle.tryToMoveTo(STOPPED)) {
        LOG.info("SonarQube is stopped");
      }
      awaitTermination.countDown();
    }
  }

  private static void interrupt(@Nullable Thread thread) {
    Thread currentThread = Thread.currentThread();
    // prevent current thread from interrupting itself
    if (thread != null && currentThread != thread) {
      thread.interrupt();
      if (LOG.isTraceEnabled()) {
        Exception e = new Exception("(capturing stacktrace for debugging purpose)");
        LOG.trace("{} interrupted {}", currentThread.getName(), thread.getName(), e);
      }
    }
  }

  /**
   * Request for graceful stop then blocks until process is stopped.
   * Returns immediately if the process is disabled in configuration.
   *
   * @throws InterruptedException if {@link ManagedProcessHandler#hardStop()} throws a {@link InterruptedException}
   */
  private void hardStopProcess(ProcessId processId) throws InterruptedException {
    ManagedProcessHandler process = processesById.get(processId);
    if (process != null) {
      process.hardStop();
    }
  }

  @Override
  public void awaitTermination() {
    try {
      awaitTermination.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void onManagedProcessEvent(ProcessId processId, Type type) {
    if (type == Type.OPERATIONAL) {
      onProcessOperational(processId);
    } else if (type == Type.ASK_FOR_RESTART && nodeLifecycle.tryToMoveTo(RESTARTING)) {
      LOG.info("SQ restart requested by Process[{}]", processId.getKey());
      stopAsyncForRestart();
    }
  }

  private void onProcessOperational(ProcessId processId) {
    LOG.info("Process[{}] is up", processId.getKey());
    appState.setOperational(processId);
    boolean lastProcessStarted = operationalCountDown.decrementAndGet() == 0;
    if (lastProcessStarted && nodeLifecycle.tryToMoveTo(NodeLifecycle.State.OPERATIONAL)) {
      LOG.info("SonarQube is up");
    }
  }

  @Override
  public void onAppStateOperational(ProcessId processId) {
    if (nodeLifecycle.getState() == NodeLifecycle.State.STARTING) {
      try {
        tryToStartAll();
      } catch (InterruptedException e) {
        // startup process was interrupted, let's assume it means shutdown was requested
        LOG.debug("Startup process was interrupted on notification that process [{}] was operation", processId.getKey(), e);
        hardStopAsync();
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public void onProcessState(ProcessId processId, ManagedProcessLifecycle.State to) {
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
    LOG.info("Process[{}] is stopped", processId.getKey());
    boolean lastProcessStopped = stopCountDown.decrementAndGet() == 0;
    switch (nodeLifecycle.getState()) {
      case RESTARTING:
        if (lastProcessStopped) {
          LOG.info("SonarQube is restarting");
          restartAsync();
        }
        break;
      case HARD_STOPPING:
      case STOPPING:
        if (lastProcessStopped) {
          finalizeStop();
        }
        break;
      default:
        // a sub process disappeared while this wasn't requested, SQ should be shutdown completely
        hardStopAsync();
    }
  }

  private void hardStopAsync() {
    if (hardStopperThread != null) {
      logThreadRecreated("Hard stopper", hardStopperThread);
      hardStopperThread.interrupt();
    }

    hardStopperThread = new HardStopperThread();
    hardStopperThread.start();
  }

  private void stopAsyncForRestart() {
    if (restartStopperThread != null) {
      logThreadRecreated("Restart stopper", restartStopperThread);
      restartStopperThread.interrupt();
    }

    restartStopperThread = new RestartStopperThread();
    restartStopperThread.start();
  }

  private static void logThreadRecreated(String threadType, Thread existingThread) {
    if (LOG.isDebugEnabled()) {
      Exception e = new Exception("(capturing stack trace for debugging purpose)");
      LOG.debug("{} thread was not null (currentThread={},existingThread={})",
        threadType, Thread.currentThread().getName(), existingThread.getName(), e);
    }
  }

  private void restartAsync() {
    if (restarterThread != null) {
      LOG.debug("Restarter thread was not null (name is \"{}\")", restarterThread.getName(), new Exception());
      restarterThread.interrupt();
    }

    restarterThread = new RestarterThread();
    restarterThread.start();
  }

  private class RestarterThread extends Thread {
    private RestarterThread() {
      super("Restarter");
    }

    @Override
    public void run() {
      try {
        appReloader.reload(settings);
        schedule();
      } catch (InterruptedException e) {
        // restart was interrupted, most likely by a stop thread, restart must be aborted
        LOG.debug("{} thread was interrupted", getName(), e);
        super.interrupt();
      } catch (Exception e) {
        LOG.error("Failed to restart", e);
        hardStop();
      }
    }
  }

  private class RestartStopperThread extends Thread {

    private RestartStopperThread() {
      super("RestartStopper-" + restartStopperThreadIndex++);
    }

    @Override
    public void run() {
      stopImpl();
    }
  }

  private class HardStopperThread extends Thread {

    private HardStopperThread() {
      super("HardStopper-" + hardStopperThreadIndex++);
    }

    @Override
    public void run() {
      if (nodeLifecycle.tryToMoveTo(HARD_STOPPING)) {
        hardStopImpl();
      }
    }
  }
}
