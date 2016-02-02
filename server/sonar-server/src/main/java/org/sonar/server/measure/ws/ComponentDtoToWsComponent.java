/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonarqube.ws.WsMeasures;

import static org.sonar.server.measure.ws.MeasureDtoToWsMeasure.measureDtoToWsMeasure;

class ComponentDtoToWsComponent {
  private ComponentDtoToWsComponent() {
    // static methods only
  }

  static WsMeasures.Component.Builder componentDtoToWsComponent(ComponentDto component, Map<MetricDto, MeasureDto> measuresByMetric,
    Map<Long, ComponentDto> referenceComponentsById) {
    WsMeasures.Component.Builder wsComponent = componentDtoToWsComponent(component);

    ComponentDto referenceComponent = referenceComponentsById.get(component.getCopyResourceId());
    if (!referenceComponentsById.isEmpty() && referenceComponent != null) {
      wsComponent.setRefId(referenceComponent.uuid());
      wsComponent.setRefKey(referenceComponent.key());
    }

    for (Map.Entry<MetricDto, MeasureDto> entry : measuresByMetric.entrySet()) {
      wsComponent.addMeasures(measureDtoToWsMeasure(entry.getKey(), entry.getValue()));
    }

    return wsComponent;
  }

  static WsMeasures.Component.Builder componentDtoToWsComponent(ComponentDto component) {
    WsMeasures.Component.Builder wsComponent = WsMeasures.Component.newBuilder()
      .setId(component.uuid())
      .setKey(component.key())
      .setName(component.name())
      .setQualifier(component.qualifier());
    if (component.path() != null) {
      wsComponent.setPath(component.path());
    }
    if (component.description() != null) {
      wsComponent.setDescription(component.description());
    }
    if (component.language() != null) {
      wsComponent.setLanguage(component.language());
    }

    return wsComponent;
  }
}
