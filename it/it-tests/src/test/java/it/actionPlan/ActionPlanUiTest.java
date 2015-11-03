/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package it.actionPlan;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category1Suite;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.wsclient.issue.ActionPlanClient;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.runProjectAnalysis;

@Ignore
public class ActionPlanUiTest {

  private static final String PROJECT_KEY = "sample";

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @BeforeClass
  public static void analyzeProject() {
    orchestrator.resetData();

    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/actionPlan/one-issue-per-line-profile.xml"));
    orchestrator.getServer().provisionProject("sample", "Sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line-profile");
    runProjectAnalysis(orchestrator, "shared/xoo-sample");
  }

  protected static ActionPlanClient adminActionPlanClient() {
    return orchestrator.getServer().adminWsClient().actionPlanClient();
  }

  @Before
  public void resetData() {
    // TODO should be done by a WS
    orchestrator.getDatabase().truncate("action_plans");
    assertThat(adminActionPlanClient().find(PROJECT_KEY)).isEmpty();
  }

  @Test
  @Ignore("Must be refactored to use web services when applicable")
  public void test_console() {
    Selenese selenese = Selenese
      .builder()
      .setHtmlTestsInClasspath("action-plans-console",
        "/actionPlan/ActionPlanUiTest/create_and_delete_action_plan.html",
        "/actionPlan/ActionPlanUiTest/cannot_create_action_plan_with_date_in_past.html",
        "/actionPlan/ActionPlanUiTest/cannot_create_action_plan_with_invalid_date.html",
        "/actionPlan/ActionPlanUiTest/cannot_create_two_action_plans_with_same_name.html",
        // SONAR-3200
        "/actionPlan/ActionPlanUiTest/close_and_reopen_action_plan.html",
        "/actionPlan/ActionPlanUiTest/edit_action_plan.html",
        // SONAR-3198
        "/actionPlan/ActionPlanUiTest/can_create_action_plan_with_date_today.html").build();
    orchestrator.executeSelenese(selenese);
  }

}
