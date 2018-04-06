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

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.QGateTester;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Measures.Measure;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.Qualitygates;
import org.sonarqube.ws.Qualitygates.CreateResponse;
import org.sonarqube.ws.Qualitygates.ListWsResponse.QualityGate;
import org.sonarqube.ws.Qualitygates.ProjectStatusResponse;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.client.ce.TaskRequest;
import org.sonarqube.ws.client.metrics.CreateRequest;
import org.sonarqube.ws.client.metrics.DeleteRequest;
import org.sonarqube.ws.client.permissions.AddUserRequest;
import org.sonarqube.ws.client.qualitygates.CreateConditionRequest;
import org.sonarqube.ws.client.qualitygates.DeleteConditionRequest;
import org.sonarqube.ws.client.qualitygates.DestroyRequest;
import org.sonarqube.ws.client.qualitygates.ListRequest;
import org.sonarqube.ws.client.qualitygates.ProjectStatusRequest;
import org.sonarqube.ws.client.qualitygates.SelectRequest;
import org.sonarqube.ws.client.qualitygates.SetAsDefaultRequest;
import org.sonarqube.ws.client.qualitygates.UpdateConditionRequest;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonarqube.ws.Qualitygates.ProjectStatusResponse.Status.ERROR;
import static util.ItUtils.concat;
import static util.ItUtils.expectHttpError;
import static util.ItUtils.extractCeTaskId;
import static util.ItUtils.getMeasure;
import static util.ItUtils.projectDir;

public class QualityGateTest {

  private static final String TASK_STATUS_SUCCESS = "SUCCESS";
  private static final String QG_STATUS_OK = "OK";
  private static final String QG_STATUS_ERROR = "ERROR";
  private static final String QG_STATUS_WARN = "WARN";

  @ClassRule
  public static Orchestrator orchestrator = QualityGateSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  @Test
  public void status_ok_if_empty_gate() throws Exception {
    Project project = tester.projects().provision();
    Qualitygates.CreateResponse qualityGate = tester.qGates().generate();
    tester.qGates().associateProject(qualityGate, project);
    BuildResult buildResult = executeAnalysis(project.getKey());

    verifyQGStatusInPostTask(buildResult, project.getKey(), TASK_STATUS_SUCCESS, QG_STATUS_OK);

    assertThat(getGateStatusMeasure(project.getKey()).getValue()).isEqualTo("OK");
  }

  @Test
  public void test_status_ok() throws IOException {
    Project project = tester.projects().provision();
    Qualitygates.CreateResponse qualityGate = tester.qGates().generate();
    tester.qGates().associateProject(qualityGate, project);
    tester.qGates().service().createCondition(new CreateConditionRequest().setGateId(Long.toString(qualityGate.getId())).setMetric("ncloc").setOp("GT").setWarning("40"));
    BuildResult buildResult = executeAnalysis(project.getKey());

    verifyQGStatusInPostTask(buildResult, project.getKey(), TASK_STATUS_SUCCESS, QG_STATUS_OK);

    assertThat(getGateStatusMeasure(project.getKey()).getValue()).isEqualTo("OK");
  }

  @Test
  public void test_status_warning() throws IOException {
    Project project = tester.projects().provision();
    Qualitygates.CreateResponse qualityGate = tester.qGates().generate();
    tester.qGates().associateProject(qualityGate, project);
    tester.qGates().service().createCondition(new CreateConditionRequest().setGateId(Long.toString(qualityGate.getId())).setMetric("ncloc").setOp("GT").setWarning("10"));
    BuildResult buildResult = executeAnalysis(project.getKey());

    verifyQGStatusInPostTask(buildResult, project.getKey(), TASK_STATUS_SUCCESS, QG_STATUS_WARN);

    assertThat(getGateStatusMeasure(project.getKey()).getValue()).isEqualTo("WARN");
  }

  @Test
  public void test_status_error() throws IOException {
    Project project = tester.projects().provision();
    Qualitygates.CreateResponse qualityGate = tester.qGates().generate();
    tester.qGates().associateProject(qualityGate, project);
    tester.qGates().service().createCondition(new CreateConditionRequest().setGateId(Long.toString(qualityGate.getId())).setMetric("ncloc").setOp("GT").setError("10"));
    BuildResult buildResult = executeAnalysis(project.getKey());

    verifyQGStatusInPostTask(buildResult, project.getKey(), TASK_STATUS_SUCCESS, QG_STATUS_ERROR);

    assertThat(getGateStatusMeasure(project.getKey()).getValue()).isEqualTo("ERROR");
  }

  @Test
  public void use_server_settings_instead_of_default_gate() throws IOException {
    QualityGate existingDefaultQualityGate = tester.qGates().service().list(new ListRequest()).getQualitygatesList()
      .stream()
      .filter(QualityGate::getIsDefault)
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("No default quality gate found"));
    try {
      Qualitygates.CreateResponse defaultQualityGate = tester.qGates().generate();
      tester.qGates().service().createCondition(new CreateConditionRequest().setGateId(Long.toString(defaultQualityGate.getId())).setMetric("ncloc").setOp("GT").setWarning("10"));
      tester.qGates().service().setAsDefault(new SetAsDefaultRequest().setId(Long.toString(defaultQualityGate.getId())));

      Project project = tester.projects().provision();
      Qualitygates.CreateResponse qualityGate = tester.qGates().generate();
      tester.qGates().service().createCondition(new CreateConditionRequest().setGateId(Long.toString(qualityGate.getId())).setMetric("ncloc").setOp("GT").setError("10"));
      tester.qGates().associateProject(qualityGate, project);

      BuildResult buildResult = executeAnalysis(project.getKey());

      verifyQGStatusInPostTask(buildResult, project.getKey(), TASK_STATUS_SUCCESS, QG_STATUS_ERROR);
      assertThat(getGateStatusMeasure(project.getKey()).getValue()).isEqualTo("ERROR");
    } finally {
      tester.qGates().service().setAsDefault(new SetAsDefaultRequest().setId(Long.toString(existingDefaultQualityGate.getId())));
    }
  }

  @Test
  public void conditions_on_multiple_metric_types() throws IOException {
    Project project = tester.projects().provision();
    Qualitygates.CreateResponse qualityGate = tester.qGates().generate();
    tester.qGates().associateProject(qualityGate, project);
    tester.qGates().service().createCondition(new CreateConditionRequest().setGateId(Long.toString(qualityGate.getId())).setMetric("ncloc").setOp("GT").setWarning("10"));
    tester.qGates().service()
      .createCondition(new CreateConditionRequest().setGateId(Long.toString(qualityGate.getId())).setMetric("duplicated_lines_density").setOp("GT").setWarning("20"));
    BuildResult buildResult = executeAnalysis(project.getKey(), "sonar.cpd.xoo.minimumLines", "2", "sonar.cpd.xoo.minimumTokens", "5");

    verifyQGStatusInPostTask(buildResult, project.getKey(), TASK_STATUS_SUCCESS, QG_STATUS_WARN);

    Measure alertStatus = getGateStatusMeasure(project.getKey());
    assertThat(alertStatus.getValue()).isEqualTo("WARN");

    String qualityGateDetailJson = getMeasure(orchestrator, project.getKey(), "quality_gate_details").getValue();
    assertThat(QualityGateDetails.parse(qualityGateDetailJson).getConditions())
      .extracting(QualityGateDetails.Conditions::getMetric, QualityGateDetails.Conditions::getOp, QualityGateDetails.Conditions::getWarning)
      .contains(tuple("ncloc", "GT", "10"), tuple("duplicated_lines_density", "GT", "20"));
  }

  @Test
  public void ad_hoc_build_break_strategy() throws IOException {
    Project project = tester.projects().provision();
    Qualitygates.CreateResponse qualityGate = tester.qGates().generate();
    tester.qGates().associateProject(qualityGate, project);
    tester.qGates().service().createCondition(new CreateConditionRequest().setGateId(Long.toString(qualityGate.getId())).setMetric("ncloc").setOp("GT").setError("7"));
    BuildResult buildResult = executeAnalysis(project.getKey());

    verifyQGStatusInPostTask(buildResult, project.getKey(), TASK_STATUS_SUCCESS, QG_STATUS_ERROR);

    String taskId = getTaskIdInLocalReport(projectDir("qualitygate/xoo-sample"));
    String analysisId = tester.wsClient().ce().task(new TaskRequest().setId(taskId)).getTask().getAnalysisId();

    ProjectStatusResponse projectStatusWsResponse = tester.qGates().service().projectStatus(new ProjectStatusRequest().setAnalysisId(analysisId));
    ProjectStatusResponse.ProjectStatus projectStatus = projectStatusWsResponse.getProjectStatus();
    assertThat(projectStatus.getStatus()).isEqualTo(ERROR);
    assertThat(projectStatus.getConditionsCount()).isEqualTo(1);
    ProjectStatusResponse.Condition condition = projectStatus.getConditionsList().get(0);
    assertThat(condition.getMetricKey()).isEqualTo("ncloc");
    assertThat(condition.getErrorThreshold()).isEqualTo("7");
  }

  @Test
  public void does_not_fail_when_condition_is_on_removed_metric() throws Exception {
    Project project = tester.projects().provision();
    String customMetricKey = randomAlphabetic(10);
    tester.wsClient().metrics().create(new CreateRequest().setKey(customMetricKey).setName(customMetricKey).setType("INT"));
    try {
      // create quality gate
      Qualitygates.CreateResponse qualityGate = tester.qGates().generate();
      Long qualityGateId = qualityGate.getId();
      tester.qGates().service().createCondition(new CreateConditionRequest().setGateId(Long.toString(qualityGate.getId())).setMetric(customMetricKey).setOp("GT").setWarning("40"));
      // delete custom metric
      tester.wsClient().metrics().delete(new DeleteRequest().setKeys(ImmutableList.of(customMetricKey)));

      // run analysis
      tester.qGates().service().select(new SelectRequest().setProjectKey(project.getKey()).setGateId(String.valueOf(qualityGateId)));
      BuildResult buildResult = executeAnalysis(project.getKey());

      // verify quality gate
      verifyQGStatusInPostTask(buildResult, project.getKey(), TASK_STATUS_SUCCESS, QG_STATUS_OK);
      assertThat(getGateStatusMeasure(project.getKey()).getValue()).isEqualTo("OK");
    } finally {
      tester.wsClient().metrics().delete(new DeleteRequest().setKeys(ImmutableList.of(customMetricKey)));
    }
  }

  @Test
  public void administrate_quality_gate_with_gateadmin_permission() {
    // user is quality gate admin of default organization
    Users.CreateWsResponse.User user = tester.users().generate();
    tester.wsClient().permissions().addUser(new AddUserRequest().setLogin(user.getLogin()).setPermission("gateadmin"));
    QGateTester qGateAdminTester = tester.as(user.getLogin()).qGates();

    // perform administration operations
    CreateResponse qualityGate = qGateAdminTester.generate();
    Qualitygates.CreateConditionResponse condition = qGateAdminTester.service().createCondition(new CreateConditionRequest()
      .setGateId(String.valueOf(qualityGate.getId())).setMetric("coverage").setOp("LT").setError("90"));
    qGateAdminTester.service().updateCondition(new UpdateConditionRequest()
      .setId(String.valueOf(condition.getId())).setMetric("coverage").setOp("LT").setError("90").setWarning("80"));
    qGateAdminTester.service().deleteCondition(new DeleteConditionRequest().setId(Long.toString(condition.getId())));
    qGateAdminTester.service().destroy(new DestroyRequest().setId(Long.toString(qualityGate.getId())));
  }

  @Test
  public void fail_to_create_and_update_conditions_when_using_invalid_values() {
    Qualitygates.CreateResponse qualityGate = tester.qGates().generate();

    expectHttpError(400,
      "Invalid value 'INVALID' for metric 'Lines of Code'",
      () -> tester.qGates().service().createCondition(new CreateConditionRequest().setGateId(Long.toString(qualityGate.getId())).setMetric("ncloc").setOp("GT").setWarning("INVALID")));
    expectHttpError(400,
      "Invalid value '10d' for metric 'Technical Debt'",
      () -> tester.qGates().service().createCondition(new CreateConditionRequest().setGateId(Long.toString(qualityGate.getId())).setMetric("sqale_index").setOp("GT").setWarning("10d")));
    expectHttpError(400,
      "Invalid value '10%' for metric 'Coverage'",
      () -> tester.qGates().service().createCondition(new CreateConditionRequest().setGateId(Long.toString(qualityGate.getId())).setMetric("coverage").setOp("GT").setWarning("10%")));
  }

  private BuildResult executeAnalysis(String projectKey, String... keyValueProperties) {
    return orchestrator.executeBuild(SonarScanner.create(
      projectDir("qualitygate/xoo-sample"), concat(keyValueProperties, "sonar.projectKey", projectKey)));
  }

  private void verifyQGStatusInPostTask(BuildResult buildResult, String projectKey, String taskStatus, String qgStatus) throws IOException {
    List<String> logsLines = FileUtils.readLines(orchestrator.getServer().getCeLogs(), Charsets.UTF_8);
    List<String> postTaskLogLines = extractPosttaskPluginLogs(extractCeTaskId(buildResult), logsLines);

    assertThat(postTaskLogLines).hasSize(1);
    assertThat(postTaskLogLines.iterator().next())
      .contains("CeTask[" + taskStatus + "]")
      .contains("Project[" + projectKey + "]")
      .contains("QualityGate[" + qgStatus + "]");
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

  private Measure getGateStatusMeasure(String projectKey) {
    return getMeasure(orchestrator, projectKey, "alert_status");
  }

  private static List<String> extractPosttaskPluginLogs(String taskUuid, Iterable<String> ceLogs) {
    return StreamSupport.stream(ceLogs.spliterator(), false)
      .filter(s -> s.contains("POSTASKPLUGIN: finished()"))
      .filter(s -> s.contains(taskUuid))
      .collect(Collectors.toList());
  }

  static class QualityGateDetails {

    private String level;

    private List<Conditions> conditions = new ArrayList<>();

    String getLevel() {
      return level;
    }

    List<Conditions> getConditions() {
      return conditions;
    }

    public static QualityGateDetails parse(String json) {
      Gson gson = new Gson();
      return gson.fromJson(json, QualityGateDetails.class);
    }

    public static class Conditions {
      private final String metric;
      private final String op;
      private final String warning;
      private final String actual;
      private final String level;

      private Conditions(String metric, String op, String values, String actual, String level) {
        this.metric = metric;
        this.op = op;
        this.warning = values;
        this.actual = actual;
        this.level = level;
      }

      String getMetric() {
        return metric;
      }

      String getOp() {
        return op;
      }

      String getWarning() {
        return warning;
      }

      String getActual() {
        return actual;
      }

      String getLevel() {
        return level;
      }
    }
  }
}
