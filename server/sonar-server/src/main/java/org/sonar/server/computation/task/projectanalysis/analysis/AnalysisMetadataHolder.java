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
package org.sonar.server.computation.task.projectanalysis.analysis;

import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.server.computation.task.projectanalysis.qualityprofile.QualityProfile;

public interface AnalysisMetadataHolder {

  /**
   * Returns the UUID generated for this analysis.
   * @throws IllegalStateException if uuid has not been set
   */
  String getUuid();

  /**
   * @throws IllegalStateException if no analysis date has been set
   */
  long getAnalysisDate();

  /**
   * Convenience method equivalent to calling {@link #getBaseAnalysis() == null}
   *
   * @throws IllegalStateException if baseProjectSnapshot has not been set
   */
  boolean isFirstAnalysis();

  /**
   * Return the last analysis of the project.
   * If it's the first analysis, it will return null.
   *
   * @throws IllegalStateException if baseAnalysis has not been set
   */
  @CheckForNull
  Analysis getBaseAnalysis();

  /**
   * @throws IllegalStateException if cross project duplication flag has not been set
   */
  boolean isCrossProjectDuplicationEnabled();

  /**
   * @throws IllegalStateException if branch has not been set
   */
  @CheckForNull
  String getBranch();

  /**
   * @throws IllegalStateException if root component ref has not been set
   */
  int getRootComponentRef();

  Map<String, QualityProfile> getQProfilesByLanguage();

}
