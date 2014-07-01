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

import org.fest.assertions.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleQuery;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.tester.ServerTester;

import java.util.Collections;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

@Deprecated
public class DefaultRuleFinderMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  private DefaultRuleFinder finder;
  private DbSession session;

  @Before
  public void setup() {
    finder = tester.get(DefaultRuleFinder.class);

    session = tester.get(DbClient.class).openSession(false);

    tester.get(RuleDao.class).insert(session,
      new RuleDto()
        .setName("Check Header")
        .setConfigKey("Checker/Treewalker/HeaderCheck")
        .setRuleKey("com.puppycrawl.tools.checkstyle.checks.header.HeaderCheck")
        .setRepositoryKey("checkstyle")
        .setSeverity(4)
        .setStatus(RuleStatus.READY),
      new RuleDto()
        .setName("Disabled checked")
        .setConfigKey("Checker/Treewalker/DisabledCheck")
        .setRuleKey("DisabledCheck")
        .setRepositoryKey("checkstyle")
        .setSeverity(4)
        .setStatus(RuleStatus.REMOVED),
      new RuleDto()
        .setName("Check Annotation")
        .setConfigKey("Checker/Treewalker/AnnotationUseStyleCheck")
        .setRuleKey("com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck")
        .setRepositoryKey("checkstyle")
        .setSeverity(4)
        .setStatus(RuleStatus.READY),
      new RuleDto()
        .setName("Call Super First")
        .setConfigKey("rulesets/android.xml/CallSuperFirst")
        .setRuleKey("CallSuperFirst")
        .setRepositoryKey("pmd")
        .setSeverity(2)
        .setStatus(RuleStatus.READY)
    );
    session.commit();
  }

  @After
  public void after() {
    session.close();
  }


  @Test
  public void should_success_finder_wrap() {

    // has Id
    Assertions.assertThat(finder.findById(1).getId()).isEqualTo(1);

    // should_find_by_id
    Assertions.assertThat(finder.findById(3).getConfigKey()).isEqualTo("Checker/Treewalker/AnnotationUseStyleCheck");

    // should_not_find_disabled_rule_by_id
    Assertions.assertThat(finder.findById(2)).isNull();

    // should_find_by_ids
    // 2 is returned even its status is REMOVED !!! Conflicts with IMPL. //TODO check with @Simon
    assertThat(finder.findByIds(newArrayList(2, 3))).hasSize(1);

    // should_find_by_key
    Rule rule = finder.findByKey("checkstyle", "com.puppycrawl.tools.checkstyle.checks.header.HeaderCheck");
    Assertions.assertThat(rule).isNotNull();
    Assertions.assertThat(rule.getKey()).isEqualTo(("com.puppycrawl.tools.checkstyle.checks.header.HeaderCheck"));
    Assertions.assertThat(rule.isEnabled()).isTrue();

    // find_should_return_null_if_no_results
    Assertions.assertThat(finder.findByKey("checkstyle", "unknown")).isNull();
    Assertions.assertThat(finder.find(RuleQuery.create().withRepositoryKey("checkstyle").withConfigKey("unknown"))).isNull();

    // find_repository_rules
    Assertions.assertThat(finder.findAll(RuleQuery.create().withRepositoryKey("checkstyle"))).hasSize(2);

    // find_all_enabled
    //Assertions.assertThat(finder.findAll(RuleQuery.create())).onProperty("id").containsOnly(1, 3, 4);
    Assertions.assertThat(finder.findAll(RuleQuery.create())).hasSize(3);

    // do_not_find_disabled_rules
    Assertions.assertThat(finder.findByKey("checkstyle", "DisabledCheck")).isNull();

    // do_not_find_unknown_rules
    Assertions.assertThat(finder.findAll(RuleQuery.create().withRepositoryKey("unknown_repository"))).isEmpty();

    // should_find_by_ids_empty
    tester.clearDbAndIndexes();
    assertThat(finder.findByIds(Collections.<Integer>emptyList())).isEmpty();
  }
}
