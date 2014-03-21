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

import org.sonar.api.utils.MessageException;

import javax.annotation.Nullable;

class DefaultDebtRemediationFunctions implements DebtRemediationFunctions {

  private final String repoKey, key;

  DefaultDebtRemediationFunctions(String repoKey, String key) {
    this.repoKey = repoKey;
    this.key = key;
  }

  @Override
  public DebtRemediationFunction linear(String factor) {
    return create(DefaultDebtRemediationFunction.Type.LINEAR, factor, null);
  }

  @Override
  public DebtRemediationFunction linearWithOffset(String factor, String offset) {
    return create(DefaultDebtRemediationFunction.Type.LINEAR_OFFSET, factor, offset);
  }

  @Override
  public DebtRemediationFunction constantPerIssue(String offset) {
    return create(DefaultDebtRemediationFunction.Type.CONSTANT_ISSUE, null, offset);
  }

  private DebtRemediationFunction create(DefaultDebtRemediationFunction.Type type, @Nullable String factor, @Nullable String offset) {
    try {
      return DefaultDebtRemediationFunction.create(type, factor, offset);
    } catch (DefaultDebtRemediationFunction.ValidationException e) {
      throw MessageException.of(String.format("The rule '%s:%s' is invalid : %s ", this.repoKey, this.key, e.getMessage()));
    }
  }

}
