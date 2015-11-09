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
import org.junit.After;
import org.junit.rules.ExternalResource;
import org.sonar.server.computation.snapshot.Snapshot;

public class AnalysisMetadataHolderRule extends ExternalResource implements AnalysisMetadataHolder {

  private Date analysisDate;

  @CheckForNull
  private Snapshot baseProjectSnapshot;

  private boolean isCrossProjectDuplicationEnabled;

  @CheckForNull
  private String branch;

  private Integer rootComponentRef;

  @After
  public void tearDown() throws Exception {
    analysisDate = null;
    baseProjectSnapshot = null;
    isCrossProjectDuplicationEnabled = false;
    branch = null;
    rootComponentRef = null;
  }

  public AnalysisMetadataHolderRule setAnalysisDate(Date analysisDate) {
    this.analysisDate = analysisDate;
    return this;
  }

  public AnalysisMetadataHolderRule setBaseProjectSnapshot(@Nullable Snapshot baseProjectSnapshot) {
    this.baseProjectSnapshot = baseProjectSnapshot;
    return this;
  }

  public AnalysisMetadataHolderRule setCrossProjectDuplicationEnabled(boolean crossProjectDuplicationEnabled) {
    isCrossProjectDuplicationEnabled = crossProjectDuplicationEnabled;
    return this;
  }

  public AnalysisMetadataHolderRule setBranch(@Nullable String branch) {
    this.branch = branch;
    return this;
  }

  public AnalysisMetadataHolderRule setRootComponentRef(int rootComponentRef) {
    this.rootComponentRef = rootComponentRef;
    return this;
  }

  @Override
  public Date getAnalysisDate() {
    return analysisDate;
  }

  @Override
  public boolean isFirstAnalysis() {
    return baseProjectSnapshot == null;
  }

  @Override
  @CheckForNull
  public Snapshot getBaseProjectSnapshot() {
    return baseProjectSnapshot;
  }

  @Override
  public boolean isCrossProjectDuplicationEnabled() {
    return isCrossProjectDuplicationEnabled;
  }

  @Override
  @CheckForNull
  public String getBranch() {
    return branch;
  }

  @Override
  public int getRootComponentRef() {
    return rootComponentRef;
  }
}
