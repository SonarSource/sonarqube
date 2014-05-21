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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.qualityprofile.persistence.ActiveRuleDao;
import org.sonar.server.rule2.persistence.RuleDao;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class ActiveRuleServiceMediumTest {

  @Rule
  public ServerTester tester = new ServerTester()
    .addComponents(XooRulesDefinition.class, JavaRulesDefinition.class);

  QualityProfileKey profileKey = QualityProfileKey.of("MyProfile", "xoo");
  DbClient dbClient;
  DbSession dbSession;
  ActiveRuleService service;
  ActiveRuleIndex index;

  @Before
  public void before() {
    dbClient = tester.get(DbClient.class);
    dbSession = dbClient.openSession(false);
    service = tester.get(ActiveRuleService.class);
    index = tester.get(ActiveRuleIndex.class);

    // create quality profile
    dbClient.qualityProfileDao().insert(QualityProfileDto.createFor("MyProfile", "xoo"), dbSession);
    dbSession.commit();
  }

  @Test
  public void activate() throws Exception {
    grantPermission();

    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(profileKey, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    service.activate(activation);

    // verify db
    List<ActiveRuleDto> activeRuleDtos = dbClient.activeRuleDao().findByProfileKey(profileKey, dbSession);
    assertThat(activeRuleDtos).hasSize(1);
    assertThat(activeRuleDtos.get(0).getSeverityString()).isEqualTo(Severity.BLOCKER);
    assertThat(activeRuleDtos.get(0).getInheritance()).isNull();
    List<ActiveRuleParamDto> params = dbClient.activeRuleDao().findParamsByActiveRule(activeRuleDtos.get(0), dbSession);
    assertThat(params).hasSize(1);
    assertThat(params.get(0).getValue()).isEqualTo("7");

    // verify es
    index.refresh();
    ActiveRule activeRule = index.getByKey(activation.getKey());
    assertThat(activeRule).isNotNull();
    assertThat(activeRule.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(activeRule.inheritance()).isEqualTo(ActiveRule.Inheritance.NONE);
    assertThat(activeRule.params()).hasSize(1);
    assertThat(activeRule.params().get("max")).isEqualTo("7");
  }

  @Test
  public void activate_with_default_severity_and_parameter() throws Exception {
    grantPermission();
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(profileKey, RuleKey.of("xoo", "x1")));
    service.activate(activation);

    // verify db
    List<ActiveRuleDto> activeRuleDtos = dbClient.activeRuleDao().findByProfileKey(profileKey, dbSession);
    assertThat(activeRuleDtos).hasSize(1);
    assertThat(activeRuleDtos.get(0).getSeverityString()).isEqualTo(Severity.MINOR);
    assertThat(activeRuleDtos.get(0).getInheritance()).isNull();
    List<ActiveRuleParamDto> params = dbClient.activeRuleDao().findParamsByActiveRule(activeRuleDtos.get(0), dbSession);
    assertThat(params).hasSize(1);
    assertThat(params.get(0).getValue()).isEqualTo("10");

    // verify es
    index.refresh();
    ActiveRule activeRule = index.getByKey(activation.getKey());
    assertThat(activeRule).isNotNull();
    assertThat(activeRule.severity()).isEqualTo(Severity.MINOR);
    assertThat(activeRule.inheritance()).isEqualTo(ActiveRule.Inheritance.NONE);
    assertThat(activeRule.params()).hasSize(1);
    assertThat(activeRule.params().get("max")).isEqualTo("10");
  }

  @Test
  public void update_activation_severity_and_parameters() throws Exception {
    // initial activation
    grantPermission();
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(profileKey, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.BLOCKER);
    service.activate(activation);

    // update
    RuleActivation update = new RuleActivation(ActiveRuleKey.of(profileKey, RuleKey.of("xoo", "x1")));
    update.setSeverity(Severity.CRITICAL);
    update.setParameter("max", "42");
    service.activate(update);

    // verify db
    List<ActiveRuleDto> activeRuleDtos = dbClient.activeRuleDao().findByProfileKey(profileKey, dbSession);
    assertThat(activeRuleDtos).hasSize(1);
    assertThat(activeRuleDtos.get(0).getSeverityString()).isEqualTo(Severity.CRITICAL);
    assertThat(activeRuleDtos.get(0).getInheritance()).isNull();
    List<ActiveRuleParamDto> params = dbClient.activeRuleDao().findParamsByActiveRule(activeRuleDtos.get(0), dbSession);
    assertThat(params).hasSize(1);
    assertThat(params.get(0).getValue()).isEqualTo("42");

    // verify es
    index.refresh();
    ActiveRule activeRule = index.getByKey(activation.getKey());
    assertThat(activeRule).isNotNull();
    assertThat(activeRule.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(activeRule.inheritance()).isEqualTo(ActiveRule.Inheritance.NONE);
    assertThat(activeRule.params()).hasSize(1);
    assertThat(activeRule.params().get("max")).isEqualTo("42");
  }

  @Test
  @Ignore
  public void update_activation_but_new_parameter() throws Exception {
    // initial activation
    grantPermission();
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(profileKey, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.BLOCKER);
    service.activate(activation);
    // TODO delete activeruleparam max


    // update
    RuleActivation update = new RuleActivation(ActiveRuleKey.of(profileKey, RuleKey.of("xoo", "x1")));
    update.setSeverity(Severity.CRITICAL);
    update.setParameter("max", "42");
    // contrary to activerule, the param 'max' is supposed to be inserted but not updated
    service.activate(update);

    // verify db
    List<ActiveRuleDto> activeRuleDtos = dbClient.activeRuleDao().findByProfileKey(profileKey, dbSession);
    assertThat(activeRuleDtos).hasSize(1);
    assertThat(activeRuleDtos.get(0).getSeverityString()).isEqualTo(Severity.CRITICAL);
    assertThat(activeRuleDtos.get(0).getInheritance()).isNull();
    List<ActiveRuleParamDto> params = dbClient.activeRuleDao().findParamsByActiveRule(activeRuleDtos.get(0), dbSession);
    assertThat(params).hasSize(1);
    assertThat(params.get(0).getValue()).isEqualTo("42");

    // verify es
    index.refresh();
    ActiveRule activeRule = index.getByKey(activation.getKey());
    assertThat(activeRule).isNotNull();
    assertThat(activeRule.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(activeRule.inheritance()).isEqualTo(ActiveRule.Inheritance.NONE);
    assertThat(activeRule.params()).hasSize(1);
    assertThat(activeRule.params().get("max")).isEqualTo("42");
  }

  @Test
  public void revert_activation_to_default_severity_and_parameters() throws Exception {
    // initial activation
    grantPermission();
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(profileKey, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    service.activate(activation);

    // update
    RuleActivation update = new RuleActivation(ActiveRuleKey.of(profileKey, RuleKey.of("xoo", "x1")));
    service.activate(update);

    // verify db
    List<ActiveRuleDto> activeRuleDtos = dbClient.activeRuleDao().findByProfileKey(profileKey, dbSession);
    assertThat(activeRuleDtos).hasSize(1);
    assertThat(activeRuleDtos.get(0).getSeverityString()).isEqualTo(Severity.MINOR);
    assertThat(activeRuleDtos.get(0).getInheritance()).isNull();
    List<ActiveRuleParamDto> params = dbClient.activeRuleDao().findParamsByActiveRule(activeRuleDtos.get(0), dbSession);
    assertThat(params).hasSize(1);
    assertThat(params.get(0).getValue()).isEqualTo("10");

    // verify es
    index.refresh();
    ActiveRule activeRule = index.getByKey(activation.getKey());
    assertThat(activeRule).isNotNull();
    assertThat(activeRule.severity()).isEqualTo(Severity.MINOR);
    assertThat(activeRule.inheritance()).isEqualTo(ActiveRule.Inheritance.NONE);
    assertThat(activeRule.params()).hasSize(1);
    assertThat(activeRule.params().get("max")).isEqualTo("10");
  }

  @Test
  public void fail_to_activate_if_not_granted() throws Exception {
    MockUserSession.set().setLogin("marius");
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(profileKey, RuleKey.of("xoo", "x1")));

    try {
      service.activate(activation);
      fail();
    } catch (ForbiddenException e) {
      verifyZeroActiveRules(activation.getKey());
    }
  }

  @Test
  public void fail_to_activate_if_different_languages() throws Exception {
    // profile and rule have different languages
    grantPermission();
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(profileKey, RuleKey.of("squid", "j1")));

    try {
      service.activate(activation);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Rule squid:j1 and profile MyProfile:xoo have different languages");
      verifyZeroActiveRules(activation.getKey());
    }
  }

  @Test
  public void fail_to_activate_if_unknown_rule() throws Exception {
    // profile and rule have different languages
    grantPermission();
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(profileKey, RuleKey.of("xoo", "x3")));

    try {
      service.activate(activation);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Rule not found: xoo:x3");
      verifyZeroActiveRules(activation.getKey());
    }
  }

  @Test
  public void fail_to_activate_if_unknown_profile() throws Exception {
    // profile and rule have different languages
    grantPermission();
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(QualityProfileKey.of("other", "js"), RuleKey.of("xoo", "x1")));

    try {
      service.activate(activation);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Quality profile not found: other:js");
    }
  }

  @Test
  public void fail_to_activate_if_invalid_parameter() throws Exception {
    grantPermission();
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(profileKey, RuleKey.of("xoo", "x1")));
    activation.setParameter("max", "foo");

    try {
      service.activate(activation);
      fail();
    } catch (BadRequestException e) {
      assertThat(e.l10nKey()).isEqualTo("errors.type.notInteger");
      verifyZeroActiveRules(activation.getKey());
    }
  }

  @Test
  public void deactivate() throws Exception {
    // activation
    grantPermission();
    ActiveRuleKey key = ActiveRuleKey.of(profileKey, RuleKey.of("xoo", "x1"));
    RuleActivation activation = new RuleActivation(key);
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    service.activate(activation);

    // deactivation
    service.deactivate(key);

    verifyZeroActiveRules(key);
  }

  @Test
  public void ignore_deactivation_if_rule_not_activated() throws Exception {
    grantPermission();

    // deactivation
    ActiveRuleKey key = ActiveRuleKey.of(profileKey, RuleKey.of("xoo", "x1"));
    service.deactivate(key);

    verifyZeroActiveRules(key);
  }

  @Test
  public void deactivation_fails_if_rule_not_found() throws Exception {
    grantPermission();
    ActiveRuleKey key = ActiveRuleKey.of(profileKey, RuleKey.of("xoo", "x3"));
    try {
      service.deactivate(key);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Rule not found: xoo:x3");
      verifyZeroActiveRules(key);
    }
  }

  @Test
  public void deactivation_fails_if_profile_not_found() throws Exception {
    grantPermission();
    ActiveRuleKey key = ActiveRuleKey.of(QualityProfileKey.of("other", "js"), RuleKey.of("xoo", "x1"));
    try {
      service.deactivate(key);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Quality profile not found: other:js");
    }
  }

  @Test
  public void find_by_ruleKey() throws Exception {
    QualityProfileDto profile = QualityProfileDto.createFor("name","java");
    dbClient.getDao(QualityProfileDao.class).insert(profile, dbSession);

    RuleDto rule = RuleDto.createFor(RuleKey.of("java", "r1"))
      .setSeverity("MAJOR");
    dbClient.getDao(RuleDao.class).insert(rule, dbSession);


    RuleDto rule2 = RuleDto.createFor(RuleKey.of("java", "r2"))
      .setSeverity("MAJOR");
    dbClient.getDao(RuleDao.class).insert(rule2, dbSession);

    dbClient.getDao(ActiveRuleDao.class).insert(
      ActiveRuleDto.createFor(profile, rule).setSeverity("MINOR"),
      dbSession);

    dbClient.getDao(ActiveRuleDao.class).insert(
      ActiveRuleDto.createFor(profile, rule2).setSeverity("BLOCKER"),
      dbSession);

    dbSession.commit();

    assertThat(service.findByRuleKey(rule.getKey())).hasSize(1);
    assertThat(service.findByRuleKey(rule.getKey()).get(0)
    .severity()).isEqualTo("MINOR");

    assertThat(service.findByRuleKey(rule2.getKey())).hasSize(1);
    assertThat(service.findByRuleKey(rule2.getKey()).get(0)
      .severity()).isEqualTo("BLOCKER");


  }

  private void grantPermission() {
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN).setLogin("marius");
  }

  private void verifyZeroActiveRules(ActiveRuleKey key) {
    // verify db
    List<ActiveRuleDto> activeRuleDtos = dbClient.activeRuleDao().findByProfileKey(key.qProfile(), dbSession);
    assertThat(activeRuleDtos).isEmpty();
    //TODO test params

    // verify es
    index.refresh();
    ActiveRule activeRule = index.getByKey(key);
    assertThat(activeRule).isNull();
  }

  public static class XooRulesDefinition implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewRepository repository = context.createRepository("xoo", "xoo").setName("Xoo Repo");
      repository.createRule("x1")
        .setName("x1 name")
        .setHtmlDescription("x1 desc")
        .setSeverity(Severity.MINOR)
        .createParam("max")
        .setDefaultValue("10")
        .setType(RuleParamType.INTEGER)
        .setDescription("Maximum");
      repository.done();
    }
  }

  public static class JavaRulesDefinition implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewRepository repository = context.createRepository("squid", "java").setName("Java Repo");
      repository.createRule("j1")
        .setName("j1")
        .setHtmlDescription("J1");
      repository.done();
    }
  }
}
