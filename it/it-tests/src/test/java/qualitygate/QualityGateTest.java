/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package qualitygate;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.project.NewProject;
import org.sonar.wsclient.qualitygate.NewCondition;
import org.sonar.wsclient.qualitygate.QualityGate;
import org.sonar.wsclient.qualitygate.QualityGateClient;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class QualityGateTest {

  private static final String PROJECT_KEY = "sample";

  private long provisionnedProjectId = -1L;

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())

    // 1 second. Required for notification test.
    .setServerProperty("sonar.notifications.delay", "1")

    .build();

  @Before
  public void cleanUp() {
    orchestrator.resetData();
    provisionnedProjectId = Long.parseLong(orchestrator.getServer().adminWsClient().projectClient().create(NewProject.create().key(PROJECT_KEY).name("Sample")).id());
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
    qgClient().selectProject(error.id(), provisionnedProjectId);

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
