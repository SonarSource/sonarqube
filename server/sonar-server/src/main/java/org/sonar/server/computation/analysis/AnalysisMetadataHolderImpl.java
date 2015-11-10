/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.analysis;

import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.server.computation.snapshot.Snapshot;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class AnalysisMetadataHolderImpl implements MutableAnalysisMetadataHolder {

  private InitializedProperty<Long> analysisDate = new InitializedProperty<>();

  private InitializedProperty<Snapshot> baseProjectSnapshot = new InitializedProperty<>();

  private InitializedProperty<Boolean> crossProjectDuplicationEnabled = new InitializedProperty<>();

  private InitializedProperty<String> branch = new InitializedProperty<>();

  private InitializedProperty<Integer> rootComponentRef = new InitializedProperty<>();

  @Override
  public void setAnalysisDate(Date date) {
    checkNotNull(date, "Date must not be null");
    checkState(!analysisDate.isInitialized(), "Analysis date has already been set");
    this.analysisDate.setProperty(date.getTime());
  }

  @Override
  public Date getAnalysisDate() {
    checkState(analysisDate.isInitialized(), "Analysis date has not been set");
    return new Date(this.analysisDate.getProperty());
  }

  @Override
  public boolean isFirstAnalysis() {
    return getBaseProjectSnapshot() == null;
  }

  @Override
  public void setBaseProjectSnapshot(@Nullable Snapshot baseProjectSnapshot) {
    checkState(!this.baseProjectSnapshot.isInitialized(), "Base project snapshot has already been set");
    this.baseProjectSnapshot.setProperty(baseProjectSnapshot);
  }

  @Override
  @CheckForNull
  public Snapshot getBaseProjectSnapshot() {
    checkState(baseProjectSnapshot.isInitialized(), "Base project snapshot has not been set");
    return baseProjectSnapshot.getProperty();
  }

  @Override
  public void setCrossProjectDuplicationEnabled(boolean isCrossProjectDuplicationEnabled) {
    checkState(!this.crossProjectDuplicationEnabled.isInitialized(), "Cross project duplication flag has already been set");
    this.crossProjectDuplicationEnabled.setProperty(isCrossProjectDuplicationEnabled);
  }

  @Override
  public boolean isCrossProjectDuplicationEnabled() {
    checkState(crossProjectDuplicationEnabled.isInitialized(), "Cross project duplication flag has not been set");
    return crossProjectDuplicationEnabled.getProperty();
  }

  @Override
  public void setBranch(@Nullable String branch) {
    checkState(!this.branch.isInitialized(), "Branch has already been set");
    this.branch.setProperty(branch);
  }

  @Override
  public String getBranch() {
    checkState(branch.isInitialized(), "Branch has not been set");
    return branch.getProperty();
  }

  @Override
  public void setRootComponentRef(int rootComponentRef) {
    checkState(!this.rootComponentRef.isInitialized(), "Root component ref has already been set");
    this.rootComponentRef.setProperty(rootComponentRef);
  }

  @Override
  public int getRootComponentRef() {
    checkState(rootComponentRef.isInitialized(), "Root component ref has not been set");
    return rootComponentRef.getProperty();
  }

  private static class InitializedProperty<E> {
    private E property;
    private boolean initialized = false;

    public InitializedProperty setProperty(@Nullable E property) {
      this.property = property;
      this.initialized = true;
      return this;
    }

    @CheckForNull
    public E getProperty() {
      return property;
    }

    public boolean isInitialized() {
      return initialized;
    }
  }

}
