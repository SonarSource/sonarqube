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
import java.io.IOException;
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
import org.sonarqube.qa.util.pageobjects.Navigation;
import org.sonarqube.ws.Ce;
import org.sonarqube.ws.Projects;
import org.sonarqube.ws.client.ce.ActivityStatusRequest;
import org.sonarqube.ws.client.qualityprofiles.AddProjectRequest;
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
  public void test_change_of_version_at_runtime() throws Exception {
    orchestrator = newOrchestratorBuilder()
      .addPlugin(pluginArtifact("blue-green-plugin-v1"))
      .addPlugin(xooPlugin())
      .build();
    tester = new Tester(orchestrator).disableOrganizations();
    orchestrator.start();
    tester.before();

    // pause compute engine so that analysis is kept pending
    tester.wsClient().ce().pause();
    Projects.CreateWsResponse.Project project = tester.projects().provision();
    associateProjectToProfile(project, "Blue Profile");
    analyze(project);
    assertThat(loadCeActivity().getPending()).isEqualTo(1);

    // open browser
    Navigation browser = tester.openBrowser();


    // upgrade plugin
    File pluginV2 = pluginArtifact("blue-green-plugin-v2").getFile();
    FileUtils.copyFileToDirectory(pluginV2, new File(orchestrator.getServer().getHome(), "extensions/downloads"));
    orchestrator.restartServer();

    // analysis task is still pending
    Ce.ActivityStatusWsResponse ceActivity = loadCeActivity();
    assertThat(ceActivity.getInProgress()).isEqualTo(0);
    assertThat(ceActivity.getPending()).isEqualTo(1);

    resumeAndWaitForCeQueueEmpty();

    // TODO check issues and measures
  }

  private void analyze(Projects.CreateWsResponse.Project project) throws IOException {
    File projectDir = new XooProjectBuilder(project.getKey())
      .setFilesPerModule(1)
      .build(temp.newFolder());
    orchestrator.executeBuild(SonarScanner.create(projectDir), false);
  }

  private void associateProjectToProfile(Projects.CreateWsResponse.Project project, String xooProfileName) {
    tester.wsClient().qualityprofiles().addProject(new AddProjectRequest()
      .setProject(project.getKey())
      .setLanguage("xoo")
      .setQualityProfile(xooProfileName));
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
}
