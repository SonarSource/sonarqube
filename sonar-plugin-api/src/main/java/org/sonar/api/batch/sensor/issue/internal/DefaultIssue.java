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
import com.google.common.base.Strings;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.sensor.SensorStorage;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.rule.RuleKey;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.UUID;

public class DefaultIssue implements Issue {

  private static final String INPUT_DIR_SHOULD_BE_NON_NULL = "InputDir should be non null";
  private static final String INPUT_FILE_SHOULD_BE_NON_NULL = "InputFile should be non null";
  private static final String ON_FILE_OR_ON_DIR_ALREADY_CALLED = "onFile or onDir already called";
  private static final String ON_PROJECT_ALREADY_CALLED = "onProject already called";
  private String key;
  private boolean onProject = false;
  private InputPath path;
  private RuleKey ruleKey;
  private String message;
  private Integer line;
  private Double effortToFix;
  private Severity overridenSeverity;
  private final SensorStorage storage;

  public DefaultIssue() {
    this.key = UUID.randomUUID().toString();
    this.storage = null;
  }

  public DefaultIssue(SensorStorage storage) {
    this.key = UUID.randomUUID().toString();
    this.storage = storage;
  }

  @Override
  public DefaultIssue ruleKey(RuleKey ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  @Override
  public DefaultIssue onFile(InputFile file) {
    Preconditions.checkState(!this.onProject, ON_PROJECT_ALREADY_CALLED);
    Preconditions.checkState(this.path == null, ON_FILE_OR_ON_DIR_ALREADY_CALLED);
    Preconditions.checkNotNull(file, INPUT_FILE_SHOULD_BE_NON_NULL);
    this.path = file;
    return this;
  }

  @Override
  public DefaultIssue onDir(InputDir dir) {
    Preconditions.checkState(!this.onProject, ON_PROJECT_ALREADY_CALLED);
    Preconditions.checkState(this.path == null, ON_FILE_OR_ON_DIR_ALREADY_CALLED);
    Preconditions.checkNotNull(dir, INPUT_DIR_SHOULD_BE_NON_NULL);
    this.path = dir;
    return this;
  }

  @Override
  public DefaultIssue onProject() {
    Preconditions.checkState(!this.onProject, ON_PROJECT_ALREADY_CALLED);
    Preconditions.checkState(this.path == null, ON_FILE_OR_ON_DIR_ALREADY_CALLED);
    this.onProject = true;
    return this;
  }

  @Override
  public DefaultIssue atLine(int line) {
    Preconditions.checkState(this.path != null && this.path instanceof InputFile, "atLine should be called after onFile");
    this.line = line;
    return this;
  }

  @Override
  public DefaultIssue effortToFix(@Nullable Double effortToFix) {
    this.effortToFix = effortToFix;
    return this;
  }

  @Override
  public DefaultIssue message(String message) {
    this.message = message;
    return this;
  }

  @Override
  public Issue overrideSeverity(@Nullable Severity severity) {
    this.overridenSeverity = severity;
    return this;
  }

  @Override
  public RuleKey ruleKey() {
    return this.ruleKey;
  }

  @CheckForNull
  @Override
  public InputPath inputPath() {
    return this.path;
  }

  @Override
  public Integer line() {
    return this.line;
  }

  @Override
  public String message() {
    return this.message;
  }

  @Override
  public Severity overridenSeverity() {
    return this.overridenSeverity;
  }

  @Override
  public Double effortToFix() {
    return this.effortToFix;
  }

  public String key() {
    return this.key;
  }

  @Override
  public void save() {
    Preconditions.checkNotNull(this.storage, "No persister on this object");
    Preconditions.checkNotNull(this.ruleKey, "ruleKey is mandatory on issue");
    Preconditions.checkState(!Strings.isNullOrEmpty(key), "Fail to generate issue key");

    storage.store(this);
  }

  /**
   * For testing only.
   */
  public DefaultIssue withKey(String key) {
    this.key = key;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DefaultIssue that = (DefaultIssue) o;
    return !key.equals(that.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
