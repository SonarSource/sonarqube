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
package org.sonar.server.webhook;

import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.webhook.WebhookDbTester;
import org.sonar.server.async.AsyncExecution;
import org.sonar.server.organization.DefaultOrganizationProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.sonar.api.utils.log.LoggerLevel.DEBUG;
import static org.sonar.db.DbTester.create;
import static org.sonar.db.webhook.WebhookTesting.newWebhook;
import static org.sonar.server.organization.TestDefaultOrganizationProvider.from;

public class SynchronousWebHooksImplTest {

  private static final long NOW = 1_500_000_000_000L;

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public DbTester db = create();
  private DbClient dbClient = db.getDbClient();

  private WebhookDbTester webhookDbTester = db.webhooks();
  private ComponentDbTester componentDbTester = db.components();
  private DefaultOrganizationProvider defaultOrganizationProvider = from(db);

  private final TestWebhookCaller caller = new TestWebhookCaller();
  private final WebhookDeliveryStorage deliveryStorage = mock(WebhookDeliveryStorage.class);
  private final WebhookPayload mock = mock(WebhookPayload.class);
  private final AsyncExecution synchronousAsyncExecution = Runnable::run;
  private final WebHooksImpl underTest = new WebHooksImpl(caller, deliveryStorage, synchronousAsyncExecution, dbClient);

  @Test
  public void isEnabled_returns_false_if_no_webhooks() {
    ComponentDto componentDto = componentDbTester.insertPrivateProject();

    assertThat(underTest.isEnabled(componentDto)).isFalse();
  }

  @Test
  public void isEnabled_returns_true_if_one_valid_global_webhook() {
    ComponentDto componentDto = componentDbTester.insertPrivateProject();
    webhookDbTester.insert(newWebhook(componentDto).setName("First").setUrl("http://url1"));

    assertThat(underTest.isEnabled(componentDto)).isTrue();
  }

  @Test
  public void isEnabled_returns_true_if_one_valid_project_webhook() {
    String organizationUuid = defaultOrganizationProvider.get().getUuid();
    ComponentDto componentDto = componentDbTester.insertPrivateProject().setOrganizationUuid(organizationUuid);
    webhookDbTester.insert(newWebhook(componentDto).setName("First").setUrl("http://url1"));

    assertThat(underTest.isEnabled(componentDto)).isTrue();
  }


  @Test
  public void do_nothing_if_no_webhooks() {
    ComponentDto componentDto = componentDbTester.insertPrivateProject().setOrganizationUuid(defaultOrganizationProvider.get().getUuid());

    underTest.sendProjectAnalysisUpdate(new WebHooks.Analysis(componentDto.uuid(), "1", "#1"), () -> mock);

    assertThat(caller.countSent()).isEqualTo(0);
    assertThat(logTester.logs(DEBUG)).isEmpty();
    verifyZeroInteractions(deliveryStorage);
  }

  @Test
  public void send_global_webhooks() {

    ComponentDto componentDto = componentDbTester.insertPrivateProject();
    webhookDbTester.insert(newWebhook(componentDto).setName("First").setUrl("http://url1"));
    webhookDbTester.insert(newWebhook(componentDto).setName("Second").setUrl("http://url2"));
    caller.enqueueSuccess(NOW, 200, 1_234);
    caller.enqueueFailure(NOW, new IOException("Fail to connect"));

    underTest.sendProjectAnalysisUpdate(new WebHooks.Analysis(componentDto.uuid(), "1", "#1"), () -> mock);

    assertThat(caller.countSent()).isEqualTo(2);
    assertThat(logTester.logs(DEBUG)).contains("Sent webhook 'First' | url=http://url1 | time=1234ms | status=200");
    assertThat(logTester.logs(DEBUG)).contains("Failed to send webhook 'Second' | url=http://url2 | message=Fail to connect");
    verify(deliveryStorage, times(2)).persist(any(WebhookDelivery.class));
    verify(deliveryStorage).purge(componentDto.uuid());

  }

  @Test
  public void send_project_webhooks() {

    String organizationUuid = defaultOrganizationProvider.get().getUuid();
    ComponentDto componentDto = componentDbTester.insertPrivateProject().setOrganizationUuid(organizationUuid);
    webhookDbTester.insert(newWebhook(componentDto).setName("First").setUrl("http://url1"));
    caller.enqueueSuccess(NOW, 200, 1_234);

    underTest.sendProjectAnalysisUpdate(new WebHooks.Analysis(componentDto.uuid(), "1", "#1"), () -> mock);

    assertThat(caller.countSent()).isEqualTo(1);
    assertThat(logTester.logs(DEBUG)).contains("Sent webhook 'First' | url=http://url1 | time=1234ms | status=200");
    verify(deliveryStorage).persist(any(WebhookDelivery.class));
    verify(deliveryStorage).purge(componentDto.uuid());

  }

}
