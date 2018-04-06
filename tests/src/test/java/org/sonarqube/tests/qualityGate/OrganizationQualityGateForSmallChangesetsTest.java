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
package org.sonarqube.tests.qualityGate;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.util.Date;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.ProjectAnalyses;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.Qualitygates;
import org.sonarqube.ws.Qualitygates.ProjectStatusResponse.Status;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.client.projectanalyses.SearchRequest;
import org.sonarqube.ws.client.qualitygates.CreateConditionRequest;
import org.sonarqube.ws.client.qualitygates.ProjectStatusRequest;
import org.sonarqube.ws.client.qualitygates.UpdateConditionRequest;

import static org.apache.commons.lang.time.DateUtils.addMonths;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.formatDate;
import static util.ItUtils.projectDir;

public class OrganizationQualityGateForSmallChangesetsTest {

  @ClassRule
  public static Orchestrator orchestrator = OrganizationQualityGateSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void do_not_fail_quality_gate_with_poor_LEAK_coverage_and_a_max_of_19_lines_of_NEW_code() {
    Organizations.Organization organization = tester.organizations().generate();
    Project project = tester.projects().provision(organization);
    Qualitygates.CreateResponse qualityGate = tester.qGates().generate(organization);
    tester.qGates().associateProject(organization, qualityGate, project);
    Qualitygates.CreateConditionResponse condition = tester.wsClient().qualitygates().createCondition(new CreateConditionRequest()
      .setOrganization(organization.getKey())
      .setGateId(String.valueOf(qualityGate.getId()))
      .setMetric("new_coverage")
      .setOp("LT")
      .setWarning("90")
      .setError("80")
      .setPeriod("1"));

    tester.settings().setProjectSetting(project.getKey(), "sonar.leak.period", "previous_version");
    String password = "password1";
    Users.CreateWsResponse.User user = tester.users().generateAdministrator(organization, u -> u.setPassword(password));

    // no leak => use usual behaviour
    SonarScanner analysis = SonarScanner
      .create(projectDir("qualitygate/small-changesets/v1-1000-lines"))
      .setProperty("sonar.projectKey", project.getKey())
      .setProperty("sonar.organization", organization.getKey())
      .setProperty("sonar.login", user.getLogin())
      .setProperty("sonar.password", password)
      .setProperty("sonar.scm.provider", "xoo")
      .setProperty("sonar.scm.disabled", "false")
      .setProperty("sonar.projectDate", formatDate(addMonths(new Date(), -4)))
      .setDebugLogs(true);
    orchestrator.executeBuild(analysis);
    verifyGateStatus(project, Status.OK, false);

    // small leak => ignore coverage warning or error
    SonarScanner analysis2 = SonarScanner
      .create(projectDir("qualitygate/small-changesets/v2-1019-lines"))
      .setProperty("sonar.projectKey", project.getKey())
      .setProperty("sonar.organization", organization.getKey())
      .setProperty("sonar.login", user.getLogin())
      .setProperty("sonar.password", password)
      .setProperty("sonar.scm.provider", "xoo")
      .setProperty("sonar.scm.disabled", "false")
      .setProperty("sonar.projectDate", formatDate(addMonths(new Date(), -3)))
      .setDebugLogs(true);
    orchestrator.executeBuild(analysis2);
    verifyGateStatus(project, Status.OK, true);

    // small leak => if coverage is OK anyways, we do not have to ignore anything
    tester.wsClient().qualitygates().updateCondition(new UpdateConditionRequest()
      .setOrganization(organization.getKey())
      .setId(String.valueOf(condition.getId()))
      .setMetric("new_coverage")
      .setOp("LT")
      .setWarning("10")
      .setError("20")
      .setPeriod("1"));
    SonarScanner analysis3 = SonarScanner
      .create(projectDir("qualitygate/small-changesets/v2-1019-lines"))
      .setProperty("sonar.projectKey", project.getKey())
      .setProperty("sonar.organization", organization.getKey())
      .setProperty("sonar.login", user.getLogin())
      .setProperty("sonar.password", password)
      .setProperty("sonar.scm.provider", "xoo")
      .setProperty("sonar.scm.disabled", "false")
      .setProperty("sonar.projectDate", formatDate(addMonths(new Date(), -2)))
      .setDebugLogs(true);
    orchestrator.executeBuild(analysis3);
    verifyGateStatus(project, Status.OK, false);

    // big leak => use usual behaviour
    tester.wsClient().qualitygates().updateCondition(new UpdateConditionRequest()
      .setOrganization(organization.getKey())
      .setId(String.valueOf(condition.getId()))
      .setMetric("new_coverage")
      .setOp("LT")
      .setWarning(null)
      .setError("70")
      .setPeriod("1"));
    SonarScanner analysis4 = SonarScanner
      .create(projectDir("qualitygate/small-changesets/v2-1020-lines"))
      .setProperty("sonar.projectKey", project.getKey())
      .setProperty("sonar.organization", organization.getKey())
      .setProperty("sonar.login", user.getLogin())
      .setProperty("sonar.password", password)
      .setProperty("sonar.scm.provider", "xoo")
      .setProperty("sonar.scm.disabled", "false")
      .setProperty("sonar.projectDate", formatDate(addMonths(new Date(), -1)))
      .setDebugLogs(true);
    orchestrator.executeBuild(analysis4);
    verifyGateStatus(project, Status.ERROR, false);
  }

  private void verifyGateStatus(Project project, Status expectedStatus, boolean expectedIgnoredConditions) {
    ProjectAnalyses.SearchResponse analysis = tester.wsClient().projectAnalyses().search(new SearchRequest().setProject(project.getKey()));
    String analysisId = analysis.getAnalysesList().get(0).getKey();
    Qualitygates.ProjectStatusResponse.ProjectStatus gateStatus = tester.wsClient().qualitygates()
      .projectStatus(new ProjectStatusRequest().setAnalysisId(analysisId))
      .getProjectStatus();

    assertThat(gateStatus.getStatus()).isEqualTo(expectedStatus);
    assertThat(gateStatus.getIgnoredConditions()).isEqualTo(expectedIgnoredConditions);
  }

}
