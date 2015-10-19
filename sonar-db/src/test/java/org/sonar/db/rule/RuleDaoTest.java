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
package org.sonar.db.rule;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.RowNotFoundException;
import org.sonar.test.DbTests;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;

@Category(DbTests.class)
public class RuleDaoTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  RuleDao underTest = dbTester.getDbClient().ruleDao();

  @Test
  public void selectByKey() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectByKey(dbTester.getSession(), RuleKey.of("NOT", "FOUND")).isPresent()).isFalse();

    Optional<RuleDto> rule = underTest.selectByKey(dbTester.getSession(), RuleKey.of("java", "S001"));
    assertThat(rule.isPresent()).isTrue();
    assertThat(rule.get().getId()).isEqualTo(1);
  }

  @Test
  public void selectById() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectById(55l, dbTester.getSession())).isAbsent();
    Optional<RuleDto> ruleDtoOptional = underTest.selectById(1l, dbTester.getSession());
    assertThat(ruleDtoOptional).isPresent();
    assertThat(ruleDtoOptional.get().getId()).isEqualTo(1);
  }

  @Test
  public void selectOrFailByKey() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    RuleDto rule = underTest.selectOrFailByKey(dbTester.getSession(), RuleKey.of("java", "S001"));
    assertThat(rule.getId()).isEqualTo(1);
  }

  @Test
  public void selectOrFailByKey_fails_if_rule_not_found() {
    thrown.expect(RowNotFoundException.class);
    thrown.expectMessage("Rule with key 'NOT:FOUND' does not exist");

    dbTester.prepareDbUnit(getClass(), "shared.xml");

    underTest.selectOrFailByKey(dbTester.getSession(), RuleKey.of("NOT", "FOUND"));
  }

  @Test
  public void selectByKeys() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectByKeys(dbTester.getSession(), Collections.<RuleKey>emptyList())).isEmpty();
    assertThat(underTest.selectByKeys(dbTester.getSession(), asList(RuleKey.of("NOT", "FOUND")))).isEmpty();

    List<RuleDto> rules = underTest.selectByKeys(dbTester.getSession(), asList(RuleKey.of("java", "S001"), RuleKey.of("java", "OTHER")));
    assertThat(rules).hasSize(1);
    assertThat(rules.get(0).getId()).isEqualTo(1);
  }

  @Test
  public void selectEnabledAndNonManual() {
    dbTester.prepareDbUnit(getClass(), "selectEnabledAndNonManual.xml");
    List<RuleDto> ruleDtos = underTest.selectEnabledAndNonManual(dbTester.getSession());

    assertThat(ruleDtos.size()).isEqualTo(1);
    RuleDto ruleDto = ruleDtos.get(0);
    assertThat(ruleDto.getId()).isEqualTo(1);
    assertThat(ruleDto.getName()).isEqualTo("Avoid Null");
    assertThat(ruleDto.getDescription()).isEqualTo("Should avoid NULL");
    assertThat(ruleDto.getDescriptionFormat()).isEqualTo(RuleDto.Format.HTML);
    assertThat(ruleDto.getStatus()).isEqualTo(RuleStatus.READY);
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("checkstyle");
    assertThat(ruleDto.getNoteData()).isEqualTo("Rule note with accents \u00e9\u00e8\u00e0");
    assertThat(ruleDto.getSubCharacteristicId()).isEqualTo(100);
    assertThat(ruleDto.getDefaultSubCharacteristicId()).isEqualTo(101);
    assertThat(ruleDto.getRemediationFunction()).isEqualTo("LINEAR");
    assertThat(ruleDto.getDefaultRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(ruleDto.getRemediationCoefficient()).isEqualTo("1h");
    assertThat(ruleDto.getDefaultRemediationCoefficient()).isEqualTo("5d");
    assertThat(ruleDto.getRemediationOffset()).isEqualTo("5min");
    assertThat(ruleDto.getDefaultRemediationOffset()).isEqualTo("10h");
    assertThat(ruleDto.getEffortToFixDescription()).isEqualTo("squid.S115.effortToFix");
  }

  @Test
  public void selectAll() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    List<RuleDto> ruleDtos = underTest.selectAll(dbTester.getSession());

    assertThat(ruleDtos).extracting("id").containsOnly(1, 2);
  }

  @Test
  public void insert() throws Exception {
    dbTester.getDbClient().ruleDao().insert(dbTester.getSession(), RuleTesting.newDto(RuleKey.of("java", "S001")).setConfigKey(null));
    dbTester.getDbClient().ruleDao().insert(dbTester.getSession(), RuleTesting.newDto(RuleKey.of("java", "S002")).setConfigKey("I002"));
    dbTester.getSession().commit();

    List<Map<String, Object>> rows = dbTester.select("select plugin_rule_key as \"ruleKey\" from rules order by plugin_rule_key");
    assertThat(rows).hasSize(2);
    assertThat(rows.get(0).get("ruleKey")).isEqualTo("S001");
    assertThat(rows.get(1).get("ruleKey")).isEqualTo("S002");
  }

  @Test
  public void selectEnabledAndNonManual_with_ResultHandler() {
    dbTester.prepareDbUnit(getClass(), "selectEnabledAndNonManual.xml");

    final List<RuleDto> rules = new ArrayList<>();
    ResultHandler resultHandler = new ResultHandler() {
      @Override
      public void handleResult(ResultContext resultContext) {
        rules.add((RuleDto) resultContext.getResultObject());
      }
    };
    underTest.selectEnabledAndNonManual(dbTester.getSession(), resultHandler);

    assertThat(rules.size()).isEqualTo(1);
    RuleDto ruleDto = rules.get(0);
    assertThat(ruleDto.getId()).isEqualTo(1);
  }
}
