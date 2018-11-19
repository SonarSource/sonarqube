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

import com.google.common.collect.ImmutableList;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.ce.ActivityWsRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.pluginArtifact;
import static util.ItUtils.xooPlugin;

public class CeWorkersTest {

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static Orchestrator orchestrator;
  private static WsClient adminWsClient;

  @BeforeClass
  public static void setUp() throws Exception {
    OrchestratorBuilder builder = Orchestrator.builderEnv()
      .addPlugin(pluginArtifact("fake-governance-plugin"))
      // overwrite default value to display heap dump on OOM and reduce max heap
      .setServerProperty("sonar.ce.javaOpts", "-Xmx256m -Xms128m")
      .addPlugin(xooPlugin());
    orchestrator = builder.build();
    orchestrator.start();

    adminWsClient = newAdminWsClient(orchestrator);
  }

  @AfterClass
  public static void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
      orchestrator = null;
    }
  }

  @Test
  public void ce_worker_is_resilient_to_OOM_and_ISE_during_processing_of_a_task() throws InterruptedException {
    submitFakeTask("OOM");

    waitForEmptyQueue();

    assertThat(adminWsClient.ce().activity(new ActivityWsRequest()
      .setType("OOM")
      .setStatus(ImmutableList.of("FAILED")))
      .getTasksCount())
        .isEqualTo(1);

    submitFakeTask("OK");

    waitForEmptyQueue();

    assertThat(adminWsClient.ce().activity(new ActivityWsRequest()
      .setType("OK")
      .setStatus(ImmutableList.of("SUCCESS")))
      .getTasksCount())
        .isEqualTo(1);

    submitFakeTask("ISE");

    waitForEmptyQueue();

    assertThat(adminWsClient.ce().activity(new ActivityWsRequest()
      .setType("ISE")
      .setStatus(ImmutableList.of("FAILED")))
      .getTasksCount())
        .isEqualTo(1);

    submitFakeTask("OK");

    waitForEmptyQueue();

    assertThat(adminWsClient.ce().activity(new ActivityWsRequest()
      .setType("OK")
      .setStatus(ImmutableList.of("SUCCESS")))
      .getTasksCount())
        .isEqualTo(2);
  }

  private void submitFakeTask(String type) {
    adminWsClient.wsConnector().call(new PostRequest("api/fake_gov/submit")
      .setParam("type", type))
      .failIfNotSuccessful();
  }

  @Test
  public void ce_worker_is_resilient_to_OOM_and_RuntimeException_when_starting_or_stopping_analysis_report_container() throws IOException {
    int initSuccessReportTaskCount = adminWsClient.ce().activity(new ActivityWsRequest()
      .setType("REPORT")
      .setStatus(ImmutableList.of("SUCCESS")))
      .getTasksCount();
    int initFailedReportTaskCount = adminWsClient.ce().activity(new ActivityWsRequest()
      .setType("REPORT")
      .setStatus(ImmutableList.of("FAILED")))
      .getTasksCount();

    SonarScanner sonarRunner = SonarScanner.create(ItUtils.projectDir("shared/xoo-sample"));
    orchestrator.executeBuild(sonarRunner, true);

    enableComponentBomb("OOM_STOP");

    orchestrator.executeBuild(sonarRunner, true);

    enableComponentBomb("NONE");

    orchestrator.executeBuild(sonarRunner, true);

    enableComponentBomb("ISE_START");

    orchestrator.executeBuild(sonarRunner, true);

    enableComponentBomb("NONE");

    orchestrator.executeBuild(sonarRunner, true);

    enableComponentBomb("ISE_STOP");

    orchestrator.executeBuild(sonarRunner, true);

    enableComponentBomb("NONE");

    orchestrator.executeBuild(sonarRunner, true);

    enableComponentBomb("OOM_START");

    orchestrator.executeBuild(sonarRunner, true);

    enableComponentBomb("NONE");

    orchestrator.executeBuild(sonarRunner, true);

    // failure while starting components does fail the tasks
    assertThat(adminWsClient.ce().activity(new ActivityWsRequest()
      .setType("REPORT")
      .setStatus(ImmutableList.of("FAILED")))
      .getTasksCount())
        .isEqualTo(initFailedReportTaskCount + 2);

    // failure while stopping components does not fail the tasks
    assertThat(adminWsClient.ce().activity(new ActivityWsRequest()
      .setType("REPORT")
      .setStatus(ImmutableList.of("SUCCESS")))
      .getTasksCount())
        .isEqualTo(initSuccessReportTaskCount + 7);

  }

  private void enableComponentBomb(String type) {
    adminWsClient.wsConnector().call(new PostRequest("api/fake_gov/activate_bomb")
      .setParam("type", type))
      .failIfNotSuccessful();
  }

  private void waitForEmptyQueue() throws InterruptedException {
    int delay = 200;
    int timeout = 5 * 10; // 10 seconds
    int i = 0;
    int tasksCount;
    do {
      Thread.sleep(delay);
      tasksCount = adminWsClient.ce().activity(new ActivityWsRequest()
        .setStatus(ImmutableList.of("PENDING", "IN_PROGRESS")))
        .getTasksCount();
      i++;
    } while (i <= timeout && tasksCount > 0);
    assertThat(tasksCount).describedAs("Failed to get to an empty CE queue in a timely fashion").isZero();
  }
}
