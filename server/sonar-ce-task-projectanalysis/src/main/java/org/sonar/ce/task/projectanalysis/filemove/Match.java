/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.filemove;

import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
final class Match {
  private final String dbUuid;
  private final String reportUuid;

  Match(String dbUuid, String reportUuid) {
    this.dbUuid = dbUuid;
    this.reportUuid = reportUuid;
  }

  public String getDbUuid() {
    return dbUuid;
  }

  public String getReportUuid() {
    return reportUuid;
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
    return dbUuid.equals(match.dbUuid) && reportUuid.equals(match.reportUuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dbUuid, reportUuid);
  }

  @Override
  public String toString() {
    return '{' + dbUuid + "=>" + reportUuid + '}';
  }
}
