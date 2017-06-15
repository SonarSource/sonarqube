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

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.platform.Platform;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.qualityprofile.RuleActivation;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;

// TODO remaining tests should be moved to RegisterRulesTest
public class RegisterRulesMediumTest {

  static final XooRulesDefinition RULE_DEFS = new XooRulesDefinition();

  @ClassRule
  public static final ServerTester TESTER = new ServerTester()
    .withEsIndexes()
    .addXoo()
    .addComponents(RULE_DEFS);

  @org.junit.Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(TESTER);

  private DbClient db = TESTER.get(DbClient.class);
  private DbSession dbSession = TESTER.get(DbClient.class).openSession(false);

  private RuleIndex ruleIndex = TESTER.get(RuleIndex.class);
  private RuleDao ruleDao = db.ruleDao();

  private OrganizationDto defaultOrganization;

  @Before
  public void before() {
    TESTER.clearDbAndIndexes();
    dbSession.clearCache();
    String defaultOrganizationUuid = TESTER.get(DefaultOrganizationProvider.class).get().getUuid();
    defaultOrganization = db.organizationDao().selectByUuid(dbSession, defaultOrganizationUuid)
      .orElseThrow(() -> new IllegalStateException(String.format("Cannot load default organization '%s'", defaultOrganizationUuid)));
  }

  @After
  public void after() {
    if (dbSession != null) {
      dbSession.close();
    }
  }

  private void register(@Nullable Rules rules) {
    if (dbSession != null) {
      dbSession.close();
    }
    RULE_DEFS.set(rules);
    TESTER.get(Platform.class).restart();
    db = TESTER.get(DbClient.class);
    dbSession = TESTER.get(DbClient.class).openSession(false);
    dbSession.clearCache();
    ruleIndex = TESTER.get(RuleIndex.class);
  }

  @Test
  public void deactivate_removed_rules_only_if_repository_still_exists() {
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("x1").setName("x1 name").setHtmlDescription("x1 desc");
      }
    });

    // Create a profile and activate rule
    logInAsQProfileAdministrator();
    QProfileDto profile = QProfileTesting.newXooP1("org-123");
    db.qualityProfileDao().insert(dbSession, profile);
    dbSession.commit();
    dbSession.clearCache();
    RuleActivation activation = RuleActivation.create(RuleTesting.XOO_X1, null, null);
    TESTER.get(RuleActivator.class).activate(dbSession, activation, profile);

    // Restart, repo xoo still exists -> deactivate x1
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("x2").setName("x2 name").setHtmlDescription("x2 desc");
      }
    });
    assertThat(ruleIndex.search(new RuleQuery().setKey(RuleTesting.XOO_X1.toString()), new SearchOptions()).getTotal()).isEqualTo(0);
    assertThat(ruleIndex.search(new RuleQuery().setKey(RuleTesting.XOO_X2.toString()), new SearchOptions()).getTotal()).isEqualTo(1);
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(profile), new SearchOptions()).getIds()).isEmpty();
    assertThat(db.activeRuleDao().selectByProfileUuid(dbSession, QProfileTesting.XOO_P1_KEY)).isEmpty();
  }

  @Test
  public void do_not_deactivate_removed_rules_if_repository_accidentally_uninstalled() {
    Rules rules = new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("x1").setName("x1 name").setHtmlDescription("x1 desc");
      }
    };
    register(rules);

    // create a profile and activate rule
    logInAsQProfileAdministrator();
    QProfileDto profile = QProfileTesting.newXooP1("org-123");
    db.qualityProfileDao().insert(dbSession, profile);
    dbSession.commit();
    dbSession.clearCache();
    RuleActivation activation = RuleActivation.create(RuleTesting.XOO_X1, null, null);
    TESTER.get(RuleActivator.class).activate(dbSession, activation, profile);
    dbSession.commit();

    // Restart without xoo
    register(null);
    dbSession.commit();
    dbSession.clearCache();

    assertThat(ruleIndex.search(new RuleQuery().setKey(RuleTesting.XOO_X1.toString()), new SearchOptions()).getTotal()).isEqualTo(0);
    assertThat(db.activeRuleDao().selectByProfileUuid(dbSession, QProfileTesting.XOO_P1_KEY)).isEmpty();

    // Re-install
    register(rules);
    assertThat(ruleIndex.search(new RuleQuery().setKey(RuleTesting.XOO_X1.toString()), new SearchOptions()).getTotal()).isEqualTo(1);
    assertThat(db.activeRuleDao().selectByProfileUuid(dbSession, QProfileTesting.XOO_P1_KEY)).hasSize(1);
  }

  @Test
  public void update_active_rules_on_param_changes() {
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        RulesDefinition.NewRule x1Rule = repository.createRule("x1").setName("x1 name").setHtmlDescription("x1 desc");
        // has default value
        x1Rule.createParam("min").setType(RuleParamType.INTEGER).setDefaultValue("5");
        // no default value
        x1Rule.createParam("format").setType(RuleParamType.STRING);
      }
    });

    // Create profile and activate rule
    logInAsQProfileAdministrator();
    QProfileDto profile = QProfileTesting.newXooP1("org-123");
    db.qualityProfileDao().insert(dbSession, profile);
    dbSession.commit();
    dbSession.clearCache();
    RuleActivation activation = RuleActivation.create(RuleTesting.XOO_X1, null, ImmutableMap.of("format", "txt"));
    TESTER.get(RuleActivator.class).activate(dbSession, activation, profile);
    dbSession.commit();

    // Default value of "min" is changed, "format" is removed, "format2" is added, "max" is added with a default value
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        RulesDefinition.NewRule x1Rule = repository.createRule("x1").setName("x1 name").setHtmlDescription("x1 desc");
        x1Rule.createParam("min").setType(RuleParamType.INTEGER).setDefaultValue("6");
        x1Rule.createParam("format2").setType(RuleParamType.STRING);
        x1Rule.createParam("max").setType(RuleParamType.INTEGER).setDefaultValue("10");
      }
    });

    ActiveRuleDto activeRuleDto = db.activeRuleDao().selectByKey(dbSession, ActiveRuleKey.of(profile, RuleTesting.XOO_X1)).get();
    List<ActiveRuleParamDto> params = db.activeRuleDao().selectParamsByActiveRuleId(dbSession, activeRuleDto.getId());
    assertThat(params).hasSize(2);

    Map<String, ActiveRuleParamDto> parmsByKey = FluentIterable.from(params).uniqueIndex(ActiveRuleParamToKey.INSTANCE);

    // do not change default value on existing active rules -> keep min=5
    assertThat(parmsByKey.get("min").getValue()).isEqualTo("5");

    // new param with default value
    assertThat(parmsByKey.get("max").getValue()).isEqualTo("10");
  }

  @Test
  public void remove_user_tags_that_are_newly_declared_as_system() {
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("x1").setName("x1 name").setHtmlDescription("x1 desc").setTags("tag1");
      }
    });
    RuleDto rule = ruleDao.selectOrFailByKey(dbSession, defaultOrganization, RuleTesting.XOO_X1);
    assertThat(rule.getSystemTags()).containsOnly("tag1");
    assertThat(rule.getTags()).isEmpty();

    // User adds tag
    RuleUpdate update = RuleUpdate.createForPluginRule(RuleTesting.XOO_X1)
      .setTags(newHashSet("tag2"))
      .setOrganization(defaultOrganization);
    TESTER.get(RuleUpdater.class).update(dbSession, update, defaultOrganization, userSessionRule);
    dbSession.commit();

    rule = ruleDao.selectOrFailByKey(dbSession, defaultOrganization, RuleTesting.XOO_X1);
    assertThat(rule.getSystemTags()).containsOnly("tag1");
    assertThat(rule.getTags()).containsOnly("tag2");

    // FIXME: not supported anymore because information specific to an organization: Definition updated -> user tag "tag2" becomes a system tag
//    register(new Rules() {
//      @Override
//      public void init(RulesDefinition.NewRepository repository) {
//        repository.createRule("x1").setName("x1 name").setHtmlDescription("x1 desc").setTags("tag1", "tag2");
//      }
//    });
//    rule = ruleDao.selectOrFailByKey(dbSession, RuleTesting.XOO_X1);
//    assertThat(rule.getSystemTags()).containsOnly("tag1", "tag2");
//    assertThat(rule.getTags()).isEmpty();
  }

  @Test
  public void update_custom_rule_on_template_change() {
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("T1")
          .setName("template1 name")
          .setHtmlDescription("template1 desc")
          .setSeverity(Severity.MAJOR)
          .setTemplate(true)
          .createParam("format")
          .setDefaultValue("csv")
          .setType(RuleParamType.STRING)
          .setDescription("format parameter");
      }
    });
    RuleDefinitionDto template = ruleDao.selectOrFailDefinitionByKey(dbSession, RuleKey.of("xoo", "T1"));

    // Create custom rule
    RuleKey customRuleKey = TESTER.get(RuleCreator.class).create(dbSession, NewCustomRule.createForCustomRule("CUSTOM_RULE", template.getKey())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("format", "txt")));

    // Update template and restart
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        RulesDefinition.NewRule rule = repository.createRule("T1")
          .setName("template1 name")
          .setHtmlDescription("template1 desc")
          .setSeverity(Severity.BLOCKER)
          .setStatus(RuleStatus.BETA)
          .setTemplate(true)
          .setInternalKey("new_internal");
        rule
          .setDebtRemediationFunction(rule.debtRemediationFunctions().linearWithOffset("1h", "30min"))
          .setGapDescription("Effort");
      }
    });

    // Verify custom rule has been restore from the template
    RuleDefinitionDto customRule = ruleDao.selectOrFailDefinitionByKey(dbSession, customRuleKey);
    assertThat(customRule.getLanguage()).isEqualTo("xoo");
    assertThat(customRule.getConfigKey()).isEqualTo("new_internal");
    assertThat(customRule.getSeverityString()).isEqualTo(Severity.BLOCKER);
    assertThat(customRule.getStatus()).isEqualTo(RuleStatus.BETA);
    assertThat(customRule.getDefRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
    assertThat(customRule.getGapDescription()).isEqualTo("Effort");

    assertThat(ruleIndex.search(new RuleQuery().setKey(customRuleKey.toString()), new SearchOptions()).getTotal()).isEqualTo(1);
  }

  @Test
  public void do_not_update_custom_rule_if_no_template_change() {
    Rules rules = new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("T1")
          .setName("template1 name")
          .setHtmlDescription("template1 desc")
          .setSeverity(Severity.MAJOR)
          .setTemplate(true)
          .createParam("format")
          .setDefaultValue("csv")
          .setType(RuleParamType.STRING)
          .setDescription("format parameter");
      }
    };
    register(rules);
    RuleDefinitionDto template = ruleDao.selectOrFailDefinitionByKey(dbSession, RuleKey.of("xoo", "T1"));

    // Create custom rule
    RuleKey customRuleKey = TESTER.get(RuleCreator.class).create(dbSession, NewCustomRule.createForCustomRule("CUSTOM_RULE", template.getKey())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("format", "txt")));

    Long updatedAt = ruleDao.selectOrFailDefinitionByKey(dbSession, customRuleKey).getUpdatedAt();

    register(rules);

    // Verify custom rule has been restore from the template
    RuleDefinitionDto customRuleReloaded = ruleDao.selectOrFailDefinitionByKey(dbSession, customRuleKey);
    assertThat(customRuleReloaded.getUpdatedAt()).isEqualTo(updatedAt);
  }

  @Test
  public void do_not_update_custom_rule_params_from_template() {
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("T1")
          .setName("template1 name")
          .setHtmlDescription("template1 desc")
          .setSeverity(Severity.MAJOR)
          .setTemplate(true)
          .createParam("format")
          .setDefaultValue("csv")
          .setType(RuleParamType.STRING)
          .setDescription("format parameter");
      }
    });
    RuleDefinitionDto templateRule = ruleDao.selectOrFailDefinitionByKey(dbSession, RuleKey.of("xoo", "T1"));

    // Create custom rule
    RuleKey customRuleKey = TESTER.get(RuleCreator.class).create(dbSession, NewCustomRule.createForCustomRule("CUSTOM_RULE", templateRule.getKey())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("format", "txt")));

    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("T1")
          .setName("template1 name")
          .setHtmlDescription("template1 desc")
          .setSeverity(Severity.MAJOR)
          .setTemplate(true)
          // "format" removed, "format2" added
          .createParam("format2")
          .setDefaultValue("csv")
          .setType(RuleParamType.STRING)
          .setDescription("format parameter");
      }
    });

    // Verify custom rule param has not been changed!
    List<RuleParamDto> customRuleParams = ruleDao.selectRuleParamsByRuleKey(dbSession, customRuleKey);
    assertThat(customRuleParams.get(0).getName()).isEqualTo("format");
  }

  @Test
  public void disable_custom_rules_if_template_disabled() {
    Rules rules = new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("T1")
          .setName("template1 name")
          .setHtmlDescription("template1 desc")
          .setSeverity(Severity.MAJOR)
          .setTemplate(true)
          .createParam("format")
          .setDefaultValue("csv")
          .setType(RuleParamType.STRING)
          .setDescription("format parameter");
      }
    };
    register(rules);
    RuleDefinitionDto templateRule = ruleDao.selectOrFailDefinitionByKey(dbSession, RuleKey.of("xoo", "T1"));

    // Create custom rule
    RuleKey customRuleKey = TESTER.get(RuleCreator.class).create(dbSession, NewCustomRule.createForCustomRule("CUSTOM_RULE", templateRule.getKey())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("format", "txt")));
    assertThat(ruleDao.selectOrFailDefinitionByKey(dbSession, customRuleKey).getStatus()).isEqualTo(RuleStatus.READY);

    // Restart without template
    register(null);

    // Verify custom rule is removed
    assertThat(ruleDao.selectOrFailDefinitionByKey(dbSession, templateRule.getKey()).getStatus()).isEqualTo(RuleStatus.REMOVED);
    assertThat(ruleDao.selectOrFailDefinitionByKey(dbSession, customRuleKey).getStatus()).isEqualTo(RuleStatus.REMOVED);

    // Re-install template
    register(rules);
    assertThat(ruleDao.selectOrFailDefinitionByKey(dbSession, templateRule.getKey()).getStatus()).isEqualTo(RuleStatus.READY);
    assertThat(ruleDao.selectOrFailDefinitionByKey(dbSession, customRuleKey).getStatus()).isEqualTo(RuleStatus.READY);
  }

  interface Rules {
    void init(RulesDefinition.NewRepository repository);
  }

  public static class XooRulesDefinition implements RulesDefinition {
    private Rules rules = null;

    void set(@Nullable Rules rules) {
      this.rules = rules;
    }

    @Override
    public void define(Context context) {
      if (rules != null) {
        NewRepository repository = context.createRepository("xoo", ServerTester.Xoo.KEY).setName("Xoo Repo");
        rules.init(repository);
        repository.done();
      }
    }
  }

  private enum ActiveRuleParamToKey implements Function<ActiveRuleParamDto, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull ActiveRuleParamDto input) {
      return input.getKey();
    }
  }

  private void logInAsQProfileAdministrator() {
    userSessionRule.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, TESTER.get(DefaultOrganizationProvider.class).get().getUuid());
  }
}
