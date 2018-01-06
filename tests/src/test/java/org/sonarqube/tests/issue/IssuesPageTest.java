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
package org.sonarqube.tests.issue;

import com.sonar.orchestrator.Orchestrator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.pageobjects.Navigation;
import org.sonarqube.qa.util.pageobjects.issues.Issue;
import org.sonarqube.qa.util.pageobjects.issues.IssuesPage;
import util.ItUtils;
import util.user.UserRule;

import static util.ItUtils.runProjectAnalysis;

public class IssuesPageTest {
  private static final String PROJECT_KEY = "sample";

  @ClassRule
  public static Orchestrator ORCHESTRATOR = IssueSuite.ORCHESTRATOR;

  @Rule
  public UserRule userRule = UserRule.from(ORCHESTRATOR);

  public Navigation nav = Navigation.create(ORCHESTRATOR);

  private String adminUser;

  @BeforeClass
  public static void prepareData() {
    ORCHESTRATOR.resetData();

    ItUtils.restoreProfile(ORCHESTRATOR, IssuesPageTest.class.getResource("/issue/with-many-rules.xml"));

    ORCHESTRATOR.getServer().provisionProject(PROJECT_KEY, PROJECT_KEY);
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(PROJECT_KEY, "xoo", "with-many-rules");
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-multi-modules-sample");
  }

  @Before
  public void before() {
    adminUser = userRule.createAdminUser();
  }

  @Test
  public void should_display_actions() {
    IssuesPage page = nav.logIn().submitCredentials(adminUser).openIssues();
    Issue issue = page.getFirstIssue();
    issue.shouldAllowAssign().shouldAllowChangeType();
  }

  @Test
  public void should_not_display_actions() {
    Navigation nav = Navigation.create(ORCHESTRATOR);
    IssuesPage page = nav.openIssues();
    Issue issue = page.getFirstIssue();
    issue.shouldNotAllowAssign().shouldNotAllowChangeType();
  }
}
