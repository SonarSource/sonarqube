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
import org.sonar.core.persistence.DbSession;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.properties.PropertyQuery;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.persistence.ActiveRuleDao;
import org.sonar.server.tester.ServerTester;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class RegisterQualityProfilesMediumTest {

  ServerTester tester;
  DbSession dbSession;

  @After
  public void tearDown() throws Exception {
    if (dbSession != null) {
      dbSession.close();
    }
    if (tester != null) {
      tester.stop();
    }
  }

  @Test
  public void register_profile_definitions() throws Exception {
    tester = new ServerTester().addComponents(XooRulesDefinition.class, XooProfileDefinition.class);
    tester.start();
    dbSession = dbClient().openSession(false);
    QualityProfileKey qualityProfileKey = QualityProfileKey.of("Basic", "xoo");

    // Check Profile in DB
    QualityProfileDao qualityProfileDao = dbClient().qualityProfileDao();
    assertThat(qualityProfileDao.findAll(dbSession)).hasSize(1);
    assertThat(qualityProfileDao.getByKey(qualityProfileKey, dbSession)).isNotNull();

    // Check Default Profile
    PropertiesDao propertiesDao = dbClient().propertiesDao();
    List<PropertyDto> xooDefault = propertiesDao.selectByQuery(PropertyQuery.builder().setKey("sonar.profile.xoo").build(), dbSession);
    assertThat(xooDefault).hasSize(1);
    assertThat(xooDefault.get(0).getValue()).isEqualTo("Basic");

    // Check ActiveRules in DB
    ActiveRuleDao activeRuleDao = dbClient().activeRuleDao();
    assertThat(activeRuleDao.findByProfileKey(qualityProfileKey, dbSession)).hasSize(2);
    RuleKey ruleKey = RuleKey.of("xoo", "x1");

    ActiveRuleDto activeRule = activeRuleDao.getByKey(ActiveRuleKey.of(qualityProfileKey, ruleKey), dbSession);
    assertThat(activeRule.getKey().qProfile()).isEqualTo(qualityProfileKey);
    assertThat(activeRule.getKey().ruleKey()).isEqualTo(ruleKey);
    assertThat(activeRule.getSeverityString()).isEqualTo(Severity.CRITICAL);

    // Check ActiveRuleParameters in DB
    Map<String, ActiveRuleParamDto> params = ActiveRuleParamDto.groupByKey(activeRuleDao.findParamsByActiveRule(activeRule, dbSession));
    assertThat(params).hasSize(2);
    // set by profile
    assertThat(params.get("acceptWhitespace").getValue()).isEqualTo("true");
    // default value
    assertThat(params.get("max").getValue()).isEqualTo("10");
  }

  @Test
  public void fail_if_two_definitions_are_marked_as_default_on_the_same_language() throws Exception {
    tester = new ServerTester().addComponents(new SimpleProfileDefinition("one", true), new SimpleProfileDefinition("two", true));

    try {
      tester.start();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Several Quality profiles are flagged as default for the language xoo: [one, two]");
    }
  }

  @Test
  public void mark_profile_as_default() throws Exception {
    tester = new ServerTester().addComponents(new SimpleProfileDefinition("one", false), new SimpleProfileDefinition("two", true));

    tester.start();
    dbSession = dbClient().openSession(false);
    PropertiesDao propertiesDao = dbClient().propertiesDao();
    List<PropertyDto> props = propertiesDao.selectByQuery(PropertyQuery.builder().setKey("sonar.profile.xoo").build(), dbSession);
    assertThat(props).hasSize(1);
    assertThat(props.get(0).getValue()).isEqualTo("two");
  }

  @Test
  public void use_sonar_way_as_default_profile_if_none_are_marked_as_default() throws Exception {
    tester = new ServerTester().addComponents(new SimpleProfileDefinition("Sonar way", false), new SimpleProfileDefinition("Other way", false));

    tester.start();
    dbSession = dbClient().openSession(false);
    PropertiesDao propertiesDao = dbClient().propertiesDao();
    List<PropertyDto> props = propertiesDao.selectByQuery(PropertyQuery.builder().setKey("sonar.profile.xoo").build(), dbSession);
    assertThat(props).hasSize(1);
    assertThat(props.get(0).getValue()).isEqualTo("Sonar way");
  }

  private DbClient dbClient() {
    return tester.get(DbClient.class);
  }

  public static class XooProfileDefinition extends ProfileDefinition {
    @Override
    public RulesProfile createProfile(ValidationMessages validation) {
      final RulesProfile profile = RulesProfile.create("Basic", "xoo");
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
      NewRepository repository = context.createRepository("xoo", "xoo").setName("Xoo Repo");
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
