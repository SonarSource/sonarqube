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
package org.sonar.server.computation.task.projectanalysis.webhook;

import java.io.IOException;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.ce.posttask.CeTask;
import org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.computation.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.server.computation.task.projectanalysis.component.TestSettingsRepository;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.newCeTaskBuilder;
import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.newProjectBuilder;
import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.newScannerContextBuilder;
import static org.sonar.server.computation.task.projectanalysis.component.ReportComponent.DUMB_PROJECT;

public class WebhookPostTaskTest {

  private static final long NOW = 1_500_000_000_000L;
  private static final String PROJECT_UUID = "P1_UUID";

  @Rule
  public LogTester logTester = new LogTester().setLevel(LoggerLevel.DEBUG);

  @Rule
  public TreeRootHolderRule rootHolder = new TreeRootHolderRule().setRoot(DUMB_PROJECT);

  private final MapSettings settings = new MapSettings();
  private final TestWebhookCaller caller = new TestWebhookCaller();
  private final WebhookPayloadFactory payloadFactory = new TestWebhookPayloadFactory();
  private final WebhookDeliveryStorage deliveryStorage = mock(WebhookDeliveryStorage.class);

  @Test
  public void do_nothing_if_no_webhooks() {
    execute();

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

    execute();

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

    execute();

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

    execute();

    assertThat(caller.countSent()).isEqualTo(10);
    assertThat(logTester.logs(LoggerLevel.DEBUG).stream().filter(log -> log.contains("Sent"))).hasSize(10);
  }

  private void execute() {
    ConfigurationRepository settingsRepository = new TestSettingsRepository(settings.asConfig());
    WebhookPostTask task = new WebhookPostTask(rootHolder, settingsRepository, payloadFactory, caller, deliveryStorage);

    PostProjectAnalysisTaskTester.of(task)
      .at(new Date())
      .withCeTask(newCeTaskBuilder()
        .setStatus(CeTask.Status.SUCCESS)
        .setId("#1")
        .build())
      .withProject(newProjectBuilder()
        .setUuid(PROJECT_UUID)
        .setKey("P1")
        .setName("Project One")
        .build())
      .withScannerContext(newScannerContextBuilder().build())
      .execute();
  }
}
