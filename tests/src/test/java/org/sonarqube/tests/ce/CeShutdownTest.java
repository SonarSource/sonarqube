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
package org.sonarqube.tests.ce;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.File;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonarqube.qa.util.LogsTailer;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.ce.ActivityStatusRequest;
import util.ItUtils;

import static com.google.common.base.Preconditions.checkState;
import static org.assertj.core.api.Assertions.assertThat;

public class CeShutdownTest {

  @Rule
  public TestRule safeguard = new DisableOnDebug(Timeout.seconds(600));
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void stopping_CE_waits_for_in_progress_task_to_be_finished() throws Exception {
    try (ComputeEngine ce = new ComputeEngine(40_000)) {

      try (LogsTailer.Watch watch = ce.logs().watch("CE analysis is paused")) {
        ce.triggerTask();
        watch.waitForLog();
        assertThat(ce.countInProgressTasks()).isEqualTo(1);
      }

      // stop does not kill in-progress workers. It waits
      // for them (at least a few dozens of seconds)
      try (LogsTailer.Watch watch = ce.logs().watch("Waiting for workers to finish in-progress tasks")) {
        ce.triggerStop();
        watch.waitForLog();
        assertThat(ce.countInProgressTasks()).isEqualTo(1);
      }

      // resume the in-progress task, so that it can
      // finish successfully
      try (LogsTailer.Watch watch = ce.logs().watch("Process [ce] is stopped")) {
        ce.resumeTask();
        watch.waitForLog();
        assertThat(ce.hasTaskFinishedSuccessfully()).isTrue();
        assertThat(ce.hasTaskFinishedInFailure()).isFalse();
        assertThat(ce.hasErrorLogs()).isFalse();
      }
    }
  }

  @Test
  public void stopping_CE_kills_in_progress_tasks_if_too_long_to_gracefully_stop() throws Exception {
    try (ComputeEngine ce = new ComputeEngine(10)) {

      try (LogsTailer.Watch watch = ce.logs().watch("CE analysis is paused")) {
        ce.triggerTask();
        watch.waitForLog();
        assertThat(ce.countInProgressTasks()).isEqualTo(1);
      }

      // stop does not kill in-progress workers. It waits
      // for them (at least a few dozens of seconds)
      try (LogsTailer.Watch watch = ce.logs().watch("Waiting for workers to finish in-progress tasks")) {
        ce.triggerStop();
        watch.waitForLog();
        assertThat(ce.countInProgressTasks()).isEqualTo(1);
      }

      // wait for task to be hard killed
      try (LogsTailer.Watch watch = ce.logs().watch("Process [ce] is stopped")) {
        watch.waitForLog();
        assertThat(ce.hasTaskFinishedSuccessfully()).isFalse();
        assertThat(ce.hasTaskFinishedInFailure()).isTrue();
        assertThat(ce.hasErrorLogs()).isTrue();
      }
    }
  }

  private class ComputeEngine implements AutoCloseable {
    private final Orchestrator orchestrator;
    private final File pauseFile;
    private final WsClient adminWsClient;
    private Thread stopper;
    private final LogsTailer logsTailer;
    private final LogsTailer.Content content = new LogsTailer.Content();

    ComputeEngine(int timeOutInMs) throws Exception {
      pauseFile = temp.newFile();
      FileUtils.touch(pauseFile);

      orchestrator = Orchestrator.builderEnv()
        .setServerProperty("sonar.ce.pauseTask.path", pauseFile.getAbsolutePath())
        .setServerProperty("sonar.ce.gracefulStopTimeOutInMs", "" + timeOutInMs)
        .addPlugin(ItUtils.xooPlugin())
        .addPlugin(ItUtils.pluginArtifact("server-plugin"))
        .build();
      orchestrator.start();
      adminWsClient = ItUtils.newAdminWsClient(orchestrator);
      logsTailer = LogsTailer.builder()
        .addFile(orchestrator.getServer().getCeLogs())
        .addFile(orchestrator.getServer().getAppLogs())
        .addConsumer(content)
        .build();
    }

    LogsTailer logs() {
      return logsTailer;
    }

    void triggerTask() {
      orchestrator.executeBuild(SonarScanner.create(new File("projects/shared/xoo-sample"), "sonar.projectKey", "foo"), false);
    }

    void resumeTask() throws Exception {
      FileUtils.forceDelete(pauseFile);
    }

    int countInProgressTasks() {
      return adminWsClient.ce().activityStatus(new ActivityStatusRequest()).getInProgress();
    }

    boolean hasTaskFinishedSuccessfully() {
      return content.hasLineMatching(Pattern.compile(".* INFO .*Executed task \\| project=foo \\| type=REPORT.*\\| status=SUCCESS.*"));
    }

    boolean hasTaskFinishedInFailure() {
      return content.hasLineMatching(Pattern.compile(".* INFO .*Executed task \\| project=foo \\| type=REPORT.*\\| status=FAILED.*"));
    }

    boolean hasErrorLogs() {
      return content.hasText(" ERROR ");
    }

    /**
     * non-blocking stop
     */
    void triggerStop() {
      checkState(stopper == null);
      stopper = new Thread(orchestrator::stop);
      stopper.start();
    }

    @Override
    public void close() {
      if (stopper != null) {
        stopper.interrupt();
      }
      if (orchestrator != null) {
        orchestrator.stop();
      }
      if (logsTailer != null) {
        logsTailer.close();
      }
    }
  }
}
