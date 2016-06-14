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

package org.sonar.server.component.ws;

import java.util.Map;
import org.sonar.db.component.ComponentDto;
import org.sonarqube.ws.WsComponents;

import static com.google.common.base.Strings.isNullOrEmpty;

class ComponentDtoToWsComponent {
  private ComponentDtoToWsComponent() {
    // prevent instantiation
  }

  static WsComponents.Component.Builder componentDtoToWsComponent(ComponentDto dto) {
    WsComponents.Component.Builder wsComponent = WsComponents.Component.newBuilder()
      .setId(dto.uuid())
      .setKey(dto.key())
      .setName(dto.name())
      .setQualifier(dto.qualifier());
    if (!isNullOrEmpty(dto.path())) {
      wsComponent.setPath(dto.path());
    }
    if (!isNullOrEmpty(dto.description())) {
      wsComponent.setDescription(dto.description());
    }
    if (!isNullOrEmpty(dto.language())) {
      wsComponent.setLanguage(dto.language());
    }

    return wsComponent;
  }

  static WsComponents.Component.Builder componentDtoToWsComponent(ComponentDto component, Map<String, ComponentDto> referenceComponentsByUuid) {
    WsComponents.Component.Builder wsComponent = componentDtoToWsComponent(component);

    ComponentDto referenceComponent = referenceComponentsByUuid.get(component.getCopyResourceUuid());
    if (referenceComponent != null) {
      wsComponent.setRefId(referenceComponent.uuid());
      wsComponent.setRefKey(referenceComponent.key());
    }

    return wsComponent;
  }
}
