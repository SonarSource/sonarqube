/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.findbugs;

import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RulePriorityMapper;

public class FindbugsRulePriorityMapper implements RulePriorityMapper<String, String> {

  public RulePriority from(String priority) {
    if (priority.equals("1")) {
      return RulePriority.BLOCKER;
    }
    if (priority.equals("2")) {
      return RulePriority.MAJOR;
    }
    if (priority.equals("3")) {
      return RulePriority.INFO;
    }
    throw new IllegalArgumentException("Priority not supported: " + priority);
  }

  public String to(RulePriority priority) {
    if (priority.equals(RulePriority.BLOCKER) || priority.equals(RulePriority.CRITICAL)) {
      return "1";
    }
    if (priority.equals(RulePriority.MAJOR)) {
      return "2";
    }
    if (priority.equals(RulePriority.INFO) || priority.equals(RulePriority.MINOR)) {
      return "3";
    }
    throw new IllegalArgumentException("Priority not supported: " + priority);
  }

}
