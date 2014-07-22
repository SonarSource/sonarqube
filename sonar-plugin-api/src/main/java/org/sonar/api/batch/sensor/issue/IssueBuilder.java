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
package org.sonar.api.batch.sensor.issue;

import com.google.common.annotations.Beta;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;

import javax.annotation.Nullable;

/**
 * Builder for {@link Issue}.
 *
 * @since 4.4
 */
@Beta
public interface IssueBuilder {

  /**
   * The {@link RuleKey} of the issue.
   */
  IssueBuilder ruleKey(RuleKey ruleKey);

  /**
   * The {@link InputFile} the issue belongs to. For global issues call {@link #onProject()}.
   */
  IssueBuilder onFile(InputFile file);

  /**
   * The {@link InputDir} the issue belongs to. For global issues call {@link #onProject()}.
   */
  IssueBuilder onDir(InputDir inputDir);

  /**
   * Tell that the issue is global to the project.
   */
  IssueBuilder onProject();

  /**
   * Line of the issue. Only available for {@link #onFile(InputFile)} issues. If no line is specified then issue is supposed to be global to the file.
   */
  IssueBuilder atLine(int line);

  /**
   * Effort to fix the issue.
   */
  IssueBuilder effortToFix(@Nullable Double effortToFix);

  /**
   * Message of the issue.
   */
  IssueBuilder message(String message);

  /**
   * Severity of the issue. See {@link Severity}.
   * Setting a null value means to use severity configured in quality profile.
   */
  IssueBuilder severity(@Nullable String severity);

  /**
   * Build the issue.
   */
  Issue build();

}
