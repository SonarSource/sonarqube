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
package org.sonar.server.qualitygate.changeevent;

import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.Metric;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.server.qualitygate.EvaluatedQualityGate;

import static java.util.Objects.requireNonNull;

@Immutable
public class QGChangeEvent {
  private final ComponentDto project;
  private final BranchDto branch;
  private final SnapshotDto analysis;
  private final Configuration projectConfiguration;
  private final Metric.Level previousStatus;
  private final Supplier<Optional<EvaluatedQualityGate>> qualityGateSupplier;

  public QGChangeEvent(ComponentDto project, BranchDto branch, SnapshotDto analysis, Configuration projectConfiguration,
    @Nullable Metric.Level previousStatus, Supplier<Optional<EvaluatedQualityGate>> qualityGateSupplier) {
    this.project = requireNonNull(project, "project can't be null");
    this.branch = requireNonNull(branch, "branch can't be null");
    this.analysis = requireNonNull(analysis, "analysis can't be null");
    this.projectConfiguration = requireNonNull(projectConfiguration, "projectConfiguration can't be null");
    this.previousStatus = previousStatus;
    this.qualityGateSupplier = requireNonNull(qualityGateSupplier, "qualityGateSupplier can't be null");
  }

  public BranchDto getBranch() {
    return branch;
  }

  public ComponentDto getProject() {
    return project;
  }

  public SnapshotDto getAnalysis() {
    return analysis;
  }

  public Configuration getProjectConfiguration() {
    return projectConfiguration;
  }

  public Optional<Metric.Level> getPreviousStatus() {
    return Optional.ofNullable(previousStatus);
  }

  public Supplier<Optional<EvaluatedQualityGate>> getQualityGateSupplier() {
    return qualityGateSupplier;
  }

  @Override
  public String toString() {
    return "QGChangeEvent{" +
      "project=" + toString(project) +
      ", branch=" + toString(branch) +
      ", analysis=" + toString(analysis) +
      ", projectConfiguration=" + projectConfiguration +
      ", previousStatus=" + previousStatus +
      ", qualityGateSupplier=" + qualityGateSupplier +
      '}';
  }

  private static String toString(ComponentDto project) {
    return project.uuid() + ":" + project.getKey();
  }

  private static String toString(BranchDto branch) {
    return branch.getBranchType() + ":" + branch.getUuid() + ":" + branch.getProjectUuid() + ":" + branch.getMergeBranchUuid();
  }

  private static String toString(SnapshotDto analysis) {
    return analysis.getUuid() + ":" + analysis.getCreatedAt();
  }
}
