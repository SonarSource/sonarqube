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
package org.sonar.api.rules;

import org.sonar.check.Priority;

/**
 * A class to hold rules priority
 */
public enum RulePriority {

  /**
   * WARNING : DO NOT CHANGE THE ENUMERATION ORDER
   * the enum ordinal is used for db persistence
   */
  INFO, MINOR, MAJOR, CRITICAL, BLOCKER;

  /**
   * A class to map priority level prior to Sonar 1.10 to the new ones
   *
   * @param level an old priority level : Error or Warning
   * @return the corresponding RulePriority
   */
  public static RulePriority valueOfString(String level) {
    try {
      return RulePriority.valueOf(level.toUpperCase());

    } catch (IllegalArgumentException ex) {
      // backward compatibility
      if (level.equalsIgnoreCase("ERROR")) {
        return RulePriority.MAJOR;
      } else if (level.equalsIgnoreCase("WARNING")) {
        return RulePriority.INFO;
      }
    }
    throw new IllegalArgumentException("Unknown priority " + level);
  }


  public static RulePriority fromCheckPriority(Priority checkPriority) {
    if (checkPriority == Priority.BLOCKER) {
      return RulePriority.BLOCKER;
    }
    if (checkPriority == Priority.CRITICAL) {
      return RulePriority.CRITICAL;
    }
    if (checkPriority == Priority.MAJOR) {
      return RulePriority.MAJOR;
    }
    if (checkPriority == Priority.MINOR) {
      return RulePriority.MINOR;
    }
    if (checkPriority == Priority.INFO) {
      return RulePriority.INFO;
    }
    return null;
  }

  public Priority toCheckPriority() {
    if (this == BLOCKER) {
      return Priority.BLOCKER;
    }
    if (this == CRITICAL) {
      return Priority.CRITICAL;
    }
    if (this == MAJOR) {
      return Priority.MAJOR;
    }
    if (this == MINOR) {
      return Priority.MINOR;
    }
    if (this == INFO) {
      return Priority.INFO;
    }
    return null;
  }
}