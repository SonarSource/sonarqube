/*
 * SonarQube :: Process Monitor
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
package org.sonar.process.monitor;

import com.github.kevinsawicki.http.HttpRequest;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.internal.Longs;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonar.process.Lifecycle.State;
import org.sonar.process.NetworkUtils;
import org.sonar.process.ProcessCommands;
import org.sonar.process.SystemExit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.sonar.process.monitor.MonitorTest.HttpProcessClientAssert.assertThat;

public class MonitorTest {

  static File testJar;
  Monitor monitor;
  SystemExit exit = mock(SystemExit.class);

  /**
   * Find the JAR file containing the test apps. Classes can't be moved in sonar-process-monitor because
   * they require sonar-process dependencies when executed here (sonar-process, commons-*, ...).
   */
  @BeforeClass
  public static void initTestJar() {
    File targetDir = new File("server/sonar-process/target");
    if (!targetDir.exists() || !targetDir.isDirectory()) {
      targetDir = new File("../sonar-process/target");
    }
    if (!targetDir.exists() || !targetDir.isDirectory()) {
      throw new IllegalStateException("target dir of sonar-process module not found. Please build it.");
    }
    Collection<File> jars = FileUtils.listFiles(targetDir, new String[] {"jar"}, false);
    for (File jar : jars) {
      if (jar.getName().startsWith("sonar-process-") && jar.getName().endsWith("-test-jar-with-dependencies.jar")) {
        testJar = jar;
        return;
      }
    }
    throw new IllegalStateException("No sonar-process-*-test-jar-with-dependencies.jar in " + targetDir);
  }

  /**
   * Safeguard
   */
  @Rule
  public TestRule globalTimeout = new DisableOnDebug(Timeout.seconds(60));

  /**
   * Temporary directory is used to interact with monitored processes, which write in it.
   */
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  /**
   * Safeguard
   */
  @After
  public void tearDown() {
    try {
      if (monitor != null) {
        monitor.stop();
      }
    } catch (Throwable ignored) {
    }
  }

  @Test
  public void fail_to_start_if_no_commands() {
    monitor = newDefaultMonitor();
    try {
      monitor.start(Collections.<JavaCommand>emptyList());
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("At least one command is required");
    }
  }

  @Test
  public void fail_to_start_multiple_times() throws Exception {
    monitor = newDefaultMonitor();
    monitor.start(Arrays.asList(newStandardProcessCommand()));
    boolean failed = false;
    try {
      monitor.start(Arrays.asList(newStandardProcessCommand()));
    } catch (IllegalStateException e) {
      failed = e.getMessage().equals("Can not start multiple times");
    }
    monitor.stop();
    assertThat(failed).isTrue();
  }

  @Test
  public void start_then_stop_gracefully() throws Exception {
    monitor = newDefaultMonitor();
    HttpProcessClient client = new HttpProcessClient("test");
    // blocks until started
    monitor.start(Arrays.asList(client.newCommand()));

    assertThat(client).isReady()
      .wasStartedBefore(System.currentTimeMillis());

    // blocks until stopped
    monitor.stop();
    assertThat(client)
      .isNotReady()
      .wasGracefullyTerminated();
    assertThat(monitor.getState()).isEqualTo(State.STOPPED);
  }

  @Test
  public void start_then_stop_sequence_of_commands() throws Exception {
    monitor = newDefaultMonitor();
    HttpProcessClient p1 = new HttpProcessClient("p1");
    HttpProcessClient p2 = new HttpProcessClient("p2");
    monitor.start(Arrays.asList(p1.newCommand(), p2.newCommand()));

    // start p2 when p1 is fully started (ready)
    assertThat(p1)
      .isReady()
      .wasStartedBefore(p2);
    assertThat(p2)
        .isReady();

    monitor.stop();

    // stop in inverse order
    assertThat(p1)
      .isNotReady()
      .wasGracefullyTerminated();
    assertThat(p2)
      .isNotReady()
      .wasGracefullyTerminatedBefore(p1);
  }

  @Test
  public void stop_all_processes_if_monitor_shutdowns() throws Exception {
    monitor = newDefaultMonitor();
    HttpProcessClient p1 = new HttpProcessClient("p1");
    HttpProcessClient p2 = new HttpProcessClient("p2");
    monitor.start(Arrays.asList(p1.newCommand(), p2.newCommand()));
    assertThat(p1).isReady();
    assertThat(p2).isReady();

    // emulate CTRL-C
    monitor.getShutdownHook().run();
    monitor.getShutdownHook().join();

    assertThat(p1).wasGracefullyTerminated();
    assertThat(p2).wasGracefullyTerminated();
  }

  @Test
  public void restart_all_processes_if_one_asks_for_restart() throws Exception {
    monitor = newDefaultMonitor();
    HttpProcessClient p1 = new HttpProcessClient("p1");
    HttpProcessClient p2 = new HttpProcessClient("p2");
    monitor.start(Arrays.asList(p1.newCommand(), p2.newCommand()));

    assertThat(p1).isReady();
    assertThat(p2).isReady();

    p2.restart();

    assertThat(monitor.waitForOneRestart()).isTrue();

    assertThat(p1)
        .wasStarted(2)
        .wasGracefullyTerminated(1);
    assertThat(p2)
        .wasStarted(2)
        .wasGracefullyTerminated(1);

    monitor.stop();

    assertThat(p1)
        .wasStarted(2)
        .wasGracefullyTerminated(2);
    assertThat(p2)
        .wasStarted(2)
        .wasGracefullyTerminated(2);
  }

  @Test
  public void stop_all_processes_if_one_shutdowns() throws Exception {
    monitor = newDefaultMonitor();
    HttpProcessClient p1 = new HttpProcessClient("p1");
    HttpProcessClient p2 = new HttpProcessClient("p2");
    monitor.start(Arrays.asList(p1.newCommand(), p2.newCommand()));
    assertThat(p1.isReady()).isTrue();
    assertThat(p2.isReady()).isTrue();

    // kill p1 -> waiting for detection by monitor than termination of p2
    p1.kill();
    monitor.awaitTermination();

    assertThat(p1)
      .isNotReady()
      .wasNotGracefullyTerminated();
    assertThat(p2)
      .isNotReady()
      .wasGracefullyTerminated();
  }

  @Test
  public void stop_all_processes_if_one_fails_to_start() throws Exception {
    monitor = newDefaultMonitor();
    HttpProcessClient p1 = new HttpProcessClient("p1");
    HttpProcessClient p2 = new HttpProcessClient("p2", -1);
    try {
      monitor.start(Arrays.asList(p1.newCommand(), p2.newCommand()));
      fail();
    } catch (Exception expected) {
      assertThat(p1)
        .hasBeenReady()
        .wasGracefullyTerminated();
      assertThat(p2)
        .hasNotBeenReady()
        // self "gracefully terminated", even if startup went bad
        .wasGracefullyTerminated();
    }
  }

  @Test
  public void test_too_many_processes() {
    while (Monitor.getNextProcessId() < ProcessCommands.MAX_PROCESSES - 1) {
    }
    try {
      newDefaultMonitor();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessageStartingWith("The maximum number of processes launched has been reached ");
    } finally {
      Monitor.nextProcessId = 0;
    }
  }

  @Test
  public void fail_to_start_if_bad_class_name() throws Exception {
    monitor = newDefaultMonitor();
    JavaCommand command = new JavaCommand("test")
      .addClasspath(testJar.getAbsolutePath())
      .setClassName("org.sonar.process.test.Unknown")
      .setTempDir(temp.newFolder());

    try {
      monitor.start(Arrays.asList(command));
      fail();
    } catch (Exception e) {
      // expected
      // TODO improve, too many stacktraces logged
    }
  }

  private Monitor newDefaultMonitor() {
    Timeouts timeouts = new Timeouts();
    return new Monitor(new JavaProcessLauncher(timeouts), exit);
  }

  /**
   * Interaction with {@link org.sonar.process.test.HttpProcess}
   */
  private class HttpProcessClient {
    private final int httpPort;
    private final String commandKey;
    private final File tempDir;

    private HttpProcessClient(String commandKey) throws IOException {
      this(commandKey, NetworkUtils.freePort());
    }

    /**
     * Use httpPort=-1 to make server fail to start
     */
    private HttpProcessClient(String commandKey, int httpPort) throws IOException {
      this.commandKey = commandKey;
      this.tempDir = temp.newFolder(commandKey);
      this.httpPort = httpPort;
    }

    JavaCommand newCommand() {
      return new JavaCommand(commandKey)
        .addClasspath(testJar.getAbsolutePath())
        .setClassName("org.sonar.process.test.HttpProcess")
        .setArgument("httpPort", String.valueOf(httpPort))
        .setTempDir(tempDir);
    }

    /**
     * @see org.sonar.process.test.HttpProcess
     */
    boolean isReady() {
      try {
        HttpRequest httpRequest = HttpRequest.get("http://localhost:" + httpPort + "/" + "ping")
          .readTimeout(2000).connectTimeout(2000);
        return httpRequest.ok() && httpRequest.body().equals("ping");
      } catch (HttpRequest.HttpRequestException e) {
        return false;
      }
    }

    /**
     * @see org.sonar.process.test.HttpProcess
     */
    void kill() {
      try {
        HttpRequest.post("http://localhost:" + httpPort + "/" + "kill")
          .readTimeout(5000).connectTimeout(5000).ok();
      } catch (Exception e) {
        // HTTP request can't be fully processed, as web server hardly
        // calls "System.exit()"
      }
    }

    public void restart() {
      try {
        HttpRequest httpRequest = HttpRequest.post("http://localhost:" + httpPort + "/" + "restart")
            .readTimeout(5000).connectTimeout(5000);
        if (!httpRequest.ok() || !"ok".equals(httpRequest.body())) {
          throw new IllegalStateException("Wrong response calling restart");
        }
      } catch (Exception e) {
        throw new IllegalStateException("Failed to call restart", e);
      }
    }

    /**
     * @see org.sonar.process.test.HttpProcess
     */
    boolean wasGracefullyTerminated() {
      return fileExists("terminatedAt");
    }

    List<Long> wasStartingAt() {
      return readTimeFromFile("startingAt");
    }

    List<Long> wasGracefullyTerminatedAt() {
      return readTimeFromFile("terminatedAt");
    }

    boolean wasReady() {
      return fileExists("readyAt");
    }

    List<Long> wasReadyAt() {
      return readTimeFromFile("readyAt");
    }

    private List<Long> readTimeFromFile(String filename) {
      try {
        File file = new File(tempDir, filename);
        if (file.isFile() && file.exists()) {
          String[] split = StringUtils.split(FileUtils.readFileToString(file), ',');
          List<Long> res = new ArrayList<>(split.length);
          for (String s : split) {
            res.add(Long.parseLong(s));
          }
          return res;
        }
      } catch (IOException e) {
        return Collections.emptyList();
      }
      throw new IllegalStateException("File does not exist");
    }

    private boolean fileExists(String filename) {
      File file = new File(tempDir, filename);
      return file.isFile() && file.exists();
    }
  }

  public static class HttpProcessClientAssert extends AbstractAssert<HttpProcessClientAssert, HttpProcessClient> {
    Longs longs = Longs.instance();

    protected HttpProcessClientAssert(HttpProcessClient actual) {
      super(actual, HttpProcessClientAssert.class);
    }

    public static HttpProcessClientAssert assertThat(HttpProcessClient actual) {
      return new HttpProcessClientAssert(actual);
    }

    public HttpProcessClientAssert wasStarted(int times) {
      isNotNull();

      List<Long> startingAt = actual.wasStartingAt();
      longs.assertEqual(info, startingAt.size(), times);

      return this;
    }

    public HttpProcessClientAssert wasStartedBefore(long date) {
      isNotNull();

      List<Long> startingAt = actual.wasStartingAt();
      longs.assertEqual(info, startingAt.size(), 1);
      longs.assertLessThanOrEqualTo(info, startingAt.iterator().next(), date);

      return this;
    }

    public HttpProcessClientAssert wasStartedBefore(HttpProcessClient client) {
      isNotNull();

      List<Long> startingAt = actual.wasStartingAt();
      longs.assertEqual(info, startingAt.size(), 1);
      longs.assertLessThanOrEqualTo(info, startingAt.iterator().next(), client.wasStartingAt().iterator().next());

      return this;
    }

    public HttpProcessClientAssert wasTerminated(int times) {
      isNotNull();

      List<Long> terminatedAt = actual.wasGracefullyTerminatedAt();
      longs.assertEqual(info, terminatedAt.size(), 2);

      return this;
    }

    public HttpProcessClientAssert wasGracefullyTerminated() {
      isNotNull();

      if (!actual.wasGracefullyTerminated()) {
        failWithMessage("HttpClient %s should have been gracefully terminated", actual.commandKey);
      }

      return this;
    }

    public HttpProcessClientAssert wasNotGracefullyTerminated() {
      isNotNull();

      if (actual.wasGracefullyTerminated()) {
        failWithMessage("HttpClient %s should not have been gracefully terminated", actual.commandKey);
      }

      return this;
    }

    public HttpProcessClientAssert wasGracefullyTerminatedBefore(HttpProcessClient p1) {
      isNotNull();

      List<Long> wasGracefullyTerminatedAt = actual.wasGracefullyTerminatedAt();
      longs.assertEqual(info, wasGracefullyTerminatedAt.size(), 1);
      longs.assertLessThanOrEqualTo(info, wasGracefullyTerminatedAt.iterator().next(), p1.wasGracefullyTerminatedAt().iterator().next());

      return this;
    }

    public HttpProcessClientAssert wasGracefullyTerminated(int times) {
      isNotNull();

      List<Long> wasGracefullyTerminatedAt = actual.wasGracefullyTerminatedAt();
      longs.assertEqual(info, wasGracefullyTerminatedAt.size(), times);

      return this;
    }

    public HttpProcessClientAssert isReady() {
      isNotNull();

      // check condition
      if (!actual.isReady()) {
        failWithMessage("HttpClient %s should be ready", actual.commandKey);
      }

      return this;
    }

    public HttpProcessClientAssert isNotReady() {
      isNotNull();

      if (actual.isReady()) {
        failWithMessage("HttpClient %s should not be ready", actual.commandKey);
      }

      return this;
    }

    public HttpProcessClientAssert hasBeenReady() {
      isNotNull();

      // check condition
      if (!actual.wasReady()) {
        failWithMessage("HttpClient %s should been ready at least once", actual.commandKey);
      }

      return this;
    }

    public HttpProcessClientAssert hasNotBeenReady() {
      isNotNull();

      // check condition
      if (actual.wasReady()) {
        failWithMessage("HttpClient %s should never been ready", actual.commandKey);
      }

      return this;
    }
  }

  private JavaCommand newStandardProcessCommand() throws IOException {
    return new JavaCommand("standard")
      .addClasspath(testJar.getAbsolutePath())
      .setClassName("org.sonar.process.test.StandardProcess")
      .setTempDir(temp.newFolder());
  }

}
