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
package org.sonar.api.ce.posttask;

import java.util.Date;
import java.util.Optional;
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
public interface PostProjectAnalysisTask {
  /**
   * This method is called whenever the processing of a Project analysis has finished, whether successfully or not.
   */
  void finished(ProjectAnalysis analysis);

  /**
   * @since 5.5
   */
  interface ProjectAnalysis {
    /**
     * When organizations are enabled in SonarQube, the organization the project belongs to.
     *
     * @since 7.0
     * @return a non empty value when organizations are enabled, otherwise empty
     */
    Optional<Organization> getOrganization();

    /**
     * Details of the Compute Engine task in which the project analysis was run.
     */
    CeTask getCeTask();

    /**
     * Details of the analyzed project.
     */
    Project getProject();

    /**
     * The branch that is being analyzed.
     *
     * @since 6.6
     */
    Optional<Branch> getBranch();

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
     * @deprecated use {@link #getAnalysis().getDate()} instead. When {@link #getAnalysis()} returns
     *             {@link Optional#empty() empty}, the current date will be returned.
     */
    @Deprecated
    Date getDate();

    /**
     * Date of the analysis.
     * <p>
     * This date is the same as the date of the project analysis report and therefore as the analysis in DB. It can be
     * missing when the status of the task is {@link org.sonar.api.ce.posttask.CeTask.Status#FAILED FAILED}.
     * </p>
     * @deprecated use {@link #getAnalysis().getDate()} instead
     */
    @Deprecated
    Optional<Date> getAnalysisDate();

    /**
     * Analysis containing the UUID of the analysis and the date
     *
     * <p>
     * This Analysis can be missing when the status of the task is
     * {@link org.sonar.api.ce.posttask.CeTask.Status#FAILED FAILED}.
     * </p>
     */
    Optional<Analysis> getAnalysis();

    /**
     * Context as defined by scanner through {@link org.sonar.api.batch.sensor.SensorContext#addContextProperty(String, String)}.
     * It does not contain the settings used by scanner.
     *
     * @since 6.1
     */
    ScannerContext getScannerContext();

    /**
     * Revision Id that has been analysed
     */
    String getScmRevisionId();
  }
}
