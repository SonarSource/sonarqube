/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.tests.projectEvent;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category4Suite;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.pageobjects.Navigation;
import org.sonarqube.pageobjects.ProjectActivityPage;
import org.sonarqube.pageobjects.ProjectAnalysisItem;
import util.user.UserRule;

import static util.ItUtils.projectDir;

public class ProjectActivityPageTest {

  @ClassRule
  public static Orchestrator ORCHESTRATOR = Category4Suite.ORCHESTRATOR;

  @Rule
  public UserRule userRule = UserRule.from(ORCHESTRATOR);

  private Navigation nav = Navigation.create(ORCHESTRATOR);

  @Before
  public void setUp() throws Exception {
    ORCHESTRATOR.resetData();
  }

  @Test
  public void should_list_snapshots() {
    analyzeProject("shared/xoo-history-v1", "2014-10-19");
    analyzeProject("shared/xoo-history-v2", "2014-11-13");

    ProjectActivityPage page = openPage();
    page.getAnalyses().shouldHaveSize(2);

    List<ProjectAnalysisItem> analyses = page.getAnalysesAsItems();
    analyses.get(0)
      .shouldHaveEventWithText("1.0-SNAPSHOT")
      .shouldNotHaveDeleteButton();

    analyses.get(1)
      .shouldHaveEventWithText("0.9-SNAPSHOT")
      .shouldHaveDeleteButton();
  }

  @Test
  public void add_change_delete_custom_event() {
    analyzeProject();
    openPage().getLastAnalysis()
      .addCustomEvent("foo")
      .changeFirstEvent("bar")
      .deleteFirstEvent();
  }

  @Test
  public void delete_analysis() {
    analyzeProject();
    analyzeProject();
    ProjectActivityPage page = openPage();
    page.getAnalyses().shouldHaveSize(2);
    page.getFirstAnalysis().delete();
  }

  private ProjectActivityPage openPage() {
    String userAdmin = userRule.createAdminUser();
    nav.logIn().submitCredentials(userAdmin, userAdmin);
    return nav.openProjectActivity("sample");
  }

  private static void analyzeProject() {
    ORCHESTRATOR.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));
  }

  private static void analyzeProject(String path, String date) {
    ORCHESTRATOR.executeBuild(SonarScanner.create(projectDir(path)).setProperties("sonar.projectDate", date));
  }
}
