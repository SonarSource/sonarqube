/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.RowNotFoundException;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;


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
  public void selectByIds() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectByIds(dbTester.getSession(), asList(1))).hasSize(1);
    assertThat(underTest.selectByIds(dbTester.getSession(), asList(1,2))).hasSize(2);
    assertThat(underTest.selectByIds(dbTester.getSession(), asList(1,2,3))).hasSize(2);

    assertThat(underTest.selectByIds(dbTester.getSession(), asList(123))).isEmpty();
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
  public void selectEnabled() {
    dbTester.prepareDbUnit(getClass(), "selectEnabled.xml");
    List<RuleDto> ruleDtos = underTest.selectEnabled(dbTester.getSession());

    assertThat(ruleDtos.size()).isEqualTo(1);
    RuleDto ruleDto = ruleDtos.get(0);
    assertThat(ruleDto.getId()).isEqualTo(1);
    assertThat(ruleDto.getName()).isEqualTo("Avoid Null");
    assertThat(ruleDto.getDescription()).isEqualTo("Should avoid NULL");
    assertThat(ruleDto.getDescriptionFormat()).isEqualTo(RuleDto.Format.HTML);
    assertThat(ruleDto.getStatus()).isEqualTo(RuleStatus.READY);
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("checkstyle");
    assertThat(ruleDto.getNoteData()).isEqualTo("Rule note with accents \u00e9\u00e8\u00e0");
    assertThat(ruleDto.getRemediationFunction()).isEqualTo("LINEAR");
    assertThat(ruleDto.getDefaultRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(ruleDto.getRemediationGapMultiplier()).isEqualTo("1h");
    assertThat(ruleDto.getDefaultRemediationGapMultiplier()).isEqualTo("5d");
    assertThat(ruleDto.getRemediationBaseEffort()).isEqualTo("5min");
    assertThat(ruleDto.getDefaultRemediationBaseEffort()).isEqualTo("10h");
    assertThat(ruleDto.getGapDescription()).isEqualTo("squid.S115.effortToFix");
  }

  @Test
  public void selectAll() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    List<RuleDto> ruleDtos = underTest.selectAll(dbTester.getSession());

    assertThat(ruleDtos).extracting("id").containsOnly(1, 2, 10);
  }

  @Test
  public void selectEnabled_with_ResultHandler() {
    dbTester.prepareDbUnit(getClass(), "selectEnabled.xml");

    final List<RuleDto> rules = new ArrayList<>();
    ResultHandler resultHandler = new ResultHandler() {
      @Override
      public void handleResult(ResultContext resultContext) {
        rules.add((RuleDto) resultContext.getResultObject());
      }
    };
    underTest.selectEnabled(dbTester.getSession(), resultHandler);

    assertThat(rules.size()).isEqualTo(1);
    RuleDto ruleDto = rules.get(0);
    assertThat(ruleDto.getId()).isEqualTo(1);
  }

  @Test
  public void select_by_query() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectByQuery(dbTester.getSession(), RuleQuery.create())).hasSize(2);
    assertThat(underTest.selectByQuery(dbTester.getSession(), RuleQuery.create().withKey("S001"))).hasSize(1);
    assertThat(underTest.selectByQuery(dbTester.getSession(), RuleQuery.create().withConfigKey("S1"))).hasSize(1);
    assertThat(underTest.selectByQuery(dbTester.getSession(), RuleQuery.create().withRepositoryKey("java"))).hasSize(2);
    assertThat(underTest.selectByQuery(dbTester.getSession(),
      RuleQuery.create().withKey("S001").withConfigKey("S1").withRepositoryKey("java"))).hasSize(1);
  }

  @Test
  public void insert() throws Exception {
    RuleDto newRule = new RuleDto()
      .setRuleKey("NewRuleKey")
      .setRepositoryKey("plugin")
      .setName("new name")
      .setDescription("new description")
      .setDescriptionFormat(RuleDto.Format.MARKDOWN)
      .setStatus(RuleStatus.DEPRECATED)
      .setConfigKey("NewConfigKey")
      .setSeverity(Severity.INFO)
      .setIsTemplate(true)
      .setLanguage("dart")
      .setTemplateId(3)
      .setNoteData("My note")
      .setNoteUserLogin("admin")
      .setNoteCreatedAt(DateUtils.parseDate("2013-12-19"))
      .setNoteUpdatedAt(DateUtils.parseDate("2013-12-20"))
      .setRemediationFunction(DebtRemediationFunction.Type.LINEAR.toString())
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.toString())
      .setRemediationGapMultiplier("1h")
      .setDefaultRemediationGapMultiplier("5d")
      .setRemediationBaseEffort("5min")
      .setDefaultRemediationBaseEffort("10h")
      .setGapDescription("squid.S115.effortToFix")
      .setTags(newHashSet("tag1", "tag2"))
      .setSystemTags(newHashSet("systag1", "systag2"))
      .setType(RuleType.BUG)
      .setCreatedAt(1500000000000L)
      .setUpdatedAt(2000000000000L);
    underTest.insert(dbTester.getSession(), newRule);
    dbTester.getSession().commit();

    RuleDto ruleDto = underTest.selectOrFailByKey(dbTester.getSession(), RuleKey.of("plugin", "NewRuleKey"));
    assertThat(ruleDto.getId()).isNotNull();
    assertThat(ruleDto.getName()).isEqualTo("new name");
    assertThat(ruleDto.getDescription()).isEqualTo("new description");
    assertThat(ruleDto.getDescriptionFormat()).isEqualTo(RuleDto.Format.MARKDOWN);
    assertThat(ruleDto.getStatus()).isEqualTo(RuleStatus.DEPRECATED);
    assertThat(ruleDto.getRuleKey()).isEqualTo("NewRuleKey");
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("plugin");
    assertThat(ruleDto.getConfigKey()).isEqualTo("NewConfigKey");
    assertThat(ruleDto.getSeverity()).isEqualTo(0);
    assertThat(ruleDto.getLanguage()).isEqualTo("dart");
    assertThat(ruleDto.isTemplate()).isTrue();
    assertThat(ruleDto.getTemplateId()).isEqualTo(3);
    assertThat(ruleDto.getNoteData()).isEqualTo("My note");
    assertThat(ruleDto.getNoteUserLogin()).isEqualTo("admin");
    assertThat(ruleDto.getNoteCreatedAt()).isNotNull();
    assertThat(ruleDto.getNoteUpdatedAt()).isNotNull();
    assertThat(ruleDto.getRemediationFunction()).isEqualTo("LINEAR");
    assertThat(ruleDto.getDefaultRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(ruleDto.getRemediationGapMultiplier()).isEqualTo("1h");
    assertThat(ruleDto.getDefaultRemediationGapMultiplier()).isEqualTo("5d");
    assertThat(ruleDto.getRemediationBaseEffort()).isEqualTo("5min");
    assertThat(ruleDto.getDefaultRemediationBaseEffort()).isEqualTo("10h");
    assertThat(ruleDto.getGapDescription()).isEqualTo("squid.S115.effortToFix");
    assertThat(ruleDto.getTags()).containsOnly("tag1", "tag2");
    assertThat(ruleDto.getSystemTags()).containsOnly("systag1", "systag2");
    assertThat(ruleDto.getType()).isEqualTo(RuleType.BUG.getDbConstant());
    assertThat(ruleDto.getCreatedAt()).isEqualTo(1500000000000L);
    assertThat(ruleDto.getUpdatedAt()).isEqualTo(2000000000000L);
  }

  @Test
  public void update() {
    dbTester.prepareDbUnit(getClass(), "update.xml");

    RuleDto ruleToUpdate = new RuleDto()
      .setId(1)
      .setRuleKey("NewRuleKey")
      .setRepositoryKey("plugin")
      .setName("new name")
      .setDescription("new description")
      .setDescriptionFormat(RuleDto.Format.MARKDOWN)
      .setStatus(RuleStatus.DEPRECATED)
      .setConfigKey("NewConfigKey")
      .setSeverity(Severity.INFO)
      .setIsTemplate(true)
      .setLanguage("dart")
      .setTemplateId(3)
      .setNoteData("My note")
      .setNoteUserLogin("admin")
      .setNoteCreatedAt(DateUtils.parseDate("2013-12-19"))
      .setNoteUpdatedAt(DateUtils.parseDate("2013-12-20"))
      .setRemediationFunction(DebtRemediationFunction.Type.LINEAR.toString())
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.toString())
      .setRemediationGapMultiplier("1h")
      .setDefaultRemediationGapMultiplier("5d")
      .setRemediationBaseEffort("5min")
      .setDefaultRemediationBaseEffort("10h")
      .setGapDescription("squid.S115.effortToFix")
      .setTags(newHashSet("tag1", "tag2"))
      .setSystemTags(newHashSet("systag1", "systag2"))
      .setType(RuleType.BUG)
      .setUpdatedAt(2000000000000L);

    underTest.update(dbTester.getSession(), ruleToUpdate);
    dbTester.getSession().commit();

    RuleDto ruleDto = underTest.selectOrFailByKey(dbTester.getSession(), RuleKey.of("plugin", "NewRuleKey"));
    assertThat(ruleDto.getName()).isEqualTo("new name");
    assertThat(ruleDto.getDescription()).isEqualTo("new description");
    assertThat(ruleDto.getDescriptionFormat()).isEqualTo(RuleDto.Format.MARKDOWN);
    assertThat(ruleDto.getStatus()).isEqualTo(RuleStatus.DEPRECATED);
    assertThat(ruleDto.getRuleKey()).isEqualTo("NewRuleKey");
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("plugin");
    assertThat(ruleDto.getConfigKey()).isEqualTo("NewConfigKey");
    assertThat(ruleDto.getSeverity()).isEqualTo(0);
    assertThat(ruleDto.getLanguage()).isEqualTo("dart");
    assertThat(ruleDto.isTemplate()).isTrue();
    assertThat(ruleDto.getTemplateId()).isEqualTo(3);
    assertThat(ruleDto.getNoteData()).isEqualTo("My note");
    assertThat(ruleDto.getNoteUserLogin()).isEqualTo("admin");
    assertThat(ruleDto.getNoteCreatedAt()).isNotNull();
    assertThat(ruleDto.getNoteUpdatedAt()).isNotNull();
    assertThat(ruleDto.getRemediationFunction()).isEqualTo("LINEAR");
    assertThat(ruleDto.getDefaultRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(ruleDto.getRemediationGapMultiplier()).isEqualTo("1h");
    assertThat(ruleDto.getDefaultRemediationGapMultiplier()).isEqualTo("5d");
    assertThat(ruleDto.getRemediationBaseEffort()).isEqualTo("5min");
    assertThat(ruleDto.getDefaultRemediationBaseEffort()).isEqualTo("10h");
    assertThat(ruleDto.getGapDescription()).isEqualTo("squid.S115.effortToFix");
    assertThat(ruleDto.getTags()).containsOnly("tag1", "tag2");
    assertThat(ruleDto.getSystemTags()).containsOnly("systag1", "systag2");
    assertThat(ruleDto.getType()).isEqualTo(RuleType.BUG.getDbConstant());
    assertThat(ruleDto.getCreatedAt()).isEqualTo(1500000000000L);
    assertThat(ruleDto.getUpdatedAt()).isEqualTo(2000000000000L);
  }

  @Test
  public void select_parameters_by_rule_key() {
    dbTester.prepareDbUnit(getClass(), "select_parameters_by_rule_key.xml");
    List<RuleParamDto> ruleDtos = underTest.selectRuleParamsByRuleKey(dbTester.getSession(), RuleKey.of("checkstyle", "AvoidNull"));

    assertThat(ruleDtos.size()).isEqualTo(1);
    RuleParamDto ruleDto = ruleDtos.get(0);
    assertThat(ruleDto.getId()).isEqualTo(1);
    assertThat(ruleDto.getName()).isEqualTo("myParameter");
    assertThat(ruleDto.getDescription()).isEqualTo("My Parameter");
    assertThat(ruleDto.getType()).isEqualTo("plop");
    assertThat(ruleDto.getRuleId()).isEqualTo(1);
  }

  @Test
  public void select_parameters_by_rule_keys() {
    dbTester.prepareDbUnit(getClass(), "select_parameters_by_rule_key.xml");

    assertThat(underTest.selectRuleParamsByRuleKeys(dbTester.getSession(),
      Arrays.asList(RuleKey.of("checkstyle", "AvoidNull"), RuleKey.of("unused", "Unused"))
    )).hasSize(2);

    assertThat(underTest.selectRuleParamsByRuleKeys(dbTester.getSession(),
      singletonList(RuleKey.of("unknown", "Unknown"))
    )).isEmpty();
  }

  @Test
  public void insert_parameter() {
    dbTester.prepareDbUnit(getClass(), "insert_parameter.xml");
    RuleDto rule1 = underTest.selectOrFailByKey(dbTester.getSession(), RuleKey.of("plugin", "NewRuleKey"));

    RuleParamDto param = RuleParamDto.createFor(rule1)
      .setName("max")
      .setType("INTEGER")
      .setDefaultValue("30")
      .setDescription("My Parameter");

    underTest.insertRuleParam(dbTester.getSession(), rule1, param);
    dbTester.getSession().commit();

    dbTester.assertDbUnit(getClass(), "insert_parameter-result.xml", "rules_parameters");
  }

  @Test
  public void update_parameter() {
    dbTester.prepareDbUnit(getClass(), "update_parameter.xml");

    RuleDto rule1 = underTest.selectOrFailByKey(dbTester.getSession(), RuleKey.of("checkstyle", "AvoidNull"));

    List<RuleParamDto> params = underTest.selectRuleParamsByRuleKey(dbTester.getSession(), rule1.getKey());
    assertThat(params).hasSize(1);

    RuleParamDto param = Iterables.getFirst(params, null);
    param
      .setName("format")
      .setType("STRING")
      .setDefaultValue("^[a-z]+(\\.[a-z][a-z0-9]*)*$")
      .setDescription("Regular expression used to check the package names against.");

    underTest.updateRuleParam(dbTester.getSession(), rule1, param);
    dbTester.getSession().commit();

    dbTester.assertDbUnit(getClass(), "update_parameter-result.xml", "rules_parameters");
  }

  @Test
  public void delete_parameter() {
    dbTester.prepareDbUnit(getClass(), "select_parameters_by_rule_key.xml");
    assertThat(underTest.selectRuleParamsByRuleKey(dbTester.getSession(), RuleKey.of("checkstyle", "AvoidNull"))).hasSize(1);

    underTest.deleteRuleParam(dbTester.getSession(), 1);
    dbTester.getSession().commit();

    assertThat(underTest.selectRuleParamsByRuleKey(dbTester.getSession(), RuleKey.of("checkstyle", "AvoidNull"))).isEmpty();
  }
}
