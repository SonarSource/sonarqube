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
package org.sonar.server.measure.ws;

import java.util.Map;
import org.sonar.core.util.Protobuf;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonarqube.ws.Measures;
import org.sonarqube.ws.Measures.Component;

class ComponentDtoToWsComponent {
  private ComponentDtoToWsComponent() {
    // static methods only
  }

  static Component.Builder componentDtoToWsComponent(ComponentDto component, Map<MetricDto, LiveMeasureDto> measuresByMetric,
    Map<String, ComponentDto> referenceComponentsByUuid) {
    Component.Builder wsComponent = componentDtoToWsComponent(component);

    ComponentDto referenceComponent = referenceComponentsByUuid.get(component.getCopyResourceUuid());
    if (referenceComponent != null) {
      wsComponent.setRefId(referenceComponent.uuid());
      wsComponent.setRefKey(referenceComponent.getDbKey());
    }

    Measures.Measure.Builder measureBuilder = Measures.Measure.newBuilder();
    for (Map.Entry<MetricDto, LiveMeasureDto> entry : measuresByMetric.entrySet()) {
      MeasureDtoToWsMeasure.updateMeasureBuilder(measureBuilder, entry.getKey(), entry.getValue());
      wsComponent.addMeasures(measureBuilder);
      measureBuilder.clear();
    }

    return wsComponent;
  }

  static Component.Builder componentDtoToWsComponent(ComponentDto component) {
    Component.Builder wsComponent = Component.newBuilder()
      .setId(component.uuid())
      .setKey(component.getKey())
      .setName(component.name())
      .setQualifier(component.qualifier());
    Protobuf.setNullable(component.getBranch(), wsComponent::setBranch);
    Protobuf.setNullable(component.path(), wsComponent::setPath);
    Protobuf.setNullable(component.description(), wsComponent::setDescription);
    Protobuf.setNullable(component.language(), wsComponent::setLanguage);
    return wsComponent;
  }
}
