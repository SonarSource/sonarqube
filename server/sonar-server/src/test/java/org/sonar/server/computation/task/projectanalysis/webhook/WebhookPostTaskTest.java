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

import java.util.Collections;
import java.util.Date;
import java.util.Random;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.ce.posttask.CeTask;
import org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester;
import org.sonar.api.ce.posttask.Project;
import org.sonar.api.config.Configuration;
import org.sonar.server.computation.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.server.webhook.ProjectAnalysis;
import org.sonar.server.webhook.WebHooks;
import org.sonar.server.webhook.WebhookPayload;
import org.sonar.server.webhook.WebhookPayloadFactory;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.newCeTaskBuilder;
import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.newProjectBuilder;
import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.newScannerContextBuilder;

public class WebhookPostTaskTest {

  private final Configuration configuration = mock(Configuration.class);
  private final WebhookPayload webhookPayload = mock(WebhookPayload.class);
  private final WebhookPayloadFactory payloadFactory = mock(WebhookPayloadFactory.class);
  private final WebHooks webHooks = mock(WebHooks.class);
  private final ConfigurationRepository configurationRepository = mock(ConfigurationRepository.class);
  private WebhookPostTask underTest = new WebhookPostTask(configurationRepository, payloadFactory, webHooks);

  @Before
  public void wireMocks() throws Exception {
    when(payloadFactory.create(any(ProjectAnalysis.class))).thenReturn(webhookPayload);
    when(configurationRepository.getConfiguration()).thenReturn(configuration);
  }

  @Test
  public void call_webhooks() {
    Project project = newProjectBuilder()
      .setUuid(randomAlphanumeric(3))
      .setKey(randomAlphanumeric(4))
      .setName(randomAlphanumeric(5))
      .build();
    CeTask ceTask = newCeTaskBuilder()
      .setStatus(CeTask.Status.values()[new Random().nextInt(CeTask.Status.values().length)])
      .setId(randomAlphanumeric(6))
      .build();
    Date date = new Date();

    PostProjectAnalysisTaskTester.of(underTest)
      .at(date)
      .withCeTask(ceTask)
      .withProject(project)
      .withScannerContext(newScannerContextBuilder().build())
      .execute();

    ArgumentCaptor<Supplier> supplierCaptor = ArgumentCaptor.forClass(Supplier.class);
    verify(webHooks)
      .sendProjectAnalysisUpdate(
        same(configuration),
        eq(new WebHooks.Analysis(project.getUuid(),
          "",
          ceTask.getId())),
        supplierCaptor.capture());

    assertThat(supplierCaptor.getValue().get()).isSameAs(webhookPayload);

    verify(payloadFactory).create(new ProjectAnalysis(
      new org.sonar.server.webhook.CeTask(ceTask.getId(),
        org.sonar.server.webhook.CeTask.Status.valueOf(ceTask.getStatus().name())),
      new org.sonar.server.webhook.Project(project.getUuid(), project.getKey(), project.getName()),
      null,
      null,
      date.getTime(),
      Collections.emptyMap()));
  }

}
