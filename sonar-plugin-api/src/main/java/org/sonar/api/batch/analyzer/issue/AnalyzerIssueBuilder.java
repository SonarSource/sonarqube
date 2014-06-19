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
package org.sonar.api.batch.analyzer.issue;

import com.google.common.annotations.Beta;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.rule.RuleKey;

import javax.annotation.Nullable;

/**
 * Builder for {@link AnalyzerIssue}.
 *
 * @since 4.4
 */
@Beta
public interface AnalyzerIssueBuilder {

  /**
   * The {@link RuleKey} of the issue.
   */
  AnalyzerIssueBuilder ruleKey(RuleKey ruleKey);

  /**
   * The {@link InputFile} the issue belongs to. For global issues call {@link #onProject()}.
   */
  AnalyzerIssueBuilder onFile(InputFile file);

  /**
   * Tell that the issue is global to the project.
   */
  AnalyzerIssueBuilder onProject();

  /**
   * Line of the issue.
   */
  AnalyzerIssueBuilder atLine(int line);

  /**
   * Effort to fix for the issue.
   */
  AnalyzerIssueBuilder effortToFix(@Nullable Double effortToFix);

  /**
   * Message of the issue.
   */
  AnalyzerIssueBuilder message(String message);

  /**
   * Build the issue.
   */
  AnalyzerIssue build();

}
