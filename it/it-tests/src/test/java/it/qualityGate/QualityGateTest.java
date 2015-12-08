/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package it.qualityGate;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import it.Category1Suite;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.project.NewProject;
import org.sonar.wsclient.qualitygate.NewCondition;
import org.sonar.wsclient.qualitygate.QualityGate;
import org.sonar.wsclient.qualitygate.QualityGateClient;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.WsCe;
import org.sonarqube.ws.WsQualityGates.ProjectStatusWsResponse;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsResponse;
import org.sonarqube.ws.client.qualitygate.ProjectStatusWsRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.projectDir;

public class QualityGateTest {

  private static final String PROJECT_KEY = "sample";

  private long provisionedProjectId = -1L;

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;
  static WsClient wsClient;

  @BeforeClass
  public static void startOrchestrator() {
    wsClient = newAdminWsClient(orchestrator);
  }

  @Before
  public void cleanUp() {
    orchestrator.resetData();
    provisionedProjectId = Long.parseLong(orchestrator.getServer().adminWsClient().projectClient().create(NewProject.create().key(PROJECT_KEY).name("Sample")).id());
  }

  @Test
  public void do_not_compute_status_if_no_gate() {
    SonarRunner build = SonarRunner.create(projectDir("qualitygate/xoo-sample"));
    orchestrator.executeBuild(build);

    assertThat(fetchResourceWithGateStatus()).isNull();
  }

  @Test
  public void status_ok_if_empty_gate() {
    QualityGate empty = qgClient().create("Empty");
    qgClient().setDefault(empty.id());

    try {
      SonarRunner build = SonarRunner.create(projectDir("qualitygate/xoo-sample"));
      orchestrator.executeBuild(build);

      assertThat(fetchGateStatus().getData()).isEqualTo("OK");
    } finally {
      qgClient().unsetDefault();
      qgClient().destroy(empty.id());
    }
  }

  @Test
  public void test_status_ok() {
    QualityGate simple = qgClient().create("SimpleWithHighThreshold");
    qgClient().setDefault(simple.id());
    qgClient().createCondition(NewCondition.create(simple.id()).metricKey("ncloc").operator("GT").warningThreshold("40"));

    try {
      SonarRunner build = SonarRunner.create(projectDir("qualitygate/xoo-sample"));
      orchestrator.executeBuild(build);

      assertThat(fetchGateStatus().getData()).isEqualTo("OK");
    } finally {
      qgClient().unsetDefault();
      qgClient().destroy(simple.id());
    }
  }

  @Test
  public void test_status_warning() {
    QualityGate simple = qgClient().create("SimpleWithLowThreshold");
    qgClient().setDefault(simple.id());
    qgClient().createCondition(NewCondition.create(simple.id()).metricKey("ncloc").operator("GT").warningThreshold("10"));

    try {
      SonarRunner build = SonarRunner.create(projectDir("qualitygate/xoo-sample"));
      orchestrator.executeBuild(build);

      assertThat(fetchGateStatus().getData()).isEqualTo("WARN");
    } finally {
      qgClient().unsetDefault();
      qgClient().destroy(simple.id());
    }

  }

  @Test
  public void test_status_error() {
    QualityGate simple = qgClient().create("SimpleWithLowThreshold");
    qgClient().setDefault(simple.id());
    qgClient().createCondition(NewCondition.create(simple.id()).metricKey("ncloc").operator("GT").errorThreshold("10"));

    try {
      SonarRunner build = SonarRunner.create(projectDir("qualitygate/xoo-sample"));
      orchestrator.executeBuild(build);

      assertThat(fetchGateStatus().getData()).isEqualTo("ERROR");
    } finally {
      qgClient().unsetDefault();
      qgClient().destroy(simple.id());
    }
  }

  @Test
  public void use_server_settings_instead_of_default_gate() {
    QualityGate alert = qgClient().create("AlertWithLowThreshold");
    qgClient().createCondition(NewCondition.create(alert.id()).metricKey("ncloc").operator("GT").warningThreshold("10"));
    QualityGate error = qgClient().create("ErrorWithLowThreshold");
    qgClient().createCondition(NewCondition.create(error.id()).metricKey("ncloc").operator("GT").errorThreshold("10"));

    qgClient().setDefault(alert.id());
    qgClient().selectProject(error.id(), provisionedProjectId);

    try {
      SonarRunner build = SonarRunner.create(projectDir("qualitygate/xoo-sample"));
      orchestrator.executeBuild(build);

      assertThat(fetchGateStatus().getData()).isEqualTo("ERROR");
    } finally {
      qgClient().unsetDefault();
      qgClient().destroy(alert.id());
      qgClient().destroy(error.id());
    }
  }

  @Test
  public void conditions_on_multiple_metric_types() {
    QualityGate allTypes = qgClient().create("AllMetricTypes");
    qgClient().createCondition(NewCondition.create(allTypes.id()).metricKey("ncloc").operator("GT").warningThreshold("10"));
    qgClient().createCondition(NewCondition.create(allTypes.id()).metricKey("duplicated_lines_density").operator("GT").warningThreshold("20"));
    qgClient().setDefault(allTypes.id());

    try {
      SonarRunner build = SonarRunner.create(projectDir("qualitygate/xoo-sample"))
        .setProperty("sonar.cpd.xoo.minimumLines", "2")
        .setProperty("sonar.cpd.xoo.minimumTokens", "5");
      orchestrator.executeBuild(build);

      Measure alertStatus = fetchGateStatus();
      assertThat(alertStatus.getData()).isEqualTo("WARN");
      assertThat(alertStatus.getAlertText())
        .contains("Lines of code > 10")
        .contains("Duplicated lines (%) > 20");
    } finally {
      qgClient().unsetDefault();
      qgClient().destroy(allTypes.id());
    }
  }

  @Test
  public void ad_hoc_build_break_strategy() throws IOException {
    QualityGate simple = qgClient().create("SimpleWithLowThresholdForBuildBreakStrategy");
    qgClient().setDefault(simple.id());
    qgClient().createCondition(NewCondition.create(simple.id()).metricKey("ncloc").operator("GT").errorThreshold("7"));

    try {
      File projectDir = projectDir("qualitygate/xoo-sample");
      SonarRunner build = SonarRunner.create(projectDir);
      orchestrator.executeBuild(build);

      String taskId = getTaskIdInLocalReport(projectDir);
      String analysisId = getAnalysisId(taskId);

      ProjectStatusWsResponse projectStatusWsResponse = wsClient.qualityGates().projectStatus(new ProjectStatusWsRequest().setAnalysisId(analysisId));
      ProjectStatusWsResponse.ProjectStatus projectStatus = projectStatusWsResponse.getProjectStatus();
      assertThat(projectStatus.getStatus()).isEqualTo(ProjectStatusWsResponse.Status.ERROR);
      assertThat(projectStatus.getConditionsCount()).isEqualTo(1);
      ProjectStatusWsResponse.Condition condition = projectStatus.getConditionsList().get(0);
      assertThat(condition.getMetricKey()).isEqualTo("ncloc");
      assertThat(condition.getErrorThreshold()).isEqualTo("7");
    } finally {
      qgClient().unsetDefault();
      qgClient().destroy(simple.id());
    }
  }

  private String getAnalysisId(String taskId) throws IOException {
    WsResponse activity = wsClient
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

  private Measure fetchGateStatus() {
    return fetchResourceWithGateStatus().getMeasure("alert_status");
  }

  private Resource fetchResourceWithGateStatus() {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT_KEY, "alert_status").setIncludeAlerts(true));
  }

  private static QualityGateClient qgClient() {
    return orchestrator.getServer().adminWsClient().qualityGateClient();
  }
}
