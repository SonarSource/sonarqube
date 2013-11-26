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
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rules.Rule;
import org.sonar.check.Cardinality;
import org.sonar.check.Priority;
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
    assertThat(ruleDto.getId()).isEqualTo(1L);
    assertThat(ruleDto.getName()).isEqualTo("Avoid Null");
    assertThat(ruleDto.getDescription()).isEqualTo("Should avoid NULL");
    assertThat(ruleDto.getStatus()).isEqualTo(Rule.STATUS_READY);
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("checkstyle");
  }

  @Test
  public void testSelectById() throws Exception {
    setupData("selectById");
    RuleDto ruleDto = dao.selectById(2L);

    assertThat(ruleDto.getId()).isEqualTo(2L);
    assertThat(ruleDto.getName()).isEqualTo("Avoid Null");
    assertThat(ruleDto.getDescription()).isEqualTo("Should avoid NULL");
    assertThat(ruleDto.getStatus()).isEqualTo(Rule.STATUS_READY);
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("checkstyle");
  }

  @Test
  public void testSelectNonManual() throws Exception {
    setupData("selectNonManual");
    List<RuleDto> ruleDtos = dao.selectNonManual();

    assertThat(ruleDtos.size()).isEqualTo(1);
    RuleDto ruleDto = ruleDtos.get(0);
    assertThat(ruleDto.getId()).isEqualTo(1L);
    assertThat(ruleDto.getName()).isEqualTo("Avoid Null");
    assertThat(ruleDto.getDescription()).isEqualTo("Should avoid NULL");
    assertThat(ruleDto.getStatus()).isEqualTo(Rule.STATUS_READY);
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("checkstyle");
  }

  @Test
  public void testUpdate() {
    setupData("update");
    RuleDto ruleToUpdate = new RuleDto();
    final long ruleId = 1L;
    String newRuleKey = "NewRuleKey";
    String newRepositoryKey = "plugin";
    String newName = "new name";
    String newDescription = "new description";
    String newStatus = Rule.STATUS_DEPRECATED;
    String newConfigKey = "NewConfigKey";
    Priority newPriority = Priority.INFO;
    Cardinality newCardinality = Cardinality.MULTIPLE;
    String newLanguage = "dart";
    Date updatedAt = new Date();
    Long newParentId = 3L;

    ruleToUpdate.setId(ruleId);
    ruleToUpdate.setRuleKey(newRuleKey);
    ruleToUpdate.setRepositoryKey(newRepositoryKey);
    ruleToUpdate.setName(newName);
    ruleToUpdate.setDescription(newDescription);
    ruleToUpdate.setStatus(newStatus);
    ruleToUpdate.setConfigKey(newConfigKey);
    ruleToUpdate.setPriority(newPriority);
    ruleToUpdate.setCardinality(newCardinality);
    ruleToUpdate.setLanguage(newLanguage);
    ruleToUpdate.setUpdatedAt(updatedAt);
    ruleToUpdate.setParentId(newParentId);
    dao.update(ruleToUpdate);

    RuleDto updatedRule = dao.selectById(ruleId);
    assertThat(updatedRule.getRuleKey()).isEqualTo(newRuleKey);
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
    Priority newPriority = Priority.INFO;
    Cardinality newCardinality = Cardinality.MULTIPLE;
    String newLanguage = "dart";
    Date updatedAt = new Date();
    Long newParentId = 3L;

    ruleToInsert.setRuleKey(newRuleKey);
    ruleToInsert.setRepositoryKey(newRepositoryKey);
    ruleToInsert.setName(newName);
    ruleToInsert.setDescription(newDescription);
    ruleToInsert.setStatus(newStatus);
    ruleToInsert.setConfigKey(newConfigKey);
    ruleToInsert.setPriority(newPriority);
    ruleToInsert.setCardinality(newCardinality);
    ruleToInsert.setLanguage(newLanguage);
    ruleToInsert.setUpdatedAt(updatedAt);
    ruleToInsert.setParentId(newParentId);
    dao.insert(ruleToInsert);

    List<RuleDto> ruleDtos = dao.selectAll();
    assertThat(ruleDtos.size()).isEqualTo(1);
    RuleDto insertedRule = ruleDtos.get(0);
    // First inserted rule, auto generated ID should be 1
    assertThat(insertedRule.getId()).isEqualTo(1L);
    assertThat(insertedRule.getRuleKey()).isEqualTo(newRuleKey);
    assertThat(insertedRule.getRepositoryKey()).isEqualTo(newRepositoryKey);
    assertThat(insertedRule.getName()).isEqualTo(newName);
    assertThat(insertedRule.getDescription()).isEqualTo(newDescription);
    assertThat(insertedRule.getStatus()).isEqualTo(newStatus);
    assertThat(insertedRule.getConfigKey()).isEqualTo(newConfigKey);
    assertThat(insertedRule.getPriority()).isEqualTo(newPriority);
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
    Priority newPriority = Priority.INFO;
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
    ruleToInsert1.setPriority(newPriority);
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
    Priority newPriority1 = Priority.INFO;
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
    ruleToInsert2.setPriority(newPriority1);
    ruleToInsert2.setCardinality(newCardinality1);
    ruleToInsert2.setLanguage(newLanguage1);
    ruleToInsert2.setCreatedAt(createdAt1);
    ruleToInsert2.setUpdatedAt(updatedAt1);

    dao.insert(ImmutableList.of(ruleToInsert1, ruleToInsert2));

    List<RuleDto> ruleDtos = dao.selectAll();
    assertThat(ruleDtos.size()).isEqualTo(2);
    assertThat(ruleDtos.get(0).getId()).isEqualTo(1L);
    assertThat(ruleDtos.get(1).getId()).isEqualTo(2L);
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
    long ruleId = 1L;
    List<RuleParamDto> ruleDtos = dao.selectParameters(ruleId);

    assertThat(ruleDtos.size()).isEqualTo(1);
    RuleParamDto ruleDto = ruleDtos.get(0);
    assertThat(ruleDto.getId()).isEqualTo(1);
    assertThat(ruleDto.getName()).isEqualTo("myParameter");
    assertThat(ruleDto.getDescription()).isEqualTo("My Parameter");
    assertThat(ruleDto.getType()).isEqualTo("plop");
    assertThat(ruleDto.getRuleId()).isEqualTo(ruleId);
  }
}
