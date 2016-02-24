/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Function;
import javax.annotation.Nonnull;
import org.sonar.api.rule.RuleKey;

public final class RuleKeyFunctions {

  private RuleKeyFunctions() {
    // only static methods
  }

  public static Function<String, RuleKey> stringToRuleKey() {
    return StringToRuleKey.INSTANCE;
  }

  /**
   * Transforms a string representation of key to {@link RuleKey}. It
   * does not accept null string inputs.
   */
  private enum StringToRuleKey implements Function<String, RuleKey> {
    INSTANCE;
    @Nonnull
    @Override
    public RuleKey apply(@Nonnull String input) {
      return RuleKey.parse(input);
    }
  }

}
