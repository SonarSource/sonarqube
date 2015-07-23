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

import java.util.Map;
import org.junit.After;
import org.junit.Test;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.RuleParam;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QualityProfileDao;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.loadedtemplate.LoadedTemplateDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.platform.Platform;
import org.sonar.server.qualityprofile.db.ActiveRuleDao;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.tester.ServerTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class RegisterQualityProfilesMediumTest {

  ServerTester tester;
  DbSession dbSession;

  @After
  public void tearDown() {
    if (dbSession != null) {
      dbSession.close();
    }
    if (tester != null) {
      tester.stop();
    }
  }

  @Test
  public void register_existing_profile_definitions() {
    tester = new ServerTester().withStartupTasks().addXoo().addComponents(XooRulesDefinition.class, XooProfileDefinition.class);
    tester.start();
    dbSession = dbClient().openSession(false);

    // Check Profile in DB
    QualityProfileDao qualityProfileDao = dbClient().qualityProfileDao();
    assertThat(qualityProfileDao.selectAll(dbSession)).hasSize(1);
    QualityProfileDto profile = qualityProfileDao.selectByNameAndLanguage("Basic", "xoo", dbSession);
    assertThat(profile).isNotNull();

    // Check ActiveRules in DB
    ActiveRuleDao activeRuleDao = dbClient().activeRuleDao();
    assertThat(activeRuleDao.selectByProfileKey(dbSession, profile.getKey())).hasSize(2);
    RuleKey ruleKey = RuleKey.of("xoo", "x1");
    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(profile.getKey(), ruleKey);

    // 0. Check and clear ES
    assertThat(tester.get(ActiveRuleIndex.class).getNullableByKey(activeRuleKey)).isNotNull();
    tester.clearIndexes();
    assertThat(tester.get(ActiveRuleIndex.class).getNullableByKey(activeRuleKey)).isNull();
    tester.get(Platform.class).restart();
    assertThat(tester.get(ActiveRuleIndex.class).getNullableByKey(activeRuleKey)).isNotNull();

    // Check ActiveRules in ES
    org.sonar.server.qualityprofile.ActiveRule activeRule = tester.get(ActiveRuleIndex.class).getNullableByKey(activeRuleKey);
    assertThat(activeRule.key().qProfile()).isEqualTo(profile.getKee());
    assertThat(activeRule.key().ruleKey()).isEqualTo(ruleKey);
    assertThat(activeRule.severity()).isEqualTo(Severity.CRITICAL);

    // TODO
    // Check ActiveRuleParameters in DB
    Map<String, ActiveRuleParamDto> params =
      ActiveRuleParamDto.groupByKey(activeRuleDao.selectParamsByActiveRuleKey(dbSession, activeRule.key()));
    assertThat(params).hasSize(2);
    // set by profile
    assertThat(params.get("acceptWhitespace").getValue()).isEqualTo("true");
    // default value
    assertThat(params.get("max").getValue()).isEqualTo("10");

  }

  @Test
  public void register_profile_definitions() {
    tester = new ServerTester().withStartupTasks().addXoo().addComponents(XooRulesDefinition.class, XooProfileDefinition.class);
    tester.start();
    dbSession = dbClient().openSession(false);

    // Check Profile in DB
    QualityProfileDao qualityProfileDao = dbClient().qualityProfileDao();
    assertThat(qualityProfileDao.selectAll(dbSession)).hasSize(1);
    QualityProfileDto profile = qualityProfileDao.selectByNameAndLanguage("Basic", "xoo", dbSession);
    assertThat(profile).isNotNull();

    // Check Default Profile
    verifyDefaultProfile("xoo", "Basic");

    // Check ActiveRules in DB
    ActiveRuleDao activeRuleDao = dbClient().activeRuleDao();
    assertThat(activeRuleDao.selectByProfileKey(dbSession, profile.getKey())).hasSize(2);
    RuleKey ruleKey = RuleKey.of("xoo", "x1");

    ActiveRuleDto activeRule = activeRuleDao.getNullableByKey(dbSession, ActiveRuleKey.of(profile.getKey(), ruleKey));
    assertThat(activeRule.getKey().qProfile()).isEqualTo(profile.getKey());
    assertThat(activeRule.getKey().ruleKey()).isEqualTo(ruleKey);
    assertThat(activeRule.getSeverityString()).isEqualTo(Severity.CRITICAL);

    // Check ActiveRuleParameters in DB
    Map<String, ActiveRuleParamDto> params =
      ActiveRuleParamDto.groupByKey(activeRuleDao.selectParamsByActiveRuleKey(dbSession, activeRule.getKey()));
    assertThat(params).hasSize(2);
    // set by profile
    assertThat(params.get("acceptWhitespace").getValue()).isEqualTo("true");
    // default value
    assertThat(params.get("max").getValue()).isEqualTo("10");
  }

  @Test
  public void do_not_register_profile_if_missing_language() {
    // xoo language is not installed
    tester = new ServerTester().addComponents(XooRulesDefinition.class, XooProfileDefinition.class);
    tester.start();
    dbSession = dbClient().openSession(false);

    // Check Profile in DB
    QualityProfileDao qualityProfileDao = dbClient().qualityProfileDao();
    assertThat(qualityProfileDao.selectAll(dbSession)).hasSize(0);
  }

  @Test
  public void fail_if_two_definitions_are_marked_as_default_on_the_same_language() {
    tester = new ServerTester().addXoo().addComponents(new SimpleProfileDefinition("one", true), new SimpleProfileDefinition("two", true));

    try {
      tester.start();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Several Quality profiles are flagged as default for the language xoo: [one, two]");
    }
  }

  @Test
  public void mark_profile_as_default() {
    tester = new ServerTester().withStartupTasks().addXoo().addComponents(new SimpleProfileDefinition("one", false), new SimpleProfileDefinition("two", true));

    tester.start();
    verifyDefaultProfile("xoo", "two");
  }

  @Test
  public void use_sonar_way_as_default_profile_if_none_are_marked_as_default() {
    tester = new ServerTester().withStartupTasks().addXoo().addComponents(new SimpleProfileDefinition("Sonar way", false), new SimpleProfileDefinition("Other way", false));

    tester.start();
    verifyDefaultProfile("xoo", "Sonar way");
  }

  @Test
  public void do_not_reset_default_profile_if_still_valid() {
    tester = new ServerTester().withStartupTasks().addXoo().addComponents(new SimpleProfileDefinition("one", true), new SimpleProfileDefinition("two", false));
    tester.start();

    QualityProfileDao profileDao = dbClient().qualityProfileDao();
    DbSession session = dbClient().openSession(false);
    QualityProfileDto profileTwo = profileDao.selectByNameAndLanguage("two", "xoo", session);
    tester.get(QProfileFactory.class).setDefault(session, profileTwo.getKee());
    session.commit();

    verifyDefaultProfile("xoo", "two");

    tester.get(Platform.class).restart();
    // restart must keep "two" as default profile, even if "one" is marked as it
    verifyDefaultProfile("xoo", "two");
  }

  /**
   * Probably for db migration
   */
  @Test
  public void clean_up_profiles_if_missing_loaded_template() {
    tester = new ServerTester().addXoo().addComponents(XooRulesDefinition.class, XooProfileDefinition.class);
    tester.start();

    dbSession = dbClient().openSession(false);
    String templateKey = RegisterQualityProfiles.templateKey(new QProfileName("xoo", "Basic"));
    dbClient().loadedTemplateDao().delete(dbSession, LoadedTemplateDto.QUALITY_PROFILE_TYPE, templateKey);
    dbSession.commit();
    assertThat(dbClient().loadedTemplateDao().countByTypeAndKey(LoadedTemplateDto.QUALITY_PROFILE_TYPE, templateKey, dbSession)).isEqualTo(0);
    dbSession.close();

    tester.get(Platform.class).restart();

    // do not fail
  }

  private void verifyDefaultProfile(String language, String name) {
    QualityProfileDto defaultProfile = dbClient().qualityProfileDao().selectDefaultProfile(language);
    assertThat(defaultProfile).isNotNull();
    assertThat(defaultProfile.getName()).isEqualTo(name);
  }

  private DbClient dbClient() {
    return tester.get(DbClient.class);
  }

  public static class XooProfileDefinition extends ProfileDefinition {
    @Override
    public RulesProfile createProfile(ValidationMessages validation) {
      final RulesProfile profile = RulesProfile.create("Basic", ServerTester.Xoo.KEY);
      ActiveRule activeRule1 = profile.activateRule(
        org.sonar.api.rules.Rule.create("xoo", "x1").setParams(newArrayList(new RuleParam().setKey("acceptWhitespace"))),
        RulePriority.CRITICAL);
      activeRule1.setParameter("acceptWhitespace", "true");

      profile.activateRule(org.sonar.api.rules.Rule.create("xoo", "x2"), RulePriority.INFO);
      return profile;
    }
  }

  public static class XooRulesDefinition implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewRepository repository = context.createRepository("xoo", ServerTester.Xoo.KEY).setName("Xoo Repo");
      NewRule x1 = repository.createRule("x1")
        .setName("x1 name")
        .setHtmlDescription("x1 desc")
        .setSeverity(Severity.MINOR);
      x1.createParam("acceptWhitespace")
        .setDefaultValue("false")
        .setType(RuleParamType.BOOLEAN)
        .setDescription("Accept whitespaces on the line");
      x1.createParam("max")
        .setDefaultValue("10")
        .setType(RuleParamType.INTEGER)
        .setDescription("Maximum");

      repository.createRule("x2")
        .setName("x2 name")
        .setHtmlDescription("x2 desc")
        .setSeverity(Severity.INFO);
      repository.done();
    }
  }

  public static class SimpleProfileDefinition extends ProfileDefinition {
    private final boolean asDefault;
    private final String name;

    public SimpleProfileDefinition(String name, boolean asDefault) {
      this.name = name;
      this.asDefault = asDefault;
    }

    @Override
    public RulesProfile createProfile(ValidationMessages validation) {
      RulesProfile profile = RulesProfile.create(name, "xoo");
      profile.setDefaultProfile(asDefault);
      return profile;
    }
  }
}
