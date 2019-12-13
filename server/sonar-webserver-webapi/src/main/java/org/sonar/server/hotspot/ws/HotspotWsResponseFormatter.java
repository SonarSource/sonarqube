/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.hotspot.ws;

import org.sonar.db.component.ComponentDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonarqube.ws.Hotspots;

import static java.util.Optional.ofNullable;

public class HotspotWsResponseFormatter {
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public HotspotWsResponseFormatter(DefaultOrganizationProvider defaultOrganizationProvider) {
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  Hotspots.Component formatComponent(Hotspots.Component.Builder builder, ComponentDto component) {
    builder
      .clear()
      .setOrganization(defaultOrganizationProvider.get().getKey())
      .setKey(component.getKey())
      .setQualifier(component.qualifier())
      .setName(component.name())
      .setLongName(component.longName());
    ofNullable(component.getBranch()).ifPresent(builder::setBranch);
    ofNullable(component.getPullRequest()).ifPresent(builder::setPullRequest);
    ofNullable(component.path()).ifPresent(builder::setPath);
    return builder.build();
  }

}
