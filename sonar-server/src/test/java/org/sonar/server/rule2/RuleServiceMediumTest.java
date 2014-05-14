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
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.check.Cardinality;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule2.index.RuleIndex;
import org.sonar.server.rule2.persistence.RuleDao;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class RuleServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  MyBatis myBatis = tester.get(MyBatis.class);
  RuleDao dao = tester.get(RuleDao.class);
  RuleIndex index = tester.get(RuleIndex.class);
  RuleService service = tester.get(RuleService.class);
  DbSession dbSession;

  @Before
  public void before() {
    tester.clearDataStores();
    dbSession = myBatis.openSession(false);
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
    index.refresh();
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
    index.refresh();
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
    index.refresh();
    Rule hit = index.getByKey(ruleKey);
    assertThat(hit.tags()).containsExactly("hello");
    assertThat(hit.name()).isEqualTo("first name");

    //Update in DB
    ruleDto.setTags(ImmutableSet.of("world"))
      .setName("second name");
    dao.update(ruleDto, dbSession);
    dbSession.commit();

    // verify that parameters are updated in es
    index.refresh();
    hit = index.getByKey(ruleKey);
    assertThat(hit.tags()).containsExactly("world");
    assertThat(hit.name()).isEqualTo("second name");
  }

  @Test
  public void insert_and_update_rule_param() {

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
    index.refresh();
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
    index.refresh();
    hit = index.getByKey(ruleKey);
    assertThat(hit.params()).hasSize(2);

    param = hit.params().get(0);
    assertThat(param.key()).isEqualTo("min");
    assertThat(param.defaultValue()).isEqualTo("0.5");
    assertThat(param.description()).isEqualTo("new description");
  }

  @Test
  public void setTags() throws InterruptedException {
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    // insert db
    RuleKey rule1 = RuleKey.of("javascript", "S001");
    dao.insert(newRuleDto(rule1)
        .setTags(Sets.newHashSet("security"))
        .setSystemTags(Collections.<String>emptySet()),
      dbSession
    );

    RuleKey rule2 = RuleKey.of("java", "S001");
    dao.insert(newRuleDto(rule2)
      .setTags(Sets.newHashSet("toberemoved"))
      .setSystemTags(Sets.newHashSet("bug")), dbSession);
    dbSession.commit();

    service.setTags(rule2, Sets.newHashSet("bug", "security"));

    // verify that tags are indexed in es

    service.refresh();

    Set<String> tags = service.listTags();
    assertThat(tags).containsOnly("security", "bug");
  }

  @Test
  public void setTags_fail_if_rule_does_not_exist() {
    try {
      MockUserSession.set().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
      service.setTags(RuleKey.of("java", "S001"), Sets.newHashSet("bug", "security"));
      fail();
    } catch (NotFoundException e) {
      assertThat(e).hasMessage("Rule java:S001 not found");
    }
  }

  @Test
  public void setTags_fail_if_not_permitted() {
    try {
      MockUserSession.set();
      service.setTags(RuleKey.of("java", "S001"), Sets.newHashSet("bug", "security"));
      fail();
    } catch (ForbiddenException e) {
      assertThat(e).hasMessage("Insufficient privileges");
    }
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
      .setRemediationFunction("linear")
      .setDefaultRemediationFunction("linear_offset")
      .setRemediationCoefficient("1h")
      .setDefaultRemediationCoefficient("5d")
      .setRemediationOffset("5min")
      .setDefaultRemediationOffset("10h")
      .setEffortToFixDescription(ruleKey.repository() + "." + ruleKey.rule() + ".effortToFix");
  }
}
