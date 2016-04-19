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
package it.qualityGate;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category1Suite;
import java.util.Iterator;
import javax.mail.internet.MimeMessage;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
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
import util.QaOnly;
import util.selenium.SeleneseTest;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;
import static util.ItUtils.setServerProperty;

@Category(QaOnly.class)
public class QualityGateNotificationTest {

  private static long DEFAULT_QUALITY_GATE;

  private static final String PROJECT_KEY = "sample";

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @BeforeClass
  public static void initPeriods() throws Exception {
    setServerProperty(orchestrator, "sonar.timemachine.period1", "previous_analysis");
    setServerProperty(orchestrator, "sonar.timemachine.period2", "30");
    setServerProperty(orchestrator, "sonar.timemachine.period3", "previous_version");
    DEFAULT_QUALITY_GATE = qgClient().list().defaultGate().id();
  }

  @AfterClass
  public static void resetData() throws Exception {
    ItUtils.resetPeriods(orchestrator);
    qgClient().setDefault(DEFAULT_QUALITY_GATE);
  }

  @Before
  public void cleanUp() {
    orchestrator.resetData();
  }

  @Test
  public void status_on_metric_variation_and_send_notifications() throws Exception {
    Wiser smtpServer = new Wiser(0);
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
          "/qualityGate/notifications/email_configuration.html",
          "/qualityGate/notifications/activate_notification_channels.html").build();
      new SeleneseTest(selenese).runOn(orchestrator);

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
      assertThat((String) message.getContent()).contains("Quality gate threshold: Lines of Code variation = 0 since previous analysis");
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
