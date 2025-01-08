/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectexport;

import java.util.Arrays;
import java.util.List;
import org.sonar.ce.task.container.TaskContainer;
import org.sonar.ce.task.projectanalysis.step.AbstractComputationSteps;
import org.sonar.ce.task.projectexport.analysis.ExportAnalysesStep;
import org.sonar.ce.task.projectexport.branches.ExportBranchesStep;
import org.sonar.ce.task.projectexport.component.ExportComponentsStep;
import org.sonar.ce.task.projectexport.file.ExportLineHashesStep;
import org.sonar.ce.task.projectexport.issue.ExportIssuesChangelogStep;
import org.sonar.ce.task.projectexport.issue.ExportIssuesStep;
import org.sonar.ce.task.projectexport.rule.ExportAdHocRulesStep;
import org.sonar.ce.task.projectexport.rule.ExportRuleStep;
import org.sonar.ce.task.projectexport.steps.ExportEventsStep;
import org.sonar.ce.task.projectexport.steps.ExportLinksStep;
import org.sonar.ce.task.projectexport.steps.ExportLiveMeasuresStep;
import org.sonar.ce.task.projectexport.steps.ExportMeasuresStep;
import org.sonar.ce.task.projectexport.steps.ExportMetricsStep;
import org.sonar.ce.task.projectexport.steps.ExportNewCodePeriodsStep;
import org.sonar.ce.task.projectexport.steps.ExportPluginsStep;
import org.sonar.ce.task.projectexport.steps.ExportSettingsStep;
import org.sonar.ce.task.projectexport.steps.LoadProjectStep;
import org.sonar.ce.task.projectexport.steps.PublishDumpStep;
import org.sonar.ce.task.projectexport.steps.WriteMetadataStep;
import org.sonar.ce.task.step.ComputationStep;

public class ProjectExportComputationSteps extends AbstractComputationSteps {
  private static final List<Class<? extends ComputationStep>> STEPS_CLASSES = Arrays.asList(
    LoadProjectStep.class,
    WriteMetadataStep.class,
    ExportComponentsStep.class,
    ExportSettingsStep.class,
    ExportPluginsStep.class,
    ExportBranchesStep.class,
    ExportAnalysesStep.class,
    ExportMeasuresStep.class,
    ExportLiveMeasuresStep.class,
    ExportMetricsStep.class,
    ExportIssuesStep.class,
    ExportIssuesChangelogStep.class,
    ExportRuleStep.class,
    ExportAdHocRulesStep.class,
    ExportLinksStep.class,
    ExportEventsStep.class,
    ExportLineHashesStep.class,
    ExportNewCodePeriodsStep.class,
    PublishDumpStep.class);

  public ProjectExportComputationSteps(TaskContainer container) {
    super(container);
  }

  @Override
  public List<Class<? extends ComputationStep>> orderedStepClasses() {
    return STEPS_CLASSES;
  }

}
