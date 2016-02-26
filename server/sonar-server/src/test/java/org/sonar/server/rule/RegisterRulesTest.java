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

import com.google.common.collect.Sets;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.INFO;

@Category(DbTests.class)
public class RegisterRulesTest {

  static final Date DATE1 = DateUtils.parseDateTime("2014-01-01T19:10:03+0100");
  static final Date DATE2 = DateUtils.parseDateTime("2014-02-01T12:10:03+0100");
  static final Date DATE3 = DateUtils.parseDateTime("2014-03-01T12:10:03+0100");

  static final RuleKey RULE_KEY1 = RuleKey.of("fake", "rule1");
  static final RuleKey RULE_KEY2 = RuleKey.of("fake", "rule2");
  static final RuleKey RULE_KEY3 = RuleKey.of("fake", "rule3");

  System2 system = mock(System2.class);;

  @org.junit.Rule
  public DbTester dbTester = DbTester.create(system);

  @ClassRule
  public static EsTester esTester = new EsTester().addDefinitions(new RuleIndexDefinition(new Settings()));

  RuleActivator ruleActivator = mock(RuleActivator.class);

  DbClient dbClient = dbTester.getDbClient();

  RuleIndexer ruleIndexer;

  RuleIndex ruleIndex;

  @Before
  public void before() {
    esTester.truncateIndices();
    when(system.now()).thenReturn(DATE1.getTime());
    ruleIndexer = new RuleIndexer(dbClient, esTester.client());
    ruleIndexer.setEnabled(true);
    ruleIndex = new RuleIndex(esTester.client());
  }

  @Test
  public void insert_new_rules() {
    execute(new FakeRepositoryV1());

    // verify db
    assertThat(dbClient.ruleDao().selectAll(dbTester.getSession())).hasSize(2);
    RuleDto rule1 = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), RULE_KEY1);
    assertThat(rule1.getName()).isEqualTo("One");
    assertThat(rule1.getDescription()).isEqualTo("Description of One");
    assertThat(rule1.getSeverityString()).isEqualTo(BLOCKER);
    assertThat(rule1.getTags()).isEmpty();
    assertThat(rule1.getSystemTags()).containsOnly("tag1", "tag2", "tag3");
    assertThat(rule1.getConfigKey()).isEqualTo("config1");
    assertThat(rule1.getStatus()).isEqualTo(RuleStatus.BETA);
    assertThat(rule1.getCreatedAt()).isEqualTo(DATE1);
    assertThat(rule1.getUpdatedAt()).isEqualTo(DATE1);
    assertThat(rule1.getDefaultRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
    assertThat(rule1.getDefaultRemediationCoefficient()).isEqualTo("5d");
    assertThat(rule1.getDefaultRemediationOffset()).isEqualTo("10h");

    List<RuleParamDto> params = dbClient.ruleDao().selectRuleParamsByRuleKey(dbTester.getSession(), RULE_KEY1);
    assertThat(params).hasSize(2);
    RuleParamDto param = getParam(params, "param1");
    assertThat(param.getDescription()).isEqualTo("parameter one");
    assertThat(param.getDefaultValue()).isEqualTo("default1");

    // verify index
    assertThat(ruleIndex.search(new RuleQuery(), new SearchOptions()).getIds()).containsOnly(RULE_KEY1, RULE_KEY2);
  }

  @Test
  public void update_and_remove_rules_on_changes() {
    execute(new FakeRepositoryV1());
    assertThat(dbClient.ruleDao().selectAll(dbTester.getSession())).hasSize(2);
    assertThat(esTester.getIds(RuleIndexDefinition.INDEX, RuleIndexDefinition.TYPE_RULE)).containsOnly(RULE_KEY1.toString(), RULE_KEY2.toString());

    // user adds tags and sets markdown note
    RuleDto rule1 = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), RULE_KEY1);
    rule1.setTags(Sets.newHashSet("usertag1", "usertag2"));
    rule1.setNoteData("user *note*");
    rule1.setNoteUserLogin("marius");
    dbClient.ruleDao().update(dbTester.getSession(), rule1);
    dbTester.getSession().commit();

    when(system.now()).thenReturn(DATE2.getTime());
    execute(new FakeRepositoryV2());

    // rule1 has been updated
    rule1 = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), RULE_KEY1);
    assertThat(rule1.getName()).isEqualTo("One v2");
    assertThat(rule1.getDescription()).isEqualTo("Description of One v2");
    assertThat(rule1.getSeverityString()).isEqualTo(INFO);
    assertThat(rule1.getTags()).containsOnly("usertag1", "usertag2");
    assertThat(rule1.getSystemTags()).containsOnly("tag1", "tag4");
    assertThat(rule1.getConfigKey()).isEqualTo("config1 v2");
    assertThat(rule1.getNoteData()).isEqualTo("user *note*");
    assertThat(rule1.getNoteUserLogin()).isEqualTo("marius");
    assertThat(rule1.getStatus()).isEqualTo(RuleStatus.READY);
    assertThat(rule1.getCreatedAt()).isEqualTo(DATE1);
    assertThat(rule1.getUpdatedAt()).isEqualTo(DATE2);
    // TODO check remediation function

    List<RuleParamDto> params = dbClient.ruleDao().selectRuleParamsByRuleKey(dbTester.getSession(), RULE_KEY1);
    assertThat(params).hasSize(2);
    RuleParamDto param = getParam(params, "param1");
    assertThat(param.getDescription()).isEqualTo("parameter one v2");
    assertThat(param.getDefaultValue()).isEqualTo("default1 v2");

    // rule2 has been removed -> status set to REMOVED but db row is not deleted
    RuleDto rule2 = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), RULE_KEY2);
    assertThat(rule2.getStatus()).isEqualTo(RuleStatus.REMOVED);
    assertThat(rule2.getUpdatedAt()).isEqualTo(DATE2);

    // rule3 has been created
    RuleDto rule3 = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), RULE_KEY3);
    assertThat(rule3).isNotNull();
    assertThat(rule3.getStatus()).isEqualTo(RuleStatus.READY);

    assertThat(ruleIndex.search(new RuleQuery(), new SearchOptions()).getIds()).containsOnly(RULE_KEY1, RULE_KEY3);
  }

  @Test
  public void update_only_rule_name() throws Exception {
    when(system.now()).thenReturn(DATE1.getTime());
    execute(new RulesDefinition() {
      @Override
      public void define(Context context) {
        NewRepository repo = context.createRepository("fake", "java");
        repo.createRule("rule")
          .setName("Name1")
          .setHtmlDescription("Description");
        repo.done();
      }
    });

    when(system.now()).thenReturn(DATE2.getTime());
    execute(new RulesDefinition() {
      @Override
      public void define(Context context) {
        NewRepository repo = context.createRepository("fake", "java");
        repo.createRule("rule")
          .setName("Name2")
          .setHtmlDescription("Description");
        repo.done();
      }
    });

    // rule1 has been updated
    RuleDto rule1 = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), RuleKey.of("fake", "rule"));
    assertThat(rule1.getName()).isEqualTo("Name2");
    assertThat(rule1.getDescription()).isEqualTo("Description");

    assertThat(ruleIndex.search(new RuleQuery().setQueryText("Name2"), new SearchOptions()).getTotal()).isEqualTo(1);
    assertThat(ruleIndex.search(new RuleQuery().setQueryText("Name1"), new SearchOptions()).getTotal()).isEqualTo(0);
  }

  @Test
  public void update_only_rule_description() throws Exception {
    when(system.now()).thenReturn(DATE1.getTime());
    execute(new RulesDefinition() {
      @Override
      public void define(Context context) {
        NewRepository repo = context.createRepository("fake", "java");
        repo.createRule("rule")
          .setName("Name")
          .setHtmlDescription("Desc1");
        repo.done();
      }
    });

    when(system.now()).thenReturn(DATE2.getTime());
    execute(new RulesDefinition() {
      @Override
      public void define(Context context) {
        NewRepository repo = context.createRepository("fake", "java");
        repo.createRule("rule")
          .setName("Name")
          .setHtmlDescription("Desc2");
        repo.done();
      }
    });

    // rule1 has been updated
    RuleDto rule1 = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), RuleKey.of("fake", "rule"));
    assertThat(rule1.getName()).isEqualTo("Name");
    assertThat(rule1.getDescription()).isEqualTo("Desc2");

    assertThat(ruleIndex.search(new RuleQuery().setQueryText("Desc2"), new SearchOptions()).getTotal()).isEqualTo(1);
    assertThat(ruleIndex.search(new RuleQuery().setQueryText("Desc1"), new SearchOptions()).getTotal()).isEqualTo(0);
  }

  @Test
  public void disable_then_enable_rule() throws Exception {
    // Install rule
    when(system.now()).thenReturn(DATE1.getTime());
    execute(new FakeRepositoryV1());

    // Uninstall rule
    when(system.now()).thenReturn(DATE2.getTime());
    execute();

    RuleDto rule = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), RULE_KEY1);
    assertThat(rule.getStatus()).isEqualTo(RuleStatus.REMOVED);
    assertThat(ruleIndex.search(new RuleQuery().setKey(RULE_KEY1.toString()), new SearchOptions()).getTotal()).isEqualTo(0);

    // Re-install rule
    when(system.now()).thenReturn(DATE3.getTime());
    execute(new FakeRepositoryV1());

    rule = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), RULE_KEY1);
    assertThat(rule.getStatus()).isEqualTo(RuleStatus.BETA);
    assertThat(ruleIndex.search(new RuleQuery().setKey(RULE_KEY1.toString()), new SearchOptions()).getTotal()).isEqualTo(1);
  }

  @Test
  public void do_not_update_rules_when_no_changes() {
    execute(new FakeRepositoryV1());
    assertThat(dbClient.ruleDao().selectAll(dbTester.getSession())).hasSize(2);

    when(system.now()).thenReturn(DATE2.getTime());
    execute(new FakeRepositoryV1());

    RuleDto rule1 = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), RULE_KEY1);
    assertThat(rule1.getCreatedAt()).isEqualTo(DATE1);
    assertThat(rule1.getUpdatedAt()).isEqualTo(DATE1);
  }

  @Test
  public void do_not_update_already_removed_rules() {
    execute(new FakeRepositoryV1());
    assertThat(dbClient.ruleDao().selectAll(dbTester.getSession())).hasSize(2);
    assertThat(esTester.getIds(RuleIndexDefinition.INDEX, RuleIndexDefinition.TYPE_RULE)).containsOnly(RULE_KEY1.toString(), RULE_KEY2.toString());

    RuleDto rule2 = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), RULE_KEY2);
    assertThat(rule2.getStatus()).isEqualTo(RuleStatus.READY);

    when(system.now()).thenReturn(DATE2.getTime());
    execute(new FakeRepositoryV2());

    // On MySQL, need to update a rule otherwise rule2 will be seen as READY, but why ???
    dbClient.ruleDao().update(dbTester.getSession(), dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), RULE_KEY1));
    dbTester.getSession().commit();

    // rule2 is removed
    rule2 = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), RULE_KEY2);
    assertThat(rule2.getStatus()).isEqualTo(RuleStatus.REMOVED);

    assertThat(ruleIndex.search(new RuleQuery(), new SearchOptions()).getIds()).containsOnly(RULE_KEY1, RULE_KEY3);

    when(system.now()).thenReturn(DATE3.getTime());
    execute(new FakeRepositoryV2());
    dbTester.getSession().commit();

    // -> rule2 is still removed, but not update at DATE3
    rule2 = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), RULE_KEY2);
    assertThat(rule2.getStatus()).isEqualTo(RuleStatus.REMOVED);
    assertThat(rule2.getUpdatedAt()).isEqualTo(DATE2);

    assertThat(ruleIndex.search(new RuleQuery(), new SearchOptions()).getIds()).containsOnly(RULE_KEY1, RULE_KEY3);
  }

  @Test
  public void mass_insert() {
    execute(new BigRepository());
    assertThat(dbTester.countRowsOfTable("rules")).isEqualTo(BigRepository.SIZE);
    assertThat(dbTester.countRowsOfTable("rules_parameters")).isEqualTo(BigRepository.SIZE * 20);
    assertThat(esTester.getIds(RuleIndexDefinition.INDEX, RuleIndexDefinition.TYPE_RULE)).hasSize(BigRepository.SIZE);
  }

  @Test
  public void manage_repository_extensions() {
    execute(new FindbugsRepository(), new FbContribRepository());
    List<RuleDto> rules = dbClient.ruleDao().selectAll(dbTester.getSession());
    assertThat(rules).hasSize(2);
    for (RuleDto rule : rules) {
      assertThat(rule.getRepositoryKey()).isEqualTo("findbugs");
    }
  }

  private void execute(RulesDefinition... defs) {
    RuleDefinitionsLoader loader = new RuleDefinitionsLoader(mock(DeprecatedRulesDefinitionLoader.class), new RuleRepositories(), mock(CommonRuleDefinitionsImpl.class), defs);
    Languages languages = mock(Languages.class);
    when(languages.get("java")).thenReturn(mock(Language.class));

    RegisterRules task = new RegisterRules(loader, ruleActivator, dbClient, ruleIndexer, languages, system);
    task.start();
    // Execute a commit to refresh session state as the task is using its own session
    dbTester.getSession().commit();
  }

  private RuleParamDto getParam(List<RuleParamDto> params, String key) {
    for (RuleParamDto param : params) {
      if (param.getName().equals(key)) {
        return param;
      }
    }
    return null;
  }

  static class FakeRepositoryV1 implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewRepository repo = context.createRepository("fake", "java");
      NewRule rule1 = repo.createRule("rule1")
        .setName("One")
        .setHtmlDescription("Description of One")
        .setSeverity(BLOCKER)
        .setInternalKey("config1")
        .setTags("tag1", "tag2", "tag3")
        .setStatus(RuleStatus.BETA)
        .setDebtSubCharacteristic("MEMORY_EFFICIENCY")
        .setEffortToFixDescription("squid.S115.effortToFix");
      rule1.setDebtRemediationFunction(rule1.debtRemediationFunctions().linearWithOffset("5d", "10h"));

      rule1.createParam("param1").setDescription("parameter one").setDefaultValue("default1");
      rule1.createParam("param2").setDescription("parameter two").setDefaultValue("default2");

      repo.createRule("rule2")
        .setName("Two")
        .setHtmlDescription("Minimal rule");
      repo.done();
    }
  }

  /**
   * FakeRepositoryV1 with some changes
   */
  static class FakeRepositoryV2 implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewRepository repo = context.createRepository("fake", "java");

      // almost all the attributes of rule1 are changed
      NewRule rule1 = repo.createRule("rule1")
        .setName("One v2")
        .setHtmlDescription("Description of One v2")
        .setSeverity(INFO)
        .setInternalKey("config1 v2")
        // tag2 and tag3 removed, tag4 added
        .setTags("tag1", "tag4")
        .setStatus(RuleStatus.READY)
        .setDebtSubCharacteristic("MEMORY_EFFICIENCY")
        .setEffortToFixDescription("squid.S115.effortToFix.v2");
      rule1.setDebtRemediationFunction(rule1.debtRemediationFunctions().linearWithOffset("6d", "2h"));
      rule1.createParam("param1").setDescription("parameter one v2").setDefaultValue("default1 v2");
      rule1.createParam("param2").setDescription("parameter two v2").setDefaultValue("default2 v2");

      // rule2 is dropped, rule3 is new
      repo.createRule("rule3")
        .setName("Three")
        .setHtmlDescription("Rule Three");
      repo.done();
    }
  }

  static class BigRepository implements RulesDefinition {
    static final int SIZE = 500;

    @Override
    public void define(Context context) {
      NewRepository repo = context.createRepository("big", "java");
      for (int i = 0; i < SIZE; i++) {
        NewRule rule = repo.createRule("rule" + i)
          .setName("name of " + i)
          .setHtmlDescription("description of " + i);
        for (int j = 0; j < 20; j++) {
          rule.createParam("param" + j);
        }

      }
      repo.done();
    }
  }

  static class FindbugsRepository implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewRepository repo = context.createRepository("findbugs", "java");
      repo.createRule("rule1")
        .setName("Rule One")
        .setHtmlDescription("Description of Rule One");
      repo.done();
    }
  }

  static class FbContribRepository implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewExtendedRepository repo = context.extendRepository("findbugs", "java");
      repo.createRule("rule2")
        .setName("Rule Two")
        .setHtmlDescription("Description of Rule Two");
      repo.done();
    }
  }
}
