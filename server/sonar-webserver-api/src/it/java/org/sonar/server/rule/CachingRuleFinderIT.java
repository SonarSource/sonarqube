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
package org.sonar.server.rule;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleTesting;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class CachingRuleFinderIT {
  @org.junit.Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = dbTester.getDbClient();
  private final AlwaysIncreasingSystem2 system2 = new AlwaysIncreasingSystem2();
  private RuleDto[] ruleDtos;
  private RuleParamDto[] ruleParams;
  private CachingRuleFinder underTest;
  private RuleDescriptionFormatter ruleDescriptionFormatter = new RuleDescriptionFormatter();

  @Before()
  public void setUp() {
    Consumer<RuleDto> setUpdatedAt = rule -> rule.setUpdatedAt(system2.now());
    this.ruleDtos = new RuleDto[] {
      dbTester.rules().insert(setUpdatedAt),
      dbTester.rules().insert(setUpdatedAt),
      dbTester.rules().insert(setUpdatedAt),
      dbTester.rules().insert(setUpdatedAt),
      dbTester.rules().insert(setUpdatedAt),
      dbTester.rules().insert(setUpdatedAt)
    };
    this.ruleParams = Arrays.stream(ruleDtos)
      .map(rule -> dbTester.rules().insertRuleParam(rule))
      .toArray(RuleParamDto[]::new);

    underTest = new CachingRuleFinder(dbClient, ruleDescriptionFormatter);

    // delete all data from DB to ensure tests rely on cache exclusively
    dbTester.executeUpdateSql("delete from rules");
    dbTester.executeUpdateSql("delete from rules_parameters");
    assertThat(dbTester.countRowsOfTable("rules")).isZero();
    assertThat(dbTester.countRowsOfTable("rules_parameters")).isZero();
  }

  @Test
  public void constructor_reads_rules_from_DB() {
    DbClient dbClient = mock(DbClient.class);
    DbSession dbSession = mock(DbSession.class);
    RuleDao ruleDao = mock(RuleDao.class);
    when(dbClient.openSession(anyBoolean())).thenReturn(dbSession);
    when(dbClient.ruleDao()).thenReturn(ruleDao);

    new CachingRuleFinder(dbClient, ruleDescriptionFormatter);

    verify(dbClient).openSession(anyBoolean());
    verify(ruleDao).selectAll(dbSession);
    verify(ruleDao).selectAllRuleParams(dbSession);
    verifyNoMoreInteractions(ruleDao);
  }

  @Test
  public void constructor_reads_parameters_from_DB() {
    DbClient dbClient = mock(DbClient.class);
    DbSession dbSession = mock(DbSession.class);
    RuleDao ruleDao = mock(RuleDao.class);
    when(dbClient.openSession(anyBoolean())).thenReturn(dbSession);
    when(dbClient.ruleDao()).thenReturn(ruleDao);
    List<RuleKey> ruleKeys = Arrays.asList(RuleKey.of("A", "B"), RuleKey.of("C", "D"), RuleKey.of("E", "F"));
    when(ruleDao.selectAll(dbSession)).thenReturn(ruleKeys.stream().map(RuleTesting::newRule).collect(toList()));

    new CachingRuleFinder(dbClient, ruleDescriptionFormatter);

    verify(ruleDao).selectAllRuleParams(dbSession);
  }

  @Test
  public void findByKey_returns_all_loaded_rules() {
    for (int i = 0; i < ruleDtos.length; i++) {
      RuleDto ruleDto = ruleDtos[i];
      RuleParamDto ruleParam = ruleParams[i];

      org.sonar.api.rules.Rule rule = underTest.findByKey(ruleDto.getKey());
      verifyRule(rule, ruleDto, ruleParam);
      assertThat(underTest.findByKey(ruleDto.getRepositoryKey(), ruleDto.getRuleKey()))
        .isSameAs(rule);
    }
  }

  @Test
  public void findByKey_returns_null_when_RuleKey_is_null() {
    assertThat(underTest.findByKey(null)).isNull();
  }

  @Test
  public void findByKey_returns_null_when_repository_key_is_null() {
    assertThat(underTest.findByKey(null, secure().nextAlphabetic(2))).isNull();
  }

  @Test
  public void findByKey_returns_null_when_key_is_null() {
    assertThat(underTest.findByKey(secure().nextAlphabetic(2), null)).isNull();
  }

  @Test
  public void findByKey_returns_null_when_both_repository_key_and_key_are_null() {
    assertThat(underTest.findByKey(null, null)).isNull();
  }

  @Test
  public void find_returns_null_when_RuleQuery_is_empty() {
    assertThat(underTest.find(null)).isNull();
  }

  @Test
  public void find_returns_most_recent_rule_when_RuleQuery_has_no_non_null_field() {
    Rule rule = underTest.find(RuleQuery.create());

    assertThat(toRuleKey(rule)).isEqualTo(ruleDtos[5].getKey());
  }

  @Test
  public void find_searches_by_exact_match_of_repository_key_and_returns_most_recent_rule() {
    String repoKey = "ABCD";
    RuleDto[] sameRepoKey = {
      dbTester.rules().insert(rule -> rule.setRepositoryKey(repoKey).setUpdatedAt(system2.now())),
      dbTester.rules().insert(rule -> rule.setRepositoryKey(repoKey).setUpdatedAt(system2.now()))
    };
    RuleDto otherRule = dbTester.rules().insert(rule -> rule.setUpdatedAt(system2.now()));

    CachingRuleFinder underTest = new CachingRuleFinder(dbClient, ruleDescriptionFormatter);

    assertThat(toRuleKey(underTest.find(RuleQuery.create().withRepositoryKey(repoKey))))
      .isEqualTo(sameRepoKey[1].getKey());
    assertThat(toRuleKey(underTest.find(RuleQuery.create().withRepositoryKey(otherRule.getRepositoryKey()))))
      .isEqualTo(otherRule.getKey());
    assertThat(underTest.find(RuleQuery.create().withRepositoryKey(repoKey.toLowerCase())))
      .isNull();
    assertThat(underTest.find(RuleQuery.create().withRepositoryKey(secure().nextAlphabetic(3))))
      .isNull();
  }

  @Test
  public void find_searches_by_exact_match_of_ruleKey_and_returns_most_recent_rule() {
    String ruleKey = "ABCD";
    RuleDto[] sameRuleKey = {
      dbTester.rules().insert(rule -> rule.setRuleKey(ruleKey).setUpdatedAt(system2.now())),
      dbTester.rules().insert(rule -> rule.setRuleKey(ruleKey).setUpdatedAt(system2.now()))
    };
    RuleDto otherRule = dbTester.rules().insert(rule -> rule.setUpdatedAt(system2.now()));

    CachingRuleFinder underTest = new CachingRuleFinder(dbClient, ruleDescriptionFormatter);

    assertThat(toRuleKey(underTest.find(RuleQuery.create().withKey(ruleKey))))
      .isEqualTo(sameRuleKey[1].getKey());
    assertThat(toRuleKey(underTest.find(RuleQuery.create().withKey(otherRule.getRuleKey()))))
      .isEqualTo(otherRule.getKey());
    assertThat(underTest.find(RuleQuery.create().withKey(ruleKey.toLowerCase())))
      .isNull();
    assertThat(underTest.find(RuleQuery.create().withKey(secure().nextAlphabetic(3))))
      .isNull();
  }

  @Test
  public void find_searches_by_exact_match_of_configKey_and_returns_most_recent_rule() {
    String configKey = "ABCD";
    RuleDto[] sameConfigKey = {
      dbTester.rules().insert(rule -> rule.setConfigKey(configKey).setUpdatedAt(system2.now())),
      dbTester.rules().insert(rule -> rule.setConfigKey(configKey).setUpdatedAt(system2.now()))
    };
    RuleDto otherRule = dbTester.rules().insert(rule -> rule.setUpdatedAt(system2.now()));

    CachingRuleFinder underTest = new CachingRuleFinder(dbClient, ruleDescriptionFormatter);

    assertThat(toRuleKey(underTest.find(RuleQuery.create().withConfigKey(configKey))))
      .isEqualTo(sameConfigKey[1].getKey());
    assertThat(toRuleKey(underTest.find(RuleQuery.create().withConfigKey(otherRule.getConfigKey()))))
      .isEqualTo(otherRule.getKey());
    assertThat(underTest.find(RuleQuery.create().withConfigKey(configKey.toLowerCase())))
      .isNull();
    assertThat(underTest.find(RuleQuery.create().withConfigKey(secure().nextAlphabetic(3))))
      .isNull();
  }

  @Test
  public void find_searches_by_exact_match_and_match_on_all_criterias_and_returns_most_recent_match() {
    String repoKey = "ABCD";
    String ruleKey = "EFGH";
    String configKey = "IJKL";
    RuleDto[] rules = {
      dbTester.rules().insert(rule -> rule.setRepositoryKey(repoKey).setRuleKey(ruleKey).setConfigKey(configKey).setUpdatedAt(system2.now())),
      dbTester.rules().insert(rule -> rule.setRuleKey(ruleKey).setConfigKey(configKey).setUpdatedAt(system2.now())),
      dbTester.rules().insert(rule -> rule.setRepositoryKey(repoKey).setConfigKey(configKey).setUpdatedAt(system2.now())),
      dbTester.rules().insert(rule -> rule.setUpdatedAt(system2.now()))
    };
    RuleQuery allQuery = RuleQuery.create().withRepositoryKey(repoKey).withKey(ruleKey).withConfigKey(configKey);
    RuleQuery ruleAndConfigKeyQuery = RuleQuery.create().withKey(ruleKey).withConfigKey(configKey);
    RuleQuery repoAndConfigKeyQuery = RuleQuery.create().withRepositoryKey(repoKey).withConfigKey(configKey);
    RuleQuery repoAndKeyQuery = RuleQuery.create().withRepositoryKey(repoKey).withKey(ruleKey);
    RuleQuery configKeyQuery = RuleQuery.create().withConfigKey(configKey);
    RuleQuery ruleKeyQuery = RuleQuery.create().withKey(ruleKey);
    RuleQuery repoKeyQuery = RuleQuery.create().withRepositoryKey(repoKey);

    CachingRuleFinder underTest = new CachingRuleFinder(dbClient, ruleDescriptionFormatter);

    assertThat(toRuleKey(underTest.find(allQuery))).isEqualTo(rules[0].getKey());
    assertThat(toRuleKey(underTest.find(ruleAndConfigKeyQuery))).isEqualTo(rules[1].getKey());
    assertThat(toRuleKey(underTest.find(repoAndConfigKeyQuery))).isEqualTo(rules[2].getKey());
    assertThat(toRuleKey(underTest.find(repoAndKeyQuery))).isEqualTo(rules[0].getKey());
    assertThat(toRuleKey(underTest.find(repoKeyQuery))).isEqualTo(rules[2].getKey());
    assertThat(toRuleKey(underTest.find(ruleKeyQuery))).isEqualTo(rules[1].getKey());
    assertThat(toRuleKey(underTest.find(configKeyQuery))).isEqualTo(rules[2].getKey());
  }

  @Test
  public void findAll_returns_empty_when_RuleQuery_is_empty() {
    assertThat(underTest.findAll(null)).isEmpty();
  }

  @Test
  public void findAll_returns_all_rules_when_RuleQuery_has_no_non_null_field() {
    assertThat(underTest.findAll(RuleQuery.create()))
      .extracting(CachingRuleFinderIT::toRuleKey)
      .containsOnly(Arrays.stream(ruleDtos).map(RuleDto::getKey).toArray(RuleKey[]::new));
  }

  @Test
  public void findAll_returns_all_rules_with_exact_same_repository_key_and_order_them_most_recent_first() {
    String repoKey = "ABCD";
    long currentTimeMillis = System.currentTimeMillis();
    RuleDto[] sameRepoKey = {
      dbTester.rules().insert(rule -> rule.setRepositoryKey(repoKey).setUpdatedAt(currentTimeMillis + system2.now())),
      dbTester.rules().insert(rule -> rule.setRepositoryKey(repoKey).setUpdatedAt(currentTimeMillis + system2.now()))
    };
    RuleDto otherRule = dbTester.rules().insert(rule -> rule.setUpdatedAt(currentTimeMillis + system2.now()));

    CachingRuleFinder underTest = new CachingRuleFinder(dbClient, ruleDescriptionFormatter);

    assertThat(underTest.findAll(RuleQuery.create().withRepositoryKey(repoKey)))
      .extracting(CachingRuleFinderIT::toRuleKey)
      .containsExactly(sameRepoKey[1].getKey(), sameRepoKey[0].getKey());
    assertThat(underTest.findAll(RuleQuery.create().withRepositoryKey(otherRule.getRepositoryKey())))
      .extracting(CachingRuleFinderIT::toRuleKey)
      .containsExactly(otherRule.getKey());
    assertThat(underTest.findAll(RuleQuery.create().withRepositoryKey(repoKey.toLowerCase())))
      .isEmpty();
    assertThat(underTest.findAll(RuleQuery.create().withRepositoryKey(secure().nextAlphabetic(3))))
      .isEmpty();
  }

  @Test
  public void findAll_returns_all_rules_with_exact_same_rulekey_and_order_them_most_recent_first() {
    String ruleKey = "ABCD";
    RuleDto[] sameRuleKey = {
      dbTester.rules().insert(rule -> rule.setRuleKey(ruleKey).setUpdatedAt(system2.now())),
      dbTester.rules().insert(rule -> rule.setRuleKey(ruleKey).setUpdatedAt(system2.now()))
    };
    RuleDto otherRule = dbTester.rules().insert(rule -> rule.setUpdatedAt(system2.now()));

    CachingRuleFinder underTest = new CachingRuleFinder(dbClient, ruleDescriptionFormatter);

    assertThat(underTest.findAll(RuleQuery.create().withKey(ruleKey)))
      .extracting(CachingRuleFinderIT::toRuleKey)
      .containsExactly(sameRuleKey[1].getKey(), sameRuleKey[0].getKey());
    assertThat(underTest.findAll(RuleQuery.create().withKey(otherRule.getRuleKey())))
      .extracting(CachingRuleFinderIT::toRuleKey)
      .containsExactly(otherRule.getKey());
    assertThat(underTest.findAll(RuleQuery.create().withKey(ruleKey.toLowerCase())))
      .isEmpty();
    assertThat(underTest.findAll(RuleQuery.create().withKey(secure().nextAlphabetic(3))))
      .isEmpty();
  }

  @Test
  public void findAll_returns_all_rules_with_exact_same_configkey_and_order_them_most_recent_first() {
    String configKey = "ABCD";
    RuleDto[] sameConfigKey = {
      dbTester.rules().insert(rule -> rule.setConfigKey(configKey).setUpdatedAt(system2.now())),
      dbTester.rules().insert(rule -> rule.setConfigKey(configKey).setUpdatedAt(system2.now()))
    };
    RuleDto otherRule = dbTester.rules().insert(rule -> rule.setUpdatedAt(system2.now()));

    CachingRuleFinder underTest = new CachingRuleFinder(dbClient, ruleDescriptionFormatter);

    assertThat(underTest.findAll(RuleQuery.create().withConfigKey(configKey)))
      .extracting(CachingRuleFinderIT::toRuleKey)
      .containsExactly(sameConfigKey[1].getKey(), sameConfigKey[0].getKey());
    assertThat(underTest.findAll(RuleQuery.create().withConfigKey(otherRule.getConfigKey())))
      .extracting(CachingRuleFinderIT::toRuleKey)
      .containsExactly(otherRule.getKey());
    assertThat(underTest.findAll(RuleQuery.create().withConfigKey(configKey.toLowerCase())))
      .isEmpty();
    assertThat(underTest.findAll(RuleQuery.create().withConfigKey(secure().nextAlphabetic(3))))
      .isEmpty();
  }

  @Test
  public void findAll_returns_all_rules_which_match_exactly_all_criteria_and_order_then_by_most_recent_first() {
    String repoKey = "ABCD";
    String ruleKey = "EFGH";
    String configKey = "IJKL";
    RuleDto[] rules = {
      dbTester.rules().insert(rule -> rule.setRepositoryKey(repoKey).setRuleKey(ruleKey).setConfigKey(configKey).setUpdatedAt(system2.now())),
      dbTester.rules().insert(rule -> rule.setRuleKey(ruleKey).setConfigKey(configKey).setUpdatedAt(system2.now())),
      dbTester.rules().insert(rule -> rule.setRepositoryKey(repoKey).setConfigKey(configKey).setUpdatedAt(system2.now())),
      dbTester.rules().insert(rule -> rule.setUpdatedAt(system2.now()))
    };
    RuleQuery allQuery = RuleQuery.create().withRepositoryKey(repoKey).withKey(ruleKey).withConfigKey(configKey);
    RuleQuery ruleAndConfigKeyQuery = RuleQuery.create().withKey(ruleKey).withConfigKey(configKey);
    RuleQuery repoAndConfigKeyQuery = RuleQuery.create().withRepositoryKey(repoKey).withConfigKey(configKey);
    RuleQuery repoAndKeyQuery = RuleQuery.create().withRepositoryKey(repoKey).withKey(ruleKey);
    RuleQuery configKeyQuery = RuleQuery.create().withConfigKey(configKey);
    RuleQuery ruleKeyQuery = RuleQuery.create().withKey(ruleKey);
    RuleQuery repoKeyQuery = RuleQuery.create().withRepositoryKey(repoKey);

    CachingRuleFinder underTest = new CachingRuleFinder(dbClient, ruleDescriptionFormatter);

    assertThat(underTest.findAll(allQuery))
      .extracting(CachingRuleFinderIT::toRuleKey)
      .containsExactly(rules[0].getKey());
    assertThat(underTest.findAll(ruleAndConfigKeyQuery))
      .extracting(CachingRuleFinderIT::toRuleKey)
      .containsExactly(rules[1].getKey(), rules[0].getKey());
    assertThat(underTest.findAll(repoAndConfigKeyQuery))
      .extracting(CachingRuleFinderIT::toRuleKey)
      .containsExactly(rules[2].getKey(), rules[0].getKey());
    assertThat(underTest.findAll(repoAndKeyQuery))
      .extracting(CachingRuleFinderIT::toRuleKey)
      .containsExactly(rules[0].getKey());
    assertThat(underTest.findAll(repoKeyQuery))
      .extracting(CachingRuleFinderIT::toRuleKey)
      .containsExactly(rules[2].getKey(), rules[0].getKey());
    assertThat(underTest.findAll(ruleKeyQuery))
      .extracting(CachingRuleFinderIT::toRuleKey)
      .containsExactly(rules[1].getKey(), rules[0].getKey());
    assertThat(underTest.findAll(configKeyQuery))
      .extracting(CachingRuleFinderIT::toRuleKey)
      .containsExactly(rules[2].getKey(), rules[1].getKey(), rules[0].getKey());
  }

  @Test
  public void findDtoByKey_finds_rules() {
    for(RuleDto dto : ruleDtos) {
      assertThat(underTest.findDtoByKey(dto.getKey())).contains(dto);
    }
  }

  @Test
  public void findDtoByUuid_finds_rules() {
    for(RuleDto dto : ruleDtos) {
      assertThat(underTest.findDtoByUuid(dto.getUuid())).contains(dto);
    }
  }

  @Test
  public void findDtoByKey_returns_empty_if_rule_not_found() {
    assertThat(underTest.findDtoByKey(RuleKey.of("unknown", "unknown"))).isEmpty();
  }

  @Test
  public void findDtoByUuid_returns_empty_if_rule_not_found() {
    assertThat(underTest.findDtoByUuid("unknown")).isEmpty();
  }

  @Test
  public void findAll_returns_all_rules() {
    assertThat(underTest.findAll()).containsOnly(ruleDtos);
  }

  private static RuleKey toRuleKey(Rule rule) {
    return RuleKey.of(rule.getRepositoryKey(), rule.getKey());
  }

  private void verifyRule(@Nullable Rule rule, RuleDto ruleDto, RuleParamDto ruleParam) {
    assertThat(rule).isNotNull();

    assertThat(rule.getName()).isEqualTo(ruleDto.getName());
    assertThat(rule.getLanguage()).isEqualTo(ruleDto.getLanguage());
    assertThat(rule.getKey()).isEqualTo(ruleDto.getRuleKey());
    assertThat(rule.getConfigKey()).isEqualTo(ruleDto.getConfigKey());
    assertThat(rule.isTemplate()).isEqualTo(ruleDto.isTemplate());
    assertThat(rule.getCreatedAt().getTime()).isEqualTo(ruleDto.getCreatedAt());
    assertThat(rule.getUpdatedAt().getTime()).isEqualTo(ruleDto.getUpdatedAt());
    assertThat(rule.getRepositoryKey()).isEqualTo(ruleDto.getRepositoryKey());
    assertThat(rule.getSeverity().name()).isEqualTo(ruleDto.getSeverityString());
    assertThat(rule.getSystemTags()).containsExactlyInAnyOrder(ruleDto.getSystemTags().toArray(new String[0]));
    assertThat(rule.getTags()).isEmpty();
    assertThat(rule.getDescription()).isEqualTo(ruleDto.getDefaultRuleDescriptionSection().getContent());

    assertThat(rule.getParams()).hasSize(1);
    org.sonar.api.rules.RuleParam param = rule.getParams().iterator().next();
    assertThat(param.getRule()).isSameAs(rule);
    assertThat(param.getKey()).isEqualTo(ruleParam.getName());
    assertThat(param.getDescription()).isEqualTo(ruleParam.getDescription());
    assertThat(param.getType()).isEqualTo(ruleParam.getType());
    assertThat(param.getDefaultValue()).isEqualTo(ruleParam.getDefaultValue());
  }
}
