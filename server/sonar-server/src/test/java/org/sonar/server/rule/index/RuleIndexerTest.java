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

import com.google.common.collect.Iterators;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.config.Settings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.EsTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class RuleIndexerTest {

  @ClassRule
  public static EsTester esTester = new EsTester().addDefinitions(new RuleIndexDefinition(new Settings()));

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Before
  public void setUp() {
    esTester.truncateIndices();
  }

  @Test
  public void index_nothing() {
    RuleIndexer indexer = createIndexer();
    indexer.index(Iterators.<RuleDoc>emptyIterator());
    assertThat(esTester.countDocuments(RuleIndexDefinition.INDEX, RuleIndexDefinition.TYPE_RULE)).isEqualTo(0L);
  }

  @Test
  public void index_nothing_if_disabled() {
    dbTester.prepareDbUnit(getClass(), "index.xml");

    createIndexer().setEnabled(false).index();

    assertThat(esTester.countDocuments(RuleIndexDefinition.INDEX, RuleIndexDefinition.TYPE_RULE)).isZero();
  }

  @Test
  public void index() {
    dbTester.prepareDbUnit(getClass(), "index.xml");

    RuleIndexer indexer = createIndexer();
    indexer.index();

    assertThat(esTester.countDocuments(RuleIndexDefinition.INDEX, RuleIndexDefinition.TYPE_RULE)).isEqualTo(1);
  }

  @Test
  public void removed_rule_is_not_removed_from_index() {
    RuleIndexer indexer = createIndexer();

    // Create and Index rule
    RuleDto ruleDto = RuleTesting.newDto(RuleKey.of("xoo", "S001"))
      .setStatus(RuleStatus.READY)
      .setUpdatedAt(1000L);
    dbTester.getDbClient().ruleDao().insert(dbTester.getSession(), ruleDto);
    dbTester.getSession().commit();
    indexer.index();

    // Remove rule
    ruleDto.setStatus(RuleStatus.REMOVED);
    ruleDto.setUpdatedAt(2000L);
    dbTester.getDbClient().ruleDao().update(dbTester.getSession(), ruleDto);
    dbTester.getSession().commit();
    indexer.index();

    assertThat(esTester.countDocuments(RuleIndexDefinition.INDEX, RuleIndexDefinition.TYPE_RULE)).isEqualTo(1);
  }

  private RuleIndexer createIndexer() {
    RuleIndexer indexer = new RuleIndexer(dbTester.getDbClient(), esTester.client());
    indexer.setEnabled(true);
    return indexer;
  }

}
