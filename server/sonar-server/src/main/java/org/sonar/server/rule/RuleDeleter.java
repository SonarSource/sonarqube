/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.rule;

import org.sonar.api.ServerSide;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.rule.index.RuleDoc;

@ServerSide
public class RuleDeleter {

  private final DbClient dbClient;
  private final RuleActivator ruleActivator;

  public RuleDeleter(DbClient dbClient, RuleActivator ruleActivator) {
    this.dbClient = dbClient;
    this.ruleActivator = ruleActivator;
  }

  public void delete(RuleKey ruleKey) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      RuleDto rule = dbClient.ruleDao().getByKey(dbSession, ruleKey);
      if (rule.getTemplateId() == null && !rule.getRepositoryKey().equals(RuleDoc.MANUAL_REPOSITORY)) {
        throw new IllegalStateException("Only custom rules and manual rules can be deleted");
      }

      // For custom rule, first deactivate the rule on all profiles
      if (rule.getTemplateId() != null) {
        ruleActivator.deactivate(dbSession, rule);
      }

      rule.setStatus(RuleStatus.REMOVED);
      dbClient.ruleDao().update(dbSession, rule);

      dbSession.commit();
    } finally {
      dbSession.close();
    }
  }
}
