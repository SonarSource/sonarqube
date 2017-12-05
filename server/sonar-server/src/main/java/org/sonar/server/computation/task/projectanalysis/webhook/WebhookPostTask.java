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
package org.sonar.server.computation.task.projectanalysis.webhook;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.Metric;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.server.computation.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.server.qualitygate.Condition;
import org.sonar.server.qualitygate.EvaluatedCondition;
import org.sonar.server.qualitygate.EvaluatedQualityGate;
import org.sonar.server.webhook.Analysis;
import org.sonar.server.webhook.Branch;
import org.sonar.server.webhook.CeTask;
import org.sonar.server.webhook.Project;
import org.sonar.server.webhook.WebHooks;
import org.sonar.server.webhook.WebhookPayloadFactory;

public class WebhookPostTask implements PostProjectAnalysisTask {

  private final ConfigurationRepository configRepository;
  private final WebhookPayloadFactory payloadFactory;
  private final WebHooks webHooks;

  public WebhookPostTask(ConfigurationRepository configRepository, WebhookPayloadFactory payloadFactory, WebHooks webHooks) {
    this.configRepository = configRepository;
    this.payloadFactory = payloadFactory;
    this.webHooks = webHooks;
  }

  @Override
  public void finished(ProjectAnalysis analysis) {
    Configuration config = configRepository.getConfiguration();

    webHooks.sendProjectAnalysisUpdate(
      config,
      new WebHooks.Analysis(
        analysis.getProject().getUuid(),
        analysis.getAnalysis().map(org.sonar.api.ce.posttask.Analysis::getAnalysisUuid).orElse(null),
        analysis.getCeTask().getId()),
      () -> payloadFactory.create(convert(analysis)));
  }

  private static org.sonar.server.webhook.ProjectAnalysis convert(ProjectAnalysis projectAnalysis) {
    CeTask ceTask = new CeTask(projectAnalysis.getCeTask().getId(), CeTask.Status.valueOf(projectAnalysis.getCeTask().getStatus().name()));
    Project project = new Project(projectAnalysis.getProject().getUuid(), projectAnalysis.getProject().getKey(), projectAnalysis.getProject().getName());
    Analysis analysis = projectAnalysis.getAnalysis().map(a -> new Analysis(a.getAnalysisUuid(), a.getDate().getTime())).orElse(null);
    Branch branch = projectAnalysis.getBranch().map(b -> new Branch(b.isMain(), b.getName().orElse(null), Branch.Type.valueOf(b.getType().name()))).orElse(null);
    EvaluatedQualityGate qualityGate = Optional.ofNullable(projectAnalysis.getQualityGate())
      .map(qg -> {
        EvaluatedQualityGate.Builder builder = EvaluatedQualityGate.newBuilder();
        Set<Condition> conditions = qg.getConditions().stream()
          .map(q -> {
            Condition condition = new Condition(q.getMetricKey(), Condition.Operator.valueOf(q.getOperator().name()),
              q.getErrorThreshold(), q.getWarningThreshold(), q.isOnLeakPeriod());
            builder.addCondition(condition,
              EvaluatedCondition.EvaluationStatus.valueOf(q.getStatus().name()),
              q.getStatus() == org.sonar.api.ce.posttask.QualityGate.EvaluationStatus.NO_VALUE ? null : q.getValue());
            return condition;
          })
          .collect(MoreCollectors.toSet());
        return builder.setQualityGate(new org.sonar.server.qualitygate.QualityGate(qg.getId(), qg.getName(), conditions))
          .setStatus(Metric.Level.valueOf(qg.getStatus().name()))
          .build();
      })
      .orElse(null);
    Long date = projectAnalysis.getAnalysis().map(a -> a.getDate().getTime()).orElse(null);
    Map<String, String> properties = projectAnalysis.getScannerContext().getProperties();

    return new org.sonar.server.webhook.ProjectAnalysis(project, ceTask, analysis, branch, qualityGate, date, properties);
  }
}
