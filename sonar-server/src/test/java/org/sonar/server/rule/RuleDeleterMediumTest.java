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

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.check.Cardinality;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.tester.ServerTester;

import static org.fest.assertions.Assertions.assertThat;

public class RuleDeleterMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  RuleDao dao = tester.get(RuleDao.class);
  RuleIndex index = tester.get(RuleIndex.class);
  RuleCreator creator = tester.get(RuleCreator.class);
  RuleDeleter deleter = tester.get(RuleDeleter.class);
  DbSession dbSession;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    dbSession = tester.get(DbClient.class).openSession(false);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void delete_custom_rule() throws Exception {
    // Create template rule
    RuleKey templateRuleKey = RuleKey.of("java", "S001");
    dao.insert(dbSession, RuleTesting.newDto(templateRuleKey).setCardinality(Cardinality.MULTIPLE));
    dbSession.commit();

    // Create custom rule
    NewRule newRule = new NewRule()
      .setTemplateKey(templateRuleKey)
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("regex", "a.*"));
    RuleKey customRuleKey = creator.create(newRule);

    // Delete custom rule
    deleter.delete(customRuleKey);

    Rule customRuleReloaded = index.getByKey(customRuleKey);
    assertThat(customRuleReloaded).isNotNull();
    assertThat(customRuleReloaded.status()).isEqualTo(RuleStatus.REMOVED);

    // TODO check active rules are removed
  }

  @Test
  public void fail_to_delete_if_not_custom() throws Exception {
    // Create rule
    RuleKey ruleKey = RuleKey.of("java", "S001");
    dao.insert(dbSession, RuleTesting.newDto(ruleKey));
    dbSession.commit();

    try {
      // Delete rule
      deleter.delete(ruleKey);
    } catch (Exception e){
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Only custom rules can be deleted");
    }
  }

}
