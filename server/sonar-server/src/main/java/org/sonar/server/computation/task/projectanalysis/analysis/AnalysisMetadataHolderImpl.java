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

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.component.BranchType;
import org.sonar.server.computation.util.InitializedProperty;
import org.sonar.server.qualityprofile.QualityProfile;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class AnalysisMetadataHolderImpl implements MutableAnalysisMetadataHolder {
  private static final String BRANCH_NOT_SET = "Branch has not been set";
  private final InitializedProperty<Boolean> organizationsEnabled = new InitializedProperty<>();
  private final InitializedProperty<Organization> organization = new InitializedProperty<>();
  private final InitializedProperty<String> uuid = new InitializedProperty<>();
  private final InitializedProperty<Long> analysisDate = new InitializedProperty<>();
  private final InitializedProperty<Analysis> baseProjectSnapshot = new InitializedProperty<>();
  private final InitializedProperty<Boolean> crossProjectDuplicationEnabled = new InitializedProperty<>();
  private final InitializedProperty<Branch> branch = new InitializedProperty<>();
  private final InitializedProperty<Project> project = new InitializedProperty<>();
  private final InitializedProperty<Integer> rootComponentRef = new InitializedProperty<>();
  private final InitializedProperty<Map<String, QualityProfile>> qProfilesPerLanguage = new InitializedProperty<>();
  private final InitializedProperty<Map<String, ScannerPlugin>> pluginsByKey = new InitializedProperty<>();

  @Override
  public MutableAnalysisMetadataHolder setOrganizationsEnabled(boolean isOrganizationsEnabled) {
    checkState(!this.organizationsEnabled.isInitialized(), "Organization enabled flag has already been set");
    this.organizationsEnabled.setProperty(isOrganizationsEnabled);
    return this;
  }

  @Override
  public boolean isOrganizationsEnabled() {
    checkState(organizationsEnabled.isInitialized(), "Organizations enabled flag has not been set");
    return organizationsEnabled.getProperty();
  }

  @Override
  public MutableAnalysisMetadataHolder setOrganization(Organization organization) {
    checkState(!this.organization.isInitialized(), "Organization has already been set");
    requireNonNull(organization, "Organization can't be null");
    this.organization.setProperty(organization);
    return this;
  }

  @Override
  public Organization getOrganization() {
    checkState(organization.isInitialized(), "Organization has not been set");
    return organization.getProperty();
  }

  @Override
  public MutableAnalysisMetadataHolder setUuid(String s) {
    checkState(!uuid.isInitialized(), "Analysis uuid has already been set");
    requireNonNull(s, "Analysis uuid can't be null");
    this.uuid.setProperty(s);
    return this;
  }

  @Override
  public String getUuid() {
    checkState(uuid.isInitialized(), "Analysis uuid has not been set");
    return this.uuid.getProperty();
  }

  @Override
  public MutableAnalysisMetadataHolder setAnalysisDate(long date) {
    checkState(!analysisDate.isInitialized(), "Analysis date has already been set");
    this.analysisDate.setProperty(date);
    return this;
  }

  @Override
  public long getAnalysisDate() {
    checkState(analysisDate.isInitialized(), "Analysis date has not been set");
    return this.analysisDate.getProperty();
  }

  @Override
  public boolean hasAnalysisDateBeenSet() {
    return analysisDate.isInitialized();
  }

  @Override
  public boolean isFirstAnalysis() {
    return getBaseAnalysis() == null;
  }

  @Override
  public MutableAnalysisMetadataHolder setBaseAnalysis(@Nullable Analysis baseAnalysis) {
    checkState(!this.baseProjectSnapshot.isInitialized(), "Base project snapshot has already been set");
    this.baseProjectSnapshot.setProperty(baseAnalysis);
    return this;
  }

  @Override
  @CheckForNull
  public Analysis getBaseAnalysis() {
    checkState(baseProjectSnapshot.isInitialized(), "Base project snapshot has not been set");
    return baseProjectSnapshot.getProperty();
  }

  @Override
  public MutableAnalysisMetadataHolder setCrossProjectDuplicationEnabled(boolean isCrossProjectDuplicationEnabled) {
    checkState(!this.crossProjectDuplicationEnabled.isInitialized(), "Cross project duplication flag has already been set");
    this.crossProjectDuplicationEnabled.setProperty(isCrossProjectDuplicationEnabled);
    return this;
  }

  @Override
  public boolean isCrossProjectDuplicationEnabled() {
    checkState(crossProjectDuplicationEnabled.isInitialized(), "Cross project duplication flag has not been set");
    return crossProjectDuplicationEnabled.getProperty();
  }

  @Override
  public MutableAnalysisMetadataHolder setBranch(Branch branch) {
    checkState(!this.branch.isInitialized(), "Branch has already been set");
    this.branch.setProperty(branch);
    return this;
  }

  @Override
  public Branch getBranch() {
    checkState(branch.isInitialized(), BRANCH_NOT_SET);
    return branch.getProperty();
  }

  @Override
  public MutableAnalysisMetadataHolder setProject(Project project) {
    checkState(!this.project.isInitialized(), "Project has already been set");
    this.project.setProperty(project);
    return this;
  }

  @Override
  public Project getProject() {
    checkState(project.isInitialized(), "Project has not been set");
    return project.getProperty();
  }

  @Override
  public MutableAnalysisMetadataHolder setRootComponentRef(int rootComponentRef) {

    checkState(!this.rootComponentRef.isInitialized(), "Root component ref has already been set");
    this.rootComponentRef.setProperty(rootComponentRef);
    return this;
  }

  @Override
  public int getRootComponentRef() {
    checkState(rootComponentRef.isInitialized(), "Root component ref has not been set");
    return rootComponentRef.getProperty();
  }

  @Override
  public MutableAnalysisMetadataHolder setQProfilesByLanguage(Map<String, QualityProfile> qprofilesByLanguage) {
    checkState(!this.qProfilesPerLanguage.isInitialized(), "QProfiles by language has already been set");
    this.qProfilesPerLanguage.setProperty(ImmutableMap.copyOf(qprofilesByLanguage));
    return this;
  }

  @Override
  public Map<String, QualityProfile> getQProfilesByLanguage() {
    checkState(qProfilesPerLanguage.isInitialized(), "QProfiles by language has not been set");
    return qProfilesPerLanguage.getProperty();
  }

  @Override
  public MutableAnalysisMetadataHolder setScannerPluginsByKey(Map<String, ScannerPlugin> pluginsByKey) {
    checkState(!this.pluginsByKey.isInitialized(), "Plugins by key has already been set");
    this.pluginsByKey.setProperty(ImmutableMap.copyOf(pluginsByKey));
    return this;
  }

  @Override
  public Map<String, ScannerPlugin> getScannerPluginsByKey() {
    checkState(pluginsByKey.isInitialized(), "Plugins by key has not been set");
    return pluginsByKey.getProperty();
  }

  public boolean isShortLivingBranch() {
    checkState(this.branch.isInitialized(), BRANCH_NOT_SET);
    Branch prop = branch.getProperty();
    return prop != null && prop.getType() == BranchType.SHORT;
  }

  public boolean isLongLivingBranch() {
    checkState(this.branch.isInitialized(), BRANCH_NOT_SET);
    Branch prop = branch.getProperty();
    return prop != null && prop.getType() == BranchType.LONG;
  }

}
