/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.batch.protocol.output.resource.ReportComponent;
import org.sonar.batch.protocol.output.resource.ReportComponents;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.computation.db.AnalysisReportDto;

import javax.annotation.CheckForNull;
import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ComputationContext {

  private final AnalysisReportDto reportDto;
  private final ComponentDto project;
  private final File reportDirectory;
  private Map<Long, ReportComponent> components = new HashMap<>();
  private Date analysisDate;

  public ComputationContext(AnalysisReportDto reportDto, ComponentDto project, File reportDir) {
    this.reportDto = reportDto;
    this.project = project;
    this.reportDirectory = reportDir;
  }

  public AnalysisReportDto getReportDto() {
    return reportDto;
  }

  public ComponentDto getProject() {
    return project;
  }

  public File getReportDirectory() {
    return reportDirectory;
  }

  public void addResources(ReportComponents reportComponents) {
    analysisDate = reportComponents.analysisDate();
    addResource(reportComponents.root());
  }

  @CheckForNull
  public ReportComponent getComponentByBatchId(Long batchId) {
    return components.get(batchId);
  }

  @VisibleForTesting
  Map<Long, ReportComponent> getComponents() {
    return components;
  }

  private void addResource(ReportComponent resource) {
    this.components.put(resource.batchId(), resource);
    for (ReportComponent childResource : resource.children()) {
      addResource(childResource);
    }
  }

  @CheckForNull
  public Date getAnalysisDate() {
    return analysisDate;
  }
}
