/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

package org.sonar.server.computation.task.projectanalysis.webhook;

import java.util.Date;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.api.ce.posttask.Analysis;
import org.sonar.api.ce.posttask.Branch;
import org.sonar.api.ce.posttask.CeTask;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.Project;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.ce.posttask.ScannerContext;

public class ProjectAnalysis implements PostProjectAnalysisTask.ProjectAnalysis {

  private final Branch branch;
  private final Project project;
  private final CeTask ceTask;
  private final QualityGate qualityGate;
  private final Analysis analysis;
  private final ScannerContext scannerContext;

  ProjectAnalysis(Project project, Branch branch, CeTask ceTask, QualityGate qualityGate, Analysis analysis, ScannerContext scannerContext) {
    this.branch = branch;
    this.project = project;
    this.ceTask = ceTask;
    this.qualityGate = qualityGate;
    this.analysis = analysis;
    this.scannerContext = scannerContext;
  }

  @Override
  public CeTask getCeTask() {
    return ceTask;
  }

  @Override
  public Project getProject() {
    return project;
  }

  @Override
  public Optional<Branch> getBranch() {
    return Optional.ofNullable(branch);
  }

  @CheckForNull
  @Override
  public QualityGate getQualityGate() {
    return qualityGate;
  }

  @Override
  public Date getDate() {
      throw new UnsupportedOperationException();
  }

  @Override
  public Optional<Date> getAnalysisDate() {
    return analysis == null ? Optional.empty() : Optional.ofNullable(analysis.getDate());
  }

  @Override
  public Optional<Analysis> getAnalysis() {
    return Optional.ofNullable(analysis);
  }

  @Override
  public ScannerContext getScannerContext() {
    return scannerContext;
  }
}
