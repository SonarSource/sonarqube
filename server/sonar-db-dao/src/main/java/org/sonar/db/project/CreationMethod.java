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
package org.sonar.db.project;

import java.util.Arrays;

public enum CreationMethod {
  UNKNOWN(Category.UNKNOWN, false, true),
  LOCAL_API(Category.LOCAL, false, true),
  LOCAL_BROWSER(Category.LOCAL, true, true),
  ALM_IMPORT_API(Category.ALM_IMPORT, false, false),
  ALM_IMPORT_BROWSER(Category.ALM_IMPORT, true, false),
  ALM_IMPORT_MONOREPO_API(Category.ALM_IMPORT_MONOREPO, false, false),
  ALM_IMPORT_MONOREPO_BROWSER(Category.ALM_IMPORT_MONOREPO, true, false),
  SCANNER_API(Category.SCANNER, false, true),
  SCANNER_API_DEVOPS_AUTO_CONFIG(Category.SCANNER, false, false);

  private final Category category;
  private final boolean isCreatedViaBrowser;
  private final boolean isLocal;

  CreationMethod(Category category, boolean isCreatedViaBrowser, boolean isLocal) {
    this.isCreatedViaBrowser = isCreatedViaBrowser;
    this.category = category;
    this.isLocal = isLocal;
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

  public boolean isLocal() {
    return isLocal;
  }

  public enum Category {
    UNKNOWN, LOCAL, ALM_IMPORT, ALM_IMPORT_MONOREPO, SCANNER
  }
}
