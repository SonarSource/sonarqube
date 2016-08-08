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
package it.projectAdministration;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import it.Category1Suite;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import pageobjects.Navigation;
import pageobjects.ProjectHistoryPage;
import pageobjects.ProjectHistorySnapshotItem;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.confirm;
import static util.ItUtils.projectDir;

public class ProjectHistoryPageTest {

  @ClassRule
  public static Orchestrator ORCHESTRATOR = Category1Suite.ORCHESTRATOR;

  @Rule
  public Navigation nav = Navigation.get(ORCHESTRATOR);

  @Before
  public void setUp() {
    ORCHESTRATOR.resetData();
    analyzeProject("shared/xoo-history-v1", "2014-10-19");
    analyzeProject("shared/xoo-history-v2", "2014-11-13");
  }

  @Test
  public void should_list_snapshots() {
    ProjectHistoryPage page = openPage();

    page.getSnapshots().shouldHaveSize(2);

    List<ProjectHistorySnapshotItem> snapshots = page.getSnapshotsAsItems();

    snapshots.get(0).getVersionText().shouldBe(text("1.0-SNAPSHOT"));
    snapshots.get(0).getDeleteButton().shouldNot(exist);

    snapshots.get(1).getVersionText().shouldBe(text("0.9-SNAPSHOT"));
    snapshots.get(1).getDeleteButton().should(exist);
  }

  @Test
  public void should_delete_snapshot() {
    ProjectHistoryPage page = openPage();

    page.getSnapshots().shouldHaveSize(2);

    page.getSnapshotsAsItems().get(1).clickDelete();
    confirm();

    page.checkAlertDisplayed();
    page.getSnapshots().shouldHaveSize(1);
  }

  private ProjectHistoryPage openPage() {
    nav.logIn().submitCredentials("admin", "admin");
    return nav.openProjectHistory("sample");
  }

  private static void analyzeProject(String path, String date) {
    ORCHESTRATOR.executeBuild(SonarScanner.create(projectDir(path))
      .setProperties("sonar.projectDate", date));
  }
}
