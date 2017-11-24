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
package org.sonar.server.webhook;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.AnalysisPropertyDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.server.qualitygate.changeevent.QGChangeEvent;
import org.sonar.server.qualitygate.changeevent.QGChangeEventListener;
import org.sonar.server.qualitygate.changeevent.Trigger;

public class WebhookQGChangeEventListener implements QGChangeEventListener {
  private final WebHooks webhooks;
  private final WebhookPayloadFactory webhookPayloadFactory;
  private final DbClient dbClient;

  public WebhookQGChangeEventListener(WebHooks webhooks, WebhookPayloadFactory webhookPayloadFactory, DbClient dbClient) {
    this.webhooks = webhooks;
    this.webhookPayloadFactory = webhookPayloadFactory;
    this.dbClient = dbClient;
  }

  @Override
  public void onChanges(Trigger trigger, Collection<QGChangeEvent> changeEvents) {
    if (changeEvents.isEmpty()) {
      return;
    }

    List<QGChangeEvent> branchesWithWebhooks = changeEvents.stream()
      .filter(changeEvent -> webhooks.isEnabled(changeEvent.getProjectConfiguration()))
      .collect(MoreCollectors.toList());
    if (branchesWithWebhooks.isEmpty()) {
      return;
    }

    try (DbSession dbSession = dbClient.openSession(false)) {
      branchesWithWebhooks.forEach(event -> callWebhook(dbSession, event));
    }
  }

  private void callWebhook(DbSession dbSession, QGChangeEvent event) {
    webhooks.sendProjectAnalysisUpdate(
      event.getProjectConfiguration(),
      new WebHooks.Analysis(event.getBranch().getUuid(), event.getAnalysis().getUuid(), null),
      () -> buildWebHookPayload(dbSession, event));
  }

  private WebhookPayload buildWebHookPayload(DbSession dbSession, QGChangeEvent event) {
    ComponentDto branch = event.getProject();
    BranchDto shortBranch = event.getBranch();
    SnapshotDto analysis = event.getAnalysis();
    Map<String, String> analysisProperties = dbClient.analysisPropertiesDao().selectBySnapshotUuid(dbSession, analysis.getUuid())
      .stream()
      .collect(Collectors.toMap(AnalysisPropertyDto::getKey, AnalysisPropertyDto::getValue));
    ProjectAnalysis projectAnalysis = new ProjectAnalysis(
      new Project(branch.getMainBranchProjectUuid(), branch.getKey(), branch.name()),
      null,
      new Analysis(analysis.getUuid(), analysis.getCreatedAt()),
      new Branch(false, shortBranch.getKey(), Branch.Type.SHORT),
      event.getQualityGateSupplier().get().orElse(null),
      null,
      analysisProperties);
    return webhookPayloadFactory.create(projectAnalysis);
  }

}
