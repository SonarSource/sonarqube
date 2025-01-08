/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.measure.ws;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonarqube.ws.Measures;
import org.sonarqube.ws.Measures.Component;

import static java.util.Optional.ofNullable;
import static org.sonar.server.measure.ws.ComponentResponseCommon.addMeasureIncludingRenamedMetric;

class ComponentDtoToWsComponent {
  private ComponentDtoToWsComponent() {
    // static methods only
  }

  static Component.Builder componentDtoToWsComponent(ComponentDto component, @Nullable MeasureDto measureDto,
    Map<String, ComponentDto> referenceComponentsByUuid, @Nullable String branch, @Nullable String pullRequest,
    Collection<MetricDto> metrics, Collection<String> requestedMetrics) {
    Component.Builder wsComponent = componentDtoToWsComponent(component, branch, pullRequest);

    ComponentDto referenceComponent = referenceComponentsByUuid.get(component.getCopyComponentUuid());
    if (referenceComponent != null) {
      wsComponent.setRefKey(referenceComponent.getKey());
    }

    if (measureDto != null) {
      Measures.Measure.Builder measureBuilder = Measures.Measure.newBuilder();
      Map<String, MetricDto> metricsByKey = metrics.stream().collect(Collectors.toMap(MetricDto::getKey, Function.identity()));
      metricsByKey.keySet().stream()
        .filter(metricKey -> measureDto.getMetricValues().containsKey(metricKey))
        .forEach(metricKey -> {
          MeasureDtoToWsMeasure.updateMeasureBuilder(measureBuilder, metricsByKey.get(metricKey), measureDto);
          addMeasureIncludingRenamedMetric(requestedMetrics, wsComponent, measureBuilder);
          measureBuilder.clear();
        });
    }

    return wsComponent;
  }

  static Component.Builder componentDtoToWsComponent(ComponentDto component, @Nullable String branch, @Nullable String pullRequest) {
    Component.Builder wsComponent = Component.newBuilder()
      .setKey(ComponentDto.removeBranchAndPullRequestFromKey(component.getKey()))
      .setName(component.name())
      .setQualifier(component.qualifier());
    ofNullable(branch).ifPresent(wsComponent::setBranch);
    ofNullable(pullRequest).ifPresent(wsComponent::setPullRequest);
    ofNullable(component.path()).ifPresent(wsComponent::setPath);
    ofNullable(component.description()).ifPresent(wsComponent::setDescription);
    ofNullable(component.language()).ifPresent(wsComponent::setLanguage);
    return wsComponent;
  }
}
