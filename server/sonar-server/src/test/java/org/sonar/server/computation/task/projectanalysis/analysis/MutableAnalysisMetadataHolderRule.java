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
import javax.annotation.Nullable;
import org.junit.rules.ExternalResource;
import org.sonar.server.computation.task.projectanalysis.qualityprofile.QualityProfile;

public class MutableAnalysisMetadataHolderRule extends ExternalResource implements MutableAnalysisMetadataHolder {

  private AnalysisMetadataHolderImpl delegate = new AnalysisMetadataHolderImpl();

  @Override
  protected void after() {
    delegate = new AnalysisMetadataHolderImpl();
  }

  @Override
  public String getUuid() {
    return delegate.getUuid();
  }

  public MutableAnalysisMetadataHolderRule setUuid(String s) {
    delegate.setUuid(s);
    return this;
  }

  @Override
  public long getAnalysisDate() {
    return delegate.getAnalysisDate();
  }

  public MutableAnalysisMetadataHolderRule setAnalysisDate(long date) {
    delegate.setAnalysisDate(date);
    return this;
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
  public String getBranch() {
    return delegate.getBranch();
  }

  @Override
  public MutableAnalysisMetadataHolderRule setBranch(@Nullable String branch) {
    delegate.setBranch(branch);
    return this;
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
}
