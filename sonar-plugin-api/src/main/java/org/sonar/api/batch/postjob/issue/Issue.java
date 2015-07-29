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
package org.sonar.api.batch.postjob.issue;

import com.google.common.annotations.Beta;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.rule.RuleKey;

/**
 * Represents an issue state at the end of the batch analysis. Only available after local issue tracking in preview mode.
 *
 * @since 5.2
 */
@Beta
public interface Issue {

  /**
   * Key of the issue.
   */
  String key();

  /**
   * The {@link RuleKey} of this issue.
   */
  RuleKey ruleKey();

  /**
   * Component key like foo:src/Foo.php
   */
  String componentKey();

  /**
   * The {@link InputComponent} this issue belongs to. Returns null if component was deleted (for resolved issues).
   */
  @CheckForNull
  InputComponent inputComponent();

  /**
   * Line of the issue. Null for global issues and issues on directories. Can also be null
   * for files (issue global to the file).
   */
  @CheckForNull
  Integer line();

  /**
   * Effort to fix the issue. Used by technical debt model.
   */
  @CheckForNull
  Double effortToFix();

  /**
   * Message of the issue.
   */
  @CheckForNull
  String message();

  /**
   * Severity.
   */
  Severity severity();

  /**
   * If the issue a new one.
   */
  boolean isNew();

}
