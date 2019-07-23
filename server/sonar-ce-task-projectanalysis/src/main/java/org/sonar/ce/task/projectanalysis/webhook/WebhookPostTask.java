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
package org.sonar.ce.task.projectanalysis.webhook;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.ce.task.projectanalysis.api.posttask.QGToEvaluatedQG;
import org.sonar.server.qualitygate.EvaluatedQualityGate;
import org.sonar.server.webhook.Analysis;
import org.sonar.server.webhook.Branch;
import org.sonar.server.webhook.CeTask;
import org.sonar.server.webhook.Project;
import org.sonar.server.webhook.WebHooks;
import org.sonar.server.webhook.WebhookPayload;
import org.sonar.server.webhook.WebhookPayloadFactory;

public class WebhookPostTask implements PostProjectAnalysisTask {
  private final WebhookPayloadFactory payloadFactory;
  private final WebHooks webHooks;

  public WebhookPostTask(WebhookPayloadFactory payloadFactory, WebHooks webHooks) {
    this.payloadFactory = payloadFactory;
    this.webHooks = webHooks;
  }

  @Override
  public String getDescription() {
    return "Webhooks";
  }

  @Override
  public void finished(Context context) {
    ProjectAnalysis projectAnalysis = context.getProjectAnalysis();
    WebHooks.Analysis analysis = new WebHooks.Analysis(
      projectAnalysis.getProject().getUuid(),
      projectAnalysis.getAnalysis().map(org.sonar.api.ce.posttask.Analysis::getAnalysisUuid).orElse(null),
      projectAnalysis.getCeTask().getId());
    Supplier<WebhookPayload> payloadSupplier = () -> payloadFactory.create(convert(projectAnalysis));

    webHooks.sendProjectAnalysisUpdate(analysis, payloadSupplier, context.getLogStatistics());
  }

  private static org.sonar.server.webhook.ProjectAnalysis convert(ProjectAnalysis projectAnalysis) {
    CeTask ceTask = new CeTask(projectAnalysis.getCeTask().getId(), CeTask.Status.valueOf(projectAnalysis.getCeTask().getStatus().name()));
    Analysis analysis = projectAnalysis.getAnalysis().map(a -> new Analysis(a.getAnalysisUuid(), a.getDate().getTime(), a.getRevision().orElse(null))).orElse(null);
    Branch branch = projectAnalysis.getBranch().map(b -> new Branch(b.isMain(), b.getName().orElse(null), Branch.Type.valueOf(b.getType().name()))).orElse(null);
    EvaluatedQualityGate qualityGate = Optional.ofNullable(projectAnalysis.getQualityGate())
      .map(QGToEvaluatedQG.INSTANCE)
      .orElse(null);
    Long date = projectAnalysis.getAnalysis().map(a -> a.getDate().getTime()).orElse(null);
    Map<String, String> properties = projectAnalysis.getScannerContext().getProperties();

    Project project = new Project(projectAnalysis.getProject().getUuid(), projectAnalysis.getProject().getKey(), projectAnalysis.getProject().getName());
    return new org.sonar.server.webhook.ProjectAnalysis(project, ceTask, analysis, branch, qualityGate, date, properties);
  }
}
