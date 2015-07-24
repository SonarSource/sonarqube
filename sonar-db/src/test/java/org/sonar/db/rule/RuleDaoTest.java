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
package org.sonar.db.rule;

import com.google.common.base.Optional;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.RowNotFoundException;
import org.sonar.test.DbTests;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class RuleDaoTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  RuleDao underTest = dbTester.getDbClient().ruleDao();

  @Test
  public void selectByKey() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectByKey(dbTester.getSession(), RuleKey.of("NOT", "FOUND")).isPresent()).isFalse();

    Optional<RuleDto> rule = underTest.selectByKey(dbTester.getSession(), RuleKey.of("java", "S001"));
    assertThat(rule.isPresent()).isTrue();
    assertThat(rule.get().getId()).isEqualTo(1);
  }

  @Test
  public void selectOrFailByKey() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    RuleDto rule = underTest.selectOrFailByKey(dbTester.getSession(), RuleKey.of("java", "S001"));
    assertThat(rule.getId()).isEqualTo(1);
  }

  @Test
  public void selectOrFailByKey_fails_if_rule_not_found() {
    thrown.expect(RowNotFoundException.class);
    thrown.expectMessage("Rule with key 'NOT:FOUND' does not exist");

    dbTester.prepareDbUnit(getClass(), "shared.xml");

    underTest.selectOrFailByKey(dbTester.getSession(), RuleKey.of("NOT", "FOUND"));
  }

  @Test
  public void selectByKeys() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectByKeys(dbTester.getSession(), Collections.<RuleKey>emptyList())).isEmpty();
    assertThat(underTest.selectByKeys(dbTester.getSession(), asList(RuleKey.of("NOT", "FOUND"))).isEmpty());

    List<RuleDto> rules = underTest.selectByKeys(dbTester.getSession(), asList(RuleKey.of("java", "S001"), RuleKey.of("java", "OTHER")));
    assertThat(rules).hasSize(1);
    assertThat(rules.get(0).getId()).isEqualTo(1);
  }
}
