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
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.mockito.ArgumentCaptor;
import org.sonar.process.Lifecycle.State;
import org.sonar.process.sharedmemoryfile.ProcessCommands;
import org.sonar.process.test.StandardProcess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_GRACEFUL_STOP_TIMEOUT_MS;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_PROCESS_INDEX;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_PROCESS_KEY;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_SHARED_PATH;

public class ProcessEntryPointTest {

  private SystemExit exit = mock(SystemExit.class);

  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60));

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private ProcessCommands commands = new OperationalFlagOnlyProcessCommands();
  private Runtime runtime;

  @Before
  public void setUp() {
    runtime = mock(Runtime.class);
  }

  @Test
  public void load_properties_from_file() throws Exception {
    File propsFile = temp.newFile();
    FileUtils.write(propsFile, "sonar.foo=bar\nprocess.key=web\nprocess.index=1\nprocess.sharedDir=" + temp.newFolder().getAbsolutePath().replaceAll("\\\\", "/"));

    ProcessEntryPoint entryPoint = ProcessEntryPoint.createForArguments(new String[] {propsFile.getAbsolutePath()});
    assertThat(entryPoint.getProps().value("sonar.foo")).isEqualTo("bar");
    assertThat(entryPoint.getProps().value("process.key")).isEqualTo("web");
  }

  @Test
  public void test_initial_state() throws Exception {
    Props props = createProps();
    ProcessEntryPoint entryPoint = new ProcessEntryPoint(props, exit, commands, runtime);

    assertThat(entryPoint.getProps()).isSameAs(props);
  }

  @Test
  public void fail_to_launch_multiple_times() throws IOException {
    Props props = createProps();
    ProcessEntryPoint entryPoint = new ProcessEntryPoint(props, exit, commands, runtime);

    entryPoint.launch(new NoopProcess());
    try {
      entryPoint.launch(new NoopProcess());
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Already started");
    }
  }

  @Test
  public void launch_then_request_graceful_stop() throws Exception {
    Props props = createProps();
    final ProcessEntryPoint entryPoint = new ProcessEntryPoint(props, exit, commands, runtime);
    final StandardProcess process = new StandardProcess();

    new Thread(() -> entryPoint.launch(process)).start();

    waitForOperational(process, commands);

    // requests for graceful stop -> waits until down
    // Should terminate before the timeout of 30s
    entryPoint.stop();

    assertThat(process.getState()).isEqualTo(State.STOPPED);
    assertThat(process.wasStopped()).isEqualTo(true);
    assertThat(process.wasHardStopped()).isEqualTo(false);
  }

  @Test
  public void launch_then_request_hard_stop() throws Exception {
    Props props = createProps();
    final ProcessEntryPoint entryPoint = new ProcessEntryPoint(props, exit, commands, runtime);
    final StandardProcess process = new StandardProcess();

    // starts and waits until terminated
    Thread runner = new Thread(() -> entryPoint.launch(process));
    runner.start();

    waitForOperational(process, commands);

    // requests for stop hardly waiting
    entryPoint.hardStop();

    assertThat(process.getState()).isEqualTo(State.STOPPED);
    assertThat(process.wasHardStopped()).isEqualTo(true);
  }

  @Test
  public void terminate_if_unexpected_shutdown() throws Exception {
    Props props = createProps();
    final ProcessEntryPoint entryPoint = new ProcessEntryPoint(props, exit, commands, runtime);
    final StandardProcess process = new StandardProcess();

    Thread runner = new Thread() {
      @Override
      public void run() {
        // starts and waits until terminated
        entryPoint.launch(process);
      }
    };
    runner.start();

    waitForOperational(process, commands);

    // emulate signal to shutdown process
    ArgumentCaptor<Thread> shutdownHookCaptor = ArgumentCaptor.forClass(Thread.class);
    verify(runtime).addShutdownHook(shutdownHookCaptor.capture());
    shutdownHookCaptor.getValue().start();

    while (process.getState() != State.STOPPED) {
      Thread.sleep(10L);
    }
    // exit before test timeout, ok !
  }

  @Test
  public void terminate_if_startup_error() throws IOException {
    Props props = createProps();
    final ProcessEntryPoint entryPoint = new ProcessEntryPoint(props, exit, commands, runtime);
    final Monitored process = mock(Monitored.class);
    doThrow(IllegalStateException.class).when(process).start();

    entryPoint.launch(process);
  }

  private static void waitForOperational(StandardProcess process, ProcessCommands commands) throws InterruptedException {
    while (!(process.getState() == State.STARTED && commands.isOperational())) {
      Thread.sleep(10L);
    }
  }

  private Props createProps() throws IOException {
    Props props = new Props(new Properties());
    props.set(PROPERTY_SHARED_PATH, temp.newFolder().getAbsolutePath());
    props.set(PROPERTY_PROCESS_INDEX, "1");
    props.set(PROPERTY_PROCESS_KEY, "test");
    props.set(PROPERTY_GRACEFUL_STOP_TIMEOUT_MS, "30000");
    return props;
  }

  private static class NoopProcess implements Monitored {

    @Override
    public void start() {

    }

    @Override
    public Status getStatus() {
      return Status.OPERATIONAL;
    }

    @Override
    public void awaitStop() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void hardStop() {

    }
  }

  private static class OperationalFlagOnlyProcessCommands implements ProcessCommands {
    private final AtomicBoolean operational = new AtomicBoolean(false);

    @Override
    public boolean isUp() {
      return false;
    }

    @Override
    public void setUp() {

    }

    @Override
    public boolean isOperational() {
      return operational.get();
    }

    @Override
    public void setOperational() {
      operational.set(true);
    }

    @Override
    public void ping() {

    }

    @Override
    public long getLastPing() {
      return 0;
    }

    @Override
    public void setHttpUrl(String s) {

    }

    @Override
    public String getHttpUrl() {
      return null;
    }

    @Override
    public void askForStop() {

    }

    @Override
    public boolean askedForStop() {
      return false;
    }

    @Override
    public void askForHardStop() {

    }

    @Override
    public boolean askedForHardStop() {
      return false;
    }

    @Override
    public void askForRestart() {

    }

    @Override
    public boolean askedForRestart() {
      return false;
    }

    @Override
    public void acknowledgeAskForRestart() {

    }

    @Override
    public void endWatch() {

    }
  }
}
