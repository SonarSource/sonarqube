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

  @Rule
  public MockWebServer server = new MockWebServer();

  private System2 system = new TestSystem2().setNow(NOW);

  @Test
  public void send_posts_payload_to_http_server() throws Exception {
    Webhook webhook = new Webhook("my-webhook", server.url("/ping").toString());
    WebhookPayload payload = new WebhookPayload("P1", "{the payload}");

    server.enqueue(new MockResponse().setBody("pong").setResponseCode(201));
    WebhookDelivery delivery = newSender().call(webhook, payload);

    assertThat(delivery.getHttpStatus().get()).isEqualTo(201);
    assertThat(delivery.getDurationInMs().get()).isGreaterThanOrEqualTo(0L);
    assertThat(delivery.getThrowable()).isEmpty();
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
  public void send_does_not_throw_exception_on_errors() throws Exception {
    Webhook webhook = new Webhook("my-webhook", server.url("/ping").toString());
    WebhookPayload payload = new WebhookPayload("P1", "{the payload}");

    server.shutdown();
    WebhookDelivery delivery = newSender().call(webhook, payload);

    assertThat(delivery.getHttpStatus()).isEmpty();
    assertThat(delivery.getDurationInMs()).isEmpty();
    assertThat(delivery.getThrowable().get().getMessage()).startsWith("Failed to connect to");
    assertThat(delivery.getAt()).isEqualTo(NOW);
    assertThat(delivery.getWebhook()).isSameAs(webhook);
    assertThat(delivery.getPayload()).isSameAs(payload);
  }

  private WebhookCaller newSender() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(Version.parse("6.2"), SonarQubeSide.SERVER);
    return new WebhookCallerImpl(system, new OkHttpClientProvider().provide(new MapSettings(), runtime));
  }
}
