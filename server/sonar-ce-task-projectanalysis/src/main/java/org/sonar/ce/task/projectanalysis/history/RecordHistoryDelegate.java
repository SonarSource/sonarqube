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
package org.sonar.ce.task.projectanalysis.history;

/**
 * Extension point implemented by the base {@code sonar-ce-task-projectanalysis} module.
 * Allows the public CE analysis pipeline to record issue and measure history.
 */
public interface RecordHistoryDelegate {

  /**
   * Records issue and measure history snapshots for the entity identified by {@code entityUuid}.
   * Implementations should be idempotent: calling this multiple times for the same entity on the
   * same UTC day must not create duplicate history rows.
   *
   * @param entityUuid the UUID of the entity (branch component UUID or portfolio ID) being analysed
   */
  void recordHistory(String entityUuid);
}
