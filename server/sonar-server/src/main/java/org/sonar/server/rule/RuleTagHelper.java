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
package org.sonar.server.rule;

import com.google.common.collect.Sets;
import java.util.Set;
import org.sonar.api.server.rule.RuleTagFormat;
import org.sonar.db.rule.RuleDto;

class RuleTagHelper {

  private RuleTagHelper() {
    // only static stuff
  }

  /**
   * Validates tags and resolves conflicts between user and system tags.
   */
  static boolean applyTags(RuleDto rule, Set<String> tags) {
    for (String tag : tags) {
      RuleTagFormat.validate(tag);
    }

    Set<String> initialTags = rule.getTags();
    final Set<String> systemTags = rule.getSystemTags();
    Set<String> withoutSystemTags = Sets.filter(tags, input -> input != null && !systemTags.contains(input));
    rule.setTags(withoutSystemTags);
    return withoutSystemTags.size() != initialTags.size() || !withoutSystemTags.containsAll(initialTags);
  }
}
