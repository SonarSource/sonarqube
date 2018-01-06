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

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.qualitygate.Condition;
import org.sonar.server.qualitygate.EvaluatedCondition;
import org.sonar.server.qualitygate.EvaluatedQualityGate;

import static java.lang.String.format;
import static org.sonar.core.config.WebhookProperties.ANALYSIS_PROPERTY_PREFIX;

@ComputeEngineSide
public class WebhookPayloadFactoryImpl implements WebhookPayloadFactory {

  private static final String PROPERTY_STATUS = "status";
  private final Server server;
  private final System2 system2;

  public WebhookPayloadFactoryImpl(Server server, System2 system2) {
    this.server = server;
    this.system2 = system2;
  }

  @Override
  public WebhookPayload create(ProjectAnalysis analysis) {
    Writer string = new StringWriter();
    try (JsonWriter writer = JsonWriter.of(string)) {
      writer.beginObject();
      writeServer(writer);
      writeTask(writer, analysis.getCeTask());
      writeDates(writer, analysis, system2);
      writeProject(analysis, writer, analysis.getProject());
      analysis.getBranch().ifPresent(b -> writeBranch(writer, analysis.getProject(), b));
      analysis.getQualityGate().ifPresent(qualityGate -> writeQualityGate(writer, qualityGate));
      writeAnalysisProperties(writer, analysis.getProperties());
      writer.endObject().close();
      return new WebhookPayload(analysis.getProject().getKey(), string.toString());
    }
  }

  private void writeServer(JsonWriter writer) {
    writer.prop("serverUrl", server.getPublicRootUrl());
  }

  private static void writeDates(JsonWriter writer, ProjectAnalysis analysis, System2 system2) {
    analysis.getAnalysis().ifPresent(a -> writer.propDateTime("analysedAt", a.getDate()));
    writer.propDateTime("changedAt", new Date(analysis.getUpdatedAt().orElse(system2.now())));
  }

  private void writeProject(ProjectAnalysis analysis, JsonWriter writer, Project project) {
    writer
      .name("project")
      .beginObject()
      .prop("key", project.getKey())
      .prop("name", analysis.getProject().getName())
      .prop("url", projectUrlOf(project))
      .endObject();
  }

  private static void writeAnalysisProperties(JsonWriter writer, Map<String, String> properties) {
    writer
      .name("properties")
      .beginObject();
    properties.entrySet()
      .stream()
      .filter(prop -> prop.getKey().startsWith(ANALYSIS_PROPERTY_PREFIX))
      .forEach(prop -> writer.prop(prop.getKey(), prop.getValue()));
    writer.endObject();
  }

  private static void writeTask(JsonWriter writer, Optional<CeTask> ceTask) {
    ceTask.ifPresent(ceTask1 -> writer.prop("taskId", ceTask1.getId()));
    writer.prop(PROPERTY_STATUS, ceTask.map(CeTask::getStatus).orElse(CeTask.Status.SUCCESS).toString());
  }

  private void writeBranch(JsonWriter writer, Project project, Branch branch) {
    writer
      .name("branch")
      .beginObject()
      .prop("name", branch.getName().orElse(null))
      .prop("type", branch.getType().name())
      .prop("isMain", branch.isMain())
      .prop("url", branchUrlOf(project, branch))
      .endObject();
  }

  private String projectUrlOf(Project project) {
    return format("%s/dashboard?id=%s", server.getPublicRootUrl(), encode(project.getKey()));
  }

  private String branchUrlOf(Project project, Branch branch) {
    if (branch.getType() == Branch.Type.LONG) {
      if (branch.isMain()) {
        return projectUrlOf(project);
      }
      return format("%s/dashboard?branch=%s&id=%s",
        server.getPublicRootUrl(), encode(branch.getName().orElse("")), encode(project.getKey()));
    } else {
      return format("%s/project/issues?branch=%s&id=%s&resolved=false",
        server.getPublicRootUrl(), encode(branch.getName().orElse("")), encode(project.getKey()));
    }
  }

  private static void writeQualityGate(JsonWriter writer, EvaluatedQualityGate gate) {
    writer
      .name("qualityGate")
      .beginObject()
      .prop("name", gate.getQualityGate().getName())
      .prop(PROPERTY_STATUS, gate.getStatus().toString())
      .name("conditions")
      .beginArray();
    for (EvaluatedCondition evaluatedCondition : gate.getEvaluatedConditions()) {
      Condition condition = evaluatedCondition.getCondition();
      writer
        .beginObject()
        .prop("metric", condition.getMetricKey())
        .prop("operator", condition.getOperator().name());
      evaluatedCondition.getValue().ifPresent(t -> writer.prop("value", t));
      writer
        .prop(PROPERTY_STATUS, evaluatedCondition.getStatus().name())
        .prop("onLeakPeriod", condition.isOnLeakPeriod())
        .prop("errorThreshold", condition.getErrorThreshold().orElse(null))
        .prop("warningThreshold", condition.getWarningThreshold().orElse(null))
        .endObject();
    }
    writer
      .endArray()
      .endObject();
  }

  private static String encode(String toEncode) {
    try {
      return URLEncoder.encode(toEncode, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Encoding not supported", e);
    }
  }
}
