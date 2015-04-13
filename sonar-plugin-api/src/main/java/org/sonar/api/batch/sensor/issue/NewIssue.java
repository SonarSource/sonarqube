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
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.rule.RuleKey;

import javax.annotation.Nullable;

/**
 * Represents an issue detected by a {@link Sensor}.
 *
 * @since 5.1
 */
@Beta
public interface NewIssue {

  /**
   * The {@link RuleKey} of the issue.
   */
  NewIssue forRule(RuleKey ruleKey);

  /**
   * The {@link InputFile} the issue belongs to. For global issues call {@link #onProject()}.
   */
  NewIssue onFile(InputFile file);

  /**
   * The {@link InputDir} the issue belongs to. For global issues call {@link #onProject()}.
   */
  NewIssue onDir(InputDir inputDir);

  /**
   * Tell that the issue is global to the project.
   */
  NewIssue onProject();

  /**
   * Line of the issue. Only available for {@link #onFile(InputFile)} issues. 
   * If no line is specified it means that issue is global to the file.
   */
  NewIssue atLine(int line);

  /**
   * Effort to fix the issue.
   */
  NewIssue effortToFix(@Nullable Double effortToFix);

  /**
   * Message of the issue.
   */
  NewIssue message(String message);

  /**
   * Override severity of the issue.
   * Setting a null value or not calling this method means to use severity configured in quality profile.
   */
  NewIssue overrideSeverity(@Nullable Severity severity);

  /**
   * Save the issue. If rule key is unknown or rule not enabled in the current quality profile then a warning is logged but no exception
   * is thrown.
   */
  void save();

}
