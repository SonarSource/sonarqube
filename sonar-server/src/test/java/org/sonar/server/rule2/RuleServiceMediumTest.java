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
import org.sonar.check.Cardinality;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule2.index.RuleIndex;
import org.sonar.server.rule2.persistence.RuleDao;
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
    tester.clearDataStores();
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
    index.refresh();
    Rule rule = index.getByKey(ruleKey);
    // TODO
//    assertThat(rule.getNote()).isEqualTo("my *note*");
//    assertThat(rule.getNoteCreatedAt()).isNotNull();
//    assertThat(rule.getNoteUpdatedAt()).isNotNull();
//    assertThat(rule.getNoteUserLogin()).isEqualTo("marius");

    // 2. DELETE NOTE
    service.setNote(ruleKey, null);
    dbSession.clearCache();
    dto = dao.getNonNullByKey(ruleKey, dbSession);
    assertThat(dto.getNoteData()).isNull();
    assertThat(dto.getNoteCreatedAt()).isNull();
    assertThat(dto.getNoteUpdatedAt()).isNull();
    assertThat(dto.getNoteUserLogin()).isNull();
    index.refresh();
    rule = index.getByKey(ruleKey);
    // TODO
    //    assertThat(rule.getNote()).isNull();
//    assertThat(rule.getNoteCreatedAt()).isNull();
//    assertThat(rule.getNoteUpdatedAt()).isNull();
//    assertThat(rule.getNoteUserLogin()).isNull();

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