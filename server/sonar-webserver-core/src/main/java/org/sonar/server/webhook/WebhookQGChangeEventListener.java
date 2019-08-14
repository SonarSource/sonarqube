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

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.measures.Metric;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.AnalysisPropertyDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.server.qualitygate.EvaluatedQualityGate;
import org.sonar.server.qualitygate.changeevent.QGChangeEvent;
import org.sonar.server.qualitygate.changeevent.QGChangeEventListener;
import org.sonar.server.webhook.Branch.Type;

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
  public void onIssueChanges(QGChangeEvent qualityGateEvent, Set<ChangedIssue> changedIssues) {

    if (!webhooks.isEnabled(qualityGateEvent.getProject())) {
      return;
    }
    Optional<EvaluatedQualityGate> evaluatedQualityGate = qualityGateEvent.getQualityGateSupplier().get();
    if (isQGStatusUnchanged(qualityGateEvent, evaluatedQualityGate)) {
      return;
    }

    try (DbSession dbSession = dbClient.openSession(false)) {
      callWebhook(dbSession, qualityGateEvent, evaluatedQualityGate.orElse(null));
    }
  }

  private static boolean isQGStatusUnchanged(QGChangeEvent qualityGateEvent, Optional<EvaluatedQualityGate> evaluatedQualityGate) {
    Optional<Metric.Level> previousStatus = qualityGateEvent.getPreviousStatus();
    if (!previousStatus.isPresent() && !evaluatedQualityGate.isPresent()) {
      return true;
    }

    return previousStatus
      .map(previousQGStatus -> evaluatedQualityGate
        .filter(newQualityGate -> newQualityGate.getStatus() == previousQGStatus)
        .isPresent())
      .orElse(false);
  }

  private void callWebhook(DbSession dbSession, QGChangeEvent event, @Nullable EvaluatedQualityGate evaluatedQualityGate) {
    webhooks.sendProjectAnalysisUpdate(
      new WebHooks.Analysis(event.getBranch().getUuid(), event.getAnalysis().getUuid(), null),
      () -> buildWebHookPayload(dbSession, event, evaluatedQualityGate));
  }

  private WebhookPayload buildWebHookPayload(DbSession dbSession, QGChangeEvent event, @Nullable EvaluatedQualityGate evaluatedQualityGate) {
    ComponentDto project = event.getProject();
    BranchDto branch = event.getBranch();
    SnapshotDto analysis = event.getAnalysis();
    Map<String, String> analysisProperties = dbClient.analysisPropertiesDao().selectByAnalysisUuid(dbSession, analysis.getUuid())
      .stream()
      .collect(Collectors.toMap(AnalysisPropertyDto::getKey, AnalysisPropertyDto::getValue));
    String projectUuid = StringUtils.defaultString(project.getMainBranchProjectUuid(), project.projectUuid());
    ProjectAnalysis projectAnalysis = new ProjectAnalysis(
      new Project(projectUuid, project.getKey(), project.name()),
      null,
      new Analysis(analysis.getUuid(), analysis.getCreatedAt(), analysis.getRevision()),
      new Branch(branch.isMain(), branch.getKey(), Type.valueOf(branch.getBranchType().name())),
      evaluatedQualityGate,
      null,
      analysisProperties);
    return webhookPayloadFactory.create(projectAnalysis);
  }

}
