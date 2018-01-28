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
package org.sonar.server.qualityprofile;

import java.util.List;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.RulesProfileDto;

public interface BuiltInQProfileUpdate {
  /**
   * Persist an existing built-in profile and associate it to all existing organizations.
   * Db session is committed and Elasticsearch indices are updated.
   */
  List<ActiveRuleChange> update(DbSession dbSession, BuiltInQProfile builtInQProfile, RulesProfileDto ruleProfile);
}
