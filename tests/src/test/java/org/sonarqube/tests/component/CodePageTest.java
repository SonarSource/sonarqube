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
import com.sonar.orchestrator.build.SonarScanner;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;

import static util.ItUtils.projectDir;

public class CodePageTest {

  @ClassRule
  public static Orchestrator orchestrator = ComponentSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void code_page() {
    Project project = tester.projects().provision();
    executeAnalysis(project);

    tester.openBrowser().openCode(project.getKey())
      .shouldHaveComponent("src/main/xoo/sample")
      .openFirstComponent()
      .shouldHaveComponent("Sample.xoo")
      .openFirstComponent()
      .shouldHaveCode("public class Sample")
      .shouldHaveBreadcrumbs(project.getName(), "src/main/xoo/sample", "Sample.xoo");

    // search
    tester.openBrowser().openCode(project.getKey())
      .shouldHaveComponent(project.getName())
      .search("xoo")
      .shouldSearchResult("Sample.xoo");

    // permalink
    tester.openBrowser().openCode(project.getKey(), project.getKey() + ":src/main/xoo/sample/Sample.xoo")
      .shouldHaveCode("public class Sample")
      .shouldHaveBreadcrumbs(project.getName(), "src/main/xoo/sample", "Sample.xoo");
  }

  @Test
  public void expand_root_dir() {
    Project project = tester.projects().provision();
    executeAnalysis(project, "shared/xoo-sample-with-root-dir");

    tester.openBrowser().openCode(project.getKey())
      .shouldHaveComponent("Hello.xoo")
      .shouldHaveComponent("src/main/xoo/sample");
  }

  private void executeAnalysis(Project project, String path) {
    orchestrator.executeBuild(
      SonarScanner.create(projectDir(path))
        .setProjectKey(project.getKey())
        .setProjectName(project.getName()));
  }

  private void executeAnalysis(Project project) {
    executeAnalysis(project, "shared/xoo-sample");
  }
}
