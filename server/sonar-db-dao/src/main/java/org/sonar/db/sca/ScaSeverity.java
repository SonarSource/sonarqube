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
package org.sonar.db.sca;

/**
 * The severity of a dependency issue found by SCA.
 * This is calculated as a base severity (which may be based on a vulnerability's CVSS score
 * or just based on the type of issue), and then analysis-specific factors such as reachability
 * can be considered to get the final severity.
 */
public enum ScaSeverity {
  INFO(5),
  LOW(10),
  MEDIUM(15),
  HIGH(20),
  BLOCKER(25);

  // this needs to match the DB varchar length
  public static final int MAX_NAME_LENGTH = 15;
  private final int databaseSortKey;

  ScaSeverity(int databaseSortKey) {
    this.databaseSortKey = databaseSortKey;
  }

  /**
   * Returns the sort key for the severity in the database.
   * We store the severity as a string for debuggability
   * and so on, but to sort by severity we need an integer
   * that gets higher as the severity gets more severe.
   * The sort keys have gaps so we could add new
   * in-between values to the enum without a big data migration.
   * @return integer to sort by severity
   */
  public final int databaseSortKey() {
    return databaseSortKey;
  }
}
