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
package org.sonarqube.tests.measure;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.util.List;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.ProjectActivityPage;
import org.sonarqube.qa.util.pageobjects.ProjectAnalysisItem;

import static util.ItUtils.projectDir;

public class ProjectActivityPageTest {

  @ClassRule
  public static Orchestrator orchestrator = MeasureSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void should_list_snapshots() {
    analyzeXooSample("shared/xoo-history-v1", "2014-10-19");
    analyzeXooSample("shared/xoo-history-v2", "2014-11-13");

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
    analyzeXooSample();
    openPage().getLastAnalysis()
      .addCustomEvent("foo")
      .changeFirstEvent("bar")
      .deleteFirstEvent();
  }

  @Test
  public void delete_analysis() {
    analyzeXooSample();
    analyzeXooSample();
    ProjectActivityPage page = openPage();
    page.getAnalyses().shouldHaveSize(2);
    page.getFirstAnalysis().delete();
  }

  private ProjectActivityPage openPage() {
    String userAdmin = tester.users().generateAdministratorOnDefaultOrganization().getLogin();
    return tester.openBrowser()
      .logIn()
      .submitCredentials(userAdmin, userAdmin)
      .openProjectActivity("sample");
  }

  private void analyzeXooSample() {
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));
  }

  private  void analyzeXooSample(String path, String date) {
    orchestrator.executeBuild(SonarScanner.create(projectDir(path)).setProperties("sonar.projectDate", date));
  }
}
