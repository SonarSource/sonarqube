/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.common.rule.service;

import java.util.List;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.common.rule.RuleCreator;

import static java.util.Collections.singletonList;

public class RuleService {

  private final DbClient dbClient;
  private final RuleCreator ruleCreator;

  public RuleService(DbClient dbClient, RuleCreator ruleCreator) {
    this.dbClient = dbClient;
    this.ruleCreator = ruleCreator;
  }

  public RuleInformation createCustomRule(NewCustomRule newCustomRule) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return createCustomRule(newCustomRule, dbSession);
    }
  }

  public RuleInformation createCustomRule(NewCustomRule newCustomRule, DbSession dbSession) {
    RuleDto ruleDto = ruleCreator.create(dbSession, newCustomRule);
    List<RuleParamDto> ruleParameters = dbClient.ruleDao().selectRuleParamsByRuleUuids(dbSession, singletonList(ruleDto.getUuid()));
    return new RuleInformation(ruleDto, ruleParameters);
  }
}
