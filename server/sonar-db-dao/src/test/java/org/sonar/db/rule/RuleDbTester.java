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
package org.sonar.db.rule;

import java.util.function.Consumer;
import org.apache.commons.lang.RandomStringUtils;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;

import static org.sonar.db.rule.RuleTesting.newRuleDto;

public class RuleDbTester {

  private final DbTester db;

  public RuleDbTester(DbTester db) {
    this.db = db;
  }

  public RuleDefinitionDto insertRule(RuleDefinitionDto ruleDto) {
    RuleDao ruleDao = db.getDbClient().ruleDao();
    ruleDao.insert(db.getSession(), ruleDto);
    db.commit();
    return ruleDto;
  }

  public RuleDto insertRule(RuleDto ruleDto) {
    RuleDao ruleDao = db.getDbClient().ruleDao();
    ruleDao.insert(db.getSession(), ruleDto.getDefinition());
    db.commit();
    RuleMetadataDto metadata = ruleDto.getMetadata();
    if (metadata.getOrganizationUuid() != null) {
      ruleDao.update(db.getSession(), metadata.setRuleId(ruleDto.getId()));
      db.commit();
    }
    return ruleDto;
  }

  /**
   * Create and persist a rule with random values.
   */
  public RuleDto insertRule() {
    return insertRule(rule -> {
    });
  }

  public RuleDto insertRule(OrganizationDto organization, Consumer<RuleDto> populateRuleDto) {
    RuleDto ruleDto = newRuleDto(organization);
    populateRuleDto.accept(ruleDto);
    return insertRule(ruleDto);
  }

  public RuleDto insertRule(Consumer<RuleDto> populateRuleDto) {
    RuleDto ruleDto = newRuleDto();
    populateRuleDto.accept(ruleDto);
    return insertRule(ruleDto);
  }

  public RuleParamDto insertRuleParam(RuleDto rule) {
    RuleParamDto param = new RuleParamDto();
    param.setRuleId(rule.getId());
    param.setName(RandomStringUtils.random(10));
    param.setType(RuleParamType.STRING.type());
    db.getDbClient().ruleDao().insertRuleParam(db.getSession(), rule.getDefinition(), param);
    db.commit();
    return param;
  }
}
