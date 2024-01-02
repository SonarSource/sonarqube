/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import javax.annotation.Nullable;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonarqube.ws.Hotspots;

import static java.util.Optional.ofNullable;

public class HotspotWsResponseFormatter {

  public HotspotWsResponseFormatter() {
    // nothing to do here
  }

  Hotspots.Component formatComponent(Hotspots.Component.Builder builder, ComponentDto component, @Nullable String branch, @Nullable String pullRequest) {
    builder
      .clear()
      .setKey(component.getKey())
      .setQualifier(component.qualifier())
      .setName(component.name())
      .setLongName(component.longName());
    ofNullable(branch).ifPresent(builder::setBranch);
    ofNullable(pullRequest).ifPresent(builder::setPullRequest);
    ofNullable(component.path()).ifPresent(builder::setPath);
    return builder.build();
  }

  Hotspots.Component formatComponent(Hotspots.Component.Builder builder, ComponentDto component, @Nullable BranchDto branchDto) {
    if (branchDto == null || branchDto.isMain()) {
      return formatComponent(builder, component, null, null);
    }
    return formatComponent(builder, component, branchDto.getBranchKey(), branchDto.getPullRequestKey());
  }

}
