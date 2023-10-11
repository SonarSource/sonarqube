/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.db.project;

import java.util.Arrays;

public enum CreationMethod {
  UNKNOWN(Category.UNKNOWN, false),
  LOCAL_API(Category.LOCAL, false),
  LOCAL_BROWSER(Category.LOCAL, true),
  ALM_IMPORT_API(Category.ALM_IMPORT, false),
  ALM_IMPORT_BROWSER(Category.ALM_IMPORT, true),
  SCANNER_API(Category.SCANNER, false),
  SCANNER_API_DEVOPS_AUTO_CONFIG(Category.SCANNER, false);

  private final boolean isCreatedViaBrowser;
  private final Category category;

  CreationMethod(Category category, boolean isCreatedViaBrowser) {
    this.isCreatedViaBrowser = isCreatedViaBrowser;
    this.category = category;
  }

  public static CreationMethod getCreationMethod(Category category, boolean isBrowserCall) {
    return Arrays.stream(CreationMethod.values())
      .filter(creationMethod -> creationMethod.getCategory().equals(category))
      .filter(creationMethod -> creationMethod.isCreatedViaBrowser() == isBrowserCall)
      .findAny()
      .orElse(UNKNOWN);
  }

  private boolean isCreatedViaBrowser() {
    return isCreatedViaBrowser;
  }

  private Category getCategory() {
    return category;
  }

  public enum Category {UNKNOWN, LOCAL, ALM_IMPORT, SCANNER}
}
