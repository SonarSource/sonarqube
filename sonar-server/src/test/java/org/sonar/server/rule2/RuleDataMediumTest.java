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
package org.sonar.server.rule2;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.check.Cardinality;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.rule2.index.RuleIndex;
import org.sonar.server.rule2.persistence.RuleDao;
import org.sonar.server.tester.ServerTester;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test persistence in db and indexation in es (--> integration of daos and indexes)
 */
public class RuleDataMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  RuleDao dao = tester.get(RuleDao.class);
  RuleIndex index = tester.get(RuleIndex.class);
  DbClient db;
  DbSession dbSession;

  @Before
  public void before() {
    tester.clearDbAndEs();
    db = tester.get(DbClient.class);
    dbSession = tester.get(MyBatis.class).openSession(false);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void insert_in_db_and_index_in_es() throws InterruptedException {
    // insert db
    RuleKey ruleKey = RuleKey.of("javascript", "S001");
    dao.insert(newRuleDto(ruleKey), dbSession);
    dbSession.commit();

    // verify that rule is persisted in db
    RuleDto persistedDto = dao.getByKey(ruleKey, dbSession);
    assertThat(persistedDto).isNotNull();
    assertThat(persistedDto.getId()).isGreaterThanOrEqualTo(0);
    assertThat(persistedDto.getRuleKey()).isEqualTo(ruleKey.rule());
    assertThat(persistedDto.getLanguage()).isEqualTo("js");
    assertThat(persistedDto.getTags()).containsOnly("tag1", "tag2");
    assertThat(persistedDto.getSystemTags()).containsOnly("systag1", "systag2");
    assertThat(persistedDto.getCreatedAt()).isNotNull();
    assertThat(persistedDto.getUpdatedAt()).isNotNull();

    // verify that rule is indexed in es

    Rule hit = index.getByKey(ruleKey);
    assertThat(hit).isNotNull();
    assertThat(hit.key().repository()).isEqualTo(ruleKey.repository());
    assertThat(hit.key().rule()).isEqualTo(ruleKey.rule());
    assertThat(hit.language()).isEqualTo("js");
    assertThat(hit.name()).isEqualTo("Rule S001");
    assertThat(hit.htmlDescription()).isEqualTo("Description S001");
    assertThat(hit.status()).isEqualTo(RuleStatus.READY);
    assertThat(hit.createdAt()).isNotNull();
    assertThat(hit.updatedAt()).isNotNull();
    assertThat(hit.internalKey()).isEqualTo("InternalKeyS001");
    assertThat(hit.severity()).isEqualTo("INFO");
    assertThat(hit.template()).isFalse();
    assertThat(hit.tags()).containsOnly("tag1", "tag2");
    assertThat(hit.systemTags()).containsOnly("systag1", "systag2");
  }

  @Test
  public void insert_and_index_rule_parameters() {
    // insert db
    RuleKey ruleKey = RuleKey.of("javascript", "S001");
    RuleDto ruleDto = newRuleDto(ruleKey);
    dao.insert(ruleDto, dbSession);
    dbSession.commit();

    RuleParamDto minParamDto = new RuleParamDto()
      .setName("min")
      .setType(RuleParamType.INTEGER.type())
      .setDefaultValue("2")
      .setDescription("Minimum");
    dao.addRuleParam(ruleDto, minParamDto, dbSession);
    RuleParamDto maxParamDto = new RuleParamDto()
      .setName("max")
      .setType(RuleParamType.INTEGER.type())
      .setDefaultValue("10")
      .setDescription("Maximum");
    dao.addRuleParam(ruleDto, maxParamDto, dbSession);
    dbSession.commit();

    //Verify that RuleDto has date from insertion
    RuleDto theRule = dao.getByKey(ruleKey, dbSession);
    assertThat(theRule.getCreatedAt()).isNotNull();
    assertThat(theRule.getUpdatedAt()).isNotNull();

    // verify that parameters are persisted in db
    List<RuleParamDto> persistedDtos = dao.findRuleParamsByRuleKey(theRule.getKey(), dbSession);
    assertThat(persistedDtos).hasSize(2);

    // verify that parameters are indexed in es

    Rule hit = index.getByKey(ruleKey);
    assertThat(hit).isNotNull();
    assertThat(hit.key()).isNotNull();

    RuleService service = tester.get(RuleService.class);
    Rule rule = service.getByKey(ruleKey);

    assertThat(rule.params()).hasSize(2);
    assertThat(Iterables.getLast(rule.params(), null).key()).isEqualTo("max");
  }

  @Test
  public void insert_and_update_rule() {
    // insert db
    RuleKey ruleKey = RuleKey.of("javascript", "S001");
    RuleDto ruleDto = newRuleDto(ruleKey)
      .setTags(ImmutableSet.of("hello"))
      .setName("first name");
    dao.insert(ruleDto, dbSession);
    dbSession.commit();

    // verify that parameters are indexed in es

    Rule hit = index.getByKey(ruleKey);
    assertThat(hit.tags()).containsExactly("hello");
    assertThat(hit.name()).isEqualTo("first name");

    //Update in DB
    ruleDto.setTags(ImmutableSet.of("world"))
      .setName("second name");
    dao.update(ruleDto, dbSession);
    dbSession.commit();

    // verify that parameters are updated in es

    hit = index.getByKey(ruleKey);
    assertThat(hit.tags()).containsExactly("world");
    assertThat(hit.name()).isEqualTo("second name");
  }

  @Test
  public void insert_and_update_rule_param() throws InterruptedException {

    // insert db
    RuleKey ruleKey = RuleKey.of("javascript", "S001");
    RuleDto ruleDto = newRuleDto(ruleKey)
      .setTags(ImmutableSet.of("hello"))
      .setName("first name");
    dao.insert(ruleDto, dbSession);
    dbSession.commit();

    RuleParamDto minParamDto = new RuleParamDto()
      .setName("min")
      .setType(RuleParamType.INTEGER.type())
      .setDefaultValue("2")
      .setDescription("Minimum");
    dao.addRuleParam(ruleDto, minParamDto, dbSession);

    RuleParamDto maxParamDto = new RuleParamDto()
      .setName("max")
      .setType(RuleParamType.INTEGER.type())
      .setDefaultValue("10")
      .setDescription("Maximum");
    dao.addRuleParam(ruleDto, maxParamDto, dbSession);
    dbSession.commit();

    // verify that parameters are indexed in es

    Rule hit = index.getByKey(ruleKey);
    assertThat(hit.params()).hasSize(2);

    RuleParam param = hit.params().get(0);
    assertThat(param.key()).isEqualTo("min");
    assertThat(param.defaultValue()).isEqualTo("2");
    assertThat(param.description()).isEqualTo("Minimum");


    //Update in DB
    minParamDto
      .setDefaultValue("0.5")
      .setDescription("new description");
    dao.updateRuleParam(ruleDto, minParamDto, dbSession);
    dbSession.commit();

    // verify that parameters are updated in es

    hit = index.getByKey(ruleKey);
    assertThat(hit.params()).hasSize(2);

    param = hit.params().get(0);
    assertThat(param.key()).isEqualTo("min");
    assertThat(param.defaultValue()).isEqualTo("0.5");
    assertThat(param.description()).isEqualTo("new description");
  }

  @Test
  public void insert_update_characteristics() throws Exception {

    CharacteristicDto char1 = new CharacteristicDto()
      .setEnabled(true)
      .setKey("c1")
      .setName("char1");
    db.debtCharacteristicDao().insert(char1, dbSession);
    dbSession.commit();

    CharacteristicDto char11 = new CharacteristicDto()
      .setEnabled(true)
      .setKey("c11")
      .setName("char11")
      .setParentId(char1.getId());
    db.debtCharacteristicDao().insert(char11, dbSession);

    RuleKey ruleKey = RuleKey.of("test","r1");
    RuleDto ruleDto = newRuleDto(ruleKey)
      .setDefaultSubCharacteristicId(char11.getId());
    dao.insert(ruleDto, dbSession);
    dbSession.commit();


    // 0. assert chars in DB
    assertThat(db.debtCharacteristicDao().selectByKey("c1",dbSession)).isNotNull();
    assertThat(db.debtCharacteristicDao().selectByKey("c1",dbSession).getParentId()).isNull();
    assertThat(db.debtCharacteristicDao().selectByKey("c11",dbSession)).isNotNull();
    assertThat(db.debtCharacteristicDao().selectByKey("c11",dbSession).getParentId()).isEqualTo(char1.getId());

    // 1. find char and subChar from rule
    Rule rule = index.getByKey(ruleKey);
    assertThat(rule.debtCharacteristicKey()).isEqualTo(char1.getKey());
    assertThat(rule.debtSubCharacteristicKey()).isEqualTo(char11.getKey());

    // 3. set Non-default characteristics
    CharacteristicDto char2 = new CharacteristicDto()
      .setEnabled(true)
      .setKey("c2")
      .setName("char2");
    db.debtCharacteristicDao().insert(char2, dbSession);

    CharacteristicDto char21 = new CharacteristicDto()
      .setEnabled(true)
      .setKey("c21")
      .setName("char21")
      .setParentId(char2.getId());
    db.debtCharacteristicDao().insert(char21, dbSession);

    ruleDto.setSubCharacteristicId(char21.getId());
    dao.update(ruleDto, dbSession);

    dbSession.commit();

    // 4. Get non-default chars from Rule
    rule = index.getByKey(ruleKey);
    assertThat(rule.debtCharacteristicKey()).isEqualTo(char2.getKey());
    assertThat(rule.debtSubCharacteristicKey()).isEqualTo(char21.getKey());
  }

  private RuleDto newRuleDto(RuleKey ruleKey) {
    return new RuleDto()
      .setRuleKey(ruleKey.rule())
      .setRepositoryKey(ruleKey.repository())
      .setName("Rule " + ruleKey.rule())
      .setDescription("Description " + ruleKey.rule())
      .setStatus(RuleStatus.READY.toString())
      .setConfigKey("InternalKey" + ruleKey.rule())
      .setSeverity(Severity.INFO)
      .setCardinality(Cardinality.SINGLE)
      .setLanguage("js")
      .setTags(ImmutableSet.of("tag1", "tag2"))
      .setSystemTags(ImmutableSet.of("systag1", "systag2"))
      .setRemediationFunction(DebtRemediationFunction.Type.LINEAR.toString())
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.toString())
      .setRemediationCoefficient("1h")
      .setDefaultRemediationCoefficient("5d")
      .setRemediationOffset("5min")
      .setDefaultRemediationOffset("10h")
      .setEffortToFixDescription(ruleKey.repository() + "." + ruleKey.rule() + ".effortToFix");
  }
}
