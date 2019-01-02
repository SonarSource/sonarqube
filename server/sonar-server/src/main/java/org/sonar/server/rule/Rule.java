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
package org.sonar.server.rule;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.debt.DebtRemediationFunction;

import javax.annotation.CheckForNull;

import java.util.Date;
import java.util.List;

/**
 * @since 4.4
 */
public interface Rule {

  RuleKey key();

  String language();

  String name();

  @CheckForNull
  String htmlDescription();

  @CheckForNull
  String markdownDescription();

  String effortToFixDescription();

  /**
   * Default severity when activated on a Quality profile
   *
   * @see org.sonar.api.rule.Severity
   */
  String severity();

  /**
   * @see org.sonar.api.rule.RuleStatus
   */
  RuleStatus status();

  boolean isTemplate();

  @CheckForNull
  RuleKey templateKey();

  /**
   * Tags that can be customized by administrators
   */
  List<String> tags();

  /**
   * Read-only tags defined by plugins
   */
  List<String> systemTags();

  List<RuleParam> params();

  @CheckForNull
  RuleParam param(final String key);

  boolean debtOverloaded();

  @CheckForNull
  DebtRemediationFunction debtRemediationFunction();

  @CheckForNull
  DebtRemediationFunction defaultDebtRemediationFunction();

  Date createdAt();

  Date updatedAt();

  @CheckForNull
  String internalKey();

  @CheckForNull
  String markdownNote();

  @CheckForNull
  String noteLogin();

  @CheckForNull
  Date noteCreatedAt();

  @CheckForNull
  Date noteUpdatedAt();

  boolean isManual();
}
