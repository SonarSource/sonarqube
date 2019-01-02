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
package org.sonar.server.webhook;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.server.qualitygate.EvaluatedQualityGate;

import static com.google.common.collect.ImmutableMap.copyOf;
import static java.util.Objects.requireNonNull;

public class ProjectAnalysis {
  private final Project project;
  private final CeTask ceTask;
  private final Branch branch;
  private final EvaluatedQualityGate qualityGate;
  private final Long updatedAt;
  private final Map<String, String> properties;
  private final Analysis analysis;

  public ProjectAnalysis(Project project, @Nullable CeTask ceTask, @Nullable Analysis analysis,
    @Nullable Branch branch, @Nullable EvaluatedQualityGate qualityGate, @Nullable Long updatedAt,
    Map<String, String> properties) {
    this.project = requireNonNull(project, "project can't be null");
    this.ceTask = ceTask;
    this.branch = branch;
    this.qualityGate = qualityGate;
    this.updatedAt = updatedAt;
    this.properties = copyOf(requireNonNull(properties, "properties can't be null"));
    this.analysis = analysis;
  }

  public Optional<CeTask> getCeTask() {
    return Optional.ofNullable(ceTask);
  }

  public Project getProject() {
    return project;
  }

  public Optional<Branch> getBranch() {
    return Optional.ofNullable(branch);
  }

  public Optional<EvaluatedQualityGate> getQualityGate() {
    return Optional.ofNullable(qualityGate);
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public Optional<Analysis> getAnalysis() {
    return Optional.ofNullable(analysis);
  }

  public Optional<Long> getUpdatedAt() {
    return Optional.ofNullable(updatedAt);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectAnalysis that = (ProjectAnalysis) o;
    return Objects.equals(project, that.project) &&
      Objects.equals(ceTask, that.ceTask) &&
      Objects.equals(branch, that.branch) &&
      Objects.equals(qualityGate, that.qualityGate) &&
      Objects.equals(updatedAt, that.updatedAt) &&
      Objects.equals(properties, that.properties) &&
      Objects.equals(analysis, that.analysis);
  }

  @Override
  public int hashCode() {
    return Objects.hash(project, ceTask, branch, qualityGate, updatedAt, properties, analysis);
  }

  @Override
  public String toString() {
    return "ProjectAnalysis{" +
      "project=" + project +
      ", ceTask=" + ceTask +
      ", branch=" + branch +
      ", qualityGate=" + qualityGate +
      ", updatedAt=" + updatedAt +
      ", properties=" + properties +
      ", analysis=" + analysis +
      '}';
  }
}
