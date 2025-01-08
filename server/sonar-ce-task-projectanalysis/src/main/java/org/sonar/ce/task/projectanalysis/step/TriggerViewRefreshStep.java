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
package org.sonar.ce.task.projectanalysis.step;

import java.util.OptionalInt;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.view.TriggerViewRefreshDelegate;
import org.sonar.ce.task.step.ComputationStep;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This step will trigger refresh of Portfolios and Applications that include the current project.
 */
public class TriggerViewRefreshStep implements ComputationStep {


  private final TriggerViewRefreshDelegate[] triggerViewRefreshDelegates;
  private final AnalysisMetadataHolder analysisMetadata;

  /**
   * Constructor used by the ioc container when no implementation of {@link TriggerViewRefreshDelegate} is available
   */
  @Autowired(required = false)
  public TriggerViewRefreshStep(AnalysisMetadataHolder analysisMetadata) {
    this.analysisMetadata = analysisMetadata;
    this.triggerViewRefreshDelegates = new TriggerViewRefreshDelegate[0];
  }

  /**
   * Constructor used by the ioc container when an implementation of {@link TriggerViewRefreshDelegate} is available
   */
  @Autowired(required = false)
  public TriggerViewRefreshStep(AnalysisMetadataHolder analysisMetadata, TriggerViewRefreshDelegate[] triggerViewRefreshDelegates) {
    this.analysisMetadata = analysisMetadata;
    this.triggerViewRefreshDelegates = triggerViewRefreshDelegates;
  }

  @Override
  public String getDescription() {
    return "Trigger refresh of Portfolios and Applications";
  }

  @Override
  public void execute(ComputationStep.Context context) {
    for (TriggerViewRefreshDelegate triggerViewRefreshDelegate : this.triggerViewRefreshDelegates) {
      OptionalInt count = triggerViewRefreshDelegate.triggerFrom(analysisMetadata.getProject());
      count.ifPresent(i -> context.getStatistics().add("refreshes" + triggerViewRefreshDelegate.getQualifier(), i));
    }
  }
}
