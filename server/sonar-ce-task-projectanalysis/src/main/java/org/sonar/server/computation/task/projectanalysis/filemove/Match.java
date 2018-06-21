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
package org.sonar.server.computation.task.projectanalysis.filemove;

import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
final class Match {
  private final String dbKey;
  private final String reportKey;

  Match(String dbKey, String reportKey) {
    this.dbKey = dbKey;
    this.reportKey = reportKey;
  }

  public String getDbKey() {
    return dbKey;
  }

  public String getReportKey() {
    return reportKey;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Match match = (Match) o;
    return dbKey.equals(match.dbKey) && reportKey.equals(match.reportKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dbKey, reportKey);
  }

  @Override
  public String toString() {
    return '{' + dbKey + "=>" + reportKey + '}';
  }
}
