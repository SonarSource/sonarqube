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

import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.server.util.OkHttpClientProvider;

import static org.assertj.core.api.Assertions.assertThat;

public class WebhookCallerImplTest {

  private static final long NOW = 1_500_000_000_000L;
  private static final String PROJECT_UUID = "P_UUID1";
  private static final String WEBHOOK_UUID = "WH_UUID1";
  private static final String CE_TASK_UUID = "CE_UUID1";
  private static final String SOME_JSON = "{\"payload\": {}}";
  private static final WebhookPayload PAYLOAD = new WebhookPayload("P1", SOME_JSON);

  @Rule
  public MockWebServer server = new MockWebServer();

  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60));

  private System2 system = new TestSystem2().setNow(NOW);

  @Test
  public void send_posts_payload_to_http_server() throws Exception {
    Webhook webhook = new Webhook(WEBHOOK_UUID, PROJECT_UUID, CE_TASK_UUID, RandomStringUtils.randomAlphanumeric(40),"my-webhook", server.url("/ping").toString());

    server.enqueue(new MockResponse().setBody("pong").setResponseCode(201));
    WebhookDelivery delivery = newSender().call(webhook, PAYLOAD);

    assertThat(delivery.getHttpStatus()).hasValue(201);
    assertThat(delivery.getWebhook().getUuid()).isEqualTo(WEBHOOK_UUID);
    assertThat(delivery.getDurationInMs().get()).isGreaterThanOrEqualTo(0);
    assertThat(delivery.getError()).isEmpty();
    assertThat(delivery.getAt()).isEqualTo(NOW);
    assertThat(delivery.getWebhook()).isSameAs(webhook);
    assertThat(delivery.getPayload()).isSameAs(PAYLOAD);

    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod()).isEqualTo("POST");
    assertThat(recordedRequest.getPath()).isEqualTo("/ping");
    assertThat(recordedRequest.getBody().readUtf8()).isEqualTo(PAYLOAD.getJson());
    assertThat(recordedRequest.getHeader("User-Agent")).isEqualTo("SonarQube/6.2");
    assertThat(recordedRequest.getHeader("Content-Type")).isEqualTo("application/json; charset=utf-8");
    assertThat(recordedRequest.getHeader("X-SonarQube-Project")).isEqualTo(PAYLOAD.getProjectKey());
  }

  @Test
  public void silently_catch_error_when_external_server_does_not_answer() throws Exception {
    Webhook webhook = new Webhook(WEBHOOK_UUID, PROJECT_UUID, CE_TASK_UUID, RandomStringUtils.randomAlphanumeric(40),"my-webhook", server.url("/ping").toString());

    server.shutdown();
    WebhookDelivery delivery = newSender().call(webhook, PAYLOAD);

    assertThat(delivery.getHttpStatus()).isEmpty();
    assertThat(delivery.getDurationInMs().get()).isGreaterThanOrEqualTo(0);
    // message can be "Connection refused" or "connect timed out"
    assertThat(delivery.getErrorMessage().get()).matches("(.*Connection refused.*)|(.*connect timed out.*)");
    assertThat(delivery.getAt()).isEqualTo(NOW);
    assertThat(delivery.getWebhook()).isSameAs(webhook);
    assertThat(delivery.getPayload()).isSameAs(PAYLOAD);
  }

  @Test
  public void silently_catch_error_when_url_is_incorrect() {
    Webhook webhook = new Webhook(WEBHOOK_UUID, PROJECT_UUID, CE_TASK_UUID, RandomStringUtils.randomAlphanumeric(40),"my-webhook", "this_is_not_an_url");

    WebhookDelivery delivery = newSender().call(webhook, PAYLOAD);

    assertThat(delivery.getHttpStatus()).isEmpty();
    assertThat(delivery.getDurationInMs().get()).isGreaterThanOrEqualTo(0);
    assertThat(delivery.getError().get()).isInstanceOf(IllegalArgumentException.class);
    assertThat(delivery.getErrorMessage().get()).isEqualTo("Webhook URL is not valid: this_is_not_an_url");
    assertThat(delivery.getAt()).isEqualTo(NOW);
    assertThat(delivery.getWebhook()).isSameAs(webhook);
    assertThat(delivery.getPayload()).isSameAs(PAYLOAD);
  }

  /**
   * SONAR-8799
   */
  @Test
  public void redirects_should_be_followed_with_POST_method() throws Exception {
    Webhook webhook = new Webhook(WEBHOOK_UUID, PROJECT_UUID, CE_TASK_UUID, RandomStringUtils.randomAlphanumeric(40),"my-webhook", server.url("/redirect").toString());

    // /redirect redirects to /target
    server.enqueue(new MockResponse().setResponseCode(307).setHeader("Location", server.url("target")));
    server.enqueue(new MockResponse().setResponseCode(200));

    WebhookDelivery delivery = newSender().call(webhook, PAYLOAD);

    assertThat(delivery.getHttpStatus().get()).isEqualTo(200);
    assertThat(delivery.getDurationInMs().get()).isGreaterThanOrEqualTo(0);
    assertThat(delivery.getError()).isEmpty();
    assertThat(delivery.getAt()).isEqualTo(NOW);
    assertThat(delivery.getWebhook()).isSameAs(webhook);
    assertThat(delivery.getPayload()).isSameAs(PAYLOAD);

    takeAndVerifyPostRequest("/redirect");
    takeAndVerifyPostRequest("/target");
  }

  @Test
  public void credentials_are_propagated_to_POST_redirects() throws Exception {
    HttpUrl url = server.url("/redirect").newBuilder().username("theLogin").password("thePassword").build();
    Webhook webhook = new Webhook(WEBHOOK_UUID, PROJECT_UUID, CE_TASK_UUID, RandomStringUtils.randomAlphanumeric(40),"my-webhook", url.toString());

    // /redirect redirects to /target
    server.enqueue(new MockResponse().setResponseCode(307).setHeader("Location", server.url("target")));
    server.enqueue(new MockResponse().setResponseCode(200));

    WebhookDelivery delivery = newSender().call(webhook, PAYLOAD);

    assertThat(delivery.getHttpStatus().get()).isEqualTo(200);

    RecordedRequest redirectedRequest = takeAndVerifyPostRequest("/redirect");
    assertThat(redirectedRequest.getHeader("Authorization")).isEqualTo(Credentials.basic(url.username(), url.password()));

    RecordedRequest targetRequest = takeAndVerifyPostRequest("/target");
    assertThat(targetRequest.getHeader("Authorization")).isEqualTo(Credentials.basic(url.username(), url.password()));
  }

  @Test
  public void redirects_throws_ISE_if_header_Location_is_missing() {
    HttpUrl url = server.url("/redirect");
    Webhook webhook = new Webhook(WEBHOOK_UUID, PROJECT_UUID, CE_TASK_UUID, RandomStringUtils.randomAlphanumeric(40),"my-webhook", url.toString());

    server.enqueue(new MockResponse().setResponseCode(307));

    WebhookDelivery delivery = newSender().call(webhook, PAYLOAD);

    Throwable error = delivery.getError().get();
    assertThat(error)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Missing HTTP header 'Location' in redirect of " + url);
  }

  @Test
  public void redirects_throws_ISE_if_header_Location_does_not_relate_to_a_supported_protocol() {
    HttpUrl url = server.url("/redirect");
    Webhook webhook = new Webhook(WEBHOOK_UUID, PROJECT_UUID, CE_TASK_UUID, RandomStringUtils.randomAlphanumeric(40),"my-webhook", url.toString());

    server.enqueue(new MockResponse().setResponseCode(307).setHeader("Location", "ftp://foo"));

    WebhookDelivery delivery = newSender().call(webhook, PAYLOAD);

    Throwable error = delivery.getError().get();
    assertThat(error)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Unsupported protocol in redirect of " + url + " to ftp://foo");
  }

  @Test
  public void send_basic_authentication_header_if_url_contains_credentials() throws Exception {
    HttpUrl url = server.url("/ping").newBuilder().username("theLogin").password("thePassword").build();
    Webhook webhook = new Webhook(WEBHOOK_UUID, PROJECT_UUID, CE_TASK_UUID, RandomStringUtils.randomAlphanumeric(40),"my-webhook", url.toString());
    server.enqueue(new MockResponse().setBody("pong"));

    WebhookDelivery delivery = newSender().call(webhook, PAYLOAD);

    assertThat(delivery.getWebhook().getUrl())
      .isEqualTo(url.toString())
      .contains("://theLogin:thePassword@");
    RecordedRequest recordedRequest = takeAndVerifyPostRequest("/ping");
    assertThat(recordedRequest.getHeader("Authorization")).isEqualTo(Credentials.basic(url.username(), url.password()));
  }

  private RecordedRequest takeAndVerifyPostRequest(String expectedPath) throws Exception {
    RecordedRequest request = server.takeRequest();

    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getPath()).isEqualTo(expectedPath);
    assertThat(request.getHeader("User-Agent")).isEqualTo("SonarQube/6.2");
    return request;
  }

  private WebhookCaller newSender() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(Version.parse("6.2"), SonarQubeSide.SERVER);
    return new WebhookCallerImpl(system, new OkHttpClientProvider().provide(new MapSettings().asConfig(), runtime));
  }
}
