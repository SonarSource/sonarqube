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
package it.projectEvent;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import it.Category1Suite;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import pageobjects.Navigation;
import pageobjects.ProjectActivityPage;

import static util.ItUtils.projectDir;

public class ProjectActivityPageTest {

  @ClassRule
  public static Orchestrator ORCHESTRATOR = Category1Suite.ORCHESTRATOR;

  @Rule
  public Navigation nav = Navigation.get(ORCHESTRATOR);

  @Before
  public void setUp() throws Exception {
    ORCHESTRATOR.resetData();
  }

  @Test
  public void add_change_delete_custom_event() {
    executeAnalysis();
    openPage().getLastAnalysis()
      .addCustomEvent("foo")
      .changeLastEvent("bar")
      .deleteLastEvent();
  }

  @Test
  public void delete_analysis() {
    executeAnalysis();
    executeAnalysis();
    openPage().getFirstAnalysis().delete();
  }

  private ProjectActivityPage openPage() {
    nav.logIn().submitCredentials("admin", "admin");
    return nav.openProjectActivity("sample");
  }

  private static void executeAnalysis(String... properties) {
    SonarScanner sampleProject = SonarScanner.create(projectDir("shared/xoo-sample")).setProperties(properties);
    ORCHESTRATOR.executeBuild(sampleProject);
  }
}
