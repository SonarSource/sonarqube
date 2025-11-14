/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.dismissmessage;

public enum MessageType {
  INFO(false, false),
  GENERIC(false, true),
  SUGGEST_DEVELOPER_EDITION_UPGRADE(true, true),
  GLOBAL_NCD_90(true, false),
  GLOBAL_NCD_PAGE_90(true, false),
  PROJECT_NCD_90(true, false),
  PROJECT_NCD_PAGE_90(true, false),
  BRANCH_NCD_90(true, false),
  UNRESOLVED_FINDINGS_IN_AI_GENERATED_CODE(true, true);

  private final boolean dismissible;
  private final boolean isWarning;

  MessageType(boolean dismissible, boolean isWarning) {
    this.dismissible = dismissible;
    this.isWarning = isWarning;
  }

  public boolean isDismissible() {
    return dismissible;
  }

  public boolean isWarning() {
    return isWarning;
  }
}
