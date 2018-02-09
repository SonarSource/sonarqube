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
package org.sonar.db.rule;

import java.util.function.Consumer;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.sonar.db.rule.RuleTesting.newDeprecatedRuleKey;
import static org.sonar.db.rule.RuleTesting.newRule;
import static org.sonar.db.rule.RuleTesting.newRuleDto;

public class RuleDbTester {

  private final DbTester db;

  public RuleDbTester(DbTester db) {
    this.db = db;
  }

  public RuleDefinitionDto insert() {
    return insert(newRule());
  }

  public RuleDefinitionDto insert(RuleKey key) {
    return insert(newRule(key));
  }

  @SafeVarargs
  public final RuleDefinitionDto insert(Consumer<RuleDefinitionDto>... populaters) {
    RuleDefinitionDto rule = newRule();
    asList(populaters).forEach(populater -> populater.accept(rule));
    return insert(rule);
  }

  public RuleDefinitionDto insert(RuleKey key, Consumer<RuleDefinitionDto> populater) {
    RuleDefinitionDto rule = newRule(key);
    populater.accept(rule);
    return insert(rule);
  }

  public RuleDefinitionDto insert(RuleDefinitionDto rule) {
    db.getDbClient().ruleDao().insert(db.getSession(), rule);
    db.commit();
    return rule;
  }

  public RuleDefinitionDto update(RuleDefinitionDto rule) {
    db.getDbClient().ruleDao().update(db.getSession(), rule);
    db.commit();
    return rule;
  }

  @SafeVarargs
  public final RuleMetadataDto insertOrUpdateMetadata(RuleDefinitionDto rule, OrganizationDto organization, Consumer<RuleMetadataDto>... populaters) {
    RuleMetadataDto dto = RuleTesting.newRuleMetadata(rule, organization);
    asList(populaters).forEach(populater -> populater.accept(dto));
    return insertOrUpdateMetadata(dto);
  }

  public RuleMetadataDto insertOrUpdateMetadata(RuleMetadataDto metadata) {
    db.getDbClient().ruleDao().insertOrUpdate(db.getSession(), metadata);
    db.commit();
    return metadata;
  }

  public RuleParamDto insertRuleParam(RuleDefinitionDto rule) {
    return insertRuleParam(rule, p -> {});
  }

  @SafeVarargs
  public final RuleParamDto insertRuleParam(RuleDefinitionDto rule, Consumer<RuleParamDto>... populaters) {
    RuleParamDto param = RuleTesting.newRuleParam(rule);
    asList(populaters).forEach(populater -> populater.accept(param));
    db.getDbClient().ruleDao().insertRuleParam(db.getSession(), rule, param);
    db.commit();
    return param;
  }

  public RuleDto insertRule(RuleDto ruleDto) {
    insert(ruleDto.getDefinition());
    RuleMetadataDto metadata = ruleDto.getMetadata();
    if (metadata.getOrganizationUuid() != null) {
      db.getDbClient().ruleDao().insertOrUpdate(db.getSession(), metadata.setRuleId(ruleDto.getId()));
      db.commit();
    }
    return ruleDto;
  }

  public RuleDto updateRule(RuleDto ruleDto) {
    update(ruleDto.getDefinition());
    RuleMetadataDto metadata = ruleDto.getMetadata();
    if (metadata.getOrganizationUuid() != null) {
      db.getDbClient().ruleDao().insertOrUpdate(db.getSession(), metadata.setRuleId(ruleDto.getId()));
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

  @SafeVarargs
  public final RuleDto insertRule(OrganizationDto organization, Consumer<RuleDto>... populaters) {
    RuleDto ruleDto = newRuleDto(organization);
    asList(populaters).forEach(populater -> populater.accept(ruleDto));
    return insertRule(ruleDto);
  }

  public RuleDto insertRule(Consumer<RuleDto> populateRuleDto) {
    RuleDto ruleDto = newRuleDto();
    populateRuleDto.accept(ruleDto);
    return insertRule(ruleDto);
  }

  @SafeVarargs
  public final DeprecatedRuleKeyDto insertDeprecatedKey(Consumer<DeprecatedRuleKeyDto>... deprecatedRuleKeyDtoConsumers) {
    DeprecatedRuleKeyDto deprecatedRuleKeyDto = newDeprecatedRuleKey();
    asList(deprecatedRuleKeyDtoConsumers).forEach(c -> c.accept(deprecatedRuleKeyDto));
    db.getDbClient().ruleDao().insert(db.getSession(), deprecatedRuleKeyDto);
    return deprecatedRuleKeyDto;
  }


  public RuleParamDto insertRuleParam(RuleDto rule) {
    RuleParamDto param = new RuleParamDto();
    param.setRuleId(rule.getId());
    param.setName(randomAlphabetic(10));
    param.setType(RuleParamType.STRING.type());
    db.getDbClient().ruleDao().insertRuleParam(db.getSession(), rule.getDefinition(), param);
    db.commit();
    return param;
  }

  public RuleDto insertRule(RuleDefinitionDto ruleDefinition, RuleMetadataDto ruleMetadata) {
    db.getDbClient().ruleDao().insertOrUpdate(db.getSession(), ruleMetadata.setRuleId(ruleDefinition.getId()));
    db.commit();
    return new RuleDto(ruleDefinition, ruleMetadata);
  }
}
