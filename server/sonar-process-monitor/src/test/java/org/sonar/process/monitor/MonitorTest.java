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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonar.process.Lifecycle.State;
import org.sonar.process.NetworkUtils;
import org.sonar.process.ProcessId;
import org.sonar.process.SystemExit;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.process.monitor.MonitorTest.HttpProcessClientAssert.assertThat;

public class MonitorTest {

  private static File testJar;

  private FileSystem fileSystem = mock(FileSystem.class);
  private SystemExit exit = mock(SystemExit.class);

  private Monitor underTest;

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

  private File tempDir;

  @Before
  public void setUp() throws Exception {
    tempDir = temp.newFolder();

  }

  /**
   * Safeguard
   */
  @After
  public void tearDown() {
    try {
      if (underTest != null) {
        underTest.stop();
      }
    } catch (Throwable ignored) {
    }
  }

  @Test
  public void fail_to_start_if_no_commands() throws Exception {
    underTest = newDefaultMonitor(tempDir);
    try {
      underTest.start(Collections.<JavaCommand>emptyList());
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("At least one command is required");
    }
  }

  @Test
  public void fail_to_start_multiple_times() throws Exception {
    underTest = newDefaultMonitor(tempDir);
    underTest.start(singletonList(newStandardProcessCommand()));
    boolean failed = false;
    try {
      underTest.start(singletonList(newStandardProcessCommand()));
    } catch (IllegalStateException e) {
      failed = e.getMessage().equals("Can not start multiple times");
    }
    underTest.stop();
    assertThat(failed).isTrue();
  }

  @Test
  public void start_then_stop_gracefully() throws Exception {
    underTest = newDefaultMonitor(tempDir);
    HttpProcessClient client = new HttpProcessClient(tempDir, ProcessId.ELASTICSEARCH);
    // blocks until started
    underTest.start(singletonList(client.newCommand()));

    assertThat(client).isUp()
      .wasStartedBefore(System.currentTimeMillis());

    // blocks until stopped
    underTest.stop();
    assertThat(client)
      .isNotUp()
      .wasGracefullyTerminated();
    assertThat(underTest.getState()).isEqualTo(State.STOPPED);
    verify(fileSystem).reset();
  }

  @Test
  public void start_then_stop_sequence_of_commands() throws Exception {
    underTest = newDefaultMonitor(tempDir);
    HttpProcessClient p1 = new HttpProcessClient(tempDir, ProcessId.ELASTICSEARCH);
    HttpProcessClient p2 = new HttpProcessClient(tempDir, ProcessId.WEB_SERVER);
    underTest.start(Arrays.asList(p1.newCommand(), p2.newCommand()));

    // start p2 when p1 is fully started (ready)
    assertThat(p1)
      .isUp()
      .wasStartedBefore(p2);
    assertThat(p2)
      .isUp();

    underTest.stop();

    // stop in inverse order
    assertThat(p1)
      .isNotUp()
      .wasGracefullyTerminated();
    assertThat(p2)
      .isNotUp()
      .wasGracefullyTerminatedBefore(p1);
    verify(fileSystem).reset();
  }

  @Test
  public void stop_all_processes_if_monitor_shutdowns() throws Exception {
    underTest = newDefaultMonitor(tempDir);
    HttpProcessClient p1 = new HttpProcessClient(tempDir, ProcessId.ELASTICSEARCH);
    HttpProcessClient p2 = new HttpProcessClient(tempDir, ProcessId.WEB_SERVER);
    underTest.start(Arrays.asList(p1.newCommand(), p2.newCommand()));
    assertThat(p1).isUp();
    assertThat(p2).isUp();

    // emulate CTRL-C
    underTest.getShutdownHook().run();
    underTest.getShutdownHook().join();

    assertThat(p1).wasGracefullyTerminated();
    assertThat(p2).wasGracefullyTerminated();

    verify(fileSystem).reset();
  }

  @Test
  public void restart_all_processes_if_one_asks_for_restart() throws Exception {
    underTest = newDefaultMonitor(tempDir);
    HttpProcessClient p1 = new HttpProcessClient(tempDir, ProcessId.ELASTICSEARCH);
    HttpProcessClient p2 = new HttpProcessClient(tempDir, ProcessId.WEB_SERVER);
    underTest.start(Arrays.asList(p1.newCommand(), p2.newCommand()));

    assertThat(p1).isUp();
    assertThat(p2).isUp();

    p2.restart();

    assertThat(underTest.waitForOneRestart()).isTrue();

    assertThat(p1)
      .wasStarted(2)
      .wasGracefullyTerminated(1);
    assertThat(p2)
      .wasStarted(2)
      .wasGracefullyTerminated(1);

    underTest.stop();

    assertThat(p1)
      .wasStarted(2)
      .wasGracefullyTerminated(2);
    assertThat(p2)
      .wasStarted(2)
      .wasGracefullyTerminated(2);

    verify(fileSystem, times(2)).reset();
  }

  @Test
  public void stop_all_processes_if_one_shutdowns() throws Exception {
    underTest = newDefaultMonitor(tempDir);
    HttpProcessClient p1 = new HttpProcessClient(tempDir, ProcessId.ELASTICSEARCH);
    HttpProcessClient p2 = new HttpProcessClient(tempDir, ProcessId.WEB_SERVER);
    underTest.start(Arrays.asList(p1.newCommand(), p2.newCommand()));
    assertThat(p1.isUp()).isTrue();
    assertThat(p2.isUp()).isTrue();

    // kill p1 -> waiting for detection by monitor than termination of p2
    p1.kill();
    underTest.awaitTermination();

    assertThat(p1)
      .isNotUp()
      .wasNotGracefullyTerminated();
    assertThat(p2)
      .isNotUp()
      .wasGracefullyTerminated();

    verify(fileSystem).reset();
  }

  @Test
  public void stop_all_processes_if_one_fails_to_start() throws Exception {
    underTest = newDefaultMonitor(tempDir);
    HttpProcessClient p1 = new HttpProcessClient(tempDir, ProcessId.ELASTICSEARCH);
    HttpProcessClient p2 = new HttpProcessClient(tempDir, ProcessId.WEB_SERVER, -1);
    try {
      underTest.start(Arrays.asList(p1.newCommand(), p2.newCommand()));
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
  public void fail_to_start_if_bad_class_name() throws Exception {
    underTest = newDefaultMonitor(tempDir);
    JavaCommand command = new JavaCommand(ProcessId.ELASTICSEARCH)
      .addClasspath(testJar.getAbsolutePath())
      .setClassName("org.sonar.process.test.Unknown");

    try {
      underTest.start(singletonList(command));
      fail();
    } catch (Exception e) {
      // expected
      // TODO improve, too many stacktraces logged
    }
  }

  @Test
  public void watchForHardStop_adds_a_hardStopWatcher_thread_and_starts_it() throws Exception {
    underTest = newDefaultMonitor(tempDir, true);
    assertThat(underTest.hardStopWatcher).isNull();

    HttpProcessClient p1 = new HttpProcessClient(tempDir, ProcessId.COMPUTE_ENGINE);
    underTest.start(singletonList(p1.newCommand()));

    assertThat(underTest.hardStopWatcher).isNotNull();
    assertThat(underTest.hardStopWatcher.isAlive()).isTrue();

    p1.kill();
    underTest.awaitTermination();

    assertThat(underTest.hardStopWatcher.isAlive()).isFalse();
  }

  private Monitor newDefaultMonitor(File tempDir) throws IOException {
    return newDefaultMonitor(tempDir, false);
  }

  private Monitor newDefaultMonitor(File tempDir, boolean watchForHardStop) throws IOException {
    when(fileSystem.getTempDir()).thenReturn(tempDir);
    return new Monitor(1, fileSystem, exit, watchForHardStop);
  }

  /**
   * Interaction with {@link org.sonar.process.test.HttpProcess}
   */
  private class HttpProcessClient {
    private final int httpPort;
    private final ProcessId processId;
    private final File tempDir;

    private HttpProcessClient(File tempDir, ProcessId processId) throws IOException {
      this(tempDir, processId, NetworkUtils.freePort());
    }

    /**
     * Use httpPort=-1 to make server fail to start
     */
    private HttpProcessClient(File tempDir, ProcessId processId, int httpPort) throws IOException {
      this.tempDir = tempDir;
      this.processId = processId;
      this.httpPort = httpPort;
    }

    JavaCommand newCommand() {
      return new JavaCommand(processId)
        .addClasspath(testJar.getAbsolutePath())
        .setClassName("org.sonar.process.test.HttpProcess")
        .setArgument("httpPort", String.valueOf(httpPort));
    }

    /**
     * @see org.sonar.process.test.HttpProcess
     */
    boolean isUp() {
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
        File file = new File(tempDir, httpPort + "-" + filename);
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
      File file = new File(tempDir, httpPort + "-" + filename);
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
        failWithMessage("HttpClient %s should have been gracefully terminated", actual.processId.getKey());
      }

      return this;
    }

    public HttpProcessClientAssert wasNotGracefullyTerminated() {
      isNotNull();

      if (actual.wasGracefullyTerminated()) {
        failWithMessage("HttpClient %s should not have been gracefully terminated", actual.processId.getKey());
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

    public HttpProcessClientAssert isUp() {
      isNotNull();

      // check condition
      if (!actual.isUp()) {
        failWithMessage("HttpClient %s should be up", actual.processId.getKey());
      }

      return this;
    }

    public HttpProcessClientAssert isNotUp() {
      isNotNull();

      if (actual.isUp()) {
        failWithMessage("HttpClient %s should not be up", actual.processId.getKey());
      }

      return this;
    }

    public HttpProcessClientAssert hasBeenReady() {
      isNotNull();

      // check condition
      if (!actual.wasReady()) {
        failWithMessage("HttpClient %s should been ready at least once", actual.processId.getKey());
      }

      return this;
    }

    public HttpProcessClientAssert hasNotBeenReady() {
      isNotNull();

      // check condition
      if (actual.wasReady()) {
        failWithMessage("HttpClient %s should never been ready", actual.processId.getKey());
      }

      return this;
    }
  }

  private JavaCommand newStandardProcessCommand() throws IOException {
    return new JavaCommand(ProcessId.ELASTICSEARCH)
      .addClasspath(testJar.getAbsolutePath())
      .setClassName("org.sonar.process.test.StandardProcess");
  }

}
