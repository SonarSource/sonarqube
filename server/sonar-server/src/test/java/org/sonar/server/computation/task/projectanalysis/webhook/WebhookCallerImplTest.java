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
package org.sonar.server.computation.task.projectanalysis.webhook;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.config.MapSettings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.server.util.OkHttpClientProvider;

import static org.assertj.core.api.Assertions.assertThat;


public class WebhookCallerImplTest {

  private static final long NOW = 1_500_000_000_000L;
  private static final String PROJECT_UUID = "P_UUID1";
  private static final String CE_TASK_UUID = "CE_UUID1";

  @Rule
  public MockWebServer server = new MockWebServer();
  @Rule
  public Timeout timeout = Timeout.seconds(60);

  private System2 system = new TestSystem2().setNow(NOW);

  @Test
  public void send_posts_payload_to_http_server() throws Exception {
    Webhook webhook = new Webhook(PROJECT_UUID, CE_TASK_UUID, "my-webhook", server.url("/ping").toString());
    WebhookPayload payload = new WebhookPayload("P1", "{the payload}");

    server.enqueue(new MockResponse().setBody("pong").setResponseCode(201));
    WebhookDelivery delivery = newSender().call(webhook, payload);

    assertThat(delivery.getHttpStatus().get()).isEqualTo(201);
    assertThat(delivery.getDurationInMs().get()).isGreaterThanOrEqualTo(0);
    assertThat(delivery.getError()).isEmpty();
    assertThat(delivery.getAt()).isEqualTo(NOW);
    assertThat(delivery.getWebhook()).isSameAs(webhook);
    assertThat(delivery.getPayload()).isSameAs(payload);

    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod()).isEqualTo("POST");
    assertThat(recordedRequest.getPath()).isEqualTo("/ping");
    assertThat(recordedRequest.getBody().readUtf8()).isEqualTo(payload.toJson());
    assertThat(recordedRequest.getHeader("User-Agent")).isEqualTo("SonarQube/6.2");
    assertThat(recordedRequest.getHeader("Content-Type")).isEqualTo("application/json; charset=utf-8");
    assertThat(recordedRequest.getHeader("X-SonarQube-Project")).isEqualTo(payload.getProjectKey());
  }

  @Test
  public void silently_catch_error_when_external_server_does_not_answer() throws Exception {
    Webhook webhook = new Webhook(PROJECT_UUID, CE_TASK_UUID, "my-webhook", server.url("/ping").toString());
    WebhookPayload payload = new WebhookPayload("P1", "{the payload}");

    server.shutdown();
    WebhookDelivery delivery = newSender().call(webhook, payload);

    assertThat(delivery.getHttpStatus()).isEmpty();
    assertThat(delivery.getDurationInMs()).isEmpty();
    // message can be "Failed to connect" or "connect timed out"
    assertThat(delivery.getErrorMessage().get()).contains("connect");
    assertThat(delivery.getAt()).isEqualTo(NOW);
    assertThat(delivery.getWebhook()).isSameAs(webhook);
    assertThat(delivery.getPayload()).isSameAs(payload);
  }

  @Test
  public void silently_catch_error_when_url_is_incorrect() throws Exception {
    Webhook webhook = new Webhook(PROJECT_UUID, CE_TASK_UUID, "my-webhook", "this_is_not_an_url");
    WebhookPayload payload = new WebhookPayload("P1", "{the payload}");

    WebhookDelivery delivery = newSender().call(webhook, payload);

    assertThat(delivery.getHttpStatus()).isEmpty();
    assertThat(delivery.getDurationInMs()).isEmpty();
    assertThat(delivery.getError().get()).isInstanceOf(IllegalArgumentException.class);
    assertThat(delivery.getErrorMessage().get()).isEqualTo("unexpected url: this_is_not_an_url");
    assertThat(delivery.getAt()).isEqualTo(NOW);
    assertThat(delivery.getWebhook()).isSameAs(webhook);
    assertThat(delivery.getPayload()).isSameAs(payload);
  }

  private WebhookCaller newSender() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(Version.parse("6.2"), SonarQubeSide.SERVER);
    return new WebhookCallerImpl(system, new OkHttpClientProvider().provide(new MapSettings(), runtime));
  }
}
