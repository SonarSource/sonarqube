/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.api.ce.posttask;

import com.google.common.annotations.Beta;
import java.util.Date;
import javax.annotation.CheckForNull;
import org.sonar.api.ExtensionPoint;
import org.sonar.api.ce.ComputeEngineSide;

/**
 * Extension point of which any plugin can provide an implementation and will allow them to be notified whenever some
 * analysis report processing ends in the Compute Engine.
 *
 * <p>
 * If more then one implementation of {@link PostProjectAnalysisTask} is found, they will be executed in no specific order.
 * 
 *
 * <p>
 * Class {@link PostProjectAnalysisTaskTester} is provided to write unit tests of implementations of this interface.
 * 
 *
 * @since 5.5
 * @see PostProjectAnalysisTaskTester
 */
@ExtensionPoint
@ComputeEngineSide
@Beta
public interface PostProjectAnalysisTask {
  /**
   * This method is called whenever the processing of a Project analysis has finished, whether successfully or not.
   */
  void finished(ProjectAnalysis analysis);

  /**
   * @since 5.5
   */
  @Beta
  interface ProjectAnalysis {
    /**
     * Details of the Compute Engine task in which the project analysis was run.
     */
    CeTask getCeTask();

    /**
     * Details of the analyzed project.
     */
    Project getProject();

    /**
     * Status and details of the Quality Gate of the project (if any was configured on the project).
     */
    @CheckForNull
    QualityGate getQualityGate();

    /**
     * Date of the analysis.
     * <p>
     * This date is the same as the date of the project analysis report and the snapshot.
     * 
     */
    Date getDate();
  }
}
