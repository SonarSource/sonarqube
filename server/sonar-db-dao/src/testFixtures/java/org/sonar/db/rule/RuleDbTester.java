/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.util.Random;
import java.util.function.Consumer;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbTester;

import static java.util.Arrays.asList;
import static org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;
import static org.sonar.db.rule.RuleTesting.newDeprecatedRuleKey;
import static org.sonar.db.rule.RuleTesting.newRule;

public class RuleDbTester {
  private static final RuleType[] RULE_TYPES_EXCEPT_HOTSPOTS = Arrays.stream(RuleType.values())
    .filter(ruleType -> SECURITY_HOTSPOT != ruleType).toArray(RuleType[]::new);

  private final DbTester db;

  public RuleDbTester(DbTester db) {
    this.db = db;
  }

  public RuleDto insert() {
    return insert(newRule());
  }

  public RuleDto insert(RuleKey key) {
    return insert(newRule(key));
  }

  @SafeVarargs
  public final RuleDto insert(Consumer<RuleDto>... populaters) {
    RuleDto rule = newRule();
    asList(populaters).forEach(populater -> populater.accept(rule));
    return insert(rule);
  }

  public RuleDto insert(RuleKey key, Consumer<RuleDto> populater) {
    RuleDto rule = newRule(key);
    populater.accept(rule);
    return insert(rule);
  }

  public RuleDto insertIssueRule() {
    return insert(newIssueRule());
  }

  public RuleDto insertIssueRule(RuleKey key) {
    return insert(newIssueRule(key));
  }

  @SafeVarargs
  public final RuleDto insertIssueRule(Consumer<RuleDto>... populaters) {
    RuleDto rule = newIssueRule();
    asList(populaters).forEach(populater -> populater.accept(rule));
    return insert(rule);
  }

  public RuleDto insertIssueRule(RuleKey key, Consumer<RuleDto> populater) {
    RuleDto rule = newIssueRule(key);
    populater.accept(rule);
    return insert(rule);
  }

  private static RuleDto newIssueRule(RuleKey key) {
    return newRule(key).setType(RULE_TYPES_EXCEPT_HOTSPOTS[new Random().nextInt(RULE_TYPES_EXCEPT_HOTSPOTS.length)]);
  }

  private static RuleDto newIssueRule() {
    return newRule().setType(RULE_TYPES_EXCEPT_HOTSPOTS[new Random().nextInt(RULE_TYPES_EXCEPT_HOTSPOTS.length)]);
  }

  public RuleDto insertHotspotRule() {
    return insert(newHotspotRule());
  }

  @SafeVarargs
  public final RuleDto insertHotspotRule(Consumer<RuleDto>... populaters) {
    RuleDto rule = newHotspotRule();
    asList(populaters).forEach(populater -> populater.accept(rule));
    return insert(rule);
  }

  private static RuleDto newHotspotRule() {
    return newRule().setType(SECURITY_HOTSPOT);
  }

  public RuleDto insert(RuleDto rule) {
    if (rule.getUuid() == null) {
      rule.setUuid(Uuids.createFast());
    }

    db.getDbClient().ruleDao().insert(db.getSession(), rule);
    db.commit();
    return rule;
  }

  public RuleDto update(RuleDto rule) {
    db.getDbClient().ruleDao().update(db.getSession(), rule);
    db.commit();
    return rule;
  }

  @SafeVarargs
  public final RuleParamDto insertRuleParam(RuleDto rule, Consumer<RuleParamDto>... populaters) {
    RuleParamDto param = RuleTesting.newRuleParam(rule);
    asList(populaters).forEach(populater -> populater.accept(param));
    db.getDbClient().ruleDao().insertRuleParam(db.getSession(), rule, param);
    db.commit();
    return param;
  }

  /**
   * Create and persist a rule with random values.
   */
  public RuleDto insertRule() {
    return insertRule(rule -> {
    });
  }

  @SafeVarargs
  public final RuleDto insertRule(Consumer<RuleDto>... populaters) {
    RuleDto ruleDto = RuleTesting.newRule();
    asList(populaters).forEach(populater -> populater.accept(ruleDto));
    return insert(ruleDto);
  }

  public RuleDto insertRule(Consumer<RuleDto> populateRuleDto) {
    RuleDto ruleDto = RuleTesting.newRule();
    populateRuleDto.accept(ruleDto);

    if (ruleDto.getUuid() == null) {
      ruleDto.setUuid(Uuids.createFast());
    }

    return insert(ruleDto);
  }

  @SafeVarargs
  public final DeprecatedRuleKeyDto insertDeprecatedKey(Consumer<DeprecatedRuleKeyDto>... deprecatedRuleKeyDtoConsumers) {
    DeprecatedRuleKeyDto deprecatedRuleKeyDto = newDeprecatedRuleKey();
    asList(deprecatedRuleKeyDtoConsumers).forEach(c -> c.accept(deprecatedRuleKeyDto));
    db.getDbClient().ruleDao().insert(db.getSession(), deprecatedRuleKeyDto);
    return deprecatedRuleKeyDto;
  }

}
