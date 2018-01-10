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
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.pageobjects.Navigation;
import org.sonarqube.tests.Category6Suite;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.Users;
import util.issue.IssueRule;

import static util.ItUtils.restoreProfile;
import static util.ItUtils.runProjectAnalysis;

public class OrganizationIssuesPageTest {

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Rule
  public IssueRule issueRule = IssueRule.from(orchestrator);

  private Organizations.Organization org1;
  private Organizations.Organization org2;
  private Users.CreateWsResponse.User user1;
  private Users.CreateWsResponse.User user2;

  @Before
  public void setUp() {
    org1 = tester.organizations().generate();
    org2 = tester.organizations().generate();
    user1 = tester.users().generate();
    user2 = tester.users().generate();
    tester.organizations().addMember(org1, user1);
    tester.organizations().addMember(org2, user2);
    restoreProfile(orchestrator, getClass().getResource("/issue/with-many-rules.xml"), org1.getKey());
    restoreProfile(orchestrator, getClass().getResource("/issue/with-many-rules.xml"), org2.getKey());
  }

  @Test
  public void display_organization_rules_only() {
    String project1 = provisionProject(org1);
    analyseProject(project1, org1.getKey());
    String project2 = provisionProject(org2);
    analyseProject(project2, org2.getKey());
    Navigation nav = tester.openBrowser().logIn().submitCredentials(user1.getLogin());

    nav.openIssues(org1.getKey())
      .issuesCount(2)
      .componentsShouldNotContain(org1.getName());

    nav.openIssues()
      .issuesCount(4)
      .componentsShouldContain("Org ");
  }

  private String provisionProject(Organizations.Organization organization) {
    return tester.projects().provision(organization).getKey();
  }

  private void analyseProject(String projectKey, String organization) {
    runProjectAnalysis(orchestrator, "shared/xoo-multi-modules-sample",
      "sonar.projectKey", projectKey,
      "sonar.organization", organization,
      "sonar.login", "admin",
      "sonar.password", "admin",
      "sonar.scm.disabled", "false",
      "sonar.scm.provider", "xoo");
  }

}
