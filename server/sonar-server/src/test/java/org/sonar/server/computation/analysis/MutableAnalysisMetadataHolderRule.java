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
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.sonar.server.computation.snapshot.Snapshot;

public class MutableAnalysisMetadataHolderRule implements MutableAnalysisMetadataHolder, TestRule {

  private AnalysisMetadataHolderImpl delegate = new AnalysisMetadataHolderImpl();

  @Override
  public Statement apply(final Statement statement, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          statement.evaluate();
        } finally {
          delegate = new AnalysisMetadataHolderImpl();
        }
      }
    };
  }

  @Override
  public Date getAnalysisDate() {
    return delegate.getAnalysisDate();
  }

  public MutableAnalysisMetadataHolderRule setAnalysisDate(Date date) {
    delegate.setAnalysisDate(date);
    return this;
  }

  @Override
  public boolean isFirstAnalysis() {
    return delegate.isFirstAnalysis();
  }

  @Override
  public MutableAnalysisMetadataHolderRule setBaseProjectSnapshot(@Nullable Snapshot baseProjectSnapshot) {
    delegate.setBaseProjectSnapshot(baseProjectSnapshot);
    return this;
  }

  @Override
  @CheckForNull
  public Snapshot getBaseProjectSnapshot() {
    return delegate.getBaseProjectSnapshot();
  }

  @Override
  public boolean isCrossProjectDuplicationEnabled() {
    return delegate.isCrossProjectDuplicationEnabled();
  }

  @Override
  public MutableAnalysisMetadataHolderRule setIsCrossProjectDuplicationEnabled(boolean isCrossProjectDuplicationEnabled) {
    delegate.setIsCrossProjectDuplicationEnabled(isCrossProjectDuplicationEnabled);
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
}
