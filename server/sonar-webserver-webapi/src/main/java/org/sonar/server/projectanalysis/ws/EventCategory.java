/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.projectanalysis.ws;

public enum EventCategory {
  VERSION("Version"),
  OTHER("Other"),
  QUALITY_PROFILE("Profile"),
  QUALITY_GATE("Alert"),
  DEFINITION_CHANGE("Definition change"),
  ISSUE_DETECTION("Issue Detection"),
  SQ_UPGRADE("SQ Upgrade");

  private final String label;

  EventCategory(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public static EventCategory fromLabel(String label) {
    for (EventCategory category : values()) {
      if (category.getLabel().equals(label)) {
        return category;
      }
    }

    throw new IllegalArgumentException("Unknown event category label '" + label + "'");
  }
}
