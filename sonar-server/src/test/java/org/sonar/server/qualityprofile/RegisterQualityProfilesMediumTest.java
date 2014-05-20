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
import org.junit.Before;
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
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.properties.PropertyQuery;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.qualityprofile.persistence.ActiveRuleDao;
import org.sonar.server.rule2.persistence.RuleDao;
import org.sonar.server.tester.ServerTester;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class RegisterQualityProfilesMediumTest {

  @org.junit.Rule
  public ServerTester serverTester = new ServerTester().addComponents(XooRulesDefinition.class, XooProfileDefinition.class);

  private DbSession session;

  @Before
  public void setup(){
    session  = serverTester.get(MyBatis.class).openSession(false);
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void register_profile_at_startup() throws Exception {

    QualityProfileKey qualityProfileKey = QualityProfileKey.of("Basic","xoo");

    // 1. Check Profile in DB
    QualityProfileDao qualityProfileDao = serverTester.get(QualityProfileDao.class);
    assertThat(qualityProfileDao.selectAll()).hasSize(1);
    assertThat(qualityProfileDao.selectByNameAndLanguage(qualityProfileKey.name(), qualityProfileKey.lang()))
      .isNotNull();

    // 2. Check Default Profile
    PropertiesDao propertiesDao = serverTester.get(PropertiesDao.class);
    List<PropertyDto> xooDefault = propertiesDao.selectByQuery(PropertyQuery.builder().setKey("sonar.profile.xoo").build(), session);
    assertThat(xooDefault).hasSize(1);
    assertThat(xooDefault.get(0).getValue()).isEqualTo("Basic");

    // 3. Check ActiveRules in DB
    ActiveRuleDao activeRuleDao = serverTester.get(ActiveRuleDao.class);

    assertThat(activeRuleDao.findByProfileKey(qualityProfileKey, session)).hasSize(2);
    RuleKey ruleKey = RuleKey.of("xoo", "x1");
    RuleDto rule = serverTester.get(RuleDao.class).getByKey(ruleKey, session);


    ActiveRuleDto activeRule = activeRuleDao.getByKey(ActiveRuleKey.of(qualityProfileKey,ruleKey), session);

    assertThat(activeRule.getKey().qProfile()).isEqualTo(qualityProfileKey);
    assertThat(activeRule.getKey().ruleKey()).isEqualTo(rule.getKey());
    assertThat(activeRule.getSeverityString()).isEqualToIgnoringCase("MAJOR");





//
//
//    // 1. Check active rule in DAO
//
//    QProfiles qProfiles = serverTester.get(QProfiles.class);
//    List<QProfile> profiles = qProfiles.profilesByLanguage("xoo");
//    assertThat(profiles).hasSize(1);
//
//    QProfile profile = profiles.get(0);
//    assertThat(profile.id()).isNotNull();
//    assertThat(profile.name()).isEqualTo("Basic");
//    assertThat(profile.language()).isEqualTo("xoo");
//
//    assertThat(qProfiles.searchProfileRules(ProfileRuleQuery.create(profile.id()), Paging.create(10, 1)).rules()).hasSize(2);
//
//    // Rule x1
//    QProfileRule qProfileRule1 = qProfiles.searchProfileRules(ProfileRuleQuery.create(profile.id()).setNameOrKey("x1"), Paging.create(10, 1)).rules().get(0);
//    assertThat(qProfileRule1.key()).isEqualTo("x1");
//    assertThat(qProfileRule1.severity()).isEqualTo("MAJOR");
//    assertThat(qProfileRule1.params()).hasSize(1);
//
//    QProfileRuleParam qProfileRule1Param = qProfileRule1.params().get(0);
//    assertThat(qProfileRule1Param.key()).isEqualTo("acceptWhitespace");
//    assertThat(qProfileRule1Param.value()).isEqualTo("true");
//
//    // Rule x2
//    QProfileRule qProfileRule2 = qProfiles.searchProfileRules(ProfileRuleQuery.create(profile.id()).setNameOrKey("x2"), Paging.create(10, 1)).rules().get(0);
//    assertThat(qProfileRule2.key()).isEqualTo("x2");
//    assertThat(qProfileRule2.severity()).isEqualTo("BLOCKER");
//    assertThat(qProfileRule2.params()).isEmpty();
  }

  public static class XooProfileDefinition extends ProfileDefinition {
    @Override
    public RulesProfile createProfile(ValidationMessages validation) {
      final RulesProfile profile = RulesProfile.create("Basic", "xoo");
      ActiveRule activeRule1 = profile.activateRule(
        org.sonar.api.rules.Rule.create("xoo", "x1").setParams(newArrayList(new RuleParam().setKey("acceptWhitespace"))),
        RulePriority.MAJOR);
      activeRule1.setParameter("acceptWhitespace", "true");

      profile.activateRule(org.sonar.api.rules.Rule.create("xoo", "x2"), RulePriority.BLOCKER);
      return profile;
    }
  }

  public static class XooRulesDefinition implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewRepository repository = context.createRepository("xoo", "xoo").setName("Xoo Repo");
      repository.createRule("x1")
        .setName("x1 name")
        .setHtmlDescription("x1 desc")
        .setSeverity(Severity.MINOR)
        .createParam("acceptWhitespace")
        .setDefaultValue("false")
        .setType(RuleParamType.BOOLEAN)
        .setDescription("Accept whitespaces on the line");

      repository.createRule("x2")
        .setName("x2 name")
        .setHtmlDescription("x2 desc")
        .setSeverity(Severity.MAJOR);
      repository.done();
    }
  }

  private QualityProfileKey profileKey(QProfile profile){
    return QualityProfileKey.of(profile.name(), profile.language());
  }
}
