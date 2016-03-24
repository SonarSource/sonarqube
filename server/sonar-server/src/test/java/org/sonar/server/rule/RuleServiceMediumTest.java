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
package org.sonar.server.rule;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;

public class RuleServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().withEsIndexes();

  @org.junit.Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  RuleDao dao = tester.get(RuleDao.class);
  RuleIndex index = tester.get(RuleIndex.class);
  RuleService service = tester.get(RuleService.class);
  DbSession dbSession;
  RuleIndexer ruleIndexer;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    dbSession = tester.get(DbClient.class).openSession(false);
    ruleIndexer = tester.get(RuleIndexer.class);
    ruleIndexer.setEnabled(true);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void list_tags() {
    // insert db
    RuleKey key1 = RuleKey.of("javascript", "S001");
    RuleKey key2 = RuleKey.of("java", "S001");
    dao.insert(dbSession,
      RuleTesting.newDto(key1).setTags(Sets.newHashSet("tag1")).setSystemTags(Sets.newHashSet("sys1", "sys2")));
    dao.insert(dbSession,
      RuleTesting.newDto(key2).setTags(Sets.newHashSet("tag2")).setSystemTags(Collections.<String>emptySet()));
    dbSession.commit();
    ruleIndexer.index();

    // all tags, including system
    Set<String> tags = service.listTags();
    assertThat(tags).containsOnly("tag1", "tag2", "sys1", "sys2");

    // verify in es
    tags = index.terms(RuleIndexDefinition.FIELD_RULE_ALL_TAGS);
    assertThat(tags).containsOnly("tag1", "tag2", "sys1", "sys2");
  }

  @Test(expected = UnauthorizedException.class)
  public void do_not_delete_if_not_granted() {
    userSessionRule.setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    service.delete(RuleKey.of("java", "S001"));
  }
}
