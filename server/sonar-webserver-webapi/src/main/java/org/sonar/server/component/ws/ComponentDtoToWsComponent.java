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
package org.sonar.server.component.ws;

import java.util.Arrays;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.project.Visibility;
import org.sonarqube.ws.Components;

import static com.google.common.base.Strings.emptyToNull;
import static java.util.Optional.ofNullable;
import static org.sonar.api.utils.DateUtils.formatDateTime;

class ComponentDtoToWsComponent {

  /**
   * The concept of "visibility" will only be configured for these qualifiers.
   */
  private static final Set<String> QUALIFIERS_WITH_VISIBILITY = Set.of(Qualifiers.PROJECT, Qualifiers.VIEW, Qualifiers.APP);

  private ComponentDtoToWsComponent() {
    // prevent instantiation
  }

  static Components.Component.Builder projectOrAppToWsComponent(ProjectDto project, @Nullable SnapshotDto lastAnalysis) {
    Components.Component.Builder wsComponent = Components.Component.newBuilder()
      .setKey(project.getKey())
      .setName(project.getName())
      .setQualifier(project.getQualifier());

    ofNullable(emptyToNull(project.getDescription())).ifPresent(wsComponent::setDescription);
    ofNullable(lastAnalysis).ifPresent(
      analysis -> {
        wsComponent.setAnalysisDate(formatDateTime(analysis.getCreatedAt()));
        ofNullable(analysis.getPeriodDate()).ifPresent(leak -> wsComponent.setLeakPeriodDate(formatDateTime(leak)));
        ofNullable(analysis.getProjectVersion()).ifPresent(wsComponent::setVersion);
      });
    if (QUALIFIERS_WITH_VISIBILITY.contains(project.getQualifier())) {
      wsComponent.setVisibility(Visibility.getLabel(project.isPrivate()));
      wsComponent.getTagsBuilder().addAllTags(project.getTags());
    }

    return wsComponent;
  }

  public static Components.Component.Builder componentDtoToWsComponent(ComponentDto dto, @Nullable ProjectDto parentProjectDto,
    @Nullable SnapshotDto lastAnalysis, @Nullable String branch, @Nullable String pullRequest) {
    Components.Component.Builder wsComponent = Components.Component.newBuilder()
      .setKey(ComponentDto.removeBranchAndPullRequestFromKey(dto.getKey()))
      .setName(dto.name())
      .setQualifier(dto.qualifier());
    ofNullable(emptyToNull(branch)).ifPresent(wsComponent::setBranch);
    ofNullable(emptyToNull(pullRequest)).ifPresent(wsComponent::setPullRequest);
    ofNullable(emptyToNull(dto.path())).ifPresent(wsComponent::setPath);
    ofNullable(emptyToNull(dto.description())).ifPresent(wsComponent::setDescription);
    ofNullable(emptyToNull(dto.language())).ifPresent(wsComponent::setLanguage);
    ofNullable(lastAnalysis).ifPresent(
      analysis -> {
        wsComponent.setAnalysisDate(formatDateTime(analysis.getCreatedAt()));
        ofNullable(analysis.getPeriodDate()).ifPresent(leak -> wsComponent.setLeakPeriodDate(formatDateTime(leak)));
        ofNullable(analysis.getProjectVersion()).ifPresent(wsComponent::setVersion);
      });
    if (QUALIFIERS_WITH_VISIBILITY.contains(dto.qualifier())) {
      wsComponent.setVisibility(Visibility.getLabel(dto.isPrivate()));
      if (Arrays.asList(Qualifiers.PROJECT, Qualifiers.APP).contains(dto.qualifier()) && dto.getMainBranchProjectUuid() != null && parentProjectDto != null) {
        wsComponent.getTagsBuilder().addAllTags(parentProjectDto.getTags());
      }
    }
    return wsComponent;
  }
}
