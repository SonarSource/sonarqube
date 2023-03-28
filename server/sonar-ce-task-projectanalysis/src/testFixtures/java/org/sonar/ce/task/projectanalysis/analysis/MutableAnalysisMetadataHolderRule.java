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

import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.junit.rules.ExternalResource;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.server.project.Project;
import org.sonar.server.qualityprofile.QualityProfile;

import static org.mockito.Mockito.mock;

public class MutableAnalysisMetadataHolderRule extends ExternalResource implements MutableAnalysisMetadataHolder {

  private final PlatformEditionProvider editionProvider = mock(PlatformEditionProvider.class);
  private AnalysisMetadataHolderImpl delegate = new AnalysisMetadataHolderImpl(editionProvider);

  @Override
  protected void after() {
    delegate = new AnalysisMetadataHolderImpl(editionProvider);
  }

  @Override
  public MutableAnalysisMetadataHolderRule setOrganization(Organization organization) {
    delegate.setOrganization(organization);
    return this;
  }

  @Override
  public Organization getOrganization() {
    return delegate.getOrganization();
  }

  public MutableAnalysisMetadataHolderRule setUuid(String s) {
    delegate.setUuid(s);
    return this;
  }

  @Override
  public String getUuid() {
    return delegate.getUuid();
  }

  public MutableAnalysisMetadataHolderRule setAnalysisDate(long date) {
    delegate.setAnalysisDate(date);
    return this;
  }

  @Override
  public long getAnalysisDate() {
    return delegate.getAnalysisDate();
  }

  @Override
  public boolean hasAnalysisDateBeenSet() {
    return delegate.hasAnalysisDateBeenSet();
  }

  @Override
  public boolean isFirstAnalysis() {
    return delegate.isFirstAnalysis();
  }

  @Override
  public MutableAnalysisMetadataHolderRule setBaseAnalysis(@Nullable Analysis baseAnalysis) {
    delegate.setBaseAnalysis(baseAnalysis);
    return this;
  }

  @Override
  @CheckForNull
  public Analysis getBaseAnalysis() {
    return delegate.getBaseAnalysis();
  }

  @Override
  public boolean isCrossProjectDuplicationEnabled() {
    return delegate.isCrossProjectDuplicationEnabled();
  }

  @Override
  public MutableAnalysisMetadataHolderRule setCrossProjectDuplicationEnabled(boolean isCrossProjectDuplicationEnabled) {
    delegate.setCrossProjectDuplicationEnabled(isCrossProjectDuplicationEnabled);
    return this;
  }

  @Override
  public Branch getBranch() {
    return delegate.getBranch();
  }

  @Override
  public MutableAnalysisMetadataHolderRule setBranch(Branch branch) {
    delegate.setBranch(branch);
    return this;
  }

  @Override
  public String getPullRequestKey() {
    return delegate.getPullRequestKey();
  }

  @Override
  public MutableAnalysisMetadataHolder setPullRequestKey(String pullRequestKey) {
    delegate.setPullRequestKey(pullRequestKey);
    return this;
  }

  @Override
  public MutableAnalysisMetadataHolderRule setProject(@Nullable Project project) {
    delegate.setProject(project);
    return this;
  }

  @Override
  public Project getProject() {
    return delegate.getProject();
  }

  @Override
  public MutableAnalysisMetadataHolderRule setRootComponentRef(int rootComponentRef) {
    delegate.setRootComponentRef(rootComponentRef);
    return this;
  }

  @Override
  public int getRootComponentRef() {
    return delegate.getRootComponentRef();
  }

  @Override
  public MutableAnalysisMetadataHolder setQProfilesByLanguage(Map<String, QualityProfile> qprofilesByLanguage) {
    delegate.setQProfilesByLanguage(qprofilesByLanguage);
    return this;
  }

  @Override
  public Map<String, QualityProfile> getQProfilesByLanguage() {
    return delegate.getQProfilesByLanguage();
  }

  @Override
  public MutableAnalysisMetadataHolder setScannerPluginsByKey(Map<String, ScannerPlugin> plugins) {
    delegate.setScannerPluginsByKey(plugins);
    return this;
  }

  @Override
  public Map<String, ScannerPlugin> getScannerPluginsByKey() {
    return delegate.getScannerPluginsByKey();
  }

  @Override
  public MutableAnalysisMetadataHolder setScmRevision(String scmRevisionId) {
    delegate.setScmRevision(scmRevisionId);
    return this;
  }

  @Override public MutableAnalysisMetadataHolder setNewCodeReferenceBranch(String newCodeReferenceBranch) {
    delegate.setNewCodeReferenceBranch(newCodeReferenceBranch);
    return this;
  }

  @Override
  public Optional<String> getScmRevision() {
    return delegate.getScmRevision();
  }

  @Override public Optional<String> getNewCodeReferenceBranch() {
    return delegate.getNewCodeReferenceBranch();
  }

  @Override
  public boolean isBranch() {
    return delegate.isBranch();
  }

  @Override
  public boolean isPullRequest() {
    return delegate.isPullRequest();
  }
}
