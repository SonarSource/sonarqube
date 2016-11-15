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
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.es.EsTester;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;

public class RuleIndexerTest {

  private System2 system2 = System2.INSTANCE;

  @Rule
  public EsTester esTester = new EsTester(new RuleIndexDefinition(new MapSettings()));

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();
  private RuleDto rule = new RuleDto()
    .setRuleKey("S001")
    .setRepositoryKey("xoo")
    .setConfigKey("S1")
    .setName("Null Pointer")
    .setDescription("S001 desc")
    .setDescriptionFormat(RuleDto.Format.HTML)
    .setLanguage("xoo")
    .setSeverity(Severity.BLOCKER)
    .setStatus(RuleStatus.READY)
    .setIsTemplate(true)
    .setTags(newHashSet("performance"))
    .setSystemTags(newHashSet("cwe"))
    .setType(RuleType.BUG)
    .setCreatedAt(1500000000000L)
    .setUpdatedAt(1600000000000L);

  @Test
  public void index_nothing() {
    RuleIndexer indexer = createIndexer();
    indexer.index(Iterators.emptyIterator());
    assertThat(esTester.countDocuments(RuleIndexDefinition.INDEX, RuleIndexDefinition.TYPE_RULE)).isEqualTo(0L);
  }

  @Test
  public void index() {
    dbClient.ruleDao().insert(dbSession, rule);
    dbSession.commit();

    RuleIndexer indexer = createIndexer();
    indexer.index();

    assertThat(esTester.countDocuments(RuleIndexDefinition.INDEX, RuleIndexDefinition.TYPE_RULE)).isEqualTo(1);
  }

  @Test
  public void removed_rule_is_not_removed_from_index() {
    RuleIndexer indexer = createIndexer();

    // Create and Index rule
    dbClient.ruleDao().insert(dbSession, rule.setStatus(RuleStatus.READY));
    dbSession.commit();
    indexer.index();
    assertThat(esTester.countDocuments(RuleIndexDefinition.INDEX, RuleIndexDefinition.TYPE_RULE)).isEqualTo(1);

    // Remove rule
    dbTester.getDbClient().ruleDao().update(dbTester.getSession(), rule.setStatus(RuleStatus.READY).setUpdatedAt(2000000000000L));
    dbTester.getSession().commit();
    indexer.index();

    assertThat(esTester.countDocuments(RuleIndexDefinition.INDEX, RuleIndexDefinition.TYPE_RULE)).isEqualTo(1);
  }

  private RuleIndexer createIndexer() {
    return new RuleIndexer(system2, dbTester.getDbClient(), esTester.client());
  }

}
