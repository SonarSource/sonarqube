/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import org.sonarqube.tests.Category1Suite;
import java.util.Iterator;
import javax.mail.internet.MimeMessage;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.qualitygate.NewCondition;
import org.sonar.wsclient.qualitygate.QualityGate;
import org.sonar.wsclient.qualitygate.QualityGateClient;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;
import util.user.UserRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarqube.ws.WsMeasures.Measure;
import static util.ItUtils.getMeasure;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.newUserWsClient;
import static util.ItUtils.projectDir;
import static util.ItUtils.resetEmailSettings;
import static util.ItUtils.resetPeriod;
import static util.ItUtils.setServerProperty;

public class QualityGateNotificationTest {

  private static long DEFAULT_QUALITY_GATE;

  private static final String PROJECT_KEY = "sample";

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @ClassRule
  public static UserRule userRule = UserRule.from(orchestrator);

  private static Wiser smtpServer;

  @BeforeClass
  public static void init() throws Exception {
    DEFAULT_QUALITY_GATE = qgClient().list().defaultGate().id();

    setServerProperty(orchestrator, "sonar.leak.period", "previous_analysis");
    resetEmailSettings(orchestrator);

    smtpServer = new Wiser(0);
    smtpServer.start();
  }

  @AfterClass
  public static void resetData() throws Exception {
    qgClient().setDefault(DEFAULT_QUALITY_GATE);

    resetPeriod(orchestrator);
    resetEmailSettings(orchestrator);

    if (smtpServer != null) {
      smtpServer.stop();
    }
  }

  @Before
  public void cleanUp() {
    orchestrator.resetData();
  }

  @Test
  public void status_on_metric_variation_and_send_notifications() throws Exception {
    setServerProperty(orchestrator, "email.smtp_host.secured", "localhost");
    setServerProperty(orchestrator, "email.smtp_port.secured", Integer.toString(smtpServer.getServer().getPort()));

    // Create user, who will receive notifications for new violations
    userRule.createUser("tester", "Tester", "tester@example.org", "tester");
    // Send test email to the test user
    newAdminWsClient(orchestrator).wsConnector().call(new PostRequest("api/emails/send")
      .setParam("to", "test@example.org")
      .setParam("message", "This is a test message from SonarQube"))
      .failIfNotSuccessful();
    // Add notifications to the test user
    WsClient wsClient = newUserWsClient(orchestrator, "tester", "tester");
    wsClient.wsConnector().call(new PostRequest("api/notifications/add")
      .setParam("type", "NewAlerts")
      .setParam("channel", "EmailNotificationChannel"))
      .failIfNotSuccessful();

    // Create quality gate with conditions on variations
    QualityGate simple = qgClient().create("SimpleWithDifferential");
    qgClient().setDefault(simple.id());
    qgClient().createCondition(NewCondition.create(simple.id()).metricKey("ncloc").period(1).operator("EQ").warningThreshold("0"));

    SonarScanner analysis = SonarScanner.create(projectDir("qualitygate/xoo-sample"));
    orchestrator.executeBuild(analysis);
    assertThat(getGateStatusMeasure().getValue()).isEqualTo("OK");

    orchestrator.executeBuild(analysis);
    assertThat(getGateStatusMeasure().getValue()).isEqualTo("WARN");

    qgClient().unsetDefault();
    qgClient().destroy(simple.id());

    waitUntilAllNotificationsAreDelivered(smtpServer);

    Iterator<WiserMessage> emails = smtpServer.getMessages().iterator();

    MimeMessage message = emails.next().getMimeMessage();
    assertThat(message.getHeader("To", null)).isEqualTo("<test@example.org>");
    assertThat((String) message.getContent()).contains("This is a test message from SonarQube");

    assertThat(emails.hasNext()).isTrue();
    message = emails.next().getMimeMessage();
    assertThat(message.getHeader("To", null)).isEqualTo("<tester@example.org>");
    assertThat((String) message.getContent()).contains("Quality gate status: Orange (was Green)");
    assertThat((String) message.getContent()).contains("Quality gate threshold: Lines of Code variation = 0 since previous analysis");
    assertThat((String) message.getContent()).contains("/dashboard?id=sample");
    assertThat(emails.hasNext()).isFalse();
  }

  private Measure getGateStatusMeasure() {
    return getMeasure(orchestrator, PROJECT_KEY, "alert_status");
  }

  private static QualityGateClient qgClient() {
    return orchestrator.getServer().adminWsClient().qualityGateClient();
  }

  private static void waitUntilAllNotificationsAreDelivered(Wiser smtpServer) throws InterruptedException {
    for (int i = 0; i < 10; i++) {
      if (smtpServer.getMessages().size() == 2) {
        break;
      }
      Thread.sleep(1_000);
    }
  }
}
