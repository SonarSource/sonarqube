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
package org.sonar.server.rule;

import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.rule.index.RuleIndexer;

import static org.sonar.api.rule.RuleStatus.READY;
import static org.sonar.db.rule.RuleDto.Scope.ALL;

public class AddHocRuleCreator {

  private final DbClient dbClient;
  private final System2 system2;
  private final RuleIndexer ruleIndexer;

  public AddHocRuleCreator(DbClient dbClient, System2 system2, RuleIndexer ruleIndexer) {
    this.dbClient = dbClient;
    this.system2 = system2;
    this.ruleIndexer = ruleIndexer;
  }

  /**
   * Persists a new add hoc rule in the DB and indexes it.
   * @return the rule that was inserted in the DB, which <b>includes the generated ID</b>. 
   */
  public RuleDto persistAndIndex(DbSession dbSession, NewAddHocRule adHoc) {
    RuleDao dao = dbClient.ruleDao();
    dao.insert(dbSession, new RuleDefinitionDto()
      .setRuleKey(adHoc.getKey())
      .setPluginKey(adHoc.getPluginKey())
      .setIsExternal(true)
      .setName(adHoc.getName())
      .setIsAdHoc(true)
      .setScope(ALL)
      .setStatus(READY)
      .setCreatedAt(system2.now())
      .setUpdatedAt(system2.now()));

    RuleDto ruleDto = dao.selectOrFailByKey(dbSession, adHoc.getKey());
    ruleIndexer.commitAndIndex(dbSession, ruleDto.getId());
    return ruleDto;
  }

}
