/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package qualitygate;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import com.sonar.orchestrator.util.NetworkUtils;
import java.util.Iterator;
import javax.mail.internet.MimeMessage;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.qualitygate.NewCondition;
import org.sonar.wsclient.qualitygate.QualityGate;
import org.sonar.wsclient.qualitygate.QualityGateClient;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.PropertyUpdateQuery;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class QualityGateNotificationTest {

  private static final String PROJECT_KEY = "sample";

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())

    // 1 second. Required for notification test.
    .setServerProperty("sonar.notifications.delay", "1")

    .build();

  @Before
  public void cleanUp() {
    orchestrator.resetData();
  }

  @Test
  public void status_on_metric_variation_and_send_notifications() throws Exception {
    Wiser smtpServer = new Wiser(NetworkUtils.getNextAvailablePort());
    try {
      // Configure SMTP
      smtpServer.start();
      Sonar wsClient = orchestrator.getServer().getAdminWsClient();
      wsClient.update(new PropertyUpdateQuery("email.smtp_host.secured", "localhost"));
      wsClient.update(new PropertyUpdateQuery("email.smtp_port.secured", Integer.toString(smtpServer.getServer().getPort())));

      // Create user, who will receive notifications for new violations
      orchestrator.getServer().adminWsClient().post("api/users/create", "login", "tester", "name", "Tester", "email", "tester@example.org", "password", "tester");
      Selenese selenese = Selenese
        .builder()
        .setHtmlTestsInClasspath("notifications",
          "/qualitygate/notifications/email_configuration.html",
          "/qualitygate/notifications/activate_notification_channels.html").build();
      orchestrator.executeSelenese(selenese);

      // Create quality gate with conditions on variations
      QualityGate simple = qgClient().create("SimpleWithDifferential");
      qgClient().setDefault(simple.id());
      qgClient().createCondition(NewCondition.create(simple.id()).metricKey("ncloc").period(1).operator("EQ").warningThreshold("0"));

      SonarRunner analysis = SonarRunner.create(projectDir("qualitygate/xoo-sample"));
      orchestrator.executeBuild(analysis);
      assertThat(fetchGateStatus().getData()).isEqualTo("OK");

      orchestrator.executeBuild(analysis);
      assertThat(fetchGateStatus().getData()).isEqualTo("WARN");

      qgClient().unsetDefault();
      qgClient().destroy(simple.id());

      // Wait until all notifications are delivered
      Thread.sleep(10000);

      Iterator<WiserMessage> emails = smtpServer.getMessages().iterator();

      MimeMessage message = emails.next().getMimeMessage();
      assertThat(message.getHeader("To", null)).isEqualTo("<test@example.org>");
      assertThat((String) message.getContent()).contains("This is a test message from Sonar");

      assertThat(emails.hasNext()).isTrue();
      message = emails.next().getMimeMessage();
      assertThat(message.getHeader("To", null)).isEqualTo("<tester@example.org>");
      assertThat((String) message.getContent()).contains("Quality gate status: Orange (was Green)");
      assertThat((String) message.getContent()).contains("Quality gate threshold: Lines of code variation = 0 since previous analysis");
      assertThat((String) message.getContent()).contains("/dashboard/index/sample");
      assertThat(emails.hasNext()).isFalse();

    } finally {
      smtpServer.stop();
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
