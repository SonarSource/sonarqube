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
package org.sonar.server.computation.analysis;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.server.computation.qualityprofile.QualityProfile;
import org.sonar.server.computation.snapshot.Snapshot;
import org.sonar.server.computation.util.InitializedProperty;

import static com.google.common.base.Preconditions.checkState;

public class AnalysisMetadataHolderImpl implements MutableAnalysisMetadataHolder {

  private InitializedProperty<Long> analysisDate = new InitializedProperty<>();

  private InitializedProperty<Snapshot> baseProjectSnapshot = new InitializedProperty<>();

  private InitializedProperty<Boolean> crossProjectDuplicationEnabled = new InitializedProperty<>();

  private InitializedProperty<String> branch = new InitializedProperty<>();

  private InitializedProperty<Integer> rootComponentRef = new InitializedProperty<>();

  private InitializedProperty<Map<String, QualityProfile>> qProfilesPerLanguage = new InitializedProperty<>();

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
  public boolean isFirstAnalysis() {
    return getBaseProjectSnapshot() == null;
  }

  @Override
  public MutableAnalysisMetadataHolder setBaseProjectSnapshot(@Nullable Snapshot baseProjectSnapshot) {
    checkState(!this.baseProjectSnapshot.isInitialized(), "Base project snapshot has already been set");
    this.baseProjectSnapshot.setProperty(baseProjectSnapshot);
    return this;
  }

  @Override
  @CheckForNull
  public Snapshot getBaseProjectSnapshot() {
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
  public MutableAnalysisMetadataHolder setBranch(@Nullable String branch) {
    checkState(!this.branch.isInitialized(), "Branch has already been set");
    this.branch.setProperty(branch);
    return this;
  }

  @Override
  public String getBranch() {
    checkState(branch.isInitialized(), "Branch has not been set");
    return branch.getProperty();
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

}
