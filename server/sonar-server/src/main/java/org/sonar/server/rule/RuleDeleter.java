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
package org.sonar.server.rule;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.rule.index.RuleIndexer;

@ServerSide
public class RuleDeleter {

  private final System2 system2;
  private final RuleIndexer ruleIndexer;
  private final DbClient dbClient;
  private final RuleActivator ruleActivator;

  public RuleDeleter(System2 system2, RuleIndexer ruleIndexer, DbClient dbClient, RuleActivator ruleActivator) {
    this.system2 = system2;
    this.ruleIndexer = ruleIndexer;
    this.dbClient = dbClient;
    this.ruleActivator = ruleActivator;
  }

  public void delete(RuleKey ruleKey) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      RuleDefinitionDto rule = dbClient.ruleDao().selectOrFailDefinitionByKey(dbSession, ruleKey);
      if (rule.getTemplateId() == null) {
        throw new IllegalStateException("Only custom rules can be deleted");
      }

      // For custom rule, first deactivate the rule on all profiles
      if (rule.getTemplateId() != null) {
        ruleActivator.deactivate(dbSession, rule);
      }

      rule.setStatus(RuleStatus.REMOVED);
      rule.setUpdatedAt(system2.now());
      dbClient.ruleDao().update(dbSession, rule);

      dbSession.commit();
      ruleIndexer.indexRuleDefinition(ruleKey);
    }
  }
}
