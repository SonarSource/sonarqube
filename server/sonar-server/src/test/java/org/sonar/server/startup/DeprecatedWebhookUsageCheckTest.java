/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.startup;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.webhook.WebhookDao;
import org.sonar.db.webhook.WebhookDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeprecatedWebhookUsageCheckTest {

  @Rule
  public LogTester logTester = new LogTester();

  private final WebhookDao webhookDao = mock(WebhookDao.class);
  private final ComponentDao componentDao = mock(ComponentDao.class);
  private final DbClient dbClient = mock(DbClient.class);
  private final DeprecatedWebhookUsageCheck underTest = new DeprecatedWebhookUsageCheck(dbClient);

  @Before
  public void setup() {
    when(dbClient.webhookDao()).thenReturn(webhookDao);
    when(dbClient.componentDao()).thenReturn(componentDao);
    when(componentDao.selectOrFailByUuid(any(), any())).thenReturn(new ComponentDto().setName("ProjectA"));
  }

  @Test
  public void start() {
    underTest.start();

    verify(webhookDao, times(1)).scrollAll(any(), any());
  }

  @Test
  public void stop() {
    underTest.start();
    underTest.stop();

    verify(webhookDao, times(1)).scrollAll(any(), any());
  }

  @Test
  public void logsWarningWhenDeprecatedAddressIsUsedInProjectLevelWebhook() {
    DeprecatedWebhookUsageCheck.WebhookConsumer handler = new DeprecatedWebhookUsageCheck.WebhookConsumer(dbClient, mock(DbSession.class));

    handler.consumer.accept(new WebhookDto().setName("W1").setProjectUuid("u1").setUrl("http://127.0.0.1/webhook"));

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.WARN)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.WARN))
      .containsExactlyInAnyOrder("Webhook 'W1' for project 'ProjectA' uses an invalid, unsafe URL and will be automatically removed in a " +
        "future version of SonarQube. You should update the URL of that webhook or ask a project administrator to do it.");
  }

  @Test
  public void logsWarningWhenDeprecatedAddressIsUsedInGlobalLevelWebhook() {
    DeprecatedWebhookUsageCheck.WebhookConsumer handler = new DeprecatedWebhookUsageCheck.WebhookConsumer(dbClient, mock(DbSession.class));

    handler.consumer.accept(new WebhookDto().setName("W1").setOrganizationUuid("org1").setUrl("http://127.0.0.1/webhook"));

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.WARN)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.WARN))
      .containsExactlyInAnyOrder("Global webhook 'W1' uses an invalid, unsafe URL and will be automatically removed in a future version of SonarQube. " +
        "You should update the URL of that webhook.");
  }

  @Test
  public void doesNotLogAnythingWhenWebhookHasNonDeprecatedAddress() {
    DeprecatedWebhookUsageCheck.WebhookConsumer handler = new DeprecatedWebhookUsageCheck.WebhookConsumer(dbClient, mock(DbSession.class));

    handler.consumer.accept(new WebhookDto().setName("W1").setProjectUuid("u1").setUrl("http://abc.com/webhook"));

    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  public void doesNotLogAnythingWhenUnknownHost() {
    DeprecatedWebhookUsageCheck.WebhookConsumer handler = new DeprecatedWebhookUsageCheck.WebhookConsumer(dbClient, mock(DbSession.class));

    handler.consumer.accept(new WebhookDto().setName("W1").setProjectUuid("u1").setUrl("http://nice/webhook"));

    assertThat(logTester.logs()).isEmpty();
  }
}
