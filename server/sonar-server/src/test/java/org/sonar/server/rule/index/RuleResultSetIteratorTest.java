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
package org.sonar.server.rule.index;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nonnull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;


public class RuleResultSetIteratorTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Before
  public void setUp() {
    dbTester.truncateTables();
  }

  @Test
  public void iterator_over_one_rule() {
    dbTester.prepareDbUnit(getClass(), "one_rule.xml");
    RuleResultSetIterator it = RuleResultSetIterator.create(dbTester.getDbClient(), dbTester.getSession(), 0L);
    Map<String, RuleDoc> rulesByKey = rulesByKey(it);
    it.close();

    assertThat(rulesByKey).hasSize(1);

    RuleDoc rule = rulesByKey.get("S001");
    assertThat(rule.key()).isEqualTo(RuleKey.of("xoo", "S001"));
    assertThat(rule.keyAsList()).containsOnly("xoo", "S001");
    assertThat(rule.ruleKey()).isEqualTo("S001");
    assertThat(rule.repository()).isEqualTo("xoo");
    assertThat(rule.internalKey()).isEqualTo("S1");
    assertThat(rule.name()).isEqualTo("Null Pointer");
    assertThat(rule.htmlDescription()).isEqualTo("S001 desc");
    assertThat(rule.language()).isEqualTo("xoo");
    assertThat(rule.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(rule.status()).isEqualTo(RuleStatus.READY);
    assertThat(rule.isTemplate()).isFalse();
    assertThat(rule.allTags()).containsOnly("bug", "performance", "cwe");
    assertThat(rule.createdAt()).isEqualTo(1500000000000L);
    assertThat(rule.updatedAt()).isEqualTo(1600000000000L);
  }

  @Test
  public void select_after_date() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    RuleResultSetIterator it = RuleResultSetIterator.create(dbTester.getDbClient(), dbTester.getSession(), 1_900_000_000_000L);

    assertThat(it.hasNext()).isTrue();
    RuleDoc issue = it.next();
    assertThat(issue.key()).isEqualTo(RuleKey.of("xoo", "S002"));

    assertThat(it.hasNext()).isFalse();
    it.close();
  }

  @Test
  public void iterator_over_rules() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    RuleResultSetIterator it = RuleResultSetIterator.create(dbTester.getDbClient(), dbTester.getSession(), 0L);
    Map<String, RuleDoc> rulesByKey = rulesByKey(it);
    it.close();

    assertThat(rulesByKey).hasSize(2);

    RuleDoc rule = rulesByKey.get("S001");
    assertThat(rule.key()).isEqualTo(RuleKey.of("xoo", "S001"));
    assertThat(rule.keyAsList()).containsOnly("xoo", "S001");
    assertThat(rule.ruleKey()).isEqualTo("S001");
    assertThat(rule.repository()).isEqualTo("xoo");
    assertThat(rule.internalKey()).isEqualTo("S1");
    assertThat(rule.name()).isEqualTo("Null Pointer");
    assertThat(rule.htmlDescription()).isEqualTo("S001 desc");
    assertThat(rule.language()).isEqualTo("xoo");
    assertThat(rule.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(rule.status()).isEqualTo(RuleStatus.READY);
    assertThat(rule.isTemplate()).isFalse();
    assertThat(rule.allTags()).containsOnly("bug", "performance", "cwe");
    assertThat(rule.createdAt()).isEqualTo(1500000000000L);
    assertThat(rule.updatedAt()).isEqualTo(1600000000000L);

    rule = rulesByKey.get("S002");
    assertThat(rule.key()).isEqualTo(RuleKey.of("xoo", "S002"));
    assertThat(rule.keyAsList()).containsOnly("xoo", "S002");
    assertThat(rule.ruleKey()).isEqualTo("S002");
    assertThat(rule.repository()).isEqualTo("xoo");
    assertThat(rule.internalKey()).isEqualTo("S2");
    assertThat(rule.name()).isEqualTo("Slow");
    assertThat(rule.htmlDescription()).isEqualTo("<strong>S002 desc</strong>");
    assertThat(rule.language()).isEqualTo("xoo");
    assertThat(rule.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(rule.status()).isEqualTo(RuleStatus.BETA);
    assertThat(rule.isTemplate()).isTrue();
    assertThat(rule.allTags()).isEmpty();
    assertThat(rule.createdAt()).isEqualTo(2000000000000L);
    assertThat(rule.updatedAt()).isEqualTo(2100000000000L);
  }

  @Test
  public void custom_rule() {
    dbTester.prepareDbUnit(getClass(), "custom_rule.xml");

    RuleResultSetIterator it = RuleResultSetIterator.create(dbTester.getDbClient(), dbTester.getSession(), 0L);
    Map<String, RuleDoc> rulesByKey = rulesByKey(it);
    it.close();

    assertThat(rulesByKey).hasSize(2);

    RuleDoc rule = rulesByKey.get("S001");
    assertThat(rule.isTemplate()).isTrue();
    assertThat(rule.templateKey()).isNull();

    rule = rulesByKey.get("S002");
    assertThat(rule.isTemplate()).isFalse();
    assertThat(rule.templateKey()).isEqualTo(RuleKey.of("xoo", "S001"));
  }

  @Test
  public void removed_rule_is_returned() {
    dbTester.prepareDbUnit(getClass(), "removed_rule.xml");
    RuleResultSetIterator it = RuleResultSetIterator.create(dbTester.getDbClient(), dbTester.getSession(), 0L);
    Map<String, RuleDoc> rulesByKey = rulesByKey(it);
    it.close();

    assertThat(rulesByKey).hasSize(1);
  }

  private static Map<String, RuleDoc> rulesByKey(RuleResultSetIterator it) {
    return Maps.uniqueIndex(it, new Function<RuleDoc, String>() {
      @Override
      public String apply(@Nonnull RuleDoc rule) {
        return rule.key().rule();
      }
    });
  }
}
