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

import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.qualityprofile.db.ActiveRuleDao;
import org.sonar.server.rule.db.RuleDao;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RegisterRulesTest extends AbstractDaoTestCase {

  static final Date DATE1 = DateUtils.parseDateTime("2014-01-01T19:10:03+0100");
  static final Date DATE2 = DateUtils.parseDateTime("2014-02-01T12:10:03+0100");
  static final Date DATE3 = DateUtils.parseDateTime("2014-03-01T12:10:03+0100");

  RuleActivator ruleActivator = mock(RuleActivator.class);
  System2 system;
  DbClient dbClient;
  DbSession dbSession;

  @Before
  public void before() {
    system = mock(System2.class);
    when(system.now()).thenReturn(DATE1.getTime());
    RuleDao ruleDao = new RuleDao(system);
    ActiveRuleDao activeRuleDao = new ActiveRuleDao(new QualityProfileDao(getMyBatis(), system), ruleDao, system);
    dbClient = new DbClient(getDatabase(), getMyBatis(), ruleDao, activeRuleDao,
      new QualityProfileDao(getMyBatis(), system), new CharacteristicDao(getMyBatis()));
    dbSession = dbClient.openSession(false);
  }

  @After
  public void after() throws Exception {
    dbSession.close();
  }

  @Test
  public void insert_new_rules() {
    execute(new FakeRepositoryV1());

    // verify db
    assertThat(dbClient.ruleDao().findAll(dbSession)).hasSize(2);
    RuleKey ruleKey1 = RuleKey.of("fake", "rule1");
    RuleDto rule1 = dbClient.ruleDao().getNullableByKey(dbSession, ruleKey1);
    assertThat(rule1.getName()).isEqualTo("One");
    assertThat(rule1.getDescription()).isEqualTo("Description of One");
    assertThat(rule1.getSeverityString()).isEqualTo(Severity.BLOCKER);
    assertThat(rule1.getTags()).isEmpty();
    assertThat(rule1.getSystemTags()).containsOnly("tag1", "tag2", "tag3");
    assertThat(rule1.getConfigKey()).isEqualTo("config1");
    assertThat(rule1.getStatus()).isEqualTo(RuleStatus.BETA);
    assertThat(rule1.getCreatedAt()).isEqualTo(DATE1);
    assertThat(rule1.getUpdatedAt()).isEqualTo(DATE1);
    // TODO check characteristic and remediation function

    List<RuleParamDto> params = dbClient.ruleDao().findRuleParamsByRuleKey(dbSession, ruleKey1);
    assertThat(params).hasSize(2);
    RuleParamDto param = getParam(params, "param1");
    assertThat(param.getDescription()).isEqualTo("parameter one");
    assertThat(param.getDefaultValue()).isEqualTo("default1");
  }

  @Test
  public void do_not_update_rules_when_no_changes() {
    execute(new FakeRepositoryV1());
    assertThat(dbClient.ruleDao().findAll(dbSession)).hasSize(2);

    when(system.now()).thenReturn(DATE2.getTime());
    execute(new FakeRepositoryV1());

    RuleKey ruleKey1 = RuleKey.of("fake", "rule1");
    RuleDto rule1 = dbClient.ruleDao().getNullableByKey(dbSession, ruleKey1);
    assertThat(rule1.getCreatedAt()).isEqualTo(DATE1);
    assertThat(rule1.getUpdatedAt()).isEqualTo(DATE1);
  }

  @Test
  public void update_and_remove_rules_on_changes() {
    execute(new FakeRepositoryV1());
    assertThat(dbClient.ruleDao().findAll(dbSession)).hasSize(2);

    // user adds tags and sets markdown note
    RuleKey ruleKey1 = RuleKey.of("fake", "rule1");
    RuleDto rule1 = dbClient.ruleDao().getNullableByKey(dbSession, ruleKey1);
    rule1.setTags(Sets.newHashSet("usertag1", "usertag2"));
    rule1.setNoteData("user *note*");
    rule1.setNoteUserLogin("marius");
    dbClient.ruleDao().update(dbSession, rule1);
    dbSession.commit();

    when(system.now()).thenReturn(DATE2.getTime());
    execute(new FakeRepositoryV2());

    // rule1 has been updated
    rule1 = dbClient.ruleDao().getNullableByKey(dbSession, ruleKey1);
    assertThat(rule1.getName()).isEqualTo("One v2");
    assertThat(rule1.getDescription()).isEqualTo("Description of One v2");
    assertThat(rule1.getSeverityString()).isEqualTo(Severity.INFO);
    assertThat(rule1.getTags()).containsOnly("usertag1", "usertag2");
    assertThat(rule1.getSystemTags()).containsOnly("tag1", "tag4");
    assertThat(rule1.getConfigKey()).isEqualTo("config1 v2");
    assertThat(rule1.getNoteData()).isEqualTo("user *note*");
    assertThat(rule1.getNoteUserLogin()).isEqualTo("marius");
    assertThat(rule1.getStatus()).isEqualTo(RuleStatus.READY);
    assertThat(rule1.getCreatedAt()).isEqualTo(DATE1);
    assertThat(rule1.getUpdatedAt()).isEqualTo(DATE2);

    // TODO check characteristic and remediation function
    List<RuleParamDto> params = dbClient.ruleDao().findRuleParamsByRuleKey(dbSession, ruleKey1);
    assertThat(params).hasSize(2);
    RuleParamDto param = getParam(params, "param1");
    assertThat(param.getDescription()).isEqualTo("parameter one v2");
    assertThat(param.getDefaultValue()).isEqualTo("default1 v2");

    // rule2 has been removed -> status set to REMOVED but db row is not deleted
    RuleDto rule2 = dbClient.ruleDao().getNullableByKey(dbSession, RuleKey.of("fake", "rule2"));
    assertThat(rule2.getStatus()).isEqualTo(RuleStatus.REMOVED);
    assertThat(rule2.getUpdatedAt()).isEqualTo(DATE2);

    // rule3 has been created
    RuleDto rule3 = dbClient.ruleDao().getNullableByKey(dbSession, RuleKey.of("fake", "rule3"));
    assertThat(rule3).isNotNull();
    assertThat(rule3.getStatus()).isEqualTo(RuleStatus.READY);
  }

  @Test
  public void do_not_update_already_removed_rules() {
    execute(new FakeRepositoryV1());
    assertThat(dbClient.ruleDao().findAll(dbSession)).hasSize(2);

    RuleDto rule2 = dbClient.ruleDao().getByKey(dbSession, RuleKey.of("fake", "rule2"));
    assertThat(rule2.getStatus()).isEqualTo(RuleStatus.READY);

    when(system.now()).thenReturn(DATE2.getTime());
    execute(new FakeRepositoryV2());

    // On MySQL, need to update a rule otherwise rule2 will be seen as READY, but why ???
    dbClient.ruleDao().update(dbSession, dbClient.ruleDao().getByKey(dbSession, RuleKey.of("fake", "rule1")));
    dbSession.commit();

    // rule2 is removed
    rule2 = dbClient.ruleDao().getNullableByKey(dbSession, RuleKey.of("fake", "rule2"));
    assertThat(rule2.getStatus()).isEqualTo(RuleStatus.REMOVED);

    when(system.now()).thenReturn(DATE3.getTime());
    execute(new FakeRepositoryV2());
    dbSession.commit();

    // -> rule2 is still removed, but not update at DATE3
    rule2 = dbClient.ruleDao().getNullableByKey(dbSession, RuleKey.of("fake", "rule2"));
    assertThat(rule2.getStatus()).isEqualTo(RuleStatus.REMOVED);
    assertThat(rule2.getUpdatedAt()).isEqualTo(DATE2);
  }

  @Test
  public void mass_insert() {
    execute(new BigRepository());
    assertThat(dbClient.ruleDao().findAll(dbSession)).hasSize(BigRepository.SIZE);
    assertThat(dbClient.ruleDao().findAllRuleParams(dbSession)).hasSize(BigRepository.SIZE * 20);
  }

  @Test
  public void manage_repository_extensions() {
    execute(new FindbugsRepository(), new FbContribRepository());
    List<RuleDto> rules = dbClient.ruleDao().findAll(dbSession);
    assertThat(rules).hasSize(2);
    for (RuleDto rule : rules) {
      assertThat(rule.getRepositoryKey()).isEqualTo("findbugs");
    }
  }

  private void execute(RulesDefinition... defs) {
    RuleDefinitionsLoader loader = new RuleDefinitionsLoader(mock(DeprecatedRulesDefinitionLoader.class), new RuleRepositories(),defs);
    Languages languages = mock(Languages.class);
    when(languages.get("java")).thenReturn(mock(Language.class));

    RegisterRules task = new RegisterRules(loader, ruleActivator, dbClient, languages);
    task.start();
    // Execute a commit to refresh session state as the task is using its own session
    dbSession.commit();
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
        .setSeverity(Severity.BLOCKER)
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
        .setSeverity(Severity.INFO)
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
