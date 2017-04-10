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

import java.util.Arrays;
import java.util.function.Consumer;
import org.apache.commons.lang.RandomStringUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;

import static org.sonar.db.rule.RuleTesting.newRule;
import static org.sonar.db.rule.RuleTesting.newRuleDto;
import static org.sonar.db.rule.RuleTesting.newRuleMetadata;

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

  public RuleDefinitionDto insert(Consumer<RuleDefinitionDto> populater) {
    RuleDefinitionDto rule = newRule();
    populater.accept(rule);
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

  public RuleMetadataDto insertOrUpdateMetadata(RuleDefinitionDto rule, OrganizationDto organization) {
    return insertOrUpdateMetadata(rule, organization, r -> {});
  }

  public RuleMetadataDto insertOrUpdateMetadata(RuleDefinitionDto rule, OrganizationDto organization, Consumer<RuleMetadataDto> populater) {
    RuleMetadataDto dto = RuleTesting.newRuleMetadata(rule, organization);
    populater.accept(dto);
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

  public RuleParamDto insertRuleParam(RuleDefinitionDto rule, Consumer<RuleParamDto> populater) {
    RuleParamDto param = RuleTesting.newRuleParam(rule);
    populater.accept(param);
    db.getDbClient().ruleDao().insertRuleParam(db.getSession(), rule, param);
    db.commit();
    return param;
  }

  public RuleDto insertRule(RuleDto ruleDto) {
    RuleDao ruleDao = insertRule(ruleDto.getDefinition());
    RuleMetadataDto metadata = ruleDto.getMetadata();
    if (metadata.getOrganizationUuid() != null) {
      ruleDao.insertOrUpdate(db.getSession(), metadata.setRuleId(ruleDto.getId()));
      db.commit();
    }
    return ruleDto;
  }

  public RuleDao insertRule(RuleDefinitionDto ruleDto) {
    RuleDao ruleDao = db.getDbClient().ruleDao();
    ruleDao.insert(db.getSession(), ruleDto);
    db.commit();
    return ruleDao;
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
    Arrays.asList(populaters).forEach(populater -> populater.accept(ruleDto));
    return insertRule(ruleDto);
  }

  @SafeVarargs
  public final RuleDefinitionDto insertRuleDefinition(Consumer<RuleDefinitionDto>... populaters) {
    RuleDefinitionDto rule = newRule();
    Arrays.asList(populaters).forEach(populater -> populater.accept(rule));
    insertRuleDefinition(rule);
    return rule;
  }

  private void insertRuleDefinition(RuleDefinitionDto rule) {
    RuleDao ruleDao = db.getDbClient().ruleDao();
    ruleDao.insert(db.getSession(), rule);
    db.commit();
  }

  @SafeVarargs
  public final RuleMetadataDto insertRuleMetadata(RuleDefinitionDto rule, OrganizationDto organization, Consumer<RuleMetadataDto>... populaters) {
    RuleMetadataDto ruleMetadataDto = newRuleMetadata()
      .setRuleId(rule.getId())
      .setOrganizationUuid(organization.getUuid());
    Arrays.asList(populaters).forEach(populater -> populater.accept(ruleMetadataDto));
    insertRuleMetadata(ruleMetadataDto);
    return ruleMetadataDto;
  }

  private void insertRuleMetadata(RuleMetadataDto metadata) {
    RuleDao ruleDao = db.getDbClient().ruleDao();
    ruleDao.insert(db.getSession(), metadata);
    db.commit();
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
