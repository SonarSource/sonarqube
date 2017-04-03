/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleRepositoryDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.rule.index.RuleQuery;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.INFO;

public class RegisterRulesTest {

  private static final Date DATE1 = DateUtils.parseDateTime("2014-01-01T19:10:03+0100");
  private static final Date DATE2 = DateUtils.parseDateTime("2014-02-01T12:10:03+0100");
  private static final Date DATE3 = DateUtils.parseDateTime("2014-03-01T12:10:03+0100");

  private static final RuleKey RULE_KEY1 = RuleKey.of("fake", "rule1");
  private static final RuleKey RULE_KEY2 = RuleKey.of("fake", "rule2");
  private static final RuleKey RULE_KEY3 = RuleKey.of("fake", "rule3");

  private System2 system = mock(System2.class);

  @org.junit.Rule
  public DbTester dbTester = DbTester.create(system);
  @org.junit.Rule
  public EsTester esTester = new EsTester(new RuleIndexDefinition(new MapSettings()));

  private RuleActivator ruleActivator = mock(RuleActivator.class);
  private DbClient dbClient = dbTester.getDbClient();
  private RuleIndexer ruleIndexer;
  private ActiveRuleIndexer activeRuleIndexer;
  private RuleIndex ruleIndex;

  @Before
  public void before() {
    when(system.now()).thenReturn(DATE1.getTime());
    ruleIndexer = new RuleIndexer(esTester.client(), dbClient);
    ruleIndex = new RuleIndex(esTester.client());
    activeRuleIndexer = new ActiveRuleIndexer(system, dbClient, esTester.client());
  }

  @Test
  public void insert_new_rules() {
    execute(new FakeRepositoryV1());

    // verify db
    assertThat(dbClient.ruleDao().selectAllDefinitions(dbTester.getSession())).hasSize(2);
    RuleDto rule1 = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), dbTester.getDefaultOrganization().getUuid(), RULE_KEY1);
    assertThat(rule1.getName()).isEqualTo("One");
    assertThat(rule1.getDescription()).isEqualTo("Description of One");
    assertThat(rule1.getSeverityString()).isEqualTo(BLOCKER);
    assertThat(rule1.getTags()).isEmpty();
    assertThat(rule1.getSystemTags()).containsOnly("tag1", "tag2", "tag3");
    assertThat(rule1.getConfigKey()).isEqualTo("config1");
    assertThat(rule1.getStatus()).isEqualTo(RuleStatus.BETA);
    assertThat(rule1.getCreatedAt()).isEqualTo(DATE1.getTime());
    assertThat(rule1.getUpdatedAt()).isEqualTo(DATE1.getTime());
    assertThat(rule1.getDefRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
    assertThat(rule1.getDefRemediationGapMultiplier()).isEqualTo("5d");
    assertThat(rule1.getDefRemediationBaseEffort()).isEqualTo("10h");
    assertThat(rule1.getType()).isEqualTo(RuleType.CODE_SMELL.getDbConstant());

    List<RuleParamDto> params = dbClient.ruleDao().selectRuleParamsByRuleKey(dbTester.getSession(), RULE_KEY1);
    assertThat(params).hasSize(2);
    RuleParamDto param = getParam(params, "param1");
    assertThat(param.getDescription()).isEqualTo("parameter one");
    assertThat(param.getDefaultValue()).isEqualTo("default1");

    // verify index
    assertThat(ruleIndex.search(new RuleQuery(), new SearchOptions()).getIds()).containsOnly(RULE_KEY1, RULE_KEY2);

    // verify repositories
    assertThat(dbClient.ruleRepositoryDao().selectAll(dbTester.getSession())).extracting(RuleRepositoryDto::getKey).containsOnly("fake");
  }

  @Test
  public void insert_then_remove_rule() {
    String ruleKey = randomAlphanumeric(5);

    // register one rule
    execute(context -> {
      RulesDefinition.NewRepository repo = context.createRepository("fake", "java");
      repo.createRule(ruleKey)
        .setName(randomAlphanumeric(5))
        .setHtmlDescription(randomAlphanumeric(20));
      repo.done();
    });

    // verify db
    assertThat(dbClient.ruleDao().selectAllDefinitions(dbTester.getSession()))
      .extracting(RuleDefinitionDto::getKey)
      .extracting(RuleKey::rule)
      .containsExactly(ruleKey);

    // verify index
    assertThat(ruleIndex.search(new RuleQuery(), new SearchOptions()).getIds())
      .extracting(RuleKey::rule)
      .containsExactly(ruleKey);

    // register no rule
    execute(context -> context.createRepository("fake", "java").done());

    // verify db
    assertThat(dbClient.ruleDao().selectAllDefinitions(dbTester.getSession()))
      .extracting(RuleDefinitionDto::getKey)
      .extracting(RuleKey::rule)
      .containsExactly(ruleKey);
    assertThat(dbClient.ruleDao().selectAllDefinitions(dbTester.getSession()))
      .extracting(RuleDefinitionDto::getStatus)
      .containsExactly(RuleStatus.REMOVED);

    // verify index
    assertThat(ruleIndex.search(new RuleQuery(), new SearchOptions()).getIds())
      .extracting(RuleKey::rule)
      .isEmpty();
  }

  @Test
  public void delete_repositories_that_have_been_uninstalled() {
    RuleRepositoryDto repository = new RuleRepositoryDto("findbugs", "java", "Findbugs");
    DbSession dbSession = dbTester.getSession();
    dbTester.getDbClient().ruleRepositoryDao().insert(dbSession, asList(repository));
    dbSession.commit();

    execute(new FakeRepositoryV1());

    assertThat(dbTester.getDbClient().ruleRepositoryDao().selectAll(dbSession)).extracting(RuleRepositoryDto::getKey).containsOnly("fake");
  }

  @Test
  public void update_and_remove_rules_on_changes() {
    execute(new FakeRepositoryV1());
    assertThat(dbClient.ruleDao().selectAllDefinitions(dbTester.getSession())).hasSize(2);
    assertThat(esTester.getIds(RuleIndexDefinition.INDEX_TYPE_RULE)).containsOnly(RULE_KEY1.toString(), RULE_KEY2.toString());

    // user adds tags and sets markdown note
    String organizationUuid = dbTester.getDefaultOrganization().getUuid();
    RuleDto rule1 = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), organizationUuid, RULE_KEY1);
    rule1.setTags(newHashSet("usertag1", "usertag2"));
    rule1.setNoteData("user *note*");
    rule1.setNoteUserLogin("marius");
    dbClient.ruleDao().insertOrUpdate(dbTester.getSession(), rule1.getMetadata());
    dbTester.getSession().commit();

    when(system.now()).thenReturn(DATE2.getTime());
    execute(new FakeRepositoryV2());

    // rule1 has been updated
    rule1 = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), organizationUuid, RULE_KEY1);
    assertThat(rule1.getName()).isEqualTo("One v2");
    assertThat(rule1.getDescription()).isEqualTo("Description of One v2");
    assertThat(rule1.getSeverityString()).isEqualTo(INFO);
    assertThat(rule1.getTags()).containsOnly("usertag1", "usertag2");
    assertThat(rule1.getSystemTags()).containsOnly("tag1", "tag4");
    assertThat(rule1.getConfigKey()).isEqualTo("config1 v2");
    assertThat(rule1.getNoteData()).isEqualTo("user *note*");
    assertThat(rule1.getNoteUserLogin()).isEqualTo("marius");
    assertThat(rule1.getStatus()).isEqualTo(RuleStatus.READY);
    assertThat(rule1.getType()).isEqualTo(RuleType.BUG.getDbConstant());
    assertThat(rule1.getCreatedAt()).isEqualTo(DATE1.getTime());
    assertThat(rule1.getUpdatedAt()).isEqualTo(DATE2.getTime());
    // TODO check remediation function

    List<RuleParamDto> params = dbClient.ruleDao().selectRuleParamsByRuleKey(dbTester.getSession(), RULE_KEY1);
    assertThat(params).hasSize(2);
    RuleParamDto param = getParam(params, "param1");
    assertThat(param.getDescription()).isEqualTo("parameter one v2");
    assertThat(param.getDefaultValue()).isEqualTo("default1 v2");

    // rule2 has been removed -> status set to REMOVED but db row is not deleted
    RuleDto rule2 = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), organizationUuid, RULE_KEY2);
    assertThat(rule2.getStatus()).isEqualTo(RuleStatus.REMOVED);
    assertThat(rule2.getUpdatedAt()).isEqualTo(DATE2.getTime());

    // rule3 has been created
    RuleDto rule3 = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), organizationUuid, RULE_KEY3);
    assertThat(rule3).isNotNull();
    assertThat(rule3.getStatus()).isEqualTo(RuleStatus.READY);

    // verify index
    assertThat(ruleIndex.search(new RuleQuery(), new SearchOptions()).getIds()).containsOnly(RULE_KEY1, RULE_KEY3);

    // verify repositories
    assertThat(dbClient.ruleRepositoryDao().selectAll(dbTester.getSession())).extracting(RuleRepositoryDto::getKey).containsOnly("fake");
  }

  @Test
  public void add_new_tag() {
    execute(new RulesDefinition() {
      @Override
      public void define(RulesDefinition.Context context) {
        RulesDefinition.NewRepository repo = context.createRepository("fake", "java");
        repo.createRule("rule1")
          .setName("Rule One")
          .setHtmlDescription("Description of Rule One")
          .setTags("tag1");
        repo.done();
      }
    });

    String organizationUuid = dbTester.getDefaultOrganization().getUuid();
    RuleDto rule = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), organizationUuid, RULE_KEY1);
    assertThat(rule.getSystemTags()).containsOnly("tag1");

    execute(new RulesDefinition() {
      @Override
      public void define(RulesDefinition.Context context) {
        RulesDefinition.NewRepository repo = context.createRepository("fake", "java");
        repo.createRule("rule1")
          .setName("Rule One")
          .setHtmlDescription("Description of Rule One")
          .setTags("tag1", "tag2");
        repo.done();
      }
    });

    rule = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), organizationUuid, RULE_KEY1);
    assertThat(rule.getSystemTags()).containsOnly("tag1", "tag2");
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
    RuleDto rule1 = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), dbTester.getDefaultOrganization().getUuid(), RuleKey.of("fake", "rule"));
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
    RuleDto rule1 = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), dbTester.getDefaultOrganization().getUuid(), RuleKey.of("fake", "rule"));
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

    String organizationUuid = dbTester.getDefaultOrganization().getUuid();
    RuleDto rule = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), organizationUuid, RULE_KEY1);
    assertThat(rule.getStatus()).isEqualTo(RuleStatus.REMOVED);
    assertThat(ruleIndex.search(new RuleQuery().setKey(RULE_KEY1.toString()), new SearchOptions()).getTotal()).isEqualTo(0);

    // Re-install rule
    when(system.now()).thenReturn(DATE3.getTime());
    execute(new FakeRepositoryV1());

    rule = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), organizationUuid, RULE_KEY1);
    assertThat(rule.getStatus()).isEqualTo(RuleStatus.BETA);
    assertThat(ruleIndex.search(new RuleQuery().setKey(RULE_KEY1.toString()), new SearchOptions()).getTotal()).isEqualTo(1);
  }

  @Test
  public void do_not_update_rules_when_no_changes() {
    execute(new FakeRepositoryV1());
    assertThat(dbClient.ruleDao().selectAllDefinitions(dbTester.getSession())).hasSize(2);

    when(system.now()).thenReturn(DATE2.getTime());
    execute(new FakeRepositoryV1());

    String organizationUuid = dbTester.getDefaultOrganization().getUuid();
    RuleDto rule1 = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), organizationUuid, RULE_KEY1);
    assertThat(rule1.getCreatedAt()).isEqualTo(DATE1.getTime());
    assertThat(rule1.getUpdatedAt()).isEqualTo(DATE1.getTime());
  }

  @Test
  public void do_not_update_already_removed_rules() {
    execute(new FakeRepositoryV1());
    assertThat(dbClient.ruleDao().selectAllDefinitions(dbTester.getSession())).hasSize(2);
    assertThat(esTester.getIds(RuleIndexDefinition.INDEX_TYPE_RULE)).containsOnly(RULE_KEY1.toString(), RULE_KEY2.toString());

    String organizationUuid = dbTester.getDefaultOrganization().getUuid();
    RuleDto rule2 = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), organizationUuid, RULE_KEY2);
    assertThat(rule2.getStatus()).isEqualTo(RuleStatus.READY);

    when(system.now()).thenReturn(DATE2.getTime());
    execute(new FakeRepositoryV2());

    // On MySQL, need to update a rule otherwise rule2 will be seen as READY, but why ???
    dbClient.ruleDao().update(dbTester.getSession(), dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), organizationUuid, RULE_KEY1).getDefinition());
    dbTester.getSession().commit();

    // rule2 is removed
    rule2 = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), organizationUuid, RULE_KEY2);
    assertThat(rule2.getStatus()).isEqualTo(RuleStatus.REMOVED);

    assertThat(ruleIndex.search(new RuleQuery(), new SearchOptions()).getIds()).containsOnly(RULE_KEY1, RULE_KEY3);

    when(system.now()).thenReturn(DATE3.getTime());
    execute(new FakeRepositoryV2());
    dbTester.getSession().commit();

    // -> rule2 is still removed, but not update at DATE3
    rule2 = dbClient.ruleDao().selectOrFailByKey(dbTester.getSession(), organizationUuid, RULE_KEY2);
    assertThat(rule2.getStatus()).isEqualTo(RuleStatus.REMOVED);
    assertThat(rule2.getUpdatedAt()).isEqualTo(DATE2.getTime());

    assertThat(ruleIndex.search(new RuleQuery(), new SearchOptions()).getIds()).containsOnly(RULE_KEY1, RULE_KEY3);
  }

  @Test
  public void mass_insert() {
    execute(new BigRepository());
    assertThat(dbTester.countRowsOfTable("rules")).isEqualTo(BigRepository.SIZE);
    assertThat(dbTester.countRowsOfTable("rules_parameters")).isEqualTo(BigRepository.SIZE * 20);
    assertThat(esTester.getIds(RuleIndexDefinition.INDEX_TYPE_RULE)).hasSize(BigRepository.SIZE);
  }

  @Test
  public void manage_repository_extensions() {
    execute(new FindbugsRepository(), new FbContribRepository());
    List<RuleDefinitionDto> rules = dbClient.ruleDao().selectAllDefinitions(dbTester.getSession());
    assertThat(rules).hasSize(2);
    for (RuleDefinitionDto rule : rules) {
      assertThat(rule.getRepositoryKey()).isEqualTo("findbugs");
    }
  }

  @Test
  public void remove_system_tags_when_plugin_does_not_provide_any() {
    // Rule already exists in DB, with some system tags
    dbClient.ruleDao().insert(dbTester.getSession(), new RuleDefinitionDto()
      .setRuleKey("rule1")
      .setRepositoryKey("findbugs")
      .setName("Rule One")
      .setDescription("Rule one description")
      .setDescriptionFormat(RuleDto.Format.HTML)
      .setSystemTags(newHashSet("tag1", "tag2")));
    dbTester.getSession().commit();

    // Synchronize rule without tag
    execute(new FindbugsRepository());

    List<RuleDefinitionDto> rules = dbClient.ruleDao().selectAllDefinitions(dbTester.getSession());
    assertThat(rules).hasSize(1);
    RuleDefinitionDto result = rules.get(0);
    assertThat(result.getKey()).isEqualTo(RuleKey.of("findbugs", "rule1"));
    assertThat(result.getSystemTags()).isEmpty();
  }

  private void execute(RulesDefinition... defs) {
    RuleDefinitionsLoader loader = new RuleDefinitionsLoader(mock(DeprecatedRulesDefinitionLoader.class), mock(CommonRuleDefinitionsImpl.class), defs);
    Languages languages = mock(Languages.class);
    when(languages.get("java")).thenReturn(mock(Language.class));

    RegisterRules task = new RegisterRules(loader, ruleActivator, dbClient, ruleIndexer, activeRuleIndexer, languages, system);
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
        .setType(RuleType.CODE_SMELL)
        .setStatus(RuleStatus.BETA)
        .setGapDescription("squid.S115.effortToFix");
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
        .setType(RuleType.BUG)
        .setStatus(RuleStatus.READY)
        .setGapDescription("squid.S115.effortToFix.v2");
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
