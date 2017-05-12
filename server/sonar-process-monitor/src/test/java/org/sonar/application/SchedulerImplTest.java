/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.mockito.Mockito;
import org.sonar.application.config.TestAppSettings;
import org.sonar.application.process.JavaCommand;
import org.sonar.application.process.JavaCommandFactory;
import org.sonar.application.process.JavaProcessLauncher;
import org.sonar.application.process.ProcessMonitor;
import org.sonar.process.ProcessId;
import org.sonar.process.ProcessProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.sonar.process.ProcessId.COMPUTE_ENGINE;
import static org.sonar.process.ProcessId.ELASTICSEARCH;
import static org.sonar.process.ProcessId.WEB_SERVER;

public class SchedulerImplTest {

  private static final JavaCommand ES_COMMAND = new JavaCommand(ELASTICSEARCH);
  private static final JavaCommand WEB_LEADER_COMMAND = new JavaCommand(WEB_SERVER);
  private static final JavaCommand WEB_FOLLOWER_COMMAND = new JavaCommand(WEB_SERVER);
  private static final JavaCommand CE_COMMAND = new JavaCommand(COMPUTE_ENGINE);

  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60));
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AppReloader appReloader = mock(AppReloader.class);
  private TestAppSettings settings = new TestAppSettings();
  private TestJavaCommandFactory javaCommandFactory = new TestJavaCommandFactory();
  private TestJavaProcessLauncher processLauncher = new TestJavaProcessLauncher();
  private TestAppState appState = new TestAppState();
  private List<ProcessId> orderedStops = new ArrayList<>();

  @After
  public void tearDown() throws Exception {
    processLauncher.close();
  }

  @Test
  public void start_and_stop_sequence_of_ES_WEB_CE_in_order() throws Exception {
    enableAllProcesses();
    SchedulerImpl underTest = newScheduler();
    underTest.schedule();

    // elasticsearch does not have preconditions to start
    TestProcess es = processLauncher.waitForProcess(ELASTICSEARCH);
    assertThat(es.isAlive()).isTrue();
    assertThat(processLauncher.processes).hasSize(1);

    // elasticsearch becomes operational -> web leader is starting
    es.operational = true;
    waitForAppStateOperational(ELASTICSEARCH);
    TestProcess web = processLauncher.waitForProcess(WEB_SERVER);
    assertThat(web.isAlive()).isTrue();
    assertThat(processLauncher.processes).hasSize(2);
    assertThat(processLauncher.commands).containsExactly(ES_COMMAND, WEB_LEADER_COMMAND);

    // web becomes operational -> CE is starting
    web.operational = true;
    waitForAppStateOperational(WEB_SERVER);
    TestProcess ce = processLauncher.waitForProcess(COMPUTE_ENGINE);
    assertThat(ce.isAlive()).isTrue();
    assertThat(processLauncher.processes).hasSize(3);
    assertThat(processLauncher.commands).containsExactly(ES_COMMAND, WEB_LEADER_COMMAND, CE_COMMAND);

    // all processes are up
    processLauncher.processes.values().forEach(p -> assertThat(p.isAlive()).isTrue());

    // processes are stopped in reverse order of startup
    underTest.terminate();
    assertThat(orderedStops).containsExactly(COMPUTE_ENGINE, WEB_SERVER, ELASTICSEARCH);
    processLauncher.processes.values().forEach(p -> assertThat(p.isAlive()).isFalse());

    // does nothing because scheduler is already terminated
    underTest.awaitTermination();
  }

  private void enableAllProcesses() {
    settings.set(ProcessProperties.CLUSTER_ENABLED, "true");
  }

  @Test
  public void all_processes_are_stopped_if_one_process_goes_down() throws Exception {
    Scheduler underTest = startAll();

    processLauncher.waitForProcess(WEB_SERVER).destroyForcibly();

    underTest.awaitTermination();
    assertThat(orderedStops).containsExactly(WEB_SERVER, COMPUTE_ENGINE, ELASTICSEARCH);
    processLauncher.processes.values().forEach(p -> assertThat(p.isAlive()).isFalse());

    // following does nothing
    underTest.terminate();
    underTest.awaitTermination();
  }

  @Test
  public void all_processes_are_stopped_if_one_process_fails_to_start() throws Exception {
    enableAllProcesses();
    SchedulerImpl underTest = newScheduler();
    processLauncher.makeStartupFail = COMPUTE_ENGINE;

    underTest.schedule();

    processLauncher.waitForProcess(ELASTICSEARCH).operational = true;
    processLauncher.waitForProcess(WEB_SERVER).operational = true;

    underTest.awaitTermination();
    assertThat(orderedStops).containsExactly(WEB_SERVER, ELASTICSEARCH);
    processLauncher.processes.values().forEach(p -> assertThat(p.isAlive()).isFalse());
  }

  @Test
  public void terminate_can_be_called_multiple_times() throws Exception {
    Scheduler underTest = startAll();

    underTest.terminate();
    processLauncher.processes.values().forEach(p -> assertThat(p.isAlive()).isFalse());

    // does nothing
    underTest.terminate();
  }

  @Test
  public void awaitTermination_blocks_until_all_processes_are_stopped() throws Exception {
    Scheduler underTest = startAll();

    Thread awaitingTermination = new Thread(() -> underTest.awaitTermination());
    awaitingTermination.start();
    assertThat(awaitingTermination.isAlive()).isTrue();

    underTest.terminate();
    // the thread is being stopped
    awaitingTermination.join();
    assertThat(awaitingTermination.isAlive()).isFalse();
  }

  @Test
  public void restart_reloads_java_commands_and_restarts_all_processes() throws Exception {
    Scheduler underTest = startAll();

    processLauncher.waitForProcess(WEB_SERVER).askedForRestart = true;

    // waiting for all processes to be stopped
    boolean stopped = false;
    while (!stopped) {
      stopped = orderedStops.size() == 3;
      Thread.sleep(1L);
    }

    // restarting
    verify(appReloader, timeout(60_000)).reload(settings);
    processLauncher.waitForProcessAlive(ELASTICSEARCH);
    processLauncher.waitForProcessAlive(COMPUTE_ENGINE);
    processLauncher.waitForProcessAlive(WEB_SERVER);

    underTest.terminate();
    // 3+3 processes have been stopped
    assertThat(orderedStops).hasSize(6);
    assertThat(processLauncher.waitForProcess(ELASTICSEARCH).isAlive()).isFalse();
    assertThat(processLauncher.waitForProcess(COMPUTE_ENGINE).isAlive()).isFalse();
    assertThat(processLauncher.waitForProcess(WEB_SERVER).isAlive()).isFalse();

    // verify that awaitTermination() does not block
    underTest.awaitTermination();
  }

  @Test
  public void restart_stops_all_if_new_settings_are_not_allowed() throws Exception {
    Scheduler underTest = startAll();
    doThrow(new IllegalStateException("reload error")).when(appReloader).reload(settings);

    processLauncher.waitForProcess(WEB_SERVER).askedForRestart = true;

    // waiting for all processes to be stopped
    processLauncher.waitForProcessDown(ELASTICSEARCH);
    processLauncher.waitForProcessDown(COMPUTE_ENGINE);
    processLauncher.waitForProcessDown(WEB_SERVER);

    // verify that awaitTermination() does not block
    underTest.awaitTermination();
  }

  @Test
  public void web_follower_starts_only_when_web_leader_is_operational() throws Exception {
    // leader takes the lock, so underTest won't get it
    assertThat(appState.tryToLockWebLeader()).isTrue();

    appState.setOperational(ProcessId.ELASTICSEARCH);
    enableAllProcesses();
    SchedulerImpl underTest = newScheduler();
    underTest.schedule();

    processLauncher.waitForProcessAlive(ProcessId.ELASTICSEARCH);
    assertThat(processLauncher.processes).hasSize(1);

    // leader becomes operational -> follower can start
    appState.setOperational(ProcessId.WEB_SERVER);
    processLauncher.waitForProcessAlive(WEB_SERVER);

    underTest.terminate();
  }

  @Test
  public void web_server_waits_for_remote_elasticsearch_to_be_started_if_local_es_is_disabled() throws Exception {
    settings.set(ProcessProperties.CLUSTER_ENABLED, "true");
    settings.set(ProcessProperties.CLUSTER_SEARCH_DISABLED, "true");
    SchedulerImpl underTest = newScheduler();
    underTest.schedule();

    // WEB and CE wait for ES to be up
    assertThat(processLauncher.processes).isEmpty();

    // ES becomes operational on another node -> web leader can start
    appState.setRemoteOperational(ProcessId.ELASTICSEARCH);
    processLauncher.waitForProcessAlive(WEB_SERVER);
    assertThat(processLauncher.processes).hasSize(1);

    underTest.terminate();
  }

  @Test
  public void compute_engine_waits_for_remote_elasticsearch_and_web_leader_to_be_started_if_local_es_is_disabled() throws Exception {
    settings.set(ProcessProperties.CLUSTER_ENABLED, "true");
    settings.set(ProcessProperties.CLUSTER_SEARCH_DISABLED, "true");
    settings.set(ProcessProperties.CLUSTER_WEB_DISABLED, "true");
    SchedulerImpl underTest = newScheduler();
    underTest.schedule();

    // CE waits for ES and WEB leader to be up
    assertThat(processLauncher.processes).isEmpty();

    // ES and WEB leader become operational on another nodes -> CE can start
    appState.setRemoteOperational(ProcessId.ELASTICSEARCH);
    appState.setRemoteOperational(ProcessId.WEB_SERVER);

    processLauncher.waitForProcessAlive(COMPUTE_ENGINE);
    assertThat(processLauncher.processes).hasSize(1);

    underTest.terminate();
  }

  private SchedulerImpl newScheduler() {
    return new SchedulerImpl(settings, appReloader, javaCommandFactory, processLauncher, appState)
      .setProcessWatcherDelayMs(1L);
  }

  private Scheduler startAll() throws InterruptedException {
    enableAllProcesses();
    SchedulerImpl scheduler = newScheduler();
    scheduler.schedule();
    processLauncher.waitForProcess(ELASTICSEARCH).operational = true;
    processLauncher.waitForProcess(WEB_SERVER).operational = true;
    processLauncher.waitForProcess(COMPUTE_ENGINE).operational = true;
    return scheduler;
  }

  private void waitForAppStateOperational(ProcessId id) throws InterruptedException {
    while (true) {
      if (appState.isOperational(id, true)) {
        return;
      }
      Thread.sleep(1L);
    }
  }

  private static class TestJavaCommandFactory implements JavaCommandFactory {
    @Override
    public JavaCommand createEsCommand() {
      return ES_COMMAND;
    }

    @Override
    public JavaCommand createWebCommand(boolean leader) {
      return leader ? WEB_LEADER_COMMAND : WEB_FOLLOWER_COMMAND;
    }

    @Override
    public JavaCommand createCeCommand() {
      return CE_COMMAND;
    }
  }

  private class TestJavaProcessLauncher implements JavaProcessLauncher {
    private final EnumMap<ProcessId, TestProcess> processes = new EnumMap<>(ProcessId.class);
    private final List<JavaCommand> commands = new ArrayList<>();
    private ProcessId makeStartupFail = null;

    @Override
    public ProcessMonitor launch(JavaCommand javaCommand) {
      commands.add(javaCommand);
      if (makeStartupFail == javaCommand.getProcessId()) {
        throw new IllegalStateException("cannot start " + javaCommand.getProcessId());
      }
      TestProcess process = new TestProcess(javaCommand.getProcessId());
      processes.put(javaCommand.getProcessId(), process);
      return process;
    }

    private TestProcess waitForProcess(ProcessId id) throws InterruptedException {
      while (true) {
        TestProcess p = processes.get(id);
        if (p != null) {
          return p;
        }
        Thread.sleep(1L);
      }
    }

    private TestProcess waitForProcessAlive(ProcessId id) throws InterruptedException {
      while (true) {
        TestProcess p = processes.get(id);
        if (p != null && p.isAlive()) {
          return p;
        }
        Thread.sleep(1L);
      }
    }

    private TestProcess waitForProcessDown(ProcessId id) throws InterruptedException {
      while (true) {
        TestProcess p = processes.get(id);
        if (p != null && !p.isAlive()) {
          return p;
        }
        Thread.sleep(1L);
      }
    }

    @Override
    public void close() {
      for (TestProcess process : processes.values()) {
        process.destroyForcibly();
      }
    }
  }

  private class TestProcess implements ProcessMonitor, AutoCloseable {
    private final ProcessId processId;
    private final CountDownLatch alive = new CountDownLatch(1);
    private boolean operational = false;
    private boolean askedForRestart = false;

    private TestProcess(ProcessId processId) {
      this.processId = processId;
    }

    @Override
    public InputStream getInputStream() {
      return mock(InputStream.class, Mockito.RETURNS_MOCKS);
    }

    @Override
    public void closeStreams() {
    }

    @Override
    public boolean isAlive() {
      return alive.getCount() == 1;
    }

    @Override
    public void askForStop() {
      destroyForcibly();
    }

    @Override
    public void destroyForcibly() {
      if (isAlive()) {
        orderedStops.add(processId);
      }
      alive.countDown();
    }

    @Override
    public void waitFor() throws InterruptedException {
      alive.await();
    }

    @Override
    public void waitFor(long timeout, TimeUnit timeoutUnit) throws InterruptedException {
      alive.await(timeout, timeoutUnit);
    }

    @Override
    public boolean isOperational() {
      return operational;
    }

    @Override
    public boolean askedForRestart() {
      return askedForRestart;
    }

    @Override
    public void acknowledgeAskForRestart() {
      this.askedForRestart = false;
    }

    @Override
    public void close() {
      alive.countDown();
    }
  }
}
