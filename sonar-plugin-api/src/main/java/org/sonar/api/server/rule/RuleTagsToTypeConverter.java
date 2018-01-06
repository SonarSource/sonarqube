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
package org.sonar.api.server.rule;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.sonar.api.rules.RuleType;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

/**
 * @see org.sonar.api.server.rule.RulesDefinition.NewRule#setType(RuleType)
 * @since 5.5
 */
class RuleTagsToTypeConverter {

  public static final String TAG_BUG = "bug";
  public static final String TAG_SECURITY = "security";
  static final Set<String> RESERVED_TAGS = unmodifiableSet(new HashSet<>(asList(TAG_BUG, TAG_SECURITY)));


  private RuleTagsToTypeConverter() {
    // only statics
  }

  static RuleType convert(Collection<String> tags) {
    if (tags.contains(TAG_BUG)) {
      return RuleType.BUG;
    }
    if (tags.contains(TAG_SECURITY)) {
      return RuleType.VULNERABILITY;
    }
    return RuleType.CODE_SMELL;
  }
}
