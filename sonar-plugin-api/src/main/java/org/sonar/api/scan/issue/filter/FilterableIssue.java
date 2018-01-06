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
package org.sonar.api.scan.issue.filter;

import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;
import org.sonar.api.rule.RuleKey;

/**
 * @since 5.3
 */
@ThreadSafe
public interface FilterableIssue {

  String componentKey();

  RuleKey ruleKey();

  String severity();

  String message();

  @CheckForNull
  Integer line();

  /**
   * @deprecated since 5.5 use {@link #gap()}
   */
  @Deprecated
  Double effortToFix();

  /**
   * @since 5.5
   */
  @CheckForNull
  Double gap();

  /**
   * @deprecated since 6.6 useless
   */
  @Deprecated
  Date creationDate();

  String projectKey();
}
