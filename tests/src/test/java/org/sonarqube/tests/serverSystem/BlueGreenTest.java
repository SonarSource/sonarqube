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
package org.sonarqube.tests.serverSystem;

import com.google.common.util.concurrent.Uninterruptibles;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.File;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Ce;
import org.sonarqube.ws.Projects;
import org.sonarqube.ws.client.ce.ActivityStatusRequest;
import org.sonarqube.ws.client.plugins.UninstallRequest;
import org.sonarqube.ws.client.qualityprofiles.AddProjectRequest;
import util.ItUtils;
import util.XooProjectBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newOrchestratorBuilder;
import static util.ItUtils.pluginArtifact;
import static util.ItUtils.xooPlugin;

public class BlueGreenTest {

  @Rule
  public TestRule safeguard = new DisableOnDebug(Timeout.seconds(600));

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private Orchestrator orchestrator;
  private Tester tester;

  @After
  public void tearDown() {
    if (tester != null) {
      tester.after();
    }
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  @Test
  public void upgrade_analyzer_when_analysis_is_pending_in_compute_engine_queue() throws Exception {
    orchestrator = newOrchestratorBuilder()
      .addPlugin(pluginArtifact("blue-green-plugin-v1"))
      .addPlugin(xooPlugin())
      .build();
    tester = new Tester(orchestrator).disableOrganizations();
    orchestrator.start();
    tester.before();

    Project project = new Project();
    project.associateToXooProfile("Blue Profile");
    project.analyzeAndWait();
    // assert 2 issues + security rating E (rule A is blocker, rule B is critical)
    assertThat(ItUtils.getMeasureAsDouble(orchestrator, project.getKey(), "violations")).isEqualTo(2.0);
    assertThat(ItUtils.getMeasureAsDouble(orchestrator, project.getKey(), "security_rating")).isEqualTo(5.0);
    assertThat(ItUtils.getMeasureAsDouble(orchestrator, project.getKey(), "blue")).isEqualTo(10.0);
    assertThat(ItUtils.getMeasureAsDouble(orchestrator, project.getKey(), "bluegreen")).isEqualTo(20.0);

    // remove rule "A" and metric "blue" between analysis and execution of Compute Engine

    // 1. pause compute engine so that the second analysis is kept pending
    tester.wsClient().ce().pause();
    project.analyze();
    assertThat(loadCeActivity().getPending()).isEqualTo(1);

    // 2. upgrade plugin and verify that analysis is still pending in CE queue
    File pluginV2 = pluginArtifact("blue-green-plugin-v2").getFile();
    FileUtils.copyFileToDirectory(pluginV2, new File(orchestrator.getServer().getHome(), "extensions/downloads"));
    orchestrator.restartServer();
    Ce.ActivityStatusWsResponse ceActivity = loadCeActivity();
    assertThat(ceActivity.getInProgress()).isEqualTo(0);
    assertThat(ceActivity.getPending()).isEqualTo(1);

    // 3. resume the queue and verify that the issue on rule A is ignored. Only
    // the critical issue on rule B is remaining
    resumeAndWaitForCeQueueEmpty();
    assertThat(ItUtils.getMeasureAsDouble(orchestrator, project.getKey(), "violations")).isEqualTo(1.0);
    assertThat(ItUtils.getMeasureAsDouble(orchestrator, project.getKey(), "security_rating")).isEqualTo(4.0);
    assertThat(ItUtils.getMeasure(orchestrator, project.getKey(), "blue")).isNull();
    assertThat(ItUtils.getMeasureAsDouble(orchestrator, project.getKey(), "bluegreen")).isEqualTo(20.0);

    // test removal of analyzer. Analysis should not fail when queue is resumed.
    tester.wsClient().ce().pause();
    project.analyze();
    tester.wsClient().plugins().uninstall(new UninstallRequest().setKey("xoo"));
    tester.wsClient().plugins().uninstall(new UninstallRequest().setKey("bluegreen"));
    orchestrator.restartServer();

    resumeAndWaitForCeQueueEmpty();
    assertThat(ItUtils.getMeasureAsDouble(orchestrator, project.getKey(), "violations")).isEqualTo(0.0);
    assertThat(ItUtils.getMeasureAsDouble(orchestrator, project.getKey(), "security_rating")).isEqualTo(1.0);
    assertThat(ItUtils.getMeasure(orchestrator, project.getKey(), "blue")).isNull();
    assertThat(ItUtils.getMeasure(orchestrator, project.getKey(), "bluegreen")).isNull();
  }

  private void resumeAndWaitForCeQueueEmpty() {
    tester.wsClient().ce().resume();
    while (true) {
      Ce.ActivityStatusWsResponse activity = loadCeActivity();
      if (activity.getPending() + activity.getInProgress() == 0) {
        return;
      }
      Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
    }
  }

  private Ce.ActivityStatusWsResponse loadCeActivity() {
    return tester.wsClient().ce().activityStatus(new ActivityStatusRequest());
  }

  private class Project {
    private final Projects.CreateWsResponse.Project project;
    private final File dir;

    Project() throws Exception {
      this.project = tester.projects().provision();
      this.dir = new XooProjectBuilder(project.getKey())
        .setFilesPerModule(1)
        .build(temp.newFolder());
    }

    String getKey() {
      return project.getKey();
    }

    void associateToXooProfile(String name) {
      tester.wsClient().qualityprofiles().addProject(new AddProjectRequest()
        .setProject(project.getKey())
        .setLanguage("xoo")
        .setQualityProfile(name));
    }

    void analyzeAndWait() {
      orchestrator.executeBuild(SonarScanner.create(dir), true);
    }

    void analyze() {
      orchestrator.executeBuild(SonarScanner.create(dir), false);
    }
  }
}
