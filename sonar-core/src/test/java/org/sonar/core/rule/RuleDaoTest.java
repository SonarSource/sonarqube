/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import com.google.common.collect.ImmutableList;
import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.Rule;
import org.sonar.api.utils.DateUtils;
import org.sonar.check.Cardinality;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Date;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class RuleDaoTest extends AbstractDaoTestCase {

  private static RuleDao dao;

  @Before
  public void createDao() throws Exception {
    dao = new RuleDao(getMyBatis());
  }

  @Test
  public void testSelectAll() throws Exception {
    setupData("selectAll");
    List<RuleDto> ruleDtos = dao.selectAll();

    assertThat(ruleDtos.size()).isEqualTo(1);
    RuleDto ruleDto = ruleDtos.get(0);
    assertThat(ruleDto.getId()).isEqualTo(1);
    assertThat(ruleDto.getName()).isEqualTo("Avoid Null");
    assertThat(ruleDto.getDescription()).isEqualTo("Should avoid NULL");
    assertThat(ruleDto.getStatus()).isEqualTo(Rule.STATUS_READY);
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("checkstyle");
    assertThat(ruleDto.getNoteData()).isEqualTo("Rule note with accents \u00e9\u00e8\u00e0");
  }

  @Test
  public void testSelectById() throws Exception {
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
  public void testSelectNonManual() throws Exception {
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
      .setUpdatedAt(DateUtils.parseDate("2013-12-17"))
      .setNoteData("My note")
      .setNoteUserLogin("admin")
      .setNoteCreatedAt(DateUtils.parseDate("2013-12-19"))
      .setNoteUpdatedAt(DateUtils.parseDate("2013-12-20"));

    dao.update(ruleToUpdate);

    checkTables("update", "rules");
  }

  @Test
  public void testInsert() {
    setupData("insert");
    RuleDto ruleToInsert = new RuleDto();
    String newRuleKey = "NewRuleKey";
    String newRepositoryKey = "plugin";
    String newName = "new name";
    String newDescription = "new description";
    String newStatus = Rule.STATUS_DEPRECATED;
    String newConfigKey = "NewConfigKey";
    String newSeverity = Severity.INFO;
    Cardinality newCardinality = Cardinality.MULTIPLE;
    String newLanguage = "dart";
    Date updatedAt = new Date();
    Integer newParentId = 3;

    ruleToInsert.setRuleKey(newRuleKey);
    ruleToInsert.setRepositoryKey(newRepositoryKey);
    ruleToInsert.setName(newName);
    ruleToInsert.setDescription(newDescription);
    ruleToInsert.setStatus(newStatus);
    ruleToInsert.setConfigKey(newConfigKey);
    ruleToInsert.setSeverity(Severity.INFO);
    ruleToInsert.setCardinality(newCardinality);
    ruleToInsert.setLanguage(newLanguage);
    ruleToInsert.setUpdatedAt(updatedAt);
    ruleToInsert.setParentId(newParentId);
    dao.insert(ruleToInsert);

    List<RuleDto> ruleDtos = dao.selectAll();
    assertThat(ruleDtos.size()).isEqualTo(1);
    RuleDto insertedRule = ruleDtos.get(0);
    // First inserted rule, auto generated ID should be 1
    assertThat(insertedRule.getId()).isEqualTo(1);
    assertThat(insertedRule.getRuleKey()).isEqualTo(newRuleKey);
    assertThat(insertedRule.getRepositoryKey()).isEqualTo(newRepositoryKey);
    assertThat(insertedRule.getName()).isEqualTo(newName);
    assertThat(insertedRule.getDescription()).isEqualTo(newDescription);
    assertThat(insertedRule.getStatus()).isEqualTo(newStatus);
    assertThat(insertedRule.getConfigKey()).isEqualTo(newConfigKey);
    assertThat(insertedRule.getSeverityString()).isEqualTo(newSeverity);
    assertThat(insertedRule.getCardinality()).isEqualTo(newCardinality);
    assertThat(insertedRule.getLanguage()).isEqualTo(newLanguage);
    assertThat(insertedRule.getParentId()).isEqualTo(newParentId);
  }

  @Test
  public void testInsertAll() {
    setupData("insert");
    RuleDto ruleToInsert1 = new RuleDto();
    String newRuleKey = "NewRuleKey1";
    String newRepositoryKey = "plugin1";
    String newName = "new name1";
    String newDescription = "new description1";
    String newStatus = Rule.STATUS_DEPRECATED;
    String newConfigKey = "NewConfigKey1";
    String newSeverity = Severity.INFO;
    Cardinality newCardinality = Cardinality.MULTIPLE;
    String newLanguage = "dart";
    Date createdAt = new Date();
    Date updatedAt = new Date();

    ruleToInsert1.setRuleKey(newRuleKey);
    ruleToInsert1.setRepositoryKey(newRepositoryKey);
    ruleToInsert1.setName(newName);
    ruleToInsert1.setDescription(newDescription);
    ruleToInsert1.setStatus(newStatus);
    ruleToInsert1.setConfigKey(newConfigKey);
    ruleToInsert1.setSeverity(newSeverity);
    ruleToInsert1.setCardinality(newCardinality);
    ruleToInsert1.setLanguage(newLanguage);
    ruleToInsert1.setCreatedAt(createdAt);
    ruleToInsert1.setUpdatedAt(updatedAt);

    RuleDto ruleToInsert2 = new RuleDto();
    String newRuleKey1 = "NewRuleKey2";
    String newRepositoryKey1 = "plugin2";
    String newName1 = "new name2";
    String newDescription1 = "new description2";
    String newStatus1 = Rule.STATUS_DEPRECATED;
    String newConfigKey1 = "NewConfigKey2";
    String newSeverity1 = Severity.INFO;
    Cardinality newCardinality1 = Cardinality.MULTIPLE;
    String newLanguage1 = "dart";
    Date createdAt1 = new Date();
    Date updatedAt1 = new Date();

    ruleToInsert2.setRuleKey(newRuleKey1);
    ruleToInsert2.setRepositoryKey(newRepositoryKey1);
    ruleToInsert2.setName(newName1);
    ruleToInsert2.setDescription(newDescription1);
    ruleToInsert2.setStatus(newStatus1);
    ruleToInsert2.setConfigKey(newConfigKey1);
    ruleToInsert2.setSeverity(newSeverity1);
    ruleToInsert2.setCardinality(newCardinality1);
    ruleToInsert2.setLanguage(newLanguage1);
    ruleToInsert2.setCreatedAt(createdAt1);
    ruleToInsert2.setUpdatedAt(updatedAt1);

    dao.insert(ImmutableList.of(ruleToInsert1, ruleToInsert2));

    List<RuleDto> ruleDtos = dao.selectAll();
    assertThat(ruleDtos.size()).isEqualTo(2);
    assertThat(ruleDtos.get(0).getId()).isEqualTo(1);
    assertThat(ruleDtos.get(1).getId()).isEqualTo(2);
  }

  @Test
  public void testSelectParameters() throws Exception {
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
  public void testSelectParamsForRule() throws Exception {
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
}
