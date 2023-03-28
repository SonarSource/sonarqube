/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.junit.rules.ExternalResource;
import org.sonar.ce.task.util.InitializedProperty;
import org.sonar.db.component.BranchType;
import org.sonar.server.project.Project;
import org.sonar.server.qualityprofile.QualityProfile;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.defaultIfBlank;

public class AnalysisMetadataHolderRule extends ExternalResource implements MutableAnalysisMetadataHolder {

  private final InitializedProperty<Organization> organization = new InitializedProperty<>();
  private final InitializedProperty<String> uuid = new InitializedProperty<>();
  private final InitializedProperty<Long> analysisDate = new InitializedProperty<>();
  private final InitializedProperty<Analysis> baseAnalysis = new InitializedProperty<>();
  private final InitializedProperty<Boolean> crossProjectDuplicationEnabled = new InitializedProperty<>();
  private final InitializedProperty<Branch> branch = new InitializedProperty<>();
  private final InitializedProperty<String> pullRequestId = new InitializedProperty<>();
  private final InitializedProperty<Project> project = new InitializedProperty<>();
  private final InitializedProperty<Integer> rootComponentRef = new InitializedProperty<>();
  private final InitializedProperty<Map<String, QualityProfile>> qProfilesPerLanguage = new InitializedProperty<>();
  private final InitializedProperty<Map<String, ScannerPlugin>> pluginsByKey = new InitializedProperty<>();
  private final InitializedProperty<String> scmRevision = new InitializedProperty<>();
  private final InitializedProperty<String> newCodeReferenceBranch = new InitializedProperty<>();

  @Override
  public AnalysisMetadataHolderRule setOrganization(Organization organization) {
    requireNonNull(organization, "organization can't be null");
    this.organization.setProperty(organization);
    return this;
  }

  @Override
  public Organization getOrganization() {
    checkState(organization.isInitialized(), "Organization has not been set");
    return this.organization.getProperty();
  }

  @Override
  public AnalysisMetadataHolderRule setUuid(String s) {
    checkNotNull(s, "UUID must not be null");
    this.uuid.setProperty(s);
    return this;
  }

  @Override
  public String getUuid() {
    checkState(uuid.isInitialized(), "Analysis UUID has not been set");
    return this.uuid.getProperty();
  }

  public AnalysisMetadataHolderRule setAnalysisDate(Date date) {
    checkNotNull(date, "Date must not be null");
    this.analysisDate.setProperty(date.getTime());
    return this;
  }

  @Override
  public AnalysisMetadataHolderRule setAnalysisDate(long date) {
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
  public AnalysisMetadataHolderRule setBaseAnalysis(@Nullable Analysis baseAnalysis) {
    this.baseAnalysis.setProperty(baseAnalysis);
    return this;
  }

  @Override
  @CheckForNull
  public Analysis getBaseAnalysis() {
    checkState(baseAnalysis.isInitialized(), "Base analysis has not been set");
    return baseAnalysis.getProperty();
  }

  @Override
  public AnalysisMetadataHolderRule setCrossProjectDuplicationEnabled(boolean isCrossProjectDuplicationEnabled) {
    this.crossProjectDuplicationEnabled.setProperty(isCrossProjectDuplicationEnabled);
    return this;
  }

  @Override
  public boolean isCrossProjectDuplicationEnabled() {
    checkState(crossProjectDuplicationEnabled.isInitialized(), "Cross project duplication flag has not been set");
    return crossProjectDuplicationEnabled.getProperty();
  }

  @Override
  public AnalysisMetadataHolderRule setBranch(Branch branch) {
    this.branch.setProperty(branch);
    return this;
  }

  @Override
  public Branch getBranch() {
    checkState(branch.isInitialized(), "Branch has not been set");
    return branch.getProperty();
  }

  @Override
  public MutableAnalysisMetadataHolder setPullRequestKey(String pullRequestKey) {
    this.pullRequestId.setProperty(pullRequestKey);
    return this;
  }

  @Override
  public String getPullRequestKey() {
    checkState(pullRequestId.isInitialized(), "Pull request id has not been set");
    return pullRequestId.getProperty();
  }

  @Override
  public AnalysisMetadataHolderRule setProject(Project p) {
    this.project.setProperty(p);
    return this;
  }

  @Override
  public Project getProject() {
    checkState(project.isInitialized(), "Project has not been set");
    return project.getProperty();
  }

  @Override
  public AnalysisMetadataHolderRule setRootComponentRef(int rootComponentRef) {
    this.rootComponentRef.setProperty(rootComponentRef);
    return this;
  }

  @Override
  public int getRootComponentRef() {
    checkState(rootComponentRef.isInitialized(), "Root component ref has not been set");
    return rootComponentRef.getProperty();
  }

  @Override
  public AnalysisMetadataHolderRule setQProfilesByLanguage(Map<String, QualityProfile> qProfilesPerLanguage) {
    this.qProfilesPerLanguage.setProperty(qProfilesPerLanguage);
    return this;
  }

  @Override
  public Map<String, QualityProfile> getQProfilesByLanguage() {
    checkState(qProfilesPerLanguage.isInitialized(), "QProfile per language has not been set");
    return qProfilesPerLanguage.getProperty();
  }

  @Override
  public AnalysisMetadataHolderRule setScannerPluginsByKey(Map<String, ScannerPlugin> plugins) {
    this.pluginsByKey.setProperty(plugins);
    return this;
  }

  @Override
  public Map<String, ScannerPlugin> getScannerPluginsByKey() {
    checkState(pluginsByKey.isInitialized(), "Plugins per key has not been set");
    return pluginsByKey.getProperty();
  }

  @Override
  public MutableAnalysisMetadataHolder setScmRevision(@Nullable String s) {
    checkState(!this.scmRevision.isInitialized(), "ScmRevisionId has already been set");
    this.scmRevision.setProperty(defaultIfBlank(s, null));
    return this;
  }

  @Override
  public MutableAnalysisMetadataHolder setNewCodeReferenceBranch(String newCodeReferenceBranch) {
    checkState(!this.newCodeReferenceBranch.isInitialized(), "ScmRevisionId has already been set");
    this.newCodeReferenceBranch.setProperty(defaultIfBlank(newCodeReferenceBranch, null));
    return this;
  }

  @Override
  public Optional<String> getScmRevision() {
    if (!scmRevision.isInitialized()) {
      return Optional.empty();
    }
    return Optional.ofNullable(scmRevision.getProperty());
  }

  @Override
  public Optional<String> getNewCodeReferenceBranch() {
    if (!newCodeReferenceBranch.isInitialized()) {
      return Optional.empty();
    }
    return Optional.ofNullable(newCodeReferenceBranch.getProperty());
  }

  @Override
  public boolean isBranch() {
    Branch property = this.branch.getProperty();
    return property != null && property.getType() == BranchType.BRANCH;
  }

  @Override
  public boolean isPullRequest() {
    Branch property = this.branch.getProperty();
    return property != null && property.getType() == BranchType.PULL_REQUEST;
  }
}
