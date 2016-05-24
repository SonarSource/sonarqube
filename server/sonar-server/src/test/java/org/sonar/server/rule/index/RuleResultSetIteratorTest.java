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
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.rule.RuleDto;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;

public class RuleResultSetIteratorTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  DbClient dbClient = dbTester.getDbClient();

  DbSession dbSession = dbTester.getSession();

  RuleDto templateRule = new RuleDto()
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

  RuleDto customRule = new RuleDto()
    .setRuleKey("S002")
    .setRepositoryKey("xoo")
    .setConfigKey("S2")
    .setName("Slow")
    .setDescription("*S002 desc*")
    .setDescriptionFormat(RuleDto.Format.MARKDOWN)
    .setLanguage("xoo")
    .setSeverity(Severity.MAJOR)
    .setStatus(RuleStatus.BETA)
    .setIsTemplate(false)
    .setType(RuleType.CODE_SMELL)
    .setCreatedAt(2000000000000L)
    .setUpdatedAt(2100000000000L);

  @Test
  public void iterator_over_one_rule() {
    dbClient.ruleDao().insert(dbSession, templateRule);
    dbSession.commit();

    RuleResultSetIterator it = RuleResultSetIterator.create(dbTester.getDbClient(), dbTester.getSession(), 0L);
    Map<String, RuleDoc> rulesByKey = rulesByKey(it);
    it.close();

    assertThat(rulesByKey).hasSize(1);

    RuleDoc rule = rulesByKey.get(templateRule.getRuleKey());
    assertThat(rule).isNotNull();
    assertThat(rule.key()).isEqualTo(RuleKey.of("xoo", "S001"));
    assertThat(rule.ruleKey()).isEqualTo("S001");
    assertThat(rule.repository()).isEqualTo("xoo");
    assertThat(rule.internalKey()).isEqualTo("S1");
    assertThat(rule.name()).isEqualTo("Null Pointer");
    assertThat(rule.htmlDescription()).isEqualTo("S001 desc");
    assertThat(rule.language()).isEqualTo("xoo");
    assertThat(rule.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(rule.status()).isEqualTo(RuleStatus.READY);
    assertThat(rule.isTemplate()).isTrue();
    assertThat(rule.allTags()).containsOnly("performance", "cwe");
    assertThat(rule.createdAt()).isEqualTo(1500000000000L);
    assertThat(rule.updatedAt()).isEqualTo(1600000000000L);
  }

  @Test
  public void select_after_date() {
    dbClient.ruleDao().insert(dbSession, templateRule);
    dbClient.ruleDao().insert(dbSession, customRule);
    dbSession.commit();

    RuleResultSetIterator it = RuleResultSetIterator.create(dbTester.getDbClient(), dbTester.getSession(), 1_900_000_000_000L);

    assertThat(it.hasNext()).isTrue();
    RuleDoc issue = it.next();
    assertThat(issue.key()).isEqualTo(RuleKey.of("xoo", "S002"));

    assertThat(it.hasNext()).isFalse();
    it.close();
  }

  @Test
  public void iterator_over_rules() {
    dbClient.ruleDao().insert(dbSession, templateRule);
    dbClient.ruleDao().insert(dbSession, customRule);
    dbSession.commit();

    RuleResultSetIterator it = RuleResultSetIterator.create(dbTester.getDbClient(), dbTester.getSession(), 0L);
    Map<String, RuleDoc> rulesByKey = rulesByKey(it);
    it.close();

    assertThat(rulesByKey).hasSize(2);

    RuleDoc rule = rulesByKey.get(templateRule.getRuleKey());
    assertThat(rule.key()).isEqualTo(RuleKey.of("xoo", "S001"));
    assertThat(rule.ruleKey()).isEqualTo("S001");
    assertThat(rule.repository()).isEqualTo("xoo");
    assertThat(rule.internalKey()).isEqualTo("S1");
    assertThat(rule.name()).isEqualTo("Null Pointer");
    assertThat(rule.htmlDescription()).isEqualTo("S001 desc");
    assertThat(rule.language()).isEqualTo("xoo");
    assertThat(rule.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(rule.status()).isEqualTo(RuleStatus.READY);
    assertThat(rule.isTemplate()).isTrue();
    assertThat(rule.allTags()).containsOnly("performance", "cwe");
    assertThat(rule.createdAt()).isEqualTo(1500000000000L);
    assertThat(rule.updatedAt()).isEqualTo(1600000000000L);

    rule = rulesByKey.get(customRule.getRuleKey());
    assertThat(rule.key()).isEqualTo(RuleKey.of("xoo", "S002"));
    assertThat(rule.ruleKey()).isEqualTo("S002");
    assertThat(rule.repository()).isEqualTo("xoo");
    assertThat(rule.internalKey()).isEqualTo("S2");
    assertThat(rule.name()).isEqualTo("Slow");
    assertThat(rule.htmlDescription()).isEqualTo("<strong>S002 desc</strong>");
    assertThat(rule.language()).isEqualTo("xoo");
    assertThat(rule.severity()).isEqualTo(Severity.MAJOR);
    assertThat(rule.status()).isEqualTo(RuleStatus.BETA);
    assertThat(rule.isTemplate()).isFalse();
    assertThat(rule.allTags()).isEmpty();
    assertThat(rule.createdAt()).isEqualTo(2000000000000L);
    assertThat(rule.updatedAt()).isEqualTo(2100000000000L);
  }

  @Test
  public void custom_rule() {
    dbClient.ruleDao().insert(dbSession, templateRule);
    dbClient.ruleDao().insert(dbSession, customRule.setTemplateId(templateRule.getId()));
    dbSession.commit();

    RuleResultSetIterator it = RuleResultSetIterator.create(dbTester.getDbClient(), dbTester.getSession(), 0L);
    Map<String, RuleDoc> rulesByKey = rulesByKey(it);
    it.close();

    assertThat(rulesByKey).hasSize(2);

    RuleDoc rule = rulesByKey.get(templateRule.getRuleKey());
    assertThat(rule.isTemplate()).isTrue();
    assertThat(rule.templateKey()).isNull();

    rule = rulesByKey.get(customRule.getRuleKey());
    assertThat(rule.isTemplate()).isFalse();
    assertThat(rule.templateKey()).isEqualTo(RuleKey.of("xoo", "S001"));
  }

  @Test
  public void removed_rule_is_returned() {
    dbClient.ruleDao().insert(dbSession, templateRule.setStatus(RuleStatus.REMOVED));
    dbSession.commit();

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
