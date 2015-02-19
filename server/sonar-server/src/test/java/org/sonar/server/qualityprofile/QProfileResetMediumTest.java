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

package org.sonar.server.qualityprofile;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleParam;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.platform.Platform;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.tester.ServerTester;

import javax.annotation.Nullable;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class QProfileResetMediumTest {

  static final XooRulesDefinition RULE_DEFS = new XooRulesDefinition();
  static final XooProfileDefinition PROFILE_DEFS = new XooProfileDefinition();

  @ClassRule
  public static ServerTester tester = new ServerTester().addXoo().addComponents(PROFILE_DEFS, RULE_DEFS);

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
  public void after() throws Exception {
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
  public void reset_language_profile() throws Exception {
    RulesProfile defProfile = RulesProfile.create("Basic", ServerTester.Xoo.KEY);
    defProfile.activateRule(
      org.sonar.api.rules.Rule.create("xoo", "x1").setParams(newArrayList(new RuleParam().setKey("acceptWhitespace"))),
      RulePriority.CRITICAL
    ).setParameter("acceptWhitespace", "true");

    register(new Rules() {
    @Override
    public void init(RulesDefinition.NewRepository repository) {
      RulesDefinition.NewRule x1 = repository.createRule("x1")
        .setName("x1 name")
        .setHtmlDescription("x1 desc")
        .setSeverity(Severity.MINOR);
      x1.createParam("acceptWhitespace")
        .setDefaultValue("false")
        .setType(RuleParamType.BOOLEAN)
        .setDescription("Accept whitespaces on the line");
    }},
      defProfile
    );

    RuleKey ruleKey = RuleKey.of("xoo", "x1");
    QualityProfileDto profile = tester.get(QualityProfileDao.class).getByNameAndLanguage("Basic", ServerTester.Xoo.KEY, dbSession);
    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(profile.getKey(), ruleKey);

    // Change the severity and the value of the parameter in the active rule
    tester.get(RuleActivator.class).activate(dbSession,
      new RuleActivation(ruleKey).setSeverity(Severity.BLOCKER)
        .setParameter("acceptWhitespace", "false"),
      profile.getKey()
    );
    dbSession.commit();

    // Verify severity and param has changed
    ActiveRule activeRule = tester.get(ActiveRuleIndex.class).getNullableByKey(activeRuleKey);
    assertThat(activeRule.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(activeRule.params()).isEqualTo(ImmutableMap.of("acceptWhitespace", "false"));

    reset.resetLanguage(ServerTester.Xoo.KEY);

    // Severity and parameter value come back to origin after reset
    activeRule = tester.get(ActiveRuleIndex.class).getNullableByKey(activeRuleKey);
    assertThat(activeRule.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(activeRule.params()).isEqualTo(ImmutableMap.of("acceptWhitespace", "true"));
  }

  @Test
  public void reset_language_profile_param_when_rule_definition_has_changed() throws Exception {
    RulesProfile defProfile = RulesProfile.create("Basic", ServerTester.Xoo.KEY);
    defProfile.activateRule(org.sonar.api.rules.Rule.create("xoo", "x1"), null);

    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        RulesDefinition.NewRule x1 = repository.createRule("x1")
          .setName("x1 name")
          .setHtmlDescription("x1 desc")
          .setSeverity(Severity.MAJOR);
        x1.createParam("acceptWhitespace")
          .setDefaultValue("false")
          .setType(RuleParamType.BOOLEAN)
          .setDescription("Accept whitespaces on the line");
      }}, defProfile);

    QualityProfileDto profile = tester.get(QualityProfileDao.class).getByNameAndLanguage("Basic", ServerTester.Xoo.KEY, dbSession);
    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(profile.getKey(), RuleKey.of("xoo", "x1"));

    // Change param in the rule def
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        RulesDefinition.NewRule x1 = repository.createRule("x1")
          .setName("x1 name")
          .setHtmlDescription("x1 desc")
          .setSeverity(Severity.MAJOR);
        x1.createParam("acceptWhitespace")
          .setDefaultValue("true")
          .setType(RuleParamType.BOOLEAN)
          .setDescription("Accept whitespaces on the line");
      }}, defProfile);

    reset.resetLanguage(ServerTester.Xoo.KEY);

    // Parameter value come back to origin after reset
    ActiveRule activeRule = tester.get(ActiveRuleIndex.class).getNullableByKey(activeRuleKey);
    assertThat(activeRule.params()).isEqualTo(ImmutableMap.of("acceptWhitespace", "true"));
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
