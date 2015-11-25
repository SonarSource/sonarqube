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
package org.sonar.api.server.rule;

import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtRemediationFunction;
import org.sonar.api.utils.MessageException;

import javax.annotation.Nullable;

/**
 * Factory of {@link org.sonar.api.server.debt.DebtRemediationFunction} that keeps
 * a context of rule for better error messages. Used only when declaring rules.
 *
 * @see org.sonar.api.server.rule.RulesDefinition
 */
class DefaultDebtRemediationFunctions implements RulesDefinition.DebtRemediationFunctions {

  private final String repoKey;
  private final String key;

  DefaultDebtRemediationFunctions(String repoKey, String key) {
    this.repoKey = repoKey;
    this.key = key;
  }

  @Override
  public DebtRemediationFunction linear(String coefficient) {
    return create(DefaultDebtRemediationFunction.Type.LINEAR, coefficient, null);
  }

  @Override
  public DebtRemediationFunction linearWithOffset(String coefficient, String offset) {
    return create(DefaultDebtRemediationFunction.Type.LINEAR_OFFSET, coefficient, offset);
  }

  @Override
  public DebtRemediationFunction constantPerIssue(String offset) {
    return create(DefaultDebtRemediationFunction.Type.CONSTANT_ISSUE, null, offset);
  }

  @Override
  public DebtRemediationFunction create(DebtRemediationFunction.Type type, @Nullable String coefficient, @Nullable String offset) {
    try {
      return new DefaultDebtRemediationFunction(type, coefficient, offset);
    } catch (Exception e) {
      throw MessageException.of(String.format("The rule '%s:%s' is invalid : %s ", this.repoKey, this.key, e.getMessage()));
    }
  }

}
