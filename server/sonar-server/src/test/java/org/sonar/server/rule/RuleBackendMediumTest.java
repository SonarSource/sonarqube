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
package org.sonar.server.rule;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.debt.DebtTesting;
import org.sonar.server.platform.Platform;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.rule.index.RuleDoc;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.search.QueryContext;
import org.sonar.server.tester.ServerTester;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test persistence in db and indexation in es (--> integration of DAOs and Indexes)
 */
public class RuleBackendMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  RuleDao dao = tester.get(RuleDao.class);
  RuleIndex index = tester.get(RuleIndex.class);
  DbClient db;
  DbSession dbSession;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    dbSession = tester.get(DbClient.class).openSession(false);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void insert_in_db_and_multiget_in_es() throws InterruptedException {
    // insert db
    RuleDto ruleDto = RuleTesting.newXooX1();
    RuleDto ruleDto2 = RuleTesting.newXooX2();
    dao.insert(dbSession, ruleDto, ruleDto2);
    dbSession.commit();

    // check that we get two rules
    Collection<Rule> hits = index.getByKeys(RuleTesting.XOO_X1, RuleTesting.XOO_X2);
    assertThat(hits).hasSize(2);
  }

  @Test
  public void insert_in_db_and_index_in_es() throws InterruptedException {
    // insert db
    RuleDto ruleDto = RuleTesting.newXooX1();
    dao.insert(dbSession, ruleDto);
    dbSession.commit();

    // verify that rule is persisted in db
    RuleDto persistedDto = dao.getNullableByKey(dbSession, RuleTesting.XOO_X1);
    assertThat(persistedDto).isNotNull();
    assertThat(persistedDto.getId()).isGreaterThanOrEqualTo(0);
    assertThat(persistedDto.getRuleKey()).isEqualTo(ruleDto.getRuleKey());
    assertThat(persistedDto.getLanguage()).isEqualTo(ruleDto.getLanguage());
    assertThat(persistedDto.getCreatedAt()).isNotNull();
    assertThat(persistedDto.getUpdatedAt()).isNotNull();

    // verify that rule is indexed in es
    Rule hit = index.getByKey(RuleTesting.XOO_X1);
    assertRuleEquivalent(ruleDto, hit);

    // Verify Multi-get
    Collection<Rule> hits = index.getByKeys(RuleTesting.XOO_X1);
    assertThat(hits).hasSize(1);
    assertRuleEquivalent(ruleDto, Iterables.getFirst(hits, null));

  }

  private void assertRuleEquivalent(RuleDto ruleDto, Rule hit) {
    assertThat(hit).isNotNull();
    assertThat(hit.key().repository()).isEqualTo(ruleDto.getRepositoryKey());
    assertThat(hit.key().rule()).isEqualTo(ruleDto.getRuleKey());
    assertThat(hit.language()).isEqualTo(ruleDto.getLanguage());
    assertThat(hit.name()).isEqualTo(ruleDto.getName());
    assertThat(hit.htmlDescription()).isEqualTo(ruleDto.getDescription());
    assertThat(hit.status()).isEqualTo(RuleStatus.READY);
    assertThat(hit.createdAt()).isNotNull();
    assertThat(hit.updatedAt()).isNotNull();
    assertThat(hit.internalKey()).isEqualTo(ruleDto.getConfigKey());
    assertThat(hit.severity()).isEqualTo(ruleDto.getSeverityString());
    assertThat(hit.isTemplate()).isFalse();
    assertThat(hit.effortToFixDescription()).isEqualTo(ruleDto.getEffortToFixDescription());
  }

  @Test
  public void insert_rule_tags_in_db_and_index_in_es() throws InterruptedException {
    // insert db
    RuleDto ruleDto = RuleTesting.newXooX1();
    dao.insert(dbSession, ruleDto);
    dbSession.commit();

    RuleDto persistedDto = dao.getNullableByKey(dbSession, RuleTesting.XOO_X1);
    assertThat(persistedDto.getTags().containsAll(ruleDto.getTags())).isTrue();
    assertThat(persistedDto.getSystemTags().containsAll(ruleDto.getSystemTags())).isTrue();

    Rule hit = index.getByKey(RuleTesting.XOO_X1);
    assertThat(hit.tags().containsAll(ruleDto.getTags())).isTrue();
    assertThat(hit.systemTags().containsAll(ruleDto.getSystemTags())).isTrue();
  }

  @Test
  public void insert_and_index_rule_parameters() {
    // insert db
    RuleDto ruleDto = RuleTesting.newXooX1();
    dao.insert(dbSession, ruleDto);
    dbSession.commit();

    RuleParamDto minParamDto = new RuleParamDto()
      .setName("min")
      .setType(RuleParamType.INTEGER.type())
      .setDefaultValue("2")
      .setDescription("Minimum");
    dao.addRuleParam(dbSession, ruleDto, minParamDto);
    RuleParamDto maxParamDto = new RuleParamDto()
      .setName("max")
      .setType(RuleParamType.INTEGER.type())
      .setDefaultValue("10")
      .setDescription("Maximum");
    dao.addRuleParam(dbSession, ruleDto, maxParamDto);
    dbSession.commit();

    //Verify that RuleDto has date from insertion
    RuleDto theRule = dao.getNullableByKey(dbSession, RuleTesting.XOO_X1);
    assertThat(theRule.getCreatedAt()).isNotNull();
    assertThat(theRule.getUpdatedAt()).isNotNull();

    // verify that parameters are persisted in db
    List<RuleParamDto> persistedDtos = dao.findRuleParamsByRuleKey(dbSession, theRule.getKey());
    assertThat(persistedDtos).hasSize(2);

    // verify that parameters are indexed in es

    Rule hit = index.getByKey(RuleTesting.XOO_X1);
    assertThat(hit).isNotNull();
    assertThat(hit.key()).isNotNull();

    RuleService service = tester.get(RuleService.class);
    Rule rule = service.getByKey(RuleTesting.XOO_X1);

    assertThat(rule.params()).hasSize(2);
    assertThat(Iterables.getLast(rule.params(), null).key()).isEqualTo("max");
  }

  @Test
  public void insert_and_delete_rule_parameters() {
    // insert db
    RuleDto ruleDto = RuleTesting.newXooX1();
    dao.insert(dbSession, ruleDto);
    dbSession.commit();

    RuleParamDto minParamDto = new RuleParamDto()
      .setName("min")
      .setType(RuleParamType.INTEGER.type())
      .setDefaultValue("2")
      .setDescription("Minimum");
    dao.addRuleParam(dbSession, ruleDto, minParamDto);
    RuleParamDto maxParamDto = new RuleParamDto()
      .setName("max")
      .setType(RuleParamType.INTEGER.type())
      .setDefaultValue("10")
      .setDescription("Maximum");
    dao.addRuleParam(dbSession, ruleDto, maxParamDto);
    dbSession.commit();

    // 0. Verify that RuleDto has date from insertion
    assertThat(dao.findRuleParamsByRuleKey(dbSession, RuleTesting.XOO_X1)).hasSize(2);
    assertThat(index.getByKey(RuleTesting.XOO_X1).params()).hasSize(2);

    // 1. Delete parameter
    dao.removeRuleParam(dbSession, ruleDto, maxParamDto);
    dbSession.commit();

    // 2. assert only one param left
    assertThat(dao.findRuleParamsByRuleKey(dbSession, RuleTesting.XOO_X1)).hasSize(1);
    assertThat(index.getByKey(RuleTesting.XOO_X1).params()).hasSize(1);
  }


  @Test
  public void insert_and_update_rule() {
    // insert db
    RuleDto ruleDto = RuleTesting.newXooX1()
      .setTags(ImmutableSet.of("hello"))
      .setName("first name");
    dao.insert(dbSession, ruleDto);
    dbSession.commit();

    // verify that parameters are indexed in es

    Rule hit = index.getByKey(RuleTesting.XOO_X1);
    assertThat(hit.tags()).containsExactly("hello");
    assertThat(hit.name()).isEqualTo("first name");

    //Update in DB
    ruleDto.setTags(ImmutableSet.of("world"))
      .setName("second name");
    dao.update(dbSession, ruleDto);
    dbSession.commit();

    // verify that parameters are updated in es

    hit = index.getByKey(RuleTesting.XOO_X1);
    assertThat(hit.tags()).containsExactly("world");
    assertThat(hit.name()).isEqualTo("second name");
  }

  @Test
  public void insert_and_update_rule_param() throws InterruptedException {

    // insert db
    RuleDto ruleDto = RuleTesting.newXooX1();
    dao.insert(dbSession, ruleDto);
    dbSession.commit();

    RuleParamDto minParamDto = new RuleParamDto()
      .setName("min")
      .setType(RuleParamType.INTEGER.type())
      .setDefaultValue("2")
      .setDescription("Minimum");
    dao.addRuleParam(dbSession, ruleDto, minParamDto);

    RuleParamDto maxParamDto = new RuleParamDto()
      .setName("max")
      .setType(RuleParamType.INTEGER.type())
      .setDefaultValue("10")
      .setDescription("Maximum");
    dao.addRuleParam(dbSession, ruleDto, maxParamDto);
    dbSession.commit();

    // verify that parameters are indexed in es

    Rule hit = index.getByKey(RuleTesting.XOO_X1);
    assertThat(hit.params()).hasSize(2);

    RuleParam param = hit.params().get(0);
    assertThat(param.key()).isEqualTo("min");
    assertThat(param.defaultValue()).isEqualTo("2");
    assertThat(param.description()).isEqualTo("Minimum");


    //Update in DB
    minParamDto
      .setDefaultValue("0.5")
      .setDescription("new description");
    dao.updateRuleParam(dbSession, ruleDto, minParamDto);
    dbSession.commit();

    // verify that parameters are updated in es

    hit = index.getByKey(RuleTesting.XOO_X1);
    assertThat(hit.params()).hasSize(2);

    param = null;
    for (RuleParam pparam : hit.params()) {
      if (pparam.key().equals("min")) {
        param = pparam;
      }
    }
    assertThat(param).isNotNull();
    assertThat(param.key()).isEqualTo("min");
    assertThat(param.defaultValue()).isEqualTo("0.5");
    assertThat(param.description()).isEqualTo("new description");
  }

  @Test
  @Deprecated
  public void has_id() throws Exception {

    RuleDto ruleDto = RuleTesting.newXooX1();
    dao.insert(dbSession, ruleDto);
    dbSession.commit();

    assertThat(((RuleDoc) index.getByKey(RuleTesting.XOO_X1)).id()).isEqualTo(ruleDto.getId());
  }


  @Test
  public void insert_update_characteristics() throws Exception {

    CharacteristicDto char1 = DebtTesting.newCharacteristicDto("c1");
    db.debtCharacteristicDao().insert(char1, dbSession);
    dbSession.commit();

    CharacteristicDto char11 = DebtTesting.newCharacteristicDto("c11")
      .setParentId(char1.getId());
    db.debtCharacteristicDao().insert(char11, dbSession);

    dao.insert(dbSession,
      RuleTesting.newXooX1()
        .setDefaultSubCharacteristicId(char11.getId())
        .setRemediationFunction(null)
        .setRemediationCoefficient(null)
        .setRemediationOffset(null));
    dbSession.commit();


    // 0. assert chars in DB
    assertThat(db.debtCharacteristicDao().selectByKey("c1", dbSession)).isNotNull();
    assertThat(db.debtCharacteristicDao().selectByKey("c1", dbSession).getParentId()).isNull();
    assertThat(db.debtCharacteristicDao().selectByKey("c11", dbSession)).isNotNull();
    assertThat(db.debtCharacteristicDao().selectByKey("c11", dbSession).getParentId()).isEqualTo(char1.getId());

    // 1. find char and subChar from rule
    Rule rule = index.getByKey(RuleTesting.XOO_X1);
    assertThat(rule.debtCharacteristicKey()).isEqualTo(char1.getKey());
    assertThat(rule.debtSubCharacteristicKey()).isEqualTo(char11.getKey());
    assertThat(rule.debtOverloaded()).isFalse();


    // 3. set Non-default characteristics
    RuleDto ruleDto = db.ruleDao().getByKey(dbSession, RuleTesting.XOO_X1);
    CharacteristicDto char2 = DebtTesting.newCharacteristicDto("c2");
    db.debtCharacteristicDao().insert(char2, dbSession);

    CharacteristicDto char21 = DebtTesting.newCharacteristicDto("c21")
      .setParentId(char2.getId());
    db.debtCharacteristicDao().insert(char21, dbSession);

    ruleDto.setSubCharacteristicId(char21.getId());
    dao.update(dbSession, ruleDto);
    dbSession.commit();

    rule = index.getByKey(RuleTesting.XOO_X1);

    // Assert rule has debt Overloaded Char
    assertThat(rule.debtOverloaded()).isTrue();

    // 4. Get non-default chars from Rule
    assertThat(rule.debtCharacteristicKey()).isEqualTo(char2.getKey());
    assertThat(rule.debtSubCharacteristicKey()).isEqualTo(char21.getKey());

    //  5 Assert still get the default one
    assertThat(rule.debtOverloaded()).isTrue();
    assertThat(rule.defaultDebtCharacteristicKey()).isEqualTo(char1.getKey());
    assertThat(rule.defaultDebtSubCharacteristicKey()).isEqualTo(char11.getKey());

  }

  @Test
  public void insert_update_debt_overload() throws Exception {

    RuleDto ruleDto = RuleTesting.newXooX1()
      .setRemediationFunction(null)
      .setRemediationCoefficient(null)
      .setRemediationOffset(null);

    RuleDto overloadedRuleDto = RuleTesting.newXooX2();

    dao.insert(dbSession, ruleDto, overloadedRuleDto);
    dbSession.commit();

    // Assert is overloaded or not
    assertThat(index.getByKey(RuleTesting.XOO_X1).debtOverloaded()).isFalse();
    assertThat(index.getByKey(RuleTesting.XOO_X2).debtOverloaded()).isTrue();

    // Assert overloaded value
    Rule base = index.getByKey(RuleTesting.XOO_X1);
    Rule overloaded = index.getByKey(RuleTesting.XOO_X2);

    assertThat(base.debtRemediationFunction().type().toString())
      .isEqualTo(ruleDto.getDefaultRemediationFunction());
    assertThat(base.debtRemediationFunction().coefficient())
      .isEqualTo(ruleDto.getDefaultRemediationCoefficient());
    assertThat(base.debtRemediationFunction().offset())
      .isEqualTo(ruleDto.getDefaultRemediationOffset());

    assertThat(overloaded.debtRemediationFunction().type().toString())
      .isEqualTo(overloadedRuleDto.getRemediationFunction());
    assertThat(overloaded.debtRemediationFunction().coefficient())
      .isEqualTo(overloadedRuleDto.getRemediationCoefficient());
    assertThat(overloaded.debtRemediationFunction().offset())
      .isEqualTo(overloadedRuleDto.getRemediationOffset());
  }

  @Test
  public void should_not_find_removed() {
    // insert db
    dao.insert(dbSession,
      RuleTesting.newXooX1(),
      RuleTesting.newXooX2().setStatus(RuleStatus.REMOVED));
    dbSession.commit();

    // 0. Assert rules are in DB
    assertThat(dao.findAll(dbSession)).hasSize(2);

    // 1. assert getBy for removed
    assertThat(index.getByKey(RuleTesting.XOO_X2)).isNotNull();

    // 2. assert find does not get REMOVED
    List<Rule> rules = index.search(new RuleQuery(), new QueryContext()).getHits();
    assertThat(rules).hasSize(1);
    assertThat(rules.get(0).key()).isEqualTo(RuleTesting.XOO_X1);
  }

  @Test
  public void synchronize_after() {
    // insert db
    dao.insert(dbSession,
      RuleTesting.newXooX1());
    dbSession.commit();

    // 0. Assert rules are in DB
    assertThat(dao.findAll(dbSession)).hasSize(1);
    assertThat(index.countAll()).isEqualTo(1);

    tester.clearIndexes();
    assertThat(index.countAll()).isEqualTo(0);

    tester.get(Platform.class).executeStartupTasks();
    assertThat(index.countAll()).isEqualTo(1);

  }

  @Test
  public void synchronize_after_with_nested() {
    RuleDto rule = RuleTesting.newXooX1();

    // insert db
    dao.insert(dbSession, rule);

    dao.addRuleParam(dbSession, rule, RuleParamDto.createFor(rule).setName("MyParam").setType("STRING").setDefaultValue("test"));
    dbSession.commit();

    // 0. Assert rules are in DB
    assertThat(dao.findAll(dbSession)).hasSize(1);
    assertThat(index.countAll()).isEqualTo(1);

    tester.clearIndexes();
    assertThat(index.countAll()).isEqualTo(0);

    tester.get(Platform.class).executeStartupTasks();
    assertThat(index.countAll()).isEqualTo(1);

    assertThat(index.getByKey(rule.getKey()).param("MyParam").defaultValue()).isEqualTo("test");

  }
}
