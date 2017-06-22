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

package org.sonarqube.tests.qualityProfile;

import com.sonar.orchestrator.Orchestrator;
import java.io.File;
import java.util.List;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonarqube.ws.WsUsers;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.permission.AddGroupWsRequest;
import org.sonarqube.ws.client.permission.AddUserWsRequest;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;
import util.ItUtils;
import util.user.UserRule;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.pluginArtifact;

public class BuiltInQualityProfilesNotificationTest {

  private static Orchestrator orchestrator;
  private static Wiser smtpServer;
  private static UserRule userRule;

  @Before
  public void init() throws Exception {
    smtpServer = new Wiser(0);
    smtpServer.start();
  }

  @After
  public void tearDown() throws Exception {
    if (orchestrator != null) {
      orchestrator.stop();
    }
    if (smtpServer != null) {
      smtpServer.stop();
    }
  }

  @Test
  public void does_not_send_mail_if_no_quality_profile_is_updated() throws Exception {
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(pluginArtifact("foo-plugin-v1"))
      .setServerProperty("email.smtp_host.secured", "localhost")
      .setServerProperty("email.smtp_port.secured", Integer.toString(smtpServer.getServer().getPort()))
      // .setServerProperty("sonar.web.javaAdditionalOpts", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005")// FIXME
      // remove web debugging
      .build();
    orchestrator.start();

    userRule = UserRule.from(orchestrator);

    WsUsers.CreateWsResponse.User profileAdmin1 = userRule.generate();
    WsClient wsClient = ItUtils.newAdminWsClient(orchestrator);
    wsClient.permissions().addUser(new AddUserWsRequest().setLogin(profileAdmin1.getLogin()).setPermission("profileadmin"));

    WsUsers.CreateWsResponse.User profileAdmin2 = userRule.generate();
    String groupName = randomAlphanumeric(20);
    wsClient.wsConnector().call(new PostRequest("api/user_groups/create").setParam("name", groupName)).failIfNotSuccessful();
    wsClient.permissions().addGroup(new AddGroupWsRequest().setPermission("profileadmin").setGroupName(groupName));
    wsClient.wsConnector().call(new PostRequest("api/user_groups/add_user").setParam("name", groupName).setParam("login", profileAdmin2.getLogin())).failIfNotSuccessful();

    WsUsers.CreateWsResponse.User noProfileAdmin = userRule.generate();

    orchestrator.restartServer();

    waitUntilAllNotificationsAreDelivered(1, 10, 100);
    assertThat(smtpServer.getMessages()).isEmpty();
  }

  @Test
  public void send_mail_if_quality_profile_is_updated() throws Exception {
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(pluginArtifact("foo-plugin-v1"))
      .setServerProperty("email.smtp_host.secured", "localhost")
      .setServerProperty("email.smtp_port.secured", Integer.toString(smtpServer.getServer().getPort()))
      // .setServerProperty("sonar.web.javaAdditionalOpts", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8001")// FIXME
      // remove web debugging
      .build();
    orchestrator.start();

    userRule = UserRule.from(orchestrator);

    WsUsers.CreateWsResponse.User profileAdmin1 = userRule.generate();
    WsClient wsClient = ItUtils.newAdminWsClient(orchestrator);
    wsClient.permissions().addUser(new AddUserWsRequest().setLogin(profileAdmin1.getLogin()).setPermission("profileadmin"));

    WsUsers.CreateWsResponse.User profileAdmin2 = userRule.generate();
    String groupName = randomAlphanumeric(20);
    wsClient.wsConnector().call(new PostRequest("api/user_groups/create").setParam("name", groupName)).failIfNotSuccessful();
    wsClient.permissions().addGroup(new AddGroupWsRequest().setPermission("profileadmin").setGroupName(groupName));
    wsClient.wsConnector().call(new PostRequest("api/user_groups/add_user").setParam("name", groupName).setParam("login", profileAdmin2.getLogin())).failIfNotSuccessful();

    WsUsers.CreateWsResponse.User noProfileAdmin = userRule.generate();

    // uninstall plugin V1
    wsClient.wsConnector().call(new PostRequest("api/plugins/uninstall").setParam("key", "foo")).failIfNotSuccessful();

    // install plugin V2
    File pluginsDir = new File(orchestrator.getServer().getHome() + "/extensions/plugins");
    orchestrator.getConfiguration().fileSystem().copyToDirectory(pluginArtifact("foo-plugin-v2"), pluginsDir);

    orchestrator.restartServer();

    waitUntilAllNotificationsAreDelivered(2, 10, 1_000);
    List<WiserMessage> messages = smtpServer.getMessages();
    assertThat(messages)
      .extracting(this::getMimeMessage)
      .extracting(this::getAllRecipients)
      .containsOnly("<" + profileAdmin1.getEmail() + ">", "<" + profileAdmin2.getEmail() + ">");
    String url = orchestrator.getServer().getUrl();
    assertThat(messages.get(0).getMimeMessage().getContent().toString())
      .containsSequence(
        "Built-in quality profiles have been updated:",
        "\"Basic\" - Foo " + url + "/profiles/changelog?language=foo&name=Basic&since=", "&to=",
        " 1 new rules",
        " 3 rules have been updated",
        " 1 rules removed",
        "This is a good time to review your quality profiles and update them to benefit from the latest evolutions. " + url + "/profiles")
      .isEqualTo(messages.get(1).getMimeMessage().getContent().toString());
  }

  private MimeMessage getMimeMessage(WiserMessage msg) {
    try {
      return msg.getMimeMessage();
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }

  private String getAllRecipients(MimeMessage mimeMessage) {
    try {
      return mimeMessage.getHeader("To", null);
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }

  private static void waitUntilAllNotificationsAreDelivered(int expectedNumberOfEmails, int pollNumber, int pollMillis) throws InterruptedException {
    for (int i = 0; i < pollNumber; i++) {
      if (smtpServer.getMessages().size() == expectedNumberOfEmails) {
        break;
      }
      Thread.sleep(pollMillis);
    }
  }

}
