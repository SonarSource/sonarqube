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
package org.sonarqube.tests.analysis;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import org.sonarqube.tests.Category3Suite;
import java.util.Map;
import org.junit.ClassRule;
import org.junit.Test;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.getComponent;
import static util.ItUtils.getMeasuresAsDoubleByMetricKey;

/**
 * Test the extension point org.sonar.api.batch.bootstrap.ProjectBuilder
 * <p/>
 * A Sonar plugin can override the project definition injected by build-tool.
 * Example: C# plugin loads project structure and modules from Visual Studio metadata file.
 *
 * @since 2.9
 */
public class ProjectBuilderTest {

  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;

  @Test
  public void shouldDefineProjectFromPlugin() {
    MavenBuild build = MavenBuild.create(ItUtils.projectPom("analysis/project-builder"))
      .setCleanSonarGoals()
      .setProperty("sonar.enableProjectBuilder", "true");
    orchestrator.executeBuild(build);

    checkProject();
    checkSubProject("project-builder-module-a");
    checkSubProject("project-builder-module-b");
    checkFile("project-builder-module-a", "src/HelloA.java");
    checkFile("project-builder-module-b", "src/HelloB.java");
    assertThat(getComponent(orchestrator, "com.sonarsource.it.projects.batch:project-builder-module-b:src/IgnoredFile.java")).isNull();
  }

  private void checkProject() {
    // name has been changed by plugin
    assertThat(getComponent(orchestrator, "com.sonarsource.it.projects.batch:project-builder").getName()).isEqualTo("Name changed by plugin");

    Map<String, Double> measures = getMeasures("com.sonarsource.it.projects.batch:project-builder");
    assertThat(measures.get("files")).isEqualTo(3);
    assertThat(measures.get("lines")).isGreaterThan(10);
  }

  private void checkSubProject(String subProjectKey) {
    Map<String, Double> measures = getMeasures("com.sonarsource.it.projects.batch:" + subProjectKey);
    assertThat(measures.get("files")).isEqualTo(1);
    assertThat(measures.get("lines")).isGreaterThan(5);
  }

  private void checkFile(String subProjectKey, String fileKey) {
    Map<String, Double> measures = getMeasures("com.sonarsource.it.projects.batch:" + subProjectKey + ":" + fileKey);
    assertThat(measures.get("lines")).isGreaterThan(5);
  }

  private Map<String, Double> getMeasures(String key) {
    return getMeasuresAsDoubleByMetricKey(orchestrator, key, "lines", "files");
  }
}
