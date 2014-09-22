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
import org.sonar.api.batch.sensor.issue.IssueBuilder;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;

import javax.annotation.Nullable;

public class DefaultIssueBuilder implements IssueBuilder {

  private static final String INPUT_DIR_SHOULD_BE_NON_NULL = "InputDir should be non null";
  private static final String INPUT_FILE_SHOULD_BE_NON_NULL = "InputFile should be non null";
  private static final String ON_FILE_OR_ON_DIR_ALREADY_CALLED = "onFile or onDir already called";
  private static final String ON_PROJECT_ALREADY_CALLED = "onProject already called";
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
    Preconditions.checkState(!this.onProject, ON_PROJECT_ALREADY_CALLED);
    Preconditions.checkState(this.path == null, ON_FILE_OR_ON_DIR_ALREADY_CALLED);
    Preconditions.checkNotNull(file, INPUT_FILE_SHOULD_BE_NON_NULL);
    this.path = file;
    return this;
  }

  @Override
  public DefaultIssueBuilder onDir(InputDir dir) {
    Preconditions.checkState(!this.onProject, ON_PROJECT_ALREADY_CALLED);
    Preconditions.checkState(this.path == null, ON_FILE_OR_ON_DIR_ALREADY_CALLED);
    Preconditions.checkNotNull(dir, INPUT_DIR_SHOULD_BE_NON_NULL);
    this.path = dir;
    return this;
  }

  @Override
  public DefaultIssueBuilder onProject() {
    Preconditions.checkState(!this.onProject, ON_PROJECT_ALREADY_CALLED);
    Preconditions.checkState(this.path == null, ON_FILE_OR_ON_DIR_ALREADY_CALLED);
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
  public DefaultIssue build() {
    DefaultIssue result = new DefaultIssue(this);
    reset();
    return result;
  }

  private void reset() {
    key = null;
    onProject = false;
    path = null;
    ruleKey = null;
    message = null;
    line = null;
    effortToFix = null;
    severity = null;
  }

}
