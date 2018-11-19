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
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.tests.Category6Suite;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.WsCe;
import org.sonarqube.ws.WsProjects.CreateWsResponse.Project;
import org.sonarqube.ws.WsQualityGates;
import org.sonarqube.ws.WsQualityGates.CreateWsResponse;
import org.sonarqube.ws.WsUsers;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;
import org.sonarqube.ws.client.qualitygate.CreateConditionRequest;
import org.sonarqube.ws.client.qualitygate.ProjectStatusWsRequest;
import org.sonarqube.ws.client.qualitygate.UpdateConditionRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.getMeasure;
import static util.ItUtils.projectDir;

public class QualityGateForSmallChangesetsTest {

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void do_not_fail_quality_gate_with_poor_LEAK_coverage_and_a_max_of_19_lines_of_NEW_code() throws Exception {
    Organizations.Organization organization = tester.organizations().generate();
    Project project = tester.projects().generate(organization);
    CreateWsResponse qualityGate = tester.qGates().generate();
    tester.qGates().associateProject(qualityGate, project);
    WsQualityGates.CreateConditionWsResponse condition = tester.wsClient().qualityGates().createCondition(CreateConditionRequest.builder()
      .setQualityGateId(qualityGate.getId())
      .setMetricKey("new_coverage")
      .setOperator("LT")
      .setWarning("90")
      .setError("80")
      .setPeriod(1)
      .build());

    tester.settings().setProjectSetting(project.getKey(), "sonar.leak.period", "previous_version");
    String password = "password1";
    WsUsers.CreateWsResponse.User user = tester.users().generateAdministrator(organization, u -> u.setPassword(password));

    // no leak => use usual behaviour
    SonarScanner analysis = SonarScanner
      .create(projectDir("qualitygate/small-changesets/v1-1000-lines"))
      .setProperty("sonar.projectKey", project.getKey())
      .setProperty("sonar.organization", organization.getKey())
      .setProperty("sonar.login", user.getLogin())
      .setProperty("sonar.password", password)
      .setProperty("sonar.scm.provider", "xoo")
      .setProperty("sonar.scm.disabled", "false")
      .setProperty("sonar.projectDate", "2013-04-01")
      .setDebugLogs(true);
    orchestrator.executeBuild(analysis);
    assertThat(getMeasure(orchestrator, project.getKey(), "alert_status").getValue()).isEqualTo("OK");
    assertIgnoredConditions(project, "qualitygate/small-changesets/v1-1000-lines", false);

    // small leak => ignore coverage warning or error
    SonarScanner analysis2 = SonarScanner
      .create(projectDir("qualitygate/small-changesets/v2-1019-lines"))
      .setProperty("sonar.projectKey", project.getKey())
      .setProperty("sonar.organization", organization.getKey())
      .setProperty("sonar.login", user.getLogin())
      .setProperty("sonar.password", password)
      .setProperty("sonar.scm.provider", "xoo")
      .setProperty("sonar.scm.disabled", "false")
      .setProperty("sonar.projectDate", "2014-04-01")
      .setDebugLogs(true);
    orchestrator.executeBuild(analysis2);
    assertThat(getMeasure(orchestrator, project.getKey(), "alert_status").getValue()).isEqualTo("OK");
    assertIgnoredConditions(project, "qualitygate/small-changesets/v2-1019-lines", true);

    // small leak => if coverage is OK anyways, we do not have to ignore anything
    tester.wsClient().qualityGates().updateCondition(UpdateConditionRequest.builder()
      .setConditionId(condition.getId())
      .setMetricKey("new_coverage")
      .setOperator("LT")
      .setWarning("10")
      .setError("20")
      .setPeriod(1)
      .build());
    SonarScanner analysis3 = SonarScanner
      .create(projectDir("qualitygate/small-changesets/v2-1019-lines"))
      .setProperty("sonar.projectKey", project.getKey())
      .setProperty("sonar.organization", organization.getKey())
      .setProperty("sonar.login", user.getLogin())
      .setProperty("sonar.password", password)
      .setProperty("sonar.scm.provider", "xoo")
      .setProperty("sonar.scm.disabled", "false")
      .setProperty("sonar.projectDate", "2014-04-02")
      .setDebugLogs(true);
    orchestrator.executeBuild(analysis3);
    assertThat(getMeasure(orchestrator, project.getKey(), "alert_status").getValue()).isEqualTo("OK");
    assertIgnoredConditions(project, "qualitygate/small-changesets/v2-1019-lines", false);

    // big leak => use usual behaviour
    tester.wsClient().qualityGates().updateCondition(UpdateConditionRequest.builder()
      .setConditionId(condition.getId())
      .setMetricKey("new_coverage")
      .setOperator("LT")
      .setWarning(null)
      .setError("70")
      .setPeriod(1)
      .build());
    SonarScanner analysis4 = SonarScanner
      .create(projectDir("qualitygate/small-changesets/v2-1020-lines"))
      .setProperty("sonar.projectKey", project.getKey())
      .setProperty("sonar.organization", organization.getKey())
      .setProperty("sonar.login", user.getLogin())
      .setProperty("sonar.password", password)
      .setProperty("sonar.scm.provider", "xoo")
      .setProperty("sonar.scm.disabled", "false")
      .setProperty("sonar.projectDate", "2014-04-03")
      .setDebugLogs(true);
    orchestrator.executeBuild(analysis4);
    assertThat(getMeasure(orchestrator, project.getKey(), "alert_status").getValue()).isEqualTo("ERROR");
    assertIgnoredConditions(project, "qualitygate/small-changesets/v2-1020-lines", false);
  }

  private void assertIgnoredConditions(Project project, String projectDir, boolean expected) throws IOException {
    String analysisId = getAnalysisId(getTaskIdInLocalReport(projectDir(projectDir)));
    boolean ignoredConditions = tester.wsClient().qualityGates()
      .projectStatus(new ProjectStatusWsRequest().setAnalysisId(analysisId))
      .getProjectStatus()
      .getIgnoredConditions();
    assertThat(ignoredConditions).isEqualTo(expected);
  }

  private String getAnalysisId(String taskId) throws IOException {
    WsResponse activity = tester.wsClient()
      .wsConnector()
      .call(new GetRequest("api/ce/task")
        .setParam("id", taskId)
        .setMediaType(MediaTypes.PROTOBUF));
    WsCe.TaskResponse activityWsResponse = WsCe.TaskResponse.parseFrom(activity.contentStream());
    return activityWsResponse.getTask().getAnalysisId();
  }

  private String getTaskIdInLocalReport(File projectDirectory) throws IOException {
    File metadata = new File(projectDirectory, ".sonar/report-task.txt");
    assertThat(metadata).exists().isFile();
    // verify properties
    Properties props = new Properties();
    props.load(new StringReader(FileUtils.readFileToString(metadata, StandardCharsets.UTF_8)));
    assertThat(props.getProperty("ceTaskId")).isNotEmpty();

    return props.getProperty("ceTaskId");
  }
}
