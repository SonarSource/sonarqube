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
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.rule.RuleKey;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * Represents an issue detected by a {@link Sensor}.
 *
 * @since 5.0
 */
@Beta
public interface Issue {

  public enum Severity {
    INFO,
    MINOR,
    MAJOR,
    CRITICAL,
    BLOCKER;
  }

  /**
   * The {@link RuleKey} of the issue.
   */
  Issue ruleKey(RuleKey ruleKey);

  /**
   * The {@link RuleKey} of this issue.
   */
  RuleKey ruleKey();

  /**
   * The {@link InputFile} the issue belongs to. For global issues call {@link #onProject()}.
   */
  Issue onFile(InputFile file);

  /**
   * The {@link InputDir} the issue belongs to. For global issues call {@link #onProject()}.
   */
  Issue onDir(InputDir inputDir);

  /**
   * Tell that the issue is global to the project.
   */
  Issue onProject();

  /**
   * The {@link InputPath} this issue belongs to. Returns null if issue is global to the project.
   */
  @CheckForNull
  InputPath inputPath();

  /**
   * Line of the issue. Only available for {@link #onFile(InputFile)} issues. 
   * If no line is specified it means that issue is global to the file.
   */
  Issue atLine(int line);

  /**
   * Line of the issue. Null for global issues and issues on directories. Can also be null
   * for files (issue global to the file).
   */
  @CheckForNull
  Integer line();

  /**
   * Effort to fix the issue.
   */
  Issue effortToFix(@Nullable Double effortToFix);

  /**
   * Effort to fix the issue. Used by technical debt model.
   */
  @CheckForNull
  Double effortToFix();

  /**
   * Message of the issue.
   */
  Issue message(String message);

  /**
   * Message of the issue.
   */
  @CheckForNull
  String message();

  /**
   * Override severity of the issue.
   * Setting a null value or not calling this method means to use severity configured in quality profile.
   */
  Issue overrideSeverity(@Nullable Severity severity);

  /**
   * Overriden severity.
   */
  @CheckForNull
  Severity overridenSeverity();

  /**
   * Save the issue. If rule key is unknow or rule not enabled in the current quality profile then a warning is logged but no exception
   * is thrown.
   */
  void save();

}
