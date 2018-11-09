/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.rule.RuleKey;

/**
 * @since 5.3
 * @deprecated since 7.6
 */
@ThreadSafe
@Deprecated
public interface FilterableIssue {

  /**
   * @deprecated since 7.6 filtering issue should not depend on the key
   */
  @Deprecated
  String componentKey();

  RuleKey ruleKey();

  String severity();

  String message();

  /**
   * @deprecated since 7.2. Use {@link #textRange()} instead.
   */
  @Deprecated
  @CheckForNull
  Integer line();

  /**
   * @since 7.2 
   */
  @CheckForNull
  TextRange textRange();

  /**
   * @since 5.5
   */
  @CheckForNull
  Double gap();

  /**
   * @deprecated since 6.6 useless since creation date is computed on server side
   */
  @Deprecated
  Date creationDate();

  /**
   * @deprecated since 7.6 filtering issue should not depend on the key
   */
  @Deprecated
  String projectKey();
}
