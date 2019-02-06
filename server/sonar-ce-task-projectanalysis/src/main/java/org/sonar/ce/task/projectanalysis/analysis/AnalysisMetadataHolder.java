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
package org.sonar.ce.task.projectanalysis.analysis;

import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.server.project.Project;
import org.sonar.server.qualityprofile.QualityProfile;

public interface AnalysisMetadataHolder {

  /**
   * @throws IllegalStateException if organizations enabled flag has not been set
   */
  boolean isOrganizationsEnabled();

  /**
   * Returns the organization the analysis belongs to.
   * @throws IllegalStateException if organization has not been set
   */
  Organization getOrganization();

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
   * Tell whether the analysisDate has been set.
   */
  boolean hasAnalysisDateBeenSet();

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
   * Convenience method equivalent to do the check using {@link #getBranch()}
   *
   * @throws IllegalStateException if branch has not been set
   */
  boolean isShortLivingBranch();

  /**
   * Convenience method equivalent to do the check using {@link #getBranch()}
   *
   * @throws IllegalStateException if branch has not been set
   */
  default boolean isSLBorPR() {
    return isShortLivingBranch() || isPullRequest();
  }

  /**
   * Convenience method equivalent to do the check using {@link #getBranch()}
   *
   * @throws IllegalStateException if branch has not been set
   */
  boolean isLongLivingBranch();

  /**
   * Convenience method equivalent to do the check using {@link #getBranch()}
   *
   * @throws IllegalStateException if branch has not been set
   */
  boolean isPullRequest();

  /**
   * @throws IllegalStateException if cross project duplication flag has not been set
   */
  boolean isCrossProjectDuplicationEnabled();

  /**
   * Branch being analyzed. Can be of any type: long or short, main or not.
   *
   * @throws IllegalStateException if branch has not been set
   */
  Branch getBranch();

  /**
   * In a pull request analysis, return the ID of the pull request
   *
   * @throws IllegalStateException if pull request key has not been set
   */
  String getPullRequestKey();

  /**
   * The project as represented by the main branch. It is used to load settings
   * like Quality gates, webhooks and configuration.
   *
   * In case of analysis of main branch, the returned value is the main branch,
   * so its uuid and key are the same in
   * {@link org.sonar.ce.task.projectanalysis.component.TreeRootHolder#getRoot().
   *
   * In case of analysis of non-main branch or pull request, the returned value
   * is the main branch. Its uuid and key are different than
   * {@link org.sonar.ce.task.projectanalysis.component.TreeRootHolder#getRoot().
   *
   * @throws IllegalStateException if project has not been set
   */
  Project getProject();

  /**
   * @throws IllegalStateException if root component ref has not been set
   */
  int getRootComponentRef();

  Map<String, QualityProfile> getQProfilesByLanguage();

  /**
   * Plugins used during the analysis on scanner side
   */
  Map<String, ScannerPlugin> getScannerPluginsByKey();

  /**
   * Scm Revision id of the analysed code
   */
  Optional<String> getScmRevisionId();
}
