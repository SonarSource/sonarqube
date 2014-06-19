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
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.rule.RuleKey;

import javax.annotation.Nullable;

import java.io.Serializable;

public class DefaultAnalyzerIssue implements AnalyzerIssue, Serializable {

  private final InputFile inputFile;
  private final RuleKey ruleKey;
  private final String message;
  private final Integer line;
  private final Double effortToFix;

  DefaultAnalyzerIssue(DefaultAnalyzerIssueBuilder builder) {
    Preconditions.checkNotNull(builder.ruleKey, "ruleKey is mandatory on issue");
    this.inputFile = builder.file;
    this.ruleKey = builder.ruleKey;
    this.message = builder.message;
    this.line = builder.line;
    this.effortToFix = builder.effortToFix;
  }

  @Override
  @Nullable
  public InputFile inputFile() {
    return inputFile;
  }

  @Override
  public RuleKey ruleKey() {
    return ruleKey;
  }

  @Override
  public String message() {
    return message;
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

}
