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
package org.sonar.server.qualityprofile.index;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nonnull;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.server.qualityprofile.ActiveRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.CRITICAL;
import static org.sonar.api.rule.Severity.INFO;
import static org.sonar.server.qualityprofile.ActiveRule.Inheritance.INHERITED;

public class ActiveRuleResultSetIteratorTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Test
  public void iterator_over_one_active_rule() {
    dbTester.prepareDbUnit(getClass(), "one_active_rule.xml");
    ActiveRuleResultSetIterator it = ActiveRuleResultSetIterator.create(dbTester.getDbClient(), dbTester.getSession(), 0L);
    Map<ActiveRuleKey, ActiveRuleDoc> activeRulesByKey = activeRulesByKey(it);
    it.close();

    assertThat(activeRulesByKey).hasSize(1);

    ActiveRuleKey key = ActiveRuleKey.of("sonar-way", RuleKey.of("xoo", "S001"));
    ActiveRuleDoc activeRule = activeRulesByKey.get(key);
    assertThat(activeRule.key()).isEqualTo(key);
    assertThat(activeRule.severity()).isEqualTo(CRITICAL);
    assertThat(activeRule.inheritance()).isEqualTo(ActiveRule.Inheritance.NONE);
    assertThat(activeRule.createdAt()).isEqualTo(1500000000000L);
    assertThat(activeRule.updatedAt()).isEqualTo(1600000000000L);
  }

  @Test
  public void iterator_over_active_rules() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    ActiveRuleResultSetIterator it = ActiveRuleResultSetIterator.create(dbTester.getDbClient(), dbTester.getSession(), 0L);
    Map<ActiveRuleKey, ActiveRuleDoc> activeRulesByKey = activeRulesByKey(it);
    it.close();

    assertThat(activeRulesByKey).hasSize(3);

    ActiveRuleKey key = ActiveRuleKey.of("sonar-way", RuleKey.of("xoo", "S002"));
    ActiveRuleDoc activeRule = activeRulesByKey.get(key);
    assertThat(activeRule.key()).isEqualTo(key);
    assertThat(activeRule.severity()).isEqualTo(CRITICAL);
    assertThat(activeRule.inheritance()).isEqualTo(ActiveRule.Inheritance.NONE);
    assertThat(activeRule.createdAt()).isEqualTo(2000000000000L);
    assertThat(activeRule.updatedAt()).isEqualTo(2100000000000L);

    key = ActiveRuleKey.of("parent", RuleKey.of("xoo", "S001"));
    activeRule = activeRulesByKey.get(key);
    assertThat(activeRule.key()).isEqualTo(key);
    assertThat(activeRule.severity()).isEqualTo(INFO);
    assertThat(activeRule.inheritance()).isEqualTo(ActiveRule.Inheritance.NONE);
    assertThat(activeRule.createdAt()).isEqualTo(1700000000000L);
    assertThat(activeRule.updatedAt()).isEqualTo(1800000000000L);

    key = ActiveRuleKey.of("child", RuleKey.of("xoo", "S001"));
    activeRule = activeRulesByKey.get(key);
    assertThat(activeRule.key()).isEqualTo(key);
    assertThat(activeRule.severity()).isEqualTo(BLOCKER);
    assertThat(activeRule.inheritance()).isEqualTo(INHERITED);
    assertThat(activeRule.createdAt()).isEqualTo(1500000000000L);
    assertThat(activeRule.updatedAt()).isEqualTo(1600000000000L);
  }

  @Test
  public void active_rule_with_inherited_inheritance() {
    dbTester.prepareDbUnit(getClass(), "active_rule_with_inherited_inheritance.xml");
    ActiveRuleResultSetIterator it = ActiveRuleResultSetIterator.create(dbTester.getDbClient(), dbTester.getSession(), 0L);
    Map<ActiveRuleKey, ActiveRuleDoc> activeRulesByKey = activeRulesByKey(it);
    it.close();

    assertThat(activeRulesByKey).hasSize(2);

    ActiveRuleKey key = ActiveRuleKey.of("child", RuleKey.of("xoo", "S001"));
    ActiveRuleDoc activeRule = activeRulesByKey.get(key);
    assertThat(activeRule.inheritance()).isEqualTo(INHERITED);
  }

  @Test
  public void active_rule_with_overrides_inheritance() {
    dbTester.prepareDbUnit(getClass(), "active_rule_with_overrides_inheritance.xml");
    ActiveRuleResultSetIterator it = ActiveRuleResultSetIterator.create(dbTester.getDbClient(), dbTester.getSession(), 0L);
    Map<ActiveRuleKey, ActiveRuleDoc> activeRulesByKey = activeRulesByKey(it);
    it.close();

    assertThat(activeRulesByKey).hasSize(2);

    ActiveRuleKey key = ActiveRuleKey.of("child", RuleKey.of("xoo", "S001"));
    ActiveRuleDoc activeRule = activeRulesByKey.get(key);
    assertThat(activeRule.inheritance()).isEqualTo(ActiveRule.Inheritance.OVERRIDES);
  }

  @Test
  public void select_after_date() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    ActiveRuleResultSetIterator it = ActiveRuleResultSetIterator.create(dbTester.getDbClient(), dbTester.getSession(), 1_900_000_000_000L);

    assertThat(it.hasNext()).isTrue();
    ActiveRuleDoc doc = it.next();
    assertThat(doc.key()).isEqualTo(ActiveRuleKey.of("sonar-way", RuleKey.of("xoo", "S002")));

    assertThat(it.hasNext()).isFalse();
    it.close();
  }

  private static Map<ActiveRuleKey, ActiveRuleDoc> activeRulesByKey(ActiveRuleResultSetIterator it) {
    return Maps.uniqueIndex(it, DocToKey.INSTANCE);
  }

  private enum DocToKey implements Function<ActiveRuleDoc, ActiveRuleKey> {
    INSTANCE;

    @Override
    public ActiveRuleKey apply(@Nonnull ActiveRuleDoc doc) {
      return doc.key();
    }
  }

}
