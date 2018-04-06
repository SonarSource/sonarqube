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
package org.sonarqube.tests.component;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildFailureException;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.File;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.client.components.ShowRequest;
import org.sonarqube.ws.client.components.TreeRequest;
import org.sonarqube.ws.client.projects.UpdateKeyRequest;
import util.XooProjectBuilder;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ModuleTest {

  private static final String PROJECT_KEY = "sample";

  @ClassRule
  public static Orchestrator orchestrator = ComponentSuite.ORCHESTRATOR;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  /**
   * SONAR-10536
   */
  @Test
  public void analyze_disabled_module_as_a_new_project() throws Exception {
    String projectKey = PROJECT_KEY;
    String moduleKey = projectKey + ":module_a";

    // analyze project "sample" with module "sample:module_a"
    File projectWithModule = new XooProjectBuilder(projectKey)
      .addModules("module_a")
      .build(temp.newFolder());
    analyze(projectWithModule);
    assertThat(tester.projects().exists(moduleKey)).isTrue();
    assertThat(countFilesInProject(projectKey)).isEqualTo(2 /* 1 file in project and 1 file in module */);

    // analyze project "sample" without module "sample:module_a". The latter
    // is considered as disabled
    File projectWithoutModule = new XooProjectBuilder(projectKey)
      .build(temp.newFolder());
    analyze(projectWithoutModule);
    assertThat(tester.projects().exists(moduleKey)).isFalse();
    assertThat(countFilesInProject(projectKey)).isEqualTo(1 /* 1 file in project */);

    // analyze module_a as a project
    File moduleAsProject = new XooProjectBuilder(moduleKey)
      .build(temp.newFolder());
    try {
      analyze(moduleAsProject);
      fail();
    } catch (BuildFailureException e) {
      assertThat(e.getResult().getLogs()).contains("The project '" + moduleKey + "' is already defined in SonarQube but as a module of project '" + projectKey + "'");
      assertThat(tester.projects().exists(moduleKey)).isFalse();
    }

    // the only workaround is to rename the key of the disabled module
    updateModuleKey(moduleKey, moduleKey + "_old");

    // module_a can now be analyzed as a project
    analyze(moduleAsProject);
    assertThat(tester.projects().exists(moduleKey)).isTrue();
    assertThat(countFilesInProject(moduleKey)).isEqualTo(1);
    assertThat(tester.wsClient().components().show(new ShowRequest().setComponent(moduleKey)).getComponent().getQualifier()).isEqualTo("TRK");
  }

  private void analyze(File projectDir) {
    orchestrator.executeBuild(SonarScanner.create(projectDir));
  }

  private int countFilesInProject(String projectKey) {
    TreeRequest request = new TreeRequest().setComponent(projectKey).setQualifiers(asList("FIL"));
    return tester.wsClient().components().tree(request).getComponentsCount();
  }

  private void updateModuleKey(String fromKey, String toKey) {
    tester.wsClient().projects().updateKey(new UpdateKeyRequest()
      .setFrom(fromKey)
      .setTo(toKey));
  }
}
