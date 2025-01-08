/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.component;

public final class ComponentQualifiers {
  public static final String VIEW = "VW";
  public static final String SUBVIEW = "SVW";
  public static final String APP = "APP";
  public static final String PROJECT = "TRK";
  /** @deprecated */
  @Deprecated(since = "10.8", forRemoval = true)
  public static final String MODULE = "BRC";
  public static final String DIRECTORY = "DIR";
  public static final String FILE = "FIL";
  public static final String UNIT_TEST_FILE = "UTS";

  private ComponentQualifiers() {
  }
}
