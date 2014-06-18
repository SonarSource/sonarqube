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
package org.sonar.batch.api.analyzer.issue;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.batch.api.analyzer.Analyzer;
import org.sonar.batch.api.internal.Preconditions;
import org.sonar.batch.api.rules.RuleKey;

import javax.annotation.Nullable;

/**
 * Issue reported by an {@link Analyzer}
 *
 * @since 4.4
 */
public class AnalyzerIssue {

  private final InputFile inputFile;
  private final RuleKey ruleKey;
  private final String message;
  private final Integer line;
  private final Double effortToFix;

  private AnalyzerIssue(Builder builder) {
    this.inputFile = builder.file;
    this.ruleKey = builder.ruleKey;
    this.message = builder.message;
    this.line = builder.line;
    this.effortToFix = builder.effortToFix;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Nullable
  public InputFile inputFile() {
    return inputFile;
  }

  public RuleKey ruleKey() {
    return ruleKey;
  }

  public String message() {
    return message;
  }

  public Integer line() {
    return line;
  }

  @Nullable
  public Double effortToFix() {
    return effortToFix;
  }

  public static class Builder {

    private Boolean onProject = null;
    private InputFile file;
    private RuleKey ruleKey;
    private String message;
    private Integer line;
    private Double effortToFix;

    public AnalyzerIssue build() {
      return new AnalyzerIssue(this);
    }

    public Builder ruleKey(RuleKey ruleKey) {
      this.ruleKey = ruleKey;
      return this;
    }

    public Builder onFile(InputFile file) {
      Preconditions.checkState(onProject == null, "onFile or onProject can be called only once");
      Preconditions.checkNotNull(file, "InputFile should be non null");
      this.file = file;
      this.onProject = false;
      return this;
    }

    public Builder onProject() {
      Preconditions.checkState(onProject == null, "onFile or onProject can be called only once");
      this.file = null;
      this.onProject = true;
      return this;
    }

    public Builder atLine(int line) {
      this.line = line;
      return this;
    }

    public Builder effortToFix(@Nullable Double effortToFix) {
      this.effortToFix = effortToFix;
      return this;
    }

    public Builder message(String message) {
      this.message = message;
      return this;
    }

  }

}
