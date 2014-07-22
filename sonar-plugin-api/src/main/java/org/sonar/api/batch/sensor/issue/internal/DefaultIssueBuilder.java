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
package org.sonar.api.batch.sensor.issue.internal;

import com.google.common.base.Preconditions;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.IssueBuilder;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;

import javax.annotation.Nullable;

public class DefaultIssueBuilder implements IssueBuilder {

  String key;
  boolean onProject = false;
  InputPath path;
  RuleKey ruleKey;
  String message;
  Integer line;
  Double effortToFix;
  String severity;

  @Override
  public DefaultIssueBuilder ruleKey(RuleKey ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  @Override
  public DefaultIssueBuilder onFile(InputFile file) {
    Preconditions.checkState(!this.onProject, "onProject already called");
    Preconditions.checkState(this.path == null, "onFile or onDir already called");
    Preconditions.checkNotNull(file, "InputFile should be non null");
    this.path = file;
    return this;
  }

  @Override
  public DefaultIssueBuilder onDir(InputDir dir) {
    Preconditions.checkState(!this.onProject, "onProject already called");
    Preconditions.checkState(this.path == null, "onFile or onDir already called");
    Preconditions.checkNotNull(dir, "InputDir should be non null");
    this.path = dir;
    return this;
  }

  @Override
  public DefaultIssueBuilder onProject() {
    Preconditions.checkState(!this.onProject, "onProject already called");
    Preconditions.checkState(this.path == null, "onFile or onDir already called");
    this.onProject = true;
    return this;
  }

  @Override
  public DefaultIssueBuilder atLine(int line) {
    Preconditions.checkState(this.path != null && this.path instanceof InputFile, "atLine should be called after onFile");
    this.line = line;
    return this;
  }

  @Override
  public DefaultIssueBuilder effortToFix(@Nullable Double effortToFix) {
    this.effortToFix = effortToFix;
    return this;
  }

  @Override
  public DefaultIssueBuilder message(String message) {
    this.message = message;
    return this;
  }

  @Override
  public IssueBuilder severity(@Nullable String severity) {
    Preconditions.checkState(severity == null || Severity.ALL.contains(severity), "Invalid severity: " + severity);
    this.severity = severity;
    return this;
  }

  /**
   * For testing only.
   */
  public DefaultIssueBuilder withKey(String key) {
    this.key = key;
    return this;
  }

  @Override
  public Issue build() {
    return new DefaultIssue(this);
  }

}
