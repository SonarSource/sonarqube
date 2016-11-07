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
package it.projectSearch;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import it.Category1Suite;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import pageobjects.Navigation;
import pageobjects.projects.ProjectsPage;

import static util.ItUtils.projectDir;

public class ProjectsPageTest {

  @ClassRule
  public static Orchestrator ORCHESTRATOR = Category1Suite.ORCHESTRATOR;

  @Rule
  public Navigation nav = Navigation.get(ORCHESTRATOR);

  @BeforeClass
  public static void setUp() {
    ORCHESTRATOR.resetData();
    ORCHESTRATOR.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")).setProjectKey("key-foo"));
    ORCHESTRATOR.executeBuild(SonarScanner.create(projectDir("duplications/file-duplications")).setProjectKey("key-bar"));
  }

  @Test
  public void should_display_projects() {
    ProjectsPage page = nav.openProjects();
    page.shouldHaveTotal(2);
    page.getProjectByKey("key-foo")
      .shouldHaveMeasure("reliability_rating", "A")
      .shouldHaveMeasure("security_rating", "A")
      .shouldHaveMeasure("sqale_rating", "A")
      .shouldHaveMeasure("duplicated_lines_density", "0.0%")
      .shouldHaveMeasure("ncloc", "13")
      .shouldHaveMeasure("ncloc", "Xoo");
  }

  @Test
  public void should_display_facets() {
    ProjectsPage page = nav.openProjects();
    page.getFacetByProperty("duplications")
      .shouldHaveValue("1", "1")
      .shouldHaveValue("2", "1")
      .shouldHaveValue("3", "1")
      .shouldHaveValue("4", "1")
      .shouldHaveValue("5", "1");
  }

  @Test
  public void should_filter_using_facet() {
    ProjectsPage page = nav.openProjects();
    page.shouldHaveTotal(2);
    page.getFacetByProperty("duplications").selectValue("3");
    page.shouldHaveTotal(1);
  }

}
