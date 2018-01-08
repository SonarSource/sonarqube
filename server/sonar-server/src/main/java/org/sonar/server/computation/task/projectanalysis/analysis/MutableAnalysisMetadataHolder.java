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
package org.sonar.server.computation.task.projectanalysis.analysis;

import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.server.qualityprofile.QualityProfile;

public interface MutableAnalysisMetadataHolder extends AnalysisMetadataHolder {

  /**
   * @throws IllegalStateException if organizations enabled flag has already been set
   */
  MutableAnalysisMetadataHolder setOrganizationsEnabled(boolean isOrganizationsEnabled);

  /**
   * @throws IllegalStateException if the organization uuid has already been set
   */
  MutableAnalysisMetadataHolder setOrganization(Organization organization);

  /**
   * @throws IllegalStateException if the analysis uuid has already been set
   */
  MutableAnalysisMetadataHolder setUuid(String uuid);

  /**
   * @throws IllegalStateException if the analysis date has already been set
   */
  MutableAnalysisMetadataHolder setAnalysisDate(long date);

  /**
   * @throws IllegalStateException if baseAnalysis has already been set
   */
  MutableAnalysisMetadataHolder setBaseAnalysis(@Nullable Analysis baseAnalysis);

  /**
   * @throws IllegalStateException if cross project duplication flag has already been set
   */
  MutableAnalysisMetadataHolder setCrossProjectDuplicationEnabled(boolean isCrossProjectDuplicationEnabled);

  /**
   * @throws IllegalStateException if branch has already been set
   */
  MutableAnalysisMetadataHolder setBranch(Branch branch);

  /**
   * @throws IllegalStateException if project has already been set
   */
  MutableAnalysisMetadataHolder setProject(Project project);

  /**
   * @throws IllegalStateException if root component ref has already been set
   */
  MutableAnalysisMetadataHolder setRootComponentRef(int rootComponentRef);

  /**
   * @throws IllegalStateException if QProfile by language has already been set
   */
  MutableAnalysisMetadataHolder setQProfilesByLanguage(Map<String, QualityProfile> qprofilesByLanguage);

  /**
   * @throws IllegalStateException if Plugins by key has already been set
   */
  MutableAnalysisMetadataHolder setScannerPluginsByKey(Map<String, ScannerPlugin> pluginsByKey);
}
