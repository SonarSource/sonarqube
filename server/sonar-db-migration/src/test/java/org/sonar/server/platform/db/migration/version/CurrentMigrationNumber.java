/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version;

/**
 * The migration number of the most recently added migration step, across every {@code DbVersion*}
 * package registered in {@link org.sonar.server.platform.db.migration.MigrationConfigurationModule}.
 * <p>
 * Whenever a PR adds a new migration step, it must bump this value to match, regardless of which
 * package the step is registered in. {@code MigrationNumberingConsistencyTest} fails the build otherwise.
 * <p>
 * This is deliberately a single, always-touched line. Two PRs adding migrations around the same time
 * now get a real git merge conflict here, forcing whichever merges second to notice the other's
 * migration and pick a number above it, instead of silently landing in an older package that an
 * environment further ahead in its migration history would never select for execution again.
 */
public final class CurrentMigrationNumber {

  public static final long VALUE = 2026_06_003L;

  private CurrentMigrationNumber() {
    // prevents instantiation
  }
}
