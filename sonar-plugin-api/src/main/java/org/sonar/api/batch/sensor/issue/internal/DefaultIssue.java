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
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.rule.RuleKey;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.UUID;

public class DefaultIssue implements Issue, Serializable {

  private final String key;
  private final InputPath inputPath;
  private final RuleKey ruleKey;
  private String message;
  private final Integer line;
  private final Double effortToFix;
  private String severity;

  DefaultIssue(DefaultIssueBuilder builder) {
    Preconditions.checkNotNull(builder.ruleKey, "ruleKey is mandatory on issue");
    this.inputPath = builder.path;
    this.ruleKey = builder.ruleKey;
    this.message = builder.message;
    this.line = builder.line;
    this.effortToFix = builder.effortToFix;
    this.severity = builder.severity;
    this.key = builder.key == null ? UUID.randomUUID().toString() : builder.key;
    Preconditions.checkState(!Strings.isNullOrEmpty(key), "Fail to generate issue key");
  }

  public String key() {
    return key;
  }

  @Override
  @Nullable
  public InputPath inputPath() {
    return inputPath;
  }

  @Override
  public RuleKey ruleKey() {
    return ruleKey;
  }

  @Override
  public String message() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public Integer line() {
    return line;
  }

  @Override
  @Nullable
  public Double effortToFix() {
    return effortToFix;
  }

  @Override
  @CheckForNull
  public String severity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
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
