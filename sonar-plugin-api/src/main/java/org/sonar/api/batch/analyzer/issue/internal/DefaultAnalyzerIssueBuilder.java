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
package org.sonar.api.batch.analyzer.issue.internal;

import com.google.common.base.Preconditions;
import org.sonar.api.batch.analyzer.issue.AnalyzerIssue;
import org.sonar.api.batch.analyzer.issue.AnalyzerIssueBuilder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.rule.RuleKey;

import javax.annotation.Nullable;

public class DefaultAnalyzerIssueBuilder implements AnalyzerIssueBuilder {

  String key;
  Boolean onProject = null;
  InputFile file;
  RuleKey ruleKey;
  String message;
  Integer line;
  Double effortToFix;

  @Override
  public DefaultAnalyzerIssueBuilder ruleKey(RuleKey ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  @Override
  public DefaultAnalyzerIssueBuilder onFile(InputFile file) {
    onProject(false);
    Preconditions.checkNotNull(file, "InputFile should be non null");
    this.file = file;
    return this;
  }

  @Override
  public DefaultAnalyzerIssueBuilder onProject() {
    onProject(true);
    this.file = null;
    return this;
  }

  private void onProject(boolean isOnProject) {
    Preconditions.checkState(this.onProject == null, "onFile or onProject can be called only once");
    this.onProject = isOnProject;
  }

  @Override
  public DefaultAnalyzerIssueBuilder atLine(int line) {
    this.line = line;
    return this;
  }

  @Override
  public DefaultAnalyzerIssueBuilder effortToFix(@Nullable Double effortToFix) {
    this.effortToFix = effortToFix;
    return this;
  }

  @Override
  public DefaultAnalyzerIssueBuilder message(String message) {
    this.message = message;
    return this;
  }

  /**
   * For testing only.
   */
  public DefaultAnalyzerIssueBuilder withKey(String key) {
    this.key = key;
    return this;
  }

  @Override
  public AnalyzerIssue build() {
    return new DefaultAnalyzerIssue(this);
  }

}
