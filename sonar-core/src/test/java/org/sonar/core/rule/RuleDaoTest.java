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
package org.sonar.core.rule;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.Rule;
import org.sonar.api.utils.DateUtils;
import org.sonar.check.Cardinality;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class RuleDaoTest extends AbstractDaoTestCase {

  private static RuleDao dao;

  @Before
  public void createDao() throws Exception {
    dao = new RuleDao(getMyBatis());
  }

  @Test
  public void select_all() throws Exception {
    setupData("selectAll");
    List<RuleDto> ruleDtos = dao.selectAll();

    assertThat(ruleDtos).hasSize(1);

    RuleDto ruleDto = ruleDtos.get(0);
    assertThat(ruleDto.getId()).isEqualTo(1);
    assertThat(ruleDto.getName()).isEqualTo("Avoid Null");
    assertThat(ruleDto.getDescription()).isEqualTo("Should avoid NULL");
    assertThat(ruleDto.getStatus()).isEqualTo(Rule.STATUS_READY);
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("checkstyle");
    assertThat(ruleDto.getNoteData()).isEqualTo("Rule note with accents \u00e9\u00e8\u00e0");
    assertThat(ruleDto.getCharacteristicId()).isEqualTo(100);
    assertThat(ruleDto.getDefaultCharacteristicId()).isEqualTo(101);
    assertThat(ruleDto.getRemediationFunction()).isEqualTo("linear");
    assertThat(ruleDto.getDefaultRemediationFunction()).isEqualTo("linear_offset");
    assertThat(ruleDto.getRemediationFactor()).isEqualTo("1h");
    assertThat(ruleDto.getDefaultRemediationFactor()).isEqualTo("5d");
    assertThat(ruleDto.getRemediationOffset()).isEqualTo("5min");
    assertThat(ruleDto.getDefaultRemediationOffset()).isEqualTo("10h");
    assertThat(ruleDto.getEffortToFixDescription()).isEqualTo("squid.S115.effortToFix");
  }

  @Test
  public void select_enables_and_non_manual() throws Exception {
    setupData("select_enables_and_non_manual");
    List<RuleDto> ruleDtos = dao.selectEnablesAndNonManual();

    assertThat(ruleDtos.size()).isEqualTo(1);
    RuleDto ruleDto = ruleDtos.get(0);
    assertThat(ruleDto.getId()).isEqualTo(1);
    assertThat(ruleDto.getName()).isEqualTo("Avoid Null");
    assertThat(ruleDto.getDescription()).isEqualTo("Should avoid NULL");
    assertThat(ruleDto.getStatus()).isEqualTo(Rule.STATUS_READY);
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("checkstyle");
    assertThat(ruleDto.getNoteData()).isEqualTo("Rule note with accents \u00e9\u00e8\u00e0");
    assertThat(ruleDto.getCharacteristicId()).isEqualTo(100);
    assertThat(ruleDto.getDefaultCharacteristicId()).isEqualTo(101);
    assertThat(ruleDto.getRemediationFunction()).isEqualTo("LINEAR");
    assertThat(ruleDto.getDefaultRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(ruleDto.getRemediationFactor()).isEqualTo("1h");
    assertThat(ruleDto.getDefaultRemediationFactor()).isEqualTo("5d");
    assertThat(ruleDto.getRemediationOffset()).isEqualTo("5min");
    assertThat(ruleDto.getDefaultRemediationOffset()).isEqualTo("10h");
    assertThat(ruleDto.getEffortToFixDescription()).isEqualTo("squid.S115.effortToFix");
  }

  @Test
  public void select_by_id() throws Exception {
    setupData("selectById");
    RuleDto ruleDto = dao.selectById(2);

    assertThat(ruleDto.getId()).isEqualTo(2);
    assertThat(ruleDto.getName()).isEqualTo("Avoid Null");
    assertThat(ruleDto.getDescription()).isEqualTo("Should avoid NULL");
    assertThat(ruleDto.getStatus()).isEqualTo(Rule.STATUS_READY);
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("checkstyle");
  }

  @Test
  public void select_by_name() throws Exception {
    setupData("select_by_name");
    RuleDto ruleDto = dao.selectByName("Avoid Null");

    assertThat(ruleDto.getId()).isEqualTo(2);
    assertThat(ruleDto.getName()).isEqualTo("Avoid Null");
    assertThat(ruleDto.getDescription()).isEqualTo("Should avoid NULL");
    assertThat(ruleDto.getStatus()).isEqualTo(Rule.STATUS_READY);
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("checkstyle");
  }

  @Test
  public void select_non_manual() throws Exception {
    setupData("selectNonManual");
    SqlSession session = getMyBatis().openSession();
    List<RuleDto> ruleDtos = dao.selectNonManual(session);
    session.commit();
    session.close();

    assertThat(ruleDtos.size()).isEqualTo(1);
    RuleDto ruleDto = ruleDtos.get(0);
    assertThat(ruleDto.getId()).isEqualTo(1);
    assertThat(ruleDto.getName()).isEqualTo("Avoid Null");
    assertThat(ruleDto.getDescription()).isEqualTo("Should avoid NULL");
    assertThat(ruleDto.getStatus()).isEqualTo(Rule.STATUS_READY);
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("checkstyle");
  }

  @Test
  public void select_by_sub_characteristic_id(){
    setupData("select_by_sub_characteristic_id");

    // Rules from sub characteristic
    List<RuleDto> ruleDtos = dao.selectBySubCharacteristicId(3);
    assertThat(ruleDtos).hasSize(3);
    assertThat(idsFromRuleDtos(ruleDtos)).containsExactly(2, 4, 5);

    // Nothing on root characteristic
    ruleDtos = dao.selectBySubCharacteristicId(1);
    assertThat(ruleDtos).isEmpty();

    // Rules from disabled characteristic
    ruleDtos = dao.selectBySubCharacteristicId(11);
    assertThat(idsFromRuleDtos(ruleDtos)).containsExactly(3);
  }

  @Test
  public void update() {
    setupData("update");

    RuleDto ruleToUpdate = new RuleDto()
      .setId(1)
      .setRuleKey("NewRuleKey")
      .setRepositoryKey("plugin")
      .setName("new name")
      .setDescription("new description")
      .setStatus(Rule.STATUS_DEPRECATED)
      .setConfigKey("NewConfigKey")
      .setSeverity(Severity.INFO)
      .setCardinality(Cardinality.MULTIPLE)
      .setLanguage("dart")
      .setParentId(3)
      .setNoteData("My note")
      .setNoteUserLogin("admin")
      .setNoteCreatedAt(DateUtils.parseDate("2013-12-19"))
      .setNoteUpdatedAt(DateUtils.parseDate("2013-12-20"))
      .setCharacteristicId(100)
      .setDefaultCharacteristicId(101)
      .setRemediationFunction("linear")
      .setDefaultRemediationFunction("linear_offset")
      .setRemediationFactor("1h")
      .setDefaultRemediationFactor("5d")
      .setRemediationOffset("5min")
      .setDefaultRemediationOffset("10h")
      .setEffortToFixDescription("squid.S115.effortToFix")
      .setUpdatedAt(DateUtils.parseDate("2013-12-17"));

    dao.update(ruleToUpdate);

    checkTables("update", "rules");
  }

  @Test
  public void insert() {
    setupData("empty");

    RuleDto ruleToInsert = new RuleDto()
      .setId(1)
      .setRuleKey("NewRuleKey")
      .setRepositoryKey("plugin")
      .setName("new name")
      .setDescription("new description")
      .setStatus(Rule.STATUS_DEPRECATED)
      .setConfigKey("NewConfigKey")
      .setSeverity(Severity.INFO)
      .setCardinality(Cardinality.MULTIPLE)
      .setLanguage("dart")
      .setParentId(3)
      .setCharacteristicId(100)
      .setDefaultCharacteristicId(101)
      .setRemediationFunction("linear")
      .setDefaultRemediationFunction("linear_offset")
      .setRemediationFactor("1h")
      .setDefaultRemediationFactor("5d")
      .setRemediationOffset("5min")
      .setDefaultRemediationOffset("10h")
      .setEffortToFixDescription("squid.S115.effortToFix")
      .setCreatedAt(DateUtils.parseDate("2013-12-16"))
      .setUpdatedAt(DateUtils.parseDate("2013-12-17"));

    dao.insert(ruleToInsert);

    checkTables("insert", "rules");
  }

  @Test
  public void insert_all() {
    setupData("empty");

    RuleDto ruleToInsert1 = new RuleDto()
      .setId(1)
      .setRuleKey("NewRuleKey")
      .setRepositoryKey("plugin")
      .setName("new name")
      .setDescription("new description")
      .setStatus(Rule.STATUS_DEPRECATED)
      .setConfigKey("NewConfigKey")
      .setSeverity(Severity.INFO)
      .setCardinality(Cardinality.MULTIPLE)
      .setLanguage("dart")
      .setParentId(3)
      .setCharacteristicId(100)
      .setDefaultCharacteristicId(101)
      .setRemediationFunction("linear")
      .setDefaultRemediationFunction("linear_offset")
      .setRemediationFactor("1h")
      .setDefaultRemediationFactor("5d")
      .setRemediationOffset("5min")
      .setDefaultRemediationOffset("10h")
      .setEffortToFixDescription("squid.S115.effortToFix")
      .setCreatedAt(DateUtils.parseDate("2013-12-16"))
      .setUpdatedAt(DateUtils.parseDate("2013-12-17"));

    RuleDto ruleToInsert2 = new RuleDto()
      .setId(2)
      .setRuleKey("NewRuleKey2")
      .setRepositoryKey("plugin2")
      .setName("new name2")
      .setDescription("new description2")
      .setStatus(Rule.STATUS_BETA)
      .setConfigKey("NewConfigKey2")
      .setSeverity(Severity.MAJOR)
      .setCardinality(Cardinality.SINGLE)
      .setLanguage("js")
      .setParentId(null)
      .setCharacteristicId(102)
      .setDefaultCharacteristicId(103)
      .setRemediationFunction("linear_offset")
      .setDefaultRemediationFunction("linear")
      .setRemediationFactor("5d")
      .setDefaultRemediationFactor("1h")
      .setRemediationOffset("10h")
      .setDefaultRemediationOffset("5min")
      .setEffortToFixDescription("squid.S115.effortToFix2")
      .setCreatedAt(DateUtils.parseDate("2013-12-14"))
      .setUpdatedAt(DateUtils.parseDate("2013-12-15"));

    dao.insert(ImmutableList.of(ruleToInsert1, ruleToInsert2));

    checkTables("insert_all", "rules");
  }

  @Test
  public void select_parameters() throws Exception {
    setupData("selectParameters");
    List<RuleParamDto> ruleDtos = dao.selectParameters();

    assertThat(ruleDtos.size()).isEqualTo(1);
    RuleParamDto ruleDto = ruleDtos.get(0);
    assertThat(ruleDto.getId()).isEqualTo(1);
    assertThat(ruleDto.getName()).isEqualTo("myParameter");
    assertThat(ruleDto.getDescription()).isEqualTo("My Parameter");
    assertThat(ruleDto.getType()).isEqualTo("plop");
    assertThat(ruleDto.getDefaultValue()).isEqualTo("plouf");
  }

  @Test
  public void select_params_for_rule() throws Exception {
    setupData("selectParamsForRule");
    int ruleId = 1;
    List<RuleParamDto> ruleDtos = dao.selectParameters(ruleId);

    assertThat(ruleDtos.size()).isEqualTo(1);
    RuleParamDto ruleDto = ruleDtos.get(0);
    assertThat(ruleDto.getId()).isEqualTo(1);
    assertThat(ruleDto.getName()).isEqualTo("myParameter");
    assertThat(ruleDto.getDescription()).isEqualTo("My Parameter");
    assertThat(ruleDto.getType()).isEqualTo("plop");
    assertThat(ruleDto.getRuleId()).isEqualTo(ruleId);
  }

  @Test
  public void insert_parameter() {
    setupData("insert_parameter");

    RuleParamDto param = new RuleParamDto()
      .setRuleId(1)
      .setName("max")
      .setType("INTEGER")
      .setDefaultValue("30")
      .setDescription("My Parameter");

    dao.insert(param);

    checkTables("insert_parameter", "rules_parameters");
  }

  @Test
  public void update_parameter() {
    setupData("update_parameter");

    RuleParamDto param = new RuleParamDto()
      .setId(1)
      .setName("format")
      .setType("STRING")
      .setDefaultValue("^[a-z]+(\\.[a-z][a-z0-9]*)*$")
      .setDescription("Regular expression used to check the package names against.");

    dao.update(param);

    checkTables("update_parameter", "rules_parameters");
  }

  private List<Integer> idsFromRuleDtos(List<RuleDto> ruleDtos){
    return newArrayList(Iterables.transform(ruleDtos, new Function<RuleDto, Integer>() {
      @Override
      public Integer apply(RuleDto input) {
        return input.getId();
      }
    }));
  }
}
