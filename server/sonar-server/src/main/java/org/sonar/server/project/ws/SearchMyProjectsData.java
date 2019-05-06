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
package org.sonar.server.project.ws;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectLinkDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.LiveMeasureDto;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

class SearchMyProjectsData {
  private final List<ComponentDto> projects;
  private final ListMultimap<String, ProjectLinkDto> projectLinksByProjectUuid;
  private final Map<String, SnapshotDto> snapshotsByComponentUuid;
  private final Map<String, String> qualityGateStatuses;
  private final int totalNbOfProject;

  private SearchMyProjectsData(Builder builder) {
    this.projects = copyOf(builder.projects);
    this.projectLinksByProjectUuid = buildProjectLinks(builder.projectLinks);
    this.snapshotsByComponentUuid =builder.snapshots.stream().collect(uniqueIndex(SnapshotDto::getComponentUuid, identity()));
    this.qualityGateStatuses = buildQualityGateStatuses(builder.qualityGates);
    this.totalNbOfProject = builder.totalNbOfProjects;
  }

  static Builder builder() {
    return new Builder();
  }

  List<ComponentDto> projects() {
    return projects;
  }

  List<ProjectLinkDto> projectLinksFor(String projectUuid) {
    return projectLinksByProjectUuid.get(projectUuid);
  }

  Optional<SnapshotDto> lastSnapshot(String componentUuid) {
    return Optional.ofNullable(snapshotsByComponentUuid.get(componentUuid));
  }

  Optional<String> qualityGateStatusFor(String componentUuid) {
    return Optional.ofNullable(qualityGateStatuses.get(componentUuid));
  }

  int totalNbOfProjects() {
    return totalNbOfProject;
  }

  private static ListMultimap<String, ProjectLinkDto> buildProjectLinks(List<ProjectLinkDto> dtos) {
    ImmutableListMultimap.Builder<String, ProjectLinkDto> projectLinks = ImmutableListMultimap.builder();
    dtos.forEach(projectLink -> projectLinks.put(projectLink.getProjectUuid(), projectLink));
    return projectLinks.build();
  }

  private static Map<String, String> buildQualityGateStatuses(List<LiveMeasureDto> measures) {
    return ImmutableMap.copyOf(measures.stream()
      .collect(Collectors.toMap(LiveMeasureDto::getComponentUuid, LiveMeasureDto::getDataAsString)));
  }

  static class Builder {
    private List<ComponentDto> projects;
    private List<ProjectLinkDto> projectLinks;
    private List<SnapshotDto> snapshots;
    private List<LiveMeasureDto> qualityGates;
    private Integer totalNbOfProjects;

    private Builder() {
      // enforce method constructor
    }

    Builder setProjects(List<ComponentDto> projects) {
      this.projects = projects;
      return this;
    }

    public Builder setProjectLinks(List<ProjectLinkDto> projectLinks) {
      this.projectLinks = projectLinks;
      return this;
    }

    public Builder setSnapshots(List<SnapshotDto> snapshots) {
      this.snapshots = snapshots;
      return this;
    }

    public Builder setQualityGates(List<LiveMeasureDto> qGateStatuses) {
      this.qualityGates = qGateStatuses;
      return this;
    }

    public Builder setTotalNbOfProjects(Integer totalNbOfProjects) {
      this.totalNbOfProjects = totalNbOfProjects;
      return this;
    }

    SearchMyProjectsData build() {
      requireNonNull(projects);
      requireNonNull(projectLinks);
      requireNonNull(snapshots);
      requireNonNull(qualityGates);
      requireNonNull(totalNbOfProjects);

      return new SearchMyProjectsData(this);
    }
  }
}
