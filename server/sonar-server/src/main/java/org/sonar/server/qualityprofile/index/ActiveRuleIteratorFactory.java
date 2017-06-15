/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.qualityprofile.index;

import java.util.Collection;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.RulesProfileDto;

@ServerSide
public class ActiveRuleIteratorFactory {

  private final DbClient dbClient;

  public ActiveRuleIteratorFactory(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public ActiveRuleIterator createForAll(DbSession dbSession) {
    return new ActiveRuleIteratorForSingleChunk(dbClient, dbSession);
  }

  public ActiveRuleIterator createForRuleProfile(DbSession dbSession, RulesProfileDto ruleProfile) {
    return new ActiveRuleIteratorForSingleChunk(dbClient, dbSession, ruleProfile);
  }

  public ActiveRuleIterator createForActiveRules(DbSession dbSession, Collection<Integer> activeRuleIds) {
    return new ActiveRuleIteratorForMultipleChunks(dbClient, dbSession, activeRuleIds);
  }
}
