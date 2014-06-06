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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.platform.Platform;
import org.sonar.server.qualityprofile.QProfileService;
import org.sonar.server.qualityprofile.RuleActivation;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.rule.index.RuleResult;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class RegisterRulesMediumTest {

  // Hack to restart server without x2
  static boolean includeX1 = true, includeX2 = true;

  @org.junit.Rule
  public ServerTester tester = new ServerTester().addComponents(XooRulesDefinition.class);

  DbClient db;
  DbSession dbSession;

  @Before
  public void before() {
    db = tester.get(DbClient.class);
    dbSession = tester.get(DbClient.class).openSession(false);
  }

  @After
  public void after() {
    dbSession.close();
    includeX1 = true;
    includeX2 = true;
  }

  @Test
  public void register_rules_at_startup() throws Exception {
    verifyTwoRulesInDb();

    RuleIndex index = tester.get(RuleIndex.class);

    RuleResult searchResult = index.search(new RuleQuery(), new QueryOptions());
    assertThat(searchResult.getTotal()).isEqualTo(2);
    assertThat(searchResult.getHits()).hasSize(2);
  }

  /**
   * support the use-case:
   * 1. start server
   * 2. stop server
   * 3. drop elasticsearch index: rm -rf data/es
   * 4. start server -> db is up-to-date (no changes) but rules must be re-indexed
   */
  @Test
  public void index_even_if_no_changes() throws Exception {
    RuleIndex index = tester.get(RuleIndex.class);

    verifyTwoRulesInDb();

    // clear ES but keep db
    tester.clearIndexes();
    verifyTwoRulesInDb();
    RuleResult searchResult = index.search(new RuleQuery(), new QueryOptions());
    assertThat(searchResult.getTotal()).isEqualTo(0);
    assertThat(searchResult.getHits()).hasSize(0);

    // db is not updated (same rules) but es must be reindexed
    tester.get(Platform.class).restart();

    index = tester.get(RuleIndex.class);

    verifyTwoRulesInDb();
    searchResult = index.search(new RuleQuery().setKey("xoo:x1"), new QueryOptions());
    assertThat(searchResult.getTotal()).isEqualTo(1);
    assertThat(searchResult.getHits()).hasSize(1);
    assertThat(searchResult.getHits().get(0).params()).hasSize(1);
  }

  @Test
  public void mark_rule_as_removed() throws Exception {
    verifyTwoRulesInDb();

    includeX2 = false;
    tester.get(Platform.class).restart();

    verifyTwoRulesInDb();
    RuleDto rule = db.ruleDao().getByKey(dbSession, RuleKey.of("xoo", "x2"));
    assertThat(rule.getStatus()).isEqualTo(RuleStatus.REMOVED);
  }

  @Test
  public void deactivate_removed_rules_only_if_repository_still_exists() throws Exception {
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN).setLogin("me");

    // create a profile and activate rule
    QualityProfileKey profileKey = QualityProfileKey.of("P1", "xoo");
    db.qualityProfileDao().insert(dbSession, QualityProfileDto.createFor(profileKey));
    dbSession.commit();
    dbSession.clearCache();
    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(profileKey, RuleKey.of("xoo", "x1"));
    RuleActivation activation = new RuleActivation(activeRuleKey);
    tester.get(QProfileService.class).activate(activation);
    dbSession.clearCache();

    // restart, x2 still exists -> deactivate x1
    includeX1 = false;
    includeX2 = true;
    tester.get(Platform.class).restart();
    dbSession.clearCache();
    assertThat(db.ruleDao().getByKey(dbSession, RuleKey.of("xoo", "x1")).getStatus()).isEqualTo(RuleStatus.REMOVED);
    assertThat(db.ruleDao().getByKey(dbSession, RuleKey.of("xoo", "x2")).getStatus()).isEqualTo(RuleStatus.READY);
    assertThat(db.activeRuleDao().findByProfileKey(dbSession, profileKey)).hasSize(0);
  }

  @Test
  public void do_not_deactivate_removed_rules_if_repository_accidentaly_uninstalled() throws Exception {
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN).setLogin("me");

    // create a profile and activate rule
    QualityProfileKey profileKey = QualityProfileKey.of("P1", "xoo");
    db.qualityProfileDao().insert(dbSession, QualityProfileDto.createFor(profileKey));
    dbSession.commit();
    dbSession.clearCache();
    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(profileKey, RuleKey.of("xoo", "x1"));
    RuleActivation activation = new RuleActivation(activeRuleKey);
    tester.get(QProfileService.class).activate(activation);
    dbSession.clearCache();

    // restart without x1 and x2 -> keep active rule of x1
    includeX1 = false;
    includeX2 = false;
    tester.get(Platform.class).restart();
    dbSession.clearCache();
    assertThat(db.ruleDao().getByKey(dbSession, RuleKey.of("xoo", "x1")).getStatus()).isEqualTo(RuleStatus.REMOVED);
    assertThat(db.ruleDao().getByKey(dbSession, RuleKey.of("xoo", "x2")).getStatus()).isEqualTo(RuleStatus.REMOVED);
    assertThat(db.activeRuleDao().findByProfileKey(dbSession, profileKey)).hasSize(1);
  }

  private void verifyTwoRulesInDb() {
    List<RuleDto> rules = db.ruleDao().findAll(dbSession);
    assertThat(rules).hasSize(2);
    List<RuleParamDto> ruleParams = db.ruleDao().findAllRuleParams(dbSession);
    assertThat(ruleParams).hasSize(1);
  }

  public static class XooRulesDefinition implements RulesDefinition {

    @Override
    public void define(Context context) {
      if (includeX1 || includeX2) {
        NewRepository repository = context.createRepository("xoo", "xoo").setName("Xoo Repo");
        if (includeX1) {
          repository.createRule("x1")
            .setName("x1 name")
            .setHtmlDescription("x1 desc")
            .setSeverity(Severity.MINOR)
            .createParam("acceptWhitespace")
            .setDefaultValue("false")
            .setType(RuleParamType.BOOLEAN)
            .setDescription("Accept whitespaces on the line");
        }

        if (includeX2) {
          repository.createRule("x2")
            .setName("x2 name")
            .setHtmlDescription("x2 desc")
            .setSeverity(Severity.MAJOR);
        }
        repository.done();
      }
    }
  }
}
