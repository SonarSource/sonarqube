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
package org.sonar.server.qualityprofile;

import java.util.List;
import javax.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleParam;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDao;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QualityProfileDao;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.platform.Platform;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.CRITICAL;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;

// TODO Replace ServerTester by EsTester and DbTester
public class QProfileResetMediumTest {

  static final XooRulesDefinition RULE_DEFS = new XooRulesDefinition();
  static final XooProfileDefinition PROFILE_DEFS = new XooProfileDefinition();

  @ClassRule
  public static ServerTester tester = new ServerTester()
    .withEsIndexes()
    .addXoo().addComponents(PROFILE_DEFS, RULE_DEFS);

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  DbClient db;
  DbSession dbSession;
  QProfileReset reset;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    dbSession = db.openSession(false);
    reset = tester.get(QProfileReset.class);
  }

  @After
  public void after() {
    dbSession.close();
  }

  private void register(@Nullable Rules rules, @Nullable RulesProfile profile) {
    if (dbSession != null) {
      dbSession.close();
    }
    RULE_DEFS.set(rules);
    PROFILE_DEFS.set(profile);
    tester.get(Platform.class).executeStartupTasks();

    db = tester.get(DbClient.class);
    dbSession = tester.get(DbClient.class).openSession(false);
    dbSession.clearCache();

    reset = tester.get(QProfileReset.class);
  }

  @Test
  public void reset_language_profile() {
    RulesProfile defProfile = RulesProfile.create("Basic", ServerTester.Xoo.KEY);
    defProfile.activateRule(
      org.sonar.api.rules.Rule.create("xoo", "x1").setParams(newArrayList(new RuleParam().setKey("acceptWhitespace"))),
      RulePriority.CRITICAL).setParameter("acceptWhitespace", "true");

    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        RulesDefinition.NewRule x1 = repository.createRule("x1")
          .setName("x1 name")
          .setHtmlDescription("x1 desc")
          .setSeverity(MINOR);
        x1.createParam("acceptWhitespace")
          .setDefaultValue("false")
          .setType(RuleParamType.BOOLEAN)
          .setDescription("Accept whitespaces on the line");
      }
    },
      defProfile);

    RuleKey ruleKey = RuleKey.of("xoo", "x1");
    QualityProfileDto profile = tester.get(QualityProfileDao.class).selectByNameAndLanguage("Basic", ServerTester.Xoo.KEY, dbSession);
    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(profile.getKey(), ruleKey);

    // Change the severity and the value of the parameter in the active rule
    tester.get(RuleActivator.class).activate(dbSession,
      new RuleActivation(ruleKey).setSeverity(BLOCKER)
        .setParameter("acceptWhitespace", "false"),
      profile.getKey());
    dbSession.commit();

    // Verify severity and param has changed
    ActiveRuleDto activeRuleDto = tester.get(ActiveRuleDao.class).selectOrFailByKey(dbSession, activeRuleKey);
    assertThat(activeRuleDto.getSeverityString()).isEqualTo(BLOCKER);
    List<ActiveRuleParamDto> activeRuleParamDtos = tester.get(ActiveRuleDao.class).selectParamsByActiveRuleId(dbSession, activeRuleDto.getId());
    assertThat(activeRuleParamDtos.get(0).getKey()).isEqualTo("acceptWhitespace");
    assertThat(activeRuleParamDtos.get(0).getValue()).isEqualTo("false");

    reset.resetLanguage(dbSession, ServerTester.Xoo.KEY);
    dbSession.commit();

    // Severity and parameter value come back to origin after reset
    activeRuleDto = tester.get(ActiveRuleDao.class).selectOrFailByKey(dbSession, activeRuleKey);
    assertThat(activeRuleDto.getSeverityString()).isEqualTo(CRITICAL);

    activeRuleParamDtos = tester.get(ActiveRuleDao.class).selectParamsByActiveRuleId(dbSession, activeRuleDto.getId());
    assertThat(activeRuleParamDtos.get(0).getKey()).isEqualTo("acceptWhitespace");
    assertThat(activeRuleParamDtos.get(0).getValue()).isEqualTo("true");
  }

  @Test
  public void reset_language_profile_param_when_rule_definition_has_changed() {
    RulesProfile defProfile = RulesProfile.create("Basic", ServerTester.Xoo.KEY);
    defProfile.activateRule(org.sonar.api.rules.Rule.create("xoo", "x1"), null);

    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        RulesDefinition.NewRule x1 = repository.createRule("x1")
          .setName("x1 name")
          .setHtmlDescription("x1 desc")
          .setSeverity(MAJOR);
        x1.createParam("acceptWhitespace")
          .setDefaultValue("false")
          .setType(RuleParamType.BOOLEAN)
          .setDescription("Accept whitespaces on the line");
      }
    }, defProfile);

    QualityProfileDto profile = tester.get(QualityProfileDao.class).selectByNameAndLanguage("Basic", ServerTester.Xoo.KEY, dbSession);
    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(profile.getKey(), RuleKey.of("xoo", "x1"));

    // Change param in the rule def
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        RulesDefinition.NewRule x1 = repository.createRule("x1")
          .setName("x1 name")
          .setHtmlDescription("x1 desc")
          .setSeverity(MAJOR);
        x1.createParam("acceptWhitespace")
          .setDefaultValue("true")
          .setType(RuleParamType.BOOLEAN)
          .setDescription("Accept whitespaces on the line");
      }
    }, defProfile);

    reset.resetLanguage(dbSession, ServerTester.Xoo.KEY);

    // Parameter value come back to origin after reset
    ActiveRuleDto activeRuleDto = tester.get(ActiveRuleDao.class).selectOrFailByKey(dbSession, activeRuleKey);
    List<ActiveRuleParamDto> params = tester.get(ActiveRuleDao.class).selectParamsByActiveRuleId(dbSession, activeRuleDto.getId());
    assertThat(params).hasSize(1);
    assertThat(params.get(0).getKey()).isEqualTo("acceptWhitespace");
    assertThat(params.get(0).getValue()).isEqualTo("true");
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
        RulesDefinition.NewRepository repository = context.createRepository("xoo", ServerTester.Xoo.KEY).setName("Xoo Repo");
        rules.init(repository);
        repository.done();
      }
    }
  }

  public static class XooProfileDefinition extends ProfileDefinition {
    private RulesProfile profile;

    void set(@Nullable RulesProfile profile) {
      this.profile = profile;
    }

    @Override
    public RulesProfile createProfile(ValidationMessages validation) {
      return profile;
    }
  }

}
