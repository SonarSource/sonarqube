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
package org.sonar.server.platform.db.migration;

import java.util.ArrayList;
import java.util.List;

public class DatabaseMigrationLoggerContext {

  List<MigrationData> migrationData = new ArrayList<>();

  public void addMigrationData(String step, Long durationInMs, boolean success, Long startedAt, String targetVersion) {
    MigrationData data = new MigrationData(step, durationInMs, success, startedAt, targetVersion);
    migrationData.add(data);
  }

  public List<MigrationData> getMigrationData() {
    return migrationData;
  }

  public record MigrationData(
    String step,
    Long durationInMs,
    boolean success,
    Long startedAt,
    String targetVersion) {
  }

}
