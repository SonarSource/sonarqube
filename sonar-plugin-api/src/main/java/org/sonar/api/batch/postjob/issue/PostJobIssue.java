/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.batch.postjob.issue;

import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.rule.RuleKey;

/**
 * Represents an issue state at the end of the scanner analysis in preview/issues mode.
 *
 * @since 5.2
 */
public interface PostJobIssue {

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
