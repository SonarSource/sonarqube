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
package org.sonar.ce.task.projectanalysis.measure;

import org.sonar.api.ExtensionPoint;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;

/**
 * Extension point that is called during processing of a task
 * by {@link PostMeasuresComputationChecksStep}.
 *
 * It is stateless, the same instance is reused for all tasks.
 * As a consequence Compute Engine task components can't be injected
 * as dependencies.
 */
@ComputeEngineSide
@ExtensionPoint
public interface PostMeasuresComputationCheck {

  /**
   * This method can make the task fail by throwing a {@link RuntimeException}
   */
  void onCheck(Context context);

  interface Context {

    /**
     * Return the project UUID, as returned by {@link AnalysisMetadataHolder#getProject()#getUuid()}.
     *
     * It means that when analyzing a non main-branch, it will be the UUID of the project, not the UUId of the branch/pull-request.
     */
    String getProjectUuid();

    /**
     * Return the ncloc computed for the current analysis
     */
    int getNcloc();

    /**
     * Return the organization
     */
    String getOrganizationUuid();

    /**
     * Return the organization
     */
    String getOrganizationKey();
  }
}
