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
package org.sonar.server.qualityprofile.builtin;

import java.util.List;
import org.sonar.db.DbSession;
import org.sonar.server.qualityprofile.ActiveRuleChange;

public interface BuiltInQProfileInsert {
  /**
   * Persist a new built-in profile.
   * The session is not committed and Elasticsearch indices are not updated: the caller is responsible for
   * committing and indexing the returned changes.
   */
  List<ActiveRuleChange> create(DbSession batchSession, BuiltInQProfile builtInQProfile);
}
