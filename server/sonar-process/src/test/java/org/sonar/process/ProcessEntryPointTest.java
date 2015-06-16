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

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonar.process.Lifecycle.State;
import org.sonar.process.test.StandardProcess;

import java.io.File;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class ProcessEntryPointTest {

  SystemExit exit = mock(SystemExit.class);

  /**
   * Safeguard
   */
  @Rule
  public TestRule timeout = new DisableOnDebug(Timeout.seconds(60));

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

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
    Props props = new Props(new Properties());
    ProcessEntryPoint entryPoint = new ProcessEntryPoint(props, exit, mock(ProcessCommands.class));

    assertThat(entryPoint.getProps()).isSameAs(props);
    assertThat(entryPoint.isStarted()).isFalse();
    assertThat(entryPoint.getState()).isEqualTo(State.INIT);
  }

  @Test
  public void fail_to_launch_multiple_times() {
    Props props = new Props(new Properties());
    props.set(ProcessEntryPoint.PROPERTY_PROCESS_KEY, "test");
    props.set(ProcessEntryPoint.PROPERTY_TERMINATION_TIMEOUT, "30000");
    ProcessEntryPoint entryPoint = new ProcessEntryPoint(props, exit, mock(ProcessCommands.class));

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
    Props props = new Props(new Properties());
    props.set(ProcessEntryPoint.PROPERTY_PROCESS_KEY, "test");
    props.set(ProcessEntryPoint.PROPERTY_TERMINATION_TIMEOUT, "30000");
    final ProcessEntryPoint entryPoint = new ProcessEntryPoint(props, exit, mock(ProcessCommands.class));
    final StandardProcess process = new StandardProcess();

    Thread runner = new Thread() {
      @Override
      public void run() {
        // starts and waits until terminated
        entryPoint.launch(process);
      }
    };
    runner.start();

    while (process.getState() != State.STARTED) {
      Thread.sleep(10L);
    }

    // requests for graceful stop -> waits until down
    // Should terminate before the timeout of 30s
    entryPoint.stop();

    assertThat(process.getState()).isEqualTo(State.STOPPED);
  }

  @Test
  public void terminate_if_unexpected_shutdown() throws Exception {
    Props props = new Props(new Properties());
    props.set(ProcessEntryPoint.PROPERTY_PROCESS_KEY, "foo");
    props.set(ProcessEntryPoint.PROPERTY_TERMINATION_TIMEOUT, "30000");
    final ProcessEntryPoint entryPoint = new ProcessEntryPoint(props, exit, mock(ProcessCommands.class));
    final StandardProcess process = new StandardProcess();

    Thread runner = new Thread() {
      @Override
      public void run() {
        // starts and waits until terminated
        entryPoint.launch(process);
      }
    };
    runner.start();
    while (process.getState() != State.STARTED) {
      Thread.sleep(10L);
    }

    // emulate signal to shutdown process
    entryPoint.getShutdownHook().start();

    // hack to prevent JUnit JVM to fail when executing the shutdown hook a second time
    Runtime.getRuntime().removeShutdownHook(entryPoint.getShutdownHook());

    while (process.getState() != State.STOPPED) {
      Thread.sleep(10L);
    }
    // exit before test timeout, ok !
  }

  @Test
  public void terminate_if_startup_error() {
    Props props = new Props(new Properties());
    props.set(ProcessEntryPoint.PROPERTY_PROCESS_KEY, "foo");
    props.set(ProcessEntryPoint.PROPERTY_TERMINATION_TIMEOUT, "30000");
    final ProcessEntryPoint entryPoint = new ProcessEntryPoint(props, exit, mock(ProcessCommands.class));
    final Monitored process = new StartupErrorProcess();

    entryPoint.launch(process);
    assertThat(entryPoint.getState()).isEqualTo(State.STOPPED);
  }

  private static class NoopProcess implements Monitored {

    @Override
    public void start() {

    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void awaitStop() {

    }

    @Override
    public void stop() {

    }
  }

  private static class StartupErrorProcess implements Monitored {

    @Override
    public void start() {
      throw new IllegalStateException("ERROR");
    }

    @Override
    public boolean isReady() {
      return false;
    }

    @Override
    public void awaitStop() {

    }

    @Override
    public void stop() {

    }
  }
}
