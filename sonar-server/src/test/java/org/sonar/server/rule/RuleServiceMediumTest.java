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

import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleNormalizer;
import org.sonar.server.tester.ServerTester;

import java.util.Collections;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

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
    dbSession = tester.get(DbClient.class).openSession(false);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void test_list_tags() throws InterruptedException {
    // insert db
    RuleKey key1 = RuleKey.of("javascript", "S001");
    RuleKey key2 = RuleKey.of("java", "S001");
    dao.insert(dbSession,
      RuleTesting.newDto(key1).setTags(Sets.newHashSet("tag1")).setSystemTags(Sets.newHashSet("sys1", "sys2")),
      RuleTesting.newDto(key2).setTags(Sets.newHashSet("tag2")).setSystemTags(Collections.<String>emptySet()));
    dbSession.commit();

    // all tags, including system
    Set<String> tags = service.listTags();
    assertThat(tags).containsOnly("tag1", "tag2", "sys1", "sys2");

    // verify user tags in es
    tags = index.terms(RuleNormalizer.RuleField.TAGS.field());
    assertThat(tags).containsOnly("tag1", "tag2");

    // verify system tags in es
    tags = index.terms(RuleNormalizer.RuleField.SYSTEM_TAGS.field());
    assertThat(tags).containsOnly("sys1", "sys2");
  }
}
