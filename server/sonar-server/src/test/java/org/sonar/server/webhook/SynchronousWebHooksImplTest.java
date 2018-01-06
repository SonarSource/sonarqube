/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.async.AsyncExecution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class SynchronousWebHooksImplTest {

  private static final long NOW = 1_500_000_000_000L;
  private static final String PROJECT_UUID = "P1_UUID";

  @Rule
  public LogTester logTester = new LogTester();

  private final MapSettings settings = new MapSettings();
  private final TestWebhookCaller caller = new TestWebhookCaller();
  private final WebhookDeliveryStorage deliveryStorage = mock(WebhookDeliveryStorage.class);
  private final WebhookPayload mock = mock(WebhookPayload.class);
  private final AsyncExecution synchronousAsyncExecution = Runnable::run;
  private final WebHooksImpl underTest = new WebHooksImpl(caller, deliveryStorage, synchronousAsyncExecution);

  @Test
  public void isEnabled_returns_false_if_no_webHoolds() {
    assertThat(underTest.isEnabled(settings.asConfig())).isFalse();
  }

  @Test
  public void isEnabled_returns_true_if_one_valid_global_webhook() {
    settings.setProperty("sonar.webhooks.global", "1");
    settings.setProperty("sonar.webhooks.global.1.name", "First");
    settings.setProperty("sonar.webhooks.global.1.url", "http://url1");

    assertThat(underTest.isEnabled(settings.asConfig())).isTrue();
  }

  @Test
  public void isEnabled_returns_false_if_only_one_global_webhook_without_url() {
    settings.setProperty("sonar.webhooks.global", "1");
    settings.setProperty("sonar.webhooks.global.1.name", "First");

    assertThat(underTest.isEnabled(settings.asConfig())).isFalse();
  }

  @Test
  public void isEnabled_returns_false_if_only_one_global_webhook_without_name() {
    settings.setProperty("sonar.webhooks.global", "1");
    settings.setProperty("sonar.webhooks.global.1.url", "http://url1");

    assertThat(underTest.isEnabled(settings.asConfig())).isFalse();
  }

  @Test
  public void isEnabled_returns_true_if_one_valid_project_webhook() {
    settings.setProperty("sonar.webhooks.project", "1");
    settings.setProperty("sonar.webhooks.project.1.name", "First");
    settings.setProperty("sonar.webhooks.project.1.url", "http://url1");

    assertThat(underTest.isEnabled(settings.asConfig())).isTrue();
  }

  @Test
  public void isEnabled_returns_false_if_only_one_project_webhook_without_url() {
    settings.setProperty("sonar.webhooks.project", "1");
    settings.setProperty("sonar.webhooks.project.1.name", "First");

    assertThat(underTest.isEnabled(settings.asConfig())).isFalse();
  }

  @Test
  public void isEnabled_returns_false_if_only_one_project_webhook_without_name() {
    settings.setProperty("sonar.webhooks.project", "1");
    settings.setProperty("sonar.webhooks.project.1.url", "http://url1");

    assertThat(underTest.isEnabled(settings.asConfig())).isFalse();
  }

  @Test
  public void do_nothing_if_no_webhooks() {
    underTest.sendProjectAnalysisUpdate(settings.asConfig(), new WebHooks.Analysis(PROJECT_UUID, "1", "#1"), () -> mock);

    assertThat(caller.countSent()).isEqualTo(0);
    assertThat(logTester.logs(LoggerLevel.DEBUG)).isEmpty();
    verifyZeroInteractions(deliveryStorage);
  }

  @Test
  public void send_global_webhooks() {
    settings.setProperty("sonar.webhooks.global", "1,2");
    settings.setProperty("sonar.webhooks.global.1.name", "First");
    settings.setProperty("sonar.webhooks.global.1.url", "http://url1");
    settings.setProperty("sonar.webhooks.global.2.name", "Second");
    settings.setProperty("sonar.webhooks.global.2.url", "http://url2");
    caller.enqueueSuccess(NOW, 200, 1_234);
    caller.enqueueFailure(NOW, new IOException("Fail to connect"));

    underTest.sendProjectAnalysisUpdate(settings.asConfig(), new WebHooks.Analysis(PROJECT_UUID, "1", "#1"), () -> mock);

    assertThat(caller.countSent()).isEqualTo(2);
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("Sent webhook 'First' | url=http://url1 | time=1234ms | status=200");
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("Failed to send webhook 'Second' | url=http://url2 | message=Fail to connect");
    verify(deliveryStorage, times(2)).persist(any(WebhookDelivery.class));
    verify(deliveryStorage).purge(PROJECT_UUID);
  }

  @Test
  public void send_project_webhooks() {
    settings.setProperty("sonar.webhooks.project", "1");
    settings.setProperty("sonar.webhooks.project.1.name", "First");
    settings.setProperty("sonar.webhooks.project.1.url", "http://url1");
    caller.enqueueSuccess(NOW, 200, 1_234);

    underTest.sendProjectAnalysisUpdate(settings.asConfig(), new WebHooks.Analysis(PROJECT_UUID, "1", "#1"), () -> mock);

    assertThat(caller.countSent()).isEqualTo(1);
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("Sent webhook 'First' | url=http://url1 | time=1234ms | status=200");
    verify(deliveryStorage).persist(any(WebhookDelivery.class));
    verify(deliveryStorage).purge(PROJECT_UUID);
  }

  @Test
  public void process_only_the_10_first_global_webhooks() {
    testMaxWebhooks("sonar.webhooks.global");
  }

  @Test
  public void process_only_the_10_first_project_webhooks() {
    testMaxWebhooks("sonar.webhooks.project");
  }

  private void testMaxWebhooks(String property) {
    IntStream.range(1, 15)
      .forEach(i -> {
        settings.setProperty(property + "." + i + ".name", "First");
        settings.setProperty(property + "." + i + ".url", "http://url");
        caller.enqueueSuccess(NOW, 200, 1_234);
      });
    settings.setProperty(property, IntStream.range(1, 15).mapToObj(String::valueOf).collect(Collectors.joining(",")));

    underTest.sendProjectAnalysisUpdate(settings.asConfig(), new WebHooks.Analysis(PROJECT_UUID, "1", "#1"), () -> mock);

    assertThat(caller.countSent()).isEqualTo(10);
    assertThat(logTester.logs(LoggerLevel.DEBUG).stream().filter(log -> log.contains("Sent"))).hasSize(10);
  }

}
