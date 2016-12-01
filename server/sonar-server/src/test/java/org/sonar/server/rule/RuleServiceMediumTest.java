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

import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;

public class RuleServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().withEsIndexes();

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private RuleDao dao = tester.get(RuleDao.class);
  private RuleService service = tester.get(RuleService.class);
  private DbSession dbSession;
  private RuleIndexer ruleIndexer;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    dbSession = tester.get(DbClient.class).openSession(false);
    ruleIndexer = tester.get(RuleIndexer.class);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void listTags_returns_all_tags() {
    // insert db
    insertRule(RuleKey.of("javascript", "S001"), newHashSet("tag1"), newHashSet("sys1", "sys2"));
    insertRule(RuleKey.of("java", "S001"), newHashSet("tag2"), newHashSet());

    // all tags, including system
    Set<String> tags = service.listTags();
    assertThat(tags).containsOnly("tag1", "tag2", "sys1", "sys2");
  }

  @Test
  public void listTags_returns_tags_filtered_by_name() {
    insertRule(RuleKey.of("javascript", "S001"), newHashSet("tag1", "misra++"), newHashSet("sys1", "sys2"));
    insertRule(RuleKey.of("java", "S001"), newHashSet("tag2"), newHashSet());

    assertThat(service.listTags("missing", 10)).isEmpty();
    assertThat(service.listTags("", 10)).containsOnly("tag1", "misra++", "tag2", "sys1", "sys2");
    assertThat(service.listTags("tag", 10)).containsOnly("tag1", "tag2");
    assertThat(service.listTags("sys", 10)).containsOnly("sys1", "sys2");
    assertThat(service.listTags("misra", 10)).containsOnly("misra++");
    assertThat(service.listTags("misra+", 10)).containsOnly("misra++");
    assertThat(service.listTags("++", 10)).containsOnly("misra++");

    // LIMITATION: case sensitive
    assertThat(service.listTags("TAG", 10)).isEmpty();
    assertThat(service.listTags("TAg", 10)).isEmpty();
    assertThat(service.listTags("MISSing", 10)).isEmpty();

    assertThat(service.listTags("misra-", 10)).isEmpty();
  }

  @Test
  public void listTags_returns_empty_results_if_filter_contains_regexp_special_characters() {
    insertRule(RuleKey.of("javascript", "S001"), newHashSet("misra++"), newHashSet("sys1", "sys2"));

    assertThat(service.listTags("mis[", 10)).isEmpty();
    assertThat(service.listTags("mis\\d", 10)).isEmpty();
    assertThat(service.listTags(".*", 10)).isEmpty();
    assertThat(service.listTags("<foo>", 10)).isEmpty();
  }

  @Test
  public void delete_throws_UnauthorizedException_if_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    service.delete(RuleKey.of("java", "S001"));
  }

  @Test
  public void delete_throws_ForbiddenException_if_not_administrator() {
    userSessionRule.login().setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    service.delete(RuleKey.of("java", "S001"));
  }

  private void insertRule(RuleKey key, Set<String> tags, Set<String> systemTags) {
    dao.insert(dbSession,
      RuleTesting.newDto(key).setTags(tags).setSystemTags(systemTags));
    dbSession.commit();
    ruleIndexer.index();
  }
}
