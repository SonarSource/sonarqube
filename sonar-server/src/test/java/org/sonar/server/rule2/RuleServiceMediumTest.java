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
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.check.Cardinality;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualityprofile.persistence.ActiveRuleDao;
import org.sonar.server.rule2.index.RuleIndex;
import org.sonar.server.rule2.index.RuleNormalizer;
import org.sonar.server.rule2.index.RuleQuery;
import org.sonar.server.rule2.index.RuleResult;
import org.sonar.server.rule2.persistence.RuleDao;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import java.util.Collections;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class RuleServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  RuleDao dao = tester.get(RuleDao.class);
  RuleIndex index = tester.get(RuleIndex.class);
  RuleService service = tester.get(RuleService.class);
  DbSession dbSession;

  @Before
  public void before() {
    tester.clearDbAndEs();
    dbSession = tester.get(MyBatis.class).openSession(false);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void setTags() throws InterruptedException {
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    // insert db
    RuleKey rule1 = RuleKey.of("javascript", "S001");
    dao.insert(newRuleDto(rule1)
        .setTags(Sets.newHashSet("security"))
        .setSystemTags(Collections.<String>emptySet()),
      dbSession);

    RuleKey rule2 = RuleKey.of("java", "S001");
    dao.insert(newRuleDto(rule2)
      .setTags(Sets.newHashSet("toberemoved"))
      .setSystemTags(Sets.newHashSet("bug")), dbSession);
    dbSession.commit();

    service.setTags(rule2, Sets.newHashSet("bug", "security"));

    // verify that tags are indexed in index
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
      assertThat(e).hasMessage("Key 'java:S001' not found");
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

  @Test
  public void setNote() throws Exception {
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN).setLogin("marius");
    RuleKey ruleKey = RuleKey.of("javascript", "S001");
    dao.insert(newRuleDto(ruleKey), dbSession);
    dbSession.commit();

    // 1. CREATE NOTE
    service.setNote(ruleKey, "my *note*");

    // verify db
    RuleDto dto = dao.getNonNullByKey(ruleKey, dbSession);
    assertThat(dto.getNoteData()).isEqualTo("my *note*");
    assertThat(dto.getNoteCreatedAt()).isNotNull();
    assertThat(dto.getNoteUpdatedAt()).isNotNull();
    assertThat(dto.getNoteUserLogin()).isEqualTo("marius");

    // verify es
    Rule rule = index.getByKey(ruleKey);
    assertThat(rule.markdownNote()).isEqualTo("my *note*");
    assertThat(rule.noteCreatedAt()).isNotNull();
    assertThat(rule.noteUpdatedAt()).isNotNull();
    assertThat(rule.noteLogin()).isEqualTo("marius");

    // 2. DELETE NOTE
    service.setNote(ruleKey, null);
    dbSession.clearCache();
    dto = dao.getNonNullByKey(ruleKey, dbSession);
    assertThat(dto.getNoteData()).isNull();
    assertThat(dto.getNoteCreatedAt()).isNull();
    assertThat(dto.getNoteUpdatedAt()).isNull();
    assertThat(dto.getNoteUserLogin()).isNull();

    rule = index.getByKey(ruleKey);
    assertThat(rule.markdownNote()).isNull();
    assertThat(rule.noteCreatedAt()).isNull();
    assertThat(rule.noteUpdatedAt()).isNull();
    assertThat(rule.noteLogin()).isNull();

  }

  @Test
  public void test_list_tags() throws InterruptedException {
    // insert db
    RuleKey rule1 = RuleKey.of("javascript", "S001");
    dao.insert(newRuleDto(rule1)
        .setTags(Sets.newHashSet("security"))
        .setSystemTags(Sets.newHashSet("java-coding","stephane.gamard@sonarsource.com")),
      dbSession);

    RuleKey rule2 = RuleKey.of("java", "S001");
    dao.insert(newRuleDto(rule2)
      .setTags(Sets.newHashSet("mytag"))
      .setSystemTags(Sets.newHashSet("")), dbSession);
    dbSession.commit();



    Set<String> tags = index.terms(RuleNormalizer.RuleField._TAGS.key());
    assertThat(tags).containsOnly("java-coding","security",
      "stephane.gamard@sonarsource.com","mytag");

    tags = index.terms(RuleNormalizer.RuleField.SYSTEM_TAGS.key());
    assertThat(tags).containsOnly("java-coding",
      "stephane.gamard@sonarsource.com");

  }

  @Test
  public void test_search_activation_on_rules() throws InterruptedException {

    // 1. Create in DB
    QualityProfileDto qprofile1 = QualityProfileDto.createFor("profile1","java");
    QualityProfileDto qprofile2 = QualityProfileDto.createFor("profile2","java");
    tester.get(QualityProfileDao.class).insert(qprofile1, dbSession);
    tester.get(QualityProfileDao.class).insert(qprofile2, dbSession);

    RuleDto rule1 = newRuleDto(RuleKey.of("test", "rule1"));
    RuleDto rule2 = newRuleDto(RuleKey.of("test", "rule2"));
    tester.get(RuleDao.class).insert(rule1, dbSession);
    tester.get(RuleDao.class).insert(rule2, dbSession);

    ActiveRuleDto activeRule1 = ActiveRuleDto.createFor(qprofile1, rule1)
      .setSeverity(Severity.BLOCKER);
    ActiveRuleDto activeRule2 = ActiveRuleDto.createFor(qprofile1, rule2)
      .setSeverity(Severity.BLOCKER);
    ActiveRuleDto activeRule3 = ActiveRuleDto.createFor(qprofile2, rule2)
      .setSeverity(Severity.BLOCKER);
    tester.get(ActiveRuleDao.class).insert(activeRule1, dbSession);
    tester.get(ActiveRuleDao.class).insert(activeRule2, dbSession);
    tester.get(ActiveRuleDao.class).insert(activeRule3, dbSession);

    dbSession.commit();



    // 2. test in DB
    assertThat(tester.get(RuleDao.class).findAll(dbSession)).hasSize(2);
    assertThat(tester.get(ActiveRuleDao.class).findByRule(rule1, dbSession)).hasSize(1);
    assertThat(tester.get(ActiveRuleDao.class).findByRule(rule2, dbSession)).hasSize(2);


    // 3. Test for ALL activations
    RuleQuery query = new RuleQuery()
      .setActivation("all");
    RuleResult result = service.search(query, new QueryOptions());
    assertThat(result.getActiveRules().values()).hasSize(3);

    // 4. Test for NO active rules
    query = new RuleQuery()
      .setActivation("false");
    result = service.search(query, new QueryOptions());
    assertThat(result.getActiveRules().values()).hasSize(0);

    // 4. Test for  active rules of QProfile
    query = new RuleQuery()
      .setActivation("true")
      .setQProfileKey(qprofile1.getKey().toString());
    result = service.search(query, new QueryOptions());
    assertThat(result.getActiveRules().values()).hasSize(2);
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
